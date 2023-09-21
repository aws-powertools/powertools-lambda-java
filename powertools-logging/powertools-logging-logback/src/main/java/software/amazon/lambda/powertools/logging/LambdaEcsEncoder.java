/*
 * Copyright 2023 Amazon.com, Inc. or its affiliates.
 * Licensed under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *     http://www.apache.org/licenses/LICENSE-2.0
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 */

package software.amazon.lambda.powertools.logging;

import static java.nio.charset.StandardCharsets.UTF_8;
import static software.amazon.lambda.powertools.logging.internal.PowertoolsLoggedFields.FUNCTION_ARN;
import static software.amazon.lambda.powertools.logging.internal.PowertoolsLoggedFields.FUNCTION_COLD_START;
import static software.amazon.lambda.powertools.logging.internal.PowertoolsLoggedFields.FUNCTION_MEMORY_SIZE;
import static software.amazon.lambda.powertools.logging.internal.PowertoolsLoggedFields.FUNCTION_NAME;
import static software.amazon.lambda.powertools.logging.internal.PowertoolsLoggedFields.FUNCTION_REQUEST_ID;
import static software.amazon.lambda.powertools.logging.internal.PowertoolsLoggedFields.FUNCTION_TRACE_ID;
import static software.amazon.lambda.powertools.logging.internal.PowertoolsLoggedFields.FUNCTION_VERSION;

import ch.qos.logback.classic.pattern.ThrowableHandlingConverter;
import ch.qos.logback.classic.pattern.ThrowableProxyConverter;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.classic.spi.IThrowableProxy;
import ch.qos.logback.classic.spi.ThrowableProxy;
import ch.qos.logback.core.encoder.EncoderBase;
import java.util.Map;
import software.amazon.lambda.powertools.common.internal.LambdaHandlerProcessor;
import software.amazon.lambda.powertools.logging.internal.LambdaEcsSerializer;


/**
 * This class will encode the logback event into the format expected by the ECS service (ElasticSearch).
 * <br/>
 * Inspired from <code>co.elastic.logging.logback.EcsEncoder</code>, this class doesn't use
 * any JSON (de)serialization library (Jackson, Gson, etc.) or Elastic library to avoid the dependency.
 * <br/>
 * This encoder also adds cloud information (see <a href="https://www.elastic.co/guide/en/ecs/current/ecs-cloud.html">doc</a>)
 * and Lambda function information (see <a href="https://www.elastic.co/guide/en/ecs/current/ecs-faas.html">doc</a>, currently in beta).
 */
public class LambdaEcsEncoder extends EncoderBase<ILoggingEvent> {

    protected static final String ECS_VERSION = "1.2.0";
    protected static final String CLOUD_PROVIDER = "aws";
    protected static final String CLOUD_SERVICE = "lambda";

    private final ThrowableProxyConverter throwableProxyConverter = new ThrowableProxyConverter();
    protected ThrowableHandlingConverter throwableConverter = null;
    private boolean includeCloudInfo = true;
    private boolean includeFaasInfo = true;

    @Override
    public byte[] headerBytes() {
        return null;
    }

    @Override
    public byte[] encode(ILoggingEvent event) {
        Map<String, String> mdcPropertyMap = event.getMDCPropertyMap();

        StringBuilder builder = new StringBuilder(256);
        LambdaEcsSerializer.serializeObjectStart(builder);
        LambdaEcsSerializer.serializeTimestamp(builder, event.getTimeStamp(), "yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", "UTC");
        LambdaEcsSerializer.serializeEcsVersion(builder, ECS_VERSION);
        LambdaEcsSerializer.serializeLogLevel(builder, event.getLevel());
        LambdaEcsSerializer.serializeFormattedMessage(builder, event.getFormattedMessage());
        IThrowableProxy throwableProxy = event.getThrowableProxy();
        if (throwableProxy != null) {
            if (throwableConverter != null) {
                LambdaEcsSerializer.serializeException(builder, throwableProxy.getClassName(),
                        throwableProxy.getMessage(), throwableConverter.convert(event));
            } else if (throwableProxy instanceof ThrowableProxy) {
                LambdaEcsSerializer.serializeException(builder, ((ThrowableProxy) throwableProxy).getThrowable());
            } else {
                LambdaEcsSerializer.serializeException(builder, throwableProxy.getClassName(),
                        throwableProxy.getMessage(), throwableProxyConverter.convert(event));
            }
        }
        LambdaEcsSerializer.serializeServiceName(builder, LambdaHandlerProcessor.serviceName());
        LambdaEcsSerializer.serializeServiceVersion(builder, mdcPropertyMap.get(FUNCTION_VERSION.getName()));
        LambdaEcsSerializer.serializeLoggerName(builder, event.getLoggerName());
        LambdaEcsSerializer.serializeThreadName(builder, event.getThreadName());
        String arn = mdcPropertyMap.get(FUNCTION_ARN.getName());

        if (includeCloudInfo) {
            LambdaEcsSerializer.serializeCloudProvider(builder, CLOUD_PROVIDER);
            LambdaEcsSerializer.serializeCloudService(builder, CLOUD_SERVICE);
            if (arn != null) {
                String[] arnParts = arn.split(":");
                LambdaEcsSerializer.serializeCloudRegion(builder, arnParts[3]);
                LambdaEcsSerializer.serializeCloudAccountId(builder, arnParts[4]);
            }
        }

        if (includeFaasInfo) {
            LambdaEcsSerializer.serializeFunctionId(builder, arn);
            LambdaEcsSerializer.serializeFunctionName(builder, mdcPropertyMap.get(FUNCTION_NAME.getName()));
            LambdaEcsSerializer.serializeFunctionVersion(builder, mdcPropertyMap.get(FUNCTION_VERSION.getName()));
            LambdaEcsSerializer.serializeFunctionMemory(builder, mdcPropertyMap.get(FUNCTION_MEMORY_SIZE.getName()));
            LambdaEcsSerializer.serializeFunctionExecutionId(builder,
                    mdcPropertyMap.get(FUNCTION_REQUEST_ID.getName()));
            LambdaEcsSerializer.serializeColdStart(builder, mdcPropertyMap.get(FUNCTION_COLD_START.getName()));
            LambdaEcsSerializer.serializeTraceId(builder, mdcPropertyMap.get(FUNCTION_TRACE_ID.getName()));
        }
        LambdaEcsSerializer.serializeAdditionalFields(builder, event.getMDCPropertyMap());
        LambdaEcsSerializer.serializeObjectEnd(builder);
        return builder.toString().getBytes(UTF_8);
    }

    @Override
    public byte[] footerBytes() {
        return null;
    }

    /**
     * Specify a throwable converter to format the stacktrace according to your need (default is <b>null</b>, no throwableConverter):
     * <br/>
     * <pre>{@code
     *     <encoder class="software.amazon.lambda.powertools.logging.LambdaEcsEncoder">
     *         <throwableConverter class="net.logstash.logback.stacktrace.ShortenedThrowableConverter">
     *              <maxDepthPerThrowable>30</maxDepthPerThrowable>
     *              <maxLength>2048</maxLength>
     *              <shortenedClassNameLength>20</shortenedClassNameLength>
     *              <exclude>sun\.reflect\..*\.invoke.*</exclude>
     *              <exclude>net\.sf\.cglib\.proxy\.MethodProxy\.invoke</exclude>
     *              <evaluator class="myorg.MyCustomEvaluator"/>
     *              <rootCauseFirst>true</rootCauseFirst>
     *              <inlineHash>true</inlineHash>
     *         </throwableConverter>
     *     </encoder>
     * }</pre>
     * @param throwableConverter
     */
    public void setThrowableConverter(ThrowableHandlingConverter throwableConverter) {
        this.throwableConverter = throwableConverter;
    }

    /**
     * Specify if cloud information should be logged (default is <b>true</b>):
     * <ul>
     *     <li>cloud.provider</li>
     *     <li>cloud.service.name</li>
     *     <li>cloud.region</li>
     *     <li>cloud.account.id</li>
     * </ul>
     * <br/>
     * We strongly recommend to keep these information.
     * <br/>
     * <pre>{@code
     *     <encoder class="software.amazon.lambda.powertools.logging.LambdaEcsEncoder">
     *         <includeCloudInfo>false</includeCloudInfo>
     *     </encoder>
     * }</pre>
     * @param includeCloudInfo if thread information should be logged
     */
    public void setIncludeCloudInfo(boolean includeCloudInfo) {
        this.includeCloudInfo = includeCloudInfo;
    }

    /**
     * Specify if Lambda function information should be logged (default is <b>true</b>):
     * <ul>
     *     <li>faas.id</li>
     *     <li>faas.name</li>
     *     <li>faas.version</li>
     *     <li>faas.memory</li>
     *     <li>faas.execution</li>
     *     <li>faas.coldstart</li>
     *     <li>trace.id</li>
     * </ul>
     * <br/>
     * We strongly recommend to keep these information.
     * <br/>
     * <pre>{@code
     *     <encoder class="software.amazon.lambda.powertools.logging.LambdaEcsEncoder">
     *         <includeFaasInfo>false</includeFaasInfo>
     *     </encoder>
     * }</pre>
     * @param includeFaasInfo if function information should be logged
     */
    public void setIncludeFaasInfo(boolean includeFaasInfo) {
        this.includeFaasInfo = includeFaasInfo;
    }
}

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

package software.amazon.lambda.powertools.logging.logback;

import static java.nio.charset.StandardCharsets.UTF_8;
import static software.amazon.lambda.powertools.logging.internal.PowertoolsLoggedFields.CORRELATION_ID;
import static software.amazon.lambda.powertools.logging.internal.PowertoolsLoggedFields.FUNCTION_ARN;
import static software.amazon.lambda.powertools.logging.internal.PowertoolsLoggedFields.FUNCTION_COLD_START;
import static software.amazon.lambda.powertools.logging.internal.PowertoolsLoggedFields.FUNCTION_MEMORY_SIZE;
import static software.amazon.lambda.powertools.logging.internal.PowertoolsLoggedFields.FUNCTION_NAME;
import static software.amazon.lambda.powertools.logging.internal.PowertoolsLoggedFields.FUNCTION_REQUEST_ID;
import static software.amazon.lambda.powertools.logging.internal.PowertoolsLoggedFields.FUNCTION_TRACE_ID;
import static software.amazon.lambda.powertools.logging.internal.PowertoolsLoggedFields.FUNCTION_VERSION;
import static software.amazon.lambda.powertools.logging.logback.JsonUtils.serializeArguments;
import static software.amazon.lambda.powertools.logging.logback.JsonUtils.serializeMDCEntries;
import static software.amazon.lambda.powertools.logging.logback.JsonUtils.serializeTimestamp;

import ch.qos.logback.classic.pattern.ThrowableHandlingConverter;
import ch.qos.logback.classic.pattern.ThrowableProxyConverter;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.classic.spi.IThrowableProxy;
import ch.qos.logback.classic.spi.ThrowableProxy;
import ch.qos.logback.core.encoder.EncoderBase;
import java.io.IOException;
import java.util.Arrays;
import java.util.Map;
import software.amazon.lambda.powertools.common.internal.LambdaHandlerProcessor;
import software.amazon.lambda.powertools.logging.internal.JsonSerializer;


/**
 * This class will encode the logback event into the format expected by the Elastic Common Schema (ECS) service (for Elasticsearch).
 * <br/>
 * Inspired from <code>co.elastic.logging.logback.EcsEncoder</code>, this class doesn't use
 * any JSON (de)serialization library (Jackson, Gson, etc.) or Elastic library to avoid the dependency.
 * <br/>
 * This encoder also adds cloud information (see <a href="https://www.elastic.co/guide/en/ecs/current/ecs-cloud.html">doc</a>)
 * and Lambda function information (see <a href="https://www.elastic.co/guide/en/ecs/current/ecs-faas.html">doc</a>, currently in beta).
 */
public class LambdaEcsEncoder extends EncoderBase<ILoggingEvent> {

    protected static final String TIMESTAMP_ATTR_NAME = "@timestamp";
    protected static final String ECS_VERSION_ATTR_NAME = "ecs.version";
    protected static final String LOGGER_ATTR_NAME = "log.logger";
    protected static final String LEVEL_ATTR_NAME = "log.level";
    protected static final String SERVICE_NAME_ATTR_NAME = "service.name";
    protected static final String SERVICE_VERSION_ATTR_NAME = "service.version";
    protected static final String FORMATTED_MESSAGE_ATTR_NAME = "message";
    protected static final String THREAD_ATTR_NAME = "process.thread.name";
    protected static final String EXCEPTION_MSG_ATTR_NAME = "error.message";
    protected static final String EXCEPTION_CLASS_ATTR_NAME = "error.type";
    protected static final String EXCEPTION_STACK_ATTR_NAME = "error.stack_trace";
    protected static final String CLOUD_PROVIDER_ATTR_NAME = "cloud.provider";
    protected static final String CLOUD_REGION_ATTR_NAME = "cloud.region";
    protected static final String CLOUD_ACCOUNT_ATTR_NAME = "cloud.account.id";
    protected static final String CLOUD_SERVICE_ATTR_NAME = "cloud.service.name";
    protected static final String FUNCTION_COLD_START_ATTR_NAME = "faas.coldstart";
    protected static final String FUNCTION_REQUEST_ID_ATTR_NAME = "faas.execution";
    protected static final String FUNCTION_ARN_ATTR_NAME = "faas.id";
    protected static final String FUNCTION_NAME_ATTR_NAME = "faas.name";
    protected static final String FUNCTION_VERSION_ATTR_NAME = "faas.version";
    protected static final String FUNCTION_MEMORY_ATTR_NAME = "faas.memory";
    protected static final String FUNCTION_TRACE_ID_ATTR_NAME = "trace.id";
    protected static final String CORRELATION_ID_ATTR_NAME = "correlation.id";

    protected static final String ECS_VERSION = "1.2.0";
    protected static final String CLOUD_PROVIDER = "aws";
    protected static final String CLOUD_SERVICE = "lambda";

    private final ThrowableProxyConverter throwableProxyConverter = new ThrowableProxyConverter();
    protected ThrowableHandlingConverter throwableConverter = null;
    private boolean includeCloudInfo = true;
    private boolean includeFaasInfo = true;

    @Override
    public byte[] headerBytes() {
        return new byte[0];
    }

    /**
     * Main method of the encoder. Encode a logging event into Json format (with Elastic Search fields)
     *
     * @param event the logging event
     * @return the encoded bytes
     */

    @SuppressWarnings("java:S106")
    @Override
    public byte[] encode(ILoggingEvent event) {
        final Map<String, String> mdcPropertyMap = event.getMDCPropertyMap();

        StringBuilder builder = new StringBuilder();
        try (JsonSerializer serializer = new JsonSerializer(builder)) {
            serializer.writeStartObject();
            serializeTimestamp(serializer, event.getTimeStamp(), "yyyy-MM-dd'T'HH:mm:ss.SSS'Z'",
                    "UTC", TIMESTAMP_ATTR_NAME);
            serializer.writeRaw(',');
            serializer.writeStringField(ECS_VERSION_ATTR_NAME, ECS_VERSION);
            serializer.writeRaw(',');
            serializer.writeStringField(LEVEL_ATTR_NAME, event.getLevel().toString());
            serializer.writeRaw(',');
            serializer.writeStringField(FORMATTED_MESSAGE_ATTR_NAME, event.getFormattedMessage());

            serializeException(event, serializer);

            serializer.writeRaw(',');
            serializer.writeStringField(SERVICE_NAME_ATTR_NAME, LambdaHandlerProcessor.serviceName());
            serializer.writeRaw(',');
            serializer.writeStringField(SERVICE_VERSION_ATTR_NAME, mdcPropertyMap.get(FUNCTION_VERSION.getName()));
            serializer.writeRaw(',');
            serializer.writeStringField(LOGGER_ATTR_NAME, event.getLoggerName());
            serializer.writeRaw(',');
            serializer.writeStringField(THREAD_ATTR_NAME, event.getThreadName());

            String arn = mdcPropertyMap.get(FUNCTION_ARN.getName());

            serializeCloudInfo(serializer, arn);

            serializeFunctionInfo(serializer, arn, mdcPropertyMap);

            serializeMDCEntries(mdcPropertyMap, serializer);

            serializeArguments(event, serializer);

            serializer.writeEndObject();
            serializer.writeRaw('\n');
        } catch (IOException e) {
            System.err.printf("Failed to encode log event, error: %s.%n", e.getMessage());
        }
        return builder.toString().getBytes(UTF_8);
    }

    private void serializeFunctionInfo(JsonSerializer serializer, String arn, Map<String, String> mdcPropertyMap) {
        if (includeFaasInfo) {
            serializer.writeRaw(',');
            serializer.writeStringField(FUNCTION_ARN_ATTR_NAME, arn);
            serializer.writeRaw(',');
            serializer.writeStringField(FUNCTION_NAME_ATTR_NAME, mdcPropertyMap.get(FUNCTION_NAME.getName()));
            serializer.writeRaw(',');
            serializer.writeStringField(FUNCTION_VERSION_ATTR_NAME, mdcPropertyMap.get(FUNCTION_VERSION.getName()));
            serializer.writeRaw(',');
            serializer.writeStringField(FUNCTION_MEMORY_ATTR_NAME, mdcPropertyMap.get(FUNCTION_MEMORY_SIZE.getName()));
            serializer.writeRaw(',');
            serializer.writeStringField(FUNCTION_REQUEST_ID_ATTR_NAME, mdcPropertyMap.get(FUNCTION_REQUEST_ID.getName()));
            serializer.writeRaw(',');
            serializer.writeStringField(FUNCTION_COLD_START_ATTR_NAME, mdcPropertyMap.get(FUNCTION_COLD_START.getName()));
            serializer.writeRaw(',');
            serializer.writeStringField(FUNCTION_TRACE_ID_ATTR_NAME, mdcPropertyMap.get(FUNCTION_TRACE_ID.getName()));
            String correlationId = mdcPropertyMap.get(CORRELATION_ID.getName());
            if (correlationId != null) {
                serializer.writeRaw(',');
                serializer.writeStringField(CORRELATION_ID_ATTR_NAME, correlationId);
            }
        }
    }

    private void serializeCloudInfo(JsonSerializer serializer, String arn) {
        if (includeCloudInfo) {
            serializer.writeRaw(',');
            serializer.writeStringField(CLOUD_PROVIDER_ATTR_NAME, CLOUD_PROVIDER);
            serializer.writeRaw(',');
            serializer.writeStringField(CLOUD_SERVICE_ATTR_NAME, CLOUD_SERVICE);
            if (arn != null) {
                String[] arnParts = arn.split(":");
                serializer.writeRaw(',');
                serializer.writeStringField(CLOUD_REGION_ATTR_NAME, arnParts[3]);
                serializer.writeRaw(',');
                serializer.writeStringField(CLOUD_ACCOUNT_ATTR_NAME, arnParts[4]);
            }
        }
    }

    private void serializeException(ILoggingEvent event, JsonSerializer serializer) {
        IThrowableProxy throwableProxy = event.getThrowableProxy();
        if (throwableProxy != null) {
            if (throwableConverter != null) {
                serializeException(serializer, throwableProxy.getClassName(),
                        throwableProxy.getMessage(), throwableConverter.convert(event));
            } else if (throwableProxy instanceof ThrowableProxy) {
                Throwable throwable = ((ThrowableProxy) throwableProxy).getThrowable();
                serializeException(serializer, throwable.getClass().getName(), throwable.getMessage(),
                        Arrays.toString(throwable.getStackTrace()));
            } else {
                serializeException(serializer, throwableProxy.getClassName(),
                        throwableProxy.getMessage(), throwableProxyConverter.convert(event));
            }
        }
    }

    @Override
    public byte[] footerBytes() {
        return new byte[0];
    }

    /**
     * Specify a throwable converter to format the stacktrace according to your need
     * (default is <b>null</b>, no throwableConverter):
     * <br/>
     * <pre>{@code
     *     <encoder class="software.amazon.lambda.powertools.logging.logback.LambdaEcsEncoder">
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
     *
     * @param throwableConverter converter for the throwable
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
     *     <encoder class="software.amazon.lambda.powertools.logging.logback.LambdaEcsEncoder">
     *         <includeCloudInfo>false</includeCloudInfo>
     *     </encoder>
     * }</pre>
     *
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
     *     <encoder class="software.amazon.lambda.powertools.logging.logback.LambdaEcsEncoder">
     *         <includeFaasInfo>false</includeFaasInfo>
     *     </encoder>
     * }</pre>
     *
     * @param includeFaasInfo if function information should be logged
     */
    public void setIncludeFaasInfo(boolean includeFaasInfo) {
        this.includeFaasInfo = includeFaasInfo;
    }

    private void serializeException(JsonSerializer serializer, String className, String message, String stackTrace) {
        serializer.writeRaw(',');
        serializer.writeObjectField(EXCEPTION_MSG_ATTR_NAME, message);
        serializer.writeRaw(',');
        serializer.writeObjectField(EXCEPTION_CLASS_ATTR_NAME, className);
        serializer.writeRaw(',');
        serializer.writeObjectField(EXCEPTION_STACK_ATTR_NAME, stackTrace);
    }
}

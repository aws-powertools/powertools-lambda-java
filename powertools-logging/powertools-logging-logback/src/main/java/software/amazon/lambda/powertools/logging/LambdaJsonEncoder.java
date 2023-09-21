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

import ch.qos.logback.classic.pattern.ThrowableHandlingConverter;
import ch.qos.logback.classic.pattern.ThrowableProxyConverter;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.classic.spi.IThrowableProxy;
import ch.qos.logback.classic.spi.ThrowableProxy;
import ch.qos.logback.core.encoder.EncoderBase;
import software.amazon.lambda.powertools.logging.internal.LambdaJsonSerializer;

/**
 * Custom encoder for logback that encodes logs in JSON format.
 * It does not use a JSON library but a custom serializer ({@link LambdaJsonSerializer}) to reduce the weight of the library.
 */
public class LambdaJsonEncoder extends EncoderBase<ILoggingEvent> {

    private final ThrowableProxyConverter throwableProxyConverter = new ThrowableProxyConverter();
    protected ThrowableHandlingConverter throwableConverter = null;
    protected String timestampFormat = "yyyy-MM-dd'T'HH:mm:ss.SSSZz";
    protected String timestampFormatTimezoneId = null;
    private boolean includeThreadInfo = false;
    private boolean includePowertoolsInfo = true;

    @Override
    public byte[] headerBytes() {
        return null;
    }

    @Override
    public void start() {
        super.start();
        throwableProxyConverter.start();
        if (throwableConverter != null) {
            throwableConverter.start();
        }
    }

    @Override
    public byte[] encode(ILoggingEvent event) {
        StringBuilder builder = new StringBuilder(256);
        LambdaJsonSerializer.serializeObjectStart(builder);
        LambdaJsonSerializer.serializeLogLevel(builder, event.getLevel());
        LambdaJsonSerializer.serializeFormattedMessage(builder, event.getFormattedMessage());
        IThrowableProxy throwableProxy = event.getThrowableProxy();
        if (throwableProxy != null) {
            if (throwableConverter != null) {
                LambdaJsonSerializer.serializeException(builder, throwableProxy.getClassName(),
                        throwableProxy.getMessage(), throwableConverter.convert(event));
            } else if (throwableProxy instanceof ThrowableProxy) {
                LambdaJsonSerializer.serializeException(builder, ((ThrowableProxy) throwableProxy).getThrowable());
            } else {
                LambdaJsonSerializer.serializeException(builder, throwableProxy.getClassName(),
                        throwableProxy.getMessage(), throwableProxyConverter.convert(event));
            }
        }
        LambdaJsonSerializer.serializePowertools(builder, event.getMDCPropertyMap(), includePowertoolsInfo);
        if (includeThreadInfo) {
            LambdaJsonSerializer.serializeThreadName(builder, event.getThreadName());
            LambdaJsonSerializer.serializeThreadId(builder, String.valueOf(Thread.currentThread().getId()));
            LambdaJsonSerializer.serializeThreadPriority(builder, String.valueOf(Thread.currentThread().getPriority()));
        }
        LambdaJsonSerializer.serializeTimestamp(builder, event.getTimeStamp(), timestampFormat,
                timestampFormatTimezoneId);
        LambdaJsonSerializer.serializeObjectEnd(builder);
        return builder.toString().getBytes(UTF_8);
    }

    @Override
    public byte[] footerBytes() {
        return null;
    }

    /**
     * Specify the format of the timestamp (default is <b>yyyy-MM-dd'T'HH:mm:ss.SSSZz</b>):
     * <br/>
     * <pre>{@code
     *     <encoder class="software.amazon.lambda.powertools.logging.LambdaJsonEncoder">
     *         <timestampFormat>yyyy-MM-dd'T'HH:mm:ss.SSSZz</timestampFormat>
     *     </encoder>
     * }</pre>
     * @param timestampFormat format of the timestamp (compatible with {@link java.text.SimpleDateFormat})
     */
    public void setTimestampFormat(String timestampFormat) {
        this.timestampFormat = timestampFormat;
    }

    /**
     * Specify the format of the time zone id for timestamp (default is <b>null</b>, no timezone):
     * <br/>
     * <pre>{@code
     *     <encoder class="software.amazon.lambda.powertools.logging.LambdaJsonEncoder">
     *         <timestampFormatTimezoneId>Europe/Paris</timestampFormatTimezoneId>
     *     </encoder>
     * }</pre>
     * @param timestampFormatTimezoneId Zone Id (see {@link java.util.TimeZone})
     */
    public void setTimestampFormatTimezoneId(String timestampFormatTimezoneId) {
        this.timestampFormatTimezoneId = timestampFormatTimezoneId;
    }

    /**
     * Specify a throwable converter to format the stacktrace according to your need (default is <b>null</b>, no throwableConverter):
     * <br/>
     * <pre>{@code
     *     <encoder class="software.amazon.lambda.powertools.logging.LambdaJsonEncoder">
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
     * Specify if thread information should be logged (default is <b>false</b>)
     * <br/>
     * <pre>{@code
     *     <encoder class="software.amazon.lambda.powertools.logging.LambdaJsonEncoder">
     *         <includeThreadInfo>true</includeThreadInfo>
     *     </encoder>
     * }</pre>
     * @param includeThreadInfo if thread information should be logged
     */
    public void setIncludeThreadInfo(boolean includeThreadInfo) {
        this.includeThreadInfo = includeThreadInfo;
    }

    /**
     * Specify if Lambda function information should be logged (default is <b>true</b>):
     * <ul>
     *     <li>function_name</li>
     *     <li>function_version</li>
     *     <li>function_arn</li>
     *     <li>function_memory_size</li>
     *     <li>function_request_id</li>
     *     <li>cold_start</li>
     *     <li>xray_trace_id</li>
     *     <li>sampling_rate</li>
     *     <li>service</li>
     * </ul>
     * <br/>
     * We strongly recommend to keep these information.
     * <br/>
     * <pre>{@code
     *     <encoder class="software.amazon.lambda.powertools.logging.LambdaJsonEncoder">
     *         <includePowertoolsInfo>false</includePowertoolsInfo>
     *     </encoder>
     * }</pre>
     * @param includePowertoolsInfo if function information should be logged
     */
    public void setIncludePowertoolsInfo(boolean includePowertoolsInfo) {
        this.includePowertoolsInfo = includePowertoolsInfo;
    }
}

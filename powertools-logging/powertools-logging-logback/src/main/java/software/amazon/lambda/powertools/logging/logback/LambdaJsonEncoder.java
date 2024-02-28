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
import static software.amazon.lambda.powertools.logging.logback.JsonUtils.serializeArguments;
import static software.amazon.lambda.powertools.logging.logback.JsonUtils.serializeMDCEntries;
import static software.amazon.lambda.powertools.logging.logback.JsonUtils.serializeMDCEntry;
import static software.amazon.lambda.powertools.logging.logback.JsonUtils.serializeTimestamp;

import ch.qos.logback.classic.pattern.ThrowableHandlingConverter;
import ch.qos.logback.classic.pattern.ThrowableProxyConverter;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.classic.spi.IThrowableProxy;
import ch.qos.logback.classic.spi.ThrowableProxy;
import ch.qos.logback.core.encoder.EncoderBase;
import java.io.IOException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.TreeMap;
import software.amazon.lambda.powertools.logging.internal.JsonSerializer;
import software.amazon.lambda.powertools.logging.internal.PowertoolsLoggedFields;

/**
 * Custom encoder for logback that encodes logs in JSON format.
 */
public class LambdaJsonEncoder extends EncoderBase<ILoggingEvent> {

    protected static final String TIMESTAMP_ATTR_NAME = "timestamp";
    protected static final String LEVEL_ATTR_NAME = "level";
    protected static final String FORMATTED_MESSAGE_ATTR_NAME = "message";
    protected static final String THREAD_ATTR_NAME = "thread";
    protected static final String THREAD_ID_ATTR_NAME = "thread_id";
    protected static final String THREAD_PRIORITY_ATTR_NAME = "thread_priority";
    protected static final String EXCEPTION_MSG_ATTR_NAME = "message";
    protected static final String EXCEPTION_CLASS_ATTR_NAME = "name";
    protected static final String EXCEPTION_STACK_ATTR_NAME = "stack";
    protected static final String EXCEPTION_ATTR_NAME = "error";

    private final ThrowableProxyConverter throwableProxyConverter = new ThrowableProxyConverter();
    protected ThrowableHandlingConverter throwableConverter = null;
    protected String timestampFormat = "yyyy-MM-dd'T'HH:mm:ss.SSS'Z'";
    protected String timestampFormatTimezoneId = null;
    private boolean includeThreadInfo = false;
    private boolean includePowertoolsInfo = true;

    @Override
    public byte[] headerBytes() {
        return new byte[0];
    }

    @Override
    public void start() {
        super.start();
        throwableProxyConverter.start();
        if (throwableConverter != null) {
            throwableConverter.start();
        }
    }

    @SuppressWarnings("java:S106")
    @Override
    public byte[] encode(ILoggingEvent event) {
        StringBuilder builder = new StringBuilder();
        try (JsonSerializer serializer = new JsonSerializer(builder)) {
            serializer.writeStartObject();
            serializer.writeStringField(LEVEL_ATTR_NAME, event.getLevel().toString());
            serializer.writeRaw(',');
            serializer.writeStringField(FORMATTED_MESSAGE_ATTR_NAME, event.getFormattedMessage());

            serializeException(event, serializer);

            TreeMap<String, String> sortedMap = new TreeMap<>(event.getMDCPropertyMap());
            serializePowertools(sortedMap, serializer);

            serializeMDCEntries(sortedMap, serializer);

            serializeArguments(event, serializer);

            serializeThreadInfo(event, serializer);

            serializer.writeRaw(',');
            serializeTimestamp(serializer, event.getTimeStamp(),
                    timestampFormat, timestampFormatTimezoneId, TIMESTAMP_ATTR_NAME);

            serializer.writeEndObject();
            serializer.writeRaw('\n');
        } catch (IOException e) {
            System.err.printf("Failed to encode log event, error: %s.%n", e.getMessage());
        }
        return builder.toString().getBytes(UTF_8);
    }

    private void serializeThreadInfo(ILoggingEvent event, JsonSerializer serializer) {
        if (includeThreadInfo) {
            if (event.getThreadName() != null) {
                serializer.writeRaw(',');
                serializer.writeStringField(THREAD_ATTR_NAME, event.getThreadName());
            }
            serializer.writeRaw(',');
            serializer.writeNumberField(THREAD_ID_ATTR_NAME, Thread.currentThread().getId());
            serializer.writeRaw(',');
            serializer.writeNumberField(THREAD_PRIORITY_ATTR_NAME, Thread.currentThread().getPriority());
        }
    }

    private void serializePowertools(TreeMap<String, String> sortedMap, JsonSerializer serializer) {
        if (includePowertoolsInfo) {
            for (Map.Entry<String, String> entry : sortedMap.entrySet()) {
                if (PowertoolsLoggedFields.stringValues().contains(entry.getKey())
                    && !(entry.getKey().equals(PowertoolsLoggedFields.SAMPLING_RATE.getName()) && entry.getValue().equals("0.0"))) {
                    serializeMDCEntry(entry, serializer);
                }
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
                serializeException(serializer, throwableProxy.getClassName(), throwableProxy.getMessage(), throwableProxyConverter.convert(
                        event));
            }
        }
    }

    @Override
    public byte[] footerBytes() {
        return new byte[0];
    }

    /**
     * Specify the format of the timestamp (default is <b>yyyy-MM-dd'T'HH:mm:ss.SSS'Z'</b>).
     * Note that if you use the Lambda Advanced Logging Configuration, you should keep the default format.
     * <br/>
     * <pre>{@code
     *     <encoder class="software.amazon.lambda.powertools.logging.logback.LambdaJsonEncoder">
     *         <timestampFormat>yyyy-MM-dd'T'HH:mm:ss.SSSZz</timestampFormat>
     *     </encoder>
     * }</pre>
     *
     * @param timestampFormat format of the timestamp (compatible with {@link java.text.SimpleDateFormat})
     */
    public void setTimestampFormat(String timestampFormat) {
        this.timestampFormat = timestampFormat;
    }

    /**
     * Specify the format of the time zone id for timestamp (default is <b>null</b>, no timezone):
     * <br/>
     * <pre>{@code
     *     <encoder class="software.amazon.lambda.powertools.logging.logback.LambdaJsonEncoder">
     *         <timestampFormatTimezoneId>Europe/Paris</timestampFormatTimezoneId>
     *     </encoder>
     * }</pre>
     *
     * @param timestampFormatTimezoneId Zone Id (see {@link java.util.TimeZone})
     */
    public void setTimestampFormatTimezoneId(String timestampFormatTimezoneId) {
        this.timestampFormatTimezoneId = timestampFormatTimezoneId;
    }

    /**
     * Specify a throwable converter to format the stacktrace according to your need
     * (default is <b>null</b>, no throwableConverter):
     * <br/>
     * <pre>{@code
     *     <encoder class="software.amazon.lambda.powertools.logging.logback.LambdaJsonEncoder">
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
     * Specify if thread information should be logged (default is <b>false</b>)
     * <br/>
     * <pre>{@code
     *     <encoder class="software.amazon.lambda.powertools.logging.logback.LambdaJsonEncoder">
     *         <includeThreadInfo>true</includeThreadInfo>
     *     </encoder>
     * }</pre>
     *
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
     *     <encoder class="software.amazon.lambda.powertools.logging.logback.LambdaJsonEncoder">
     *         <includePowertoolsInfo>false</includePowertoolsInfo>
     *     </encoder>
     * }</pre>
     *
     * @param includePowertoolsInfo if function information should be logged
     */
    public void setIncludePowertoolsInfo(boolean includePowertoolsInfo) {
        this.includePowertoolsInfo = includePowertoolsInfo;
    }

    private void serializeException(JsonSerializer serializer, String className, String message, String stackTrace) {
        Map<Object, Object> map = new HashMap<>();
        map.put(EXCEPTION_MSG_ATTR_NAME, message);
        map.put(EXCEPTION_CLASS_ATTR_NAME, className);
        map.put(EXCEPTION_STACK_ATTR_NAME, stackTrace);
        serializer.writeRaw(',');
        serializer.writeObjectField(EXCEPTION_ATTR_NAME, map);
    }
}

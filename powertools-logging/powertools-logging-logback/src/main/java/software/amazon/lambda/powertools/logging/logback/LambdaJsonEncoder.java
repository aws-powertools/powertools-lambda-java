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
import static software.amazon.lambda.powertools.logging.LoggingUtils.LOG_MESSAGES_AS_JSON;

import ch.qos.logback.classic.pattern.ThrowableHandlingConverter;
import ch.qos.logback.classic.pattern.ThrowableProxyConverter;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.classic.spi.IThrowableProxy;
import ch.qos.logback.classic.spi.ThrowableProxy;
import ch.qos.logback.core.encoder.EncoderBase;
import com.fasterxml.jackson.core.JsonGenerator;
import java.io.IOException;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.TimeZone;
import java.util.TreeMap;
import software.amazon.lambda.powertools.logging.argument.StructuredArgument;
import software.amazon.lambda.powertools.logging.internal.PowertoolsLoggedFields;
import software.amazon.lambda.powertools.logging.internal.StringBuilderJsonGenerator;
import software.amazon.lambda.powertools.logging.logback.internal.LambdaJsonSerializer;

/**
 * Custom encoder for logback that encodes logs in JSON format.
 * It does not use a JSON library but a custom serializer ({@link LambdaJsonSerializer})
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
    private boolean logMessagesAsJsonGlobal;

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
        StringBuilder builder = new StringBuilder();
        try (StringBuilderJsonGenerator generator = new StringBuilderJsonGenerator(builder)) {
            generator.writeStartObject();
            generator.writeStringField(LEVEL_ATTR_NAME, event.getLevel().toString());
            generator.writeRaw(',');
            generator.writeStringField(FORMATTED_MESSAGE_ATTR_NAME, event.getFormattedMessage());

            IThrowableProxy throwableProxy = event.getThrowableProxy();
            if (throwableProxy != null) {
                if (throwableConverter != null) {
                    serializeException(generator, throwableProxy.getClassName(), throwableProxy.getMessage(), throwableConverter.convert(event));
                } else if (throwableProxy instanceof ThrowableProxy) {
                    Throwable throwable = ((ThrowableProxy) throwableProxy).getThrowable();
                    serializeException(generator, throwable.getClass().getName(), throwable.getMessage(),
                            Arrays.toString(throwable.getStackTrace()));
                } else {
                    serializeException(generator, throwableProxy.getClassName(), throwableProxy.getMessage(), throwableProxyConverter.convert(event));
                }
            }

            TreeMap<String, String> sortedMap = new TreeMap<>(event.getMDCPropertyMap());
            if (includePowertoolsInfo) {
                for (Map.Entry<String, String> entry : sortedMap.entrySet()) {
                    if (PowertoolsLoggedFields.stringValues().contains(entry.getKey())
                        && !(entry.getKey().equals(PowertoolsLoggedFields.SAMPLING_RATE.getName()) && entry.getValue().equals("0.0"))) {
                        serializeMDCEntry(entry, generator);
                    }
                }
            }

            // log other MDC values
            for (Map.Entry<String, String> entry : sortedMap.entrySet()) {
                if (!PowertoolsLoggedFields.stringValues().contains(entry.getKey()) && !LOG_MESSAGES_AS_JSON.equals(entry.getKey())) {
                    serializeMDCEntry(entry, generator);
                }
            }

            // log structured arguments
            Object[] arguments = event.getArgumentArray();
            if (arguments != null) {
                for (Object argument : arguments) {
                    if (argument instanceof StructuredArgument) {
                        generator.writeRaw(',');
                        ((StructuredArgument) argument).writeTo(generator);
                    }
                }
            }

            if (includeThreadInfo) {
                if (event.getThreadName() != null) {
                    generator.writeRaw(',');
                    generator.writeStringField(THREAD_ATTR_NAME, event.getThreadName());
                }
                generator.writeRaw(',');
                generator.writeNumberField(THREAD_ID_ATTR_NAME, Thread.currentThread().getId());
                generator.writeRaw(',');
                generator.writeNumberField(THREAD_PRIORITY_ATTR_NAME, Thread.currentThread().getPriority());
            }

            serializeTimestamp(generator, event.getTimeStamp());

            generator.writeEndObject();
        } catch (IOException e) {
            System.err.printf("Failed to encode log event, error: %s.%n", e.getMessage());
        }
        return builder.toString().getBytes(UTF_8);
    }

    @Override
    public byte[] footerBytes() {
        return null;
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

    /**
     * Specify if messages should be logged as JSON, without escaping string (default is <b>false</b>):
     * <br/>
     * <pre>{@code
     *     <encoder class="software.amazon.lambda.powertools.logging.logback.LambdaJsonEncoder">
     *         <logMessagesAsJson>true</logMessagesAsJson>
     *     </encoder>
     * }</pre>
     *
     * @param logMessagesAsJson if messages should be looged as JSON (non escaped quotes)
     */
    public void setLogMessagesAsJson(boolean logMessagesAsJson) {
        this.logMessagesAsJsonGlobal = logMessagesAsJson;
    }

    private void serializeException(JsonGenerator generator, String className, String message, String stackTrace)
            throws IOException {
        Map<Object, Object> map = new HashMap<>();
        map.put(EXCEPTION_MSG_ATTR_NAME, message);
        map.put(EXCEPTION_CLASS_ATTR_NAME, className);
        map.put(EXCEPTION_STACK_ATTR_NAME, stackTrace);
        generator.writeRaw(',');
        generator.writeObjectField(EXCEPTION_ATTR_NAME, map);
    }

    private void serializeTimestamp(JsonGenerator generator, long timestamp) throws IOException {
        String formattedTimestamp;
        if (timestampFormat == null || timestamp < 0) {
            formattedTimestamp = String.valueOf(timestamp);
        } else {
            Date date = new Date(timestamp);
            DateFormat format = new SimpleDateFormat(timestampFormat);

            if (timestampFormatTimezoneId != null) {
                TimeZone tz = TimeZone.getTimeZone(timestampFormatTimezoneId);
                format.setTimeZone(tz);
            }
            formattedTimestamp = format.format(date);
        }
        generator.writeRaw(',');
        generator.writeStringField(TIMESTAMP_ATTR_NAME, formattedTimestamp);
    }

    private void serializeMDCEntry(Map.Entry<String, String> entry, JsonGenerator generator) throws IOException {
        generator.writeRaw(',');
        generator.writeFieldName(entry.getKey());
        if (isString(entry.getValue())) {
            generator.writeString(entry.getValue());
        } else {
            generator.writeRawValue(entry.getValue());
        }
    }

    /**
     * As MDC is a {@code Map<String, String>}, we need to check the type
     * to output numbers and booleans correctly (without quotes)
     */
    private boolean isString(String str) {
        if (str == null) {
            return true;
        }
        if ("true".equals(str) || "false".equals(str)) {
            return false; // boolean
        }
        return !isNumeric(str); // number
    }

    /**
     * Taken from commons-lang3 NumberUtils to avoid include the library
     */
    private boolean isNumeric(final String str) {
        if (str == null || str.isEmpty()) {
            return false;
        }
        if (str.charAt(str.length() - 1) == '.') {
            return false;
        }
        if (str.charAt(0) == '-') {
            if (str.length() == 1) {
                return false;
            }
            return withDecimalsParsing(str, 1);
        }
        return withDecimalsParsing(str, 0);
    }

    /**
     * Taken from commons-lang3 NumberUtils
     */
    private boolean withDecimalsParsing(final String str, final int beginIdx) {
        int decimalPoints = 0;
        for (int i = beginIdx; i < str.length(); i++) {
            final boolean isDecimalPoint = str.charAt(i) == '.';
            if (isDecimalPoint) {
                decimalPoints++;
            }
            if (decimalPoints > 1) {
                return false;
            }
            if (!isDecimalPoint && !Character.isDigit(str.charAt(i))) {
                return false;
            }
        }
        return true;
    }
}

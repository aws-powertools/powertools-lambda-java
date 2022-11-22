package software.amazon.lambda.powertools.logging;

import ch.qos.logback.classic.pattern.ThrowableHandlingConverter;
import ch.qos.logback.classic.pattern.ThrowableProxyConverter;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.classic.spi.IThrowableProxy;
import ch.qos.logback.classic.spi.ThrowableProxy;
import ch.qos.logback.core.LayoutBase;

/**
 * Custom layout for logback that encodes logs in JSON format.
 * It does not use a JSON library but a custom serializer ({@link LambdaJsonSerializer}) to reduce the weight of the library.
 */
public class LambdaJsonLayout extends LayoutBase<ILoggingEvent> {
    private final static String CONTENT_TYPE = "application/json";
    private final ThrowableProxyConverter throwableProxyConverter = new ThrowableProxyConverter();
    private ThrowableHandlingConverter throwableConverter;
    private String timestampFormat;
    private String timestampFormatTimezoneId;
    private boolean includeThreadInfo;

    @Override
    public String doLayout(ILoggingEvent event) {
        StringBuilder builder = new StringBuilder(256);
        LambdaJsonSerializer.serializeObjectStart(builder);
        LambdaJsonSerializer.serializeLogLevel(builder, event.getLevel());
        LambdaJsonSerializer.serializeFormattedMessage(builder, event.getFormattedMessage());
        IThrowableProxy throwableProxy = event.getThrowableProxy();
        if (throwableProxy != null) {
            if (throwableConverter != null) {
                LambdaJsonSerializer.serializeException(builder, throwableProxy.getClassName(), throwableProxy.getMessage(), throwableConverter.convert(event), throwableProxy.getStackTraceElementProxyArray()[0].toString());
            } else if (throwableProxy instanceof ThrowableProxy) {
                LambdaJsonSerializer.serializeException(builder, ((ThrowableProxy) throwableProxy).getThrowable());
            } else {
                LambdaJsonSerializer.serializeException(builder, throwableProxy.getClassName(), throwableProxy.getMessage(), throwableProxyConverter.convert(event), throwableProxy.getStackTraceElementProxyArray()[0].toString());
            }
        }
        LambdaJsonSerializer.serializePowertools(builder, event.getMDCPropertyMap());
        if (includeThreadInfo) {
            LambdaJsonSerializer.serializeThreadName(builder, event.getThreadName());
            LambdaJsonSerializer.serializeThreadId(builder, String.valueOf(Thread.currentThread().getId()));
            LambdaJsonSerializer.serializeThreadPriority(builder, String.valueOf(Thread.currentThread().getPriority()));
        }
        LambdaJsonSerializer.serializeTimestamp(builder, event.getTimeStamp(), timestampFormat, timestampFormatTimezoneId);
        LambdaJsonSerializer.serializeObjectEnd(builder);
        return builder.toString();
    }

    @Override
    public String getContentType() {
        return CONTENT_TYPE;
    }

    public void setTimestampFormat(String timestampFormat) {
        this.timestampFormat = timestampFormat;
    }

    public void setTimestampFormatTimezoneId(String timestampFormatTimezoneId) {
        this.timestampFormatTimezoneId = timestampFormatTimezoneId;
    }

    public void setThrowableConverter(ThrowableHandlingConverter throwableConverter) {
        this.throwableConverter = throwableConverter;
    }

//    public static final String INSTANT_ATTR_NAME = "instant";
//    public static final String EPOCH_SEC_ATTR_NAME = "epochSecond";
//    public static final String NANO_SEC_ATTR_NAME = "nanoOfSecond";
//    public static final String LOGGER_FQCN_ATTR_NAME = "loggerFqcn";
//    public static final String LOGGER_ATTR_NAME = "loggerName";
//    public static final String THREAD_ID_ATTR_NAME = "threadId";
//    public static final String THREAD_PRIORITY_ATTR_NAME = "threadPriority";
//
//    private boolean includePowertools;
//    private boolean includeInstant;
//    private boolean includeThreadInfo;
//
//    public LambdaJsonLayout() {
//        super();
//        this.includeInstant = true;
//        this.includePowertools = true;
//        this.includeThreadInfo = true;
//    }
//
//    @Override
//    protected Map<String, Object> toJsonMap(ILoggingEvent event) {
//        Map<String, Object> map = new LinkedHashMap<>();
//        addTimestamp(TIMESTAMP_ATTR_NAME, this.includeTimestamp, event.getTimeStamp(), map);
//        addInstant(this.includeInstant, event.getTimeStamp(), event.getNanoseconds(), map);
//        add(THREAD_ATTR_NAME, this.includeThreadName || this.includeThreadInfo, event.getThreadName(), map);
//        add(LEVEL_ATTR_NAME, this.includeLevel, String.valueOf(event.getLevel()), map);
//        add(LOGGER_ATTR_NAME, this.includeLoggerName, event.getLoggerName(), map);
//        add(FORMATTED_MESSAGE_ATTR_NAME, this.includeFormattedMessage, event.getFormattedMessage(), map);
//        addThrowableInfo(EXCEPTION_ATTR_NAME, this.includeException, event, map);
//        // contextStack ?
//        // endOfBatch ?
//        map.put(LOGGER_FQCN_ATTR_NAME, "ch.qos.logback.classic.Logger");
//        add(THREAD_ID_ATTR_NAME, this.includeThreadInfo, String.valueOf(Thread.currentThread().getId()), map);
//        add(THREAD_PRIORITY_ATTR_NAME, this.includeThreadInfo, String.valueOf(Thread.currentThread().getPriority()), map);
//        addPowertools(this.includePowertools, event.getMDCPropertyMap(), map);
//        return map;
//    }
//
//    private void addPowertools(boolean includePowertools, Map<String, String> mdcPropertyMap, Map<String, Object> map) {
//        TreeMap<String, String> sortedMap = new TreeMap<>(mdcPropertyMap);
//        List<String> powertoolsFields = DefaultLambdaFields.stringValues();
//
//        sortedMap.forEach((k, v) -> {
//            if (includePowertools || !powertoolsFields.contains(k)) {
//                map.put(k, v);
//            }
//        });
//
//    }
//
//    private void addInstant(boolean includeInstant, long timeStamp, int nanoseconds, Map<String, Object> map) {
//        if (includeInstant) {
//            Map<String, Object> instantMap = new LinkedHashMap<>();
//            instantMap.put(EPOCH_SEC_ATTR_NAME, timeStamp / 1000);
//            instantMap.put(NANO_SEC_ATTR_NAME, nanoseconds);
//            map.put(LambdaJsonLayout.INSTANT_ATTR_NAME, instantMap);
//        }
//    }
//
//    public void setIncludeInstant(boolean includeInstant) {
//        this.includeInstant = includeInstant;
//    }
//
//    public void setIncludePowertools(boolean includePowertools) {
//        this.includePowertools = includePowertools;
//    }
//
    public void setIncludeThreadInfo(boolean includeThreadInfo) {
        this.includeThreadInfo = includeThreadInfo;
    }

}

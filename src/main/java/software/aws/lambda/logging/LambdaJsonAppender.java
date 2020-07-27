package software.aws.lambda.logging;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.LambdaRuntime;
import com.amazonaws.services.lambda.runtime.LambdaRuntimeInternal;
import com.amazonaws.services.lambda.runtime.LambdaLogger;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.logging.log4j.core.Filter;
import org.apache.logging.log4j.core.Layout;
import org.apache.logging.log4j.core.LogEvent;
import org.apache.logging.log4j.core.appender.AbstractAppender;
import org.apache.logging.log4j.core.config.plugins.Plugin;
import org.apache.logging.log4j.core.config.plugins.PluginBuilderFactory;

import java.io.Serializable;

@Plugin(name = LambdaJsonAppender.PLUGIN_NAME, category = LambdaJsonAppender.PLUGIN_CATEGORY,
        elementType = LambdaJsonAppender.PLUGIN_TYPE, printObject = true)
public class LambdaJsonAppender extends AbstractAppender {

    private static final String serviceName = System.getProperty("POWERTOOLS_SERVICE_NAME", "service_undefined");

    public static final String PLUGIN_NAME = "LambdaJsonAppender";
    public static final String PLUGIN_CATEGORY = "Core";
    public static final String PLUGIN_TYPE = "appender";
    private static String functionName;
    private static String functionVersion;
    private static String functionArn;
    private static String functionRequestId;
    private static int functionMemorySize;

    private final LambdaLogger logger = LambdaRuntime.getLogger();
    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    /**
     * Builder class that follows log4j2 plugin convention
     * @param <B> Generic Builder class
     */
    public static class Builder<B extends Builder<B>> extends AbstractAppender.Builder<B>
            implements org.apache.logging.log4j.core.util.Builder<LambdaJsonAppender> {

        /**
         * creates a new LambdaAppender
         * @return a new LambdaAppender
         */
        public LambdaJsonAppender build() {
            return new LambdaJsonAppender(super.getName(), super.getFilter(), super.getOrCreateLayout(),
                    super.isIgnoreExceptions());
        }
    }

    /**
     * Method used by log4j2 to access this appender
     * @param <B> Generic Builder class
     * @return LambdaAppender Builder
     */
    @PluginBuilderFactory
    public static <B extends Builder<B>> B newBuilder() {
        return new Builder<B>().asBuilder();
    }

    /**
     * Constructor method following AbstractAppender convention
     * @param name name of appender
     * @param filter filter specified in xml
     * @param layout layout specified in xml
     * @param ignoreExceptions whether to show exceptions or not specified in xml
     */
    private LambdaJsonAppender(String name, Filter filter, Layout<? extends Serializable> layout, boolean ignoreExceptions) {
        super(name, filter, layout, ignoreExceptions);
        LambdaRuntimeInternal.setUseLog4jAppender(true);
    }

    /**
     * Append log event to System.out
     * @param event log4j event
     */
    public void append(LogEvent event) {
        LogEntry logEntry = LogEntry.builder()
                .level(event.getLevel().name())
                .message(event.getMessage().getFormattedMessage())
                .timestamp(event.getInstant().toString())
                .functionName(functionName)
                .functionArn(functionArn)
                .functionRequestId(functionRequestId)
                .functionVersion(functionVersion)
                .functionMemorySize(functionMemorySize)
                .service(serviceName)
                .build();

        String eventAsString = null;
        try {
            eventAsString = OBJECT_MAPPER.writeValueAsString(logEntry);
        } catch (JsonProcessingException e) {
            throw new PowerToolsClientException(e.getMessage());
        }

        this.logger.log(eventAsString);
    }

    public static void loadContextKeys(Context context) {
        functionName = context.getFunctionName();
        functionVersion = context.getFunctionVersion();
        functionArn = context.getInvokedFunctionArn();
        functionRequestId = context.getAwsRequestId();
        functionMemorySize = context.getMemoryLimitInMB();
    }
}
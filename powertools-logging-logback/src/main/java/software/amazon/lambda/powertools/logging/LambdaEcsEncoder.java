package software.amazon.lambda.powertools.logging;

import ch.qos.logback.classic.pattern.ThrowableHandlingConverter;
import ch.qos.logback.classic.pattern.ThrowableProxyConverter;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.classic.spi.IThrowableProxy;
import ch.qos.logback.classic.spi.ThrowableProxy;
import ch.qos.logback.core.encoder.EncoderBase;
import software.amazon.lambda.powertools.core.internal.LambdaHandlerProcessor;
import software.amazon.lambda.powertools.logging.internal.LambdaEcsSerializer;

import java.util.Map;

import static java.nio.charset.StandardCharsets.UTF_8;
import static software.amazon.lambda.powertools.logging.internal.PowertoolsLoggedFields.*;

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
        LambdaEcsSerializer.serializeServiceName(builder, LambdaHandlerProcessor.serviceName());
        LambdaEcsSerializer.serializeServiceVersion(builder, mdcPropertyMap.get(FUNCTION_VERSION.getName()));
        // TODO : Environment ?
        LambdaEcsSerializer.serializeEventDataset(builder, LambdaHandlerProcessor.serviceName());
        LambdaEcsSerializer.serializeThreadName(builder, event.getThreadName());
        LambdaEcsSerializer.serializeLoggerName(builder, event.getLoggerName());
        IThrowableProxy throwableProxy = event.getThrowableProxy();
        if (throwableProxy != null) {
            if (throwableConverter != null) {
                LambdaEcsSerializer.serializeException(builder, throwableProxy.getClassName(), throwableProxy.getMessage(), throwableConverter.convert(event));
            } else if (throwableProxy instanceof ThrowableProxy) {
                LambdaEcsSerializer.serializeException(builder, ((ThrowableProxy) throwableProxy).getThrowable());
            } else {
                LambdaEcsSerializer.serializeException(builder, throwableProxy.getClassName(), throwableProxy.getMessage(), throwableProxyConverter.convert(event));
            }
        }
        LambdaEcsSerializer.serializeCloudProvider(builder, CLOUD_PROVIDER);
        LambdaEcsSerializer.serializeCloudService(builder, CLOUD_SERVICE);
        String arn = mdcPropertyMap.get(FUNCTION_ARN.getName());
        if (arn != null) {
            String[] arnParts = arn.split(":");
            LambdaEcsSerializer.serializeCloudRegion(builder, arnParts[3]);
            LambdaEcsSerializer.serializeCloudAccountId(builder, arnParts[4]);
        }
        LambdaEcsSerializer.serializeFunctionId(builder, arn);
        LambdaEcsSerializer.serializeFunctionName(builder, mdcPropertyMap.get(FUNCTION_NAME.getName()));
        LambdaEcsSerializer.serializeFunctionVersion(builder, mdcPropertyMap.get(FUNCTION_VERSION.getName()));
        LambdaEcsSerializer.serializeFunctionMemory(builder, mdcPropertyMap.get(FUNCTION_MEMORY_SIZE.getName()));
        LambdaEcsSerializer.serializeFunctionExecutionId(builder, mdcPropertyMap.get(FUNCTION_REQUEST_ID.getName()));
        LambdaEcsSerializer.serializeColdStart(builder, mdcPropertyMap.get(FUNCTION_COLD_START.getName()));
        LambdaEcsSerializer.serializeAdditionalFields(builder, event.getMDCPropertyMap());
        LambdaEcsSerializer.serializeTraceId(builder, mdcPropertyMap.get(FUNCTION_TRACE_ID.getName()));
        LambdaEcsSerializer.serializeObjectEnd(builder);
        return builder.toString().getBytes(UTF_8);
    }

    @Override
    public byte[] footerBytes() {
        return null;
    }

    public void setThrowableConverter(ThrowableHandlingConverter throwableConverter) {
        this.throwableConverter = throwableConverter;
    }
}

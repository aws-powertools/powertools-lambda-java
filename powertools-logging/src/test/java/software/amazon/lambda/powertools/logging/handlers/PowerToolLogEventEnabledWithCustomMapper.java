package software.amazon.lambda.powertools.logging.handlers;

import java.io.IOException;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestHandler;
import com.amazonaws.services.lambda.runtime.events.models.s3.S3EventNotification;
import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.module.SimpleModule;
import com.fasterxml.jackson.databind.ser.std.StdSerializer;
import software.amazon.lambda.powertools.logging.Logging;
import software.amazon.lambda.powertools.logging.LoggingUtils;

public class PowerToolLogEventEnabledWithCustomMapper implements RequestHandler<S3EventNotification, Object> {

    static {
        ObjectMapper objectMapper = new ObjectMapper();
        SimpleModule module = new SimpleModule();
        module.addSerializer(S3EventNotification.class, new S3EventNotificationSerializer());
        objectMapper.registerModule(module);
        LoggingUtils.defaultObjectMapper(objectMapper);
    }

    @Logging(logEvent = true)
    @Override
    public Object handleRequest(S3EventNotification input, Context context) {
        return null;
    }

    static class S3EventNotificationSerializer extends StdSerializer<S3EventNotification> {

        public S3EventNotificationSerializer() {
            this(null);
        }

        public S3EventNotificationSerializer(Class<S3EventNotification> t) {
            super(t);
        }

        @Override
        public void serialize(S3EventNotification o, JsonGenerator jsonGenerator, SerializerProvider serializerProvider) throws IOException {
            jsonGenerator.writeStartObject();
            jsonGenerator.writeStringField("eventSource", o.getRecords().get(0).getEventSource());
            jsonGenerator.writeEndObject();
        }
    }
}

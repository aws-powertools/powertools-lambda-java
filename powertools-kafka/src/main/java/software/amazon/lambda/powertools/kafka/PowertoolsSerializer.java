package software.amazon.lambda.powertools.kafka;

import java.io.InputStream;
import java.io.OutputStream;
import java.lang.reflect.Type;
import java.util.Map;

import com.amazonaws.services.lambda.runtime.CustomPojoSerializer;
import com.amazonaws.services.lambda.runtime.serialization.factories.JacksonFactory;

import software.amazon.lambda.powertools.kafka.internal.DeserializationUtils;
import software.amazon.lambda.powertools.kafka.serializers.KafkaAvroDeserializer;
import software.amazon.lambda.powertools.kafka.serializers.KafkaJsonDeserializer;
import software.amazon.lambda.powertools.kafka.serializers.LambdaDefaultDeserializer;
import software.amazon.lambda.powertools.kafka.serializers.PowertoolsDeserializer;

/**
 * Custom Lambda serializer supporting Kafka events. It delegates to the appropriate deserializer based on the
 * deserialization type specified by {@link software.amazon.lambda.powertools.kafka.Deserialization} annotation.
 * 
 * Kafka serializers need to be specified explicitly, otherwise, the default Lambda serializer from
 * {@link com.amazonaws.services.lambda.runtime.serialization.factories.JacksonFactory} will be used.
 */
public class PowertoolsSerializer implements CustomPojoSerializer {
    private static final Map<DeserializationType, PowertoolsDeserializer> DESERIALIZERS = Map.of(
            DeserializationType.KAFKA_JSON, new KafkaJsonDeserializer(),
            DeserializationType.KAFKA_AVRO, new KafkaAvroDeserializer(),
            DeserializationType.LAMBDA_DEFAULT, new LambdaDefaultDeserializer());

    private final PowertoolsDeserializer deserializer;

    public PowertoolsSerializer() {
        this.deserializer = DESERIALIZERS.getOrDefault(
                DeserializationUtils.determineDeserializationType(),
                new LambdaDefaultDeserializer());
    }

    @Override
    public <T> T fromJson(InputStream input, Type type) {
        return deserializer.fromJson(input, type);
    }

    @Override
    public <T> T fromJson(String input, Type type) {
        return deserializer.fromJson(input, type);
    }

    @Override
    public <T> void toJson(T value, OutputStream output, Type type) {
        // This is the Lambda default Output serialization
        JacksonFactory.getInstance().getSerializer(type).toJson(value, output);
    }
}

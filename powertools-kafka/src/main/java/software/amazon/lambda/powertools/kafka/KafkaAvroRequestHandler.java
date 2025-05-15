package software.amazon.lambda.powertools.kafka;

import java.io.IOException;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Base64;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.avro.io.DatumReader;
import org.apache.avro.io.Decoder;
import org.apache.avro.io.DecoderFactory;
import org.apache.avro.specific.SpecificDatumReader;
import org.apache.avro.specific.SpecificRecordBase;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.apache.kafka.common.TopicPartition;
import org.apache.kafka.common.header.Headers;
import org.apache.kafka.common.header.internals.RecordHeaders;
import org.apache.kafka.common.record.TimestampType;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestHandler;
import com.amazonaws.services.lambda.runtime.events.KafkaEvent;

/**
 * A request handler for processing Kafka events with Avro-encoded data.
 * 
 * @param <K> The type of the key in the Kafka record
 * @param <V> The type of the value in the Kafka record
 * @param <R> The return type of the handler
 */
public abstract class KafkaAvroRequestHandler<K, V, R> implements RequestHandler<KafkaEvent, R> {
    private final Class<K> keyType;
    private final Class<V> valueType;

    @SuppressWarnings("unchecked")
    protected KafkaAvroRequestHandler() {
        Type superClass = getClass().getGenericSuperclass();
        ParameterizedType parameterizedType = (ParameterizedType) superClass;
        Type[] typeArguments = parameterizedType.getActualTypeArguments();
        this.keyType = (Class<K>) typeArguments[0];
        this.valueType = (Class<V>) typeArguments[1];
    }

    @Override
    public R handleRequest(KafkaEvent input, Context context) {
        if (input == null || input.getRecords() == null) {
            return handleRecords(ConsumerRecords.empty(), context);
        }

        Map<TopicPartition, List<ConsumerRecord<K, V>>> recordsMap = new HashMap<>();

        for (Map.Entry<String, List<KafkaEvent.KafkaEventRecord>> entry : input.getRecords().entrySet()) {
            String topic = entry.getKey();

            for (KafkaEvent.KafkaEventRecord record : entry.getValue()) {
                ConsumerRecord<K, V> consumerRecord = convertToConsumerRecord(topic, record);

                TopicPartition topicPartition = new TopicPartition(topic, record.getPartition());
                recordsMap.computeIfAbsent(topicPartition, k -> new ArrayList<>()).add(consumerRecord);
            }
        }

        return handleRecords(new ConsumerRecords<>(recordsMap), context);
    }

    private ConsumerRecord<K, V> convertToConsumerRecord(String topic, KafkaEvent.KafkaEventRecord record) {
        K key = null;
        V value = null;
        int keySize = ConsumerRecord.NULL_SIZE;
        int valueSize = ConsumerRecord.NULL_SIZE;

        if (record.getKey() != null) {
            try {
                byte[] decodedKeyBytes = Base64.getDecoder().decode(record.getKey());
                keySize = decodedKeyBytes.length;
                key = deserialize(decodedKeyBytes, keyType);
            } catch (Exception e) {
                throw new RuntimeException("Failed to deserialize Kafka record key.", e);
            }
        }

        if (record.getValue() != null) {
            try {
                byte[] decodedValueBytes = Base64.getDecoder().decode(record.getValue());
                valueSize = decodedValueBytes.length;
                value = deserialize(decodedValueBytes, valueType);
            } catch (Exception e) {
                throw new RuntimeException("Failed to deserialize Kafka record value.", e);
            }
        }

        Headers headers = new RecordHeaders();
        if (record.getHeaders() != null) {
            for (Map<String, byte[]> headerMap : record.getHeaders()) {
                for (Map.Entry<String, byte[]> header : headerMap.entrySet()) {
                    if (header.getValue() != null) {
                        headers.add(header.getKey(), header.getValue());
                    }
                }
            }
        }

        return new ConsumerRecord<>(
                topic,
                record.getPartition(),
                record.getOffset(),
                record.getTimestamp(),
                // TODO: Do not hardcode
                TimestampType.CREATE_TIME,
                keySize,
                valueSize,
                key,
                value,
                headers,
                java.util.Optional.empty());
    }

    @SuppressWarnings("unchecked")
    private <T> T deserialize(byte[] data, Class<T> type) throws IOException {
        // Handle primitive types and String
        if (type == String.class) {
            return (T) new String(data);
        } else if (type == Integer.class || type == int.class) {
            return (T) Integer.valueOf(new String(data));
        } else if (type == Long.class || type == long.class) {
            return (T) Long.valueOf(new String(data));
        } else if (type == Double.class || type == double.class) {
            return (T) Double.valueOf(new String(data));
        } else if (type == Float.class || type == float.class) {
            return (T) Float.valueOf(new String(data));
        } else if (type == Boolean.class || type == boolean.class) {
            return (T) Boolean.valueOf(new String(data));
        } else if (type == Byte.class || type == byte.class) {
            return (T) Byte.valueOf(new String(data));
        } else if (type == Short.class || type == short.class) {
            return (T) Short.valueOf(new String(data));
        } else if (type == Character.class || type == char.class) {
            String str = new String(data);
            if (!str.isEmpty()) {
                return (T) Character.valueOf(str.charAt(0));
            }
            throw new IllegalArgumentException("Cannot convert empty string to char");
        } else if (SpecificRecordBase.class.isAssignableFrom(type)) {
            // Handle Avro specific record
            try {
                // Create a datum reader for the Avro record
                DatumReader<T> datumReader = new SpecificDatumReader<>(type);

                // Create a binary decoder for the data
                Decoder decoder = DecoderFactory.get().binaryDecoder(data, null);

                // Read and return the record
                return datumReader.read(null, decoder);
            } catch (Exception e) {
                throw new IOException("Failed to deserialize Avro data", e);
            }
        } else {
            throw new IOException("Unsupported type for Avro deserialization: " + type.getName() + ". "
                    + "Avro deserialization requires a type of org.apache.avro.specific.SpecificRecord. "
                    + "Consider using an alternative Deserializer.");
        }
    }

    /**
     * Handle the Kafka records.
     *
     * @param records ConsumerRecords containing deserialized Kafka ConsumerRecord objects
     * @param context Lambda context
     * @return Response of type R
     */
    public abstract R handleRecords(ConsumerRecords<K, V> records, Context context);
}

package software.amazon.lambda.powertools.kafka;

import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Base64;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.apache.kafka.common.TopicPartition;
import org.apache.kafka.common.header.Headers;
import org.apache.kafka.common.header.internals.RecordHeaders;
import org.apache.kafka.common.record.TimestampType;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestHandler;
import com.amazonaws.services.lambda.runtime.events.KafkaEvent;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

public abstract class KafkaJsonRequestHandler<K, V, R> implements RequestHandler<KafkaEvent, R> {
    private static final ObjectMapper objectMapper = new ObjectMapper();
    private final Class<K> keyType;
    private final Class<V> valueType;

    @SuppressWarnings("unchecked")
    protected KafkaJsonRequestHandler() {
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
                key = deserialize(record.getKey(), keyType);
            } catch (Exception e) {
                throw new RuntimeException("Failed to deserialize Kafka record key.", e);
            }
        }

        if (record.getValue() != null) {
            try {
                byte[] decodedValueBytes = Base64.getDecoder().decode(record.getValue());
                valueSize = decodedValueBytes.length;
                value = deserialize(record.getValue(), valueType);
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
    private <T> T deserialize(String data, Class<T> type) throws JsonProcessingException {
        byte[] decodedBytes = Base64.getDecoder().decode(data);

        // Handle String type
        if (type == String.class) {
            return (T) new String(decodedBytes);
        }

        // Handle primitive types and their wrappers
        String decodedStr = new String(decodedBytes);

        if (type == Integer.class || type == int.class) {
            return (T) Integer.valueOf(decodedStr);
        } else if (type == Long.class || type == long.class) {
            return (T) Long.valueOf(decodedStr);
        } else if (type == Double.class || type == double.class) {
            return (T) Double.valueOf(decodedStr);
        } else if (type == Float.class || type == float.class) {
            return (T) Float.valueOf(decodedStr);
        } else if (type == Boolean.class || type == boolean.class) {
            return (T) Boolean.valueOf(decodedStr);
        } else if (type == Byte.class || type == byte.class) {
            return (T) Byte.valueOf(decodedStr);
        } else if (type == Short.class || type == short.class) {
            return (T) Short.valueOf(decodedStr);
        } else if (type == Character.class || type == char.class) {
            if (decodedStr.length() > 0) {
                return (T) Character.valueOf(decodedStr.charAt(0));
            }
            throw new IllegalArgumentException("Cannot convert empty string to char");
        } else {
            // For all other types, use Jackson ObjectMapper
            return objectMapper.readValue(decodedStr, type);
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

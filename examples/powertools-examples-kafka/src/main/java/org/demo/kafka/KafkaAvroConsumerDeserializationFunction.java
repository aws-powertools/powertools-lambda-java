package org.demo.kafka;

import static software.amazon.lambda.powertools.logging.argument.StructuredArguments.entry;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.demo.kafka.avro.AvroProduct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.amazonaws.services.lambda.runtime.Context;

import software.amazon.cloudwatchlogs.emf.logger.MetricsLogger;
import software.amazon.cloudwatchlogs.emf.model.Unit;
import software.amazon.lambda.powertools.kafka.KafkaAvroRequestHandler;
import software.amazon.lambda.powertools.logging.Logging;
import software.amazon.lambda.powertools.metrics.Metrics;
import software.amazon.lambda.powertools.metrics.MetricsUtils;

public class KafkaAvroConsumerDeserializationFunction extends KafkaAvroRequestHandler<String, AvroProduct, String> {

    private static final Logger LOGGER = LoggerFactory.getLogger(KafkaAvroConsumerDeserializationFunction.class);
    private static final MetricsLogger metrics = MetricsUtils.metricsLogger();

    @Logging
    @Metrics
    public String handleRecords(ConsumerRecords<String, AvroProduct> records, Context context) {
        for (ConsumerRecord<String, AvroProduct> consumerRecord : records) {
            LOGGER.info("{}", consumerRecord, entry("value", avroToMap(consumerRecord.value())));
            metrics.putMetric("ProcessedAvroRecord", 1, Unit.COUNT);
        }

        return "OK";
    }

    // TODO: Helper method because Avro objects cannot be serialized by the Jackson ObjectMapper used in the Logging
    // module
    // entry("value", consumerRecord.value()) would fallback to a string instead of native json object.
    private Map<String, Object> avroToMap(AvroProduct avroProduct) {
        if (avroProduct == null) {
            return Collections.emptyMap();
        }
        Map<String, Object> map = new HashMap<>();
        map.put("id", avroProduct.getId());
        map.put("name", avroProduct.getName());
        map.put("price", avroProduct.getPrice());
        return map;
    }
}

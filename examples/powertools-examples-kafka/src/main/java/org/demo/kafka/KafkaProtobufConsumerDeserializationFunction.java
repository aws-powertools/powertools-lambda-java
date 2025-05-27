package org.demo.kafka;

import static software.amazon.lambda.powertools.logging.argument.StructuredArguments.entry;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.demo.kafka.protobuf.ProtobufProduct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestHandler;

import software.amazon.cloudwatchlogs.emf.logger.MetricsLogger;
import software.amazon.cloudwatchlogs.emf.model.Unit;
import software.amazon.lambda.powertools.kafka.Deserialization;
import software.amazon.lambda.powertools.kafka.DeserializationType;
import software.amazon.lambda.powertools.logging.Logging;
import software.amazon.lambda.powertools.metrics.Metrics;
import software.amazon.lambda.powertools.metrics.MetricsUtils;

public class KafkaProtobufConsumerDeserializationFunction
        implements RequestHandler<ConsumerRecords<String, ProtobufProduct>, String> {

    private static final Logger LOGGER = LoggerFactory.getLogger(KafkaProtobufConsumerDeserializationFunction.class);
    private static final MetricsLogger metrics = MetricsUtils.metricsLogger();

    @Override
    @Logging
    @Metrics
    @Deserialization(type = DeserializationType.KAFKA_PROTOBUF)
    public String handleRequest(ConsumerRecords<String, ProtobufProduct> records, Context context) {
        for (ConsumerRecord<String, ProtobufProduct> consumerRecord : records) {
            LOGGER.info("{}", consumerRecord, entry("value", protobufToMap(consumerRecord.value())));
            metrics.putMetric("ProcessedProtobufRecord", 1, Unit.COUNT);
        }

        return "OK";
    }

    // Protobuf Message objects cannot be serialized to JSON by Jackson Object Mapper used by powertools-logging.
    // We convert to a map first to retrieve a meaningful representation.
    private Map<String, Object> protobufToMap(ProtobufProduct protobufProduct) {
        if (protobufProduct == null) {
            return Collections.emptyMap();
        }
        Map<String, Object> map = new HashMap<>();
        map.put("id", protobufProduct.getId());
        map.put("name", protobufProduct.getName());
        map.put("price", protobufProduct.getPrice());
        return map;
    }
}

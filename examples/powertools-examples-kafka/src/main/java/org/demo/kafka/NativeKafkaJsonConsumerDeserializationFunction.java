package org.demo.kafka;

import static software.amazon.lambda.powertools.logging.argument.StructuredArguments.entry;

import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.consumer.ConsumerRecords;
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

public class NativeKafkaJsonConsumerDeserializationFunction
        implements RequestHandler<ConsumerRecords<String, Product>, String> {

    private static final Logger LOGGER = LoggerFactory.getLogger(NativeKafkaJsonConsumerDeserializationFunction.class);
    private final MetricsLogger metrics = MetricsUtils.metricsLogger();

    @Override
    @Logging
    @Metrics
    @Deserialization(type = DeserializationType.KAFKA_JSON)
    public String handleRequest(ConsumerRecords<String, Product> consumerRecords, Context context) {
        for (ConsumerRecord<String, Product> consumerRecord : consumerRecords) {
            LOGGER.info("{}", consumerRecord, entry("value", consumerRecord.value()));
            metrics.putMetric("ProcessedRecord", 1, Unit.COUNT);
        }

        return "OK";
    }
}

package org.demo.kafka;

import static software.amazon.lambda.powertools.logging.argument.StructuredArguments.entry;

import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.amazonaws.services.lambda.runtime.Context;

import software.amazon.cloudwatchlogs.emf.logger.MetricsLogger;
import software.amazon.cloudwatchlogs.emf.model.Unit;
import software.amazon.lambda.powertools.kafka.KafkaJsonRequestHandler;
import software.amazon.lambda.powertools.logging.Logging;
import software.amazon.lambda.powertools.metrics.Metrics;
import software.amazon.lambda.powertools.metrics.MetricsUtils;

public class KafkaJsonConsumerDeserializationFunction extends KafkaJsonRequestHandler<String, Product, String> {

    private static final Logger LOGGER = LoggerFactory.getLogger(KafkaJsonConsumerDeserializationFunction.class);
    private static final MetricsLogger metrics = MetricsUtils.metricsLogger();

    @Override
    @Logging
    @Metrics
    public String handleRecords(ConsumerRecords<String, Product> records, Context context) {
        for (ConsumerRecord<String, Product> consumerRecord : records) {
            LOGGER.info("{}", consumerRecord, entry("value", consumerRecord.value()));
            metrics.putMetric("ProcessedRecord", 1, Unit.COUNT);
        }

        return "OK";
    }
}

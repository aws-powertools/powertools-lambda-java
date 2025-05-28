package org.demo.kafka.minimal;

import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestHandler;

import software.amazon.lambda.powertools.kafka.Deserialization;
import software.amazon.lambda.powertools.kafka.DeserializationType;

public class KafkaJsonConsumerMinimalFunction
        implements RequestHandler<ConsumerRecords<String, Object>, String> {

    private static final Logger LOGGER = LoggerFactory.getLogger(KafkaJsonConsumerMinimalFunction.class);

    @Override
    @Deserialization(type = DeserializationType.KAFKA_JSON)
    public String handleRequest(ConsumerRecords<String, Object> records, Context context) {
        for (ConsumerRecord<String, Object> consumerRecord : records) {
            LOGGER.info("Received record: {}", consumerRecord);
        }

        return "OK";
    }
}

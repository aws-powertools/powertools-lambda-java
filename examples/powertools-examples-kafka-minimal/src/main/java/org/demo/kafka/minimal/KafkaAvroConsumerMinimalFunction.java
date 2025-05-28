package org.demo.kafka.minimal;

import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.demo.kafka.avro.AvroProduct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestHandler;

import software.amazon.lambda.powertools.kafka.Deserialization;
import software.amazon.lambda.powertools.kafka.DeserializationType;

public class KafkaAvroConsumerMinimalFunction
        implements RequestHandler<ConsumerRecords<String, AvroProduct>, String> {

    private static final Logger LOGGER = LoggerFactory.getLogger(KafkaAvroConsumerMinimalFunction.class);

    @Override
    @Deserialization(type = DeserializationType.KAFKA_AVRO)
    public String handleRequest(ConsumerRecords<String, AvroProduct> records, Context context) {
        for (ConsumerRecord<String, AvroProduct> consumerRecord : records) {
            LOGGER.info("Received record: {}", consumerRecord);
        }

        return "OK";
    }
}

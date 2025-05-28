package org.demo.kafka.minimal;

import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.demo.kafka.protobuf.ProtobufProduct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestHandler;

import software.amazon.lambda.powertools.kafka.Deserialization;
import software.amazon.lambda.powertools.kafka.DeserializationType;

public class KafkaProtobufConsumerMinimalFunction
        implements RequestHandler<ConsumerRecords<String, ProtobufProduct>, String> {

    private static final Logger LOGGER = LoggerFactory.getLogger(KafkaProtobufConsumerMinimalFunction.class);

    @Override
    @Deserialization(type = DeserializationType.KAFKA_PROTOBUF)
    public String handleRequest(ConsumerRecords<String, ProtobufProduct> records, Context context) {
        for (ConsumerRecord<String, ProtobufProduct> consumerRecord : records) {
            LOGGER.info("Received record: {}", consumerRecord);
        }

        return "OK";
    }
}

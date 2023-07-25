package org.demo.batch.kinesis;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestHandler;
import com.amazonaws.services.lambda.runtime.events.ScheduledEvent;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.demo.batch.model.Product;
import software.amazon.awssdk.core.BytesWrapper;
import software.amazon.awssdk.core.SdkBytes;
import software.amazon.awssdk.http.urlconnection.UrlConnectionHttpClient;
import software.amazon.awssdk.services.kinesis.KinesisClient;
import software.amazon.awssdk.services.kinesis.model.PutRecordsRequest;
import software.amazon.awssdk.services.kinesis.model.PutRecordsRequestEntry;
import software.amazon.awssdk.services.kinesis.model.PutRecordsResponse;
import software.amazon.awssdk.services.sqs.SqsClient;
import software.amazon.awssdk.services.sqs.model.SendMessageBatchRequest;
import software.amazon.awssdk.services.sqs.model.SendMessageBatchRequestEntry;
import software.amazon.awssdk.services.sqs.model.SendMessageBatchResponse;

import java.security.SecureRandom;
import java.util.List;
import java.util.stream.IntStream;

import static java.util.stream.Collectors.toList;

public class KinesisBatchSender implements RequestHandler<ScheduledEvent, String> {

    private static final Logger LOGGER = LogManager.getLogger(KinesisBatchSender.class);

    private final KinesisClient kinesisClient;
    private final SecureRandom random;
    private final ObjectMapper objectMapper;

    public KinesisBatchSender() {
        kinesisClient = KinesisClient.builder()
                .httpClient(UrlConnectionHttpClient.create())
                .build();
        random = new SecureRandom();
        objectMapper = new ObjectMapper();
    }

    @Override
    public String handleRequest(ScheduledEvent scheduledEvent, Context context) {
        String streamName = System.getenv("STREAM_NAME");

        LOGGER.info("handleRequest");

        // Push 5 messages on each invoke.
        List<PutRecordsRequestEntry> records = IntStream.range(0, 5)
                .mapToObj(value -> {
                    long id = random.nextLong();
                    float price = random.nextFloat();
                    Product product = new Product(id, "product-" + id, price);
                    try {
                        SdkBytes data = SdkBytes.fromUtf8String(objectMapper.writeValueAsString(product));
                        return PutRecordsRequestEntry.builder()
                                .data(data)
                                .build();
                    } catch (JsonProcessingException e) {
                        LOGGER.error("Failed serializing body", e);
                        throw new RuntimeException(e);
                    }
                }).collect(toList());

        PutRecordsResponse putRecordsResponse = kinesisClient.putRecords(PutRecordsRequest.builder()
                .streamName(streamName)
                .records(records)
                .build());

        LOGGER.info("Sent Message {}", putRecordsResponse);

        return "Success";
    }
}

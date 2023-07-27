package org.demo.batch.sqs;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestHandler;
import com.amazonaws.services.lambda.runtime.events.ScheduledEvent;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.demo.batch.model.Product;
import software.amazon.awssdk.http.urlconnection.UrlConnectionHttpClient;
import software.amazon.awssdk.services.sqs.SqsClient;
import software.amazon.awssdk.services.sqs.model.SendMessageBatchRequest;
import software.amazon.awssdk.services.sqs.model.SendMessageBatchRequestEntry;
import software.amazon.awssdk.services.sqs.model.SendMessageBatchResponse;

import java.security.SecureRandom;
import java.util.List;
import java.util.stream.IntStream;

import static java.util.stream.Collectors.toList;


/**
 * A Lambda handler used to send message batches to SQS. This is only here
 * to produce an end-to-end demo, so that the {{@link org.demo.batch.sqs.SqsBatchHandler}}
 * has some data to consume.
 */
public class SqsBatchSender implements RequestHandler<ScheduledEvent, String> {

    private static final Logger LOGGER = LogManager.getLogger(SqsBatchSender.class);

    private final SqsClient sqsClient;
    private final SecureRandom random;
    private final ObjectMapper objectMapper;

    public SqsBatchSender() {
        sqsClient = SqsClient.builder()
                .httpClient(UrlConnectionHttpClient.create())
                .build();
        random = new SecureRandom();
        objectMapper = new ObjectMapper();
    }

    @Override
    public String handleRequest(ScheduledEvent scheduledEvent, Context context) {
        String queueUrl = System.getenv("QUEUE_URL");

        LOGGER.info("handleRequest");

        // Push 5 messages on each invoke.
        List<SendMessageBatchRequestEntry> batchRequestEntries = IntStream.range(0, 5)
                .mapToObj(value -> {
                    long id = random.nextLong();
                    float price = random.nextFloat();
                    Product product = new Product(id, "product-" + id, price);
                    try {

                        return SendMessageBatchRequestEntry.builder()
                                .id(scheduledEvent.getId() + value)
                                .messageBody(objectMapper.writeValueAsString(product))
                                .build();
                    } catch (JsonProcessingException e) {
                        LOGGER.error("Failed serializing body", e);
                        throw new RuntimeException(e);
                    }
                }).collect(toList());

        SendMessageBatchResponse sendMessageBatchResponse = sqsClient.sendMessageBatch(SendMessageBatchRequest.builder()
                .queueUrl(queueUrl)
                .entries(batchRequestEntries)
                .build());

        LOGGER.info("Sent Message {}", sendMessageBatchResponse);

        return "Success";
    }
}

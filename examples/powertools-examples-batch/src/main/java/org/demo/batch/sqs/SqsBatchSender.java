package org.demo.batch.sqs;

import static java.util.stream.Collectors.toList;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestHandler;
import com.amazonaws.services.lambda.runtime.events.ScheduledEvent;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.security.SecureRandom;
import java.util.List;
import java.util.stream.IntStream;
import org.demo.batch.model.Product;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import software.amazon.awssdk.http.urlconnection.UrlConnectionHttpClient;
import software.amazon.awssdk.services.sqs.SqsClient;
import software.amazon.awssdk.services.sqs.model.SendMessageBatchRequest;
import software.amazon.awssdk.services.sqs.model.SendMessageBatchRequestEntry;


/**
 * A Lambda handler used to send message batches to SQS. This is only here
 * to produce an end-to-end demo, so that the {{@link org.demo.batch.sqs.SqsBatchHandler}}
 * has some data to consume.
 */
public class SqsBatchSender implements RequestHandler<ScheduledEvent, String> {

    private static final Logger LOGGER = LoggerFactory.getLogger(SqsBatchSender.class);

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

        List<SendMessageBatchRequestEntry> batchRequestEntries = IntStream.range(0, 50)
                .mapToObj(value -> {
                    long id = Math.abs(random.nextLong());
                    float price = Math.abs(random.nextFloat() * 3465);
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

        for (int i = 0; i < 50; i += 10) {
            sqsClient.sendMessageBatch(SendMessageBatchRequest.builder()
                    .queueUrl(queueUrl)
                    .entries(batchRequestEntries.subList(i, i + 10))
                    .build());
        }

        return "Success";
    }
}

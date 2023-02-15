
package org.demo.sqs;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestHandler;
import com.amazonaws.services.lambda.runtime.events.ScheduledEvent;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.joda.JodaModule;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import software.amazon.awssdk.http.urlconnection.UrlConnectionHttpClient;
import software.amazon.awssdk.services.sqs.SqsClient;
import software.amazon.awssdk.services.sqs.model.MessageAttributeValue;
import software.amazon.awssdk.services.sqs.model.SendMessageBatchRequest;
import software.amazon.awssdk.services.sqs.model.SendMessageBatchRequestEntry;
import software.amazon.awssdk.services.sqs.model.SendMessageBatchResponse;
import software.amazon.lambda.powertools.logging.Logging;
import software.amazon.lambda.powertools.logging.LoggingUtils;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.stream.IntStream;

import static java.util.stream.Collectors.toList;

public class SqsMessageSender implements RequestHandler<ScheduledEvent, String> {

    private static final Logger log = LogManager.getLogger(SqsMessageSender.class);

    private static final SqsClient sqsClient = SqsClient.builder()
            .httpClient(UrlConnectionHttpClient.create())
            .build();

    private static final Random random = new Random();

    private static final ObjectMapper objectMapper;

    static {
        objectMapper = new ObjectMapper();
        objectMapper.registerModule(new JodaModule());
        LoggingUtils.defaultObjectMapper(objectMapper);
    }

    @Logging(logEvent = true)
    public String handleRequest(final ScheduledEvent input, final Context context) {
        String queueUrl = System.getenv("QUEUE_URL");

        // Push 5 messages on each invoke.
        List<SendMessageBatchRequestEntry> batchRequestEntries = IntStream.range(0, 5)
                .mapToObj(value -> {
                    Map<String, MessageAttributeValue> attributeValueHashMap = new HashMap<>();
                    attributeValueHashMap.put("Key" + value, MessageAttributeValue.builder()
                            .dataType("String")
                            .stringValue("Value" + value)
                            .build());

                    byte[] array = new byte[7];
                    random.nextBytes(array);

                    return SendMessageBatchRequestEntry.builder()
                            .messageAttributes(attributeValueHashMap)
                            .id(input.getId() + value)
                            .messageBody("Sample Message " + value)
                            .build();
                }).collect(toList());

        SendMessageBatchResponse sendMessageBatchResponse = sqsClient.sendMessageBatch(SendMessageBatchRequest.builder()
                .queueUrl(queueUrl)
                .entries(batchRequestEntries)
                .build());

        log.info("Sent Message {}", sendMessageBatchResponse);

        return "Success";
    }
}
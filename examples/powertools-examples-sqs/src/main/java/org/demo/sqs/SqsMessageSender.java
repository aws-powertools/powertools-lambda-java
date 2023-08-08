/*
 * Copyright 2023 Amazon.com, Inc. or its affiliates.
 * Licensed under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *     http://www.apache.org/licenses/LICENSE-2.0
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 */

package org.demo.sqs;

import static java.util.stream.Collectors.toList;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestHandler;
import com.amazonaws.services.lambda.runtime.events.ScheduledEvent;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.joda.JodaModule;
import java.security.SecureRandom;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.stream.IntStream;
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

public class SqsMessageSender implements RequestHandler<ScheduledEvent, String> {

    private static final Logger log = LogManager.getLogger(SqsMessageSender.class);

    private static final SqsClient sqsClient = SqsClient.builder()
            .httpClient(UrlConnectionHttpClient.create())
            .build();

    private static final Random random = new SecureRandom();

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
                .mapToObj(value ->
                {
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
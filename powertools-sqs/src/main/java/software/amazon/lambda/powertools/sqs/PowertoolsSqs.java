/*
 * Copyright 2020 Amazon.com, Inc. or its affiliates.
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
package software.amazon.lambda.powertools.sqs;

import java.util.List;
import java.util.function.Function;
import java.util.stream.Collectors;

import com.amazonaws.services.lambda.runtime.events.SQSEvent;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import software.amazon.lambda.powertools.sqs.internal.SqsMessageAspect;
import software.amazon.payloadoffloading.PayloadS3Pointer;

import static com.amazonaws.services.lambda.runtime.events.SQSEvent.SQSMessage;
import static software.amazon.lambda.powertools.sqs.internal.SqsMessageAspect.processMessages;

/**
 * A class of helper functions to add additional functionality to LargeMessageHandler.
 * <p>
 * {@see PowertoolsLogging}
 */
public class PowertoolsSqs {

    private static final ObjectMapper objectMapper = new ObjectMapper();

    /**
     * This is a utility method when you want to avoid using {@code LargeMessageHandler} annotation.
     * Gives you access to enriched messages from S3 in the SQS event produced via extended client lib.
     * If all the large S3 payload are successfully retrieved, it will delete them from S3 post success.
     *
     * @param sqsEvent        Event received from SQS Extended client library
     * @param messageFunction Function to execute you business logic which provides access to enriched messages from S3 when needed.
     * @return Return value from the function.
     */
    public static <R> R enrichedMessageFromS3(final SQSEvent sqsEvent,
                                              final Function<List<SQSMessage>, R> messageFunction) {
        return enrichedMessageFromS3(sqsEvent, true, messageFunction);
    }

    /**
     * This is a utility method when you want to avoid using {@code LargeMessageHandler} annotation.
     * Gives you access to enriched messages from S3 in the SQS event produced via extended client lib.
     * if all the large S3 payload are successfully retrieved, Control if it will delete payload from S3 post success.
     *
     * @param sqsEvent        Event received from SQS Extended client library
     * @param messageFunction Function to execute you business logic which provides access to enriched messages from S3 when needed.
     * @return Return value from the function.
     */
    public static <R> R enrichedMessageFromS3(final SQSEvent sqsEvent,
                                              final boolean deleteS3Payload,
                                              final Function<List<SQSMessage>, R> messageFunction) {

        List<SQSMessage> sqsMessages = sqsEvent.getRecords().stream()
                .map(PowertoolsSqs::clonedMessage)
                .collect(Collectors.toList());

        List<PayloadS3Pointer> s3Pointers = processMessages(sqsMessages);

        R returnValue = messageFunction.apply(sqsMessages);

        if (deleteS3Payload) {
            s3Pointers.forEach(SqsMessageAspect::deleteMessage);
        }

        return returnValue;
    }

    private static SQSMessage clonedMessage(SQSMessage sqsMessage) {
        try {
            return objectMapper
                    .readValue(objectMapper.writeValueAsString(sqsMessage), SQSMessage.class);
        } catch (JsonProcessingException e) {
            throw new RuntimeException(e);
        }
    }
}

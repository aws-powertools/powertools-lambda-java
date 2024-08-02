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

package software.amazon.lambda.powertools.batch;

import static org.assertj.core.api.Assertions.assertThat;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.events.DynamodbEvent;
import com.amazonaws.services.lambda.runtime.events.StreamsEventResponse;
import com.amazonaws.services.lambda.runtime.tests.annotations.Event;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.params.ParameterizedTest;
import org.mockito.Mock;
import software.amazon.lambda.powertools.batch.handler.BatchMessageHandler;

class DdbBatchProcessorTest {

    @Mock
    private Context context;

    private final List<String> threadList = Collections.synchronizedList(new ArrayList<>());

    @AfterEach
    public void clear() {
        threadList.clear();
    }

    private void processMessageSucceeds(DynamodbEvent.DynamodbStreamRecord record, Context context) {
        // Great success
    }

    private void processMessageFailsForFixedMessage(DynamodbEvent.DynamodbStreamRecord record, Context context) {
        if (record.getDynamodb().getSequenceNumber().equals("4421584500000000017450439091")) {
            throw new RuntimeException("fake exception");
        }
    }

    private void processMessageInParallelSucceeds(DynamodbEvent.DynamodbStreamRecord record, Context context) {
        String thread = Thread.currentThread().getName();
        if (!threadList.contains(thread)) {
            threadList.add(thread);
        }
        try {
            Thread.sleep(500); // simulate some processing
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
    }

    private void processMessageInParallelFailsForFixedMessage(DynamodbEvent.DynamodbStreamRecord record, Context context) {
        String thread = Thread.currentThread().getName();
        if (!threadList.contains(thread)) {
            threadList.add(thread);
        }
        try {
            Thread.sleep(500); // simulate some processing
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
        if (record.getDynamodb().getSequenceNumber().equals("4421584500000000017450439091")) {
            throw new RuntimeException("fake exception");
        }
    }

    @ParameterizedTest
    @Event(value = "dynamo_event.json", type = DynamodbEvent.class)
    void batchProcessingSucceedsAndReturns(DynamodbEvent event) {
        // Arrange
        BatchMessageHandler<DynamodbEvent, StreamsEventResponse> handler = new BatchMessageHandlerBuilder()
                .withDynamoDbBatchHandler()
                .buildWithRawMessageHandler(this::processMessageSucceeds);

        // Act
        StreamsEventResponse dynamodbBatchResponse = handler.processBatch(event, context);

        // Assert
        assertThat(dynamodbBatchResponse.getBatchItemFailures()).isEmpty();
    }

    @ParameterizedTest
    @Event(value = "dynamo_event_big.json", type = DynamodbEvent.class)
    void parallelBatchProcessingSucceedsAndReturns(DynamodbEvent event) {
        // Arrange
        BatchMessageHandler<DynamodbEvent, StreamsEventResponse> handler = new BatchMessageHandlerBuilder()
                .withDynamoDbBatchHandler()
                .buildWithRawMessageHandler(this::processMessageInParallelSucceeds);

        // Act
        StreamsEventResponse dynamodbBatchResponse = handler.processBatchInParallel(event, context);

        // Assert
        assertThat(dynamodbBatchResponse.getBatchItemFailures()).isEmpty();
        assertThat(threadList).hasSizeGreaterThan(1);
    }

    @ParameterizedTest
    @Event(value = "dynamo_event.json", type = DynamodbEvent.class)
    void shouldAddMessageToBatchFailure_whenException_withMessage(DynamodbEvent event) {
        // Arrange
        BatchMessageHandler<DynamodbEvent, StreamsEventResponse> handler = new BatchMessageHandlerBuilder()
                .withDynamoDbBatchHandler()
                .buildWithRawMessageHandler(this::processMessageFailsForFixedMessage);

        // Act
        StreamsEventResponse dynamodbBatchResponse = handler.processBatch(event, context);

        // Assert
        assertThat(dynamodbBatchResponse.getBatchItemFailures()).hasSize(1);
        StreamsEventResponse.BatchItemFailure batchItemFailure = dynamodbBatchResponse.getBatchItemFailures().get(0);
        assertThat(batchItemFailure.getItemIdentifier()).isEqualTo("4421584500000000017450439091");
    }

    @ParameterizedTest
    @Event(value = "dynamo_event_big.json", type = DynamodbEvent.class)
    void parallelBatchProcessing_shouldAddMessageToBatchFailure_whenException_withMessage(DynamodbEvent event) {
        // Arrange
        BatchMessageHandler<DynamodbEvent, StreamsEventResponse> handler = new BatchMessageHandlerBuilder()
                .withDynamoDbBatchHandler()
                .buildWithRawMessageHandler(this::processMessageInParallelFailsForFixedMessage);

        // Act
        StreamsEventResponse dynamodbBatchResponse = handler.processBatchInParallel(event, context);

        // Assert
        assertThat(dynamodbBatchResponse.getBatchItemFailures()).hasSize(1);
        StreamsEventResponse.BatchItemFailure batchItemFailure = dynamodbBatchResponse.getBatchItemFailures().get(0);
        assertThat(batchItemFailure.getItemIdentifier()).isEqualTo("4421584500000000017450439091");
        assertThat(threadList).hasSizeGreaterThan(1);
    }

    @ParameterizedTest
    @Event(value = "dynamo_event.json", type = DynamodbEvent.class)
    void failingFailureHandlerShouldntFailBatch(DynamodbEvent event) {
        // Arrange
        AtomicBoolean wasCalledAndFailed = new AtomicBoolean(false);
        BatchMessageHandler<DynamodbEvent, StreamsEventResponse> handler = new BatchMessageHandlerBuilder()
                .withDynamoDbBatchHandler()
                .withFailureHandler((m, e) -> {
                    if (m.getDynamodb().getSequenceNumber().equals("4421584500000000017450439091")) {
                        wasCalledAndFailed.set(true);
                        throw new RuntimeException("Success handler throws");
                    }
                })
                .buildWithRawMessageHandler(this::processMessageFailsForFixedMessage);

        // Act
        StreamsEventResponse dynamodbBatchResponse = handler.processBatch(event, context);

        // Assert
        assertThat(dynamodbBatchResponse).isNotNull();
        assertThat(dynamodbBatchResponse.getBatchItemFailures()).hasSize(1);
        assertThat(wasCalledAndFailed.get()).isTrue();
        StreamsEventResponse.BatchItemFailure batchItemFailure = dynamodbBatchResponse.getBatchItemFailures().get(0);
        assertThat(batchItemFailure.getItemIdentifier()).isEqualTo("4421584500000000017450439091");
    }

    @ParameterizedTest
    @Event(value = "dynamo_event.json", type = DynamodbEvent.class)
    void failingSuccessHandlerShouldntFailBatchButShouldFailMessage(DynamodbEvent event) {
        // Arrange
        AtomicBoolean wasCalledAndFailed = new AtomicBoolean(false);
        BatchMessageHandler<DynamodbEvent, StreamsEventResponse> handler = new BatchMessageHandlerBuilder()
                .withDynamoDbBatchHandler()
                .withSuccessHandler((e) -> {
                    if (e.getDynamodb().getSequenceNumber().equals("4421584500000000017450439091")) {
                        wasCalledAndFailed.set(true);
                        throw new RuntimeException("Success handler throws");
                    }
                })
                .buildWithRawMessageHandler(this::processMessageSucceeds);

        // Act
        StreamsEventResponse dynamodbBatchResponse = handler.processBatch(event, context);

        // Assert
        assertThat(dynamodbBatchResponse).isNotNull();
        assertThat(dynamodbBatchResponse.getBatchItemFailures()).hasSize(1);
        assertThat(wasCalledAndFailed.get()).isTrue();
        StreamsEventResponse.BatchItemFailure batchItemFailure = dynamodbBatchResponse.getBatchItemFailures().get(0);
        assertThat(batchItemFailure.getItemIdentifier()).isEqualTo("4421584500000000017450439091");
    }

}

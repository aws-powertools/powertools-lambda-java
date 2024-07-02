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
import com.amazonaws.services.lambda.runtime.events.SQSBatchResponse;
import com.amazonaws.services.lambda.runtime.events.SQSEvent;
import com.amazonaws.services.lambda.runtime.tests.annotations.Event;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.params.ParameterizedTest;
import org.mockito.Mock;
import software.amazon.lambda.powertools.batch.handler.BatchMessageHandler;
import software.amazon.lambda.powertools.batch.model.Product;

class SQSBatchProcessorTest {
    @Mock
    private Context context;

    private final List<String> threadList = Collections.synchronizedList(new ArrayList<>());

    @AfterEach
    public void clear() {
        threadList.clear();
    }

    // A handler that works
    private void processMessageSucceeds(SQSEvent.SQSMessage sqsMessage) {
    }

    private void processMessageInParallelSucceeds(SQSEvent.SQSMessage sqsMessage) {
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

    // A handler that throws an exception for _one_ of the sample messages
    private void processMessageFailsForFixedMessage(SQSEvent.SQSMessage message, Context context) {
        if (message.getMessageId().equals("e9144555-9a4f-4ec3-99a0-34ce359b4b54")) {
            throw new RuntimeException("fake exception");
        }
    }

    private void processMessageInParallelFailsForFixedMessage(SQSEvent.SQSMessage message, Context context) {
        String thread = Thread.currentThread().getName();
        if (!threadList.contains(thread)) {
            threadList.add(thread);
        }
        try {
            Thread.sleep(500); // simulate some processing
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
        if (message.getMessageId().equals("e9144555-9a4f-4ec3-99a0-34ce359b4b54")) {
            throw new RuntimeException("fake exception");
        }
    }

    // A handler that throws an exception for _one_ of the deserialized products in the same messages
    private void processMessageFailsForFixedProduct(Product product, Context context) {
        if (product.getId() == 12345) {
            throw new RuntimeException("fake exception");
        }
    }

    @ParameterizedTest
    @Event(value = "sqs_event.json", type = SQSEvent.class)
    void batchProcessingSucceedsAndReturns(SQSEvent event) {
        // Arrange
        BatchMessageHandler<SQSEvent, SQSBatchResponse> handler = new BatchMessageHandlerBuilder()
                .withSqsBatchHandler()
                .buildWithRawMessageHandler(this::processMessageSucceeds);

        // Act
        SQSBatchResponse sqsBatchResponse = handler.processBatch(event, context);

        // Assert
        assertThat(sqsBatchResponse.getBatchItemFailures()).isEmpty();
    }

    @ParameterizedTest
    @Event(value = "sqs_event_big.json", type = SQSEvent.class)
    void parallelBatchProcessingSucceedsAndReturns(SQSEvent event) {
        // Arrange
        BatchMessageHandler<SQSEvent, SQSBatchResponse> handler = new BatchMessageHandlerBuilder()
                .withSqsBatchHandler()
                .buildWithRawMessageHandler(this::processMessageInParallelSucceeds);

        // Act
        SQSBatchResponse sqsBatchResponse = handler.processBatchInParallel(event, context);

        // Assert
        assertThat(sqsBatchResponse.getBatchItemFailures()).isEmpty();
        assertThat(threadList).hasSizeGreaterThan(1);
    }

    @ParameterizedTest
    @Event(value = "sqs_event.json", type = SQSEvent.class)
    void shouldAddMessageToBatchFailure_whenException_withMessage(SQSEvent event) {
        // Arrange
        BatchMessageHandler<SQSEvent, SQSBatchResponse> handler = new BatchMessageHandlerBuilder()
                .withSqsBatchHandler()
                .buildWithRawMessageHandler(this::processMessageFailsForFixedMessage);

        // Act
        SQSBatchResponse sqsBatchResponse = handler.processBatch(event, context);

        // Assert
        assertThat(sqsBatchResponse.getBatchItemFailures()).hasSize(1);
        SQSBatchResponse.BatchItemFailure batchItemFailure = sqsBatchResponse.getBatchItemFailures().get(0);
        assertThat(batchItemFailure.getItemIdentifier()).isEqualTo("e9144555-9a4f-4ec3-99a0-34ce359b4b54");
    }

    @ParameterizedTest
    @Event(value = "sqs_event_big.json", type = SQSEvent.class)
    void parallelBatchProcessing_shouldAddMessageToBatchFailure_whenException_withMessage(SQSEvent event) {
        // Arrange
        BatchMessageHandler<SQSEvent, SQSBatchResponse> handler = new BatchMessageHandlerBuilder()
                .withSqsBatchHandler()
                .buildWithRawMessageHandler(this::processMessageInParallelFailsForFixedMessage);

        // Act
        SQSBatchResponse sqsBatchResponse = handler.processBatchInParallel(event, context);

        // Assert
        assertThat(sqsBatchResponse.getBatchItemFailures()).hasSize(1);
        SQSBatchResponse.BatchItemFailure batchItemFailure = sqsBatchResponse.getBatchItemFailures().get(0);
        assertThat(batchItemFailure.getItemIdentifier()).isEqualTo("e9144555-9a4f-4ec3-99a0-34ce359b4b54");
        assertThat(threadList).hasSizeGreaterThan(1);
    }

    @ParameterizedTest
    @Event(value = "sqs_fifo_event.json", type = SQSEvent.class)
    void shouldAddMessageToBatchFailure_whenException_withSQSFIFO(SQSEvent event) {
        // Arrange
        BatchMessageHandler<SQSEvent, SQSBatchResponse> handler = new BatchMessageHandlerBuilder()
                .withSqsBatchHandler()
                .buildWithRawMessageHandler(this::processMessageFailsForFixedMessage);

        // Act
        SQSBatchResponse sqsBatchResponse = handler.processBatch(event, context);

        // Assert
        assertThat(sqsBatchResponse.getBatchItemFailures()).hasSize(2);
        SQSBatchResponse.BatchItemFailure batchItemFailure = sqsBatchResponse.getBatchItemFailures().get(0);
        assertThat(batchItemFailure.getItemIdentifier()).isEqualTo("e9144555-9a4f-4ec3-99a0-34ce359b4b54");
        batchItemFailure = sqsBatchResponse.getBatchItemFailures().get(1);
        assertThat(batchItemFailure.getItemIdentifier()).isEqualTo("f9144555-9a4f-4ec3-99a0-34ce359b4b54");
    }


    @ParameterizedTest
    @Event(value = "sqs_event.json", type = SQSEvent.class)
    void shouldAddMessageToBatchFailure_whenException_withProduct(SQSEvent event) {

        // Arrange
        BatchMessageHandler<SQSEvent, SQSBatchResponse> handler = new BatchMessageHandlerBuilder()
                .withSqsBatchHandler()
                .buildWithMessageHandler(this::processMessageFailsForFixedProduct, Product.class);

        // Act
        SQSBatchResponse sqsBatchResponse = handler.processBatch(event, context);
        assertThat(sqsBatchResponse.getBatchItemFailures()).hasSize(1);

        // Assert
        SQSBatchResponse.BatchItemFailure batchItemFailure = sqsBatchResponse.getBatchItemFailures().get(0);
        assertThat(batchItemFailure.getItemIdentifier()).isEqualTo("e9144555-9a4f-4ec3-99a0-34ce359b4b54");
    }

    @ParameterizedTest
    @Event(value = "sqs_event.json", type = SQSEvent.class)
    void failingFailureHandlerShouldntFailBatch(SQSEvent event) {
        // Arrange
        AtomicBoolean wasCalled = new AtomicBoolean(false);
        BatchMessageHandler<SQSEvent, SQSBatchResponse> handler = new BatchMessageHandlerBuilder()
                .withSqsBatchHandler()
                .withFailureHandler((e, ex) -> {
                    wasCalled.set(true);
                    throw new RuntimeException("Well, this doesn't look great");
                })
                .buildWithMessageHandler(this::processMessageFailsForFixedProduct, Product.class);

        // Act
        SQSBatchResponse sqsBatchResponse = handler.processBatch(event, context);

        // Assert
        assertThat(sqsBatchResponse).isNotNull();
        assertThat(wasCalled.get()).isTrue();
        SQSBatchResponse.BatchItemFailure batchItemFailure = sqsBatchResponse.getBatchItemFailures().get(0);
        assertThat(batchItemFailure.getItemIdentifier()).isEqualTo("e9144555-9a4f-4ec3-99a0-34ce359b4b54");
    }

    @ParameterizedTest
    @Event(value = "sqs_event.json", type = SQSEvent.class)
    void failingSuccessHandlerShouldntFailBatchButShouldFailMessage(SQSEvent event) {
        // Arrange
        AtomicBoolean wasCalledAndFailed = new AtomicBoolean(false);
        BatchMessageHandler<SQSEvent, SQSBatchResponse> handler = new BatchMessageHandlerBuilder()
                .withSqsBatchHandler()
                .withSuccessHandler((e) -> {
                    if (e.getMessageId().equals("e9144555-9a4f-4ec3-99a0-34ce359b4b54")) {
                        wasCalledAndFailed.set(true);
                        throw new RuntimeException("Success handler throws");
                    }
                })
                .buildWithRawMessageHandler(this::processMessageSucceeds);

        // Act
        SQSBatchResponse sqsBatchResponse = handler.processBatch(event, context);

        // Assert
        assertThat(sqsBatchResponse).isNotNull();
        assertThat(wasCalledAndFailed.get()).isTrue();
        SQSBatchResponse.BatchItemFailure batchItemFailure = sqsBatchResponse.getBatchItemFailures().get(0);
        assertThat(batchItemFailure.getItemIdentifier()).isEqualTo("e9144555-9a4f-4ec3-99a0-34ce359b4b54");
    }


}

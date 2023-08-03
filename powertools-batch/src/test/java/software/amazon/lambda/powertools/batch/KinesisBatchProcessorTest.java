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

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.events.KinesisEvent;
import com.amazonaws.services.lambda.runtime.events.StreamsEventResponse;
import com.amazonaws.services.lambda.runtime.tests.annotations.Event;
import org.junit.jupiter.params.ParameterizedTest;
import org.mockito.Mock;
import software.amazon.lambda.powertools.batch.handler.BatchMessageHandler;
import software.amazon.lambda.powertools.batch.model.Product;

import java.util.concurrent.atomic.AtomicBoolean;

import static org.assertj.core.api.Assertions.assertThat;

public class KinesisBatchProcessorTest {

    @Mock
    private Context context;

    private void processMessageSucceeds(KinesisEvent.KinesisEventRecord record, Context context) {
        // Great success
    }

    private void processMessageFailsForFixedMessage(KinesisEvent.KinesisEventRecord record, Context context) {
        if (record.getKinesis().getSequenceNumber()
                .equals("49545115243490985018280067714973144582180062593244200961")) {
            throw new RuntimeException("fake exception");
        }
    }

    // A handler that throws an exception for _one_ of the deserialized products in the same messages
    public void processMessageFailsForFixedProduct(Product product, Context context) {
        if (product.getId() == 1234) {
            throw new RuntimeException("fake exception");
        }
    }

    @ParameterizedTest
    @Event(value = "kinesis_event.json", type = KinesisEvent.class)
    public void batchProcessingSucceedsAndReturns(KinesisEvent event) {
        // Arrange
        BatchMessageHandler<KinesisEvent, StreamsEventResponse> handler = new BatchMessageHandlerBuilder()
                .withKinesisBatchHandler()
                .buildWithRawMessageHandler(this::processMessageSucceeds);

        // Act
        StreamsEventResponse kinesisBatchResponse = handler.processBatch(event, context);

        // Assert
        assertThat(kinesisBatchResponse.getBatchItemFailures()).hasSize(0);
    }

    @ParameterizedTest
    @Event(value = "kinesis_event.json", type = KinesisEvent.class)
    public void shouldAddMessageToBatchFailure_whenException_withMessage(KinesisEvent event) {
        // Arrange
        BatchMessageHandler<KinesisEvent, StreamsEventResponse> handler = new BatchMessageHandlerBuilder()
                .withKinesisBatchHandler()
                .buildWithRawMessageHandler(this::processMessageFailsForFixedMessage);

        // Act
        StreamsEventResponse kinesisBatchResponse = handler.processBatch(event, context);

        // Assert
        assertThat(kinesisBatchResponse.getBatchItemFailures()).hasSize(1);
        StreamsEventResponse.BatchItemFailure batchItemFailure = kinesisBatchResponse.getBatchItemFailures().get(0);
        assertThat(batchItemFailure.getItemIdentifier()).isEqualTo(
                "49545115243490985018280067714973144582180062593244200961");
    }

    @ParameterizedTest
    @Event(value = "kinesis_event.json", type = KinesisEvent.class)
    public void shouldAddMessageToBatchFailure_whenException_withProduct(KinesisEvent event) {
        // Arrange
        BatchMessageHandler<KinesisEvent, StreamsEventResponse> handler = new BatchMessageHandlerBuilder()
                .withKinesisBatchHandler()
                .buildWithMessageHandler(this::processMessageFailsForFixedProduct, Product.class);

        // Act
        StreamsEventResponse kinesisBatchResponse = handler.processBatch(event, context);

        // Assert
        assertThat(kinesisBatchResponse.getBatchItemFailures()).hasSize(1);
        StreamsEventResponse.BatchItemFailure batchItemFailure = kinesisBatchResponse.getBatchItemFailures().get(0);
        assertThat(batchItemFailure.getItemIdentifier()).isEqualTo(
                "49545115243490985018280067714973144582180062593244200961");
    }

    @ParameterizedTest
    @Event(value = "kinesis_event.json", type = KinesisEvent.class)
    public void failingFailureHandlerShouldntFailBatch(KinesisEvent event) {
        // Arrange
        AtomicBoolean wasCalled = new AtomicBoolean(false);
        BatchMessageHandler<KinesisEvent, StreamsEventResponse> handler = new BatchMessageHandlerBuilder()
                .withKinesisBatchHandler()
                .withFailureHandler((e, ex) -> {
                    wasCalled.set(true);
                    throw new RuntimeException("Well, this doesn't look great");
                })
                .buildWithMessageHandler(this::processMessageFailsForFixedProduct, Product.class);

        // Act
        StreamsEventResponse kinesisBatchResponse = handler.processBatch(event, context);

        // Assert
        assertThat(kinesisBatchResponse).isNotNull();
        assertThat(kinesisBatchResponse.getBatchItemFailures().size()).isEqualTo(1);
        assertThat(wasCalled.get()).isTrue();
        StreamsEventResponse.BatchItemFailure batchItemFailure = kinesisBatchResponse.getBatchItemFailures().get(0);
        assertThat(batchItemFailure.getItemIdentifier()).isEqualTo(
                "49545115243490985018280067714973144582180062593244200961");
    }

    @ParameterizedTest
    @Event(value = "kinesis_event.json", type = KinesisEvent.class)
    public void failingSuccessHandlerShouldntFailBatchButShouldFailMessage(KinesisEvent event) {
        // Arrange
        AtomicBoolean wasCalledAndFailed = new AtomicBoolean(false);
        BatchMessageHandler<KinesisEvent, StreamsEventResponse> handler = new BatchMessageHandlerBuilder()
                .withKinesisBatchHandler()
                .withSuccessHandler((e) -> {
                    if (e.getKinesis().getSequenceNumber()
                            .equals("49545115243490985018280067714973144582180062593244200961")) {
                        wasCalledAndFailed.set(true);
                        throw new RuntimeException("Success handler throws");
                    }
                })
                .buildWithRawMessageHandler(this::processMessageSucceeds);

        // Act
        StreamsEventResponse kinesisBatchResponse = handler.processBatch(event, context);

        // Assert
        assertThat(kinesisBatchResponse).isNotNull();
        assertThat(kinesisBatchResponse.getBatchItemFailures().size()).isEqualTo(1);
        assertThat(wasCalledAndFailed.get()).isTrue();
        StreamsEventResponse.BatchItemFailure batchItemFailure = kinesisBatchResponse.getBatchItemFailures().get(0);
        assertThat(batchItemFailure.getItemIdentifier()).isEqualTo(
                "49545115243490985018280067714973144582180062593244200961");
    }

}

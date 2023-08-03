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
import com.amazonaws.services.lambda.runtime.events.DynamodbEvent;
import com.amazonaws.services.lambda.runtime.events.StreamsEventResponse;
import com.amazonaws.services.lambda.runtime.tests.annotations.Event;
import org.junit.jupiter.params.ParameterizedTest;
import org.mockito.Mock;
import software.amazon.lambda.powertools.batch.handler.BatchMessageHandler;

import java.util.concurrent.atomic.AtomicBoolean;

import static org.assertj.core.api.Assertions.assertThat;

public class DdbBatchProcessorTest {

    @Mock
    private Context context;

    private void processMessageSucceeds(DynamodbEvent.DynamodbStreamRecord record, Context context) {
        // Great success
    }

    private void processMessageFailsForFixedMessage(DynamodbEvent.DynamodbStreamRecord record, Context context) {
        if (record.getDynamodb().getSequenceNumber().equals("4421584500000000017450439091")) {
            throw new RuntimeException("fake exception");
        }
    }

    @ParameterizedTest
    @Event(value = "dynamo_event.json", type = DynamodbEvent.class)
    public void batchProcessingSucceedsAndReturns(DynamodbEvent event) {
        // Arrange
        BatchMessageHandler<DynamodbEvent, StreamsEventResponse> handler = new BatchMessageHandlerBuilder()
                .withDynamoDbBatchHandler()
                .buildWithRawMessageHandler(this::processMessageSucceeds);

        // Act
        StreamsEventResponse dynamodbBatchResponse = handler.processBatch(event, context);

        // Assert
        assertThat(dynamodbBatchResponse.getBatchItemFailures()).hasSize(0);
    }

    @ParameterizedTest
    @Event(value = "dynamo_event.json", type = DynamodbEvent.class)
    public void shouldAddMessageToBatchFailure_whenException_withMessage(DynamodbEvent event) {
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
    @Event(value = "dynamo_event.json", type = DynamodbEvent.class)
    public void failingFailureHandlerShouldntFailBatch(DynamodbEvent event) {
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
        assertThat(dynamodbBatchResponse.getBatchItemFailures().size()).isEqualTo(1);
        assertThat(wasCalledAndFailed.get()).isTrue();
        StreamsEventResponse.BatchItemFailure batchItemFailure = dynamodbBatchResponse.getBatchItemFailures().get(0);
        assertThat(batchItemFailure.getItemIdentifier()).isEqualTo("4421584500000000017450439091");
    }

    @ParameterizedTest
    @Event(value = "dynamo_event.json", type = DynamodbEvent.class)
    public void failingSuccessHandlerShouldntFailBatchButShouldFailMessage(DynamodbEvent event) {
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
        assertThat(dynamodbBatchResponse.getBatchItemFailures().size()).isEqualTo(1);
        assertThat(wasCalledAndFailed.get()).isTrue();
        StreamsEventResponse.BatchItemFailure batchItemFailure = dynamodbBatchResponse.getBatchItemFailures().get(0);
        assertThat(batchItemFailure.getItemIdentifier()).isEqualTo("4421584500000000017450439091");
    }

}

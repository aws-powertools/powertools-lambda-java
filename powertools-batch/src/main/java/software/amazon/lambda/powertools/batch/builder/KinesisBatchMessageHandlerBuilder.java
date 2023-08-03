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

package software.amazon.lambda.powertools.batch.builder;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.events.KinesisEvent;
import com.amazonaws.services.lambda.runtime.events.StreamsEventResponse;
import software.amazon.lambda.powertools.batch.handler.BatchMessageHandler;
import software.amazon.lambda.powertools.batch.handler.KinesisStreamsBatchMessageHandler;

import java.util.function.BiConsumer;

/**
 * Builds a batch processor for processing Kinesis Streams batch events
 */
public class KinesisBatchMessageHandlerBuilder
        extends AbstractBatchMessageHandlerBuilder<KinesisEvent.KinesisEventRecord,
        KinesisBatchMessageHandlerBuilder,
        KinesisEvent,
        StreamsEventResponse> {
    @Override
    public BatchMessageHandler<KinesisEvent, StreamsEventResponse> buildWithRawMessageHandler(
            BiConsumer<KinesisEvent.KinesisEventRecord, Context> rawMessageHandler) {
        return new KinesisStreamsBatchMessageHandler<Void>(
                rawMessageHandler,
                null,
                null,
                successHandler,
                failureHandler);
    }

    @Override
    public <M> BatchMessageHandler<KinesisEvent, StreamsEventResponse> buildWithMessageHandler(
            BiConsumer<M, Context> messageHandler, Class<M> messageClass) {
        return new KinesisStreamsBatchMessageHandler<>(
                null,
                messageHandler,
                messageClass,
                successHandler,
                failureHandler);
    }

    @Override
    protected KinesisBatchMessageHandlerBuilder getThis() {
        return this;
    }
}

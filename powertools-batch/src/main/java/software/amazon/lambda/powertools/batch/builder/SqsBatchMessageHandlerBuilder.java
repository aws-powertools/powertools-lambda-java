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
import com.amazonaws.services.lambda.runtime.events.SQSBatchResponse;
import com.amazonaws.services.lambda.runtime.events.SQSEvent;
import software.amazon.lambda.powertools.batch.handler.BatchMessageHandler;
import software.amazon.lambda.powertools.batch.handler.SqsBatchMessageHandler;

import java.util.function.BiConsumer;

/**
 * Builds a batch processor for the SQS event source.
 */
public class SqsBatchMessageHandlerBuilder extends AbstractBatchMessageHandlerBuilder<SQSEvent.SQSMessage,
        SqsBatchMessageHandlerBuilder,
        SQSEvent,
        SQSBatchResponse> {


    @Override
    public BatchMessageHandler<SQSEvent, SQSBatchResponse> buildWithRawMessageHandler(
            BiConsumer<SQSEvent.SQSMessage, Context> rawMessageHandler) {
        return new SqsBatchMessageHandler<Void>(
                null,
                null,
                rawMessageHandler,
                successHandler,
                failureHandler
        );
    }

    @Override
    public <M> BatchMessageHandler<SQSEvent, SQSBatchResponse> buildWithMessageHandler(
            BiConsumer<M, Context> messageHandler, Class<M> messageClass) {
        return new SqsBatchMessageHandler<>(
                messageHandler,
                messageClass,
                null,
                successHandler,
                failureHandler
        );
    }


    @Override
    protected SqsBatchMessageHandlerBuilder getThis() {
        return this;
    }


}

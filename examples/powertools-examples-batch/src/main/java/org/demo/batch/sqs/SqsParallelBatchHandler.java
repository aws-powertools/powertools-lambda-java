/*
 * Copyright 2024 Amazon.com, Inc. or its affiliates.
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

package org.demo.batch.sqs;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestHandler;
import com.amazonaws.services.lambda.runtime.events.SQSBatchResponse;
import com.amazonaws.services.lambda.runtime.events.SQSEvent;
import org.demo.batch.model.Product;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import software.amazon.lambda.powertools.batch.BatchMessageHandlerBuilder;
import software.amazon.lambda.powertools.batch.handler.BatchMessageHandler;
import software.amazon.lambda.powertools.logging.Logging;
import software.amazon.lambda.powertools.tracing.Tracing;

public class SqsParallelBatchHandler extends AbstractSqsBatchHandler implements RequestHandler<SQSEvent, SQSBatchResponse> {
    private static final Logger LOGGER = LoggerFactory.getLogger(SqsParallelBatchHandler.class);
    private final BatchMessageHandler<SQSEvent, SQSBatchResponse> handler;

    public SqsParallelBatchHandler() {
        handler = new BatchMessageHandlerBuilder()
                .withSqsBatchHandler()
                .buildWithMessageHandler(this::processMessage, Product.class);
    }

    @Logging
    @Tracing
    @Override
    public SQSBatchResponse handleRequest(SQSEvent sqsEvent, Context context) {
        LOGGER.info("Processing batch of {} messages", sqsEvent.getRecords().size());
        return handler.processBatchInParallel(sqsEvent, context);
    }
}

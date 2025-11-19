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

package software.amazon.lambda.powertools.batch.handler;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.events.DynamodbEvent;
import com.amazonaws.services.lambda.runtime.events.StreamsEventResponse;

import software.amazon.lambda.powertools.batch.internal.MultiThreadMDC;
import software.amazon.lambda.powertools.batch.internal.XRayTraceEntityPropagator;

/**
 * A batch message processor for DynamoDB Streams batches.
 *
 * @see <a href="https://docs.aws.amazon.com/lambda/latest/dg/with-ddb.html#services-ddb-batchfailurereporting">DynamoDB Streams batch failure reporting</a>
 */
public class DynamoDbBatchMessageHandler implements BatchMessageHandler<DynamodbEvent, StreamsEventResponse> {
    private static final Logger LOGGER = LoggerFactory.getLogger(DynamoDbBatchMessageHandler.class);

    private final Consumer<DynamodbEvent.DynamodbStreamRecord> successHandler;
    private final BiConsumer<DynamodbEvent.DynamodbStreamRecord, Throwable> failureHandler;
    private final BiConsumer<DynamodbEvent.DynamodbStreamRecord, Context> rawMessageHandler;

    public DynamoDbBatchMessageHandler(Consumer<DynamodbEvent.DynamodbStreamRecord> successHandler,
            BiConsumer<DynamodbEvent.DynamodbStreamRecord, Throwable> failureHandler,
            BiConsumer<DynamodbEvent.DynamodbStreamRecord, Context> rawMessageHandler) {
        this.successHandler = successHandler;
        this.failureHandler = failureHandler;
        this.rawMessageHandler = rawMessageHandler;
    }

    @Override
    public StreamsEventResponse processBatch(DynamodbEvent event, Context context) {
        List<StreamsEventResponse.BatchItemFailure> batchItemFailures = event.getRecords()
                .stream()
                .map(eventRecord -> processBatchItem(eventRecord, context))
                .filter(Optional::isPresent)
                .map(Optional::get)
                .collect(Collectors.toList());

        return StreamsEventResponse.builder().withBatchItemFailures(batchItemFailures).build();
    }

    @Override
    public StreamsEventResponse processBatchInParallel(DynamodbEvent event, Context context) {
        MultiThreadMDC multiThreadMDC = new MultiThreadMDC();
        Object capturedSubsegment = XRayTraceEntityPropagator.captureTraceEntity();

        List<StreamsEventResponse.BatchItemFailure> batchItemFailures = event.getRecords()
                .parallelStream() // Parallel processing
                .map(eventRecord -> {
                    AtomicReference<Optional<StreamsEventResponse.BatchItemFailure>> result = new AtomicReference<>();

                    XRayTraceEntityPropagator.runWithEntity(capturedSubsegment, () -> {
                        multiThreadMDC.copyMDCToThread(Thread.currentThread().getName());
                        try {
                            result.set(processBatchItem(eventRecord, context));
                        } finally {
                            multiThreadMDC.removeThread(Thread.currentThread().getName());
                        }
                    });

                    return result.get();
                })
                .filter(Optional::isPresent)
                .map(Optional::get)
                .collect(Collectors.toList());

        return StreamsEventResponse.builder().withBatchItemFailures(batchItemFailures).build();
    }

    @Override
    public StreamsEventResponse processBatchInParallel(DynamodbEvent event, Context context, Executor executor) {
        MultiThreadMDC multiThreadMDC = new MultiThreadMDC();
        Object capturedSubsegment = XRayTraceEntityPropagator.captureTraceEntity();

        List<StreamsEventResponse.BatchItemFailure> batchItemFailures = new ArrayList<>();
        List<CompletableFuture<Void>> futures = event.getRecords().stream()
                .map(eventRecord -> CompletableFuture.runAsync(() -> {
                    XRayTraceEntityPropagator.runWithEntity(capturedSubsegment, () -> {
                        multiThreadMDC.copyMDCToThread(Thread.currentThread().getName());
                        try {
                            Optional<StreamsEventResponse.BatchItemFailure> failureOpt = processBatchItem(eventRecord,
                                    context);
                            failureOpt.ifPresent(batchItemFailures::add);
                        } finally {
                            multiThreadMDC.removeThread(Thread.currentThread().getName());
                        }
                    });
                }, executor))
                .collect(Collectors.toList());
        futures.forEach(CompletableFuture::join);
        return StreamsEventResponse.builder().withBatchItemFailures(batchItemFailures).build();
    }

    private Optional<StreamsEventResponse.BatchItemFailure> processBatchItem(
            DynamodbEvent.DynamodbStreamRecord streamRecord, Context context) {
        try {
            LOGGER.debug("Processing item {}", streamRecord.getEventID());

            rawMessageHandler.accept(streamRecord, context);

            // Report success if we have a handler
            if (this.successHandler != null) {
                this.successHandler.accept(streamRecord);
            }
            return Optional.empty();
        } catch (Exception e) {
            String sequenceNumber = streamRecord.getDynamodb().getSequenceNumber();
            LOGGER.error("Error while processing record with id {}: {}, adding it to batch item failures",
                    sequenceNumber, e.getMessage());
            LOGGER.error("Error was", e);

            // Report failure if we have a handler
            if (this.failureHandler != null) {
                // A failing failure handler is no reason to fail the batch
                try {
                    this.failureHandler.accept(streamRecord, e);
                } catch (Exception e2) {
                    LOGGER.warn("failureHandler threw handling failure", e2);
                }
            }
            return Optional
                    .of(StreamsEventResponse.BatchItemFailure.builder().withItemIdentifier(sequenceNumber).build());
        }
    }
}

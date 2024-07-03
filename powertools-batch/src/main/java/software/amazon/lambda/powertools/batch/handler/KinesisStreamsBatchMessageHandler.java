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


import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.events.KinesisEvent;
import com.amazonaws.services.lambda.runtime.events.StreamsEventResponse;
import java.util.List;
import java.util.Optional;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import software.amazon.lambda.powertools.batch.internal.MultiThreadMDC;
import software.amazon.lambda.powertools.utilities.EventDeserializer;

/**
 * A batch message processor for Kinesis Streams batch processing.
 * <p>
 * Refer to <a href="https://docs.aws.amazon.com/lambda/latest/dg/with-kinesis.html#services-kinesis-batchfailurereporting">Kinesis Batch failure reporting</a>
 *
 * @param <M> The user-defined type of the Kinesis record payload
 */
public class KinesisStreamsBatchMessageHandler<M> implements BatchMessageHandler<KinesisEvent, StreamsEventResponse> {
    private static final Logger LOGGER = LoggerFactory.getLogger(KinesisStreamsBatchMessageHandler.class);

    private final BiConsumer<KinesisEvent.KinesisEventRecord, Context> rawMessageHandler;
    private final BiConsumer<M, Context> messageHandler;
    private final Class<M> messageClass;
    private final Consumer<KinesisEvent.KinesisEventRecord> successHandler;
    private final BiConsumer<KinesisEvent.KinesisEventRecord, Throwable> failureHandler;

    public KinesisStreamsBatchMessageHandler(BiConsumer<KinesisEvent.KinesisEventRecord, Context> rawMessageHandler,
                                             BiConsumer<M, Context> messageHandler,
                                             Class<M> messageClass,
                                             Consumer<KinesisEvent.KinesisEventRecord> successHandler,
                                             BiConsumer<KinesisEvent.KinesisEventRecord, Throwable> failureHandler) {

        this.rawMessageHandler = rawMessageHandler;
        this.messageHandler = messageHandler;
        this.messageClass = messageClass;
        this.successHandler = successHandler;
        this.failureHandler = failureHandler;
    }

    @Override
    public StreamsEventResponse processBatch(KinesisEvent event, Context context) {
        List<StreamsEventResponse.BatchItemFailure> batchItemFailures = event.getRecords()
                .stream()
                .map(eventRecord -> processBatchItem(eventRecord, context))
                .filter(Optional::isPresent)
                .map(Optional::get)
                .collect(Collectors.toList());

        return StreamsEventResponse.builder().withBatchItemFailures(batchItemFailures).build();
    }

    @Override
    public StreamsEventResponse processBatchInParallel(KinesisEvent event, Context context) {
        MultiThreadMDC multiThreadMDC = new MultiThreadMDC();

        List<StreamsEventResponse.BatchItemFailure> batchItemFailures = event.getRecords()
                .parallelStream() // Parallel processing
                .map(eventRecord -> {
                    multiThreadMDC.copyMDCToThread(Thread.currentThread().getName());
                    return processBatchItem(eventRecord, context);
                })
                .filter(Optional::isPresent)
                .map(Optional::get)
                .collect(Collectors.toList());

        return StreamsEventResponse.builder().withBatchItemFailures(batchItemFailures).build();
    }

    private Optional<StreamsEventResponse.BatchItemFailure> processBatchItem(KinesisEvent.KinesisEventRecord eventRecord, Context context) {
        try {
            LOGGER.debug("Processing item {}", eventRecord.getEventID());

            if (this.rawMessageHandler != null) {
                rawMessageHandler.accept(eventRecord, context);
            } else {
                M messageDeserialized = EventDeserializer.extractDataFrom(eventRecord).as(messageClass);
                messageHandler.accept(messageDeserialized, context);
            }

            // Report success if we have a handler
            if (this.successHandler != null) {
                this.successHandler.accept(eventRecord);
            }
            return Optional.empty();
        } catch (Throwable t) {
            String sequenceNumber = eventRecord.getEventID();
            LOGGER.error("Error while processing record with eventID {}: {}, adding it to batch item failures",
                    sequenceNumber, t.getMessage());
            LOGGER.error("Error was", t);

            // Report failure if we have a handler
            if (this.failureHandler != null) {
                // A failing failure handler is no reason to fail the batch
                try {
                    this.failureHandler.accept(eventRecord, t);
                } catch (Throwable t2) {
                    LOGGER.warn("failureHandler threw handling failure", t2);
                }
            }

            return Optional.of(StreamsEventResponse.BatchItemFailure.builder().withItemIdentifier(eventRecord.getKinesis().getSequenceNumber()).build());
        }
    }
}


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
import java.util.ArrayList;
import java.util.List;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import software.amazon.lambda.powertools.utilities.EventDeserializer;

/**
 * A batch message processor for Kinesis Streams batch processing.
 * <p>
 * Refer to <a href="https://docs.aws.amazon.com/lambda/latest/dg/with-kinesis.html#services-kinesis-batchfailurereporting">Kinesis Batch failure reporting</a>
 *
 * @param <M> The user-defined type of the Kinesis record payload
 */
public class KinesisStreamsBatchMessageHandler<M> implements BatchMessageHandler<KinesisEvent, StreamsEventResponse> {
    private final static Logger LOGGER = LoggerFactory.getLogger(KinesisStreamsBatchMessageHandler.class);

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
        List<StreamsEventResponse.BatchItemFailure> batchFailures = new ArrayList<>();

        for (KinesisEvent.KinesisEventRecord record : event.getRecords()) {
            try {
                if (this.rawMessageHandler != null) {
                    rawMessageHandler.accept(record, context);
                } else {
                    M messageDeserialized = EventDeserializer.extractDataFrom(record).as(messageClass);
                    messageHandler.accept(messageDeserialized, context);
                }

                // Report success if we have a handler
                if (this.successHandler != null) {
                    this.successHandler.accept(record);
                }
            } catch (Throwable t) {
                String sequenceNumber = record.getEventID();
                LOGGER.error("Error while processing record with eventID {}: {}, adding it to batch item failures",
                        sequenceNumber, t.getMessage());
                LOGGER.error("Error was", t);

                batchFailures.add(new StreamsEventResponse.BatchItemFailure(record.getKinesis().getSequenceNumber()));

                // Report failure if we have a handler
                if (this.failureHandler != null) {
                    // A failing failure handler is no reason to fail the batch
                    try {
                        this.failureHandler.accept(record, t);
                    } catch (Throwable t2) {
                        LOGGER.warn("failureHandler threw handling failure", t2);
                    }
                }
            }
        }

        return new StreamsEventResponse(batchFailures);
    }
}


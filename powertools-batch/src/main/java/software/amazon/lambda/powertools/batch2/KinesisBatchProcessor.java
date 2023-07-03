package software.amazon.lambda.powertools.batch2;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.events.KinesisEvent;
import com.amazonaws.services.lambda.runtime.events.StreamsEventResponse;
import com.amazonaws.services.lambda.runtime.events.StreamsEventResponse.BatchItemFailure;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import software.amazon.lambda.powertools.utilities.EventDeserializer;
import sun.reflect.generics.reflectiveObjects.ParameterizedTypeImpl;

import java.util.ArrayList;

public interface KinesisBatchProcessor<ITEM> extends BatchProcessor<KinesisEvent, ITEM, StreamsEventResponse> {

    Logger KINESIS_BATCH_LOGGER = LoggerFactory.getLogger(KinesisBatchProcessor.class);

    @Override
    default StreamsEventResponse processBatch(KinesisEvent event, Context context) {
        Class<ITEM> bodyClass = (Class<ITEM>) ((ParameterizedTypeImpl) getClass().getGenericInterfaces()[0]).getActualTypeArguments()[0];
        boolean isKinesisRecord = bodyClass.equals(KinesisEvent.KinesisEventRecord.class);

        StreamsEventResponse response = StreamsEventResponse.builder().withBatchItemFailures(new ArrayList<>()).build();
        for (KinesisEvent.KinesisEventRecord record : event.getRecords()) {
            try {
                if (isKinesisRecord) {
                    processRecord(record, context);
                } else {
                    processItem(EventDeserializer.extractDataFrom(record).as(bodyClass), context);
                }
            } catch (Throwable t) {
                KINESIS_BATCH_LOGGER.error("Error while processing record with id {}: {}, adding it to batch item failures", record.getEventID(), t.getMessage());
                response.getBatchItemFailures().add(BatchItemFailure.builder().withItemIdentifier(record.getEventID()).build());
            }
        }
        return response;
    }

    default void processRecord(KinesisEvent.KinesisEventRecord record, Context context) {
        KINESIS_BATCH_LOGGER.debug("[DEFAULT IMPLEMENTATION] Processing record {}", record.getEventID());
    }

}

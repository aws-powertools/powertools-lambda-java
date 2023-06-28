package software.amazon.lambda.powertools.batch2;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.events.KinesisEvent;
import com.amazonaws.services.lambda.runtime.events.StreamsEventResponse;
import software.amazon.lambda.powertools.utilities.EventDeserializer;
import sun.reflect.generics.reflectiveObjects.ParameterizedTypeImpl;

public interface KinesisBatchProcessor<ITEM> extends BatchProcessor<KinesisEvent, ITEM, StreamsEventResponse> {

    @Override
    default StreamsEventResponse processBatch(KinesisEvent event, Context context) {
        Class<ITEM> bodyClass = (Class<ITEM>) ((ParameterizedTypeImpl) getClass().getGenericInterfaces()[0]).getActualTypeArguments()[0];

        StreamsEventResponse response = new StreamsEventResponse();
        for (KinesisEvent.KinesisEventRecord record : event.getRecords()) {
            try {
                if (bodyClass.equals(KinesisEvent.KinesisEventRecord.class)) {
                    processItem(record, context);
                } else {
                    processItem(EventDeserializer.extractDataFrom(record).as(bodyClass), context);
                }
            } catch (Throwable t) {
                response.getBatchItemFailures().add(new StreamsEventResponse.BatchItemFailure(record.getEventID()));
            }
        }
        return response;
    }

    default void processItem(KinesisEvent.KinesisEventRecord record, Context context) {
        System.out.println("Processing message " + record.getEventID());
    }

}

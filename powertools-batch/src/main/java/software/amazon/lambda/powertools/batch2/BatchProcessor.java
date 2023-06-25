package software.amazon.lambda.powertools.batch2;

import com.amazonaws.services.lambda.runtime.events.DynamodbEvent;
import com.amazonaws.services.lambda.runtime.events.KinesisEvent;
import com.amazonaws.services.lambda.runtime.events.SQSEvent;
import sun.reflect.generics.reflectiveObjects.NotImplementedException;

public interface BatchProcessor<I, O> {

    default O processBatch(I input) {
        // depending on the input type (SQS/Kinesis/Dynamo/...), create the appropriate response

        // TODO - need a way of extracting items from input. Defer to interface?

        // browse the list of items
        // for each item
        try {
            // Need to typematch to get the processItem calls to bind
            if (input instanceof SQSEvent.SQSMessage) {
                processItem((SQSEvent.SQSMessage) input);
            } else if (input instanceof KinesisEvent.KinesisEventRecord) {
                processItem((KinesisEvent.KinesisEventRecord) input);
            } else if (input instanceof DynamodbEvent.DynamodbStreamRecord) {
                processItem((DynamodbEvent.DynamodbStreamRecord) input);
            }
        }
        catch(Throwable t) {
                // put item in item failure list
        }

        // TODO - need a way of converting resultset back to return type. Defer to interface?

        throw new NotImplementedException();
    }

    default void processItem(SQSEvent.SQSMessage message) {
        System.out.println(message.getMessageId());
    }

    default void processItem(KinesisEvent.KinesisEventRecord record) {
        System.out.println(record.getEventID());
    }

    default void processItem(DynamodbEvent.DynamodbStreamRecord record) {
        System.out.println(record.getEventID());
    }
}
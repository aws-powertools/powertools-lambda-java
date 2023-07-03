package software.amazon.lambda.powertools.batch2;

import com.amazonaws.services.lambda.runtime.Context;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Generic interface for batch processing with Lambda. This interface provides 2 main methods:
 * <ul>
 *     <li>{@link #processBatch(Object, Context)} that will take a batch of items (messages, records),
 *     loop over it, and trigger the <b><code>processItem</code></b> function for each item.
 *     This method is also in charge to handle <a href="https://docs.aws.amazon.com/prescriptive-guidance/latest/lambda-event-filtering-partial-batch-responses-for-sqs/best-practices-partial-batch-responses.html">partial batch failures</a>.</li>
 *     <li>{@link #processItem(Object, Context)} that will handle each item of the batch. This method is generally implemented by the handler or any other class in charge of processing items.
 *     If there are exceptions within this method, you can let them go and the <b><code>processBatch</code></b> method will handle them and add the messages to the partial batch failures.</li>
 * </ul>
 *
 * @param <EVENT> Type of the incoming event. Natively supported:
 * <ul>
 *     <li>{@link com.amazonaws.services.lambda.runtime.events.SQSEvent}</li>
 *     <li>{@link com.amazonaws.services.lambda.runtime.events.KinesisEvent}</li>
 *     <li>{@link com.amazonaws.services.lambda.runtime.events.DynamodbEvent}</li>
 * </ul>
 * @param <ITEM> Type of the message or inner element in the message (e.g. body). Natively supported:
 * <ul>
 *     <li>{@link com.amazonaws.services.lambda.runtime.events.SQSEvent.SQSMessage}</li>
 *     <li>{@link com.amazonaws.services.lambda.runtime.events.KinesisEvent.KinesisEventRecord}</li>
 *     <li>{@link com.amazonaws.services.lambda.runtime.events.DynamodbEvent.DynamodbStreamRecord}</li>
 * </ul>
 * If you choose your own model type, the content of the body within the message will be deserialized.
 * <ul>
 *     <li>For SQS, it will be {@link com.amazonaws.services.lambda.runtime.events.SQSEvent.SQSMessage#getBody()}</li>
 *     <li>For Kinesis, it will be {@link com.amazonaws.services.lambda.runtime.events.models.kinesis.Record#getData()} (base 64 decoded)</li>
 *     <li>Not supported for DynamoDB</li>
 * </ul>
 * @param <RESPONSE> Type of the handler response. Natively supported:
 * <ul>
 *     <li>{@link com.amazonaws.services.lambda.runtime.events.SQSBatchResponse} for SQS.</li>
 *     <li>{@link com.amazonaws.services.lambda.runtime.events.StreamsEventResponse} for Kinesis and DynamoDB.</li>
 * </ul>
 */
public interface BatchProcessor<EVENT, ITEM, RESPONSE> {

    Logger BATCH_LOGGER = LoggerFactory.getLogger(BatchProcessor.class);

    /**
     * This method takes a batch of items (messages, records) from an event,
     * loop over it, and trigger the <b><code>processItem</code></b> function for each item.
     * @param event handler event (e.g. {@link com.amazonaws.services.lambda.runtime.events.SQSEvent})
     * @param context handler context
     * @return the handler response containing partial batch failures (e.g. {@link com.amazonaws.services.lambda.runtime.events.SQSBatchResponse})
     */
    RESPONSE processBatch(EVENT event, Context context);

    /**
     * This method handles each item of the batch. This method is generally implemented by the handler or any other class in charge of processing items.
     * If there are exceptions within this method, you can let them go and the <b><code>processBatch</code></b> method will handle them and add the messages to the partial batch failures.
     * This method has a default implementation because it's not available for DynamoDB.
     * @param item the item to process (e.g. {@link com.amazonaws.services.lambda.runtime.events.SQSEvent.SQSMessage})
     * @param context handler context
     */
    default void processItem(ITEM item, Context context) {
        BATCH_LOGGER.debug("[DEFAULT IMPLEMENTATION] Processing custom item");
    }

}
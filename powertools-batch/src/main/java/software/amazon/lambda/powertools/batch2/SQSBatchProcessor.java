package software.amazon.lambda.powertools.batch2;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.events.SQSBatchResponse;
import com.amazonaws.services.lambda.runtime.events.SQSBatchResponse.BatchItemFailure;
import com.amazonaws.services.lambda.runtime.events.SQSEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import software.amazon.lambda.powertools.utilities.EventDeserializer;
import sun.reflect.generics.reflectiveObjects.ParameterizedTypeImpl;

import java.util.ArrayList;

public interface SQSBatchProcessor<ITEM> extends BatchProcessor<SQSEvent, ITEM, SQSBatchResponse> {

    Logger SQS_BATCH_LOGGER = LoggerFactory.getLogger(SQSBatchProcessor.class);

    // The attribute on an SQS-FIFO message used to record the message group ID
    // https://docs.aws.amazon.com/lambda/latest/dg/with-sqs.html#sample-fifo-queues-message-event
    String MESSAGE_GROUP_ID_KEY = "MessageGroupId";

    @Override
    default SQSBatchResponse processBatch(SQSEvent event, Context context) {
        Class<ITEM> bodyClass = (Class<ITEM>) ((ParameterizedTypeImpl) getClass().getGenericInterfaces()[0]).getActualTypeArguments()[0];
        boolean isSQSMessage = bodyClass.getCanonicalName().equals(SQSEvent.SQSMessage.class.getCanonicalName());

        SQSBatchResponse response = SQSBatchResponse.builder().withBatchItemFailures(new ArrayList<>()).build();

        // If we are working on a FIFO queue, when any message fails we should stop processing and return the
        // rest of the batch as failed too. We use this variable to track when that has happened.
        // https://docs.aws.amazon.com/lambda/latest/dg/with-sqs.html#services-sqs-batchfailurereporting
        boolean failWholeBatch = false;

        int messageCursor = 0;
        for (; messageCursor < event.getRecords().size() && !failWholeBatch; messageCursor++) {
            SQSEvent.SQSMessage message = event.getRecords().get(messageCursor);

            String messageGroupId = message.getAttributes() != null ?
                    message.getAttributes().get(MESSAGE_GROUP_ID_KEY) : null;

            try {
                if (isSQSMessage) {
                    processMessage(message, context);
                } else {
                    processItem(EventDeserializer.extractDataFrom(message).as(bodyClass), context);
                }
            } catch (Throwable t) {
                SQS_BATCH_LOGGER.error("Error while processing message with messageId {}: {}, adding it to batch item failures", message.getMessageId(), t.getMessage());
                response.getBatchItemFailures().add(BatchItemFailure.builder().withItemIdentifier(message.getMessageId()).build());
                if (messageGroupId != null) {
                    failWholeBatch = true;
                    SQS_BATCH_LOGGER.info("A message in a batch with messageGroupId {} and messageId {} failed; failing the rest of the batch too"
                            , messageGroupId, message.getMessageId());
                }
            }
        }

        if (failWholeBatch) {
            // Add the remaining messages to the batch item failures
            event.getRecords()
                    .subList(messageCursor, event.getRecords().size())
                    .forEach(message -> response.getBatchItemFailures().add(BatchItemFailure.builder().withItemIdentifier(message.getMessageId()).build()));
        }
        return response;
    }

    default void processMessage(SQSEvent.SQSMessage message, Context context) {
        SQS_BATCH_LOGGER.debug("[DEFAULT IMPLEMENTATION] Processing message {}", message.getMessageId());
    }

}

package software.amazon.lambda.powertools.batch.handler;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.events.SQSBatchResponse;
import com.amazonaws.services.lambda.runtime.events.SQSEvent;
import software.amazon.lambda.powertools.utilities.EventDeserializer;
import sun.reflect.generics.reflectiveObjects.ParameterizedTypeImpl;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.function.BiConsumer;
import java.util.function.Consumer;

public class SqsBatchMessageHandler implements BatchMessageHandler<SQSEvent, SQSBatchResponse> {
    Logger SQS_BATCH_LOGGER = LoggerFactory.getLogger(SqsBatchMessageHandler.class);

    // The attribute on an SQS-FIFO message used to record the message group ID
    // https://docs.aws.amazon.com/lambda/latest/dg/with-sqs.html#sample-fifo-queues-message-event
    String MESSAGE_GROUP_ID_KEY = "MessageGroupId";

    private final BiConsumer<? extends Object, Context> messageHandler;
    private final BiConsumer<SQSEvent.SQSMessage, Context> rawMessageHandler;
    private final Consumer<SQSEvent.SQSMessage> successHandler;
    private final BiConsumer<SQSEvent.SQSMessage, Exception> failureHandler;
    private final Class<?> bodyClass;

    public SqsBatchMessageHandler(BiConsumer<?, Context> messageHandler, BiConsumer<SQSEvent.SQSMessage, Context> rawMessageHandler, Consumer<SQSEvent.SQSMessage> successHandler, BiConsumer<SQSEvent.SQSMessage, Exception> failureHandler) {
        this.messageHandler = messageHandler;
        this.rawMessageHandler = rawMessageHandler;
        this.successHandler = successHandler;
        this.failureHandler = failureHandler;

        // If we've got a message handler, work out once now what type we have to deserialize
        if (this.messageHandler != null) {
            this.bodyClass = (Class<?>) ((ParameterizedTypeImpl) messageHandler.getClass()
                    .getGenericInterfaces()[0])
                    .getActualTypeArguments()[0];
        } else {
            bodyClass = null;
        }
    }

    @Override
    public SQSBatchResponse processBatch(SQSEvent event, Context context) {
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
                if (this.rawMessageHandler != null) {
                    rawMessageHandler.accept(message, context);
                } else {
                    // TODO this is bad bad not good
                    // TODO fix
                    // TODO either with type bounds for the concrete consumer type on the builder (and buildWithHandler(..))
                    // TODO .... or by making this cast in the constructor
                    Object messageDeserialized = EventDeserializer.extractDataFrom(message).as(bodyClass);
                    BiConsumer<Object, Context> consumerCast = (BiConsumer<Object, Context>) messageHandler;
                    consumerCast.accept(messageDeserialized, context);
                }
            } catch (Throwable t) {
                SQS_BATCH_LOGGER.error("Error while processing message with messageId {}: {}, adding it to batch item failures", message.getMessageId(), t.getMessage());
                response.getBatchItemFailures().add(SQSBatchResponse.BatchItemFailure.builder().withItemIdentifier(message.getMessageId()).build());
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
                    .forEach(message -> response.getBatchItemFailures().add(SQSBatchResponse.BatchItemFailure.builder().withItemIdentifier(message.getMessageId()).build()));
        }
        return response;
    }
}

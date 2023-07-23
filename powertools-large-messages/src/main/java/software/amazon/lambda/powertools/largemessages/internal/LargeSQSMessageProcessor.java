package software.amazon.lambda.powertools.largemessages.internal;

import com.amazonaws.services.lambda.runtime.events.SQSEvent.MessageAttribute;
import com.amazonaws.services.lambda.runtime.events.SQSEvent.SQSMessage;

import java.util.HashMap;
import java.util.Map;

public class LargeSQSMessageProcessor extends LargeMessageProcessor<SQSMessage> {

    protected static final String LEGACY_RESERVED_ATTRIBUTE_NAME = "SQSLargePayloadSize";

    public LargeSQSMessageProcessor() {
    }

    @Override
    protected String getMessageContent(SQSMessage message) {
        return message.getBody();
    }

    @Override
    protected void updateMessageContent(SQSMessage message, String messageContent) {
        message.setBody(messageContent);
    }

    @Override
    protected boolean isLargeMessage(SQSMessage message) {
        Map<String, MessageAttribute> msgAttributes = message.getMessageAttributes();
        return msgAttributes != null && (msgAttributes.containsKey(RESERVED_ATTRIBUTE_NAME) || msgAttributes.containsKey(LEGACY_RESERVED_ATTRIBUTE_NAME));
    }

    @Override
    protected void removeLargeMessageAttributes(SQSMessage message) {
        // message.getMessageAttributes() does not support remove operation, copy to new map
        Map<String, MessageAttribute> newAttributes = new HashMap<>(message.getMessageAttributes());
        newAttributes.remove(RESERVED_ATTRIBUTE_NAME);
        newAttributes.remove(LEGACY_RESERVED_ATTRIBUTE_NAME);
        message.setMessageAttributes(newAttributes);
    }
}

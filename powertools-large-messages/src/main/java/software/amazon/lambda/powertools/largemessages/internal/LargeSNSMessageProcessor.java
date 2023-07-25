package software.amazon.lambda.powertools.largemessages.internal;

import com.amazonaws.services.lambda.runtime.events.SNSEvent.MessageAttribute;
import com.amazonaws.services.lambda.runtime.events.SNSEvent.SNSRecord;

import java.util.HashMap;
import java.util.Map;

public class LargeSNSMessageProcessor extends LargeMessageProcessor<SNSRecord> {

    @Override
    protected String getMessageId(SNSRecord message) {
        return message.getSNS().getMessageId();
    }

    @Override
    protected String getMessageContent(SNSRecord message) {
        return message.getSNS().getMessage();
    }

    @Override
    protected void updateMessageContent(SNSRecord message, String messageContent) {
        message.getSNS().setMessage(messageContent);
    }

    @Override
    protected boolean isLargeMessage(SNSRecord message) {
        Map<String, MessageAttribute> msgAttributes = message.getSNS().getMessageAttributes();
        return msgAttributes != null && msgAttributes.containsKey(RESERVED_ATTRIBUTE_NAME);
    }

    @Override
    protected void removeLargeMessageAttributes(SNSRecord message) {
        // message.getSNS().getMessageAttributes() does not support remove operation, copy to new map
        Map<String, MessageAttribute> newAttributes = new HashMap<>(message.getSNS().getMessageAttributes());
        newAttributes.remove(RESERVED_ATTRIBUTE_NAME);
        message.getSNS().setMessageAttributes(newAttributes);
    }
}

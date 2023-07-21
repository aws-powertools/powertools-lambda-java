package software.amazon.lambda.powertools.largemessages.internal;

import com.amazonaws.services.lambda.runtime.events.SNSEvent.SNSRecord;

public class LargeSNSMessageProcessor extends LargeMessageProcessor<SNSRecord> {

    public LargeSNSMessageProcessor() {
    }

    @Override
    protected String getMessageContent(SNSRecord message) {
        return message.getSNS().getMessage();
    }

    @Override
    protected void updateMessageContent(SNSRecord message, String messageContent) {
        message.getSNS().setMessage(messageContent);
    }
}

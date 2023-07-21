package software.amazon.lambda.powertools.largemessages.internal;

import com.amazonaws.services.lambda.runtime.events.SQSEvent.SQSMessage;

public class LargeSQSMessageProcessor extends LargeMessageProcessor<SQSMessage> {

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
}

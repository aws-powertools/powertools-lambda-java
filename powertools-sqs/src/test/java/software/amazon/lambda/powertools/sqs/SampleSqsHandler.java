package software.amazon.lambda.powertools.sqs;

import com.amazonaws.services.lambda.runtime.events.SQSEvent;

public class SampleSqsHandler implements SqsMessageHandler<String> {
    private int counter;

    @Override
    public String process(SQSEvent.SQSMessage message) {
        return String.valueOf(counter++);
    }
}

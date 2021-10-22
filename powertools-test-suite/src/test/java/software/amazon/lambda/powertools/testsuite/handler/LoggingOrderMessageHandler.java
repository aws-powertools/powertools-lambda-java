package software.amazon.lambda.powertools.testsuite.handler;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestHandler;
import com.amazonaws.services.lambda.runtime.events.SQSEvent;
import software.amazon.lambda.powertools.logging.Logging;
import software.amazon.lambda.powertools.sqs.SqsLargeMessage;

public class LoggingOrderMessageHandler implements RequestHandler<SQSEvent, String> {

    @Override
    @SqsLargeMessage
    @Logging(logEvent = true)
    public String handleRequest(SQSEvent sqsEvent, Context context) {
        return sqsEvent.getRecords().get(0).getBody();
    }
}

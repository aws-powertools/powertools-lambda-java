package helloworld;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestHandler;
import com.amazonaws.services.lambda.runtime.events.SQSEvent;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import software.amazon.lambda.powertools.logging.Logging;
import software.amazon.lambda.powertools.sqs.SqsBatch;
import software.amazon.lambda.powertools.sqs.SqsMessageHandler;

import static com.amazonaws.services.lambda.runtime.events.SQSEvent.SQSMessage;

public class AppSqsEvent implements RequestHandler<SQSEvent, String> {
    private static final Logger log = LogManager.getLogger(AppSqsEvent.class);

    @SqsBatch(SampleMessageHandler.class)
    @Logging(logEvent = true)
    @Override
    public String handleRequest(SQSEvent input, Context context) {
        return "{\"statusCode\": 200}";
    }

    public static class SampleMessageHandler implements SqsMessageHandler<Object> {

        @Override
        public String process(SQSMessage message) {
            if("19dd0b57-b21e-4ac1-bd88-01bbb068cb99".equals(message.getMessageId())) {
                throw new RuntimeException(message.getMessageId());
            }
            log.info("Processing message with details {}", message);
            return message.getMessageId();
        }
    }
}

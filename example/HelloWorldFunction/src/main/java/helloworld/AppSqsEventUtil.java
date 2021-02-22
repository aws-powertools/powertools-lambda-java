package helloworld;

import java.util.List;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestHandler;
import com.amazonaws.services.lambda.runtime.events.SQSEvent;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import software.amazon.lambda.powertools.sqs.SqsUtils;
import software.amazon.lambda.powertools.sqs.SQSBatchProcessingException;

import static java.util.Collections.emptyList;

public class AppSqsEventUtil implements RequestHandler<SQSEvent, List<String>> {
    private static final Logger log = LogManager.getLogger(AppSqsEventUtil.class);

    @Override
    public List<String> handleRequest(SQSEvent input, Context context) {
        try {

            return SqsUtils.batchProcessor(input, (message) -> {
                if ("19dd0b57-b21e-4ac1-bd88-01bbb068cb99".equals(message.getMessageId())) {
                    throw new RuntimeException(message.getMessageId());
                }

                log.info("Processing message with details {}", message);
                return message.getMessageId();
            });

        } catch (SQSBatchProcessingException e) {
            log.info("Exception details {}", e.getMessage(), e);
            log.info("Success message Returns{}", e.successMessageReturnValues());
            log.info("Failed messages {}", e.getFailures());
            log.info("Failed messages Reasons {}", e.getExceptions());
            return emptyList();
        }
    }
}

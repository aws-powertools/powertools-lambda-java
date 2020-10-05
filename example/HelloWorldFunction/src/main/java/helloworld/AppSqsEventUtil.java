package helloworld;

import java.util.List;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestHandler;
import com.amazonaws.services.lambda.runtime.events.SQSEvent;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import software.amazon.lambda.powertools.sqs.PowertoolsSqs;
import software.amazon.lambda.powertools.sqs.SQSBatchProcessingException;

import static java.util.Collections.emptyList;

public class AppSqsEventUtil implements RequestHandler<SQSEvent, List<String>> {
    private static final Logger LOG = LogManager.getLogger(AppSqsEventUtil.class);

    @Override
    public List<String> handleRequest(SQSEvent input, Context context) {
        try {

            return PowertoolsSqs.batchProcessor(input, (message) -> {
                if ("19dd0b57-b21e-4ac1-bd88-01bbb068cb99".equals(message.getMessageId())) {
                    throw new RuntimeException(message.getMessageId());
                }

                LOG.info("Processing message with details {}", message);
                return message.getMessageId();
            });

        } catch (SQSBatchProcessingException e) {
            LOG.info("Exception details {}", e.getMessage(), e);
            LOG.info("Success message Returns{}", e.successMessageReturnValues());
            LOG.info("Failed messages {}", e.getFailures());
            LOG.info("Failed messages Reasons {}", e.getExceptions());
            return emptyList();
        }
    }
}

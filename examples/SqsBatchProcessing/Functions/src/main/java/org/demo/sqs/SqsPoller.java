package org.demo.sqs;

import java.util.Random;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestHandler;
import com.amazonaws.services.lambda.runtime.events.SQSEvent;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import software.amazon.awssdk.http.urlconnection.UrlConnectionHttpClient;
import software.amazon.awssdk.services.sqs.SqsClient;
import software.amazon.lambda.powertools.logging.Logging;
import software.amazon.lambda.powertools.sqs.SqsBatch;
import software.amazon.lambda.powertools.sqs.SqsMessageHandler;
import software.amazon.lambda.powertools.sqs.SqsUtils;

import static com.amazonaws.services.lambda.runtime.events.SQSEvent.SQSMessage;

/**
 * Handler for requests to Lambda function.
 */
public class SqsPoller implements RequestHandler<SQSEvent, String> {

    Logger log = LogManager.getLogger(SqsPoller.class);
    Random random = new Random(100);

    static {
        // https://docs.aws.amazon.com/sdk-for-java/latest/developer-guide/lambda-optimize-starttime.html
        SqsUtils.overrideSqsClient(SqsClient.builder()
                .httpClient(UrlConnectionHttpClient.create())
                .build());
    }

    @SqsBatch(value = BatchProcessor.class, nonRetryableExceptions = {IllegalArgumentException.class})
    @Logging(logEvent = true)
    public String handleRequest(final SQSEvent input, final Context context) {
        return "Success";
    }

    private class BatchProcessor implements SqsMessageHandler<Object> {
        @Override
        public String process(SQSMessage message) {
            log.info("Processing message with id {}", message.getMessageId());

            int nextInt = random.nextInt(100);

            if(nextInt <= 10) {
                log.info("Randomly picked message with id {} as business validation failure.", message.getMessageId());
                throw new IllegalArgumentException("Failed business validation. No point of retrying. Move me to DLQ." + message.getMessageId());
            }

            if(nextInt > 90) {
                log.info("Randomly picked message with id {} as intermittent failure.", message.getMessageId());
                throw new RuntimeException("Failed due to intermittent issue. Will be sent back for retry." + message.getMessageId());
            }

            return "Success";
        }
    }
}
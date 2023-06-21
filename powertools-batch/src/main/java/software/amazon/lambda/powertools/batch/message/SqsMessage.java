package software.amazon.lambda.powertools.batch.message;

import java.util.Map;
import com.amazonaws.services.lambda.runtime.events.SQSEvent.MessageAttribute;

public class SqsMessage extends BatchProcessorMessage {

    private String receiptHandle;
    private String body;
    private String md5OfBody;
    private String md5OfMessageAttributes;
    private Map<String, String> attributes;
    private Map<String, MessageAttribute> messageAttributes;
}

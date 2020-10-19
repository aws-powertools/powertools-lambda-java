package software.amazon.lambda.powertools.validation.handlers;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestHandler;
import com.amazonaws.services.lambda.runtime.events.SQSEvent;
import software.amazon.lambda.powertools.validation.Validation;

public class SQSHandler implements RequestHandler<SQSEvent, String> {

    @Override
    @Validation(inboundSchema = "classpath:/schema_v7.json")
    public String handleRequest(SQSEvent input, Context context) {
        return "OK";
    }
}

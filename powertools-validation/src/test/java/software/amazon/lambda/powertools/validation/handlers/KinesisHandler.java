package software.amazon.lambda.powertools.validation.handlers;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestHandler;
import com.amazonaws.services.lambda.runtime.events.KinesisEvent;
import software.amazon.lambda.powertools.validation.Validation;

public class KinesisHandler implements RequestHandler<KinesisEvent, String> {

    @Validation(inboundSchema = "classpath:/schema_v7.json")
    @Override
    public String handleRequest(KinesisEvent input, Context context) {
        return "OK";
    }
}

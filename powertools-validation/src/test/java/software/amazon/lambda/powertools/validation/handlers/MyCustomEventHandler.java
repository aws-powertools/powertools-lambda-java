package software.amazon.lambda.powertools.validation.handlers;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestHandler;
import software.amazon.lambda.powertools.validation.Validation;
import software.amazon.lambda.powertools.validation.model.MyCustomEvent;

public class MyCustomEventHandler implements RequestHandler<MyCustomEvent, String> {

    @Override
    @Validation(inboundSchema = "classpath:/schema_v7.json", envelope = "basket.products[*]")
    public String handleRequest(MyCustomEvent input, Context context) {
        return "OK";
    }
}

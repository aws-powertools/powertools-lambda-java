package software.amazon.lambda.powertools.sqs.handlers;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestHandler;
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyRequestEvent;
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyResponseEvent;
import software.amazon.lambda.powertools.sqs.LargeMessageHandler;

public class LambdaHandlerApiGateway implements RequestHandler<APIGatewayProxyRequestEvent, String> {

    @Override
    @LargeMessageHandler
    public String handleRequest(APIGatewayProxyRequestEvent sqsEvent, Context context) {
        return sqsEvent.getBody();
    }
}

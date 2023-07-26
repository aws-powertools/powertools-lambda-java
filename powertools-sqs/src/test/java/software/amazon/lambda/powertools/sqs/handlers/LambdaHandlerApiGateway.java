package software.amazon.lambda.powertools.sqs.handlers;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestHandler;
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyRequestEvent;
import software.amazon.lambda.powertools.sqs.SampleSqsHandler;
import software.amazon.lambda.powertools.sqs.SqsBatch;
import software.amazon.lambda.powertools.sqs.SqsLargeMessage;

public class LambdaHandlerApiGateway implements RequestHandler<APIGatewayProxyRequestEvent, String> {

    @Override
    @SqsLargeMessage
    @SqsBatch(value = SampleSqsHandler.class)
    public String handleRequest(APIGatewayProxyRequestEvent sqsEvent, Context context) {
        return sqsEvent.getBody();
    }
}

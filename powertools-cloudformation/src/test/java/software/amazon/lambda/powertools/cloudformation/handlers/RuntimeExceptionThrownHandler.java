package software.amazon.lambda.powertools.cloudformation.handlers;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.events.CloudFormationCustomResourceEvent;
import software.amazon.lambda.powertools.cloudformation.AbstractCustomResourceHandler;
import software.amazon.lambda.powertools.cloudformation.Response;

public class RuntimeExceptionThrownHandler extends AbstractCustomResourceHandler {

    @Override
    protected Response create(CloudFormationCustomResourceEvent event, Context context) {
        throw new RuntimeException("failure");
    }

    @Override
    protected Response update(CloudFormationCustomResourceEvent event, Context context) {
        throw new RuntimeException("failure");
    }

    @Override
    protected Response delete(CloudFormationCustomResourceEvent event, Context context) {
        return null;
    }
}

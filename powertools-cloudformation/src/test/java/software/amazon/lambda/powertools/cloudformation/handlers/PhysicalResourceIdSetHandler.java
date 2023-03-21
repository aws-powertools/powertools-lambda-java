package software.amazon.lambda.powertools.cloudformation.handlers;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.events.CloudFormationCustomResourceEvent;
import software.amazon.lambda.powertools.cloudformation.AbstractCustomResourceHandler;
import software.amazon.lambda.powertools.cloudformation.Response;

public class PhysicalResourceIdSetHandler extends AbstractCustomResourceHandler {

    private final String physicalResourceId;
    private final boolean callsSucceed;

    public PhysicalResourceIdSetHandler(String physicalResourceId, boolean callsSucceed) {
        this.physicalResourceId = physicalResourceId;
        this.callsSucceed = callsSucceed;
    }

    @Override
    protected Response create(CloudFormationCustomResourceEvent event, Context context) {
        return callsSucceed? Response.success(physicalResourceId) : Response.failed(physicalResourceId);
    }

    @Override
    protected Response update(CloudFormationCustomResourceEvent event, Context context) {
        return callsSucceed? Response.success(physicalResourceId) : Response.failed(physicalResourceId);
    }

    @Override
    protected Response delete(CloudFormationCustomResourceEvent event, Context context) {
        return callsSucceed? Response.success(physicalResourceId) : Response.failed(physicalResourceId);
    }
}

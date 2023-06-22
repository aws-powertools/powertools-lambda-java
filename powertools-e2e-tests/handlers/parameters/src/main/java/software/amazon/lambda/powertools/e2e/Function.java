package software.amazon.lambda.powertools.e2e;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestHandler;
import software.amazon.lambda.powertools.logging.Logging;
import software.amazon.lambda.powertools.parameters.AppConfigProvider;
import software.amazon.lambda.powertools.parameters.ParamManager;

public class Function implements RequestHandler<Input, String> {

    @Logging
    public String handleRequest(Input input, Context context) {
        AppConfigProvider provider = ParamManager.getAppConfigProvider(input.getEnvironment(), input.getApp());
        return provider.get(input.getKey());

    }
}
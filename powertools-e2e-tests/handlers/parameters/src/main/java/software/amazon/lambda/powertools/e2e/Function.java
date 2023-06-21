package software.amazon.lambda.powertools.e2e;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestHandler;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import software.amazon.lambda.powertools.logging.Logging;
import software.amazon.lambda.powertools.parameters.ParamManager;
import software.amazon.lambda.powertools.parameters.cache.CacheManager;
import software.amazon.lambda.powertools.parameters.AppConfigProvider;

public class Function implements RequestHandler<Input, String> {

    private static final Logger LOG = LogManager.getLogger(Function.class);

    @Logging
    public String handleRequest(Input input, Context context) {
        AppConfigProvider provider = ParamManager.getAppConfigProvider(input.getEnvironment(), input.getApp());
        return provider.get(input.getKey());

    }
}
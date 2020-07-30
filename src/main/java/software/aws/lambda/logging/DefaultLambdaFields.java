package software.aws.lambda.logging;

import com.amazonaws.services.lambda.runtime.Context;

import java.util.HashMap;
import java.util.Map;

enum DefaultLambdaFields {
    FUNCTION_NAME("functionName"),
    FUNCTION_VERSION("functionVersion"),
    FUNCTION_ARN("functionArn"),
    FUNCTION_MEMORY_SIZE("functionMemorySize");

    private String name;

    DefaultLambdaFields(String name) {
        this.name = name;
    }

    static Map<String, String> values(Context context) {
        Map<String, String> hashMap = new HashMap<>();

        hashMap.put(FUNCTION_NAME.name, context.getFunctionName());
        hashMap.put(FUNCTION_VERSION.name, context.getFunctionVersion());
        hashMap.put(FUNCTION_ARN.name, context.getInvokedFunctionArn());
        hashMap.put(FUNCTION_MEMORY_SIZE.name, String.valueOf(context.getMemoryLimitInMB()));

        return hashMap;
    }
}

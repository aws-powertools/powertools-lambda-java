package software.amazon.lambda.powertools.parameters.internal;

import java.util.Base64;
import java.util.HashMap;
import java.util.Map;
import software.amazon.lambda.powertools.parameters.BaseProvider;
import software.amazon.lambda.powertools.parameters.cache.CacheManager;

public class CustomProvider extends BaseProvider {

    private final Map<String, String> values = new HashMap<>();

    public CustomProvider(CacheManager cacheManager) {
        super(cacheManager);
        values.put("/simple", "value");
        values.put("/base64", Base64.getEncoder().encodeToString("value".getBytes()));
        values.put("/json", "{\"foo\":\"Foo\", \"bar\":42, \"baz\":123456789}");
    }

    @Override
    protected String getValue(String key) {
        return values.get(key);
    }

    @Override
    protected Map<String, String> getMultipleValues(String path) {
        return null;
    }
}

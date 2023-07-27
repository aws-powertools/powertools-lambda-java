/*
 * Copyright 2023 Amazon.com, Inc. or its affiliates.
 * Licensed under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *     http://www.apache.org/licenses/LICENSE-2.0
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 */

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

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

package software.amazon.lambda.powertools.parameters.secrets;

import static java.nio.charset.StandardCharsets.UTF_8;

import java.util.Base64;
import java.util.Map;
import software.amazon.awssdk.services.secretsmanager.SecretsManagerClient;
import software.amazon.awssdk.services.secretsmanager.model.GetSecretValueRequest;
import software.amazon.lambda.powertools.parameters.BaseProvider;
import software.amazon.lambda.powertools.parameters.cache.CacheManager;
import software.amazon.lambda.powertools.parameters.transform.TransformationManager;

/**
 * AWS Secrets Manager Parameter Provider<br/><br/>
 *
 * <u>Samples:</u>
 * <pre>
 *     SecretsProvider provider = SecretsProvider.builder().build();
 *
 *     String value = provider.get("key");
 *     System.out.println(value);
 *     >>> "value"
 *
 *     // Get a value and cache it for 30 seconds (all others values will now be cached for 30 seconds)
 *     String value = provider.defaultMaxAge(30, ChronoUnit.SECONDS).get("key");
 *
 *     // Get a value and cache it for 1 minute (all others values are cached for 5 seconds by default)
 *     String value = provider.withMaxAge(1, ChronoUnit.MINUTES).get("key");
 *
 *     // Get a base64 encoded value, decoded into a String, and store it in the cache
 *     String value = provider.withTransformation(Transformer.base64).get("key");
 *
 *     // Get a json value, transform it into an Object, and store it in the cache
 *     TargetObject = provider.withTransformation(Transformer.json).get("key", TargetObject.class);
 * </pre>
 */
public class SecretsProvider extends BaseProvider {

    private final SecretsManagerClient client;

    /**
     * Use the {@link SecretsProviderBuilder} to create an instance!
     *
     * @param client custom client you would like to use.
     */
    SecretsProvider(CacheManager cacheManager, TransformationManager transformationManager,
                    SecretsManagerClient client) {
        super(cacheManager, transformationManager);
        this.client = client;
    }

    /**
     * Create a builder that can be used to configure and create a {@link SecretsProvider}.
     *
     * @return a new instance of {@link SecretsProviderBuilder}
     */
    public static SecretsProviderBuilder builder() {
        return new SecretsProviderBuilder();
    }

    /**
     * Retrieve the parameter value from the AWS Secrets Manager.
     *
     * @param key key of the parameter
     * @return the value of the parameter identified by the key
     */
    @Override
    protected String getValue(String key) {
        GetSecretValueRequest request = GetSecretValueRequest.builder().secretId(key).build();

        String secretValue = client.getSecretValue(request).secretString();
        if (secretValue == null) {
            secretValue =
                    new String(Base64.getDecoder().decode(client.getSecretValue(request).secretBinary().asByteArray()),
                            UTF_8);
        }
        return secretValue;
    }

    /**
     * @throws UnsupportedOperationException as it is not possible to get multiple values simultaneously from Secrets Manager
     */
    @Override
    protected Map<String, String> getMultipleValues(String path) {
        throw new UnsupportedOperationException("Impossible to get multiple values from AWS Secrets Manager");
    }


    // For test purpose only
    SecretsManagerClient getClient() {
        return client;
    }

}

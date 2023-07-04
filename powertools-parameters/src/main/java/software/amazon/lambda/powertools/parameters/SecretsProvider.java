/*
 * Copyright 2020 Amazon.com, Inc. or its affiliates.
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
package software.amazon.lambda.powertools.parameters;

import java.time.temporal.ChronoUnit;
import java.util.Base64;
import java.util.Map;

import software.amazon.awssdk.auth.credentials.EnvironmentVariableCredentialsProvider;
import software.amazon.awssdk.core.SdkSystemSetting;
import software.amazon.awssdk.http.urlconnection.UrlConnectionHttpClient;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.secretsmanager.SecretsManagerClient;
import software.amazon.awssdk.services.secretsmanager.SecretsManagerClientBuilder;
import software.amazon.awssdk.services.secretsmanager.model.GetSecretValueRequest;
import software.amazon.lambda.powertools.core.internal.LambdaConstants;
import software.amazon.lambda.powertools.parameters.cache.CacheManager;
import software.amazon.lambda.powertools.parameters.transform.TransformationManager;
import software.amazon.lambda.powertools.parameters.transform.Transformer;

import static java.nio.charset.StandardCharsets.UTF_8;
import static software.amazon.lambda.powertools.core.internal.LambdaConstants.AWS_LAMBDA_INITIALIZATION_TYPE;

/**
 * AWS Secrets Manager Parameter Provider<br/><br/>
 *
 * <u>Samples:</u>
 * <pre>
 *     SecretsProvider provider = ParamManager.getSecretsProvider();
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

    private SecretsManagerClient client;

    /**
     * Constructor with custom {@link SecretsManagerClient}. <br/>
     * Use when you need to customize region or any other attribute of the client.<br/><br/>
     *
     * Use the {@link Builder} to create an instance of it.
     *
     * @param client custom client you would like to use.
     */
    SecretsProvider(CacheManager cacheManager, SecretsManagerClient client) {
        super(cacheManager);
        this.client = client;
    }

    /**
     * Constructor
     *
     * @param cacheManager handles the parameter caching
     */
    SecretsProvider(CacheManager cacheManager) {
        super(cacheManager);
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
            secretValue = new String(Base64.getDecoder().decode(client.getSecretValue(request).secretBinary().asByteArray()), UTF_8);
        }
        return secretValue;
    }

    /**
     *
     * @throws UnsupportedOperationException as it is not possible to get multiple values simultaneously from Secrets Manager
     */
    @Override
    protected Map<String, String> getMultipleValues(String path) {
        throw new UnsupportedOperationException("Impossible to get multiple values from AWS Secrets Manager");
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public SecretsProvider defaultMaxAge(int maxAge, ChronoUnit unit) {
        super.defaultMaxAge(maxAge, unit);
        return this;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public SecretsProvider withMaxAge(int maxAge, ChronoUnit unit) {
        super.withMaxAge(maxAge, unit);
        return this;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public SecretsProvider withTransformation(Class<? extends Transformer> transformerClass) {
        super.withTransformation(transformerClass);
        return this;
    }

    /**
     * Create a builder that can be used to configure and create a {@link SecretsProvider}.
     *
     * @return a new instance of {@link SecretsProvider.Builder}
     */
    public static Builder builder() {
        return new Builder();
    }

    static class Builder {

        private SecretsManagerClient client;
        private CacheManager cacheManager;
        private TransformationManager transformationManager;

        /**
         * Create a {@link SecretsProvider} instance.
         *
         * @return a {@link SecretsProvider}
         */
        public SecretsProvider build() {
            if (cacheManager == null) {
                throw new IllegalStateException("No CacheManager provided, please provide one");
            }
            SecretsProvider provider;
            if (client == null) {
                SecretsManagerClientBuilder secretsManagerClientBuilder = SecretsManagerClient.builder()
                        .httpClientBuilder(UrlConnectionHttpClient.builder())
                        .region(Region.of(System.getenv(SdkSystemSetting.AWS_REGION.environmentVariable())));

                // AWS_LAMBDA_INITIALIZATION_TYPE has two values on-demand and snap-start
                // when using snap-start mode, the env var creds provider isn't used and causes a fatal error if set
                // fall back to the default provider chain if the mode is anything other than on-demand.
                String initializationType = System.getenv().get(AWS_LAMBDA_INITIALIZATION_TYPE);
                if (initializationType  != null && initializationType.equals(LambdaConstants.ON_DEMAND)) {
                    secretsManagerClientBuilder.credentialsProvider(EnvironmentVariableCredentialsProvider.create());
                }

                client = secretsManagerClientBuilder.build();
            }

            provider = new SecretsProvider(cacheManager, client);

            if (transformationManager != null) {
                provider.setTransformationManager(transformationManager);
            }
            return provider;
        }

        /**
         * Set custom {@link SecretsManagerClient} to pass to the {@link SecretsProvider}. <br/>
         * Use it if you want to customize the region or any other part of the client.
         *
         * @param client Custom client
         * @return the builder to chain calls (eg. <pre>builder.withClient().build()</pre>)
         */
        public Builder withClient(SecretsManagerClient client) {
            this.client = client;
            return this;
        }

        /**
         * <b>Mandatory</b>. Provide a CacheManager to the {@link SecretsProvider}
         *
         * @param cacheManager the manager that will handle the cache of parameters
         * @return the builder to chain calls (eg. <pre>builder.withCacheManager().build()</pre>)
         */
        public Builder withCacheManager(CacheManager cacheManager) {
            this.cacheManager = cacheManager;
            return this;
        }

        /**
         * Provide a transformationManager to the {@link SecretsProvider}
         *
         * @param transformationManager the manager that will handle transformation of parameters
         * @return the builder to chain calls (eg. <pre>builder.withTransformationManager().build()</pre>)
         */
        public Builder withTransformationManager(TransformationManager transformationManager) {
            this.transformationManager = transformationManager;
            return this;
        }
    }
}

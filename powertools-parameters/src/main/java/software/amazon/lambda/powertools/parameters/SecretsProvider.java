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

import software.amazon.awssdk.services.secretsmanager.SecretsManagerClient;
import software.amazon.awssdk.services.secretsmanager.model.GetSecretValueRequest;

import java.util.Base64;

/**
 * AWS Secrets Manager Parameter Provider
 */
public class SecretsProvider extends BaseProvider {

    private final SecretsManagerClient client;

    /**
     * Default constructor with default {@link SecretsManagerClient}. <br/>
     * Use when you don't need to customize region or any other attribute of the client.<br/><br/>
     *
     * Use the {@link Builder} to create an instance of it.
     */
    SecretsProvider() {
        this.client = SecretsManagerClient.create();
    }

    /**
     * Constructor with custom {@link SecretsManagerClient}. <br/>
     * Use when you need to customize region or any other attribute of the client.<br/><br/>
     *
     * Use the {@link Builder} to create an instance of it.
     *
     * @param client custom client you would like to use.
     */
    SecretsProvider(SecretsManagerClient client) {
        this.client = client;
    }

    /**
     * Retrieve the parameter value from the AWS Secrets Manager.
     *
     * @param key key of the parameter
     * @return the value of the parameter identified by the key
     */
    @Override
    String getValue(String key) {
        GetSecretValueRequest request = GetSecretValueRequest.builder().secretId(key).build();

        String secretValue = client.getSecretValue(request).secretString();
        if (secretValue == null) {
            secretValue = new String(Base64.getDecoder().decode(client.getSecretValue(request).secretBinary().asByteArray()));
        }
        return secretValue;
    }

    /**
     * Use this static method to create an instance of {@link SecretsProvider} with default {@link SecretsManagerClient}
     *
     * @return a new instance of {@link SecretsProvider}
     */
    public static SecretsProvider create() {
        return builder().build();
    }

    /**
     * Create a builder that can be used to configure and create a {@link SecretsProvider}.
     *
     * @return a new instance of {@link Builder}
     */
    public static Builder builder() {
        return new Builder();
    }

    static class Builder {

        private SecretsManagerClient client;

        /**
         * Create a {@link SecretsProvider} instance.
         *
         * @return a {@link SecretsProvider}
         */
        public SecretsProvider build() {
            if (client != null) {
                return new SecretsProvider(client);
            }
            return new SecretsProvider();
        }

        /**
         * Set custom {@link SecretsManagerClient} to pass to the {@link SecretsProvider}. <br/>
         * Use it if you want to customize the region or any other part of the client.
         *
         * @param client Custom client
         * @return the builder to chain calls (eg. <code>builder.withClient().build()</code>)
         */
        public Builder withClient(SecretsManagerClient client) {
            this.client = client;
            return this;
        }
    }
}

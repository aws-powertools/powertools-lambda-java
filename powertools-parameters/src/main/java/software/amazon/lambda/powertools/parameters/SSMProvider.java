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

import software.amazon.awssdk.services.ssm.SsmClient;
import software.amazon.awssdk.services.ssm.model.GetParameterRequest;

/**
 * AWS Systems Manager Parameter Store Provider
 */
public class SSMProvider extends BaseProvider {

    private final SsmClient client;
    private boolean decrypt = false;

    /**
     * Default constructor with default {@link SsmClient}. <br/>
     * Use when you don't need to customize region or any other attribute of the client.<br/><br/>
     *
     * Use the {@link SSMProvider.Builder} to create an instance of it.
     */
    SSMProvider() {
        this(SsmClient.create());
    }

    /**
     * Constructor with custom {@link SsmClient}. <br/>
     * Use when you need to customize region or any other attribute of the client.<br/><br/>
     *
     * Use the {@link SSMProvider.Builder} to create an instance of it.
     *
     * @param client custom client you would like to use.
     */
    SSMProvider(SsmClient client) {
        this.client = client;
    }

    /**
     * Retrieve the parameter value from the AWS System Manager Parameter Store.
     *
     * @param key key of the parameter
     * @return the value of the parameter identified by the key
     */
    @Override
    String getValue(String key) {
        GetParameterRequest request = GetParameterRequest.builder()
                                        .name(key)
                                        .withDecryption(decrypt)
                                        .build();
        return client.getParameter(request).parameter().value();
    }

    public <T extends BaseProvider> BaseProvider withDecryption(boolean decrypt) {
        this.decrypt = decrypt;
        return this;
    }

    @Override
    protected void resetToDefaults() {
        super.resetToDefaults();
        decrypt = false;
    }

    /**
     * Use this static method to create an instance of {@link SSMProvider} with default {@link SsmClient}
     *
     * @return a new instance of {@link SSMProvider}
     */
    public static SSMProvider create() {
        return builder().build();
    }

    /**
     * Create a builder that can be used to configure and create a {@link SSMProvider}.
     *
     * @return a new instance of {@link SSMProvider.Builder}
     */
    public static Builder builder() {
        return new Builder();
    }

    static class Builder {
        private SsmClient client;

        /**
         * Create a {@link SSMProvider} instance.
         *
         * @return a {@link SSMProvider}
         */
        public SSMProvider build() {
            if (client != null) {
                return new SSMProvider(client);
            }
            return new SSMProvider();
        }

        /**
         * Set custom {@link SsmClient} to pass to the {@link SSMProvider}. <br/>
         * Use it if you want to customize the region or any other part of the client.
         *
         * @param client Custom client
         * @return the builder to chain calls (eg. <code>builder.withClient().build()</code>)
         */
        public Builder withClient(SsmClient client) {
            this.client = client;
            return this;
        }
    }
}

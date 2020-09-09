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
import software.amazon.awssdk.services.ssm.model.GetParametersByPathRequest;
import software.amazon.awssdk.services.ssm.model.GetParametersByPathResponse;
import software.amazon.awssdk.utils.StringUtils;

import java.util.HashMap;
import java.util.Map;

/**
 * AWS Systems Manager Parameter Store Provider
 */
public class SSMProvider extends BaseProvider {

    private final SsmClient client;
    private boolean decrypt = false;
    private boolean recursive = false;

    /**
     * Default constructor with default {@link SsmClient}. <br/>
     * Use when you don't need to customize region or any other attribute of the client.<br/><br/>
     * <p>
     * Use the {@link SSMProvider.Builder} to create an instance of it.
     */
    SSMProvider() {
        this(SsmClient.create());
    }

    /**
     * Constructor with custom {@link SsmClient}. <br/>
     * Use when you need to customize region or any other attribute of the client.<br/><br/>
     * <p>
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

    /**
     * Tells System Manager Parameter Store to decrypt the parameter value.<br/>
     * By default, parameter values are not decrypted.<br/>
     * Valid both for get and getMultiple.
     * @return the provider itself in order to chain calls (eg. <code>provider.withDecryption().get("key")</code>).
     */
    public BaseProvider withDecryption() {
        this.decrypt = true;
        return this;
    }

    /**
     * Tells System Manager Parameter Store to retrieve all parameters starting with a path (all levels)<br/>
     * Only used with {@link #getMultiple(String)}.
     * @return the provider itself in order to chain calls (eg. <code>provider.recursive().getMultiple("key")</code>).
     */
    public BaseProvider recursive() {
        this.recursive = true;
        return this;
    }

    /**
     * Retrieve multiple parameter values from AWS System Manager Parameter Store.<br/>
     * Retrieve all parameters starting with the path provided in parameter.<br/>
     * eg. getMultiple("/foo/bar") will retrieve /foo/bar/baz, foo/bar/biz<br/>
     * Using {@link #recursive()}, getMultiple("/foo/bar") will retrieve /foo/bar/baz, foo/bar/biz and foo/bar/buz/boz<br/>
     * <i>Does not support transformation.</i>
     *
     * @param path path of the parameter
     * @return a map containing parameters keys and values. The key is a subpart of the path<br/>
     * eg. getMultiple("/foo/bar") will retrieve [key="baz", value="valuebaz"] for parameter "/foo/bar/baz"
     */
    public Map<String, String> getMultiple(String path) {
        return getMultipleBis(path, null);
    }

    /**
     * Recursive method to deal with pagination (nextToken)
     */
    private Map<String, String> getMultipleBis(String path, String nextToken) {
        GetParametersByPathRequest request = GetParametersByPathRequest.builder()
                .path(path)
                .withDecryption(decrypt)
                .recursive(recursive)
                .nextToken(nextToken)
                .build();

        Map<String, String> params = new HashMap<>();

        // not using the client.getParametersByPathPaginator() as hardly testable
        GetParametersByPathResponse res = client.getParametersByPath(request);
        if (res.hasParameters()) {
            res.parameters().forEach(parameter -> {
                /* Standardize the parameter name
                   The parameter name returned by SSM will contained the full path.
                   However, for readability, we should return only the part after
                   the path.
                 */
                String name = parameter.name();
                if (name.startsWith(path)) {
                    name = name.replaceFirst(path, "");
                }
                name = name.replaceFirst("/", "");
                params.put(name, parameter.value());
            });
        }

        if (!StringUtils.isEmpty(res.nextToken())) {
            params.putAll(getMultipleBis(path, res.nextToken()));
        }

        return params;
    }

    @Override
    protected void resetToDefaults() {
        super.resetToDefaults();
        decrypt = false;
        recursive = false;
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

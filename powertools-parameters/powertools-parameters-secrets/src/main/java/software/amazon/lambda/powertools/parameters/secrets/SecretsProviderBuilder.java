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

import software.amazon.awssdk.core.SdkSystemSetting;
import software.amazon.awssdk.core.client.config.ClientOverrideConfiguration;
import software.amazon.awssdk.core.client.config.SdkAdvancedClientOption;
import software.amazon.awssdk.http.urlconnection.UrlConnectionHttpClient;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.secretsmanager.SecretsManagerClient;
import software.amazon.lambda.powertools.common.internal.UserAgentConfigurator;
import software.amazon.lambda.powertools.parameters.BaseProvider;
import software.amazon.lambda.powertools.parameters.cache.CacheManager;
import software.amazon.lambda.powertools.parameters.transform.TransformationManager;

public class SecretsProviderBuilder {

    private SecretsManagerClient client;
    private CacheManager cacheManager;
    private TransformationManager transformationManager;

    private static SecretsManagerClient createClient() {
        return SecretsManagerClient.builder()
                .httpClientBuilder(UrlConnectionHttpClient.builder())
                .region(Region.of(System.getenv(SdkSystemSetting.AWS_REGION.environmentVariable())))
                .overrideConfiguration(ClientOverrideConfiguration.builder()
                        .putAdvancedOption(SdkAdvancedClientOption.USER_AGENT_SUFFIX,
                                UserAgentConfigurator.getUserAgent(BaseProvider.PARAMETERS)).build())
                .build();
    }

    /**
     * Create a {@link SecretsProvider} instance.
     *
     * @return a {@link SecretsProvider}
     */
    public SecretsProvider build() {
        if (cacheManager == null) {
            // TODO - what should we do with this
            cacheManager = new CacheManager();
        }
        SecretsProvider provider;
        if (client == null) {
            client = createClient();
        }

        provider = new SecretsProvider(cacheManager, transformationManager, client);

        return provider;
    }

    /**
     * Set custom {@link SecretsManagerClient} to pass to the {@link SecretsProvider}. <br/>
     * Use it if you want to customize the region or any other part of the client.
     *
     * @param client Custom client
     * @return the builder to chain calls (eg. <pre>builder.withClient().build()</pre>)
     */
    public SecretsProviderBuilder withClient(SecretsManagerClient client) {
        this.client = client;
        return this;
    }

    /**
     * <b>Mandatory</b>. Provide a CacheManager to the {@link SecretsProvider}
     *
     * @param cacheManager the manager that will handle the cache of parameters
     * @return the builder to chain calls (eg. <pre>builder.withCacheManager().build()</pre>)
     */
    public SecretsProviderBuilder withCacheManager(CacheManager cacheManager) {
        this.cacheManager = cacheManager;
        return this;
    }

    /**
     * Provide a transformationManager to the {@link SecretsProvider}
     *
     * @param transformationManager the manager that will handle transformation of parameters
     * @return the builder to chain calls (eg. <pre>builder.withTransformationManager().build()</pre>)
     */
    public SecretsProviderBuilder withTransformationManager(TransformationManager transformationManager) {
        this.transformationManager = transformationManager;
        return this;
    }
}

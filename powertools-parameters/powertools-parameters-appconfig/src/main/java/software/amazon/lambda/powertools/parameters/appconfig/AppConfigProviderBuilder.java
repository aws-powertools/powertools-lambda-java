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

package software.amazon.lambda.powertools.parameters.appconfig;

import software.amazon.awssdk.core.SdkSystemSetting;
import software.amazon.awssdk.core.client.config.ClientOverrideConfiguration;
import software.amazon.awssdk.core.client.config.SdkAdvancedClientOption;
import software.amazon.awssdk.http.urlconnection.UrlConnectionHttpClient;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.appconfigdata.AppConfigDataClient;
import software.amazon.lambda.powertools.common.internal.UserAgentConfigurator;
import software.amazon.lambda.powertools.parameters.BaseProvider;
import software.amazon.lambda.powertools.parameters.ParamProvider;
import software.amazon.lambda.powertools.parameters.cache.CacheManager;
import software.amazon.lambda.powertools.parameters.transform.TransformationManager;

/**
 * Implements a {@link ParamProvider} on top of the AppConfig service. AppConfig provides
 */
public class AppConfigProviderBuilder {
    private AppConfigDataClient client;
    private CacheManager cacheManager;
    private TransformationManager transformationManager;
    private String environment;
    private String application;

    /**
     * Create a {@link AppConfigProvider} instance.
     *
     * @return a {@link AppConfigProvider}
     */
    public AppConfigProvider build() {
        if (cacheManager == null) {
            cacheManager = new CacheManager();
        }
        if (environment == null) {
            throw new IllegalStateException("No environment provided; please provide one");
        }
        if (application == null) {
            throw new IllegalStateException("No application provided; please provide one");
        }
        if (transformationManager == null) {
            transformationManager = new TransformationManager();
        }
        // Create a AppConfigDataClient if we haven't been given one
        if (client == null) {
            client = AppConfigDataClient.builder()
                    .httpClientBuilder(UrlConnectionHttpClient.builder())
                    .region(Region.of(System.getenv(SdkSystemSetting.AWS_REGION.environmentVariable())))
                    .overrideConfiguration(ClientOverrideConfiguration.builder()
                            .putAdvancedOption(SdkAdvancedClientOption.USER_AGENT_SUFFIX,
                                    UserAgentConfigurator.getUserAgent(BaseProvider.PARAMETERS)).build())
                    .build();
        }

        return new AppConfigProvider(cacheManager, transformationManager, client, environment, application);
    }

    /**
     * Set custom {@link AppConfigDataClient} to pass to the {@link AppConfigProvider}. <br/>
     * Use it if you want to customize the region or any other part of the client.
     *
     * @param client Custom client
     * @return the builder to chain calls (eg. <pre>builder.withClient().build()</pre>)
     */
    public AppConfigProviderBuilder withClient(AppConfigDataClient client) {
        this.client = client;
        return this;
    }

    /**
     * <b>Mandatory</b>. Provide an environment to the {@link AppConfigProvider}
     *
     * @param environment the AppConfig environment
     * @return the builder to chain calls (eg. <pre>builder.withCacheManager().build()</pre>)
     */
    public AppConfigProviderBuilder withEnvironment(String environment) {
        this.environment = environment;
        return this;
    }

    /**
     * <b>Mandatory</b>. Provide an application to the {@link AppConfigProvider}
     *
     * @param application the application to pull configuration from
     * @return the builder to chain calls (eg. <pre>builder.withCacheManager().build()</pre>)
     */
    public AppConfigProviderBuilder withApplication(String application) {
        this.application = application;
        return this;
    }

    /**
     * Provide a CacheManager to the {@link AppConfigProvider}
     *
     * @param cacheManager the manager that will handle the cache of parameters
     * @return the builder to chain calls (eg. <pre>builder.withCacheManager().build()</pre>)
     */
    public AppConfigProviderBuilder withCacheManager(CacheManager cacheManager) {
        this.cacheManager = cacheManager;
        return this;
    }

    /**
     * Provide a transformationManager to the {@link AppConfigProvider}
     *
     * @param transformationManager the manager that will handle transformation of parameters
     * @return the builder to chain calls (eg. <pre>builder.withTransformationManager().build()</pre>)
     */
    public AppConfigProviderBuilder withTransformationManager(TransformationManager transformationManager) {
        this.transformationManager = transformationManager;
        return this;
    }
}

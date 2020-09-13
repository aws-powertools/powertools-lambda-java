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
import software.amazon.awssdk.services.ssm.SsmClient;
import software.amazon.lambda.powertools.parameters.cache.CacheManager;
import software.amazon.lambda.powertools.parameters.cache.NowProvider;
import software.amazon.lambda.powertools.parameters.transform.TransformationManager;

/**
 * Utility class to retrieve instances of parameter providers.
 * Each instance is unique (singleton).
 */
public final class ParamManager {

    private static final NowProvider nowProvider = new NowProvider();
    private static final CacheManager cacheManager = new CacheManager(nowProvider);
    private static final TransformationManager transformationManager = new TransformationManager();

    private static SecretsProvider secretsProvider;
    private static SSMProvider ssmProvider;

    /**
     * Get a {@link SecretsProvider} with default {@link SecretsManagerClient}.<br/>
     * If you need to customize the region, or other part of the client, use {@link ParamManager#getSecretsProvider(SecretsManagerClient)} instead.
     * @return a {@link SecretsProvider}
     */
    public static SecretsProvider getSecretsProvider() {
        if (secretsProvider == null) {
            secretsProvider = SecretsProvider.builder()
                                    .withCacheManager(cacheManager)
                                    .withTransformationManager(transformationManager)
                                    .build();
        }
        return secretsProvider;
    }

    /**
     * Get a {@link SSMProvider} with default {@link SsmClient}.<br/>
     * If you need to customize the region, or other part of the client, use {@link ParamManager#getSsmProvider(SsmClient)} instead.
     * @return a {@link SSMProvider}
     */
    public static SSMProvider getSsmProvider() {
        if (ssmProvider == null) {
            ssmProvider = SSMProvider.builder()
                            .withCacheManager(cacheManager)
                            .withTransformationManager(transformationManager)
                            .build();
        }
        return ssmProvider;
    }

    /**
     * Get a {@link SecretsProvider} with your custom {@link SecretsManagerClient}.<br/>
     * Use this to configure region or other part of the client. Use {@link ParamManager#getSsmProvider()} if you don't need this customization.
     * @return a {@link SecretsProvider}
     */
    public static SecretsProvider getSecretsProvider(SecretsManagerClient client) {
        if (secretsProvider == null) {
            secretsProvider = SecretsProvider.builder()
                                    .withClient(client)
                                    .withCacheManager(cacheManager)
                                    .withTransformationManager(transformationManager)
                                    .build();
        }
        return secretsProvider;
    }

    /**
     * Get a {@link SSMProvider} with your custom {@link SsmClient}.<br/>
     * Use this to configure region or other part of the client. Use {@link ParamManager#getSsmProvider()} if you don't need this customization.
     * @return a {@link SSMProvider}
     */
    public static SSMProvider getSsmProvider(SsmClient client) {
        if (ssmProvider == null) {
            ssmProvider = SSMProvider.builder()
                                .withClient(client)
                                .withCacheManager(cacheManager)
                                .withTransformationManager(transformationManager)
                                .build();
        }
        return ssmProvider;
    }
}

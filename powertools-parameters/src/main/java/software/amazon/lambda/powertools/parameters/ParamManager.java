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
import software.amazon.awssdk.services.dynamodb.DynamoDbClient;
import software.amazon.lambda.powertools.parameters.cache.CacheManager;
import software.amazon.lambda.powertools.parameters.transform.TransformationManager;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Utility class to retrieve instances of parameter providers.
 * Each instance is unique (singleton).
 */
public final class ParamManager {

    private static final CacheManager cacheManager = new CacheManager();
    private static final TransformationManager transformationManager = new TransformationManager();

    // NOTE: For testing purposes `providers` cannot be final
    private static ConcurrentHashMap<Class<? extends BaseProvider>, BaseProvider> providers = new ConcurrentHashMap<>();

    /**
     * Get a concrete implementation of {@link BaseProvider}.<br/>
     * You can specify {@link SecretsProvider}, {@link SSMProvider}, {@link DynamoDbProvider},  or create your
     * custom provider by extending {@link BaseProvider} if you need to integrate with a different parameter store.
     * @return a {@link SecretsProvider}
     */
    public static <T extends BaseProvider> T getProvider(Class<T> providerClass) {
        if (providerClass == null) {
            throw new IllegalStateException("providerClass cannot be null.");
        }
        return (T) providers.computeIfAbsent(providerClass, ParamManager::createProvider);
    }

    /**
     * Get a {@link SecretsProvider} with default {@link SecretsManagerClient}.<br/>
     * If you need to customize the region, or other part of the client, use {@link ParamManager#getSecretsProvider(SecretsManagerClient)} instead.
     * @return a {@link SecretsProvider}
     */
    public static SecretsProvider getSecretsProvider() {
        return getProvider(SecretsProvider.class);
    }

    /**
     * Get a {@link SSMProvider} with default {@link SsmClient}.<br/>
     * If you need to customize the region, or other part of the client, use {@link ParamManager#getSsmProvider(SsmClient)} instead.
     * @return a {@link SSMProvider}
     */
    public static SSMProvider getSsmProvider() {
        return getProvider(SSMProvider.class);
    }

    /**
     * Get a {@link DynamoDbProvider} with default {@link DynamoDbClient} <br/>
     * If you need to customize the region, or other part of the client, use
     */
    public static DynamoDbProvider getDynamodbProvider() { return getProvider(DynamoDbProvider.class); }

    /**
     * Get a {@link SecretsProvider} with your custom {@link SecretsManagerClient}.<br/>
     * Use this to configure region or other part of the client. Use {@link ParamManager#getSsmProvider()} if you don't need this customization.
     * @return a {@link SecretsProvider}
     */
    public static SecretsProvider getSecretsProvider(SecretsManagerClient client) {
        return (SecretsProvider) providers.computeIfAbsent(SecretsProvider.class, (k) -> SecretsProvider.builder()
                .withClient(client)
                .withCacheManager(cacheManager)
                .withTransformationManager(transformationManager)
                .build());
    }

    /**
     * Get a {@link SSMProvider} with your custom {@link SsmClient}.<br/>
     * Use this to configure region or other part of the client. Use {@link ParamManager#getSsmProvider()} if you don't need this customization.
     * @return a {@link SSMProvider}
     */
    public static SSMProvider getSsmProvider(SsmClient client) {
        return (SSMProvider) providers.computeIfAbsent(SSMProvider.class, (k) -> SSMProvider.builder()
                .withClient(client)
                .withCacheManager(cacheManager)
                .withTransformationManager(transformationManager)
                .build());
    }

    /**
     * Get a {@link DynamoDbProvider} with your custom {@link DynamoDbClient}.<br/>
     * Use this to configure region or other part of the client. Use {@link ParamManager#getDynamodbProvider()} if you don't need this customization.
     * @return a {@link DynamoDbProvider}
     */
    public static DynamoDbProvider getDynamoDbProvider(DynamoDbClient client) {
        return (DynamoDbProvider) providers.computeIfAbsent(DynamoDbProvider.class, (k) -> DynamoDbProvider.builder()
                .withClient(client)
                .withCacheManager(cacheManager)
                .withTransformationManager(transformationManager)
                .build());
    }

    public static CacheManager getCacheManager() {
        return cacheManager;
    }

    public static TransformationManager getTransformationManager() {
        return transformationManager;
    }

    private static <T extends BaseProvider> T createProvider(Class<T> providerClass) {
        try {
            Constructor<T> constructor = providerClass.getDeclaredConstructor(CacheManager.class);
            T provider = constructor.newInstance(cacheManager);
            provider.setTransformationManager(transformationManager);
            return provider;
        } catch (NoSuchMethodException | IllegalAccessException | InstantiationException | InvocationTargetException e) {
            throw new RuntimeException("Unexpected error occurred. Please raise issue at " +
                    "https://github.com/awslabs/aws-lambda-powertools-java/issues", e);
        }
    }

}

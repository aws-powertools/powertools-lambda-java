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

package software.amazon.lambda.powertools.parameters;

import java.lang.reflect.Constructor;
import java.util.concurrent.ConcurrentHashMap;
import software.amazon.lambda.powertools.parameters.cache.CacheManager;
import software.amazon.lambda.powertools.parameters.transform.TransformationManager;

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
     * custom provider by extending {@link BaseProvider} if you need to integrate with a different parameter store.
     *
     * @return a {@link SecretsProvider}
     * @deprecated You should not use this method directly but a typed one (getSecretsProvider, getSsmProvider, getDynamoDbProvider, getAppConfigProvider), will be removed in v2
     */
    // TODO in v2: remove public access to this and review how we get providers (it was not designed for DDB and AppConfig in mind initially)
    public static <T extends BaseProvider> T getProvider(Class<T> providerClass) {
        if (providerClass == null) {
            throw new IllegalStateException("providerClass cannot be null.");
        }
        return (T) providers.computeIfAbsent(providerClass, ParamManager::createProvider);
    }


    public static CacheManager getCacheManager() {
        return cacheManager;
    }

    public static TransformationManager getTransformationManager() {
        return transformationManager;
    }

    static <T extends BaseProvider> T createProvider(Class<T> providerClass) {
        try {
            Constructor<T> constructor = providerClass.getDeclaredConstructor(CacheManager.class);
            T provider =
                    constructor.newInstance(cacheManager); // FIXME: avoid reflection here as we may have issues (#1280)
            provider.setTransformationManager(transformationManager);
            return provider;
        } catch (ReflectiveOperationException e) {
            throw new RuntimeException("Unexpected error occurred. Please raise issue at " +
                    "https://github.com/aws-powertools/powertools-lambda-java/issues", e);
        }
    }

}

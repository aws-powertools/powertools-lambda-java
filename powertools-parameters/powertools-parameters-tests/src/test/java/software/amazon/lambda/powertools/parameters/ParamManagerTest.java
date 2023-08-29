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

import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;
import static org.assertj.core.api.Assertions.assertThatIllegalStateException;
import static org.assertj.core.api.Assertions.assertThatRuntimeException;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import org.junit.jupiter.api.Test;
import software.amazon.lambda.powertools.parameters.cache.CacheManager;
import software.amazon.lambda.powertools.parameters.internal.CustomProvider;
import software.amazon.lambda.powertools.parameters.transform.TransformationManager;

public class ParamManagerTest {

    @Test
    public void testGetCacheManager() {

        // Act
        CacheManager cacheManager = ParamManager.getCacheManager();

        // Assert
        assertNotNull(cacheManager);
    }

    @Test
    public void testGetTransformationManager() {

        // Act
        TransformationManager transformationManager = ParamManager.getTransformationManager();

        // Assert
        assertNotNull(transformationManager);
    }

    @Test
    public void testCreateProvider() {

        // Act
        CustomProvider customProvider = ParamManager.createProvider(CustomProvider.class);

        // Assert
        assertNotNull(customProvider);
    }

    @Test
    public void testCreateUninstanciableProvider_throwsException() {

        // Act & Assert
        assertThatRuntimeException().isThrownBy(() -> ParamManager.createProvider(BaseProvider.class));
    }

    @Test
    public void testGetProviderWithProviderClass() {

        // Act
        SecretsProvider secretsProvider = ParamManager.getProvider(SecretsProvider.class);

        // Assert
        assertNotNull(secretsProvider);
    }

    @Test
    public void testGetProviderWithProviderClass_throwsException() {

        // Act & Assert
        assertThatIllegalStateException().isThrownBy(() -> ParamManager.getProvider(null));
    }

    @Test
    public void testGetSecretsProvider_withoutParameter_shouldCreateDefaultClient() {

        // Act
        SecretsProvider secretsProvider = ParamManager.getSecretsProvider();

        // Assert
        assertNotNull(secretsProvider);
        assertNotNull(secretsProvider.getClient());
    }


    @Test
    public void testGetDynamoDBProvider_requireOtherParameters_throwException() {

        // Act & Assert
        assertThatIllegalArgumentException().isThrownBy(() -> ParamManager.getProvider(DynamoDbProvider.class));
    }

    @Test
    public void testGetAppConfigProvider_requireOtherParameters_throwException() {

        // Act & Assert
        assertThatIllegalArgumentException().isThrownBy(() -> ParamManager.getProvider(AppConfigProvider.class));
    }
}

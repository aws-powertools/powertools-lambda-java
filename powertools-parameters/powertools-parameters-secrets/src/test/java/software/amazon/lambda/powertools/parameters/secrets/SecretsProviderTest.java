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

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalStateException;
import static org.assertj.core.api.Assertions.assertThatRuntimeException;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.MockitoAnnotations.openMocks;

import java.time.temporal.ChronoUnit;
import java.util.Base64;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;
import software.amazon.awssdk.core.SdkBytes;
import software.amazon.awssdk.services.secretsmanager.SecretsManagerClient;
import software.amazon.awssdk.services.secretsmanager.model.GetSecretValueRequest;
import software.amazon.awssdk.services.secretsmanager.model.GetSecretValueResponse;
import software.amazon.lambda.powertools.parameters.cache.CacheManager;
import software.amazon.lambda.powertools.parameters.transform.TransformationManager;

public class SecretsProviderTest {

    @Mock
    SecretsManagerClient client;

    @Mock
    TransformationManager transformationManager;

    @Captor
    ArgumentCaptor<GetSecretValueRequest> paramCaptor;

    CacheManager cacheManager;

    SecretsProvider provider;

    @BeforeEach
    public void init() {
        MockitoAnnotations.openMocks(this);
        cacheManager = new CacheManager();
        provider = new SecretsProvider(cacheManager, transformationManager, client);
    }

    @Test
    public void getValue() {
        String key = "Key1";
        String expectedValue = "Value1";
        GetSecretValueResponse response = GetSecretValueResponse.builder().secretString(expectedValue).build();
        Mockito.when(client.getSecretValue(paramCaptor.capture())).thenReturn(response);
        provider.defaultMaxAge(1, ChronoUnit.DAYS);
        provider.withMaxAge(2, ChronoUnit.DAYS);

        String value = provider.getValue(key);

        Assertions.assertThat(value).isEqualTo(expectedValue);
        Assertions.assertThat(paramCaptor.getValue().secretId()).isEqualTo(key);
    }

    @Test
    public void getValueBase64() {
        String key = "Key2";
        String expectedValue = "Value2";
        byte[] valueb64 = Base64.getEncoder().encode(expectedValue.getBytes());
        GetSecretValueResponse response =
                GetSecretValueResponse.builder().secretBinary(SdkBytes.fromByteArray(valueb64)).build();
        Mockito.when(client.getSecretValue(paramCaptor.capture())).thenReturn(response);

        String value = provider.getValue(key);

        Assertions.assertThat(value).isEqualTo(expectedValue);
        Assertions.assertThat(paramCaptor.getValue().secretId()).isEqualTo(key);
    }

    @Test
    public void getMultipleValuesThrowsException() {

        // Act & Assert
        Assertions.assertThatRuntimeException().isThrownBy(() -> provider.getMultipleValues("path"))
                .withMessage("Impossible to get multiple values from AWS Secrets Manager");

    }

    @Test
    public void testGetSecretsProvider_withoutParameter_shouldCreateDefaultClient() {

        // Act
        SecretsProvider secretsProvider = SecretsProvider.builder()
                .build();

        // Assert
        assertNotNull(secretsProvider);
        assertNotNull(secretsProvider.getClient());
    }
}

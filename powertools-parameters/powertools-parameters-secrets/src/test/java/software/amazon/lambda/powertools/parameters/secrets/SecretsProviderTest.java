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
import static org.assertj.core.api.Assertions.assertThatRuntimeException;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static software.amazon.lambda.powertools.parameters.transform.Transformer.json;

import java.time.temporal.ChronoUnit;
import java.util.Base64;

import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;

import software.amazon.awssdk.core.SdkBytes;
import software.amazon.awssdk.services.secretsmanager.SecretsManagerClient;
import software.amazon.awssdk.services.secretsmanager.model.BatchGetSecretValueRequest;
import software.amazon.awssdk.services.secretsmanager.model.BatchGetSecretValueResponse;
import software.amazon.awssdk.services.secretsmanager.model.Filter;
import software.amazon.awssdk.services.secretsmanager.model.GetSecretValueRequest;
import software.amazon.awssdk.services.secretsmanager.model.GetSecretValueResponse;
import software.amazon.awssdk.services.secretsmanager.model.SecretValueEntry;
import software.amazon.lambda.powertools.parameters.cache.CacheManager;
import software.amazon.lambda.powertools.parameters.transform.TransformationManager;

@ExtendWith(MockitoExtension.class)
class SecretsProviderTest {

    @Mock
    SecretsManagerClient client;

    @Mock
    TransformationManager transformationManager;

    @Captor
    ArgumentCaptor<GetSecretValueRequest> paramCaptor;

    @Captor
    ArgumentCaptor<BatchGetSecretValueRequest> batchCaptor;

    CacheManager cacheManager;

    SecretsProvider provider;

    @BeforeEach
    void init() {
        cacheManager = new CacheManager();
        provider = new SecretsProvider(cacheManager, transformationManager, client);
    }

    @Test
    void getValue() {
        String key = "Key1";
        String expectedValue = "Value1";
        GetSecretValueResponse response = GetSecretValueResponse.builder().secretString(expectedValue).build();
        Mockito.when(client.getSecretValue(paramCaptor.capture())).thenReturn(response);
        provider.withMaxAge(2, ChronoUnit.DAYS);

        String value = provider.getValue(key);

        assertThat(value).isEqualTo(expectedValue);
        assertThat(paramCaptor.getValue().secretId()).isEqualTo(key);
    }

    @Test
    void getValueBase64() {
        String key = "Key2";
        String expectedValue = "Value2";
        byte[] valueb64 = Base64.getEncoder().encode(expectedValue.getBytes());
        GetSecretValueResponse response = GetSecretValueResponse.builder()
                .secretBinary(SdkBytes.fromByteArray(valueb64)).build();
        Mockito.when(client.getSecretValue(paramCaptor.capture())).thenReturn(response);

        String value = provider.getValue(key);

        assertThat(value).isEqualTo(expectedValue);
        assertThat(paramCaptor.getValue().secretId()).isEqualTo(key);
    }

    @Test
    void getMultipleValuesThrowsException() {
        // Act & Assert
        assertThatRuntimeException().isThrownBy(() -> provider.getMultipleValues("path"))
                .withMessage("Impossible to get multiple values from AWS Secrets Manager via path. Use getMultiple(List<String> names) instead.");
    }
    @Test
    void getMultipleValues() {
        List<String> names = List.of("name1", "name2");
        String value1 = "Value1";
        String value2 = "Value2";

        SecretValueEntry entry1 =
                SecretValueEntry.builder()
                        .name("name1")
                        .secretString(value1)
                        .build();

        byte[] value2b64 = Base64.getEncoder().encode(value2.getBytes());
        SecretValueEntry entry2 =
                SecretValueEntry.builder()
                        .name("name2")
                        .secretBinary(SdkBytes.fromByteArray(value2b64))
                        .build();

        BatchGetSecretValueResponse response =
                BatchGetSecretValueResponse.builder()
                        .secretValues(entry1, entry2)
                        .build();

        Mockito.when(client.batchGetSecretValue(batchCaptor.capture())).thenReturn(response);

        Map<String, String> result = provider.getMultiple(names);

        assertThat(result).hasSize(2)
                .containsEntry("name1", value1)
                .containsEntry("name2", value2);

        BatchGetSecretValueRequest captured = batchCaptor.getValue();
        assertThat(captured.secretIdList()).isEmpty();
        assertThat(captured.filters()).hasSize(1);

        Filter filter = captured.filters().get(0);
        assertThat(filter.key().toString()).hasToString("name");
        assertThat(filter.values()).isEqualTo(names);
    }

    @Test
    void testGetSecretsProvider_withoutParameter_shouldCreateDefaultClient() {
        // Act
        SecretsProvider secretsProvider = SecretsProvider.builder()
                .build();

        // Assert
        assertNotNull(secretsProvider);
        assertNotNull(secretsProvider.getClient());
    }

    @Test
    void testGetSecretsProvider_withoutParameter_shouldHaveDefaultTransformationManager() {
        // Act
        SecretsProvider secretsProvider = SecretsProvider.builder()
                .build();
        // Assert
        assertDoesNotThrow(() -> secretsProvider.withTransformation(json));
    }
}

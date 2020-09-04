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

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.Mockito;
import software.amazon.awssdk.core.SdkBytes;
import software.amazon.awssdk.services.secretsmanager.SecretsManagerClient;
import software.amazon.awssdk.services.secretsmanager.model.GetSecretValueRequest;
import software.amazon.awssdk.services.secretsmanager.model.GetSecretValueResponse;

import java.util.Base64;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.mockito.MockitoAnnotations.openMocks;

public class SecretsProviderTest {

    @Mock
    SecretsManagerClient client;

    @Captor
    ArgumentCaptor<GetSecretValueRequest> paramCaptor;

    SecretsProvider provider;

    @BeforeEach
    public void init() {
        openMocks(this);
        provider = new SecretsProvider(client);
    }

    @Test
    public void getValue() {
        String key = "Key1";
        String expectedValue = "Value1";
        GetSecretValueResponse response = GetSecretValueResponse.builder().secretString(expectedValue).build();
        Mockito.when(client.getSecretValue(paramCaptor.capture())).thenReturn(response);

        String value = provider.getValue(key);

        assertEquals(expectedValue, value);
        assertEquals(key, paramCaptor.getValue().secretId());
    }

    @Test
    public void getValueBase64() {
        String key = "Key2";
        byte[] expectedValue = Base64.getEncoder().encode("Value2".getBytes());
        GetSecretValueResponse response = GetSecretValueResponse.builder().secretBinary(SdkBytes.fromByteArray(expectedValue)).build();
        Mockito.when(client.getSecretValue(paramCaptor.capture())).thenReturn(response);

        String value = provider.getValue(key);

        assertEquals("Value2", value);
        assertEquals(key, paramCaptor.getValue().secretId());
    }
}

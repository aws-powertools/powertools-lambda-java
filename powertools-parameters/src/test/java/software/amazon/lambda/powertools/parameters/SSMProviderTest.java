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
import software.amazon.awssdk.services.ssm.SsmClient;
import software.amazon.awssdk.services.ssm.model.GetParameterRequest;
import software.amazon.awssdk.services.ssm.model.GetParameterResponse;
import software.amazon.awssdk.services.ssm.model.Parameter;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.mockito.Mockito.when;
import static org.mockito.MockitoAnnotations.openMocks;

public class SSMProviderTest {

    @Mock
    SsmClient client;

    @Captor
    ArgumentCaptor<GetParameterRequest> paramCaptor;

    SSMProvider provider;

    @BeforeEach
    public void init() {
        openMocks(this);
        provider = new SSMProvider(client);
    }

    @Test
    public void getValue() {
        String key = "Key1";
        String expectedValue = "Value1";
        initMock(expectedValue);

        String value = provider.getValue(key);

        assertEquals(expectedValue, value);
        assertEquals(key, paramCaptor.getValue().name());
        assertNotEquals(true, paramCaptor.getValue().withDecryption());
    }

    @Test
    public void getValueDecrypted() {
        String key = "Key2";
        String expectedValue = "Value2";
        initMock(expectedValue);

        String value = provider.withDecryption(true).getValue(key);

        assertEquals(expectedValue, value);
        assertEquals(key, paramCaptor.getValue().name());
        assertEquals(true, paramCaptor.getValue().withDecryption());
    }

    private void initMock(String expectedValue) {
        Parameter parameter = Parameter.builder().value(expectedValue).build();
        GetParameterResponse result = GetParameterResponse.builder().parameter(parameter).build();
        when(client.getParameter(paramCaptor.capture())).thenReturn(result);
    }

}

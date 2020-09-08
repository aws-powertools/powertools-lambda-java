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
import software.amazon.awssdk.services.ssm.model.*;
import software.amazon.lambda.powertools.parameters.transform.Transformer;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.when;
import static org.mockito.MockitoAnnotations.openMocks;

public class SSMProviderTest {

    @Mock
    SsmClient client;

    @Captor
    ArgumentCaptor<GetParameterRequest> paramCaptor;

    @Captor
    ArgumentCaptor<GetParametersByPathRequest> paramByPathCaptor;

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
        assertFalse(paramCaptor.getValue().withDecryption());
    }

    @Test
    public void getValueDecrypted() {
        String key = "Key2";
        String expectedValue = "Value2";
        initMock(expectedValue);

        String value = provider.withDecryption().getValue(key);

        assertEquals(expectedValue, value);
        assertEquals(key, paramCaptor.getValue().name());
        assertTrue(paramCaptor.getValue().withDecryption());
    }

    @Test
    public void getMultiple() {
        List<Parameter> parameters = new ArrayList<>();
        parameters.add(Parameter.builder().name("/prod/app1/key1").value("foo1").build());
        parameters.add(Parameter.builder().name("/prod/app1/key2").value("foo2").build());
        parameters.add(Parameter.builder().name("/prod/app1/key3").value("foo3").build());
        GetParametersByPathResponse response = GetParametersByPathResponse.builder().parameters(parameters).build();
        when(client.getParametersByPath(paramByPathCaptor.capture())).thenReturn(response);

        Map<String, String> params = provider.getMultiple("/prod/app1");
        assertEquals("foo1", params.get("key1"));
        assertEquals("foo2", params.get("key2"));
        assertEquals("foo3", params.get("key3"));

        assertEquals("/prod/app1", paramByPathCaptor.getValue().path());
        assertFalse(paramByPathCaptor.getValue().withDecryption());
        assertFalse(paramByPathCaptor.getValue().recursive());
    }

    @Test
    public void getMultipleWithNextToken() {
        List<Parameter> parameters1 = new ArrayList<>();
        parameters1.add(Parameter.builder().name("/prod/app1/key1").value("foo1").build());
        parameters1.add(Parameter.builder().name("/prod/app1/key2").value("foo2").build());
        GetParametersByPathResponse response1 = GetParametersByPathResponse.builder().parameters(parameters1).nextToken("123abc").build();

        List<Parameter> parameters2 = new ArrayList<>();
        parameters2.add(Parameter.builder().name("/prod/app1/key3").value("foo3").build());
        GetParametersByPathResponse response2 = GetParametersByPathResponse.builder().parameters(parameters2).build();

        when(client.getParametersByPath(paramByPathCaptor.capture())).thenReturn(response1, response2);

        Map<String, String> params = provider.getMultiple("/prod/app1");

        assertEquals("foo1", params.get("key1"));
        assertEquals("foo2", params.get("key2"));
        assertEquals("foo3", params.get("key3"));

        List<GetParametersByPathRequest> requestParams = paramByPathCaptor.getAllValues();
        GetParametersByPathRequest request1 = requestParams.get(0);
        GetParametersByPathRequest request2 = requestParams.get(1);

        assertEquals("/prod/app1", request1.path());
        assertNull(request1.nextToken());
        assertFalse(request1.withDecryption());
        assertFalse(request1.recursive());

        assertEquals("/prod/app1", request2.path());
        assertEquals("123abc", request2.nextToken());
        assertFalse(request2.withDecryption());
        assertFalse(request2.recursive());
    }

    private void initMock(String expectedValue) {
        Parameter parameter = Parameter.builder().value(expectedValue).build();
        GetParameterResponse result = GetParameterResponse.builder().parameter(parameter).build();
        when(client.getParameter(paramCaptor.capture())).thenReturn(result);
    }

}

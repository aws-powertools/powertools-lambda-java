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

import static org.apache.commons.lang3.reflect.FieldUtils.writeStaticField;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.MockitoAnnotations.openMocks;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import org.assertj.core.data.MapEntry;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import software.amazon.awssdk.services.appconfigdata.AppConfigDataClient;
import software.amazon.awssdk.services.dynamodb.DynamoDbClient;
import software.amazon.awssdk.services.secretsmanager.SecretsManagerClient;
import software.amazon.awssdk.services.secretsmanager.model.GetSecretValueRequest;
import software.amazon.awssdk.services.secretsmanager.model.GetSecretValueResponse;
import software.amazon.awssdk.services.ssm.SsmClient;
import software.amazon.awssdk.services.ssm.model.GetParameterRequest;
import software.amazon.awssdk.services.ssm.model.GetParameterResponse;
import software.amazon.awssdk.services.ssm.model.GetParametersByPathRequest;
import software.amazon.awssdk.services.ssm.model.GetParametersByPathResponse;
import software.amazon.awssdk.services.ssm.model.Parameter;
import software.amazon.lambda.powertools.parameters.secrets.SecretsProvider;
import software.amazon.lambda.powertools.parameters.ssm.SSMProvider;

public class ParamManagerIntegrationTest {

    @Mock
    SsmClient ssmClient;

    @Mock
    DynamoDbClient ddbClient;
    @Captor
    ArgumentCaptor<GetParameterRequest> ssmParamCaptor;
    @Captor
    ArgumentCaptor<GetParametersByPathRequest> ssmParamByPathCaptor;
    @Mock
    SecretsManagerClient secretsManagerClient;
    @Captor
    ArgumentCaptor<GetSecretValueRequest> secretsCaptor;
    @Mock
    private AppConfigDataClient appConfigDataClient;

    @BeforeEach
    public void setup() throws IllegalAccessException {
        openMocks(this);

        writeStaticField(ParamManager.class, "providers", new ConcurrentHashMap<>(), true);
    }

    @Test
    public void ssmProvider_get() {
        SSMProvider ssmProvider = SSMProvider.builder()
                .withClient(ssmClient)
                .build();

        String expectedValue = "value";
        Parameter parameter = Parameter.builder().value(expectedValue).build();
        GetParameterResponse result = GetParameterResponse.builder().parameter(parameter).build();
        when(ssmClient.getParameter(ssmParamCaptor.capture())).thenReturn(result);

        assertThat(ssmProvider.get("key")).isEqualTo(expectedValue);
        assertThat(ssmParamCaptor.getValue().name()).isEqualTo("key");

        assertThat(ssmProvider.get("key")).isEqualTo(expectedValue); // second time is from cache
        verify(ssmClient, times(1)).getParameter(any(GetParameterRequest.class));
    }

    @Test
    public void ssmProvider_getMultiple() {
        SSMProvider ssmProvider = SSMProvider.builder()
                .withClient(ssmClient)
                .build();

        List<Parameter> parameters = new ArrayList<>();
        parameters.add(Parameter.builder().name("/prod/app1/key1").value("foo1").build());
        parameters.add(Parameter.builder().name("/prod/app1/key2").value("foo2").build());
        parameters.add(Parameter.builder().name("/prod/app1/key3").value("foo3").build());
        GetParametersByPathResponse response = GetParametersByPathResponse.builder().parameters(parameters).build();
        when(ssmClient.getParametersByPath(ssmParamByPathCaptor.capture())).thenReturn(response);

        Map<String, String> params = ssmProvider.getMultiple("/prod/app1");
        assertThat(ssmParamByPathCaptor.getValue().path()).isEqualTo("/prod/app1");

        assertThat(params).contains(
                MapEntry.entry("key1", "foo1"),
                MapEntry.entry("key2", "foo2"),
                MapEntry.entry("key3", "foo3"));

        assertThat(ssmProvider.get("/prod/app1/key1")).isEqualTo("foo1");

        ssmProvider.getMultiple("/prod/app1");// second time is from cache
        verify(ssmClient, times(1)).getParametersByPath(any(GetParametersByPathRequest.class));
    }

    @Test
    public void secretsProvider_get() {
        SecretsProvider secretsProvider = SecretsProvider.builder()
                .withClient(secretsManagerClient)
                .build();

        String expectedValue = "Value1";
        GetSecretValueResponse response = GetSecretValueResponse.builder().secretString(expectedValue).build();
        when(secretsManagerClient.getSecretValue(secretsCaptor.capture())).thenReturn(response);

        assertThat(secretsProvider.get("keys")).isEqualTo(expectedValue);
        assertThat(secretsCaptor.getValue().secretId()).isEqualTo("keys");

        assertThat(secretsProvider.get("keys")).isEqualTo(expectedValue); // second time is from cache
        verify(secretsManagerClient, times(1)).getSecretValue(any(GetSecretValueRequest.class));
    }

    @Test
    public void getAppConfigProvider() {

        // Act
        AppConfigProvider provider = ParamManager.getAppConfigProvider(appConfigDataClient, "test-env", "test-app");

        // Assert
        assertThat(provider).isNotNull();

    }
}

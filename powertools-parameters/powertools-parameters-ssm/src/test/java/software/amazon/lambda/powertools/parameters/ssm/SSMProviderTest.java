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

package software.amazon.lambda.powertools.parameters.ssm;

import static java.util.Arrays.asList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static software.amazon.lambda.powertools.parameters.transform.Transformer.json;

import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CountDownLatch;

import org.assertj.core.data.MapEntry;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.ArgumentMatchers;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;

import software.amazon.awssdk.services.ssm.SsmClient;
import software.amazon.awssdk.services.ssm.model.GetParameterRequest;
import software.amazon.awssdk.services.ssm.model.GetParameterResponse;
import software.amazon.awssdk.services.ssm.model.GetParametersByPathRequest;
import software.amazon.awssdk.services.ssm.model.GetParametersByPathResponse;
import software.amazon.awssdk.services.ssm.model.Parameter;
import software.amazon.lambda.powertools.parameters.cache.CacheManager;
import software.amazon.lambda.powertools.parameters.transform.TransformationManager;

class SSMProviderTest {

    @Mock
    SsmClient client;

    @Mock
    TransformationManager transformationManager;

    @Captor
    ArgumentCaptor<GetParameterRequest> paramCaptor;

    @Captor
    ArgumentCaptor<GetParametersByPathRequest> paramByPathCaptor;

    CacheManager cacheManager;

    SSMProvider provider;

    @BeforeEach
    void init() {
        MockitoAnnotations.openMocks(this);
        cacheManager = new CacheManager();
        provider = new SSMProvider(cacheManager, null, client);
    }

    @Test
    void getValue() {
        String key = "Key1";
        String expectedValue = "Value1";
        initMock(expectedValue);

        String value = provider.getValue(key);

        assertThat(value).isEqualTo(expectedValue);
        assertThat(paramCaptor.getValue().name()).isEqualTo(key);
        assertThat(paramCaptor.getValue().withDecryption()).isFalse();
    }

    @Test
    void getValueDecrypted() {
        String key = "Key2";
        String expectedValue = "Value2";
        initMock(expectedValue);

        String value = provider.withDecryption().getValue(key);

        assertThat(value).isEqualTo(expectedValue);
        assertThat(paramCaptor.getValue().name()).isEqualTo(key);
        assertThat(paramCaptor.getValue().withDecryption()).isTrue();
    }

    @Test
    void getMultiple() {
        List<Parameter> parameters = new ArrayList<>();
        parameters.add(Parameter.builder().name("/prod/app1/key1").value("foo1").build());
        parameters.add(Parameter.builder().name("/prod/app1/key2").value("foo2").build());
        parameters.add(Parameter.builder().name("/prod/app1/key3").value("foo3").build());
        GetParametersByPathResponse response = GetParametersByPathResponse.builder().parameters(parameters).build();
        Mockito.when(client.getParametersByPath(paramByPathCaptor.capture())).thenReturn(response);

        Map<String, String> params = provider.getMultiple("/prod/app1");
        assertThat(params).contains(
                MapEntry.entry("key1", "foo1"),
                MapEntry.entry("key2", "foo2"),
                MapEntry.entry("key3", "foo3"));
        assertThat(provider.get("/prod/app1/key1")).isEqualTo("foo1");
        assertThat(provider.get("/prod/app1/key2")).isEqualTo("foo2");
        assertThat(provider.get("/prod/app1/key3")).isEqualTo("foo3");

        assertThat(paramByPathCaptor.getValue().path()).isEqualTo("/prod/app1");
        assertThat(paramByPathCaptor.getValue().withDecryption()).isFalse();
        assertThat(paramByPathCaptor.getValue().recursive()).isFalse();
    }

    @Test
    void getMultipleWithTrailingSlash() {
        List<Parameter> parameters = new ArrayList<>();
        parameters.add(Parameter.builder().name("/prod/app1/key1").value("foo1").build());
        parameters.add(Parameter.builder().name("/prod/app1/key2").value("foo2").build());
        parameters.add(Parameter.builder().name("/prod/app1/key3").value("foo3").build());
        GetParametersByPathResponse response = GetParametersByPathResponse.builder().parameters(parameters).build();
        Mockito.when(client.getParametersByPath(paramByPathCaptor.capture())).thenReturn(response);

        Map<String, String> params = provider.getMultiple("/prod/app1/");
        assertThat(params).contains(
                MapEntry.entry("key1", "foo1"),
                MapEntry.entry("key2", "foo2"),
                MapEntry.entry("key3", "foo3"));
        assertThat(provider.get("/prod/app1/key1")).isEqualTo("foo1");
        assertThat(provider.get("/prod/app1/key2")).isEqualTo("foo2");
        assertThat(provider.get("/prod/app1/key3")).isEqualTo("foo3");

        assertThat(paramByPathCaptor.getValue().path()).isEqualTo("/prod/app1");
        assertThat(paramByPathCaptor.getValue().withDecryption()).isFalse();
        assertThat(paramByPathCaptor.getValue().recursive()).isFalse();
    }

    @Test
    void getMultiple_cached_shouldNotCallSSM() {
        List<Parameter> parameters = new ArrayList<>();
        parameters.add(Parameter.builder().name("/prod/app1/key1").value("foo1").build());
        parameters.add(Parameter.builder().name("/prod/app1/key2").value("foo2").build());
        parameters.add(Parameter.builder().name("/prod/app1/key3").value("foo3").build());
        GetParametersByPathResponse response = GetParametersByPathResponse.builder().parameters(parameters).build();
        Mockito.when(client.getParametersByPath(paramByPathCaptor.capture())).thenReturn(response);

        provider.getMultiple("/prod/app1");

        // should get the following from cache
        provider.getMultiple("/prod/app1");
        provider.get("/prod/app1/key1");
        provider.get("/prod/app1/key2");
        provider.get("/prod/app1/key3");

        Mockito.verify(client, Mockito.times(1))
                .getParametersByPath(ArgumentMatchers.any(GetParametersByPathRequest.class));

    }

    @Test
    void getMultipleWithNextToken() {
        List<Parameter> parameters1 = new ArrayList<>();
        parameters1.add(Parameter.builder().name("/prod/app1/key1").value("foo1").build());
        parameters1.add(Parameter.builder().name("/prod/app1/key2").value("foo2").build());
        GetParametersByPathResponse response1 = GetParametersByPathResponse.builder().parameters(parameters1)
                .nextToken("123abc").build();

        List<Parameter> parameters2 = new ArrayList<>();
        parameters2.add(Parameter.builder().name("/prod/app1/key3").value("foo3").build());
        GetParametersByPathResponse response2 = GetParametersByPathResponse.builder().parameters(parameters2).build();

        Mockito.when(client.getParametersByPath(paramByPathCaptor.capture())).thenReturn(response1, response2);

        Map<String, String> params = provider.getMultiple("/prod/app1");

        assertThat(params).contains(
                MapEntry.entry("key1", "foo1"),
                MapEntry.entry("key2", "foo2"),
                MapEntry.entry("key3", "foo3"));

        List<GetParametersByPathRequest> requestParams = paramByPathCaptor.getAllValues();
        GetParametersByPathRequest request1 = requestParams.get(0);
        GetParametersByPathRequest request2 = requestParams.get(1);

        assertThat(asList(request1, request2)).allSatisfy(req -> {
            assertThat(req.path()).isEqualTo("/prod/app1");
            assertThat(req.withDecryption()).isFalse();
            assertThat(req.recursive()).isFalse();
        });

        assertThat(request1.nextToken()).isNull();
        assertThat(request2.nextToken()).isEqualTo("123abc");
    }

    @Test
    void testSSMProvider_withoutParameter_shouldHaveDefaultTransformationManager() {

        // Act
        SSMProvider ssmProvider = SSMProvider.builder()
                .build();
        // Assert
        assertDoesNotThrow(() -> ssmProvider.withTransformation(json));
    }

    @Test
    void withDecryption_concurrentCalls_shouldBeThreadSafe() throws InterruptedException {
        // GIVEN
        Parameter param1 = Parameter.builder().value("value1").build();
        Parameter param2 = Parameter.builder().value("value2").build();
        GetParameterResponse response1 = GetParameterResponse.builder().parameter(param1).build();
        GetParameterResponse response2 = GetParameterResponse.builder().parameter(param2).build();
        CountDownLatch latch = new CountDownLatch(2);
        Mockito.when(client.getParameter(paramCaptor.capture()))
                .thenReturn(response1, response2);

        // WHEN
        Thread thread1 = new Thread(() -> {
            try {
                latch.countDown();
                latch.await();
                provider.withDecryption().getValue("key1");
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        });

        Thread thread2 = new Thread(() -> {
            try {
                latch.countDown();
                latch.await();
                provider.getValue("key2");
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        });

        thread1.start();
        thread2.start();
        thread1.join();
        thread2.join();

        // THEN
        List<GetParameterRequest> requests = paramCaptor.getAllValues();
        assertThat(requests)
                .hasSize(2)
                .anyMatch(GetParameterRequest::withDecryption)
                .anyMatch(r -> !r.withDecryption());
    }

    @Test
    void recursive_concurrentCalls_shouldBeThreadSafe() throws InterruptedException {
        // GIVEN
        List<Parameter> params1 = new ArrayList<>();
        params1.add(Parameter.builder().name("/path1/key1").value("value1").build());
        List<Parameter> params2 = new ArrayList<>();
        params2.add(Parameter.builder().name("/path2/key2").value("value2").build());
        GetParametersByPathResponse response1 = GetParametersByPathResponse.builder().parameters(params1).build();
        GetParametersByPathResponse response2 = GetParametersByPathResponse.builder().parameters(params2).build();
        CountDownLatch latch = new CountDownLatch(2);
        Mockito.when(client.getParametersByPath(paramByPathCaptor.capture()))
                .thenReturn(response1, response2);

        // WHEN
        Thread thread1 = new Thread(() -> {
            try {
                latch.countDown();
                latch.await();
                provider.recursive().getMultiple("/path1");
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        });

        Thread thread2 = new Thread(() -> {
            try {
                latch.countDown();
                latch.await();
                provider.getMultiple("/path2");
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        });

        thread1.start();
        thread2.start();
        thread1.join();
        thread2.join();

        // THEN
        List<GetParametersByPathRequest> requests = paramByPathCaptor.getAllValues();
        assertThat(requests).hasSize(2);
        boolean hasRecursiveRequest = requests.stream().anyMatch(GetParametersByPathRequest::recursive);
        boolean hasNonRecursiveRequest = requests.stream().anyMatch(r -> !r.recursive());
        assertThat(hasRecursiveRequest).isTrue();
        assertThat(hasNonRecursiveRequest).isTrue();
    }

    private void initMock(String expectedValue) {
        Parameter parameter = Parameter.builder().value(expectedValue).build();
        GetParameterResponse result = GetParameterResponse.builder().parameter(parameter).build();
        Mockito.when(client.getParameter(paramCaptor.capture())).thenReturn(result);
        provider.withMaxAge(2, ChronoUnit.DAYS);
        provider.recursive();
    }

}

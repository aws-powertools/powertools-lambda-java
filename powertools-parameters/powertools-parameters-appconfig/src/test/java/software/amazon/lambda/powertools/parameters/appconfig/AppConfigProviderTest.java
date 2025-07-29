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

package software.amazon.lambda.powertools.parameters.appconfig;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalStateException;
import static org.assertj.core.api.Assertions.assertThatRuntimeException;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.mockito.MockitoAnnotations.openMocks;
import static software.amazon.lambda.powertools.parameters.transform.Transformer.json;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.Mockito;

import software.amazon.awssdk.core.SdkBytes;
import software.amazon.awssdk.services.appconfigdata.AppConfigDataClient;
import software.amazon.awssdk.services.appconfigdata.model.GetLatestConfigurationRequest;
import software.amazon.awssdk.services.appconfigdata.model.GetLatestConfigurationResponse;
import software.amazon.awssdk.services.appconfigdata.model.StartConfigurationSessionRequest;
import software.amazon.awssdk.services.appconfigdata.model.StartConfigurationSessionResponse;
import software.amazon.lambda.powertools.parameters.cache.CacheManager;
import software.amazon.lambda.powertools.parameters.transform.TransformationManager;

class AppConfigProviderTest {

    private static final String ENVIRONMENT_NAME = "test";
    private static final String DEFAULT_TEST_KEY = "key1";
    private static final String APPLICATION_NAME = "fakeApp";

    @Mock
    AppConfigDataClient client;

    @Captor
    ArgumentCaptor<StartConfigurationSessionRequest> startSessionRequestCaptor;

    @Captor
    ArgumentCaptor<GetLatestConfigurationRequest> getLatestConfigurationRequestCaptor;
    private AppConfigProvider provider;

    @BeforeEach
    void init() {
        openMocks(this);

        provider = AppConfigProvider.builder()
                .withClient(client)
                .withApplication(APPLICATION_NAME)
                .withEnvironment(ENVIRONMENT_NAME)
                .withCacheManager(new CacheManager())
                .withTransformationManager(new TransformationManager())
                .build();
    }

    /**
     * Tests repeated calls to the AppConfigProvider for the same key behave correctly. This is more complicated than
     * it seems, as the service itself will return no-data if the value of a property remains unchanged since the
     * start of a session. This means the provider must cache the result and return it again if it gets no data, but
     * subsequent calls should once again return the new data.
     */
    @Test
    void getValueRetrievesValue() {
        // Arrange
        StartConfigurationSessionResponse firstSession = StartConfigurationSessionResponse.builder()
                .initialConfigurationToken("token1")
                .build();
        // first response returns 'value1'
        GetLatestConfigurationResponse firstResponse = GetLatestConfigurationResponse.builder()
                .nextPollConfigurationToken("token2")
                .configuration(SdkBytes.fromUtf8String("value1"))
                .build();
        // Second response returns 'value2'
        GetLatestConfigurationResponse secondResponse = GetLatestConfigurationResponse.builder()
                .nextPollConfigurationToken("token3")
                .configuration(SdkBytes.fromUtf8String("value2"))
                .build();
        // Third response returns nothing, which means the provider should yield the previous value again
        GetLatestConfigurationResponse thirdResponse = GetLatestConfigurationResponse.builder()
                .nextPollConfigurationToken("token4")
                .build();
        // Forth response returns empty, which means the provider should yield the previous value again
        GetLatestConfigurationResponse forthResponse = GetLatestConfigurationResponse.builder()
                .nextPollConfigurationToken("token5")
                .configuration(SdkBytes.fromUtf8String(""))
                .build();
        Mockito.when(client.startConfigurationSession(startSessionRequestCaptor.capture()))
                .thenReturn(firstSession);
        Mockito.when(client.getLatestConfiguration(getLatestConfigurationRequestCaptor.capture()))
                .thenReturn(firstResponse, secondResponse, thirdResponse, forthResponse);

        // Act
        String returnedValue1 = provider.getValue(DEFAULT_TEST_KEY);
        String returnedValue2 = provider.getValue(DEFAULT_TEST_KEY);
        String returnedValue3 = provider.getValue(DEFAULT_TEST_KEY);
        String returnedValue4 = provider.getValue(DEFAULT_TEST_KEY);

        // Assert
        assertThat(returnedValue1).isEqualTo(firstResponse.configuration().asUtf8String());
        assertThat(returnedValue2).isEqualTo(secondResponse.configuration().asUtf8String());
        assertThat(returnedValue3).isEqualTo(secondResponse.configuration()
                .asUtf8String()); // Third response is mocked to return null and should re-use previous value
        assertThat(returnedValue4).isEqualTo(secondResponse.configuration()
                .asUtf8String()); // Forth response is mocked to return empty and should re-use previous value
        assertThat(startSessionRequestCaptor.getValue().applicationIdentifier()).isEqualTo(APPLICATION_NAME);
        assertThat(startSessionRequestCaptor.getValue().environmentIdentifier()).isEqualTo(ENVIRONMENT_NAME);
        assertThat(startSessionRequestCaptor.getValue().configurationProfileIdentifier()).isEqualTo(DEFAULT_TEST_KEY);
        assertThat(getLatestConfigurationRequestCaptor.getAllValues().get(0).configurationToken()).isEqualTo(
                firstSession.initialConfigurationToken());
        assertThat(getLatestConfigurationRequestCaptor.getAllValues().get(1).configurationToken()).isEqualTo(
                firstResponse.nextPollConfigurationToken());
        assertThat(getLatestConfigurationRequestCaptor.getAllValues().get(2).configurationToken()).isEqualTo(
                secondResponse.nextPollConfigurationToken());
    }

    @Test
    void getValueNoValueExists() {
        // Arrange
        StartConfigurationSessionResponse session = StartConfigurationSessionResponse.builder()
                .initialConfigurationToken("token1")
                .build();
        GetLatestConfigurationResponse response = GetLatestConfigurationResponse.builder()
                .nextPollConfigurationToken("token2")
                .build();
        Mockito.when(client.startConfigurationSession(startSessionRequestCaptor.capture()))
                .thenReturn(session);
        Mockito.when(client.getLatestConfiguration(getLatestConfigurationRequestCaptor.capture()))
                .thenReturn(response);

        // Act
        String returnedValue = provider.getValue(DEFAULT_TEST_KEY);

        // Assert
        assertThat(returnedValue).isNull();
    }

    /**
     * If we mix requests for different keys together through the same provider, retrieval should
     * work as expected. This means two separate configuration sessions should be established with AppConfig.
     */
    @Test
    void multipleKeysRetrievalWorks() {
        // Arrange
        String param1Key = "key1";
        StartConfigurationSessionResponse param1Session = StartConfigurationSessionResponse.builder()
                .initialConfigurationToken("token1a")
                .build();
        GetLatestConfigurationResponse param1Response = GetLatestConfigurationResponse.builder()
                .nextPollConfigurationToken("token1b")
                .configuration(SdkBytes.fromUtf8String("value1"))
                .build();
        String param2Key = "key2";
        StartConfigurationSessionResponse param2Session = StartConfigurationSessionResponse.builder()
                .initialConfigurationToken("token2a")
                .build();
        GetLatestConfigurationResponse param2Response = GetLatestConfigurationResponse.builder()
                .nextPollConfigurationToken("token2b")
                .configuration(SdkBytes.fromUtf8String("value1"))
                .build();
        Mockito.when(client.startConfigurationSession(startSessionRequestCaptor.capture()))
                .thenReturn(param1Session, param2Session);
        Mockito.when(client.getLatestConfiguration(getLatestConfigurationRequestCaptor.capture()))
                .thenReturn(param1Response, param2Response);

        // Act
        String firstKeyValue = provider.getValue(param1Key);
        String secondKeyValue = provider.getValue(param2Key);

        // Assert
        assertThat(firstKeyValue).isEqualTo(param1Response.configuration().asUtf8String());
        assertThat(secondKeyValue).isEqualTo(param2Response.configuration().asUtf8String());
        assertThat(startSessionRequestCaptor.getAllValues().get(0).configurationProfileIdentifier()).isEqualTo(
                param1Key);
        assertThat(startSessionRequestCaptor.getAllValues().get(1).configurationProfileIdentifier()).isEqualTo(
                param2Key);
        assertThat(getLatestConfigurationRequestCaptor.getAllValues().get(0).configurationToken()).isEqualTo(
                param1Session.initialConfigurationToken());
        assertThat(getLatestConfigurationRequestCaptor.getAllValues().get(1).configurationToken()).isEqualTo(
                param2Session.initialConfigurationToken());
    }

    @Test
    void getMultipleValuesThrowsException() {
        // Act & Assert
        assertThatRuntimeException().isThrownBy(() -> provider.getMultipleValues("path"))
                .withMessage("Retrieving multiple parameter values is not supported with the AWS App Config Provider");
    }

    @Test
    void testAppConfigProviderBuilderMissingEnvironment_throwsException() {
        // Act & Assert
        assertThatIllegalStateException().isThrownBy(() -> AppConfigProvider.builder()
                .withCacheManager(new CacheManager())
                .withApplication(APPLICATION_NAME)
                .withClient(client)
                .build())
                .withMessage("No environment provided; please provide one");
    }

    @Test
    void testAppConfigProviderBuilderMissingApplication_throwsException() {
        // Act & Assert
        assertThatIllegalStateException().isThrownBy(() -> AppConfigProvider.builder()
                .withCacheManager(new CacheManager())
                .withEnvironment(ENVIRONMENT_NAME)
                .withClient(client)
                .build())
                .withMessage("No application provided; please provide one");
    }

    @Test
    void testAppConfigProvider_withoutParameter_shouldHaveDefaultTransformationManager() {
        // Act
        AppConfigProvider appConfigProvider = AppConfigProvider.builder()
                .withEnvironment("test")
                .withApplication("app")
                .build();
        // Assert
        assertDoesNotThrow(() -> appConfigProvider.withTransformation(json));
    }
}

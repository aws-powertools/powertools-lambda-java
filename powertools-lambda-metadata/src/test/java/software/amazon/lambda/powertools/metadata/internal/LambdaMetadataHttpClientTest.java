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

package software.amazon.lambda.powertools.metadata.internal;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.equalTo;
import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.stubFor;
import static com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;

import com.github.tomakehurst.wiremock.junit5.WireMockRuntimeInfo;
import com.github.tomakehurst.wiremock.junit5.WireMockTest;

import software.amazon.lambda.powertools.metadata.LambdaMetadata;
import software.amazon.lambda.powertools.metadata.exception.LambdaMetadataException;

@WireMockTest
class LambdaMetadataHttpClientTest {

    private static final String TEST_TOKEN = "test-token-12345";
    private static final String METADATA_PATH = "/2026-01-15/metadata/execution-environment";

    @Test
    void fetchMetadata_shouldReturnMetadata(WireMockRuntimeInfo wmRuntimeInfo) {
        // Given
        stubFor(get(urlEqualTo(METADATA_PATH))
                .withHeader("Authorization", equalTo("Bearer " + TEST_TOKEN))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "application/json")
                        .withBody("{\"AvailabilityZoneID\": \"use1-az1\"}")));

        LambdaMetadataHttpClient client = createClient(wmRuntimeInfo);

        // When
        LambdaMetadata metadata = client.fetchMetadata();

        // Then
        assertThat(metadata).isNotNull();
        assertThat(metadata.getAvailabilityZoneId()).isEqualTo("use1-az1");
    }

    @Test
    void fetchMetadata_shouldHandleUnknownFields(WireMockRuntimeInfo wmRuntimeInfo) {
        // Given - response with extra fields that should be ignored
        stubFor(get(urlEqualTo(METADATA_PATH))
                .withHeader("Authorization", equalTo("Bearer " + TEST_TOKEN))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "application/json")
                        .withBody("{\"AvailabilityZoneID\": \"use1-az2\", \"FutureField\": \"value\"}")));

        LambdaMetadataHttpClient client = createClient(wmRuntimeInfo);

        // When
        LambdaMetadata metadata = client.fetchMetadata();

        // Then
        assertThat(metadata).isNotNull();
        assertThat(metadata.getAvailabilityZoneId()).isEqualTo("use1-az2");
    }

    @Test
    void fetchMetadata_shouldThrowOnNon200Status(WireMockRuntimeInfo wmRuntimeInfo) {
        // Given
        stubFor(get(urlEqualTo(METADATA_PATH))
                .willReturn(aResponse()
                        .withStatus(500)
                        .withBody("Internal Server Error")));

        LambdaMetadataHttpClient client = createClient(wmRuntimeInfo);

        // When/Then
        assertThatThrownBy(client::fetchMetadata)
                .isInstanceOf(LambdaMetadataException.class)
                .hasMessageContaining("status 500")
                .satisfies(e -> {
                    LambdaMetadataException ex = (LambdaMetadataException) e;
                    assertThat(ex.getStatusCode()).isEqualTo(500);
                });
    }

    @Test
    void fetchMetadata_shouldThrowOnMissingToken() {
        // Given
        LambdaMetadataHttpClient client = new LambdaMetadataHttpClient() {
            @Override
            String getRequiredEnvironmentVariable(String name) {
                if (LambdaMetadataHttpClient.ENV_METADATA_API.equals(name)) {
                    return "localhost:8080";
                }
                return super.getRequiredEnvironmentVariable(name);
            }
        };

        // When/Then
        assertThatThrownBy(client::fetchMetadata)
                .isInstanceOf(LambdaMetadataException.class)
                .hasMessageContaining(LambdaMetadataHttpClient.ENV_METADATA_TOKEN);
    }

    @Test
    void fetchMetadata_shouldThrowOnMissingApi() {
        // Given
        LambdaMetadataHttpClient client = new LambdaMetadataHttpClient() {
            @Override
            String getRequiredEnvironmentVariable(String name) {
                if (LambdaMetadataHttpClient.ENV_METADATA_TOKEN.equals(name)) {
                    return TEST_TOKEN;
                }
                return super.getRequiredEnvironmentVariable(name);
            }
        };

        // When/Then
        assertThatThrownBy(client::fetchMetadata)
                .isInstanceOf(LambdaMetadataException.class)
                .hasMessageContaining(LambdaMetadataHttpClient.ENV_METADATA_API);
    }

    @Test
    void fetchMetadata_shouldThrowOn404(WireMockRuntimeInfo wmRuntimeInfo) {
        // Given
        stubFor(get(urlEqualTo(METADATA_PATH))
                .willReturn(aResponse()
                        .withStatus(404)
                        .withBody("Not Found")));

        LambdaMetadataHttpClient client = createClient(wmRuntimeInfo);

        // When/Then
        assertThatThrownBy(client::fetchMetadata)
                .isInstanceOf(LambdaMetadataException.class)
                .satisfies(e -> {
                    LambdaMetadataException ex = (LambdaMetadataException) e;
                    assertThat(ex.getStatusCode()).isEqualTo(404);
                });
    }

    private LambdaMetadataHttpClient createClient(WireMockRuntimeInfo wmRuntimeInfo) {
        return new LambdaMetadataHttpClient() {
            @Override
            String getRequiredEnvironmentVariable(String name) {
                if (LambdaMetadataHttpClient.ENV_METADATA_TOKEN.equals(name)) {
                    return TEST_TOKEN;
                }
                if (LambdaMetadataHttpClient.ENV_METADATA_API.equals(name)) {
                    return "localhost:" + wmRuntimeInfo.getHttpPort();
                }
                return super.getRequiredEnvironmentVariable(name);
            }
        };
    }
}

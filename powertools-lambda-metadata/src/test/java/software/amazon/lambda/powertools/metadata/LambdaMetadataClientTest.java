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

package software.amazon.lambda.powertools.metadata;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import software.amazon.lambda.powertools.metadata.exception.LambdaMetadataException;
import software.amazon.lambda.powertools.metadata.internal.LambdaMetadataHttpClient;

class LambdaMetadataClientTest {

    private LambdaMetadataHttpClient mockHttpClient;

    @BeforeEach
    void setUp() {
        mockHttpClient = mock(LambdaMetadataHttpClient.class);
        LambdaMetadataClient.setHttpClient(mockHttpClient);
    }

    @AfterEach
    void tearDown() {
        LambdaMetadataClient.resetCache();
    }

    @Test
    void get_shouldReturnMetadata() {
        // Given
        LambdaMetadata metadata = new LambdaMetadata("use1-az1");
        when(mockHttpClient.fetchMetadata()).thenReturn(metadata);

        // When
        LambdaMetadata result = LambdaMetadataClient.get();

        // Then
        assertThat(result).isNotNull();
        assertThat(result.getAvailabilityZoneId()).isEqualTo("use1-az1");
    }

    @Test
    void get_shouldCacheMetadata() {
        // Given
        LambdaMetadata metadata = new LambdaMetadata("use1-az1");
        when(mockHttpClient.fetchMetadata()).thenReturn(metadata);

        // When
        LambdaMetadata first = LambdaMetadataClient.get();
        LambdaMetadata second = LambdaMetadataClient.get();

        // Then
        assertThat(first).isSameAs(second);
        verify(mockHttpClient, times(1)).fetchMetadata();
    }

    @Test
    void refresh_shouldFetchNewMetadata() {
        // Given
        LambdaMetadata metadata1 = new LambdaMetadata("use1-az1");
        LambdaMetadata metadata2 = new LambdaMetadata("use1-az2");
        when(mockHttpClient.fetchMetadata())
                .thenReturn(metadata1)
                .thenReturn(metadata2);

        // When
        LambdaMetadata first = LambdaMetadataClient.get();
        LambdaMetadata refreshed = LambdaMetadataClient.refresh();

        // Then
        assertThat(first.getAvailabilityZoneId()).isEqualTo("use1-az1");
        assertThat(refreshed.getAvailabilityZoneId()).isEqualTo("use1-az2");
        verify(mockHttpClient, times(2)).fetchMetadata();
    }

    @Test
    void get_shouldThrowExceptionOnError() {
        // Given
        when(mockHttpClient.fetchMetadata())
                .thenThrow(new LambdaMetadataException("Test error"));

        // When/Then
        assertThatThrownBy(LambdaMetadataClient::get)
                .isInstanceOf(LambdaMetadataException.class)
                .hasMessage("Test error");
    }

    @Test
    void afterRestore_shouldInvalidateCache() {
        // Given
        LambdaMetadata metadata1 = new LambdaMetadata("use1-az1");
        LambdaMetadata metadata2 = new LambdaMetadata("use1-az2");
        when(mockHttpClient.fetchMetadata())
                .thenReturn(metadata1)
                .thenReturn(metadata2);

        // When
        LambdaMetadata first = LambdaMetadataClient.get();

        // Simulate SnapStart restore
        LambdaMetadataClient.resetCache();

        LambdaMetadata afterRestore = LambdaMetadataClient.get();

        // Then
        assertThat(first.getAvailabilityZoneId()).isEqualTo("use1-az1");
        assertThat(afterRestore.getAvailabilityZoneId()).isEqualTo("use1-az2");
        verify(mockHttpClient, times(2)).fetchMetadata();
    }
}

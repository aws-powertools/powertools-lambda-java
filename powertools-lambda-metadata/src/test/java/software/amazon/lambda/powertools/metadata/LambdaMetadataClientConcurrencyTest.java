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
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import software.amazon.lambda.powertools.metadata.internal.LambdaMetadataHttpClient;

class LambdaMetadataClientConcurrencyTest {

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
    void get_shouldBeThreadSafe() throws Exception {
        // Given
        LambdaMetadata metadata = new LambdaMetadata("use1-az1");
        when(mockHttpClient.fetchMetadata()).thenReturn(metadata);

        int threadCount = 10;
        ExecutorService executor = Executors.newFixedThreadPool(threadCount);
        CountDownLatch startLatch = new CountDownLatch(1);
        List<Future<LambdaMetadata>> futures = new ArrayList<>();

        // When - all threads try to get metadata simultaneously
        for (int i = 0; i < threadCount; i++) {
            futures.add(executor.submit(() -> {
                startLatch.await();
                return LambdaMetadataClient.get();
            }));
        }
        startLatch.countDown();

        // Then - all threads should get the same instance
        LambdaMetadata firstResult = null;
        for (Future<LambdaMetadata> future : futures) {
            LambdaMetadata result = future.get(5, TimeUnit.SECONDS);
            if (firstResult == null) {
                firstResult = result;
            }
            assertThat(result).isSameAs(firstResult);
            assertThat(result.getAvailabilityZoneId()).isEqualTo("use1-az1");
        }

        executor.shutdown();
        executor.awaitTermination(5, TimeUnit.SECONDS);
    }
}

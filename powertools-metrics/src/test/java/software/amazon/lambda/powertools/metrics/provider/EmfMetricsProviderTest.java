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

package software.amazon.lambda.powertools.metrics.provider;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

import software.amazon.lambda.powertools.metrics.Metrics;
import software.amazon.lambda.powertools.metrics.internal.EmfMetricsLogger;

class EmfMetricsProviderTest {

    @Test
    void shouldCreateEmfMetricsLogger() {
        // Given
        EmfMetricsProvider provider = new EmfMetricsProvider();

        // When
        Metrics metrics = provider.getMetricsInstance();

        // Then
        assertThat(metrics)
                .isNotNull()
                .isInstanceOf(EmfMetricsLogger.class);
    }
}

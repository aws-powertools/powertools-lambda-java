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

package software.amazon.lambda.powertools.logging;

import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.slf4j.MDC;
import software.amazon.lambda.powertools.utilities.JsonConfig;


class LoggingUtilsTest {

    @BeforeEach
    void setUp() {
        MDC.clear();
    }

    @Test
    void shouldAddCorrelationIdToLoggingContext() {
        String id = "correlationID_12345";
        LoggingUtils.setCorrelationId(id);

        assertThat(MDC.getCopyOfContextMap())
                .hasSize(1)
                .containsEntry("correlation_id", id);

        assertThat(LoggingUtils.getCorrelationId()).isEqualTo(id);
    }

    @Test
    void shouldGetObjectMapper() {
        assertThat(LoggingUtils.getObjectMapper()).isNotNull();
        assertThat(LoggingUtils.getObjectMapper()).isEqualTo(JsonConfig.get().getObjectMapper());

        ObjectMapper mapper = new ObjectMapper().disable(SerializationFeature.FAIL_ON_EMPTY_BEANS);
        LoggingUtils.setObjectMapper(mapper);
        assertThat(LoggingUtils.getObjectMapper()).isEqualTo(mapper);

    }
}
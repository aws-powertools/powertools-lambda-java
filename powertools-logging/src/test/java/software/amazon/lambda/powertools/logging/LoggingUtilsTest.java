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
package software.amazon.lambda.powertools.logging;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.slf4j.MDC;

import java.util.HashMap;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;


class LoggingUtilsTest {

    @BeforeEach
    void setUp() {
        MDC.clear();
    }

    @Test
    void shouldSetCustomKeyOnThreadContext() {
        LoggingUtils.appendKey("org/slf4j/test", "value");

        assertThat(MDC.getCopyOfContextMap())
                .hasSize(1)
                .containsEntry("org/slf4j/test", "value");
    }

    @Test
    void shouldSetCustomKeyAsMapOnThreadContext() {
        Map<String, String> customKeys = new HashMap<>();
        customKeys.put("org/slf4j/test", "value");
        customKeys.put("test1", "value1");

        LoggingUtils.appendKeys(customKeys);

        assertThat(MDC.getCopyOfContextMap())
                .hasSize(2)
                .containsEntry("org/slf4j/test", "value")
                .containsEntry("test1", "value1");
    }

    @Test
    void shouldRemoveCustomKeyOnThreadContext() {
        LoggingUtils.appendKey("org/slf4j/test", "value");

        assertThat(MDC.getCopyOfContextMap())
                .hasSize(1)
                .containsEntry("org/slf4j/test", "value");

        LoggingUtils.removeKey("org/slf4j/test");

        assertThat(MDC.getCopyOfContextMap())
                .isEmpty();
    }

    @Test
    void shouldRemoveCustomKeysOnThreadContext() {
        Map<String, String> customKeys = new HashMap<>();
        customKeys.put("org/slf4j/test", "value");
        customKeys.put("test1", "value1");

        LoggingUtils.appendKeys(customKeys);

        assertThat(MDC.getCopyOfContextMap())
                .hasSize(2)
                .containsEntry("org/slf4j/test", "value")
                .containsEntry("test1", "value1");

        LoggingUtils.removeKeys("org/slf4j/test", "test1");

        assertThat(MDC.getCopyOfContextMap())
                .isEmpty();
    }
}
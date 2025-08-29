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
import static org.assertj.core.api.Assertions.assertThatCode;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import software.amazon.lambda.powertools.logging.internal.LoggingManagerRegistry;
import software.amazon.lambda.powertools.logging.internal.TestLoggingManager;

class PowertoolsLoggingTest {

    private TestLoggingManager testManager;

    @BeforeEach
    void setUp() {
        // Get the TestLoggingManager instance from registry
        testManager = (TestLoggingManager) LoggingManagerRegistry.getLoggingManager();
        testManager.resetBufferState();
    }

    @Test
    void testFlushBuffer_shouldNotThrowException() {
        // WHEN/THEN
        assertThatCode(PowertoolsLogging::flushBuffer).doesNotThrowAnyException();
    }

    @Test
    void testClearBuffer_shouldNotThrowException() {
        // WHEN/THEN
        assertThatCode(PowertoolsLogging::clearBuffer).doesNotThrowAnyException();
    }

    @Test
    void testFlushBuffer_shouldCallBufferManager() {
        // WHEN
        PowertoolsLogging.flushBuffer();

        // THEN
        assertThat(testManager.isBufferFlushed()).isTrue();
    }

    @Test
    void testClearBuffer_shouldCallBufferManager() {
        // WHEN
        PowertoolsLogging.clearBuffer();

        // THEN
        assertThat(testManager.isBufferCleared()).isTrue();
    }
}

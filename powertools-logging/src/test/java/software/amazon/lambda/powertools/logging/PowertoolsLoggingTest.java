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

import static org.apache.commons.lang3.reflect.FieldUtils.writeStaticField;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatNoException;
import static org.assertj.core.api.Assertions.contentOf;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.Method;
import java.nio.channels.FileChannel;
import java.nio.file.NoSuchFileException;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;

import com.amazonaws.services.lambda.runtime.Context;

import software.amazon.lambda.powertools.common.internal.LambdaHandlerProcessor;
import software.amazon.lambda.powertools.common.stubs.TestLambdaContext;
import software.amazon.lambda.powertools.logging.internal.LoggingConstants;
import software.amazon.lambda.powertools.logging.internal.LoggingManagerRegistry;
import software.amazon.lambda.powertools.logging.internal.PowertoolsLoggedFields;
import software.amazon.lambda.powertools.logging.internal.TestLoggingManager;

class PowertoolsLoggingTest {

    private static final Logger LOG = LoggerFactory.getLogger(PowertoolsLoggingTest.class);
    private TestLoggingManager testManager;
    private Context context;

    @BeforeEach
    void setUp() throws IllegalAccessException, IOException {
        // Get the TestLoggingManager instance from registry
        testManager = (TestLoggingManager) LoggingManagerRegistry.getLoggingManager();
        testManager.resetBufferState();

        context = new TestLambdaContext();

        // Reset environment variables for clean test isolation
        writeStaticField(LoggingConstants.class, "LAMBDA_LOG_LEVEL", null, true);
        writeStaticField(LoggingConstants.class, "POWERTOOLS_LOG_LEVEL", null, true);
        writeStaticField(LoggingConstants.class, "POWERTOOLS_SAMPLING_RATE", null, true);

        // Clear MDC for clean test isolation
        MDC.clear();

        // Reset cold start state
        writeStaticField(LambdaHandlerProcessor.class, "isColdStart", null, true);
        writeStaticField(PowertoolsLogging.class, "hasBeenInitialized", new AtomicBoolean(false), true);

        try {
            FileChannel.open(Paths.get("target/logfile.json"), StandardOpenOption.WRITE).truncate(0).close();
        } catch (NoSuchFileException e) {
            // may not be there in the first run
        }
    }

    @AfterEach
    void cleanUp() throws IOException {
        // Make sure file is cleaned up
        try {
            FileChannel.open(Paths.get("target/logfile.json"), StandardOpenOption.WRITE).truncate(0).close();
        } catch (NoSuchFileException e) {
            // may not be there in the first run
        }
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

    @Test
    void shouldLogDebugWhenPowertoolsLevelEnvVarIsDebug() throws IllegalAccessException {
        // GIVEN
        writeStaticField(LoggingConstants.class, "POWERTOOLS_LOG_LEVEL", "DEBUG", true);

        // WHEN
        reinitializeLogLevel();

        // THEN
        assertThat(LOG.isDebugEnabled()).isTrue();
    }

    @Test
    void shouldLogInfoWhenPowertoolsLevelEnvVarIsInfo() throws IllegalAccessException {
        // GIVEN
        writeStaticField(LoggingConstants.class, "POWERTOOLS_LOG_LEVEL", "INFO", true);

        // WHEN
        reinitializeLogLevel();

        // THEN
        assertThat(LOG.isDebugEnabled()).isFalse();
        assertThat(LOG.isInfoEnabled()).isTrue();
    }

    @Test
    void shouldLogInfoWhenPowertoolsLevelEnvVarIsInvalid() throws IllegalAccessException {
        // GIVEN
        writeStaticField(LoggingConstants.class, "POWERTOOLS_LOG_LEVEL", "INVALID", true);

        // WHEN
        reinitializeLogLevel();

        // THEN
        assertThat(LOG.isDebugEnabled()).isFalse();
        assertThat(LOG.isInfoEnabled()).isTrue();
    }

    @Test
    void shouldLogWarnWhenPowertoolsLevelEnvVarIsWarn() throws IllegalAccessException {
        // GIVEN
        writeStaticField(LoggingConstants.class, "POWERTOOLS_LOG_LEVEL", "WARN", true);

        // WHEN
        reinitializeLogLevel();

        // THEN
        assertThat(LOG.isDebugEnabled()).isFalse();
        assertThat(LOG.isInfoEnabled()).isFalse();
        assertThat(LOG.isWarnEnabled()).isTrue();
    }

    @Test
    void shouldLogErrorWhenPowertoolsLevelEnvVarIsError() throws IllegalAccessException {
        // GIVEN
        writeStaticField(LoggingConstants.class, "POWERTOOLS_LOG_LEVEL", "ERROR", true);

        // WHEN
        reinitializeLogLevel();

        // THEN
        assertThat(LOG.isDebugEnabled()).isFalse();
        assertThat(LOG.isInfoEnabled()).isFalse();
        assertThat(LOG.isWarnEnabled()).isFalse();
        assertThat(LOG.isErrorEnabled()).isTrue();
    }

    @Test
    void shouldLogErrorWhenPowertoolsLevelEnvVarIsFatal() throws IllegalAccessException {
        // GIVEN
        writeStaticField(LoggingConstants.class, "POWERTOOLS_LOG_LEVEL", "FATAL", true);

        // WHEN
        reinitializeLogLevel();

        // THEN
        assertThat(LOG.isDebugEnabled()).isFalse();
        assertThat(LOG.isInfoEnabled()).isFalse();
        assertThat(LOG.isWarnEnabled()).isFalse();
        assertThat(LOG.isErrorEnabled()).isTrue();
    }

    @Test
    void shouldLogWarnWhenPowertoolsLevelEnvVarIsWarnAndLambdaLevelVarIsInfo() throws IllegalAccessException {
        // GIVEN
        writeStaticField(LoggingConstants.class, "POWERTOOLS_LOG_LEVEL", "WARN", true);
        writeStaticField(LoggingConstants.class, "LAMBDA_LOG_LEVEL", "INFO", true);

        // WHEN
        reinitializeLogLevel();

        // THEN
        assertThat(LOG.isDebugEnabled()).isFalse();
        assertThat(LOG.isInfoEnabled()).isFalse();
        assertThat(LOG.isWarnEnabled()).isTrue();
        File logFile = new File("target/logfile.json");
        assertThat(contentOf(logFile))
                .doesNotContain(" does not match AWS Lambda Advanced Logging Controls minimum log level");
    }

    @Test
    void shouldLogInfoWhenPowertoolsLevelEnvVarIsInfoAndLambdaLevelVarIsWarn() throws IllegalAccessException {
        // GIVEN
        writeStaticField(LoggingConstants.class, "POWERTOOLS_LOG_LEVEL", "INFO", true);
        writeStaticField(LoggingConstants.class, "LAMBDA_LOG_LEVEL", "WARN", true);

        // WHEN
        reinitializeLogLevel();

        // THEN
        assertThat(LOG.isDebugEnabled()).isFalse();
        assertThat(LOG.isInfoEnabled()).isTrue();
        File logFile = new File("target/logfile.json");
        // should log a warning as powertools level is lower than lambda level
        assertThat(contentOf(logFile)).contains(
                "Current log level (INFO) does not match AWS Lambda Advanced Logging Controls minimum log level (WARN). This can lead to data loss, consider adjusting them.");
    }

    @Test
    void shouldLogWarnWhenPowertoolsLevelEnvVarINotSetAndLambdaLevelVarIsWarn() throws IllegalAccessException {
        // GIVEN
        writeStaticField(LoggingConstants.class, "POWERTOOLS_LOG_LEVEL", null, true);
        writeStaticField(LoggingConstants.class, "LAMBDA_LOG_LEVEL", "WARN", true);

        // WHEN
        reinitializeLogLevel();

        // THEN
        assertThat(LOG.isDebugEnabled()).isFalse();
        assertThat(LOG.isInfoEnabled()).isFalse();
        assertThat(LOG.isWarnEnabled()).isTrue();
    }

    @Test
    void initializeLogging_withContextOnly_shouldSetLambdaFields() {
        // WHEN
        PowertoolsLogging.initializeLogging(context);

        // THEN
        Map<String, String> mdcMap = MDC.getCopyOfContextMap();
        assertThat(mdcMap)
                .containsEntry(PowertoolsLoggedFields.FUNCTION_NAME.getName(), "test-function")
                .containsEntry(PowertoolsLoggedFields.FUNCTION_VERSION.getName(), "1")
                .containsEntry(PowertoolsLoggedFields.FUNCTION_COLD_START.getName(), "true")
                .containsEntry(PowertoolsLoggedFields.SERVICE.getName(), "testService");
    }

    @Test
    void initializeLogging_withSamplingRate_shouldSetSamplingRateInMdc() {
        // WHEN
        PowertoolsLogging.initializeLogging(context, 0.5);

        // THEN
        assertThat(MDC.get(PowertoolsLoggedFields.SAMPLING_RATE.getName())).isEqualTo("0.5");
    }

    @Test
    void initializeLogging_withCorrelationId_shouldExtractFromEvent() {
        // GIVEN
        Map<String, Object> event = Map.of("requestContext", Map.of("requestId", "test-correlation-id"));

        // WHEN
        PowertoolsLogging.initializeLogging(context, "requestContext.requestId", event);

        // THEN
        assertThat(MDC.get(PowertoolsLoggedFields.CORRELATION_ID.getName())).isEqualTo("test-correlation-id");
    }

    @Test
    void initializeLogging_withFullConfiguration_shouldSetAllFields() {
        // GIVEN
        Map<String, Object> event = Map.of("id", "correlation-123");

        // WHEN
        PowertoolsLogging.initializeLogging(context, 0.5, "id", event);

        // THEN
        Map<String, String> mdcMap = MDC.getCopyOfContextMap();
        assertThat(mdcMap)
                .containsEntry(PowertoolsLoggedFields.FUNCTION_NAME.getName(), "test-function")
                .containsEntry(PowertoolsLoggedFields.CORRELATION_ID.getName(), "correlation-123")
                .containsEntry(PowertoolsLoggedFields.SAMPLING_RATE.getName(), "0.5");
    }

    @Test
    void initializeLogging_withInvalidSamplingRate_shouldSkipSampling() {
        // WHEN
        PowertoolsLogging.initializeLogging(context, 2.0);

        // THEN
        assertThat(MDC.get(PowertoolsLoggedFields.SAMPLING_RATE.getName())).isNull();
    }

    @Test
    void initializeLogging_withEnvVarAndParameter_shouldUseEnvVarPrecedence() throws IllegalAccessException {
        // GIVEN
        writeStaticField(LoggingConstants.class, "POWERTOOLS_SAMPLING_RATE", "0.8", true);

        // WHEN
        PowertoolsLogging.initializeLogging(context, 0.3);

        // THEN
        assertThat(MDC.get(PowertoolsLoggedFields.SAMPLING_RATE.getName())).isEqualTo("0.8");
    }

    @Test
    void initializeLogging_calledTwice_shouldMarkColdStartDoneOnSecondCall() throws IllegalAccessException {
        // GIVEN
        writeStaticField(PowertoolsLogging.class, "hasBeenInitialized", new AtomicBoolean(false), true);

        // WHEN - First call
        PowertoolsLogging.initializeLogging(context);
        String firstCallColdStart = MDC.get(PowertoolsLoggedFields.FUNCTION_COLD_START.getName());

        // WHEN - Second call
        PowertoolsLogging.initializeLogging(context);
        String secondCallColdStart = MDC.get(PowertoolsLoggedFields.FUNCTION_COLD_START.getName());

        // THEN
        assertThat(firstCallColdStart).isEqualTo("true");
        assertThat(secondCallColdStart).isEqualTo("false");
    }

    @Test
    void initializeLogging_withNullContext_shouldNotThrow() {
        // WHEN & THEN
        assertThatNoException().isThrownBy(() -> {
            PowertoolsLogging.initializeLogging(null);
            PowertoolsLogging.initializeLogging(null, 0.5);
            PowertoolsLogging.initializeLogging(null, "path", Map.of());
            PowertoolsLogging.initializeLogging(null, 0.5, "path", Map.of());
        });
    }

    @Test
    void clearState_shouldClearMdcAndBuffer() {
        // GIVEN
        MDC.put("test", "value");

        // WHEN
        PowertoolsLogging.clearState(true);

        // THEN
        assertThat(MDC.getCopyOfContextMap()).isNull();
        assertThat(testManager.isBufferCleared()).isTrue();
    }

    @Test
    void clearState_withoutMdcClear_shouldOnlyClearBuffer() {
        // GIVEN
        MDC.put("test", "value");

        // WHEN
        PowertoolsLogging.clearState(false);

        // THEN
        assertThat(MDC.get("test")).isEqualTo("value");
        assertThat(testManager.isBufferCleared()).isTrue();
    }

    @Test
    void initializeLogging_concurrentCalls_shouldBeThreadSafe() throws InterruptedException {
        // GIVEN
        int threadCount = 10;
        Thread[] threads = new Thread[threadCount];
        String[] samplingRates = new String[threadCount];
        boolean[] coldStarts = new boolean[threadCount];
        boolean[] success = new boolean[threadCount];

        // WHEN - Multiple threads call initializeLogging with alternating sampling rates
        for (int i = 0; i < threadCount; i++) {
            final int threadIndex = i;
            final double samplingRate = (i % 2 == 0) ? 1.0 : 0.0; // Alternate between 1.0 and 0.0

            threads[i] = new Thread(() -> {
                try {
                    PowertoolsLogging.initializeLogging(context, samplingRate);

                    // Capture the sampling rate and cold start values set in MDC (thread-local)
                    samplingRates[threadIndex] = MDC.get(PowertoolsLoggedFields.SAMPLING_RATE.getName());
                    coldStarts[threadIndex] = Boolean
                            .parseBoolean(MDC.get(PowertoolsLoggedFields.FUNCTION_COLD_START.getName()));
                    success[threadIndex] = true;

                    // Clean up thread-local state
                    PowertoolsLogging.clearState(true);
                } catch (Exception e) {
                    success[threadIndex] = false;
                }
            });
        }

        // Start all threads
        for (Thread thread : threads) {
            thread.start();
        }

        // Wait for all threads to complete
        for (Thread thread : threads) {
            thread.join();
        }

        // THEN - All threads should complete successfully
        for (boolean result : success) {
            assertThat(result).isTrue();
        }

        // THEN - Each thread should have its own sampling rate in MDC and exactly one invocation was a cold start
        int coldStartCount = 0;
        for (int i = 0; i < threadCount; i++) {
            String expectedSamplingRate = (i % 2 == 0) ? "1.0" : "0.0";
            assertThat(samplingRates[i]).as("Thread %d should have sampling rate %s", i, expectedSamplingRate)
                    .isEqualTo(expectedSamplingRate);

            coldStartCount += coldStarts[i] ? 1 : 0;
        }
        assertThat(coldStartCount).isEqualTo(1);
    }

    @Test
    void withLogging_basicUsage_shouldInitializeAndCleanup() {
        // WHEN
        String result = PowertoolsLogging.withLogging(context, () -> {
            assertThat(MDC.get(PowertoolsLoggedFields.FUNCTION_NAME.getName())).isEqualTo("test-function");
            return "test-result";
        });

        // THEN
        assertThat(result).isEqualTo("test-result");
        assertThat(MDC.getCopyOfContextMap()).isNull();
        assertThat(testManager.isBufferCleared()).isTrue();
    }

    @Test
    void withLogging_withSamplingRate_shouldSetSamplingRateAndCleanup() {
        // WHEN
        String result = PowertoolsLogging.withLogging(context, 0.5, () -> {
            assertThat(MDC.get(PowertoolsLoggedFields.SAMPLING_RATE.getName())).isEqualTo("0.5");
            return "sampled-result";
        });

        // THEN
        assertThat(result).isEqualTo("sampled-result");
        assertThat(MDC.getCopyOfContextMap()).isNull();
    }

    @Test
    void withLogging_withCorrelationId_shouldExtractCorrelationIdAndCleanup() {
        // GIVEN
        Map<String, Object> event = Map.of("requestId", "correlation-123");

        // WHEN
        Integer result = PowertoolsLogging.withLogging(context, "requestId", event, () -> {
            assertThat(MDC.get(PowertoolsLoggedFields.CORRELATION_ID.getName())).isEqualTo("correlation-123");
            return 42;
        });

        // THEN
        assertThat(result).isEqualTo(42);
        assertThat(MDC.getCopyOfContextMap()).isNull();
    }

    @Test
    void withLogging_withFullConfiguration_shouldSetAllFieldsAndCleanup() {
        // GIVEN
        Map<String, Object> event = Map.of("id", "full-correlation");

        // WHEN
        Boolean result = PowertoolsLogging.withLogging(context, 0.8, "id", event, () -> {
            Map<String, String> mdcMap = MDC.getCopyOfContextMap();
            assertThat(mdcMap)
                    .containsEntry(PowertoolsLoggedFields.FUNCTION_NAME.getName(), "test-function")
                    .containsEntry(PowertoolsLoggedFields.CORRELATION_ID.getName(), "full-correlation")
                    .containsEntry(PowertoolsLoggedFields.SAMPLING_RATE.getName(), "0.8");
            return true;
        });

        // THEN
        assertThat(result).isTrue();
        assertThat(MDC.getCopyOfContextMap()).isNull();
    }

    @Test
    void withLogging_whenSupplierThrowsException_shouldStillCleanup() {
        // WHEN & THEN
        try {
            PowertoolsLogging.withLogging(context, () -> {
                assertThat(MDC.get(PowertoolsLoggedFields.FUNCTION_NAME.getName())).isEqualTo("test-function");
                throw new RuntimeException("test exception");
            });
        } catch (RuntimeException e) {
            assertThat(e.getMessage()).isEqualTo("test exception");
        }

        // THEN - cleanup should still happen
        assertThat(MDC.getCopyOfContextMap()).isNull();
        assertThat(testManager.isBufferCleared()).isTrue();
    }

    private void reinitializeLogLevel() {
        try {
            Method initializeLogLevel = PowertoolsLogging.class.getDeclaredMethod("initializeLogLevel");
            initializeLogLevel.setAccessible(true);
            initializeLogLevel.invoke(null);
        } catch (Exception e) {
            throw new RuntimeException("Failed to reinitialize log level", e);
        }
    }
}

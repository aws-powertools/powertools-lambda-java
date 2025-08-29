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

package software.amazon.lambda.powertools.logging.internal;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

import org.junit.jupiter.api.Test;

class LoggingManagerRegistryTest {

    @Test
    void testMultipleLoggingManagers_shouldWarnAndSelectFirstOne() throws UnsupportedEncodingException {
        // GIVEN
        List<LoggingManager> list = new ArrayList<>();
        list.add(new TestLoggingManager());
        list.add(new DefaultLoggingManager());

        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        PrintStream stream = new PrintStream(outputStream);

        // WHEN
        LoggingManagerRegistry.selectLoggingManager(list, stream);

        // THEN
        String output = outputStream.toString("UTF-8");
        assertThat(output)
                .contains("WARN. Multiple LoggingManagers were found on the classpath")
                .contains(
                        "WARN. Make sure to have only one of powertools-logging-log4j OR powertools-logging-logback to your dependencies")
                .contains("WARN. Using the first LoggingManager found on the classpath: [" + list.get(0) + "]");
    }

    @Test
    void testNoLoggingManagers_shouldWarnAndCreateDefault() throws UnsupportedEncodingException {
        // GIVEN
        List<LoggingManager> list = new ArrayList<>();

        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        PrintStream stream = new PrintStream(outputStream);

        // WHEN
        LoggingManager loggingManager = LoggingManagerRegistry.selectLoggingManager(list, stream);

        // THEN
        String output = outputStream.toString("UTF-8");
        assertThat(output)
                .contains("ERROR. No LoggingManager was found on the classpath")
                .contains("ERROR. Applying default LoggingManager: POWERTOOLS_LOG_LEVEL variable is ignored")
                .contains(
                        "ERROR. Make sure to add either powertools-logging-log4j or powertools-logging-logback to your dependencies");

        assertThat(loggingManager).isExactlyInstanceOf(DefaultLoggingManager.class);
    }

    @Test
    void testSingleLoggingManager_shouldReturnWithoutWarning() throws UnsupportedEncodingException {
        // GIVEN
        List<LoggingManager> list = new ArrayList<>();
        TestLoggingManager testManager = new TestLoggingManager();
        list.add(testManager);

        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        PrintStream stream = new PrintStream(outputStream);

        // WHEN
        LoggingManager loggingManager = LoggingManagerRegistry.selectLoggingManager(list, stream);

        // THEN
        String output = outputStream.toString("UTF-8");
        assertThat(output).isEmpty();
        assertThat(loggingManager).isSameAs(testManager);
        assertThat(loggingManager).isInstanceOf(BufferManager.class);
    }

    @Test
    void testGetLoggingManager_shouldReturnSameInstance() {
        // WHEN
        LoggingManager first = LoggingManagerRegistry.getLoggingManager();
        LoggingManager second = LoggingManagerRegistry.getLoggingManager();

        // THEN
        assertThat(first).isSameAs(second);
        assertThat(first).isNotNull();
        assertThat(first).isInstanceOf(BufferManager.class);
    }

    @Test
    void testGetLoggingManager_shouldBeThreadSafe() throws InterruptedException {
        // GIVEN
        int threadCount = 10;
        ExecutorService executor = Executors.newFixedThreadPool(threadCount);
        CountDownLatch latch = new CountDownLatch(threadCount);
        AtomicReference<LoggingManager> sharedInstance = new AtomicReference<>();

        // WHEN
        for (int i = 0; i < threadCount; i++) {
            executor.submit(() -> {
                try {
                    LoggingManager instance = LoggingManagerRegistry.getLoggingManager();
                    sharedInstance.compareAndSet(null, instance);
                    assertThat(instance).isSameAs(sharedInstance.get());
                } finally {
                    latch.countDown();
                }
            });
        }

        // THEN
        latch.await(5, TimeUnit.SECONDS);
        executor.shutdown();
        assertThat(sharedInstance.get()).isNotNull();
    }
}

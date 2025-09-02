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
import java.io.Closeable;
import java.io.IOException;
import java.io.PrintStream;
import java.nio.channels.FileChannel;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.Deque;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class KeyBufferTest {

    private KeyBuffer<String, String> buffer;
    private static final int MAX_BYTES = 20;

    @BeforeEach
    void setUp() throws IOException {
        buffer = new KeyBuffer<>(MAX_BYTES, String::length);
        // Clean up log file before each test
        try {
            FileChannel.open(Paths.get("target/logfile.json"), StandardOpenOption.WRITE).truncate(0).close();
        } catch (IOException e) {
            // may not be there in the first run
        }
    }

    @AfterEach
    void cleanUp() throws IOException {
        // Make sure file is cleaned up after each test
        FileChannel.open(Paths.get("target/logfile.json"), StandardOpenOption.WRITE).truncate(0).close();
    }

    @Test
    void shouldAddEventToBuffer() {
        buffer.add("key1", "test");

        Deque<String> events = buffer.removeAll("key1");
        assertThat(events).containsExactly("test");
    }

    @Test
    void shouldMaintainSeparateBuffersPerKey() {
        buffer.add("key1", "event1");
        buffer.add("key2", "event2");

        Deque<String> events1 = buffer.removeAll("key1");
        Deque<String> events2 = buffer.removeAll("key2");

        assertThat(events1).containsExactly("event1");
        assertThat(events2).containsExactly("event2");
    }

    @Test
    void shouldMaintainFIFOOrder() {
        buffer.add("key1", "first");
        buffer.add("key1", "second");
        buffer.add("key1", "third");

        Deque<String> events = buffer.removeAll("key1");
        assertThat(events).containsExactly("first", "second", "third");
    }

    @Test
    void shouldEvictOldestEventsWhenBufferOverflows() {
        // Add events that total exactly maxBytes
        buffer.add("key1", "12345678901234567890"); // 20 bytes

        // Add another event that causes overflow
        buffer.add("key1", "extra");

        Deque<String> events = buffer.removeAll("key1");
        assertThat(events).containsExactly("extra");
    }

    @Test
    void shouldEvictMultipleEventsIfNeeded() {
        buffer.add("key1", "1234567890"); // 10 bytes
        buffer.add("key1", "1234567890"); // 10 bytes, total 20
        buffer.add("key1", "12345678901234567890"); // 20 bytes, should evict both previous

        Deque<String> events = buffer.removeAll("key1");
        assertThat(events).containsExactly("12345678901234567890");
    }

    @Test
    void shouldEvictMultipleSmallEventsForLargeValidEvent() {
        // Add many small events that fill the buffer
        buffer.add("key1", "12"); // 2 bytes
        buffer.add("key1", "34"); // 2 bytes, total 4
        buffer.add("key1", "56"); // 2 bytes, total 6
        buffer.add("key1", "78"); // 2 bytes, total 8
        buffer.add("key1", "90"); // 2 bytes, total 10
        buffer.add("key1", "ab"); // 2 bytes, total 12
        buffer.add("key1", "cd"); // 2 bytes, total 14
        buffer.add("key1", "ef"); // 2 bytes, total 16
        buffer.add("key1", "gh"); // 2 bytes, total 18
        buffer.add("key1", "ij"); // 2 bytes, total 20 (exactly at limit)

        // Add a large event that requires multiple evictions
        buffer.add("key1", "123456789012345678"); // 18 bytes, should evict multiple small events

        Deque<String> events = buffer.removeAll("key1");
        // Should only contain the last few small events plus the large event
        // 18 bytes for large event leaves 2 bytes, so only "ij" should remain with the large event
        assertThat(events).containsExactly("ij", "123456789012345678");
    }

    @Test
    void shouldRejectEventLargerThanMaxBytes() {
        String largeEvent = "123456789012345678901"; // 21 bytes > 20 max

        buffer.add("key1", largeEvent);

        Deque<String> events = buffer.removeAll("key1");
        assertThat(events).isNull();
    }

    @Test
    void shouldNotEvictExistingEventsWhenRejectingLargeEvent() {
        buffer.add("key1", "small");

        String largeEvent = "123456789012345678901"; // 21 bytes > 20 max
        buffer.add("key1", largeEvent);

        Deque<String> events = buffer.removeAll("key1");
        assertThat(events).containsExactly("small");
    }

    @Test
    void shouldClearSpecificKeyBuffer() {
        buffer.add("key1", "event1");
        buffer.add("key2", "event2");

        buffer.clear("key1");

        assertThat(buffer.removeAll("key1")).isNull();
        assertThat(buffer.removeAll("key2")).containsExactly("event2");
    }

    @Test
    void shouldReturnNullForNonExistentKey() {
        Deque<String> events = buffer.removeAll("nonexistent");
        assertThat(events).isNull();
    }

    @Test
    void shouldReturnDefensiveCopyOnRemoveAll() {
        buffer.add("key1", "event");

        Deque<String> events1 = buffer.removeAll("key1");
        buffer.add("key1", "event");
        Deque<String> events2 = buffer.removeAll("key1");

        // Modifying first copy shouldn't affect second
        events1.add("modified");
        assertThat(events2).containsExactly("event");
        assertThat(events1).containsExactly("event", "modified");
    }

    @Test
    void shouldLogWarningOnOverflow() {
        StringBuilder warningCapture = new StringBuilder();
        KeyBuffer<String, String> testBuffer = new KeyBuffer<>(10, String::length,
                () -> warningCapture.append("Some logs are not displayed because they were evicted from the buffer"));

        // Cause overflow
        testBuffer.add("key1", "1234567890"); // 10 bytes
        testBuffer.add("key1", "extra"); // causes overflow

        // Trigger warning by removing
        testBuffer.removeAll("key1");

        assertThat(warningCapture.toString())
                .contains("Some logs are not displayed because they were evicted from the buffer");
    }

    @Test
    void shouldLogWarningOnLargeEventRejection() {
        StringBuilder warningCapture = new StringBuilder();
        KeyBuffer<String, String> testBuffer = new KeyBuffer<>(10, String::length,
                () -> warningCapture.append("Some logs are not displayed because they were evicted from the buffer"));

        // Add large event that gets rejected
        testBuffer.add("key1", "12345678901"); // 11 bytes > 10 max

        // Trigger warning by removing
        testBuffer.removeAll("key1");

        assertThat(warningCapture.toString())
                .contains("Some logs are not displayed because they were evicted from the buffer");
    }

    @Test
    void shouldNotLogWarningWhenNoOverflow() {
        StringBuilder warningCapture = new StringBuilder();
        KeyBuffer<String, String> testBuffer = new KeyBuffer<>(20, String::length,
                () -> warningCapture.append("Some logs are not displayed because they were evicted from the buffer"));

        testBuffer.add("key1", "small");
        testBuffer.removeAll("key1");

        assertThat(warningCapture.toString())
                .doesNotContain("Some logs are not displayed because they were evicted from the buffer");
    }

    @Test
    void shouldBeThreadSafeForDifferentKeys() throws InterruptedException, IOException {
        int threadCount = 10;
        int eventsPerThread = 100;
        KeyBuffer<String, String> largeBuffer = new KeyBuffer<>(10000, String::length);
        ExecutorService executor = Executors.newFixedThreadPool(threadCount);
        try (Closeable ignored = executor::shutdown) {
            CountDownLatch latch = new CountDownLatch(threadCount);

            // Each thread works with different key
            for (int i = 0; i < threadCount; i++) {
                final String key = "key" + i;
                executor.submit(() -> {
                    try {
                        for (int j = 0; j < eventsPerThread; j++) {
                            largeBuffer.add(key, "event" + j);
                        }
                    } finally {
                        latch.countDown();
                    }
                });
            }

            assertThat(latch.await(5, TimeUnit.SECONDS)).isTrue();

            // Verify each key has its events
            for (int i = 0; i < threadCount; i++) {
                String key = "key" + i;
                Deque<String> events = largeBuffer.removeAll(key);
                assertThat(events)
                        .isNotNull()
                        .hasSize(eventsPerThread);
            }
        }
    }

    @Test
    void shouldBeThreadSafeForSameKey() throws InterruptedException, IOException {
        int threadCount = 5;
        int eventsPerThread = 20;
        KeyBuffer<String, String> largeBuffer = new KeyBuffer<>(10000, String::length);
        ExecutorService executor = Executors.newFixedThreadPool(threadCount);
        try (Closeable ignored = executor::shutdown) {
            CountDownLatch latch = new CountDownLatch(threadCount);

            // All threads work with same key
            for (int i = 0; i < threadCount; i++) {
                final int threadId = i;
                executor.submit(() -> {
                    try {
                        for (int j = 0; j < eventsPerThread; j++) {
                            largeBuffer.add("sharedKey", "t" + threadId + "e" + j);
                        }
                    } finally {
                        latch.countDown();
                    }
                });
            }

            assertThat(latch.await(5, TimeUnit.SECONDS)).isTrue();

            Deque<String> events = largeBuffer.removeAll("sharedKey");
            assertThat(events)
                    .isNotNull()
                    .hasSize(threadCount * eventsPerThread);
        }
    }

    @Test
    void shouldHandleEmptyBuffer() {
        buffer.clear("nonexistent");
        assertThat(buffer.removeAll("nonexistent")).isNull();
    }

    @Test
    void shouldHandleZeroSizeEvents() {
        KeyBuffer<String, String> zeroBuffer = new KeyBuffer<>(10, s -> 0);

        zeroBuffer.add("key1", "event1");
        zeroBuffer.add("key1", "event2");

        Deque<String> events = zeroBuffer.removeAll("key1");
        assertThat(events).containsExactly("event1", "event2");
    }

    @Test
    void shouldUseCustomWarningLogger() {
        StringBuilder customWarning = new StringBuilder();
        KeyBuffer<String, String> testBuffer = new KeyBuffer<>(5, String::length,
                () -> customWarning.append("CUSTOM WARNING LOGGED"));

        // Cause overflow
        testBuffer.add("key1", "12345"); // 5 bytes
        testBuffer.add("key1", "extra"); // causes overflow

        // Trigger warning
        testBuffer.removeAll("key1");

        assertThat(customWarning).hasToString("CUSTOM WARNING LOGGED");
    }

    @Test
    void shouldUseDefaultWarningLoggerWhenNotProvided() {
        // Capture System.err output
        ByteArrayOutputStream errCapture = new ByteArrayOutputStream();
        PrintStream originalErr = System.err;
        try (PrintStream newErr = new PrintStream(errCapture)) {
            System.setErr(newErr);

            KeyBuffer<String, String> defaultBuffer = new KeyBuffer<>(5, String::length);

            // Cause overflow
            defaultBuffer.add("key1", "12345");
            defaultBuffer.add("key1", "extra");
            defaultBuffer.removeAll("key1");

            // Assert System.err received the warning
            assertThat(errCapture)
                    .hasToString(
                            "WARN [KeyBuffer] - Some logs are not displayed because they were evicted from the buffer. Increase buffer size to store more logs in the buffer.\n");
        } finally {
            System.setErr(originalErr);
        }
    }
}

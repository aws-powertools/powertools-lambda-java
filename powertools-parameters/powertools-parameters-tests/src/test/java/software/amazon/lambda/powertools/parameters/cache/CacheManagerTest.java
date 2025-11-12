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

package software.amazon.lambda.powertools.parameters.cache;

import static java.time.Clock.offset;
import static java.time.Duration.of;
import static java.time.temporal.ChronoUnit.SECONDS;
import static org.assertj.core.api.Assertions.assertThat;

import java.time.Clock;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class CacheManagerTest {

    CacheManager manager;

    Clock clock;

    @BeforeEach
    void setup() {
        clock = Clock.systemDefaultZone();
        manager = new CacheManager();
    }

    @Test
    void getIfNotExpired_notExpired_shouldReturnValue() {
        manager.putInCache("key", "value");

        Optional<String> value = manager.getIfNotExpired("key", clock.instant());

        assertThat(value).isPresent().contains("value");
    }

    @Test
    void getIfNotExpired_expired_shouldReturnNothing() {
        manager.putInCache("key", "value");

        Optional<String> value = manager.getIfNotExpired("key", offset(clock, of(6, SECONDS)).instant());

        assertThat(value).isNotPresent();
    }

    @Test
    void getIfNotExpired_withCustomExpirationTime_notExpired_shouldReturnValue() {
        manager.setExpirationTime(of(42, SECONDS));
        manager.putInCache("key", "value");

        Optional<String> value = manager.getIfNotExpired("key", offset(clock, of(40, SECONDS)).instant());

        assertThat(value).isPresent().contains("value");
    }

    @Test
    void getIfNotExpired_withCustomDefaultExpirationTime_notExpired_shouldReturnValue() {
        manager.setDefaultExpirationTime(of(42, SECONDS));
        manager.putInCache("key", "value");

        Optional<String> value = manager.getIfNotExpired("key", offset(clock, of(40, SECONDS)).instant());

        assertThat(value).isPresent().contains("value");
    }

    @Test
    void getIfNotExpired_customDefaultExpirationTime_customExpirationTime_shouldUseExpirationTime() {
        manager.setDefaultExpirationTime(of(42, SECONDS));
        manager.setExpirationTime(of(2, SECONDS));
        manager.putInCache("key", "value");

        Optional<String> value = manager.getIfNotExpired("key", offset(clock, of(40, SECONDS)).instant());

        assertThat(value).isNotPresent();
    }

    @Test
    void getIfNotExpired_resetExpirationTime_shouldUseDefaultExpirationTime() {
        manager.setDefaultExpirationTime(of(42, SECONDS));
        manager.setExpirationTime(of(2, SECONDS));
        manager.putInCache("key", "value");
        manager.resetExpirationTime();
        manager.putInCache("key2", "value2");

        Optional<String> value = manager.getIfNotExpired("key", offset(clock, of(40, SECONDS)).instant());
        Optional<String> value2 = manager.getIfNotExpired("key2", offset(clock, of(40, SECONDS)).instant());

        assertThat(value).isNotPresent();
        assertThat(value2).isPresent().contains("value2");
    }

    @Test
    void putInCache_sharedCache_shouldBeAccessibleAcrossThreads() throws InterruptedException {
        // GIVEN
        Thread thread1 = new Thread(() -> {
            manager.setExpirationTime(of(60, SECONDS));
            manager.putInCache("sharedKey", "valueFromThread1");
            manager.resetExpirationTime();
        });

        Thread thread2 = new Thread(() -> {
            manager.setExpirationTime(of(10, SECONDS));
            // Thread 2 should be able to read the value cached by Thread 1
            Optional<String> value = manager.getIfNotExpired("sharedKey", clock.instant());
            assertThat(value).isPresent().contains("valueFromThread1");
            manager.resetExpirationTime();
        });

        // WHEN
        thread1.start();
        thread1.join();
        thread2.start();
        thread2.join();

        // THEN - Both threads should be able to access the same cached value
        Optional<String> value = manager.getIfNotExpired("sharedKey", clock.instant());
        assertThat(value).isPresent().contains("valueFromThread1");
    }

    @Test
    void putInCache_concurrentCalls_shouldBeThreadSafe() throws InterruptedException {
        // GIVEN
        int threadCount = 10;
        Thread[] threads = new Thread[threadCount];
        boolean[] success = new boolean[threadCount];
        Clock testClock = Clock.systemDefaultZone();

        // WHEN - Multiple threads set different expiration times and cache values concurrently
        for (int i = 0; i < threadCount; i++) {
            final int threadIndex = i;
            final int expirationSeconds = (i % 2 == 0) ? 60 : 10; // Alternate between 60s and 10s

            threads[i] = new Thread(() -> {
                try {
                    manager.setExpirationTime(of(expirationSeconds, SECONDS));
                    manager.putInCache("key" + threadIndex, "value" + threadIndex);
                    manager.resetExpirationTime();
                    success[threadIndex] = true;
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

        // THEN - Each cached value should have the correct expiration time
        // Values with 60s TTL should still be present after 9s, values with 10s should expire after 11s
        for (int i = 0; i < threadCount; i++) {
            final int expirationSeconds = (i % 2 == 0) ? 60 : 10;

            // Check that value is still present just before expiration
            Optional<String> valueBeforeExpiry = manager.getIfNotExpired("key" + i,
                    offset(testClock, of(expirationSeconds - 1, SECONDS)).instant());
            assertThat(valueBeforeExpiry)
                    .as("Thread %d with %ds expiration should still have value after %ds", i, expirationSeconds,
                            expirationSeconds - 1)
                    .isPresent()
                    .contains("value" + i);

            // Check that value expires after the TTL
            Optional<String> valueAfterExpiry = manager.getIfNotExpired("key" + i,
                    offset(testClock, of(expirationSeconds + 1, SECONDS)).instant());
            assertThat(valueAfterExpiry)
                    .as("Thread %d with %ds expiration should not have value after %ds", i, expirationSeconds,
                            expirationSeconds + 1)
                    .isNotPresent();
        }
    }

}

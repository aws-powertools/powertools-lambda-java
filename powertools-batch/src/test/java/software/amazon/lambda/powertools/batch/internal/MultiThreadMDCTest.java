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
package software.amazon.lambda.powertools.batch.internal;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicBoolean;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThatNoException;

class MultiThreadMDCTest {

    @Nested
    class ConcurrencyTests {

        private static final int REMOVE_THREAD_WORKERS_COUNT = 10;
        private static final int COPY_MDC_TO_THREAD_ITERATIONS = 100000;
        private static final String THREAD_NAME = "test";

        @Test
        void shouldBeThreadSafe() {
            MultiThreadMDC multiThreadMDC = new MultiThreadMDC();
            AtomicBoolean stopSignal = new AtomicBoolean(false);
            ExecutorService executor = Executors.newFixedThreadPool(REMOVE_THREAD_WORKERS_COUNT + 1);
            try {
                List<CompletableFuture<Void>> futures = new ArrayList<>();
                for (int i = 0; i < REMOVE_THREAD_WORKERS_COUNT; i++) {
                    futures.add(CompletableFuture.runAsync(() -> threadRemover(multiThreadMDC, stopSignal), executor));
                }
                futures.add(CompletableFuture.runAsync(() -> mdcCopier(multiThreadMDC, stopSignal), executor));
                assertThatNoException().isThrownBy(() -> futures.forEach(CompletableFuture::join));
            } finally {
                executor.shutdown();
            }
        }

        private static void mdcCopier(MultiThreadMDC multiThreadMDC, AtomicBoolean stopSignal) {
            try {
                for (int i = 0; i < COPY_MDC_TO_THREAD_ITERATIONS; i++) {
                    multiThreadMDC.copyMDCToThread(THREAD_NAME);
                }
            } finally {
                stopSignal.set(true);
            }
        }

        private static void threadRemover(MultiThreadMDC multiThreadMDC, AtomicBoolean stopSignal) {
            try {
                while (!stopSignal.get()) {
                    multiThreadMDC.removeThread(THREAD_NAME);
                }
            } finally {
                stopSignal.set(true);
            }
        }
    }
}

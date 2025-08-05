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

package software.amazon.lambda.powertools.testutils;

import java.time.Duration;
import java.util.function.Supplier;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.github.resilience4j.retry.Retry;
import io.github.resilience4j.retry.RetryConfig;

/**
 * Utility class for consistent retry configuration across all test utilities.
 */
public class RetryUtils {
    private static final Logger LOG = LoggerFactory.getLogger(RetryUtils.class);

    private static final RetryConfig DEFAULT_RETRY_CONFIG = RetryConfig.custom()
            .maxAttempts(60) // 60 attempts over 5 minutes
            .waitDuration(Duration.ofSeconds(5)) // 5 seconds between attempts
            .build();

    private RetryUtils() {
        // Utility class
    }

    /**
     * Creates a retry instance with default configuration for the specified exception type.
     * 
     * @param name the name for the retry instance
     * @param retryOnException the exception class to retry on
     * @return configured Retry instance
     */
    public static Retry createRetry(String name, Class<? extends Exception> retryOnException) {
        RetryConfig config = RetryConfig.from(DEFAULT_RETRY_CONFIG)
                .retryExceptions(retryOnException)
                .build();

        Retry retry = Retry.of(name, config);
        retry.getEventPublisher().onRetry(event -> LOG.warn("Retry attempt {} for {}: {}",
                event.getNumberOfRetryAttempts(), name, event.getLastThrowable().getMessage()));

        return retry;
    }

    /**
     * Decorates a supplier with retry logic for the specified exception type.
     * 
     * @param supplier the supplier to decorate
     * @param name the name for the retry instance
     * @param retryOnException the exception class to retry on
     * @return decorated supplier with retry logic
     */
    public static <T> Supplier<T> withRetry(Supplier<T> supplier, String name,
            Class<? extends Exception> retryOnException) {
        Retry retry = createRetry(name, retryOnException);
        return Retry.decorateSupplier(retry, supplier);
    }
}

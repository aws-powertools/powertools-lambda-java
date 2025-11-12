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

import static java.time.temporal.ChronoUnit.SECONDS;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicReference;

/**
 * Manages caching of parameter values with configurable expiration times.
 * <p>
 * This class is thread-safe. The cache storage is shared across all threads,
 * while expiration time configuration is thread-local to support concurrent
 * requests with different cache TTL requirements.
 */
public class CacheManager {
    static final Duration DEFAULT_MAX_AGE_SECS = Duration.of(5, SECONDS);

    private final DataStore store;
    private final AtomicReference<Duration> defaultMaxAge = new AtomicReference<>(DEFAULT_MAX_AGE_SECS);
    private final ThreadLocal<Duration> maxAge = ThreadLocal.withInitial(() -> null);

    public CacheManager() {
        store = new DataStore();
    }

    @SuppressWarnings("unchecked") // DataStore stores Object, safe cast as we control what's stored
    public <T> Optional<T> getIfNotExpired(String key, Instant now) {
        if (store.hasExpired(key, now)) {
            return Optional.empty();
        }
        return Optional.of((T) store.get(key));
    }

    public void setExpirationTime(Duration duration) {
        this.maxAge.set(duration);
    }

    public void setDefaultExpirationTime(Duration duration) {
        this.defaultMaxAge.set(duration);
    }

    public <T> void putInCache(String key, T value) {
        Duration effectiveMaxAge = maxAge.get() != null ? maxAge.get() : defaultMaxAge.get();
        store.put(key, value, Clock.systemDefaultZone().instant().plus(effectiveMaxAge));
    }

    public void resetExpirationTime() {
        maxAge.remove();
    }
}

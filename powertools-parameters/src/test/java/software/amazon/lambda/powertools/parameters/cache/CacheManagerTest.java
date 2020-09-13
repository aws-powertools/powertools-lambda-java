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
package software.amazon.lambda.powertools.parameters.cache;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.mockito.Spy;

import java.time.Duration;
import java.time.Instant;
import java.util.Optional;

import static java.time.temporal.ChronoUnit.SECONDS;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.MockitoAnnotations.openMocks;

public class CacheManagerTest {

    @Spy
    NowProvider nowProvider;

    CacheManager manager;

    @BeforeEach
    public void setup() {
        openMocks(this);
        manager = new CacheManager(nowProvider);
    }

    @Test
    public void getIfNotExpired_notExpired_shouldReturnValue() {
        manager.putInCache("key", "value");

        Optional<String> value = manager.getIfNotExpired("key");

        assertThat(value.isPresent()).isTrue();
        assertThat(value.get()).isEqualTo("value");
    }

    @Test
    public void getIfNotExpired_expired_shouldReturnNothing() {
        manager.putInCache("key", "value");

        Instant now = Instant.now();
        Mockito.when(nowProvider.now()).thenReturn(now.plus(6, SECONDS));

        Optional<String> value = manager.getIfNotExpired("key");

        assertThat(value.isPresent()).isFalse();
    }

    @Test
    public void getIfNotExpired_withCustomExpirationTime_notExpired_shouldReturnValue() {
        manager.setExpirationTime(Duration.of(42, SECONDS));
        manager.putInCache("key", "value");

        Instant now = Instant.now();
        Mockito.when(nowProvider.now()).thenReturn(now.plus(40, SECONDS));

        Optional<String> value = manager.getIfNotExpired("key");

        assertThat(value.isPresent()).isTrue();
        assertThat(value.get()).isEqualTo("value");
    }

    @Test
    public void getIfNotExpired_withCustomDefaultExpirationTime_notExpired_shouldReturnValue() {
        manager.setDefaultExpirationTime(Duration.of(42, SECONDS));
        manager.putInCache("key", "value");

        Instant now = Instant.now();
        Mockito.when(nowProvider.now()).thenReturn(now.plus(40, SECONDS));

        Optional<String> value = manager.getIfNotExpired("key");

        assertThat(value.isPresent()).isTrue();
        assertThat(value.get()).isEqualTo("value");
    }

    @Test
    public void getIfNotExpired_customDefaultExpirationTime_customExpirationTime_shouldUseExpirationTime() {
        manager.setDefaultExpirationTime(Duration.of(42, SECONDS));
        manager.setExpirationTime(Duration.of(2, SECONDS));
        manager.putInCache("key", "value");

        Instant now = Instant.now();
        Mockito.when(nowProvider.now()).thenReturn(now.plus(40, SECONDS));

        Optional<String> value = manager.getIfNotExpired("key");

        assertThat(value.isPresent()).isFalse();
    }

    @Test
    public void getIfNotExpired_resetExpirationTime_shouldUseDefaultExpirationTime() {
        manager.setDefaultExpirationTime(Duration.of(42, SECONDS));
        manager.setExpirationTime(Duration.of(2, SECONDS));
        manager.putInCache("key", "value");
        manager.resetExpirationtime();
        manager.putInCache("key2", "value2");

        Instant now = Instant.now();
        Mockito.when(nowProvider.now()).thenReturn(now.plus(40, SECONDS));

        Optional<String> value = manager.getIfNotExpired("key");
        Optional<String> value2 = manager.getIfNotExpired("key2");

        assertThat(value.isPresent()).isFalse();
        assertThat(value2.isPresent()).isTrue();
    }

}

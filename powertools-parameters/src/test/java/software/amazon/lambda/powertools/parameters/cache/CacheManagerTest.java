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

public class CacheManagerTest {

    CacheManager manager;

    Clock clock;

    @BeforeEach
    public void setup() {
        clock = Clock.systemDefaultZone();
        manager = new CacheManager();
    }

    @Test
    public void getIfNotExpired_notExpired_shouldReturnValue() {
        manager.putInCache("key", "value");

        Optional<String> value = manager.getIfNotExpired("key", clock.instant());

        assertThat(value).isPresent().contains("value");
    }

    @Test
    public void getIfNotExpired_expired_shouldReturnNothing() {
        manager.putInCache("key", "value");

        Optional<String> value = manager.getIfNotExpired("key", offset(clock, of(6, SECONDS)).instant());

        assertThat(value).isNotPresent();
    }

    @Test
    public void getIfNotExpired_withCustomExpirationTime_notExpired_shouldReturnValue() {
        manager.setExpirationTime(of(42, SECONDS));
        manager.putInCache("key", "value");

        Optional<String> value = manager.getIfNotExpired("key", offset(clock, of(40, SECONDS)).instant());

        assertThat(value).isPresent().contains("value");
    }

    @Test
    public void getIfNotExpired_withCustomDefaultExpirationTime_notExpired_shouldReturnValue() {
        manager.setDefaultExpirationTime(of(42, SECONDS));
        manager.putInCache("key", "value");


        Optional<String> value = manager.getIfNotExpired("key", offset(clock, of(40, SECONDS)).instant());

        assertThat(value).isPresent().contains("value");
    }

    @Test
    public void getIfNotExpired_customDefaultExpirationTime_customExpirationTime_shouldUseExpirationTime() {
        manager.setDefaultExpirationTime(of(42, SECONDS));
        manager.setExpirationTime(of(2, SECONDS));
        manager.putInCache("key", "value");

        Optional<String> value = manager.getIfNotExpired("key", offset(clock, of(40, SECONDS)).instant());

        assertThat(value).isNotPresent();
    }

    @Test
    public void getIfNotExpired_resetExpirationTime_shouldUseDefaultExpirationTime() {
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

}

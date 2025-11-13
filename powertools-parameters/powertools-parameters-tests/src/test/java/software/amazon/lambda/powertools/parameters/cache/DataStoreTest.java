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
import java.time.Instant;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class DataStoreTest {

    Clock clock;
    DataStore store;

    @BeforeEach
    void setup() {
        clock = Clock.systemDefaultZone();
        store = new DataStore();
    }

    @Test
    void put_shouldInsertInStore() {
        store.put("key", "value", Instant.now());
        assertThat(store.get("key")).isEqualTo("value");
    }

    @Test
    void get_invalidKey_shouldReturnNull() {
        assertThat(store.get("key")).isNull();
    }

    @Test
    void hasExpired_invalidKey_shouldReturnTrue() {
        assertThat(store.hasExpired("key", clock.instant())).isTrue();
    }

    @Test
    void hasExpired_notExpired_shouldReturnFalse() {
        Instant now = Instant.now();

        store.put("key", "value", now.plus(10, SECONDS));

        assertThat(store.hasExpired("key", offset(clock, of(5, SECONDS)).instant())).isFalse();
    }

    @Test
    void hasExpired_expired_shouldReturnTrueAndRemoveElement() {
        Instant now = Instant.now();

        store.put("key", "value", now.plus(10, SECONDS));

        assertThat(store.hasExpired("key", offset(clock, of(11, SECONDS)).instant())).isTrue();
        assertThat(store.get("key")).isNull();
    }
}

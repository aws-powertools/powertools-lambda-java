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
import org.mockito.Mock;
import org.mockito.Mockito;

import java.time.Instant;

import static java.time.temporal.ChronoUnit.SECONDS;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.MockitoAnnotations.openMocks;

public class DataStoreTest {

    @Mock
    NowProvider nowProvider;

    DataStore store;

    @BeforeEach
    public void setup() {
        openMocks(this);
        store = new DataStore(nowProvider);
    }

    @Test
    public void put_shouldInsertInStore() {
        store.put("key", "value", Instant.now());
        assertThat(store.get("key")).isEqualTo("value");
    }

    @Test
    public void get_invalidKey_shouldReturnNull() {
        assertThat(store.get("key")).isNull();
    }

    @Test
    public void hasExpired_invalidKey_shouldReturnTrue() {
        assertThat(store.hasExpired("key")).isTrue();
    }

    @Test
    public void hasExpired_notExpired_shouldReturnFalse() {
        Instant now = Instant.now();

        Mockito.when(nowProvider.now()).thenReturn(now.plus(5, SECONDS));

        store.put("key", "value", now.plus(10, SECONDS));

        assertThat(store.hasExpired("key")).isFalse();
    }

    @Test
    public void hasExpired_expired_shouldReturnTrueAndRemoveElement() {
        Instant now = Instant.now();

        Mockito.when(nowProvider.now()).thenReturn(now.plus(11, SECONDS));

        store.put("key", "value", now.plus(10, SECONDS));

        assertThat(store.hasExpired("key")).isTrue();
        assertThat(store.get("key")).isNull();
    }
}

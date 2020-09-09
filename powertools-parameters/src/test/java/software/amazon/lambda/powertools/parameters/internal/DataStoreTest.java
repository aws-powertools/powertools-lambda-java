package software.amazon.lambda.powertools.parameters.internal;

import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.mockito.Spy;

import java.time.Instant;

import static java.time.temporal.ChronoUnit.SECONDS;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.MockitoAnnotations.openMocks;

public class DataStoreTest {
    @Spy
    DataStore store;

    @BeforeEach
    public void setup() {
        openMocks(this);
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

        Mockito.when(store.now()).thenReturn(now.plus(5, SECONDS));

        store.put("key", "value", now.plus(10, SECONDS));

        assertThat(store.hasExpired("key")).isFalse();
    }

    @Test
    public void hasExpired_expired_shouldReturnTrueAndRemoveElement() {
        Instant now = Instant.now();

        Mockito.when(store.now()).thenReturn(now.plus(11, SECONDS));

        store.put("key", "value", now.plus(10, SECONDS));

        assertThat(store.hasExpired("key")).isTrue();
        assertThat(store.get("key")).isNull();
    }
}

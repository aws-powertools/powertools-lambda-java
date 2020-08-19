package software.amazon.lambda.logging;

import org.apache.logging.log4j.ThreadContext;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;


class PowerLoggerTest {

    @BeforeEach
    void setUp() {
        ThreadContext.clearAll();
    }

    @Test
    void shouldSetCustomKeyOnThreadContext() {
        PowerLogger.appendKey("test", "value");

        assertThat(ThreadContext.getImmutableContext())
                .hasSize(1)
                .containsEntry("test", "value");
    }
}
package software.amazon.lambda.powertools.logging;

import org.apache.logging.log4j.ThreadContext;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import software.amazon.lambda.powertools.logging.PowerLogger;

import static org.assertj.core.api.Assertions.assertThat;


class PowerLoggerTest {

    @BeforeEach
    void setUp() {
        ThreadContext.clearAll();
    }

    @Test
    void shouldSetCustomKeyOnThreadContext() {
        PowerLogger.customKey("test", "value");

        assertThat(ThreadContext.getImmutableContext())
                .hasSize(1)
                .containsEntry("test", "value");
    }
}
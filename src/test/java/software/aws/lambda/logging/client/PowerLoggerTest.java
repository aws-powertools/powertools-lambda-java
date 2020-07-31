package software.aws.lambda.logging.client;

import org.apache.logging.log4j.ThreadContext;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;


class PowerLoggerTest {

    @Test
    void shouldSetCustomKeyOnThreadContext() {
        PowerLogger.customKey("test", "value");

        assertThat(ThreadContext.getImmutableContext())
                .hasSize(1)
                .containsEntry("test", "value");
    }
}
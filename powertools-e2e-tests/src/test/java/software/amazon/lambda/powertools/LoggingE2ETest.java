package software.amazon.lambda.powertools;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;
import software.amazon.lambda.powertools.testutils.Infrastructure;
import software.amazon.lambda.powertools.testutils.InvocationResult;

import java.util.HashMap;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static software.amazon.lambda.powertools.testutils.InvocationLogs.Level.INFO;

public class LoggingE2ETest {

    private static final ObjectMapper objectMapper = new ObjectMapper();
    private static Infrastructure infrastructure;

    @BeforeAll
    @Timeout(value = 5, unit = TimeUnit.MINUTES)
    public static void setup() {
        infrastructure = Infrastructure.builder()
                .testName(LoggingE2ETest.class.getSimpleName())
                .pathToFunction("logging")
                .environmentVariables(new HashMap<>() {{
                      put("POWERTOOLS_LOG_LEVEL", "INFO");
                      put("POWERTOOLS_SERVICE_NAME", LoggingE2ETest.class.getSimpleName());
                  }}
                )
                .build();
        infrastructure.deploy();
    }

    @AfterAll
    public static void tearDown() {
        if (infrastructure != null)
            infrastructure.destroy();
    }

    @Test
    public void test_logInfoWithAdditionalKeys() throws JsonProcessingException {
        // GIVEN
        String orderId = UUID.randomUUID().toString();
        String event = "{\"message\":\"New Order\", \"keys\":{\"orderId\":\"" + orderId +"\"}}";

        // WHEN
        InvocationResult invocationResult1 = infrastructure.invokeFunction(event);
        InvocationResult invocationResult2 = infrastructure.invokeFunction(event);

        // THEN
        String[] functionLogs = invocationResult1.getLogs().getFunctionLogs(INFO);
        assertThat(functionLogs).hasSize(1);

        JsonNode jsonNode = objectMapper.readTree(functionLogs[0]);
        assertThat(jsonNode.get("message").asText()).isEqualTo("New Order");
        assertThat(jsonNode.get("orderId").asText()).isEqualTo(orderId);
        assertThat(jsonNode.get("coldStart").asBoolean()).isTrue();
        assertThat(jsonNode.get("function_request_id").asText()).isEqualTo(invocationResult1.getRequestId());

        // second call should not be cold start
        functionLogs = invocationResult2.getLogs().getFunctionLogs(INFO);
        assertThat(functionLogs).hasSize(1);
        jsonNode = objectMapper.readTree(functionLogs[0]);
        assertThat(jsonNode.get("coldStart").asBoolean()).isFalse();
    }
}

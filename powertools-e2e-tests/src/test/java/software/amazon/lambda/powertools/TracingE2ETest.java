package software.amazon.lambda.powertools;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;
import software.amazon.lambda.powertools.testutils.Infrastructure;
import software.amazon.lambda.powertools.testutils.InvocationResult;
import software.amazon.lambda.powertools.testutils.tracing.SegmentDocument.SubSegment;
import software.amazon.lambda.powertools.testutils.tracing.Trace;
import software.amazon.lambda.powertools.testutils.tracing.TraceFetcher;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;

public class TracingE2ETest {
    private static Infrastructure infrastructure;
    private static final String service = "TracingE2EService_"+UUID.randomUUID(); // "TracingE2EService_e479fb27-422b-4107-9f8c-086c62e1cd12";

    @BeforeAll
    @Timeout(value = 5, unit = TimeUnit.MINUTES)
    public static void setup() {
        infrastructure = Infrastructure.builder()
                .testName(TracingE2ETest.class.getSimpleName())
                .pathToFunction("tracing")
                .tracing(true)
                .environmentVariables(new HashMap<>() {{
                      put("POWERTOOLS_SERVICE_NAME", service);
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
    public void test_tracing() {
        // GIVEN
        String message = "Hello World";
        String event = String.format("{\"message\":\"%s\"}", message);
        String result = String.format("%s (%s)", message, infrastructure.getFunctionName());

        // WHEN
        InvocationResult invocationResult = infrastructure.invokeFunction(event);

        // THEN
        Trace trace = TraceFetcher.builder()
                .start(invocationResult.getStart())
                .end(invocationResult.getEnd())
//                .start(Instant.ofEpochSecond(1667468280))
//                .end(Instant.ofEpochSecond(1667468340))
                .functionName(infrastructure.getFunctionName())
//                .functionName("TracingE2ETest-744e0e5ba909-function")
                .build()
                .fetchTrace();

        assertThat(trace.getSubsegments()).hasSize(1);
        SubSegment handleRequest = trace.getSubsegments().get(0);
        assertThat(handleRequest.getName()).isEqualTo("## handleRequest");
        assertThat(handleRequest.getAnnotations()).hasSize(2);
        assertThat(handleRequest.getAnnotations().get("ColdStart")).isEqualTo(true);
        assertThat(handleRequest.getAnnotations().get("Service")).isEqualTo(service);
        assertThat(handleRequest.getMetadata()).hasSize(1);
        Map<String, Object> metadata = (Map<String, Object>) handleRequest.getMetadata().get(service);
        assertThat(metadata.get("handleRequest response")).isEqualTo(result);
        assertThat(handleRequest.getSubsegments()).hasSize(2);

        SubSegment sub = handleRequest.getSubsegments().get(0);
        assertThat(sub.getName()).isIn("## internal_stuff", "## buildMessage");

        sub = handleRequest.getSubsegments().get(1);
        assertThat(sub.getName()).isIn("## internal_stuff", "## buildMessage");

        SubSegment buildMessage = handleRequest.getSubsegments().stream().filter(subSegment -> subSegment.getName().equals("## buildMessage")).findFirst().orElse(null);
        assertThat(buildMessage).isNotNull();
        assertThat(buildMessage.getAnnotations()).hasSize(1);
        assertThat(buildMessage.getAnnotations().get("message")).isEqualTo(message);
        assertThat(buildMessage.getMetadata()).hasSize(1);
        metadata = (Map<String, Object>) buildMessage.getMetadata().get(service);
        assertThat(metadata.get("buildMessage response")).isEqualTo(result);
    }
}

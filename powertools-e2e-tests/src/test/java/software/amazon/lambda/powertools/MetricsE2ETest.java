package software.amazon.lambda.powertools;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;
import software.amazon.lambda.powertools.testutils.Infrastructure;
import software.amazon.lambda.powertools.testutils.InvocationResult;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;

public class MetricsE2ETest {
    private static Infrastructure infrastructure;

    private static final String namespace = "MetricsE2ENamespace_"+UUID.randomUUID();
    private static final String service = "MetricsE2EService_"+UUID.randomUUID();

    @BeforeAll
    @Timeout(value = 5, unit = TimeUnit.MINUTES)
    public static void setup() {
        infrastructure = Infrastructure.builder()
                .testName(MetricsE2ETest.class.getSimpleName())
                .pathToFunction("metrics")
                .environmentVariables(new HashMap<>() {{
                      put("POWERTOOLS_METRICS_NAMESPACE", namespace);
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
    public void test_recordMetrics()  {
        // GIVEN
        String event1 = "{ \"metrics\": {\"orders\": 1, \"products\": 4}, \"dimensions\": { \"Environment\": \"test\"} }";

        // WHEN
        InvocationResult invocationResult = infrastructure.invokeFunction(event1);

        // THEN
        List<Double> coldStart = infrastructure.getMetrics(invocationResult.getStart(), invocationResult.getEnd(), 60, namespace, "ColdStart", new HashMap<>() {{ put("FunctionName", infrastructure.getFunctionName()); put("Service", service); }});
        assertThat(coldStart.get(0)).isEqualTo(1);
        List<Double> orderMetrics = infrastructure.getMetrics(invocationResult.getStart(), invocationResult.getEnd(), 60, namespace, "orders", Collections.singletonMap("Environment", "test"));
        assertThat(orderMetrics.get(0)).isEqualTo(1);
        List<Double> productMetrics = infrastructure.getMetrics(invocationResult.getStart(), invocationResult.getEnd(), 60, namespace, "products", Collections.singletonMap("Environment", "test"));
        assertThat(productMetrics.get(0)).isEqualTo(4);
        orderMetrics = infrastructure.getMetrics(invocationResult.getStart(), invocationResult.getEnd(), 60, namespace, "orders", Collections.singletonMap("Service", service));
        assertThat(orderMetrics.get(0)).isEqualTo(1);
        productMetrics = infrastructure.getMetrics(invocationResult.getStart(), invocationResult.getEnd(), 60, namespace, "products", Collections.singletonMap("Service", service));
        assertThat(productMetrics.get(0)).isEqualTo(4);
    }
}

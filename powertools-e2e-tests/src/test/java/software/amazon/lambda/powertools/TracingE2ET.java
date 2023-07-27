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

package software.amazon.lambda.powertools;

import static org.assertj.core.api.Assertions.assertThat;
import static software.amazon.lambda.powertools.testutils.lambda.LambdaInvoker.invokeFunction;

import java.util.Collections;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;
import software.amazon.lambda.powertools.testutils.Infrastructure;
import software.amazon.lambda.powertools.testutils.lambda.InvocationResult;
import software.amazon.lambda.powertools.testutils.tracing.SegmentDocument.SubSegment;
import software.amazon.lambda.powertools.testutils.tracing.Trace;
import software.amazon.lambda.powertools.testutils.tracing.TraceFetcher;

public class TracingE2ET {
    private static final String service = "TracingE2EService_" + UUID.randomUUID();
            // "TracingE2EService_e479fb27-422b-4107-9f8c-086c62e1cd12";

    private static Infrastructure infrastructure;
    private static String functionName;

    @BeforeAll
    @Timeout(value = 5, unit = TimeUnit.MINUTES)
    public static void setup() {
        infrastructure = Infrastructure.builder()
                .testName(TracingE2ET.class.getSimpleName())
                .pathToFunction("tracing")
                .tracing(true)
                .environmentVariables(Collections.singletonMap("POWERTOOLS_SERVICE_NAME", service))
                .build();
        functionName = infrastructure.deploy();
    }

    @AfterAll
    public static void tearDown() {
        if (infrastructure != null) {
            infrastructure.destroy();
        }
    }

    @Test
    public void test_tracing() {
        // GIVEN
        String message = "Hello World";
        String event = String.format("{\"message\":\"%s\"}", message);
        String result = String.format("%s (%s)", message, functionName);

        // WHEN
        InvocationResult invocationResult = invokeFunction(functionName, event);

        // THEN
        Trace trace = TraceFetcher.builder()
                .start(invocationResult.getStart())
                .end(invocationResult.getEnd())
                .functionName(functionName)
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

        SubSegment buildMessage = handleRequest.getSubsegments().stream()
                .filter(subSegment -> subSegment.getName().equals("## buildMessage")).findFirst().orElse(null);
        assertThat(buildMessage).isNotNull();
        assertThat(buildMessage.getAnnotations()).hasSize(1);
        assertThat(buildMessage.getAnnotations().get("message")).isEqualTo(message);
        assertThat(buildMessage.getMetadata()).hasSize(1);
        metadata = (Map<String, Object>) buildMessage.getMetadata().get(service);
        assertThat(metadata.get("buildMessage response")).isEqualTo(result);
    }
}

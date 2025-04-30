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
import static software.amazon.lambda.powertools.testutils.Infrastructure.FUNCTION_NAME_OUTPUT;
import static software.amazon.lambda.powertools.testutils.lambda.LambdaInvoker.invokeFunction;

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

    private static Infrastructure infrastructure;
    private static String functionName;

    @BeforeAll
    @Timeout(value = 5, unit = TimeUnit.MINUTES)
    public static void setup() {
        infrastructure = Infrastructure.builder()
                .testName(TracingE2ET.class.getSimpleName())
                .pathToFunction("tracing")
                .tracing(true)
                .environmentVariables(
                        Map.of("POWERTOOLS_SERVICE_NAME", service,
                                "POWERTOOLS_TRACER_CAPTURE_RESPONSE", "true"))
                .build();
        Map<String, String> outputs = infrastructure.deploy();
        functionName = outputs.get(FUNCTION_NAME_OUTPUT);
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
        final String message = "Hello World";
        final String event = String.format("{\"message\":\"%s\"}", message);
        final String result = String.format("%s (%s)", message, functionName);

        // WHEN
        final InvocationResult invocationResult = invokeFunction(functionName, event);

        // THEN
        final Trace trace = TraceFetcher.builder()
                .start(invocationResult.getStart())
                .end(invocationResult.getEnd())
                .functionName(functionName)
                .build()
                .fetchTrace();

        assertThat(trace.getSubsegments()).hasSize(2);

        // We need to filter segments based on name because they are not returned in-order from the X-Ray API
        // The Init segment is created by default for Lambda functions in X-Ray
        final SubSegment initSegment = trace.getSubsegments().stream()
                .filter(subSegment -> subSegment.getName().equals("Init"))
                .findFirst().orElse(null);
        assertThat(initSegment.getName()).isEqualTo("Init");
        assertThat(initSegment.getAnnotations()).isNull();

        final SubSegment handleRequestSegment = trace.getSubsegments().stream()
                .filter(subSegment -> subSegment.getName().equals("## handleRequest"))
                .findFirst().orElse(null);
        assertThat(handleRequestSegment.getName()).isEqualTo("## handleRequest");
        assertThat(handleRequestSegment.getAnnotations()).hasSize(2);
        assertThat(handleRequestSegment.getAnnotations()).containsEntry("ColdStart", true);
        assertThat(handleRequestSegment.getAnnotations()).containsEntry("Service", service);
        assertThat(handleRequestSegment.getMetadata()).hasSize(1);
        final Map<String, Object> metadata = (Map<String, Object>) handleRequestSegment.getMetadata().get(service);
        assertThat(metadata).containsEntry("handleRequest response", result);
        assertThat(handleRequestSegment.getSubsegments()).hasSize(2);

        SubSegment sub = handleRequestSegment.getSubsegments().get(0);
        assertThat(sub.getName()).isIn("## internal_stuff", "## buildMessage");

        sub = handleRequestSegment.getSubsegments().get(1);
        assertThat(sub.getName()).isIn("## internal_stuff", "## buildMessage");

        SubSegment buildMessage = handleRequestSegment.getSubsegments().stream()
                .filter(subSegment -> subSegment.getName().equals("## buildMessage"))
                .findFirst().orElse(null);
        assertThat(buildMessage).isNotNull();
        assertThat(buildMessage.getAnnotations()).hasSize(1);
        assertThat(buildMessage.getAnnotations()).containsEntry("message", message);
        assertThat(buildMessage.getMetadata()).hasSize(1);
        final Map<String, Object> buildMessageSegmentMetadata = (Map<String, Object>) buildMessage.getMetadata()
                .get(service);
        assertThat(buildMessageSegmentMetadata).containsEntry("buildMessage response", result);
    }
}

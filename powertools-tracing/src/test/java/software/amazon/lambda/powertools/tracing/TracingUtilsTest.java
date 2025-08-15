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

package software.amazon.lambda.powertools.tracing;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.entry;
import static software.amazon.lambda.powertools.tracing.TracingUtils.withEntitySubsegment;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.amazonaws.xray.AWSXRay;
import com.amazonaws.xray.entities.Entity;

class TracingUtilsTest {
    @BeforeEach
    void setUp() {
        AWSXRay.beginSegment("test");
    }

    @AfterEach
    void tearDown() {
        if (AWSXRay.getCurrentSubsegmentOptional().isPresent()) {
            AWSXRay.endSubsegment();
        }

        AWSXRay.endSegment();
    }

    @Test
    void shouldSetAnnotationOnCurrentSubSegment() {
        AWSXRay.beginSubsegment("subSegment");

        TracingUtils.putAnnotation("stringKey", "val");
        TracingUtils.putAnnotation("numberKey", 10);
        TracingUtils.putAnnotation("booleanKey", false);

        assertThat(AWSXRay.getTraceEntity().getAnnotations())
                .hasSize(3)
                .contains(
                        entry("stringKey", "val"),
                        entry("numberKey", 10),
                        entry("booleanKey", false));
    }

    @Test
    void shouldNotSetAnnotationIfNoCurrentSubSegment() {
        TracingUtils.putAnnotation("key", "val");

        assertThat(AWSXRay.getTraceEntity().getAnnotations())
                .isEmpty();
    }

    @Test
    void shouldSetMetadataOnCurrentSubSegment() {
        AWSXRay.beginSubsegment("subSegment");

        TracingUtils.putMetadata("key", "val");

        assertThat(AWSXRay.getTraceEntity().getMetadata())
                .hasSize(1)
                .containsKey("service_undefined")
                .satisfies(map -> assertThat(map.get("service_undefined"))
                        .containsEntry("key", "val"));
    }

    @Test
    void shouldNotSetMetaDataIfNoCurrentSubSegment() {
        TracingUtils.putMetadata("key", "val");

        assertThat(AWSXRay.getTraceEntity().getAnnotations())
                .isEmpty();
    }

    @Test
    void shouldInvokeCodeBlockWrappedWithinSubsegment() {
        TracingUtils.withSubsegment("testSubSegment", subsegment -> {
            subsegment.putAnnotation("key", "val");
            subsegment.putMetadata("key", "val");
        });

        assertThat(AWSXRay.getTraceEntity().getSubsegments())
                .hasSize(1)
                .allSatisfy(subsegment -> {
                    assertThat(subsegment.getName())
                            .isEqualTo("## testSubSegment");

                    assertThat(subsegment.getNamespace())
                            .isEqualTo("service_undefined");

                    assertThat(subsegment.getAnnotations())
                            .hasSize(1)
                            .containsEntry("key", "val");

                    assertThat(subsegment.getMetadata())
                            .hasSize(1);
                });
    }

    @Test
    void shouldNotAddAnnotationIfInvalidCharacterInKey() {
        AWSXRay.beginSubsegment("subSegment");
        String inputKey = "stringKey with spaces";
        TracingUtils.putAnnotation(inputKey, "val");
        AWSXRay.getCurrentSubsegmentOptional()
                .ifPresent(segment -> assertThat(segment.getAnnotations()).size().isEqualTo(0));
    }

    @Test
    void shouldAddAnnotationIfValidCharactersInKey() {
        AWSXRay.beginSubsegment("subSegment");
        String inputKey = "validKey";
        TracingUtils.putAnnotation(inputKey, "val");
        AWSXRay.getCurrentSubsegmentOptional()
                .ifPresent(segment -> assertThat(segment.getAnnotations()).size().isEqualTo(1));
    }

    @Test
    void shouldInvokeCodeBlockWrappedWithinNamespacedSubsegment() {
        TracingUtils.withSubsegment("testNamespace", "testSubSegment", subsegment -> {
            subsegment.putAnnotation("key", "val");
            subsegment.putMetadata("key", "val");
        });

        assertThat(AWSXRay.getTraceEntity().getSubsegments())
                .hasSize(1)
                .allSatisfy(subsegment -> {
                    assertThat(subsegment.getName())
                            .isEqualTo("## testSubSegment");

                    assertThat(subsegment.getNamespace())
                            .isEqualTo("testNamespace");

                    assertThat(subsegment.getAnnotations())
                            .hasSize(1)
                            .containsEntry("key", "val");

                    assertThat(subsegment.getMetadata())
                            .hasSize(1);
                });
    }

    @Test
    void shouldInvokeCodeBlockWrappedWithinEntitySubsegment() throws InterruptedException {
        Entity traceEntity = AWSXRay.getTraceEntity();

        Thread thread = new Thread(() -> withEntitySubsegment("testSubSegment", traceEntity, subsegment -> {
            subsegment.putAnnotation("key", "val");
        }));

        thread.start();
        thread.join();

        assertThat(AWSXRay.getTraceEntity().getSubsegments())
                .hasSize(1)
                .allSatisfy(subsegment -> {
                    assertThat(subsegment.getName())
                            .isEqualTo("## testSubSegment");

                    assertThat(subsegment.getNamespace())
                            .isEqualTo("service_undefined");

                    assertThat(subsegment.getAnnotations())
                            .hasSize(1)
                            .containsEntry("key", "val");
                });
    }

    @Test
    void shouldInvokeCodeBlockWrappedWithinNamespacedEntitySubsegment() throws InterruptedException {
        Entity traceEntity = AWSXRay.getTraceEntity();

        Thread thread = new Thread(
                () -> withEntitySubsegment("testNamespace", "testSubSegment", traceEntity, subsegment -> {
                    subsegment.putAnnotation("key", "val");
                }));

        thread.start();
        thread.join();

        assertThat(AWSXRay.getTraceEntity().getSubsegments())
                .hasSize(1)
                .allSatisfy(subsegment -> {
                    assertThat(subsegment.getName())
                            .isEqualTo("## testSubSegment");

                    assertThat(subsegment.getNamespace())
                            .isEqualTo("testNamespace");

                    assertThat(subsegment.getAnnotations())
                            .hasSize(1)
                            .containsEntry("key", "val");
                });
    }
}

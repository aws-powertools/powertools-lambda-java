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
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static software.amazon.lambda.powertools.tracing.TracingUtils.withEntitySubsegment;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.xray.AWSXRay;
import com.amazonaws.xray.entities.Entity;
import java.util.List;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.slf4j.LoggerFactory;

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
                        entry("booleanKey", false)
                );
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
                .satisfies(map ->
                        assertThat(map.get("service_undefined"))
                                .containsEntry("key", "val")
                );
    }

    @Test
    void shouldNotSetMetaDataIfNoCurrentSubSegment() {
        TracingUtils.putMetadata("key", "val");

        assertThat(AWSXRay.getTraceEntity().getAnnotations())
                .isEmpty();
    }

    @Test
    void shouldInvokeCodeBlockWrappedWithinSubsegment() {
        Context test = mock(Context.class);

        TracingUtils.withSubsegment("testSubSegment", subsegment ->
        {
            subsegment.putAnnotation("key", "val");
            subsegment.putMetadata("key", "val");
            test.getFunctionName();
        });

        verify(test).getFunctionName();

        assertThat(AWSXRay.getTraceEntity().getSubsegments())
                .hasSize(1)
                .allSatisfy(subsegment ->
                {
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
    void shouldEmitNoLogWarnIfValidCharacterInKey() {
        AWSXRay.beginSubsegment("subSegment");
        Logger logger = (Logger) LoggerFactory.getLogger(TracingUtils.class);

        // create and start a ListAppender
        ListAppender<ILoggingEvent> listAppender = new ListAppender<>();
        listAppender.start();

        // add the appender to the logger
        logger.addAppender(listAppender);

        TracingUtils.putAnnotation("stringKey", "val");

        List<ILoggingEvent> logsList = listAppender.list;
        assertThat(AWSXRay.getTraceEntity().getAnnotations())
                .hasSize(1)
                .contains(
                        entry("stringKey", "val")
                );
        assertThat(logsList.size()).isZero();
    }

    @Test
    void shouldEmitLogWarnIfInvalidCharacterInKey() {
        AWSXRay.beginSubsegment("subSegment");
        Logger logger = (Logger) LoggerFactory.getLogger(TracingUtils.class);

        // create and start a ListAppender
        ListAppender<ILoggingEvent> listAppender = new ListAppender<>();
        listAppender.start();

        // add the appender to the logger
        logger.addAppender(listAppender);
        String inputKey = "stringKey with spaces";
        TracingUtils.putAnnotation(inputKey, "val");

        List<ILoggingEvent> logsList = listAppender.list;
        assertThat(logsList.get(0).getLevel()).isEqualTo(Level.WARN);
        assertThat(logsList.get(0).getMessage()).isEqualTo("ignoring annotation with unsupported characters in key: {}",inputKey);
    }

    @Test
    void shouldInvokeCodeBlockWrappedWithinNamespacedSubsegment() {
        Context test = mock(Context.class);

        TracingUtils.withSubsegment("testNamespace", "testSubSegment", subsegment ->
        {
            subsegment.putAnnotation("key", "val");
            subsegment.putMetadata("key", "val");
            test.getFunctionName();
        });

        verify(test).getFunctionName();

        assertThat(AWSXRay.getTraceEntity().getSubsegments())
                .hasSize(1)
                .allSatisfy(subsegment ->
                {
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
        Context test = mock(Context.class);

        Entity traceEntity = AWSXRay.getTraceEntity();

        Thread thread = new Thread(() -> withEntitySubsegment("testSubSegment", traceEntity, subsegment ->
        {
            subsegment.putAnnotation("key", "val");
            test.getFunctionName();
        }));

        thread.start();
        thread.join();

        verify(test).getFunctionName();

        assertThat(AWSXRay.getTraceEntity().getSubsegments())
                .hasSize(1)
                .allSatisfy(subsegment ->
                {
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
        Context test = mock(Context.class);

        Entity traceEntity = AWSXRay.getTraceEntity();

        Thread thread =
                new Thread(() -> withEntitySubsegment("testNamespace", "testSubSegment", traceEntity, subsegment ->
                {
                    subsegment.putAnnotation("key", "val");
                    test.getFunctionName();
                }));

        thread.start();
        thread.join();

        verify(test).getFunctionName();

        assertThat(AWSXRay.getTraceEntity().getSubsegments())
                .hasSize(1)
                .allSatisfy(subsegment ->
                {
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
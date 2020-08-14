package software.amazon.lambda.tracing;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.xray.AWSXRay;
import com.amazonaws.xray.entities.Entity;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static software.amazon.lambda.tracing.PowerTracer.withEntitySubsegment;

class PowerTracerTest {

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

        PowerTracer.putAnnotation("key", "val");

        assertThat(AWSXRay.getTraceEntity().getAnnotations())
                .hasSize(1)
                .containsEntry("key", "val");
    }

    @Test
    void shouldNotSetAnnotationIfNoCurrentSubSegment() {
        PowerTracer.putAnnotation("key", "val");

        assertThat(AWSXRay.getTraceEntity().getAnnotations())
                .isEmpty();
    }

    @Test
    void shouldSetMetadataOnCurrentSubSegment() {
        AWSXRay.beginSubsegment("subSegment");

        PowerTracer.putMetadata("key", "val");

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
        PowerTracer.putMetadata("key", "val");

        assertThat(AWSXRay.getTraceEntity().getAnnotations())
                .isEmpty();
    }

    @Test
    void shouldInvokeCodeBlockWrappedWithinSubsegment() {
        Context test = mock(Context.class);

        PowerTracer.withSubsegment("testSubSegment", subsegment -> {
            subsegment.putAnnotation("key", "val");
            subsegment.putMetadata("key", "val");
            test.getFunctionName();
        });

        verify(test).getFunctionName();

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
    void shouldInvokeCodeBlockWrappedWithinNamespacedSubsegment() {
        Context test = mock(Context.class);

        PowerTracer.withSubsegment("testNamespace", "testSubSegment", subsegment -> {
            subsegment.putAnnotation("key", "val");
            subsegment.putMetadata("key", "val");
            test.getFunctionName();
        });

        verify(test).getFunctionName();

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
        Context test = mock(Context.class);

        Entity traceEntity = AWSXRay.getTraceEntity();

        Thread thread = new Thread(() -> withEntitySubsegment("testSubSegment", traceEntity, subsegment -> {
            subsegment.putAnnotation("key", "val");
            test.getFunctionName();
        }));

        thread.start();
        thread.join();

        verify(test).getFunctionName();

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
        Context test = mock(Context.class);

        Entity traceEntity = AWSXRay.getTraceEntity();

        Thread thread = new Thread(() -> withEntitySubsegment("testNamespace", "testSubSegment", traceEntity, subsegment -> {
            subsegment.putAnnotation("key", "val");
            test.getFunctionName();
        }));

        thread.start();
        thread.join();

        verify(test).getFunctionName();

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
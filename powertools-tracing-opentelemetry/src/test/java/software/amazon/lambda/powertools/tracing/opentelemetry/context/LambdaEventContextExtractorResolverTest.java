package software.amazon.lambda.powertools.tracing.opentelemetry.context;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyRequestEvent;
import com.amazonaws.services.lambda.runtime.events.DynamodbEvent;
import com.amazonaws.services.lambda.runtime.events.KinesisEvent;
import com.amazonaws.services.lambda.runtime.events.S3Event;
import com.amazonaws.services.lambda.runtime.events.SNSEvent;
import com.amazonaws.services.lambda.runtime.events.SQSEvent;
import io.opentelemetry.api.trace.Span;
import io.opentelemetry.api.trace.SpanContext;
import io.opentelemetry.api.trace.SpanKind;
import io.opentelemetry.context.Context;
import io.opentelemetry.context.propagation.TextMapPropagator;
import io.opentelemetry.sdk.trace.SdkTracerProvider;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class LambdaEventContextExtractorResolverTest {

    @Mock
    private LambdaEventContextExtractor mockExtractor1;

    @Mock
    private LambdaEventContextExtractor mockExtractor2;

    @Mock
    private TextMapPropagator propagator;

    private Context parentContext;
    private SdkTracerProvider tracerProvider;
    private Span testSpan;

    @BeforeEach
    void setUp() {
        tracerProvider = SdkTracerProvider.builder().build();
        testSpan = tracerProvider.get("test").spanBuilder("test-span").startSpan();
        parentContext = Context.current();
    }

    @Test
    void testCreate() {

        LambdaEventContextExtractorResolver resolver = LambdaEventContextExtractorResolver.create();

        assertThat(resolver).isNotNull();
    }

    @Test
    void testExtractWithSupportedExtractor() {

        Object event = new APIGatewayProxyRequestEvent();

        SpanContext spanContext = SpanContext.createFromRemoteParent(
                "00000000000000000000000000000001",
                "0000000000000001",
                io.opentelemetry.api.trace.TraceFlags.getSampled(),
                io.opentelemetry.api.trace.TraceState.getDefault()
        );

        ExtractedTraceContext expectedContext = new ExtractedTraceContext(
                parentContext,
                List.of(spanContext),
                SpanKind.SERVER
        );

        when(mockExtractor1.supports(event)).thenReturn(true);
        when(mockExtractor1.extract(event, parentContext, propagator)).thenReturn(expectedContext);

        LambdaEventContextExtractorResolver resolver = new LambdaEventContextExtractorResolver(
                List.of(mockExtractor1, mockExtractor2)
        );

        ExtractedTraceContext result = resolver.extract(event, parentContext, propagator);

        assertThat(result).isEqualTo(expectedContext);
        assertThat(result.context()).isEqualTo(parentContext);
        assertThat(result.spanContexts()).hasSize(1);
        assertThat(result.spanKind()).isEqualTo(SpanKind.SERVER);
        verify(mockExtractor1).supports(event);
        verify(mockExtractor1).extract(event, parentContext, propagator);
        verify(mockExtractor2, never()).supports(any());
    }

    @Test
    void testExtractWithNoSupportingExtractor() {

        Object event = new Object();

        when(mockExtractor1.supports(event)).thenReturn(false);
        when(mockExtractor2.supports(event)).thenReturn(false);

        LambdaEventContextExtractorResolver resolver = new LambdaEventContextExtractorResolver(
                List.of(mockExtractor1, mockExtractor2)
        );

        ExtractedTraceContext result = resolver.extract(event, parentContext, propagator);

        assertThat(result).isNotNull();
        assertThat(result.context()).isEqualTo(parentContext);
        assertThat(result.spanContexts()).isEmpty();
        assertThat(result.spanKind()).isEqualTo(SpanKind.SERVER);
        verify(mockExtractor1).supports(event);
        verify(mockExtractor2).supports(event);
        verify(mockExtractor1, never()).extract(any(), any(), any());
        verify(mockExtractor2, never()).extract(any(), any(), any());
    }

    @Test
    void testExtractWithMultipleExtractorsFirstMatch() {

        Object event = new SQSEvent();

        ExtractedTraceContext expectedContext = new ExtractedTraceContext(
                parentContext,
                Collections.emptyList()
        );

        when(mockExtractor1.supports(event)).thenReturn(true);
        when(mockExtractor1.extract(event, parentContext, propagator)).thenReturn(expectedContext);

        LambdaEventContextExtractorResolver resolver = new LambdaEventContextExtractorResolver(
                List.of(mockExtractor1, mockExtractor2)
        );

        ExtractedTraceContext result = resolver.extract(event, parentContext, propagator);

        assertThat(result).isEqualTo(expectedContext);
        verify(mockExtractor1).supports(event);
        verify(mockExtractor1).extract(event, parentContext, propagator);
        verify(mockExtractor2, never()).supports(any());
        verify(mockExtractor2, never()).extract(any(), any(), any());
    }

    @Test
    void testEnrichSpanWithSupportedExtractor() {

        Object event = new SNSEvent();

        when(mockExtractor1.supports(event)).thenReturn(true);

        LambdaEventContextExtractorResolver resolver = new LambdaEventContextExtractorResolver(
                List.of(mockExtractor1, mockExtractor2)
        );

        resolver.enrichSpan(event, testSpan);

        verify(mockExtractor1).supports(event);
        verify(mockExtractor1).enrichSpan(event, testSpan);
        verify(mockExtractor2, never()).supports(any());
        verify(mockExtractor2, never()).enrichSpan(any(), any());
    }

    @Test
    void testEnrichSpanWithNoSupportingExtractor() {

        Object event = new Object();

        when(mockExtractor1.supports(event)).thenReturn(false);
        when(mockExtractor2.supports(event)).thenReturn(false);

        LambdaEventContextExtractorResolver resolver = new LambdaEventContextExtractorResolver(
                List.of(mockExtractor1, mockExtractor2)
        );

        resolver.enrichSpan(event, testSpan);

        verify(mockExtractor1).supports(event);
        verify(mockExtractor2).supports(event);
        verify(mockExtractor1, never()).enrichSpan(any(), any());
        verify(mockExtractor2, never()).enrichSpan(any(), any());
    }

    @Test
    void testEnrichSpanWithMultipleExtractorsFirstMatch() {

        Object event = new KinesisEvent();

        when(mockExtractor1.supports(event)).thenReturn(true);

        LambdaEventContextExtractorResolver resolver = new LambdaEventContextExtractorResolver(
                List.of(mockExtractor1, mockExtractor2)
        );

        resolver.enrichSpan(event, testSpan);

        verify(mockExtractor1).supports(event);
        verify(mockExtractor1).enrichSpan(event, testSpan);
        verify(mockExtractor2, never()).supports(any());
        verify(mockExtractor2, never()).enrichSpan(any(), any());
    }

    @Test
    void testCreateWithAllDefaultExtractors() {

        LambdaEventContextExtractorResolver resolver = LambdaEventContextExtractorResolver.create();

        APIGatewayProxyRequestEvent apiGatewayEvent = new APIGatewayProxyRequestEvent();
        apiGatewayEvent.setHeaders(new HashMap<>());

        SQSEvent sqsEvent = new SQSEvent();
        sqsEvent.setRecords(new ArrayList<>());

        SNSEvent snsEvent = new SNSEvent();
        snsEvent.setRecords(new ArrayList<>());

        KinesisEvent kinesisEvent = new KinesisEvent();
        kinesisEvent.setRecords(new ArrayList<>());

        DynamodbEvent dynamoDbEvent = new DynamodbEvent();
        dynamoDbEvent.setRecords(new ArrayList<>());

        S3Event s3Event = new S3Event(new ArrayList<>());

        ExtractedTraceContext apiResult = resolver.extract(apiGatewayEvent, parentContext, propagator);
        assertThat(apiResult).isNotNull();

        ExtractedTraceContext sqsResult = resolver.extract(sqsEvent, parentContext, propagator);
        assertThat(sqsResult).isNotNull();

        ExtractedTraceContext snsResult = resolver.extract(snsEvent, parentContext, propagator);
        assertThat(snsResult).isNotNull();

        ExtractedTraceContext kinesisResult = resolver.extract(kinesisEvent, parentContext, propagator);
        assertThat(kinesisResult).isNotNull();

        ExtractedTraceContext dynamoDbResult = resolver.extract(dynamoDbEvent, parentContext, propagator);
        assertThat(dynamoDbResult).isNotNull();

        ExtractedTraceContext s3Result = resolver.extract(s3Event, parentContext, propagator);
        assertThat(s3Result).isNotNull();
    }

    @Test
    void testConstructorCreatesImmutableCopy() {

        List<LambdaEventContextExtractor> originalList = new ArrayList<>();
        originalList.add(mockExtractor1);

        LambdaEventContextExtractorResolver resolver = new LambdaEventContextExtractorResolver(originalList);

        originalList.add(mockExtractor2);

        Object event = new Object();
        when(mockExtractor1.supports(event)).thenReturn(false);

        resolver.extract(event, parentContext, propagator);

        verify(mockExtractor1).supports(event);
        verify(mockExtractor2, never()).supports(any());
    }

    @Test
    void testExtractWithEmptyExtractorList() {

        LambdaEventContextExtractorResolver resolver = new LambdaEventContextExtractorResolver(
                Collections.emptyList()
        );
        Object event = new Object();

        ExtractedTraceContext result = resolver.extract(event, parentContext, propagator);

        assertThat(result).isNotNull();
        assertThat(result.context()).isEqualTo(parentContext);
        assertThat(result.spanContexts()).isEmpty();
    }

    @Test
    void testEnrichSpanWithEmptyExtractorList() {

        LambdaEventContextExtractorResolver resolver = new LambdaEventContextExtractorResolver(
                Collections.emptyList()
        );
        Object event = new Object();

        resolver.enrichSpan(event, testSpan);
    }

    @Test
    void testExtractWithNullEvent() {

        when(mockExtractor1.supports(null)).thenReturn(false);

        LambdaEventContextExtractorResolver resolver = new LambdaEventContextExtractorResolver(
                List.of(mockExtractor1)
        );

        ExtractedTraceContext result = resolver.extract(null, parentContext, propagator);

        assertThat(result).isNotNull();
        assertThat(result.spanContexts()).isEmpty();
        verify(mockExtractor1).supports(null);
    }

    @Test
    void testEnrichSpanWithNullEvent() {

        when(mockExtractor1.supports(null)).thenReturn(false);

        LambdaEventContextExtractorResolver resolver = new LambdaEventContextExtractorResolver(
                List.of(mockExtractor1)
        );

        resolver.enrichSpan(null, testSpan);
        verify(mockExtractor1).supports(null);
    }

    @Test
    void testExtractReturnsContextWithMultipleSpanContexts() {

        Object event = new DynamodbEvent();

        SpanContext spanContext1 = SpanContext.createFromRemoteParent(
                "00000000000000000000000000000001",
                "0000000000000001",
                io.opentelemetry.api.trace.TraceFlags.getSampled(),
                io.opentelemetry.api.trace.TraceState.getDefault()
        );

        SpanContext spanContext2 = SpanContext.createFromRemoteParent(
                "00000000000000000000000000000002",
                "0000000000000002",
                io.opentelemetry.api.trace.TraceFlags.getSampled(),
                io.opentelemetry.api.trace.TraceState.getDefault()
        );

        ExtractedTraceContext expectedContext = new ExtractedTraceContext(
                parentContext,
                Arrays.asList(spanContext1, spanContext2),
                SpanKind.CONSUMER
        );

        when(mockExtractor1.supports(event)).thenReturn(true);
        when(mockExtractor1.extract(event, parentContext, propagator)).thenReturn(expectedContext);

        LambdaEventContextExtractorResolver resolver = new LambdaEventContextExtractorResolver(
                List.of(mockExtractor1)
        );


        ExtractedTraceContext result = resolver.extract(event, parentContext, propagator);


        assertThat(result.spanContexts()).hasSize(2);
        assertThat(result.spanKind()).isEqualTo(SpanKind.CONSUMER);
    }

    @Test
    void testExtractWithDifferentSpanKinds() {

        Object event = new Object();

        ExtractedTraceContext clientContext = new ExtractedTraceContext(
                parentContext,
                Collections.emptyList(),
                SpanKind.CLIENT
        );

        when(mockExtractor1.supports(event)).thenReturn(true);
        when(mockExtractor1.extract(event, parentContext, propagator)).thenReturn(clientContext);

        LambdaEventContextExtractorResolver resolver = new LambdaEventContextExtractorResolver(
                List.of(mockExtractor1)
        );

        ExtractedTraceContext result = resolver.extract(event, parentContext, propagator);

        assertThat(result.spanKind()).isEqualTo(SpanKind.CLIENT);
    }

    @Test
    void testExtractorOrderMatters() {

        Object event = new Object();

        ExtractedTraceContext context1 = new ExtractedTraceContext(
                parentContext,
                Collections.emptyList(),
                SpanKind.SERVER
        );

        ExtractedTraceContext context2 = new ExtractedTraceContext(
                parentContext,
                Collections.emptyList(),
                SpanKind.CLIENT
        );

        when(mockExtractor1.supports(event)).thenReturn(true);
        when(mockExtractor1.extract(event, parentContext, propagator)).thenReturn(context1);
        when(mockExtractor2.supports(event)).thenReturn(true);
        when(mockExtractor2.extract(event, parentContext, propagator)).thenReturn(context2);

        LambdaEventContextExtractorResolver resolver1 = new LambdaEventContextExtractorResolver(
                List.of(mockExtractor1, mockExtractor2)
        );
        ExtractedTraceContext result1 = resolver1.extract(event, parentContext, propagator);

        LambdaEventContextExtractorResolver resolver2 = new LambdaEventContextExtractorResolver(
                List.of(mockExtractor2, mockExtractor1)
        );
        ExtractedTraceContext result2 = resolver2.extract(event, parentContext, propagator);

        assertThat(result1.spanKind()).isEqualTo(SpanKind.SERVER);
        assertThat(result2.spanKind()).isEqualTo(SpanKind.CLIENT);
    }
}
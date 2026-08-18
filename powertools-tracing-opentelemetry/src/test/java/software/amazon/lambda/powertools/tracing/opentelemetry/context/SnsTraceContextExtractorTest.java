package software.amazon.lambda.powertools.tracing.opentelemetry.context;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;

import com.amazonaws.services.lambda.runtime.events.SNSEvent;
import io.opentelemetry.api.trace.Span;
import io.opentelemetry.api.trace.SpanContext;
import io.opentelemetry.api.trace.propagation.W3CTraceContextPropagator;
import io.opentelemetry.context.Context;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

class SnsTraceContextExtractorTest {
    
    @Test
    void shouldExtractTraceContextFromSnsEvent() {
        String traceId =
                "4bf92f3577b34da6a3ce929d0e0e4736";

        String spanId =
                "00f067aa0ba902b7";

        SNSEvent.MessageAttribute traceparent = new SNSEvent.MessageAttribute();

        traceparent.setType("String");
        traceparent.setValue("00-" + traceId + "-" + spanId + "-01");

        SNSEvent.SNS sns = new SNSEvent.SNS();

        sns.setMessageAttributes(Map.of("traceparent", traceparent));

        SNSEvent.SNSRecord record = new SNSEvent.SNSRecord();

        record.setSns(sns);

        SNSEvent event = new SNSEvent();
        event.setRecords(List.of(record));

        Context extracted = new SnsTraceContextExtractor().extract(
                event,
                Context.current(),
                W3CTraceContextPropagator.getInstance()
        );

        SpanContext spanContext = Span.fromContext(extracted).getSpanContext();

        assertThat(spanContext.isValid()).isTrue();
        assertThat(spanContext.isRemote()).isTrue();
        assertThat(spanContext.getTraceId()).isEqualTo(traceId);
        assertThat(spanContext.getSpanId()).isEqualTo(spanId);
    }

}
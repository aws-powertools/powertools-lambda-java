package software.amazon.lambda.powertools.tracing.opentelemetry.context;

class ApiGatewayTraceContextExtractorTest {

//    @Test
//    void shouldExtractTraceContextFromApiGatewayEvent() {
//
//        String traceId = "4bf92f3577b34da6a3ce929d0e0e4736";
//
//        String spanId = "00f067aa0ba902b7";
//
//        APIGatewayProxyRequestEvent event = new APIGatewayProxyRequestEvent()
//                .withHeaders(Map.of(
//                        "traceparent",
//                        "00-" + traceId + "-" + spanId + "-01"
//                ));
//
//        ApiGatewayTraceContextExtractor apiGatewayTraceContextExtractor = new ApiGatewayTraceContextExtractor();
//
//        Context parentContext = apiGatewayTraceContextExtractor.extract(
//                event,
//                Context.current(),
//                W3CTraceContextPropagator.getInstance()
//        );
//
//        SpanContext spanContext = Span.fromContext(parentContext).getSpanContext();
//
//        assertThat(spanContext.isValid()).isTrue();
//        assertThat(spanContext.isRemote()).isTrue();
//
//        assertThat(spanContext.getTraceId()).isEqualTo(traceId);
//
//        assertThat(spanContext.getSpanId()).isEqualTo(spanId);
//    }


}
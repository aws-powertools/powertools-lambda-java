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

package software.amazon.lambda.powertools.tracing.opentelemetry.context;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyRequestEvent;
import io.opentelemetry.api.trace.Span;
import io.opentelemetry.api.trace.SpanKind;
import io.opentelemetry.api.trace.propagation.W3CTraceContextPropagator;
import io.opentelemetry.context.Context;
import io.opentelemetry.context.propagation.TextMapPropagator;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class ApiGatewayTraceContextExtractorTest {

    @Mock
    private Span span;

    @Mock
    private TextMapPropagator propagator;

    @InjectMocks
    private ApiGatewayTraceContextExtractor extractor;

    @Test
    void shouldSupportApiGatewayProxyRequestEvent() {
        APIGatewayProxyRequestEvent event = new APIGatewayProxyRequestEvent();

        boolean result = extractor.supports(event);

        assertThat(result).isTrue();
    }

    @Test
    void shouldNotSupportNonApiGatewayEvent() {
        assertThat(extractor.supports("Some non-API Gateway event")).isFalse();
        assertThat(extractor.supports(new Object())).isFalse();
        assertThat(extractor.supports(null)).isFalse();
    }

    @Test
    void shouldExtractTraceContextWithServerSpanKind() {
        APIGatewayProxyRequestEvent event = new APIGatewayProxyRequestEvent()
                .withHeaders(Map.of("traceparent", "00-4bf92f3577b34da6a3ce929d0e0e4736-00f067aa0ba902b7-01"));
        Context parentContext = Context.current();

        ExtractedTraceContext extractedContext =
                extractor.extract(event, parentContext, W3CTraceContextPropagator.getInstance());

        assertThat(extractedContext).isNotNull();
        assertThat(extractedContext.spanContexts()).isEmpty();
        assertThat(extractedContext.spanKind()).isEqualTo(SpanKind.SERVER);
    }

    @Test
    void shouldExtractTraceContextWhenHeadersAreEmpty() {
        APIGatewayProxyRequestEvent event = new APIGatewayProxyRequestEvent()
                .withHeaders(Map.of());
        Context parentContext = Context.current();

        ExtractedTraceContext extractedContext = extractor.extract(event, parentContext, propagator);

        assertThat(extractedContext).isNotNull();
        assertThat(extractedContext.context()).isEqualTo(parentContext);
        assertThat(extractedContext.spanContexts()).isEmpty();
        assertThat(extractedContext.spanKind()).isEqualTo(SpanKind.SERVER);
    }

    @Test
    void shouldExtractTraceContextWhenHeadersAreNull() {
        APIGatewayProxyRequestEvent event = new APIGatewayProxyRequestEvent()
                .withHeaders(null);
        Context parentContext = Context.current();

        ExtractedTraceContext extractedContext = extractor.extract(event, parentContext, propagator);

        assertThat(extractedContext).isNotNull();
        assertThat(extractedContext.context()).isEqualTo(parentContext);
        assertThat(extractedContext.spanContexts()).isEmpty();
        assertThat(extractedContext.spanKind()).isEqualTo(SpanKind.SERVER);
    }

    @Test
    void shouldEnrichSpanWithHttpMethod() {
        APIGatewayProxyRequestEvent event = new APIGatewayProxyRequestEvent()
                .withHttpMethod("POST");

        extractor.enrichSpan(event, span);

        verify(span).setAttribute("http.request.method", "POST");
    }

    @Test
    void shouldEnrichSpanWithPath() {
        APIGatewayProxyRequestEvent event = new APIGatewayProxyRequestEvent()
                .withPath("/api/users");

        extractor.enrichSpan(event, span);

        verify(span).setAttribute("url.path", "/api/users");
    }

    @Test
    void shouldEnrichSpanWithQueryString() {
        APIGatewayProxyRequestEvent event = new APIGatewayProxyRequestEvent()
                .withQueryStringParameters(Map.of("name", "test", "page", "1"));

        extractor.enrichSpan(event, span);

        verify(span).setAttribute(eq("url.query"), anyString());
    }

    @Test
    void shouldEnrichSpanWithUserAgent() {
        APIGatewayProxyRequestEvent event = new APIGatewayProxyRequestEvent()
                .withHeaders(Map.of("user-agent", "Mozilla/5.0"));

        extractor.enrichSpan(event, span);

        verify(span).setAttribute("user_agent.original", "Mozilla/5.0");
    }

    @Test
    void shouldEnrichSpanWithUserAgentCaseInsensitive() {
        APIGatewayProxyRequestEvent event = new APIGatewayProxyRequestEvent()
                .withHeaders(Map.of("User-Agent", "PostmanRuntime/7.26.8"));

        extractor.enrichSpan(event, span);

        verify(span).setAttribute("user_agent.original", "PostmanRuntime/7.26.8");
    }

    @Test
    void shouldEnrichSpanWithRequestContext() {
        APIGatewayProxyRequestEvent.ProxyRequestContext requestContext =
                new APIGatewayProxyRequestEvent.ProxyRequestContext();
        requestContext.setRequestId("request-123");
        requestContext.setStage("prod");
        requestContext.setResourceId("resource-456");
        requestContext.setResourcePath("/users/{id}");

        APIGatewayProxyRequestEvent event = new APIGatewayProxyRequestEvent()
                .withRequestContext(requestContext);

        extractor.enrichSpan(event, span);

        verify(span).setAttribute("aws.request_id", "request-123");
        verify(span).setAttribute("aws.apigateway.stage", "prod");
        verify(span).setAttribute("aws.apigateway.resource_id", "resource-456");
        verify(span).setAttribute("aws.apigateway.resource_path", "/users/{id}");
    }

    @Test
    void shouldEnrichSpanWithAllAttributes() {
        APIGatewayProxyRequestEvent.ProxyRequestContext requestContext =
                new APIGatewayProxyRequestEvent.ProxyRequestContext();
        requestContext.setRequestId("request-789");
        requestContext.setStage("dev");
        requestContext.setResourceId("resource-101");
        requestContext.setResourcePath("/api/products");

        APIGatewayProxyRequestEvent event = new APIGatewayProxyRequestEvent()
                .withHttpMethod("GET")
                .withPath("/api/products")
                .withQueryStringParameters(Map.of("category", "electronics"))
                .withHeaders(Map.of("user-agent", "PostmanRuntime/7.26.8"))
                .withRequestContext(requestContext);

        extractor.enrichSpan(event, span);

        verify(span).setAttribute("http.request.method", "GET");
        verify(span).setAttribute("url.path", "/api/products");
        verify(span).setAttribute(eq("url.query"), anyString());
        verify(span).setAttribute("user_agent.original", "PostmanRuntime/7.26.8");
        verify(span).setAttribute("aws.request_id", "request-789");
        verify(span).setAttribute("aws.apigateway.stage", "dev");
        verify(span).setAttribute("aws.apigateway.resource_id", "resource-101");
        verify(span).setAttribute("aws.apigateway.resource_path", "/api/products");
    }

    @Test
    void shouldNotEnrichSpanWhenHttpMethodIsNull() {
        APIGatewayProxyRequestEvent event = new APIGatewayProxyRequestEvent()
                .withHttpMethod(null);

        extractor.enrichSpan(event, span);

        verify(span, never()).setAttribute(eq("http.request.method"), anyString());
    }

    @Test
    void shouldNotEnrichSpanWhenPathIsNull() {
        APIGatewayProxyRequestEvent event = new APIGatewayProxyRequestEvent()
                .withPath(null);

        extractor.enrichSpan(event, span);

        verify(span, never()).setAttribute(eq("url.path"), anyString());
    }

    @Test
    void shouldNotEnrichSpanWhenQueryStringParametersIsNull() {
        APIGatewayProxyRequestEvent event = new APIGatewayProxyRequestEvent()
                .withQueryStringParameters(null);

        extractor.enrichSpan(event, span);

        verify(span, never()).setAttribute(eq("url.query"), anyString());
    }

    @Test
    void shouldNotEnrichSpanWithUserAgentWhenHeadersIsNull() {
        APIGatewayProxyRequestEvent event = new APIGatewayProxyRequestEvent()
                .withHeaders(null);

        extractor.enrichSpan(event, span);

        verify(span, never()).setAttribute(eq("user_agent.original"), anyString());
    }

    @Test
    void shouldNotEnrichSpanWhenRequestContextIsNull() {
        APIGatewayProxyRequestEvent event = new APIGatewayProxyRequestEvent()
                .withRequestContext(null);

        extractor.enrichSpan(event, span);

        verify(span, never()).setAttribute(eq("aws.request_id"), anyString());
        verify(span, never()).setAttribute(eq("aws.apigateway.stage"), anyString());
        verify(span, never()).setAttribute(eq("aws.apigateway.resource_id"), anyString());
        verify(span, never()).setAttribute(eq("aws.apigateway.resource_path"), anyString());
    }

    @Test
    void shouldNotEnrichSpanWithRequestIdWhenRequestIdIsNull() {
        APIGatewayProxyRequestEvent.ProxyRequestContext requestContext =
                new APIGatewayProxyRequestEvent.ProxyRequestContext();
        requestContext.setRequestId(null);

        APIGatewayProxyRequestEvent event = new APIGatewayProxyRequestEvent()
                .withRequestContext(requestContext);

        extractor.enrichSpan(event, span);

        verify(span, never()).setAttribute(eq("aws.request_id"), anyString());
    }

    @Test
    void shouldNotEnrichSpanWithStageWhenStageIsNull() {
        APIGatewayProxyRequestEvent.ProxyRequestContext requestContext =
                new APIGatewayProxyRequestEvent.ProxyRequestContext();
        requestContext.setStage(null);

        APIGatewayProxyRequestEvent event = new APIGatewayProxyRequestEvent()
                .withRequestContext(requestContext);

        extractor.enrichSpan(event, span);

        verify(span, never()).setAttribute(eq("aws.apigateway.stage"), anyString());
    }
}
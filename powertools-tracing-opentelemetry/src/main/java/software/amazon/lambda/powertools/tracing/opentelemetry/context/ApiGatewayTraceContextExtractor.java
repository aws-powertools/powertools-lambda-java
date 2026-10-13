/*
 * Copyright 2023 Amazon.com, Inc. or its affiliates.
 * Licensed under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License. You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 */

package software.amazon.lambda.powertools.tracing.opentelemetry.context;

import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyRequestEvent;
import io.opentelemetry.api.trace.Span;
import io.opentelemetry.api.trace.SpanKind;
import io.opentelemetry.context.Context;
import io.opentelemetry.context.propagation.TextMapPropagator;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import software.amazon.lambda.powertools.tracing.opentelemetry.provider.OpenTelemetryProvider;

/**
 * An implementation of {@link LambdaEventContextExtractor} that extracts and enriches tracing context
 * information from API Gateway events. This class supports distributed tracing by leveraging OpenTelemetry
 * to propagate and enrich trace data from API Gateway-provided HTTP headers and metadata.
 * <p>
 * This extractor handles events of type {@link APIGatewayProxyRequestEvent}.
 */
public final class ApiGatewayTraceContextExtractor implements LambdaEventContextExtractor {


    @Override
    public boolean supports(Object event) {
        return event instanceof APIGatewayProxyRequestEvent;
    }

    @Override
    public ExtractedTraceContext extract(Object event, Context parentContext, TextMapPropagator propagator) {

        APIGatewayProxyRequestEvent apiGatewayEvent = (APIGatewayProxyRequestEvent) event;

        Map<String, String> headers = apiGatewayEvent.getHeaders();

        if (headers == null || headers.isEmpty()) {
            return new ExtractedTraceContext(parentContext, List.of(), SpanKind.SERVER);
        }

        Context context = propagator.extract(
                parentContext,
                headers,
                OpenTelemetryProvider.textMapGetter()
        );

        return new ExtractedTraceContext(context, List.of(), SpanKind.SERVER);
    }

    @Override
    public void enrichSpan(Object event, Span span) {

        APIGatewayProxyRequestEvent apiGatewayEvent = (APIGatewayProxyRequestEvent) event;

        if (apiGatewayEvent.getHttpMethod() != null) {
            span.setAttribute("http.request.method", apiGatewayEvent.getHttpMethod());
        }

        if (apiGatewayEvent.getPath() != null) {
            span.setAttribute("url.path", apiGatewayEvent.getPath());
        }

        if (apiGatewayEvent.getQueryStringParameters() != null) {

            String queryString = apiGatewayEvent.getQueryStringParameters()
                    .entrySet()
                    .stream()
                    .map(entry -> entry.getKey() + "=" + entry.getValue())
                    .collect(Collectors.joining("&"));

            span.setAttribute("url.query", queryString);
        }

        if (apiGatewayEvent.getHeaders() != null) {

            apiGatewayEvent.getHeaders()
                    .entrySet()
                    .stream()
                    .filter(entry -> "user-agent".equalsIgnoreCase(entry.getKey()))
                    .map(Map.Entry::getValue)
                    .findFirst()
                    .ifPresent(userAgent -> span.setAttribute("user_agent.original", userAgent));
        }

        if (apiGatewayEvent.getRequestContext() != null) {

            APIGatewayProxyRequestEvent.ProxyRequestContext requestContext =
                    apiGatewayEvent.getRequestContext();

            if (requestContext.getRequestId() != null) {
                span.setAttribute("aws.request_id", requestContext.getRequestId());
            }

            if (requestContext.getStage() != null) {
                span.setAttribute("aws.apigateway.stage", requestContext.getStage());
            }

            if (requestContext.getResourceId() != null) {
                span.setAttribute("aws.apigateway.resource_id", requestContext.getResourceId());
            }

            if (requestContext.getResourcePath() != null) {
                span.setAttribute("aws.apigateway.resource_path", requestContext.getResourcePath());
            }
        }

    }


}
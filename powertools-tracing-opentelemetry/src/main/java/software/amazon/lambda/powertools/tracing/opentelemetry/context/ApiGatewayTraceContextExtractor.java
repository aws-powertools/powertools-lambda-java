package software.amazon.lambda.powertools.tracing.opentelemetry.context;

import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyRequestEvent;
import io.opentelemetry.api.trace.Span;
import io.opentelemetry.context.Context;
import io.opentelemetry.context.propagation.TextMapPropagator;
import java.util.Map;
import java.util.stream.Collectors;
import software.amazon.lambda.powertools.tracing.opentelemetry.provider.OpenTelemetryProvider;

public final class ApiGatewayTraceContextExtractor implements LambdaEventContextExtractor {


    @Override
    public boolean supports(Object event) {
        return event instanceof APIGatewayProxyRequestEvent;
    }

    @Override
    public Context extract(Object event, Context parentContext, TextMapPropagator propagator) {

        APIGatewayProxyRequestEvent apiGatewayEvent = (APIGatewayProxyRequestEvent) event;

        Map<String, String> headers = apiGatewayEvent.getHeaders();

        if (headers == null || headers.isEmpty()) {
            return parentContext;
        }

        return propagator.extract(
                parentContext,
                headers,
                OpenTelemetryProvider.textMapGetter()
        );
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
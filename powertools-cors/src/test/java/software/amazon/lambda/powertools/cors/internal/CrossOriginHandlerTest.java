package software.amazon.lambda.powertools.cors.internal;

import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyRequestEvent;
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyResponseEvent;
import com.amazonaws.services.lambda.runtime.tests.EventLoader;
import org.junit.jupiter.api.Test;
import org.junitpioneer.jupiter.SetEnvironmentVariable;
import software.amazon.lambda.powertools.cors.CrossOrigin;

import java.lang.annotation.Annotation;
import java.util.HashMap;
import java.util.Map;

import static org.assertj.core.api.Assertions.*;
import static software.amazon.lambda.powertools.cors.Constants.*;

public class CrossOriginHandlerTest {

    CrossOrigin defaultCors = new CrossOrigin() {

        @Override
        public Class<? extends Annotation> annotationType() {
            return CrossOrigin.class;
        }

        @Override
        public String methods() {
            return DEFAULT_ACCESS_CONTROL_ALLOW_METHODS;
        }

        @Override
        public String origins() {
            return DEFAULT_ACCESS_CONTROL_ALLOW_ORIGIN;
        }

        @Override
        public String allowedHeaders() {
            return DEFAULT_ACCESS_CONTROL_ALLOW_HEADERS;
        }

        @Override
        public String exposedHeaders() {
            return DEFAULT_ACCESS_CONTROL_EXPOSE_HEADERS;
        }

        @Override
        public boolean allowCredentials() {
            return DEFAULT_ACCESS_CONTROL_ALLOW_CREDENTIALS;
        }

        @Override
        public int maxAge() {
            return DEFAULT_ACCESS_CONTROL_MAX_AGE;
        }
    };

    @Test
    public void defaultCorsConfiguration_shouldReturnDefaultHeaders() {
        CrossOriginHandler handler = new CrossOriginHandler(defaultCors);
        APIGatewayProxyRequestEvent event = EventLoader.loadApiGatewayRestEvent("apigw_event.json");
        APIGatewayProxyResponseEvent response = handler.process(event, new APIGatewayProxyResponseEvent().withBody("OK"));

        assertThat(response.getBody()).isEqualTo("OK");
        Map<String, String> headers = response.getHeaders();
        assertThat(headers).containsEntry(ACCESS_CONTROL_ALLOW_ORIGIN, "http://origin.com");
        assertThat(headers).containsEntry(VARY, VARY_ORIGIN);
        assertThat(headers).containsEntry(ACCESS_CONTROL_ALLOW_HEADERS, DEFAULT_ACCESS_CONTROL_ALLOW_HEADERS);
        assertThat(headers).containsEntry(ACCESS_CONTROL_ALLOW_METHODS, DEFAULT_ACCESS_CONTROL_ALLOW_METHODS);
        assertThat(headers).containsEntry(ACCESS_CONTROL_MAX_AGE, String.valueOf(DEFAULT_ACCESS_CONTROL_MAX_AGE));
        assertThat(headers).containsEntry(ACCESS_CONTROL_EXPOSE_HEADERS, DEFAULT_ACCESS_CONTROL_EXPOSE_HEADERS);
        assertThat(headers).containsEntry(ACCESS_CONTROL_ALLOW_CREDENTIALS, String.valueOf(DEFAULT_ACCESS_CONTROL_ALLOW_CREDENTIALS));
    }

    @Test
    public void defaultCorsConfiguration_withExistingHeaders_shouldReturnDefaultAndExistingHeaders() {
        CrossOriginHandler handler = new CrossOriginHandler(defaultCors);
        APIGatewayProxyRequestEvent event = EventLoader.loadApiGatewayRestEvent("apigw_event.json");
        Map<String, String> initialHeaders = new HashMap<>();
        initialHeaders.put("Content-Type", "application/json");
        APIGatewayProxyResponseEvent response = handler.process(event, new APIGatewayProxyResponseEvent().withBody("OK").withHeaders(initialHeaders));

        assertThat(response.getBody()).isEqualTo("OK");
        Map<String, String> headers = response.getHeaders();
        assertThat(headers).containsEntry("Content-Type", "application/json");
        assertThat(headers).containsEntry(ACCESS_CONTROL_ALLOW_ORIGIN, "http://origin.com");
        assertThat(headers).containsEntry(VARY, VARY_ORIGIN);
        assertThat(headers).containsEntry(ACCESS_CONTROL_ALLOW_HEADERS, DEFAULT_ACCESS_CONTROL_ALLOW_HEADERS);
        assertThat(headers).containsEntry(ACCESS_CONTROL_ALLOW_METHODS, DEFAULT_ACCESS_CONTROL_ALLOW_METHODS);
        assertThat(headers).containsEntry(ACCESS_CONTROL_MAX_AGE, String.valueOf(DEFAULT_ACCESS_CONTROL_MAX_AGE));
        assertThat(headers).containsEntry(ACCESS_CONTROL_EXPOSE_HEADERS, DEFAULT_ACCESS_CONTROL_EXPOSE_HEADERS);
        assertThat(headers).containsEntry(ACCESS_CONTROL_ALLOW_CREDENTIALS, String.valueOf(DEFAULT_ACCESS_CONTROL_ALLOW_CREDENTIALS));
    }

    @Test
    public void defaultCorsConfig_withDifferentOrigin_shouldNotReturnAllowOriginHeader() {
        CrossOrigin cors = new CrossOrigin() {

            @Override
            public Class<? extends Annotation> annotationType() {
                return CrossOrigin.class;
            }

            @Override
            public String methods() {
                return DEFAULT_ACCESS_CONTROL_ALLOW_METHODS;
            }

            @Override
            public String origins() {
                return "http://other_origin.com, origin.com";
            }

            @Override
            public String allowedHeaders() {
                return DEFAULT_ACCESS_CONTROL_ALLOW_HEADERS;
            }

            @Override
            public String exposedHeaders() {
                return DEFAULT_ACCESS_CONTROL_EXPOSE_HEADERS;
            }

            @Override
            public boolean allowCredentials() {
                return DEFAULT_ACCESS_CONTROL_ALLOW_CREDENTIALS;
            }

            @Override
            public int maxAge() {
                return DEFAULT_ACCESS_CONTROL_MAX_AGE;
            }
        };
        CrossOriginHandler handler = new CrossOriginHandler(cors);
        APIGatewayProxyRequestEvent event = EventLoader.loadApiGatewayRestEvent("apigw_event.json");
        APIGatewayProxyResponseEvent response = handler.process(event, new APIGatewayProxyResponseEvent().withBody("OK"));

       Map<String, String> headers = response.getHeaders();
        assertThat(headers).doesNotContainKey(ACCESS_CONTROL_ALLOW_ORIGIN);
        assertThat(headers).doesNotContainKey(VARY);
    }

    @Test
    public void defaultCorsConfig_withMalformedOrigin_shouldNotReturnAllowOriginHeader() {
        CrossOrigin cors = new CrossOrigin() {

            @Override
            public Class<? extends Annotation> annotationType() {
                return CrossOrigin.class;
            }

            @Override
            public String methods() {
                return DEFAULT_ACCESS_CONTROL_ALLOW_METHODS;
            }

            @Override
            public String origins() {
                return "http://origin.com";
            }

            @Override
            public String allowedHeaders() {
                return DEFAULT_ACCESS_CONTROL_ALLOW_HEADERS;
            }

            @Override
            public String exposedHeaders() {
                return DEFAULT_ACCESS_CONTROL_EXPOSE_HEADERS;
            }

            @Override
            public boolean allowCredentials() {
                return DEFAULT_ACCESS_CONTROL_ALLOW_CREDENTIALS;
            }

            @Override
            public int maxAge() {
                return DEFAULT_ACCESS_CONTROL_MAX_AGE;
            }
        };
        CrossOriginHandler handler = new CrossOriginHandler(cors);
        APIGatewayProxyRequestEvent event = EventLoader.loadApiGatewayRestEvent("apigw_event_malformed_origin.json");
        APIGatewayProxyResponseEvent response = handler.process(event, new APIGatewayProxyResponseEvent().withBody("OK"));

        Map<String, String> headers = response.getHeaders();
        assertThat(headers).doesNotContainKey(ACCESS_CONTROL_ALLOW_ORIGIN);
        assertThat(headers).doesNotContainKey(VARY);
    }

    @Test
    @SetEnvironmentVariable.SetEnvironmentVariables(value = {
            @SetEnvironmentVariable(key = ENV_ACCESS_CONTROL_ALLOW_HEADERS, value = "Content-Type, X-Amz-Date"),
            @SetEnvironmentVariable(key = ENV_ACCESS_CONTROL_ALLOW_ORIGIN, value = "http://origin.com"),
            @SetEnvironmentVariable(key = ENV_ACCESS_CONTROL_ALLOW_METHODS, value = "OPTIONS, POST"),
            @SetEnvironmentVariable(key = ENV_ACCESS_CONTROL_EXPOSE_HEADERS, value = "Content-Type, X-Amz-Date"),
            @SetEnvironmentVariable(key = ENV_ACCESS_CONTROL_ALLOW_CREDENTIALS, value = "true"),
            @SetEnvironmentVariable(key = ENV_ACCESS_CONTROL_MAX_AGE, value = "42"),
    })
    public void corsConfigWithEnvVars_shouldReturnCorrectHeaders() {
        CrossOriginHandler handler = new CrossOriginHandler(defaultCors);
        APIGatewayProxyRequestEvent event = EventLoader.loadApiGatewayRestEvent("apigw_event.json");
        APIGatewayProxyResponseEvent response = handler.process(event, new APIGatewayProxyResponseEvent().withBody("OK"));

        assertThat(response.getBody()).isEqualTo("OK");
        Map<String, String> headers = response.getHeaders();
        assertThat(headers).containsEntry(ACCESS_CONTROL_ALLOW_ORIGIN, "http://origin.com");
        assertThat(headers).containsEntry(VARY, VARY_ORIGIN);
        assertThat(headers).containsEntry(ACCESS_CONTROL_ALLOW_HEADERS, "Content-Type, X-Amz-Date");
        assertThat(headers).containsEntry(ACCESS_CONTROL_ALLOW_METHODS, "OPTIONS, POST");
        assertThat(headers).containsEntry(ACCESS_CONTROL_MAX_AGE, "42");
        assertThat(headers).containsEntry(ACCESS_CONTROL_EXPOSE_HEADERS, "Content-Type, X-Amz-Date");
        assertThat(headers).containsEntry(ACCESS_CONTROL_ALLOW_CREDENTIALS, "true");
    }

    @Test
    @SetEnvironmentVariable.SetEnvironmentVariables(value = {
            @SetEnvironmentVariable(key = ENV_ACCESS_CONTROL_ALLOW_ORIGIN, value = "http://*")
    })
    public void corsWithWildcardOrigin_shouldReturnCorrectOrigin() {
        CrossOriginHandler handler = new CrossOriginHandler(defaultCors);
        APIGatewayProxyRequestEvent event = EventLoader.loadApiGatewayRestEvent("apigw_event.json");
        APIGatewayProxyResponseEvent response = handler.process(event, new APIGatewayProxyResponseEvent().withBody("OK"));

        assertThat(response.getBody()).isEqualTo("OK");
        Map<String, String> headers = response.getHeaders();
        assertThat(headers).containsEntry(ACCESS_CONTROL_ALLOW_ORIGIN, "http://origin.com");
        assertThat(headers).containsEntry(VARY, VARY_ORIGIN);
    }
}

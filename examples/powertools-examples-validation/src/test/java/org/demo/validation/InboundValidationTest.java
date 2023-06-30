package org.demo.validation;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyRequestEvent;
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyResponseEvent;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import software.amazon.lambda.powertools.validation.ValidationException;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

public class InboundValidationTest {

    @Mock
    private Context context;
    private InboundValidation inboundValidation;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        inboundValidation = new InboundValidation();
    }

    @Test
    public void shouldReturnOkStatusWhenInputIsValid() {
        String body = "{\n" +
                "      \"id\": 43242,\n" +
                "      \"name\": \"FooBar XY\",\n" +
                "      \"price\": 258\n" +
                "    }";
        APIGatewayProxyRequestEvent request = new APIGatewayProxyRequestEvent().withBody(body);

        APIGatewayProxyResponseEvent response = inboundValidation.handleRequest(request, context);

        assertEquals(200, response.getStatusCode());
    }

    @Test
    public void shouldThrowExceptionWhenRequestInInvalid() {
        String bodyWithMissedId = "{\n" +
                "      \"name\": \"FooBar XY\",\n" +
                "      \"price\": 258\n" +
                "    }";
        APIGatewayProxyRequestEvent request = new APIGatewayProxyRequestEvent().withBody(bodyWithMissedId);

        assertThrows(ValidationException.class, () -> inboundValidation.handleRequest(request, context));
    }
}
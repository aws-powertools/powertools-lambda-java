package org.demo.serialization;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyRequestEvent;
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyResponseEvent;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import static org.junit.jupiter.api.Assertions.assertEquals;

class APIGatewayRequestDeserializationFunctionTest {

    @Mock
    private Context context;
    private APIGatewayRequestDeserializationFunction deserializationFunction;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        deserializationFunction = new APIGatewayRequestDeserializationFunction();
    }

    @Test
    public void shouldReturnOkStatusWithProductId() {
        String body = "{\"id\":1234, \"name\":\"product\", \"price\":42}";
        APIGatewayProxyRequestEvent request = new APIGatewayProxyRequestEvent().withBody(body);

        APIGatewayProxyResponseEvent response = deserializationFunction.handleRequest(request, context);

        assertEquals(200, response.getStatusCode());
        assertEquals("Received request for productId: 1234", response.getBody());
    }
}
package software.amazon.lambda.powertools.idempotency;


import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyResponseEvent;
import com.amazonaws.services.lambda.runtime.tests.EventLoader;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import software.amazon.awssdk.services.dynamodb.model.ScanRequest;
import software.amazon.lambda.powertools.idempotency.handlers.IdempotencyFunction;

import static org.assertj.core.api.Assertions.assertThat;

public class IdempotencyTest extends DynamoDBConfig {

    @Mock
    private Context context;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
    }

    @Test
    public void endToEndTest() {
        IdempotencyFunction function = new IdempotencyFunction(client);

        APIGatewayProxyResponseEvent response = function.handleRequest(EventLoader.loadApiGatewayRestEvent("apigw_event2.json"), context);
        assertThat(function.handlerExecuted).isTrue();

        function.handlerExecuted = false;

        APIGatewayProxyResponseEvent response2 = function.handleRequest(EventLoader.loadApiGatewayRestEvent("apigw_event2.json"), context);
        assertThat(function.handlerExecuted).isFalse();

        assertThat(response).isEqualTo(response2);
        assertThat(response2.getBody()).contains("hello world");

        assertThat(client.scan(ScanRequest.builder().tableName(TABLE_NAME).build()).count()).isEqualTo(1);
    }
}

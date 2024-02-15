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

package software.amazon.lambda.powertools.idempotency.dynamodb;


import static org.assertj.core.api.Assertions.assertThat;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyResponseEvent;
import com.amazonaws.services.lambda.runtime.tests.EventLoader;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import software.amazon.awssdk.services.dynamodb.model.ScanRequest;
import software.amazon.lambda.powertools.idempotency.dynamodb.handlers.IdempotencyFunction;

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

        APIGatewayProxyResponseEvent response =
                function.handleRequest(EventLoader.loadApiGatewayRestEvent("apigw_event2.json"), context);
        assertThat(function.handlerExecuted).isTrue();

        function.handlerExecuted = false;

        APIGatewayProxyResponseEvent response2 =
                function.handleRequest(EventLoader.loadApiGatewayRestEvent("apigw_event2.json"), context);
        assertThat(function.handlerExecuted).isFalse();

        assertThat(response).isEqualTo(response2);
        assertThat(response2.getBody()).contains("hello world");

        assertThat(client.scan(ScanRequest.builder().tableName(TABLE_NAME).build()).count()).isEqualTo(1);
    }
}

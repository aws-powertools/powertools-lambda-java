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

package helloworld;

import com.amazonaws.services.dynamodbv2.local.main.ServerRunner;
import com.amazonaws.services.dynamodbv2.local.server.DynamoDBProxyServer;
import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyResponseEvent;
import com.amazonaws.services.lambda.runtime.tests.EventLoader;
import java.io.IOException;
import java.net.ServerSocket;
import java.net.URI;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.http.urlconnection.UrlConnectionHttpClient;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.dynamodb.DynamoDbClient;
import software.amazon.awssdk.services.dynamodb.model.AttributeDefinition;
import software.amazon.awssdk.services.dynamodb.model.BillingMode;
import software.amazon.awssdk.services.dynamodb.model.CreateTableRequest;
import software.amazon.awssdk.services.dynamodb.model.KeySchemaElement;
import software.amazon.awssdk.services.dynamodb.model.KeyType;
import software.amazon.awssdk.services.dynamodb.model.ScalarAttributeType;

public class AppTest {
    private static DynamoDbClient client;
    @Mock
    private Context context;
    private App app;

    @BeforeAll
    public static void setupDynamoLocal() {
        int port = getFreePort();
        try {
            DynamoDBProxyServer dynamoProxy = ServerRunner.createServerFromCommandLineArgs(new String[] {
                    "-inMemory",
                    "-port",
                    Integer.toString(port)
            });
            dynamoProxy.start();
        } catch (Exception e) {
            throw new RuntimeException();
        }

        client = DynamoDbClient.builder()
                .httpClient(UrlConnectionHttpClient.builder().build())
                .region(Region.EU_WEST_1)
                .endpointOverride(URI.create("http://localhost:" + port))
                .credentialsProvider(StaticCredentialsProvider.create(
                        AwsBasicCredentials.create("FAKE", "FAKE")))
                .build();

        client.createTable(CreateTableRequest.builder()
                .tableName("idempotency")
                .keySchema(KeySchemaElement.builder().keyType(KeyType.HASH).attributeName("id").build())
                .attributeDefinitions(
                        AttributeDefinition.builder().attributeName("id").attributeType(ScalarAttributeType.S).build()
                )
                .billingMode(BillingMode.PAY_PER_REQUEST)
                .build());
    }

    private static int getFreePort() {
        try {
            ServerSocket socket = new ServerSocket(0);
            int port = socket.getLocalPort();
            socket.close();
            return port;
        } catch (IOException ioe) {
            throw new RuntimeException(ioe);
        }
    }

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        app = new App(client);
    }

    @Test
    public void testApp() {
        APIGatewayProxyResponseEvent response =
                app.handleRequest(EventLoader.loadApiGatewayRestEvent("event.json"), context);
        Assertions.assertNotNull(response);
        Assertions.assertTrue(response.getBody().contains("hello world"));
    }
}

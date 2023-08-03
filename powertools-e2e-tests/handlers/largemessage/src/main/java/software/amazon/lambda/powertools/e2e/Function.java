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

package software.amazon.lambda.powertools.e2e;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestHandler;
import com.amazonaws.services.lambda.runtime.events.SQSBatchResponse;
import com.amazonaws.services.lambda.runtime.events.SQSEvent;
import com.amazonaws.services.lambda.runtime.events.SQSEvent.SQSMessage;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;
import software.amazon.awssdk.http.urlconnection.UrlConnectionHttpClient;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.dynamodb.DynamoDbClient;
import software.amazon.awssdk.services.dynamodb.model.AttributeValue;
import software.amazon.awssdk.services.dynamodb.model.PutItemRequest;
import software.amazon.awssdk.utils.BinaryUtils;
import software.amazon.awssdk.utils.Md5Utils;
import software.amazon.lambda.powertools.largemessages.LargeMessage;
import software.amazon.lambda.powertools.logging.Logging;

public class Function implements RequestHandler<SQSEvent, SQSBatchResponse> {

    private static final String TABLE_FOR_ASYNC_TESTS = System.getenv("TABLE_FOR_ASYNC_TESTS");
    private DynamoDbClient client;

    public Function() {
        if (client == null) {
            client = DynamoDbClient.builder()
                    .httpClient(UrlConnectionHttpClient.builder().build())
                    .region(Region.of(System.getenv("AWS_REGION")))
                    .build();
        }
    }

    @Logging(logEvent = true)
    public SQSBatchResponse handleRequest(SQSEvent event, Context context) {
        for (SQSMessage message : event.getRecords()) {
            processRawMessage(message, context);
        }
        return SQSBatchResponse.builder().build();
    }

    @LargeMessage
    private void processRawMessage(SQSMessage sqsMessage, Context context) {
        String bodyMD5 = md5(sqsMessage.getBody());
        if (!sqsMessage.getMd5OfBody().equals(bodyMD5)) {
            throw new SecurityException(
                    String.format("message digest does not match, expected %s, got %s", sqsMessage.getMd5OfBody(),
                            bodyMD5));
        }

        Map<String, AttributeValue> item = new HashMap<>();
        item.put("functionName", AttributeValue.builder().s(context.getFunctionName()).build());
        item.put("id", AttributeValue.builder().s(sqsMessage.getMessageId()).build());
        item.put("bodyMD5", AttributeValue.builder().s(bodyMD5).build());
        item.put("bodySize",
                AttributeValue.builder().n(String.valueOf(sqsMessage.getBody().getBytes(StandardCharsets.UTF_8).length))
                        .build());

        client.putItem(PutItemRequest.builder().tableName(TABLE_FOR_ASYNC_TESTS).item(item).build());
    }

    private String md5(String message) {
        return BinaryUtils.toHex(Md5Utils.computeMD5Hash(message.getBytes(StandardCharsets.UTF_8)));
    }
}
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

import com.amazonaws.lambda.thirdparty.com.fasterxml.jackson.databind.ObjectMapper;
import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestHandler;
import com.amazonaws.services.lambda.runtime.events.DynamodbEvent;
import com.amazonaws.services.lambda.runtime.events.KinesisEvent;
import com.amazonaws.services.lambda.runtime.events.SQSBatchResponse;
import com.amazonaws.services.lambda.runtime.events.SQSEvent;
import com.amazonaws.services.lambda.runtime.events.StreamsEventResponse;
import com.amazonaws.services.lambda.runtime.serialization.factories.JacksonFactory;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.JsonNode;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.stream.Collectors;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.core.util.IOUtils;
import software.amazon.awssdk.services.dynamodb.DynamoDbClient;
import software.amazon.awssdk.services.dynamodb.model.AttributeValue;
import software.amazon.awssdk.services.dynamodb.model.PutItemRequest;
import software.amazon.lambda.powertools.batch.BatchMessageHandlerBuilder;
import software.amazon.lambda.powertools.batch.handler.BatchMessageHandler;
import software.amazon.lambda.powertools.e2e.model.Product;
import software.amazon.lambda.powertools.logging.Logging;
import software.amazon.lambda.powertools.utilities.JsonConfig;


public class Function implements RequestHandler<InputStream, Object> {

    private final static Logger LOGGER = LogManager.getLogger(Function.class);

    private final BatchMessageHandler<SQSEvent, SQSBatchResponse> sqsHandler;
    private final BatchMessageHandler<KinesisEvent, StreamsEventResponse> kinesisHandler;
    private final BatchMessageHandler<DynamodbEvent, StreamsEventResponse> ddbHandler;
    private final String ddbOutputTable;
    private DynamoDbClient ddbClient;

    public Function() {
        sqsHandler = new BatchMessageHandlerBuilder()
                .withSqsBatchHandler()
                .buildWithMessageHandler(this::processProductMessage, Product.class);

        kinesisHandler = new BatchMessageHandlerBuilder()
                .withKinesisBatchHandler()
                .buildWithMessageHandler(this::processProductMessage, Product.class);

        ddbHandler = new BatchMessageHandlerBuilder()
                .withDynamoDbBatchHandler()
                .buildWithRawMessageHandler(this::processDdbMessage);

        this.ddbOutputTable = System.getenv("TABLE_FOR_ASYNC_TESTS");
    }

    private void processProductMessage(Product p, Context c) {
        LOGGER.info("Processing product " + p);

        // TODO - write product details to output table
        ddbClient = DynamoDbClient.builder()
                .build();
        ddbClient.putItem(PutItemRequest.builder()
                        .tableName(ddbOutputTable)
                        .item(new HashMap<String, AttributeValue>() {
                            {
                                put("functionName", AttributeValue.builder()
                                        .s(c.getFunctionName())
                                        .build());
                                put("id", AttributeValue.builder()
                                        .s(Long.toString(p.getId()))
                                        .build());
                                put("name", AttributeValue.builder()
                                        .s(p.getName())
                                        .build());
                                put("price", AttributeValue.builder()
                                        .n(Double.toString(p.getPrice()))
                                        .build());
                            }})
                .build());
    }

    private void processDdbMessage(DynamodbEvent.DynamodbStreamRecord dynamodbStreamRecord, Context context) {
        LOGGER.info("Processing DynamoDB Stream Record" + dynamodbStreamRecord);

        // TODO write DDB change details to batch output table
    }

    private Object createResult(String input, Context context) {

        // TODO - this should work for all the different types, by working
        // TODO out what we can deserialize to. Making it work _just for_ SQS for
        // TODO now to test the E2E framework.
        ObjectMapper mapper = JacksonFactory.getInstance().getMapper();

        LOGGER.info(input);

        try {
            SQSEvent event = mapper.readValue(input, SQSEvent.class);
            return sqsHandler.processBatch(event, context);
        } catch (Exception e) {
            LOGGER.warn("Wasn't SQS", e);
        }

        try {
            KinesisEvent event = mapper.readValue(input, KinesisEvent.class);
            return kinesisHandler.processBatch(event, context);
        } catch (Exception e) {
            LOGGER.warn("Wasn't Kinesis", e);
        }

        try {
            DynamodbEvent event = mapper.readValue(input, DynamodbEvent.class);
            return ddbHandler.processBatch(event, context);
        } catch (Exception e) {
            LOGGER.warn("Wasn't DynamoDB");
        }

        throw new RuntimeException("Couldn't deserialize an event we understood. Giving up!");

    }

    @Override
    public Object handleRequest(InputStream inputStream, Context context) {

        String input = new BufferedReader(
                new InputStreamReader(inputStream, StandardCharsets.UTF_8))
                .lines()
                .collect(Collectors.joining("\n"));

        return createResult(input, context);
    }
}

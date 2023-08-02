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
import com.amazonaws.services.lambda.runtime.events.DynamodbEvent;
import com.amazonaws.services.lambda.runtime.events.KinesisEvent;
import com.amazonaws.services.lambda.runtime.events.SQSBatchResponse;
import com.amazonaws.services.lambda.runtime.events.SQSEvent;
import com.amazonaws.services.lambda.runtime.events.StreamsEventResponse;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import software.amazon.lambda.powertools.batch.BatchMessageHandlerBuilder;
import software.amazon.lambda.powertools.batch.handler.BatchMessageHandler;
import software.amazon.lambda.powertools.e2e.model.Product;
import software.amazon.lambda.powertools.logging.Logging;


public class Function implements RequestHandler<Object, Object> {

    private final static Logger LOGGER = LogManager.getLogger(Function.class);

    private final BatchMessageHandler<SQSEvent, SQSBatchResponse> sqsHandler;
    private final BatchMessageHandler<KinesisEvent, StreamsEventResponse> kinesisHandler;
    private final BatchMessageHandler<DynamodbEvent, StreamsEventResponse> ddbHandler;
    private final String ddbOutputTable;

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

        this.ddbOutputTable = System.getenv("BATCH_OUTPUT_TABLE");
    }

    @Logging(logEvent = true)
    public Object handleRequest(Object input, Context context) {
        // TODO - make this work by working out whether or not we can convert the input
        // TODO to each of the different types. Doing it with the ENV thing will make it hard with the E2E framework.
        String streamType = System.getenv("STREAM_TYPE");
        switch (streamType) {
            case "sqs":
                return sqsHandler.processBatch((SQSEvent) input, context);
            case "kinesis":
                return kinesisHandler.processBatch((KinesisEvent) input, context);
            case "dynamo":
                return ddbHandler.processBatch((DynamodbEvent) input, context);
        }
        throw new RuntimeException("Whoops! Expected to find sqs/kinesis/dynamo in env var STREAM_TYPE but found " + streamType);
    }

    private void processProductMessage(Product p, Context c) {
        LOGGER.info("Processing product " + p);

        // TODO - write product details to output table
    }

    private void processDdbMessage(DynamodbEvent.DynamodbStreamRecord dynamodbStreamRecord, Context context) {
        LOGGER.info("Processing DynamoDB Stream Record" + dynamodbStreamRecord);

        // TODO write DDB change details to batch output table
    }
}
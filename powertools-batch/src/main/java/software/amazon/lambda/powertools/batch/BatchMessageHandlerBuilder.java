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

package software.amazon.lambda.powertools.batch;

import software.amazon.lambda.powertools.batch.builder.DynamoDbBatchMessageHandlerBuilder;
import software.amazon.lambda.powertools.batch.builder.KinesisBatchMessageHandlerBuilder;
import software.amazon.lambda.powertools.batch.builder.SqsBatchMessageHandlerBuilder;

/**
 * A builder-style interface we can use to build batch processing handlers for SQS, Kinesis Streams,
 * and DynamoDB Streams batches. The batch processing handlers that are returned allow
 * the user to easily process batches of messages, one-by-one, while offloading
 * the common issues - failure handling, partial responses, deserialization -
 * to the library.
 *
 * @see <a href="https://docs.powertools.aws.dev/lambda/java/utilities/batch/">Powertools for AWS Lambda (Java) Batch Documentation</a>
 **/
public class BatchMessageHandlerBuilder {

    /**
     * Build an SQS-batch message handler.
     *
     * @return A fluent builder interface to continue the building
     */
    public SqsBatchMessageHandlerBuilder withSqsBatchHandler() {
        return new SqsBatchMessageHandlerBuilder();
    }

    /**
     * Build a DynamoDB streams batch message handler.
     *
     * @return A fluent builder interface to continue the building
     */
    public DynamoDbBatchMessageHandlerBuilder withDynamoDbBatchHandler() {
        return new DynamoDbBatchMessageHandlerBuilder();
    }

    /**
     * Builds a Kinesis streams batch message handler.
     *
     * @return a fluent builder interface to continue the building
     */
    public KinesisBatchMessageHandlerBuilder withKinesisBatchHandler() {
        return new KinesisBatchMessageHandlerBuilder();
    }
}

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

package software.amazon.lambda.powertools.largemessages.tools;

import com.amazon.sqs.javamessaging.AmazonSQSExtendedClient;
import com.amazon.sqs.javamessaging.ExtendedClientConfiguration;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.sqs.SqsClient;
import software.amazon.awssdk.services.sqs.model.SendMessageRequest;
import software.amazon.awssdk.services.sqs.model.SendMessageResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Arrays;

public class LargeMessageProducer {
    private static final Logger LOG = LoggerFactory.getLogger(LargeMessageProducer.class);

    public static void main(String[] args) {
        if (args.length < 2) {
            LOG.error("Usage: LargeMessageProducer <SQS_QUEUE_URL> <S3_BUCKET_NAME>");
            System.exit(1);
        }

        String queueUrl = args[0];
        String bucketName = args[1];

        LOG.info("Starting Large Message Producer...");
        LOG.info("Queue URL: {}", queueUrl);
        LOG.info("S3 Bucket: {}", bucketName);

        // Initialize S3 and SQS clients
        S3Client s3Client = S3Client.create();
        SqsClient sqsClient = SqsClient.create();

        // Configure Extended Client
        ExtendedClientConfiguration extendedClientConfig = new ExtendedClientConfiguration()
                .withPayloadSupportEnabled(s3Client, bucketName);

        SqsClient extendedSqsClient = new AmazonSQSExtendedClient(sqsClient, extendedClientConfig);

        // Generate large payload (> 256KB). 300KB to be safe.
        String payload = generateLargePayload(300 * 1024);

        try {
            SendMessageRequest request = SendMessageRequest.builder()
                    .queueUrl(queueUrl)
                    .messageBody(payload)
                    .build();

            SendMessageResponse response = extendedSqsClient.sendMessage(request);
            LOG.info("Message sent successfully. Message ID: {}", response.messageId());
        } catch (Exception e) {
            LOG.error("Failed to send message", e);
            System.exit(1);
        }
    }

    private static String generateLargePayload(int sizeInBytes) {
        char[] chars = new char[sizeInBytes];
        Arrays.fill(chars, 'A');
        // create a simple JSON structure
        return String.format("{\"data\": \"%s\"}", new String(chars));
    }
}

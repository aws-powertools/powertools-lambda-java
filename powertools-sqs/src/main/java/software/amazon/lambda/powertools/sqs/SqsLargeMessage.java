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

package software.amazon.lambda.powertools.sqs;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * @deprecated See software.amazon.lambda.powertools.largemessages.LargeMessage in powertools-large-messages module.
 * Will be removed in version 2.
 *
 * <p>{@code SqsLargeMessage} is used to signal that the annotated method
 * should be extended to handle large SQS messages which have been offloaded
 * to S3</p>
 *
 * <p>{@code SqsLargeMessage} automatically retrieves and deletes messages
 * which have been offloaded to S3 using the {@code amazon-sqs-java-extended-client-lib}
 * client library.</p>
 *
 * <p>This version of the {@code SqsLargeMessage} is compatible with version
 * 1.1.0+ of {@code amazon-sqs-java-extended-client-lib}.</p>
 *
 * <pre>
 * &lt;dependency&gt;
 *   &lt;groupId&gt;com.amazonaws&lt;/groupId&gt;
 *   &lt;artifactId&gt;amazon-sqs-java-extended-client-lib&lt;/artifactId&gt;
 *   &lt;version&gt;1.1.0&lt;/version&gt;
 * &lt;/dependency&gt;
 * </pre>
 *
 * <p>{@code SqsLargeMessage} should be used with the handleRequest method of a class
 * which implements {@code com.amazonaws.services.lambda.runtime.RequestHandler} with
 * {@code com.amazonaws.services.lambda.runtime.events.SQSEvent} as the first parameter.</p>
 *
 * <pre>
 * public class SqsMessageHandler implements RequestHandler<SQSEvent, String> {
 *
 *    {@literal @}Override
 *    {@literal @}SqsLargeMessage
 *     public String handleRequest(SQSEvent sqsEvent, Context context) {
 *
 *         // process messages
 *
 *         return "ok";
 *     }
 *
 *     ...
 * </pre>
 *
 * <p>Using the default S3 Client {@code AmazonS3 amazonS3 = AmazonS3ClientBuilder.defaultClient();}
 * each record received in the SQSEvent {@code SqsLargeMessage} will checked
 * to see if it's body contains a payload which has been offloaded to S3. If it
 * does then {@code getObject(bucket, key)} will be called and the payload
 * retrieved.</p>
 *
 * <p><b>Note</b>: Retreiving payloads from S3 will increase the duration of the
 * Lambda function.</p>
 *
 * <p>If the request handler method returns then each payload will be deleted
 * from S3 using {@code deleteObject(bucket, key)}</p>
 *
 * <p>To disable deletion of payloads setting the following annotation parameter
 * {@code @SqsLargeMessage(deletePayloads=false)}</p>
 */
@Deprecated
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.METHOD)
public @interface SqsLargeMessage {

    boolean deletePayloads() default true;
}

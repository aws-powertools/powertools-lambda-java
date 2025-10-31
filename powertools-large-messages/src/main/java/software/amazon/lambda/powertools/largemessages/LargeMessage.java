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

package software.amazon.lambda.powertools.largemessages;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * <p>Use this annotation to handle large messages (> 1 MB) from SQS or SNS.
 * When large messages are sent to an SQS Queue or SNS Topic, they are offloaded to S3 and only a reference is passed in the message/record.</p>
 *
 * <p>{@code @LargeMessage} automatically retrieves and deletes messages
 * which have been offloaded to S3 using the {@code amazon-sqs-java-extended-client-lib} or {@code amazon-sns-java-extended-client-lib}
 * client libraries.</p>
 *
 * <p>This version of the {@code @LargeMessage} is compatible with version 1.1.0+ and 2.0.0+
 * of {@code amazon-sqs-java-extended-client-lib} / {@code amazon-sns-java-extended-client-lib}.</p>
 * <br/>
 * <p>Put this annotation on a method where the first parameter is either a {@link com.amazonaws.services.lambda.runtime.events.SQSEvent.SQSMessage} or {@link com.amazonaws.services.lambda.runtime.events.SNSEvent.SNSRecord}.
 * <br/>
 * <u>SQS</u>:<br/>
 * <pre>
 * &#64;LargeMessage
 * private void processRawMessage(SQSMessage sqsMessage, Context context) {
 *     // sqsMessage.getBody() will contain the content of the S3 Object
 * }
 * </pre>
 * <u>SNS</u>:<br/>
 * <pre>
 * &#64;LargeMessage
 * private void processMessage(SNSRecord snsRecord) {
 *     // snsRecord.getSNS().getMessage() will contain the content of the S3 Object
 * }
 * </pre>
 * </p>
 *
 * <p>To disable the deletion of S3 objects, you can configure the {@code deleteS3Object} option to false (default is true):
 * <pre>
 *     &#64;LargeMessage(deleteS3Object = false)
 * </pre>
 * </p>
 *
 * <p><b>Note 1</b>: The message object (SQSMessage or SNSRecord) is modified in-place to avoid duplicating
 * the large blob in memory. The message body will be replaced with the S3 object content.</p>
 * <p><b>Note 2</b>: Retrieving payloads and deleting objects from S3 will increase the duration of the
 * Lambda function.</p>
 * <p><b>Note 3</b>: Make sure to configure your function with enough memory to be able to retrieve S3 objects.</p>
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.METHOD)
public @interface LargeMessage {

    /**
     * Specify if S3 objects must be deleted after being processed (default = true)
     * Alternatively you might consider using S3 lifecycle policies to remove the payloads automatically after a period of time.
     */
    boolean deleteS3Object() default true;
}

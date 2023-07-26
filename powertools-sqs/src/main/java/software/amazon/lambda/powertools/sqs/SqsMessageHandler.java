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

import static com.amazonaws.services.lambda.runtime.events.SQSEvent.SQSMessage;

import com.amazonaws.services.lambda.runtime.events.SQSEvent;

/**
 * <p>
 * This interface should be implemented for processing {@link SQSMessage} inside {@link SQSEvent} received by lambda
 * function.
 * </p>
 *
 * <p>
 * It is required by utilities:
 * <ul>
 *   <li>{@link SqsBatch}</li>
 *   <li>{@link SqsUtils#batchProcessor(SQSEvent, Class)}</li>
 *   <li>{@link SqsUtils#batchProcessor(SQSEvent, boolean, Class)}</li>
 *   <li>{@link SqsUtils#batchProcessor(SQSEvent, SqsMessageHandler)}</li>
 *   <li>{@link SqsUtils#batchProcessor(SQSEvent, boolean, SqsMessageHandler)}</li>
 * </ul>
 * </p>
 *
 * @param <R> Return value type from {@link SqsMessageHandler#process(SQSMessage)}
 */
@FunctionalInterface
public interface SqsMessageHandler<R> {

    R process(SQSMessage message);
}

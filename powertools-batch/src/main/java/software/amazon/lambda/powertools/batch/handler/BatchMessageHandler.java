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

package software.amazon.lambda.powertools.batch.handler;

import com.amazonaws.services.lambda.runtime.Context;

/**
 * The basic interface a batch message handler must meet.
 *
 * @param <E> The type of the Lambda batch event
 * @param <R> The type of the lambda batch response
 */
public interface BatchMessageHandler<E, R> {

    /**
     * Processes the given batch returning a partial batch
     * response indicating the success and failure of individual
     * messages within the batch.
     *
     * @param event   The Lambda event containing the batch to process
     * @param context The lambda context
     * @return A partial batch response
     */
    R processBatch(E event, Context context);

    /**
     * Processes the given batch in parallel returning a partial batch
     * response indicating the success and failure of individual
     * messages within the batch. <br/>
     * Note that parallel processing is not always better than sequential processing,
     * and you should benchmark your code to determine the best approach for your use case. <br/>
     * Also note that to get more threads available (more vCPUs),
     * you need to increase the amount of memory allocated to your Lambda function. <br/>

     *
     * @param event   The Lambda event containing the batch to process
     * @param context The lambda context
     * @return A partial batch response
     */
    R processBatchInParallel(E event, Context context);
}

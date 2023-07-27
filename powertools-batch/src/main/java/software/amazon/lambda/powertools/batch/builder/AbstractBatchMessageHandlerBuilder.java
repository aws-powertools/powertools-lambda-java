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

package software.amazon.lambda.powertools.batch.builder;

import com.amazonaws.services.lambda.runtime.Context;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import software.amazon.lambda.powertools.batch.handler.BatchMessageHandler;

/**
 * An abstract class to capture common arguments used across all the message-binding-specific batch processing
 * builders. The builders provide a fluent interface to configure the batch processors. Any arguments specific
 * to a particular batch binding can be added to the child builder.
 * <p>
 * We capture types for the various messages involved, so that we can provide an interface that makes
 * sense for the concrete child.
 *
 * @param <T> The type of a single message in the batch
 * @param <C> The type of the child builder. We need this to provide a fluent interface - see also getThis()
 * @param <E> The type of the Lambda batch event
 * @param <R> The type of the batch response we return to Lambda
 */
abstract class AbstractBatchMessageHandlerBuilder<T, C, E, R> {
    protected BiConsumer<T, Throwable> failureHandler;
    protected Consumer<T> successHandler;

    /**
     * Provides an (Optional!) success handler. A success handler is invoked
     * once for each message after it has been processed by the user-provided
     * handler.
     * <p>
     * If the success handler throws, the item in the batch will be
     * marked failed.
     *
     * @param handler The handler to invoke
     */
    public C withSuccessHandler(Consumer<T> handler) {
        this.successHandler = handler;
        return getThis();
    }

    /**
     * Provides an (Optional!) failure handler. A failure handler is invoked
     * once for each message after it has failed to be processed by the
     * user-provided handler. This gives the user's code a useful hook to do
     * anything else that might have to be done in response to a failure - for
     * instance, updating a metric, or writing a detailed log.
     * <p>
     * Please note that this method has nothing to do with the partial batch
     * failure mechanism. Regardless of whether a failure handler is
     * specified, partial batch failures and responses to the Lambda environment
     * are handled by the batch utility separately.
     *
     * @param handler The handler to invoke on failure
     */
    public C withFailureHandler(BiConsumer<T, Throwable> handler) {
        this.failureHandler = handler;
        return getThis();
    }

    /**
     * Builds a BatchMessageHandler that can be used to process batches, given
     * a user-defined handler to process each item in the batch. This variant
     * takes a function that consumes a raw message and the Lambda context. This
     * is useful for handlers that need access to the entire message object, not
     * just the deserialized contents of the body.
     * <p>
     * Note:  If you don't need the Lambda context, use the variant of this function
     * that does not require it.
     *
     * @param handler Takes a raw message - the underlying AWS Events Library event - to process.
     *                For instance for SQS this would be an SQSMessage.
     * @return A BatchMessageHandler for processing the batch
     */
    public abstract BatchMessageHandler<E, R> buildWithRawMessageHandler(BiConsumer<T, Context> handler);

    /**
     * Builds a BatchMessageHandler that can be used to process batches, given
     * a user-defined handler to process each item in the batch. This variant
     * takes a function that consumes a raw message and the Lambda context. This
     * is useful for handlers that need access to the entire message object, not
     * just the deserialized contents of the body.
     *
     * @param handler Takes a raw message - the underlying AWS Events Library event - to process.
     *                For instance for SQS this would be an SQSMessage.
     * @return A BatchMessageHandler for processing the batch
     */
    public BatchMessageHandler<E, R> buildWithRawMessageHandler(Consumer<T> handler) {
        return buildWithRawMessageHandler((f, c) -> handler.accept(f));
    }

    /**
     * Builds a BatchMessageHandler that can be used to process batches, given
     * a user-defined handler to process each item in the batch. This variant
     * takes a function that consumes the deserialized body of the given message
     * and the lambda context. If deserialization fails, it will be treated as
     * failure of the processing of that item in the batch.
     * Note:  If you don't need the Lambda context, use the variant of this function
     * that does not require it.
     *
     * @param handler Processes the deserialized body of the message
     * @return A BatchMessageHandler for processing the batch
     */
    public abstract <M> BatchMessageHandler<E, R> buildWithMessageHandler(BiConsumer<M, Context> handler,
                                                                          Class<M> messageClass);

    /**
     * Builds a BatchMessageHandler that can be used to process batches, given
     * a user-defined handler to process each item in the batch. This variant
     * takes a function that consumes the deserialized body of the given message
     * If deserialization fails, it will be treated as
     * failure of the processing of that item in the batch.
     * Note:  If you don't need the Lambda context, use the variant of this function
     * that does not require it.
     *
     * @param handler Processes the deserialized body of the message
     * @return A BatchMessageHandler for processing the batch
     */
    public <M> BatchMessageHandler<E, R> buildWithMessageHandler(Consumer<M> handler, Class<M> messageClass) {
        return buildWithMessageHandler((f, c) -> handler.accept(f), messageClass);
    }


    /**
     * Used to chain the fluent builder interface through the child classes.
     *
     * @return This
     */
    protected abstract C getThis();
}

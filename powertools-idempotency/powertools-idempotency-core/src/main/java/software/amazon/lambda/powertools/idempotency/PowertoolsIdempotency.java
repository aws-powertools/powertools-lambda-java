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

package software.amazon.lambda.powertools.idempotency;

import java.util.function.Function;
import java.util.function.Supplier;

import com.amazonaws.services.lambda.runtime.Context;
import com.fasterxml.jackson.databind.JsonNode;

import software.amazon.lambda.powertools.idempotency.internal.IdempotencyHandler;
import software.amazon.lambda.powertools.utilities.JsonConfig;

/**
 * PowertoolsIdempotency offers a functional API for implementing idempotent workloads
 * without relying on AspectJ annotations.
 * 
 * <p>This class is thread-safe. All operations delegate to the underlying persistence store
 * which handles concurrent access safely.</p>
 * 
 * <p><strong>Important:</strong> Always call {@link Idempotency#registerLambdaContext(Context)}
 * at the start of your handler to enable proper timeout handling.</p>
 * 
 * <p>Example usage with Function (single parameter):</p>
 * <pre>{@code
 * public Basket handleRequest(Product input, Context context) {
 *     Idempotency.registerLambdaContext(context);
 *     return PowertoolsIdempotency.makeIdempotent(this::processProduct, input, Basket.class);
 * }
 * 
 * private Basket processProduct(Product product) {
 *     // business logic
 * }
 * }</pre>
 * 
 * <p>Example usage with Supplier (multi-parameter methods):</p>
 * <pre>{@code
 * public String handleRequest(SQSEvent event, Context context) {
 *     Idempotency.registerLambdaContext(context);
 *     return PowertoolsIdempotency.makeIdempotent(
 *         event.getRecords().get(0).getBody(),
 *         () -> processPayment(orderId, amount, currency),
 *         String.class
 *     );
 * }
 * }</pre>
 * 
 * <p>When different methods use the same payload as idempotency key, use explicit function names
 * to differentiate between them:</p>
 * <pre>{@code
 * // Different methods, same payload
 * PowertoolsIdempotency.makeIdempotent("processPayment", orderId, 
 *     () -> processPayment(orderId), String.class);
 * 
 * PowertoolsIdempotency.makeIdempotent("refundPayment", orderId, 
 *     () -> refundPayment(orderId), String.class);
 * }</pre>
 * 
 * @see Idempotency
 * @see #makeIdempotent(Object, Supplier, Class)
 * @see #makeIdempotent(String, Object, Supplier, Class)
 * @see #makeIdempotent(Function, Object, Class)
 */
public final class PowertoolsIdempotency {

    private static final String DEFAULT_FUNCTION_NAME = "function";

    private PowertoolsIdempotency() {
        // Utility class
    }

    /**
     * Makes a function idempotent using the provided idempotency key.
     * Uses a default function name for namespacing the idempotency key.
     * 
     * <p>This method is thread-safe and can be used in parallel processing scenarios
     * such as batch processors.</p>
     * 
     * <p>This method is suitable for making methods idempotent that have more than one parameter.
     * For simple single-parameter methods, {@link #makeIdempotent(Function, Object, Class)} is more intuitive.</p>
     * 
     * <p><strong>Note:</strong> If you need to call different functions with the same payload,
     * use {@link #makeIdempotent(String, Object, Supplier, Class)} to specify distinct function names.
     * This ensures each function has its own idempotency scope.</p>
     * 
     * @param idempotencyKey the key used for idempotency (will be converted to JSON)
     * @param function the function to make idempotent
     * @param returnType the class of the return type for deserialization
     * @param <T> the return type of the function
     * @return the result of the function execution (either fresh or cached)
     * @throws Throwable if the function execution fails
     */
    public static <T> T makeIdempotent(Object idempotencyKey, Supplier<T> function, Class<T> returnType)
            throws Throwable {
        return makeIdempotent(DEFAULT_FUNCTION_NAME, idempotencyKey, function, returnType);
    }

    /**
     * Makes a function idempotent using the provided function name and idempotency key.
     * 
     * <p>This method is thread-safe and can be used in parallel processing scenarios
     * such as batch processors.</p>
     * 
     * @param functionName the name of the function (used for persistence store configuration)
     * @param idempotencyKey the key used for idempotency (will be converted to JSON)
     * @param function the function to make idempotent
     * @param returnType the class of the return type for deserialization
     * @param <T> the return type of the function
     * @return the result of the function execution (either fresh or cached)
     * @throws Throwable if the function execution fails
     */
    @SuppressWarnings("unchecked")
    public static <T> T makeIdempotent(String functionName, Object idempotencyKey, Supplier<T> function,
            Class<T> returnType)
            throws Throwable {
        JsonNode payload = JsonConfig.get().getObjectMapper().valueToTree(idempotencyKey);
        Context lambdaContext = Idempotency.getInstance().getConfig().getLambdaContext();

        IdempotencyHandler handler = new IdempotencyHandler(
                function::get,
                returnType,
                functionName,
                payload,
                lambdaContext);

        Object result = handler.handle();
        return (T) result;
    }

    /**
     * Makes a function with one parameter idempotent.
     * The parameter is used as the idempotency key.
     * 
     * <p>For functions with more than one parameter, use {@link #makeIdempotent(Object, Supplier, Class)} instead.</p>
     * 
     * <p><strong>Note:</strong> If you need to call different functions with the same payload,
     * use {@link #makeIdempotent(String, Object, Supplier, Class)} to specify distinct function names.
     * This ensures each function has its own idempotency scope.</p>
     * 
     * @param function the function to make idempotent (method reference)
     * @param arg the argument to pass to the function (also used as idempotency key)
     * @param returnType the class of the return type for deserialization
     * @param <T> the argument type
     * @param <R> the return type
     * @return the result of the function execution (either fresh or cached)
     * @throws Throwable if the function execution fails
     */
    public static <T, R> R makeIdempotent(Function<T, R> function, T arg, Class<R> returnType) throws Throwable {
        return makeIdempotent(DEFAULT_FUNCTION_NAME, arg, () -> function.apply(arg), returnType);
    }

}

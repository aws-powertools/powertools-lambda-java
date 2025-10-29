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
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;

import software.amazon.lambda.powertools.idempotency.exceptions.IdempotencyConfigurationException;
import software.amazon.lambda.powertools.idempotency.internal.IdempotencyHandler;
import software.amazon.lambda.powertools.idempotency.persistence.BasePersistenceStore;
import software.amazon.lambda.powertools.utilities.JsonConfig;

/**
 * Idempotency provides both a configuration and a functional API for implementing idempotent workloads.
 * 
 * <p>This class is thread-safe. All operations delegate to the underlying persistence store
 * which handles concurrent access safely.</p>
 * 
 * <h2>Configuration</h2>
 * <p>Configure the persistence layer and idempotency settings before your handler executes (e.g. in constructor):</p>
 * <pre>{@code
 * Idempotency.config()
 *     .withPersistenceStore(persistenceStore)
 *     .withConfig(idempotencyConfig)
 *     .configure();
 * }</pre>
 * 
 * <h2>Functional API</h2>
 * <p>Make methods idempotent without AspectJ annotations. Generic return types (e.g., {@code Map<String, Object>}, 
 * {@code List<Product>}) are supported via Jackson {@link TypeReference}.</p>
 * 
 * <p><strong>Important:</strong> Always call {@link #registerLambdaContext(Context)}
 * at the start of your handler to enable proper timeout handling.</p>
 * 
 * <p>Example usage with Function (single parameter):</p>
 * <pre>{@code
 * public Basket handleRequest(Product input, Context context) {
 *     Idempotency.registerLambdaContext(context);
 *     return Idempotency.makeIdempotent(this::processProduct, input, Basket.class);
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
 *     return Idempotency.makeIdempotent(
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
 * Idempotency.makeIdempotent("processPayment", orderId, 
 *     () -> processPayment(orderId), String.class);
 * 
 * Idempotency.makeIdempotent("refundPayment", orderId, 
 *     () -> refundPayment(orderId), String.class);
 * }</pre>
 * 
 * @see #config()
 * @see #registerLambdaContext(Context)
 * @see #makeIdempotent(Object, Supplier, Class)
 * @see #makeIdempotent(String, Object, Supplier, Class)
 * @see #makeIdempotent(Function, Object, Class)
 * @see #makeIdempotent(Object, Supplier, TypeReference)
 * @see #makeIdempotent(String, Object, Supplier, TypeReference)
 * @see #makeIdempotent(Function, Object, TypeReference)
 */
public final class Idempotency {
    private static final String DEFAULT_FUNCTION_NAME = "function";

    private IdempotencyConfig config;
    private BasePersistenceStore persistenceStore;

    private Idempotency() {
    }

    public static Idempotency getInstance() {
        return Holder.instance;
    }

    /**
     * Can be used in a method which is not the handler to capture the Lambda context,
     * to calculate the remaining time before the invocation times out.
     *
     * @param lambdaContext
     */
    public static void registerLambdaContext(Context lambdaContext) {
        getInstance().getConfig().setLambdaContext(lambdaContext);
    }

    /**
     * Acts like a builder that can be used to configure {@link Idempotency}
     *
     * @return a new instance of {@link Config}
     */
    public static Config config() {
        return new Config();
    }

    public IdempotencyConfig getConfig() {
        return config;
    }

    private void setConfig(IdempotencyConfig config) {
        this.config = config;
    }

    public BasePersistenceStore getPersistenceStore() {
        if (persistenceStore == null) {
            throw new IllegalStateException("Persistence Store is null, did you call 'configure()'?");
        }
        return persistenceStore;
    }

    private void setPersistenceStore(BasePersistenceStore persistenceStore) {
        this.persistenceStore = persistenceStore;
    }

    private static final class Holder {
        private static final Idempotency instance = new Idempotency();
    }

    public static class Config {

        private IdempotencyConfig config;
        private BasePersistenceStore store;

        /**
         * Use this method after configuring persistence layer (mandatory) and idem potency configuration (optional)
         */
        public void configure() {
            if (store == null) {
                throw new IllegalStateException(
                        "Persistence Layer is null, configure one with 'withPersistenceStore()'");
            }
            if (config == null) {
                config = IdempotencyConfig.builder().build();
            }
            Idempotency.getInstance().setConfig(config);
            Idempotency.getInstance().setPersistenceStore(store);
        }

        public Config withPersistenceStore(BasePersistenceStore persistenceStore) {
            this.store = persistenceStore;
            return this;
        }

        public Config withConfig(IdempotencyConfig config) {
            this.config = config;
            return this;
        }
    }

    // Functional API methods

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
     */
    public static <T> T makeIdempotent(Object idempotencyKey, Supplier<T> function, Class<T> returnType) {
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
     */
    public static <T> T makeIdempotent(String functionName, Object idempotencyKey, Supplier<T> function,
            Class<T> returnType) {
        return makeIdempotent(functionName, idempotencyKey, function, JsonConfig.toTypeReference(returnType));
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
     */
    public static <T, R> R makeIdempotent(Function<T, R> function, T arg, Class<R> returnType) {
        return makeIdempotent(DEFAULT_FUNCTION_NAME, arg, () -> function.apply(arg), returnType);
    }

    /**
     * Makes a function idempotent using the provided idempotency key with support for generic return types.
     * Uses a default function name for namespacing the idempotency key.
     * 
     * <p>Use this method when the return type contains generics (e.g., {@code Map<String, Basket>}).</p>
     * 
     * <p>Example usage:</p>
     * <pre>{@code
     * Map<String, Basket> result = Idempotency.makeIdempotent(
     *     payload,
     *     () -> processBaskets(),
     *     new TypeReference<Map<String, Basket>>() {}
     * );
     * }</pre>
     * 
     * @param idempotencyKey the key used for idempotency (will be converted to JSON)
     * @param function the function to make idempotent
     * @param typeRef the TypeReference for deserialization of generic types
     * @param <T> the return type of the function
     * @return the result of the function execution (either fresh or cached)
     */
    public static <T> T makeIdempotent(Object idempotencyKey, Supplier<T> function, TypeReference<T> typeRef) {
        return makeIdempotent(DEFAULT_FUNCTION_NAME, idempotencyKey, function, typeRef);
    }

    /**
     * Makes a function idempotent using the provided function name and idempotency key with support for generic return types.
     * 
     * @param functionName the name of the function (used for persistence store configuration)
     * @param idempotencyKey the key used for idempotency (will be converted to JSON)
     * @param function the function to make idempotent
     * @param typeRef the TypeReference for deserialization of generic types
     * @param <T> the return type of the function
     * @return the result of the function execution (either fresh or cached)
     */
    @SuppressWarnings("unchecked")
    public static <T> T makeIdempotent(String functionName, Object idempotencyKey, Supplier<T> function,
            TypeReference<T> typeRef) {
        try {
            JsonNode payload = JsonConfig.get().getObjectMapper().valueToTree(idempotencyKey);
            Context lambdaContext = Idempotency.getInstance().getConfig().getLambdaContext();

            IdempotencyHandler handler = new IdempotencyHandler(
                    function::get,
                    typeRef,
                    functionName,
                    payload,
                    lambdaContext);

            Object result = handler.handle();
            return (T) result;
        } catch (RuntimeException e) {
            throw e;
        } catch (Throwable e) {
            throw new IdempotencyConfigurationException("Idempotency operation failed: " + e.getMessage());
        }
    }

    /**
     * Makes a function with one parameter idempotent with support for generic return types.
     * The parameter is used as the idempotency key.
     * 
     * <p>Example usage:</p>
     * <pre>{@code
     * Map<String, Basket> result = Idempotency.makeIdempotent(
     *     this::processProduct,
     *     product,
     *     new TypeReference<Map<String, Basket>>() {}
     * );
     * }</pre>
     * 
     * @param function the function to make idempotent (method reference)
     * @param arg the argument to pass to the function (also used as idempotency key)
     * @param typeRef the TypeReference for deserialization of generic types
     * @param <T> the argument type
     * @param <R> the return type
     * @return the result of the function execution (either fresh or cached)
     */
    public static <T, R> R makeIdempotent(Function<T, R> function, T arg, TypeReference<R> typeRef) {
        return makeIdempotent(DEFAULT_FUNCTION_NAME, arg, () -> function.apply(arg), typeRef);
    }

}

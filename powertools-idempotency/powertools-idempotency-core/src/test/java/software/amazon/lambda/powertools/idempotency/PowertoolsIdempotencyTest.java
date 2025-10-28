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

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;

import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import java.util.OptionalInt;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.amazonaws.services.lambda.runtime.Context;
import com.fasterxml.jackson.databind.JsonNode;

import software.amazon.lambda.powertools.common.stubs.TestLambdaContext;
import software.amazon.lambda.powertools.idempotency.exceptions.IdempotencyItemAlreadyExistsException;
import software.amazon.lambda.powertools.idempotency.exceptions.IdempotencyItemNotFoundException;
import software.amazon.lambda.powertools.idempotency.handlers.PowertoolsIdempotencyFunction;
import software.amazon.lambda.powertools.idempotency.handlers.PowertoolsIdempotencyMultiArgFunction;
import software.amazon.lambda.powertools.idempotency.model.Basket;
import software.amazon.lambda.powertools.idempotency.model.Product;
import software.amazon.lambda.powertools.idempotency.persistence.BasePersistenceStore;
import software.amazon.lambda.powertools.idempotency.persistence.DataRecord;

@ExtendWith(MockitoExtension.class)
class PowertoolsIdempotencyTest {

    private Context context = new TestLambdaContext();

    @Mock
    private BasePersistenceStore store;

    @Test
    void firstCall_shouldPutInStore() {
        Idempotency.config()
                .withPersistenceStore(store)
                .withConfig(IdempotencyConfig.builder()
                        .withEventKeyJMESPath("id")
                        .build())
                .configure();

        PowertoolsIdempotencyFunction function = new PowertoolsIdempotencyFunction();

        Product p = new Product(42, "fake product", 12);
        Basket basket = function.handleRequest(p, context);

        assertThat(function.processCalled()).isTrue();
        assertThat(basket.getProducts()).hasSize(1);

        ArgumentCaptor<JsonNode> nodeCaptor = ArgumentCaptor.forClass(JsonNode.class);
        ArgumentCaptor<OptionalInt> expiryCaptor = ArgumentCaptor.forClass(OptionalInt.class);
        verify(store).saveInProgress(nodeCaptor.capture(), any(), expiryCaptor.capture());
        assertThat(nodeCaptor.getValue().get("id").asLong()).isEqualTo(p.getId());
        assertThat(nodeCaptor.getValue().get("name").asText()).isEqualTo(p.getName());
        assertThat(nodeCaptor.getValue().get("price").asDouble()).isEqualTo(p.getPrice());
        assertThat(expiryCaptor.getValue().orElse(-1)).isEqualTo(30000);

        ArgumentCaptor<Basket> resultCaptor = ArgumentCaptor.forClass(Basket.class);
        verify(store).saveSuccess(any(), resultCaptor.capture(), any());
        assertThat(resultCaptor.getValue()).isEqualTo(basket);
    }

    @Test
    void testMakeIdempotentWithFunctionName() throws Throwable {
        BasePersistenceStore spyStore = spy(BasePersistenceStore.class);
        Idempotency.config()
                .withPersistenceStore(spyStore)
                .configure();
        Idempotency.registerLambdaContext(context);

        String result = PowertoolsIdempotency.makeIdempotent("myFunction", "test-key", () -> "test-result",
                String.class);

        assertThat(result).isEqualTo("test-result");

        ArgumentCaptor<String> functionNameCaptor = ArgumentCaptor.forClass(String.class);
        verify(spyStore).configure(any(), functionNameCaptor.capture());
        assertThat(functionNameCaptor.getValue()).isEqualTo("myFunction");
    }

    @Test
    void testMakeIdempotentWithMethodReferenceUsesDefaultName() throws Throwable {
        BasePersistenceStore spyStore = spy(BasePersistenceStore.class);
        Idempotency.config()
                .withPersistenceStore(spyStore)
                .configure();
        Idempotency.registerLambdaContext(context);

        String result = PowertoolsIdempotency.makeIdempotent("test-key", this::helperMethod, String.class);

        assertThat(result).isEqualTo("helper-result");

        ArgumentCaptor<String> functionNameCaptor = ArgumentCaptor.forClass(String.class);
        verify(spyStore).configure(any(), functionNameCaptor.capture());
        assertThat(functionNameCaptor.getValue()).isEqualTo("function");
    }

    private String helperMethod() {
        return "helper-result";
    }

    @Test
    void testMakeIdempotentWithFunctionOverload() throws Throwable {
        BasePersistenceStore spyStore = spy(BasePersistenceStore.class);
        Idempotency.config()
                .withPersistenceStore(spyStore)
                .configure();
        Idempotency.registerLambdaContext(context);

        Product p = new Product(42, "test product", 10);
        Basket result = PowertoolsIdempotency.makeIdempotent(this::processProduct, p, Basket.class);

        assertThat(result.getProducts()).hasSize(1);
        assertThat(result.getProducts().get(0)).isEqualTo(p);

        ArgumentCaptor<String> functionNameCaptor = ArgumentCaptor.forClass(String.class);
        verify(spyStore).configure(any(), functionNameCaptor.capture());
        assertThat(functionNameCaptor.getValue()).isEqualTo("function");

        ArgumentCaptor<JsonNode> nodeCaptor = ArgumentCaptor.forClass(JsonNode.class);
        verify(spyStore).saveInProgress(nodeCaptor.capture(), any(), any());
        assertThat(nodeCaptor.getValue().get("id").asLong()).isEqualTo(p.getId());
    }

    private Basket processProduct(Product product) {
        Basket basket = new Basket();
        basket.add(product);
        return basket;
    }

    @Test
    void firstCall_withExplicitIdempotencyKey_shouldPutInStore() {
        Idempotency.config()
                .withPersistenceStore(store)
                .configure();

        PowertoolsIdempotencyMultiArgFunction function = new PowertoolsIdempotencyMultiArgFunction();

        Product p = new Product(42, "fake product", 12);
        Basket basket = function.handleRequest(p, context);

        assertThat(function.processCalled()).isTrue();
        assertThat(function.getExtraData()).isEqualTo("extra-data");
        assertThat(basket.getProducts()).hasSize(1);

        ArgumentCaptor<JsonNode> nodeCaptor = ArgumentCaptor.forClass(JsonNode.class);
        verify(store).saveInProgress(nodeCaptor.capture(), any(), any());
        assertThat(nodeCaptor.getValue().asLong()).isEqualTo(p.getId());

        ArgumentCaptor<Basket> resultCaptor = ArgumentCaptor.forClass(Basket.class);
        verify(store).saveSuccess(any(), resultCaptor.capture(), any());
        assertThat(resultCaptor.getValue()).isEqualTo(basket);
    }

    @Test
    void secondCall_shouldRetrieveFromCacheAndDeserialize() throws Throwable {
        // Use in-memory persistence store to test actual serialization/deserialization
        Map<String, DataRecord> data = new HashMap<>();
        BasePersistenceStore inMemoryStore = new BasePersistenceStore() {
            @Override
            public DataRecord getRecord(String idempotencyKey) throws IdempotencyItemNotFoundException {
                DataRecord dr = data.get(idempotencyKey);
                if (dr == null) {
                    throw new IdempotencyItemNotFoundException(idempotencyKey);
                }
                return dr;
            }

            @Override
            public void putRecord(DataRecord dr, Instant now) throws IdempotencyItemAlreadyExistsException {
                if (data.containsKey(dr.getIdempotencyKey())) {
                    throw new IdempotencyItemAlreadyExistsException();
                }
                data.put(dr.getIdempotencyKey(), dr);
            }

            @Override
            public void updateRecord(DataRecord dr) {
                data.put(dr.getIdempotencyKey(), dr);
            }

            @Override
            public void deleteRecord(String idempotencyKey) {
                data.remove(idempotencyKey);
            }
        };

        Idempotency.config()
                .withPersistenceStore(inMemoryStore)
                .configure();
        Idempotency.registerLambdaContext(context);

        Product p = new Product(42, "test product", 10);
        int[] callCount = { 0 };

        // First call - executes function and stores result
        Basket result1 = PowertoolsIdempotency.makeIdempotent(p, () -> {
            callCount[0]++;
            return processProduct(p);
        }, Basket.class);
        assertThat(result1.getProducts()).hasSize(1);
        assertThat(callCount[0]).isEqualTo(1);

        // Second call - should retrieve from cache, deserialize, and NOT execute function
        Basket result2 = PowertoolsIdempotency.makeIdempotent(p, () -> {
            callCount[0]++;
            return processProduct(p);
        }, Basket.class);
        assertThat(result2.getProducts()).hasSize(1);
        assertThat(result2.getProducts().get(0).getId()).isEqualTo(42);
        assertThat(result2.getProducts().get(0).getName()).isEqualTo("test product");
        assertThat(callCount[0]).isEqualTo(1); // Function should NOT be called again
    }

    @Test
    void concurrentInvocations_shouldNotLeakContext() throws Exception {
        Idempotency.config()
                .withPersistenceStore(store)
                .configure();

        PowertoolsIdempotencyMultiArgFunction function = new PowertoolsIdempotencyMultiArgFunction();

        // GIVEN
        int threadCount = 10;
        Thread[] threads = new Thread[threadCount];
        Context[] capturedContexts = new Context[threadCount];
        int[] capturedRemainingTimes = new int[threadCount];
        boolean[] success = new boolean[threadCount];

        // WHEN - Multiple threads call handleRequest with different contexts
        for (int i = 0; i < threadCount; i++) {
            final int threadIndex = i;
            final int expectedTime = (i + 1) * 2000; // 2000, 4000, 6000, ..., 20000

            final Context threadContext = new TestLambdaContext() {
                @Override
                public int getRemainingTimeInMillis() {
                    return expectedTime;
                }
            };

            threads[i] = new Thread(() -> {
                try {
                    Product p = new Product(threadIndex, "product" + threadIndex, 10);
                    function.handleRequest(p, threadContext);

                    // Capture the context that was actually stored in ThreadLocal by this thread
                    Context captured = Idempotency.getInstance().getConfig().getLambdaContext();
                    capturedContexts[threadIndex] = captured;
                    capturedRemainingTimes[threadIndex] = captured != null ? captured.getRemainingTimeInMillis() : -1;
                    success[threadIndex] = true;
                } catch (Exception e) {
                    success[threadIndex] = false;
                }
            });
        }

        // Start all threads
        for (Thread thread : threads) {
            thread.start();
        }

        // Wait for all threads to complete
        for (Thread thread : threads) {
            thread.join();
        }

        // THEN - All threads should complete successfully
        for (boolean result : success) {
            assertThat(result).isTrue();
        }

        // THEN - Each thread should have captured its own context (no leakage)
        for (int i = 0; i < threadCount; i++) {
            int expectedTime = (i + 1) * 2000;
            assertThat(capturedRemainingTimes[i])
                    .as("Thread %d should have remaining time %d", i, expectedTime)
                    .isEqualTo(expectedTime);
            assertThat(capturedContexts[i]).as("Thread %d should have non-null context", i).isNotNull();
        }
    }
}

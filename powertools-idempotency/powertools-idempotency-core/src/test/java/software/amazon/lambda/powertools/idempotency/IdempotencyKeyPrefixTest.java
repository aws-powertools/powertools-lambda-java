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
import static software.amazon.lambda.powertools.common.internal.LambdaConstants.LAMBDA_FUNCTION_NAME_ENV;

import java.util.Collections;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Supplier;

import org.junit.jupiter.api.Test;
import org.junitpioneer.jupiter.SetEnvironmentVariable;

import com.fasterxml.jackson.core.type.TypeReference;

import software.amazon.lambda.powertools.common.stubs.TestLambdaContext;
import software.amazon.lambda.powertools.idempotency.internal.IdempotencyHandler;
import software.amazon.lambda.powertools.idempotency.model.Product;
import software.amazon.lambda.powertools.idempotency.persistence.DataRecord;
import software.amazon.lambda.powertools.idempotency.testutils.InMemoryPersistenceStore;
import software.amazon.lambda.powertools.utilities.JsonConfig;

@SetEnvironmentVariable(key = LAMBDA_FUNCTION_NAME_ENV, value = "orders")
class IdempotencyKeyPrefixTest {
    private static final String KEY = "Lambda rocks";
    private static final String HASH = "70c24d88041893f7fbab4105b76fd9e1";
    private static final TypeReference<Map<String, Product>> PRODUCTS = new TypeReference<Map<String, Product>>() {
    };

    @Test
    void legacyHandlerConstructors_preserveDefaultKeysAndReplayResponses() throws Throwable {
        InMemoryPersistenceStore store = configureStore();
        AtomicInteger executions = new AtomicInteger();
        assertThat(new IdempotencyHandler(() -> "result-" + executions.incrementAndGet(), String.class,
                "legacy", JsonConfig.get().getObjectMapper().valueToTree(KEY), new TestLambdaContext()).handle())
                .isEqualTo("result-1");
        assertThat(new IdempotencyHandler(() -> "result-" + executions.incrementAndGet(), new TypeReference<String>() {
        }, "legacy", JsonConfig.get().getObjectMapper().valueToTree(KEY), new TestLambdaContext()).handle())
                .isEqualTo("result-1");
        assertThat(executions).hasValue(1);
        assertThat(store.getRecord("orders.legacy#" + HASH).getResponseData()).isEqualTo("result-1");
    }

    @Test
    void annotationAndFunctionalApi_shareCustomPrefixCacheWithoutLeakingIntoDefaultNamespace() {
        InMemoryPersistenceStore store = new InMemoryPersistenceStore();
        Idempotency.config().withPersistenceStore(store)
                .withConfig(IdempotencyConfig.builder().withUseLocalCache(true).build()).configure();
        Idempotency.registerLambdaContext(new TestLambdaContext());
        SharedPrefixFunction annotated = new SharedPrefixFunction();
        AtomicInteger functionalExecutions = new AtomicInteger();
        Supplier<Product> function = () -> new Product(functionalExecutions.incrementAndGet(), "functional", 12);

        assertThat(annotated.process(KEY).getName()).isEqualTo("annotated");
        assertThat(store.getRecord("payments#" + HASH).getStatus()).isEqualTo(DataRecord.Status.COMPLETED);
        // Remove only the backing record: the next entry point must reuse the local cache.
        store.deleteRecord("payments#" + HASH);
        assertThat(Idempotency.makeIdempotent("renamed", KEY, function, Product.class, "payments").getName())
                .isEqualTo("annotated");
        assertThat(functionalExecutions).hasValue(0);

        assertThat(Idempotency.makeIdempotent("renamed", KEY, function, Product.class, "").getName())
                .isEqualTo("functional");
        assertThat(Idempotency.makeIdempotent("renamed", KEY, function, Product.class).getName())
                .isEqualTo("functional");
        assertThat(functionalExecutions).hasValue(1);
        assertThat(annotated.executions).isEqualTo(1);
        assertThat(store.getRecord("orders.renamed#" + HASH).getStatus()).isEqualTo(DataRecord.Status.COMPLETED);
    }

    public static class SharedPrefixFunction {
        private int executions;

        @Idempotent(keyPrefix = "payments")
        public Product process(String input) {
            return new Product(++executions, "annotated", 12);
        }
    }

    @Test
    void allFunctionalOverloads_useCustomPrefixAndReturnPersistedResponse() {
        for (int overload = 0; overload < 6; overload++) {
            assertRepeatedInvocation(overload, "payments", "payments");
        }
    }

    @Test
    void allFunctionalOverloads_emptyPrefixPreservesDefaultNamespace() {
        for (int overload = 0; overload < 6; overload++) {
            assertRepeatedInvocation(overload, "", overload == 1 || overload == 4 ? "orders.process" : "orders.function");
        }
    }

    @Test
    void allFunctionalOverloads_nullPrefixPreservesDefaultNamespace() {
        for (int overload = 0; overload < 6; overload++) {
            assertRepeatedInvocation(overload, null, overload == 1 || overload == 4 ? "orders.process" : "orders.function");
        }
    }

    @Test
    void customPrefixSharesRecordsAcrossRenamedFunctionsAndSeparatesOtherPrefixes() {
        InMemoryPersistenceStore store = configureStore();
        AtomicInteger executions = new AtomicInteger();
        Supplier<String> function = () -> "result-" + executions.incrementAndGet();

        assertThat(Idempotency.makeIdempotent("original", KEY, function, String.class, "payments"))
                .isEqualTo("result-1");
        assertThat(Idempotency.makeIdempotent("renamed", KEY, function, String.class, "payments"))
                .isEqualTo("result-1");
        assertThat(Idempotency.makeIdempotent("original", KEY, function, String.class, "refunds"))
                .isEqualTo("result-2");
        assertThat(Idempotency.makeIdempotent("original", KEY, function, String.class))
                .isEqualTo("result-3");
        assertThat(executions).hasValue(3);
        assertThat(store.getRecord("payments#" + HASH).getResponseData()).isEqualTo("result-1");
        assertThat(store.getRecord("refunds#" + HASH).getResponseData()).isEqualTo("result-2");
        assertThat(store.getRecord("orders.original#" + HASH).getResponseData()).isEqualTo("result-3");
    }

    @Test
    void legacyPersistenceOverridePreservesPayloadSelectionForAllPrefixes() {
        for (String prefix : new String[]{null, "", "payments"}) {
            InMemoryPersistenceStore store = new InMemoryPersistenceStore() {
                @Override
                public void configure(IdempotencyConfig config, String functionName) {
                    super.configure(IdempotencyConfig.builder().withEventKeyJMESPath("id").build(), functionName);
                }
            };
            Idempotency.config().withPersistenceStore(store).configure();
            Idempotency.registerLambdaContext(new TestLambdaContext());
            AtomicInteger executions = new AtomicInteger();
            Supplier<String> function = () -> "result-" + executions.incrementAndGet();
            Map<String, String> first = Map.of("id", "same-order", "body", "first");
            Map<String, String> duplicate = Map.of("id", "same-order", "body", "second");

            String initial = prefix == null
                    ? Idempotency.makeIdempotent("process", first, function, String.class)
                    : Idempotency.makeIdempotent("process", first, function, String.class, prefix);
            String replay = prefix == null
                    ? Idempotency.makeIdempotent("process", duplicate, function, String.class)
                    : Idempotency.makeIdempotent("process", duplicate, function, String.class, prefix);

            assertThat(initial).isEqualTo("result-1");
            assertThat(replay).as("legacy persistence payload selection with prefix %s", prefix).isEqualTo(initial);
            assertThat(executions).hasValue(1);
        }
    }


    private void assertRepeatedInvocation(int overload, String prefix, String expectedPrefix) {
        InMemoryPersistenceStore store = configureStore();
        AtomicInteger executions = new AtomicInteger();
        Supplier<Product> function = () -> new Product(executions.incrementAndGet(), "product", 12);

        Product first = invoke(overload, prefix, function);
        Product duplicate = invoke(overload, prefix, function);

        assertThat(first.getId()).isEqualTo(1);
        assertThat(duplicate).isEqualTo(first);
        assertThat(executions).hasValue(1);
        DataRecord record = store.getRecord(expectedPrefix + "#" + HASH);
        assertThat(record.getStatus()).isEqualTo(DataRecord.Status.COMPLETED);
        assertThat(record.getIdempotencyKey()).isEqualTo(expectedPrefix + "#" + HASH);
    }

    private Product invoke(int overload, String prefix, Supplier<Product> function) {
        Supplier<Map<String, Product>> products = () -> Collections.singletonMap("product", function.get());
        switch (overload) {
            case 0:
                return Idempotency.makeIdempotent(KEY, function, Product.class, prefix);
            case 1:
                return Idempotency.makeIdempotent("process", KEY, function, Product.class, prefix);
            case 2:
                return Idempotency.makeIdempotent(key -> function.get(), KEY, Product.class, prefix);
            case 3:
                return Idempotency.makeIdempotent(KEY, products, PRODUCTS, prefix).get("product");
            case 4:
                return Idempotency.makeIdempotent("process", KEY, products, PRODUCTS, prefix).get("product");
            case 5:
                return Idempotency.makeIdempotent(key -> products.get(), KEY, PRODUCTS, prefix).get("product");
            default:
                throw new IllegalArgumentException("Unknown overload: " + overload);
        }
    }

    private InMemoryPersistenceStore configureStore() {
        InMemoryPersistenceStore store = new InMemoryPersistenceStore();
        Idempotency.config().withPersistenceStore(store).configure();
        Idempotency.registerLambdaContext(new TestLambdaContext());
        return store;
    }
}

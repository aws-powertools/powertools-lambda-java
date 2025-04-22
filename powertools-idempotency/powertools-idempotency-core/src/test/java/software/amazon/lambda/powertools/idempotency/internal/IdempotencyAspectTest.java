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

package software.amazon.lambda.powertools.idempotency.internal;

import static java.time.temporal.ChronoUnit.SECONDS;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.time.Instant;
import java.util.OptionalInt;
import java.util.OptionalLong;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junitpioneer.jupiter.SetEnvironmentVariable;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import com.amazonaws.services.lambda.runtime.Context;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;

import software.amazon.lambda.powertools.idempotency.Constants;
import software.amazon.lambda.powertools.idempotency.Idempotency;
import software.amazon.lambda.powertools.idempotency.IdempotencyConfig;
import software.amazon.lambda.powertools.idempotency.exceptions.IdempotencyAlreadyInProgressException;
import software.amazon.lambda.powertools.idempotency.exceptions.IdempotencyConfigurationException;
import software.amazon.lambda.powertools.idempotency.exceptions.IdempotencyInconsistentStateException;
import software.amazon.lambda.powertools.idempotency.exceptions.IdempotencyItemAlreadyExistsException;
import software.amazon.lambda.powertools.idempotency.handlers.IdempotencyEnabledFunction;
import software.amazon.lambda.powertools.idempotency.handlers.IdempotencyInternalFunction;
import software.amazon.lambda.powertools.idempotency.handlers.IdempotencyInternalFunctionInternalKey;
import software.amazon.lambda.powertools.idempotency.handlers.IdempotencyInternalFunctionInvalid;
import software.amazon.lambda.powertools.idempotency.handlers.IdempotencyInternalFunctionVoid;
import software.amazon.lambda.powertools.idempotency.handlers.IdempotencyStringFunction;
import software.amazon.lambda.powertools.idempotency.handlers.IdempotencyWithErrorFunction;
import software.amazon.lambda.powertools.idempotency.model.Basket;
import software.amazon.lambda.powertools.idempotency.model.Product;
import software.amazon.lambda.powertools.idempotency.persistence.BasePersistenceStore;
import software.amazon.lambda.powertools.idempotency.persistence.DataRecord;
import software.amazon.lambda.powertools.utilities.JsonConfig;

public class IdempotencyAspectTest {

    @Mock
    private Context context;

    @Mock
    private BasePersistenceStore store;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
    }

    @Test
    public void firstCall_shouldPutInStore() {
        Idempotency.config()
                .withPersistenceStore(store)
                .withConfig(IdempotencyConfig.builder()
                        .withEventKeyJMESPath("id")
                        .build())
                .configure();

        IdempotencyEnabledFunction function = new IdempotencyEnabledFunction();

        when(context.getRemainingTimeInMillis()).thenReturn(30000);

        Product p = new Product(42, "fake product", 12);
        Basket basket = function.handleRequest(p, context);
        assertThat(basket.getProducts()).hasSize(1);
        assertThat(function.handlerCalled()).isTrue();

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
    public void firstCall_shouldPutInStoreAndNotApplyResponseHook() {
        Idempotency.config()
                .withPersistenceStore(store)
                .withConfig(IdempotencyConfig.builder()
                        .withEventKeyJMESPath("id")
                        // This hook will add a second product to the basket. It should not run here. Only for
                        // idempotent responses.
                        .withResponseHook((responseData, dataRecord) -> {
                            final Basket basket = (Basket) responseData;
                            basket.add(new Product(42, "fake product 2", 12));
                            return basket;
                        })
                        .build())
                .configure();

        IdempotencyEnabledFunction function = new IdempotencyEnabledFunction();

        when(context.getRemainingTimeInMillis()).thenReturn(30000);

        Product p = new Product(42, "fake product", 12);
        Basket basket = function.handleRequest(p, context);
        assertThat(basket.getProducts()).hasSize(1); // Size should be 1 because response hook should not run
        assertThat(function.handlerCalled()).isTrue();

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
    public void secondCall_notExpired_shouldGetFromStore() throws JsonProcessingException {
        // GIVEN
        Idempotency.config()
                .withPersistenceStore(store)
                .withConfig(IdempotencyConfig.builder()
                        .withEventKeyJMESPath("id")
                        .build())
                .configure();

        doThrow(new IdempotencyItemAlreadyExistsException()).when(store).saveInProgress(any(), any(), any());

        Product p = new Product(42, "fake product", 12);
        Basket b = new Basket(p);
        DataRecord dr = new DataRecord(
                "42",
                DataRecord.Status.COMPLETED,
                Instant.now().plus(356, SECONDS).getEpochSecond(),
                JsonConfig.get().getObjectMapper().writer().writeValueAsString(b),
                null);
        doReturn(dr).when(store).getRecord(any(), any());

        // WHEN
        IdempotencyEnabledFunction function = new IdempotencyEnabledFunction();
        Basket basket = function.handleRequest(p, context);

        // THEN
        assertThat(basket).isEqualTo(b);
        assertThat(function.handlerCalled()).isFalse();
    }

    @Test
    public void secondCall_notExpired_shouldNotGetFromStoreIfPresentOnIdempotencyException()
            throws JsonProcessingException {
        // GIVEN
        Idempotency.config()
                .withPersistenceStore(store)
                .withConfig(IdempotencyConfig.builder()
                        .withEventKeyJMESPath("id")
                        .build())
                .configure();

        Product p = new Product(42, "fake product", 12);
        Basket b = new Basket(p);
        DataRecord dr = new DataRecord(
                "42",
                DataRecord.Status.COMPLETED,
                Instant.now().plus(356, SECONDS).getEpochSecond(),
                JsonConfig.get().getObjectMapper().writer().writeValueAsString(b),
                null);

        // A data record on this exception should take precedence over fetching a record from the store / cache
        doThrow(new IdempotencyItemAlreadyExistsException(
                "Test message",
                new RuntimeException("Test Cause"),
                dr))
                        .when(store).saveInProgress(any(), any(), any());

        // WHEN
        IdempotencyEnabledFunction function = new IdempotencyEnabledFunction();
        Basket basket = function.handleRequest(p, context);

        // THEN
        assertThat(basket).isEqualTo(b);
        assertThat(function.handlerCalled()).isFalse();
        // Should never call the store because item is already present on IdempotencyItemAlreadyExistsException
        verify(store, never()).getRecord(any(), any());
    }

    @Test
    public void secondCall_notExpired_shouldGetStringFromStore() {
        // GIVEN
        Idempotency.config()
                .withPersistenceStore(store)
                .withConfig(IdempotencyConfig.builder()
                        .withEventKeyJMESPath("id")
                        .build())
                .configure();

        doThrow(new IdempotencyItemAlreadyExistsException()).when(store).saveInProgress(any(), any(), any());

        Product p = new Product(42, "fake product", 12);
        DataRecord dr = new DataRecord(
                "42",
                DataRecord.Status.COMPLETED,
                Instant.now().plus(356, SECONDS).getEpochSecond(),
                p.getName(),
                null);
        doReturn(dr).when(store).getRecord(any(), any());

        // WHEN
        IdempotencyStringFunction function = new IdempotencyStringFunction();
        String name = function.handleRequest(p, context);

        // THEN
        assertThat(name).isEqualTo(p.getName());
        assertThat(function.handlerCalled()).isFalse();
    }

    @Test
    public void secondCall_notExpired_shouldGetStringFromStoreWithResponseHook() {
        // GIVEN
        final String RESPONSE_HOOK_SUFFIX = " ResponseHook";
        Idempotency.config()
                .withPersistenceStore(store)
                .withConfig(IdempotencyConfig.builder()
                        .withEventKeyJMESPath("id")
                        .withResponseHook((responseData, dataRecord) -> {
                            responseData += RESPONSE_HOOK_SUFFIX;
                            return responseData;
                        })
                        .build())
                .configure();

        doThrow(new IdempotencyItemAlreadyExistsException()).when(store).saveInProgress(any(), any(), any());

        Product p = new Product(42, "fake product", 12);
        DataRecord dr = new DataRecord(
                "42",
                DataRecord.Status.COMPLETED,
                Instant.now().plus(356, SECONDS).getEpochSecond(),
                p.getName(),
                null);
        doReturn(dr).when(store).getRecord(any(), any());

        // WHEN
        IdempotencyStringFunction function = new IdempotencyStringFunction();
        String name = function.handleRequest(p, context);

        // THEN
        assertThat(name).isEqualTo(p.getName() + RESPONSE_HOOK_SUFFIX);
        assertThat(function.handlerCalled()).isFalse();
    }

    @Test
    public void secondCall_inProgress_shouldThrowIdempotencyAlreadyInProgressException()
            throws JsonProcessingException {
        // GIVEN
        Idempotency.config()
                .withPersistenceStore(store)
                .withConfig(IdempotencyConfig.builder()
                        .withEventKeyJMESPath("id")
                        .build())
                .configure();

        doThrow(new IdempotencyItemAlreadyExistsException()).when(store).saveInProgress(any(), any(), any());

        Product p = new Product(42, "fake product", 12);
        Basket b = new Basket(p);
        OptionalLong timestampInFuture = OptionalLong.of(Instant.now().toEpochMilli() + 1000); // timeout not expired
                                                                                               // (in 1sec)
        DataRecord dr = new DataRecord(
                "42",
                DataRecord.Status.INPROGRESS,
                Instant.now().plus(356, SECONDS).getEpochSecond(),
                JsonConfig.get().getObjectMapper().writer().writeValueAsString(b),
                null,
                timestampInFuture);
        doReturn(dr).when(store).getRecord(any(), any());

        // THEN
        IdempotencyEnabledFunction function = new IdempotencyEnabledFunction();
        assertThatThrownBy(() -> function.handleRequest(p, context)).isInstanceOf(
                IdempotencyAlreadyInProgressException.class);
    }

    @Test
    public void secondCall_inProgress_lambdaTimeout_timeoutExpired_shouldThrowInconsistentState()
            throws JsonProcessingException {
        // GIVEN
        Idempotency.config()
                .withPersistenceStore(store)
                .withConfig(IdempotencyConfig.builder()
                        .withEventKeyJMESPath("id")
                        .build())
                .configure();

        doThrow(new IdempotencyItemAlreadyExistsException()).when(store).saveInProgress(any(), any(), any());

        Product p = new Product(42, "fake product", 12);
        Basket b = new Basket(p);
        OptionalLong timestampInThePast = OptionalLong.of(Instant.now().toEpochMilli() - 100); // timeout expired 100ms
                                                                                               // ago
        DataRecord dr = new DataRecord(
                "42",
                DataRecord.Status.INPROGRESS,
                Instant.now().plus(356, SECONDS).getEpochSecond(),
                JsonConfig.get().getObjectMapper().writer().writeValueAsString(b),
                null,
                timestampInThePast);
        doReturn(dr).when(store).getRecord(any(), any());

        // THEN
        IdempotencyEnabledFunction function = new IdempotencyEnabledFunction();
        assertThatThrownBy(() -> function.handleRequest(p, context)).isInstanceOf(
                IdempotencyInconsistentStateException.class);
    }

    @Test
    public void functionThrowException_shouldDeleteRecord_andThrowFunctionException() {
        // GIVEN
        Idempotency.config()
                .withPersistenceStore(store)
                .withConfig(IdempotencyConfig.builder()
                        .withEventKeyJMESPath("id")
                        .build())
                .configure();

        // WHEN / THEN
        IdempotencyWithErrorFunction function = new IdempotencyWithErrorFunction();

        Product p = new Product(42, "fake product", 12);
        assertThatThrownBy(() -> function.handleRequest(p, context))
                .isInstanceOf(IndexOutOfBoundsException.class);

        verify(store).deleteRecord(any(), any(IndexOutOfBoundsException.class));
    }

    @Test
    @SetEnvironmentVariable(key = Constants.IDEMPOTENCY_DISABLED_ENV, value = "true")
    public void testIdempotencyDisabled_shouldJustRunTheFunction() {
        // GIVEN
        Idempotency.config()
                .withPersistenceStore(store)
                .withConfig(IdempotencyConfig.builder()
                        .withEventKeyJMESPath("id")
                        .build())
                .configure();

        // WHEN
        IdempotencyEnabledFunction function = new IdempotencyEnabledFunction();
        Product p = new Product(42, "fake product", 12);
        Basket basket = function.handleRequest(p, context);

        // THEN
        verifyNoInteractions(store);
        assertThat(basket.getProducts()).hasSize(1);
        assertThat(function.handlerCalled()).isTrue();
    }

    @Test
    public void idempotencyOnSubMethodAnnotated_firstCall_shouldPutInStore() {
        Idempotency.config()
                .withPersistenceStore(store)
                .configure();

        // WHEN
        boolean registerContext = true;
        when(context.getRemainingTimeInMillis()).thenReturn(30000);

        IdempotencyInternalFunction function = new IdempotencyInternalFunction(registerContext);
        Product p = new Product(42, "fake product", 12);
        Basket basket = function.handleRequest(p, context);

        // THEN
        assertThat(basket.getProducts()).hasSize(2);
        assertThat(function.subMethodCalled()).isTrue();

        ArgumentCaptor<JsonNode> nodeCaptor = ArgumentCaptor.forClass(JsonNode.class);
        ArgumentCaptor<OptionalInt> expiryCaptor = ArgumentCaptor.forClass(OptionalInt.class);
        verify(store).saveInProgress(nodeCaptor.capture(), any(), expiryCaptor.capture());
        assertThat(nodeCaptor.getValue().asText()).isEqualTo("fake");
        assertThat(expiryCaptor.getValue().orElse(-1)).isEqualTo(30000);

        ArgumentCaptor<Basket> resultCaptor = ArgumentCaptor.forClass(Basket.class);
        verify(store).saveSuccess(any(), resultCaptor.capture(), any());
        assertThat(resultCaptor.getValue().getProducts()).contains(basket.getProducts().get(0),
                new Product(0, "fake", 0));
    }

    @Test
    public void idempotencyOnSubMethodAnnotated_firstCall_contextNotRegistered_shouldPutInStore() {
        Idempotency.config()
                .withPersistenceStore(store)
                .configure();

        // WHEN
        boolean registerContext = false;
        IdempotencyInternalFunction function = new IdempotencyInternalFunction(registerContext);
        Product p = new Product(42, "fake product", 12);
        Basket basket = function.handleRequest(p, context);

        // THEN
        assertThat(basket.getProducts()).hasSize(2);
        assertThat(function.subMethodCalled()).isTrue();

        ArgumentCaptor<JsonNode> nodeCaptor = ArgumentCaptor.forClass(JsonNode.class);
        ArgumentCaptor<OptionalInt> expiryCaptor = ArgumentCaptor.forClass(OptionalInt.class);
        verify(store).saveInProgress(nodeCaptor.capture(), any(), expiryCaptor.capture());
        assertThat(nodeCaptor.getValue().asText()).isEqualTo("fake");
        assertThat(expiryCaptor.getValue()).isEmpty();

        ArgumentCaptor<Basket> resultCaptor = ArgumentCaptor.forClass(Basket.class);
        verify(store).saveSuccess(any(), resultCaptor.capture(), any());
        assertThat(resultCaptor.getValue().getProducts()).contains(basket.getProducts().get(0),
                new Product(0, "fake", 0));
    }

    @Test
    public void idempotencyOnSubMethodAnnotated_secondCall_notExpired_shouldGetFromStore()
            throws JsonProcessingException {
        // GIVEN
        Idempotency.config()
                .withPersistenceStore(store)
                .configure();

        doThrow(new IdempotencyItemAlreadyExistsException()).when(store).saveInProgress(any(), any(), any());

        Product p = new Product(42, "fake product", 12);
        Basket b = new Basket(p);
        DataRecord dr = new DataRecord(
                "fake",
                DataRecord.Status.COMPLETED,
                Instant.now().plus(356, SECONDS).getEpochSecond(),
                JsonConfig.get().getObjectMapper().writer().writeValueAsString(b),
                null);
        doReturn(dr).when(store).getRecord(any(), any());

        // WHEN
        IdempotencyInternalFunction function = new IdempotencyInternalFunction(false);
        Basket basket = function.handleRequest(p, context);

        // THEN
        assertThat(basket).isEqualTo(b);
        assertThat(function.subMethodCalled()).isFalse();
    }

    @Test
    public void idempotencyOnSubMethodAnnotated_keyJMESPath_shouldPutInStoreWithKey() {
        BasePersistenceStore persistenceStore = spy(BasePersistenceStore.class);

        Idempotency.config()
                .withPersistenceStore(persistenceStore)
                .withConfig(IdempotencyConfig.builder().withEventKeyJMESPath("id").build())
                .configure();

        // WHEN
        IdempotencyInternalFunctionInternalKey function = new IdempotencyInternalFunctionInternalKey();
        Product p = new Product(42, "fake product", 12);
        function.handleRequest(p, context);

        // THEN
        ArgumentCaptor<DataRecord> recordCaptor = ArgumentCaptor.forClass(DataRecord.class);
        verify(persistenceStore).putRecord(recordCaptor.capture(), any());
        // a1d0c6e83f027327d8461063f4ac58a6 = MD5(42)
        assertThat(recordCaptor.getValue().getIdempotencyKey()).isEqualTo(
                "testFunction.createBasket#a1d0c6e83f027327d8461063f4ac58a6");
    }

    @Test
    public void idempotencyOnSubMethodAnnotated_keyJMESPathArray_shouldPutInStoreWithKey() {
        BasePersistenceStore persistenceStore = spy(BasePersistenceStore.class);

        Idempotency.config()
                .withPersistenceStore(persistenceStore)
                .withConfig(IdempotencyConfig.builder().withEventKeyJMESPath("[id,name]").build())
                .configure();

        // WHEN
        IdempotencyInternalFunctionInternalKey function = new IdempotencyInternalFunctionInternalKey();
        Product p = new Product(42, "fake product", 12);
        function.handleRequest(p, context);

        // THEN
        ArgumentCaptor<DataRecord> recordCaptor = ArgumentCaptor.forClass(DataRecord.class);
        verify(persistenceStore).putRecord(recordCaptor.capture(), any());
        // eec7cd392d9e3bb20deb2c9676697c3c = MD5([42,"fake product"])
        assertThat(recordCaptor.getValue().getIdempotencyKey()).isEqualTo(
                "testFunction.createBasket#eec7cd392d9e3bb20deb2c9676697c3c");
    }

    @Test
    public void idempotencyOnSubMethodNotAnnotated_shouldThrowException() {
        Idempotency.config()
                .withPersistenceStore(store)
                .withConfig(IdempotencyConfig.builder().build()).configure();

        // WHEN
        IdempotencyInternalFunctionInvalid function = new IdempotencyInternalFunctionInvalid();
        Product p = new Product(42, "fake product", 12);

        // THEN
        assertThatThrownBy(() -> function.handleRequest(p, context)).isInstanceOf(
                IdempotencyConfigurationException.class);
    }

    @Test
    public void idempotencyOnSubMethodVoid_shouldThrowException() {
        Idempotency.config()
                .withPersistenceStore(store)
                .withConfig(IdempotencyConfig.builder().build()).configure();

        // WHEN
        IdempotencyInternalFunctionVoid function = new IdempotencyInternalFunctionVoid();
        Product p = new Product(42, "fake product", 12);

        // THEN
        assertThatThrownBy(() -> function.handleRequest(p, context)).isInstanceOf(
                IdempotencyConfigurationException.class);
    }

}

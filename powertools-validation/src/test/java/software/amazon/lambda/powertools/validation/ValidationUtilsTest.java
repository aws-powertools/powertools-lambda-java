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

package software.amazon.lambda.powertools.validation;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.assertj.core.api.Assertions.assertThatNoException;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static software.amazon.lambda.powertools.validation.ValidationUtils.getJsonSchema;
import static software.amazon.lambda.powertools.validation.ValidationUtils.validate;

import com.fasterxml.jackson.databind.JsonNode;
import com.networknt.schema.Schema;
import com.networknt.schema.SpecificationVersion;
import com.sun.net.httpserver.HttpServer;
import java.io.IOException;
import java.io.OutputStream;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import software.amazon.lambda.powertools.validation.model.Basket;
import software.amazon.lambda.powertools.validation.model.MyCustomEvent;
import software.amazon.lambda.powertools.validation.model.Product;

public class ValidationUtilsTest {

    private String schemaString = "classpath:/schema_v7.json";
    private Schema schema = getJsonSchema(schemaString);

    @BeforeEach
    public void setup() {
        ValidationConfig.get().setSchemaVersion(SpecificationVersion.DRAFT_7);
    }

    @Test
    public void testLoadSchemaV7OK() {
        ValidationConfig.get().setSchemaVersion(SpecificationVersion.DRAFT_7);
        Schema jsonSchema = getJsonSchema("classpath:/schema_v7.json", true);
        assertThat(jsonSchema).isNotNull();
        assertThat(jsonSchema.getId()).isEqualTo("http://example.com/product.json");
    }

    @Test
    public void testLoadSchemaV7KO() {
        ValidationConfig.get().setSchemaVersion(SpecificationVersion.DRAFT_7);
        assertThatThrownBy(() -> getJsonSchema("classpath:/schema_v7_ko.json", true))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage(
                        "The schema classpath:/schema_v7_ko.json is not valid, it does not respect the specification /draft-07/schema#");
    }

    @Test
    public void testLoadMetaSchema_NoValidation() {
        ValidationConfig.get().setSchemaVersion(SpecificationVersion.DRAFT_7);

        assertThatNoException().isThrownBy(() ->
        {
            getJsonSchema("classpath:/schema_v7_ko.json", false);
        });
    }

    @Test
    public void testLoadMetaSchemaV2019() {
        ValidationConfig.get().setSchemaVersion(SpecificationVersion.DRAFT_2019_09);
        Schema jsonSchema = getJsonSchema("classpath:/draft/2019-09/schema", true);
        assertThat(jsonSchema).isNotNull();
    }

    @Test
    public void testLoadMetaSchemaV2020() {
        ValidationConfig.get().setSchemaVersion(SpecificationVersion.DRAFT_2020_12);
        Schema jsonSchema = getJsonSchema("classpath:/draft/2020-12/schema", true);
        assertThat(jsonSchema).isNotNull();
    }

    @Test
    public void testLoadMetaSchemaV7() {
        ValidationConfig.get().setSchemaVersion(SpecificationVersion.DRAFT_7);
        Schema jsonSchema = getJsonSchema("classpath:/draft-07/schema", true);
        assertThat(jsonSchema).isNotNull();
    }

    @Test
    public void testLoadMetaSchemaV6() {
        ValidationConfig.get().setSchemaVersion(SpecificationVersion.DRAFT_6);
        Schema jsonSchema = getJsonSchema("classpath:/draft-06/schema", true);
        assertThat(jsonSchema).isNotNull();
    }

    @Test
    public void testLoadMetaSchemaV4() {
        ValidationConfig.get().setSchemaVersion(SpecificationVersion.DRAFT_4);
        Schema jsonSchema = getJsonSchema("classpath:/draft-04/schema", true);
        assertThat(jsonSchema).isNotNull();
    }

    @Test
    public void testLoadSchemaV4OK() {
        ValidationConfig.get().setSchemaVersion(SpecificationVersion.DRAFT_4);
        Schema jsonSchema = getJsonSchema("classpath:/schema_v4.json", true);
        assertThat(jsonSchema).isNotNull();
    }

    @Test
    public void testLoadSchemaNotFound() {
        assertThatThrownBy(() -> getJsonSchema("classpath:/dev/null"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("'classpath:/dev/null' is invalid, verify '/dev/null' is in your classpath");
    }

    @Test
    public void testValidateJsonNodeOK() throws IOException {
        JsonNode node =
                ValidationConfig.get().getObjectMapper().readTree(this.getClass().getResourceAsStream("/json_ok.json"));

        assertThatNoException().isThrownBy(() ->
        {
            validate(node, schemaString);
        });
    }

    @Test
    public void testValidateJsonNodeKO() throws IOException {
        JsonNode node =
                ValidationConfig.get().getObjectMapper().readTree(this.getClass().getResourceAsStream("/json_ko.json"));

        assertThatExceptionOfType(ValidationException.class).isThrownBy(() -> validate(node, schema));
    }

    @Test
    public void testValidateMapOK() {

        Map<String, Object> map = new HashMap<>();
        map.put("id", 43242);
        map.put("name", "FooBar XY");
        map.put("price", 258);

        assertThatNoException().isThrownBy(() ->
        {
            validate(map, schemaString);
        });
    }

    @Test
    public void testValidateMapKO() {
        Map<String, Object> map = new HashMap<>();
        map.put("id", 43242);
        map.put("name", "FooBar XY");

        assertThatExceptionOfType(ValidationException.class).isThrownBy(() -> validate(map, schema));
    }

    @Test
    public void testValidateMapNotValidJsonObject() {
        Map<String, Object> map = new HashMap<>();
        map.put("1234", new Object());

        assertThatExceptionOfType(ValidationException.class).isThrownBy(() -> validate(map, schema));
    }

    @Test
    public void testValidateStringOK() {
        String json = "{\n  \"id\": 43242,\n  \"name\": \"FooBar XY\",\n  \"price\": 258\n}";

        assertThatNoException().isThrownBy(() ->
        {
            validate(json, schemaString);
        });
    }

    @Test
    public void testValidateStringKO() {
        String json = "{\n  \"id\": 43242,\n  \"name\": \"FooBar XY\",\n  \"price\": 0\n}";

        assertThatExceptionOfType(ValidationException.class).isThrownBy(() -> validate(json, schema));
    }

    @Test
    public void testValidateStringKO_shouldReturnValidationErrorsAsJson() {
        String json = "{\n  \"id\": 43242,\n  \"name\": \"FooBar XY\",\n  \"price\": 0\n}";

        assertThatExceptionOfType(ValidationException.class)
                .isThrownBy(() -> validate(json, schema))
                .withMessage("{\"validationErrors\":[{\"keyword\":\"exclusiveMinimum\",\"instanceLocation\":\"/price\","
                        + "\"message\":\"must have an exclusive minimum value of 0\","
                        + "\"evaluationPath\":\"/properties/price/exclusiveMinimum\","
                        + "\"schemaLocation\":\"http://example.com/product.json#/properties/price/exclusiveMinimum\","
                        + "\"messageKey\":\"exclusiveMinimum\",\"arguments\":[\"0\"]}]}");
    }

    @Test
    public void testValidateFormat_draft7_shouldAssertFormat() {
        Schema emailSchema = getJsonSchema("{\"type\":\"string\",\"format\":\"email\"}");

        assertThatNoException().isThrownBy(() -> validate("\"john@example.com\"", emailSchema));
        assertThatExceptionOfType(ValidationException.class)
                .isThrownBy(() -> validate("\"not-an-email\"", emailSchema));
    }

    @Test
    public void testValidateFormat_draft2020_12_shouldAssertFormat() {
        ValidationConfig.get().setSchemaVersion(SpecificationVersion.DRAFT_2020_12);
        Schema dateSchema = getJsonSchema("{\"$id\":\"urn:test:format-2020-12\","
                + "\"type\":\"string\",\"format\":\"date\"}");

        assertThatNoException().isThrownBy(() -> validate("\"2026-10-07\"", dateSchema));
        assertThatExceptionOfType(ValidationException.class)
                .isThrownBy(() -> validate("\"not-a-date\"", dateSchema));
    }

    @Test
    public void testValidateRemoteRef_shouldFetchRemoteSchema() throws IOException {
        HttpServer server = HttpServer.create(new InetSocketAddress(InetAddress.getLoopbackAddress(), 0), 0);
        byte[] remoteSchema = "{\"type\":\"integer\",\"minimum\":10}".getBytes(StandardCharsets.UTF_8);
        server.createContext("/remote.json", exchange -> {
            exchange.getResponseHeaders().add("Content-Type", "application/json");
            exchange.sendResponseHeaders(200, remoteSchema.length);
            try (OutputStream body = exchange.getResponseBody()) {
                body.write(remoteSchema);
            }
        });
        server.start();
        try {
            String remoteUri = "http://localhost:" + server.getAddress().getPort() + "/remote.json";
            Schema refSchema = getJsonSchema("{\"$ref\":\"" + remoteUri + "\"}");

            assertThatNoException().isThrownBy(() -> validate("42", refSchema));
            assertThatExceptionOfType(ValidationException.class).isThrownBy(() -> validate("5", refSchema));
        } finally {
            server.stop(0);
        }
    }

    @Test
    public void testValidateObjectOK() {
        Product product = new Product(42, "FooBar", 42);

        assertThatNoException().isThrownBy(() ->
        {
            validate(product, schemaString);
        });
    }

    @Test
    public void testValidateObjectKO() {

        assertThatExceptionOfType(ValidationException.class).isThrownBy(() -> validate(new Object(), schema));
    }

    @Test
    public void testValidateObjectNotValidJson() {

        assertThatExceptionOfType(ValidationException.class).isThrownBy(() -> validate(new Object(), schema));
    }

    @Test
    public void testValidateSubObjectOK() {
        Product product = new Product(42, "FooBar", 42);
        Product product2 = new Product(420, "FooBarBaz", 420);
        Basket basket = new Basket();
        basket.add(product);
        basket.add(product2);
        MyCustomEvent event = new MyCustomEvent(basket);

        assertThatNoException().isThrownBy(() ->
        {
            validate(event, schemaString, "basket.products[0]");
        });
    }

    @Test
    public void testValidateSubObjectKO() {
        Product product = new Product(42, null, 42);
        Product product2 = new Product(420, "FooBarBaz", 420);
        Basket basket = new Basket();
        basket.add(product);
        basket.add(product2);
        MyCustomEvent event = new MyCustomEvent(basket);

        assertThatExceptionOfType(ValidationException.class).isThrownBy(
                () -> validate(event, schema, "basket.products[0]"));
    }

    @Test
    public void testValidateSubObjectListOK() {
        Product product = new Product(42, "BarBazFoo", 42);
        Product product2 = new Product(420, "FooBarBaz", 23);
        Basket basket = new Basket();
        basket.add(product);
        basket.add(product2);
        MyCustomEvent event = new MyCustomEvent(basket);

        assertThatNoException().isThrownBy(() -> validate(event, schema, "basket.products[*]"));
    }

    @Test
    public void testValidateSubObjectListKO() {
        Product product = new Product(42, "BarBazFoo", 42);
        Product product2 = new Product(420, "FooBarBaz", -23);
        Basket basket = new Basket();
        basket.add(product);
        basket.add(product2);
        MyCustomEvent event = new MyCustomEvent(basket);

        assertThatExceptionOfType(ValidationException.class).isThrownBy(
                () -> validate(event, schema, "basket.products[*]"));
    }

    @Test
    public void testValidateSubObjectNotFound() {
        Product product = new Product(42, "BarBazFoo", 42);
        Basket basket = new Basket();
        basket.add(product);
        MyCustomEvent event = new MyCustomEvent(basket);
        assertThatExceptionOfType(ValidationException.class).isThrownBy(() -> validate(event, schema, "basket."));
    }

    @Test
    public void testValidateSubObjectNotListNorObject() {
        Product product = new Product(42, "Bar", 42);
        Product product2 = new Product(420, "FooBarBaz", -23);
        Basket basket = new Basket();
        basket.add(product);
        basket.add(product2);
        MyCustomEvent event = new MyCustomEvent(basket);

        assertThatThrownBy(() -> validate(event, schema, "basket.products[0].id"))
                .isInstanceOf(ValidationException.class)
                .hasMessage("Invalid format for 'basket.products[0].id': 'NUMBER'");
    }

    @Test
    public void testValidateSubObjectJsonString() {
        Basket basket = new Basket();
        basket.setHiddenProduct("ewogICJpZCI6IDQzMjQyLAogICJuYW1lIjogIkZvb0JhciBYWSIsCiAgInByaWNlIjogMjU4Cn0=");
        MyCustomEvent event = new MyCustomEvent(basket);

        assertThatNoException().isThrownBy(() -> validate(event, schema, "basket.powertools_base64(hiddenProduct)"));
    }

    @Test
    public void testValidateSubObjectSimpleString() {
        Basket basket = new Basket();
        basket.setHiddenProduct("ghostbuster");
        MyCustomEvent event = new MyCustomEvent(basket);

        assertThatThrownBy(() -> validate(event, schema, "basket.hiddenProduct"))
                .isInstanceOf(ValidationException.class)
                .hasMessage("Invalid format for 'basket.hiddenProduct': 'STRING' and no JSON found in it.");
    }

    @Test
    public void testValidateSubObjectWithoutEnvelope() {
        Product product = new Product(42, "BarBazFoo", 42);
        assertThatNoException().isThrownBy(() -> validate(product, schema, null));
    }

    @Test
    public void testValidateSubObjectWithEmptyEnvelope() {
        Product product = new Product(42, "BarBazFoo", 42);
        assertThatNoException().isThrownBy(() -> validate(product, schema, ""));
    }

}

package software.amazon.lambda.powertools.validation;

import com.fasterxml.jackson.databind.JsonNode;
import com.networknt.schema.JsonSchema;
import com.networknt.schema.SpecVersion;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import software.amazon.lambda.powertools.validation.model.Basket;
import software.amazon.lambda.powertools.validation.model.MyCustomEvent;
import software.amazon.lambda.powertools.validation.model.Product;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import static org.assertj.core.api.Assertions.*;
import static software.amazon.lambda.powertools.validation.ValidationUtils.getJsonSchema;
import static software.amazon.lambda.powertools.validation.ValidationUtils.validate;

public class ValidationUtilsTest {

    private JsonSchema schema = getJsonSchema("classpath:/schema_v7.json");

    @BeforeEach
    public void setup() {
        ValidationConfig.get().setSchemaVersion(SpecVersion.VersionFlag.V7);
    }

    @Test
    public void testLoadSchemaV7OK() {
        ValidationConfig.get().setSchemaVersion(SpecVersion.VersionFlag.V7);
        JsonSchema jsonSchema = getJsonSchema("classpath:/schema_v7.json", true);
        assertThat(jsonSchema).isNotNull();
        assertThat(jsonSchema.getCurrentUri()).asString().isEqualTo("http://example.com/product.json");
    }

    @Test
    public void testLoadSchemaV7KO() {
        ValidationConfig.get().setSchemaVersion(SpecVersion.VersionFlag.V7);
        assertThatThrownBy(() -> getJsonSchema("classpath:/schema_v7_ko.json", true))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("The schema classpath:/schema_v7_ko.json is not valid, it does not respect the specification V7");
    }

    @Test
    public void testLoadMetaSchema_NoValidation() {
        ValidationConfig.get().setSchemaVersion(SpecVersion.VersionFlag.V201909);
        getJsonSchema("classpath:/schemas/meta_schema_V201909", false);
    }

    @Test
    public void testLoadMetaSchemaV2019() {
        ValidationConfig.get().setSchemaVersion(SpecVersion.VersionFlag.V201909);
        JsonSchema jsonSchema = getJsonSchema("classpath:/schemas/meta_schema_V201909", true);
        assertThat(jsonSchema).isNotNull();
    }

    @Test
    public void testLoadMetaSchemaV7() {
        ValidationConfig.get().setSchemaVersion(SpecVersion.VersionFlag.V7);
        JsonSchema jsonSchema = getJsonSchema("classpath:/schemas/meta_schema_V7", true);
        assertThat(jsonSchema).isNotNull();
    }

    @Test
    public void testLoadMetaSchemaV6() {
        ValidationConfig.get().setSchemaVersion(SpecVersion.VersionFlag.V6);
        JsonSchema jsonSchema = getJsonSchema("classpath:/schemas/meta_schema_V6", true);
        assertThat(jsonSchema).isNotNull();
    }

    @Test
    public void testLoadMetaSchemaV4() {
        ValidationConfig.get().setSchemaVersion(SpecVersion.VersionFlag.V4);
        JsonSchema jsonSchema = getJsonSchema("classpath:/schemas/meta_schema_V4", true);
        assertThat(jsonSchema).isNotNull();
    }

    @Test
    public void testLoadSchemaV4OK() {
        ValidationConfig.get().setSchemaVersion(SpecVersion.VersionFlag.V4);
        JsonSchema jsonSchema = getJsonSchema("classpath:/schema_v4.json", true);
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
        JsonNode node = ValidationConfig.get().getObjectMapper().readTree(this.getClass().getResourceAsStream("/json_ok.json"));

        validate(node, schema);
    }

    @Test
    public void testValidateJsonNodeKO() throws IOException {
        JsonNode node = ValidationConfig.get().getObjectMapper().readTree(this.getClass().getResourceAsStream("/json_ko.json"));

        assertThatExceptionOfType(ValidationException.class).isThrownBy(() -> validate(node, schema));
    }

    @Test
    public void testValidateMapOK() {
        Map<String, Object> map = new HashMap<>();
        map.put("id", 43242);
        map.put("name", "FooBar XY");
        map.put("price", 258);

        validate(map, schema);
    }

    @Test
    public void testValidateMapKO() {
        Map<String, Object> map = new HashMap<>();
        map.put("id", 43242);
        map.put("name", "FooBar XY");

        assertThatExceptionOfType(ValidationException.class).isThrownBy(() -> validate(map, schema));
    }

    @Test
    public void testValidateStringOK() {
        String json = "{\n  \"id\": 43242,\n  \"name\": \"FooBar XY\",\n  \"price\": 258\n}";

        validate(json, schema);
    }

    @Test
    public void testValidateStringKO() {
        String json = "{\n  \"id\": 43242,\n  \"name\": \"FooBar XY\",\n  \"price\": 0\n}";

        assertThatExceptionOfType(ValidationException.class).isThrownBy(() -> validate(json, schema));
    }

    @Test
    public void testValidateObjectOK() {
        Product product = new Product(42, "FooBar", 42);
        validate(product, schema);
    }

    @Test
    public void testValidateObjectKO() {
        Product product = new Product(42, "FooBar", -12);

        assertThatExceptionOfType(ValidationException.class).isThrownBy(() -> validate(product, schema));
    }

    @Test
    public void testValidateSubObjectOK() {
        Product product = new Product(42, "FooBar", 42);
        Product product2 = new Product(420, "FooBarBaz", 420);
        Basket basket = new Basket();
        basket.add(product);
        basket.add(product2);
        MyCustomEvent event = new MyCustomEvent(basket);
        validate(event, schema, "basket.products[0]");
    }

    @Test
    public void testValidateSubObjectKO() {
        Product product = new Product(42, null, 42);
        Product product2 = new Product(420, "FooBarBaz", 420);
        Basket basket = new Basket();
        basket.add(product);
        basket.add(product2);
        MyCustomEvent event = new MyCustomEvent(basket);

        assertThatExceptionOfType(ValidationException.class).isThrownBy(() -> validate(event, schema, "basket.products[0]"));
    }

    @Test
    public void testValidateSubObjectListOK() {
        Product product = new Product(42, "BarBazFoo", 42);
        Product product2 = new Product(420, "FooBarBaz", 23);
        Basket basket = new Basket();
        basket.add(product);
        basket.add(product2);
        MyCustomEvent event = new MyCustomEvent(basket);

        validate(event, schema, "basket.products[*]");
    }

    @Test
    public void testValidateSubObjectListKO() {
        Product product = new Product(42, "BarBazFoo", 42);
        Product product2 = new Product(420, "FooBarBaz", -23);
        Basket basket = new Basket();
        basket.add(product);
        basket.add(product2);
        MyCustomEvent event = new MyCustomEvent(basket);

        assertThatExceptionOfType(ValidationException.class).isThrownBy(() -> validate(event, schema, "basket.products[*]"));
    }

    @Test
    public void testValidateSubObjectNotFound() {
        Product product = new Product(42, "BarBazFoo", 42);
        Basket basket = new Basket();
        basket.add(product);
        MyCustomEvent event = new MyCustomEvent(basket);
        assertThatExceptionOfType(ValidationException.class).isThrownBy(() -> validate(event, schema, "basket.product"));
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

        validate(event, schema, "basket.powertools_base64(hiddenProduct)");
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
        validate(product, schema, null);
    }

}

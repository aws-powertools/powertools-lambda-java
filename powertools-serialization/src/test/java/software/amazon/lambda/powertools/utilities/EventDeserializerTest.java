/*
 * Copyright 2022 Amazon.com, Inc. or its affiliates.
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
package software.amazon.lambda.powertools.utilities;

import com.amazonaws.services.lambda.runtime.events.*;
import com.amazonaws.services.lambda.runtime.tests.annotations.Event;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import software.amazon.lambda.powertools.utilities.model.Basket;
import software.amazon.lambda.powertools.utilities.model.Product;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static software.amazon.lambda.powertools.utilities.EventDeserializer.extractDataFrom;

public class EventDeserializerTest {

    @Test
    public void testDeserializeStringAsString_shouldReturnString() {
        String stringEvent = "Hello World";
        String result = extractDataFrom(stringEvent).as(String.class);
        assertThat(result).isEqualTo(stringEvent);
    }

    @Test
    public void testDeserializeStringAsObject_shouldReturnObject() {
        String productStr = "{\"id\":1234, \"name\":\"product\", \"price\":42}";
        Product product = extractDataFrom(productStr).as(Product.class);
        assertProduct(product);
    }

    @Test
    public void testDeserializeMapAsObject_shouldReturnObject() {
        Map<String, Object> map = new HashMap<>();
        map.put("id", 1234);
        map.put("name", "product");
        map.put("price", 42);
        Product product = extractDataFrom(map).as(Product.class);
        assertProduct(product);
    }

    @ParameterizedTest
    @Event(value = "apigw_event.json", type = APIGatewayProxyRequestEvent.class)
    public void testDeserializeAPIGWEventBodyAsObject_shouldReturnObject(APIGatewayProxyRequestEvent event) {
        Product product = extractDataFrom(event).as(Product.class);
        assertProduct(product);
    }

    @ParameterizedTest
    @Event(value = "apigw_event.json", type = APIGatewayProxyRequestEvent.class)
    public void testDeserializeAPIGWEventBodyAsWrongObjectType_shouldThrowException(APIGatewayProxyRequestEvent event) {
        assertThatThrownBy(() -> extractDataFrom(event).as(Basket.class))
                .isInstanceOf(EventDeserializationException.class)
                .hasMessage("Cannot load the event as Basket");
    }

    @ParameterizedTest
    @Event(value = "sns_event.json", type = SNSEvent.class)
    public void testDeserializeSNSEventMessageAsObject_shouldReturnObject(SNSEvent event) {
        Product product = extractDataFrom(event).as(Product.class);
        assertProduct(product);
    }

    @ParameterizedTest
    @Event(value = "sqs_event.json", type = SQSEvent.class)
    public void testDeserializeSQSEventMessageAsList_shouldReturnList(SQSEvent event) {
        List<Product> products = extractDataFrom(event).asListOf(Product.class);
        assertThat(products).hasSize(2);
        assertProduct(products.get(0));
    }

    @ParameterizedTest
    @Event(value = "kinesis_event.json", type = KinesisEvent.class)
    public void testDeserializeKinesisEventMessageAsList_shouldReturnList(KinesisEvent event) {
        List<Product> products = extractDataFrom(event).asListOf(Product.class);
        assertThat(products).hasSize(2);
        assertProduct(products.get(0));
    }

    @ParameterizedTest
    @Event(value = "kafka_event.json", type = KafkaEvent.class)
    public void testDeserializeKafkaEventMessageAsList_shouldReturnList(KafkaEvent event) {
        List<Product> products = extractDataFrom(event).asListOf(Product.class);
        assertThat(products).hasSize(2);
        assertProduct(products.get(0));
    }

    @ParameterizedTest
    @Event(value = "sqs_event.json", type = SQSEvent.class)
    public void testDeserializeSQSEventMessageAsObject_shouldThrowException(SQSEvent event) {
        assertThatThrownBy(() -> extractDataFrom(event).as(Product.class))
                .isInstanceOf(EventDeserializationException.class)
                .hasMessageContaining("consider using 'extractDataAsListOf' instead");
    }

    @Test
    public void testDeserializeProductAsProduct_shouldReturnProduct() {
        Product myProduct = new Product(1234, "product", 42);
        Product product = extractDataFrom(myProduct).as(Product.class);
        assertProduct(product);
    }


    private void assertProduct(Product product) {
assertThat(product)
                .isEqualTo(new Product(1234, "product", 42))
                .usingRecursiveComparison();
    }

}

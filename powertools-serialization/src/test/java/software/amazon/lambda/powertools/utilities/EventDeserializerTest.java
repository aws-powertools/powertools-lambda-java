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

import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyRequestEvent;
import com.amazonaws.services.lambda.runtime.events.APIGatewayV2HTTPEvent;
import com.amazonaws.services.lambda.runtime.events.ActiveMQEvent;
import com.amazonaws.services.lambda.runtime.events.ApplicationLoadBalancerRequestEvent;
import com.amazonaws.services.lambda.runtime.events.CloudFormationCustomResourceEvent;
import com.amazonaws.services.lambda.runtime.events.CloudWatchLogsEvent;
import com.amazonaws.services.lambda.runtime.events.KafkaEvent;
import com.amazonaws.services.lambda.runtime.events.KinesisAnalyticsFirehoseInputPreprocessingEvent;
import com.amazonaws.services.lambda.runtime.events.KinesisAnalyticsStreamsInputPreprocessingEvent;
import com.amazonaws.services.lambda.runtime.events.KinesisEvent;
import com.amazonaws.services.lambda.runtime.events.KinesisFirehoseEvent;
import com.amazonaws.services.lambda.runtime.events.RabbitMQEvent;
import com.amazonaws.services.lambda.runtime.events.SNSEvent;
import com.amazonaws.services.lambda.runtime.events.SQSEvent;
import com.amazonaws.services.lambda.runtime.events.ScheduledEvent;
import com.amazonaws.services.lambda.runtime.tests.annotations.Event;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import software.amazon.lambda.powertools.utilities.model.Basket;
import software.amazon.lambda.powertools.utilities.model.Order;
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
    public void testDeserializeStringArrayAsList_shouldReturnList() {
        String productStr = "[{\"id\":1234, \"name\":\"product\", \"price\":42}, {\"id\":2345, \"name\":\"product2\", \"price\":43}]";
        List<Product> products = extractDataFrom(productStr).asListOf(Product.class);
        assertThat(products).hasSize(2);
        assertProduct(products.get(0));
    }

    @Test
    public void testDeserializeStringAsList_shouldThrowException() {
        String productStr = "{\"id\":1234, \"name\":\"product\", \"price\":42}";
        assertThatThrownBy(() -> extractDataFrom(productStr).asListOf(Product.class))
                .isInstanceOf(EventDeserializationException.class)
                .hasMessage("Cannot load the event as a list of Product, consider using 'as' instead");
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
                .hasMessageContaining("consider using 'asListOf' instead");
    }

    @ParameterizedTest
    @Event(value = "apigw_event.json", type = APIGatewayProxyRequestEvent.class)
    public void testDeserializeAPIGatewayEventAsList_shouldThrowException(APIGatewayProxyRequestEvent event) {
        assertThatThrownBy(() -> extractDataFrom(event).asListOf(Product.class))
                .isInstanceOf(EventDeserializationException.class)
                .hasMessageContaining("consider using 'as' instead")
                .hasMessageContaining("Cannot load the event as a list of");
    }

    @ParameterizedTest
    @Event(value = "custom_event_map.json", type = HashMap.class)
    public void testDeserializeAPIGatewayMapEventAsList_shouldThrowException(Map<String, Order> event) {
        assertThatThrownBy(() -> extractDataFrom(event).asListOf(Order.class))
                .isInstanceOf(EventDeserializationException.class)
                .hasMessage("The content of this event is not a list, consider using 'as' instead");
    }

    @Test
    public void testDeserializeEmptyEventAsList_shouldThrowException() {
        assertThatThrownBy(() -> extractDataFrom(null).asListOf(Product.class))
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("Event content is null: the event may be malformed (missing fields)");
    }

    @ParameterizedTest
    @Event(value = "sqs_event.json", type = SQSEvent.class)
    public void testDeserializeSQSEventBodyAsWrongObjectType_shouldThrowException(SQSEvent event) {
        assertThatThrownBy(() -> extractDataFrom(event).asListOf(Basket.class))
                .isInstanceOf(EventDeserializationException.class)
                .hasMessage("Cannot load the event as a list of Basket");
    }

    @ParameterizedTest
    @Event(value = "apigw_event_no_body.json", type = APIGatewayProxyRequestEvent.class)
    public void testDeserializeAPIGatewayNoBody_shouldThrowException(APIGatewayProxyRequestEvent event) {
        assertThatThrownBy(() -> extractDataFrom(event).as(Product.class))
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("Event content is null: the event may be malformed (missing fields)");
    }

    @Test
    public void testDeserializeAPIGatewayNoBodyAsList_shouldThrowException() {
        assertThatThrownBy(() -> extractDataFrom(new Object()).asListOf(Product.class))
                .isInstanceOf(EventDeserializationException.class)
                .hasMessage("The content of this event is not a list, consider using 'as' instead");
    }

    @ParameterizedTest
    @Event(value = "sqs_event_no_body.json", type = SQSEvent.class)
    public void testDeserializeSQSEventNoBody_shouldThrowException(SQSEvent event) {
        List<Product> products = extractDataFrom(event).asListOf(Product.class);
        assertThat(products.get(0)).isNull();
    }

    @Test
    public void testDeserializeProductAsProduct_shouldReturnProduct() {
        Product myProduct = new Product(1234, "product", 42);
        Product product = extractDataFrom(myProduct).as(Product.class);
        assertProduct(product);
    }


    private void assertProduct(Product product) {
        assertThat(product)
                .usingRecursiveComparison()
                .isEqualTo(new Product(1234, "product", 42));
    }

    @ParameterizedTest
    @Event(value = "scheduled_event.json", type = ScheduledEvent.class)
    public void testDeserializeScheduledEventMessageAsObject_shouldReturnObject(ScheduledEvent event) {
        Product product = extractDataFrom(event).as(Product.class);
        assertProduct(product);
    }

    @ParameterizedTest
    @Event(value = "alb_event.json", type = ApplicationLoadBalancerRequestEvent.class)
    public void testDeserializeALBEventMessageAsObjectShouldReturnObject(ApplicationLoadBalancerRequestEvent event) {
        Product product = extractDataFrom(event).as(Product.class);
        assertProduct(product);
    }

    @ParameterizedTest
    @Event(value = "cwl_event.json", type = CloudWatchLogsEvent.class)
    public void testDeserializeCWLEventMessageAsObjectShouldReturnObject(CloudWatchLogsEvent event) {
        Product product = extractDataFrom(event).as(Product.class);
        assertProduct(product);
    }

    @ParameterizedTest
    @Event(value = "kf_event.json", type = KinesisFirehoseEvent.class)
    public void testDeserializeKFEventMessageAsListShouldReturnList(KinesisFirehoseEvent event) {
        List<Product> products = extractDataFrom(event).asListOf(Product.class);
        assertThat(products).hasSize(1);
        assertProduct(products.get(0));
    }

    @ParameterizedTest
    @Event(value = "amq_event.json", type = ActiveMQEvent.class)
    public void testDeserializeAMQEventMessageAsListShouldReturnList(ActiveMQEvent event) {
        List<Product> products = extractDataFrom(event).asListOf(Product.class);
        assertThat(products).hasSize(1);
        assertProduct(products.get(0));
    }

    @ParameterizedTest
    @Event(value = "rabbitmq_event.json", type = RabbitMQEvent.class)
    public void testDeserializeRabbitMQEventMessageAsListShouldReturnList(RabbitMQEvent event) {
        List<Product> products = extractDataFrom(event).asListOf(Product.class);
        assertThat(products).hasSize(1);
        assertProduct(products.get(0));
    }

    @ParameterizedTest
    @Event(value = "kasip_event.json", type = KinesisAnalyticsStreamsInputPreprocessingEvent.class)
    public void testDeserializeKasipEventMessageAsListShouldReturnList(KinesisAnalyticsStreamsInputPreprocessingEvent event) {
        List<Product> products = extractDataFrom(event).asListOf(Product.class);
        assertThat(products).hasSize(1);
        assertProduct(products.get(0));
    }

    @ParameterizedTest
    @Event(value = "kafip_event.json", type = KinesisAnalyticsFirehoseInputPreprocessingEvent.class)
    public void testDeserializeKafipEventMessageAsListShouldReturnList(KinesisAnalyticsFirehoseInputPreprocessingEvent event) {
        List<Product> products = extractDataFrom(event).asListOf(Product.class);
        assertThat(products).hasSize(1);
        assertProduct(products.get(0));
    }

    @ParameterizedTest
    @Event(value = "apigwv2_event.json", type = APIGatewayV2HTTPEvent.class)
    public void testDeserializeApiGWV2EventMessageAsObjectShouldReturnObject(APIGatewayV2HTTPEvent event) {
        Product product = extractDataFrom(event).as(Product.class);
        assertProduct(product);
    }

    @ParameterizedTest
    @Event(value = "cfcr_event.json", type = CloudFormationCustomResourceEvent.class)
    public void testDeserializeCfcrEventMessageAsObjectShouldReturnObject(CloudFormationCustomResourceEvent event) {
        Product product = extractDataFrom(event).as(Product.class);
        assertProduct(product);
    }

}

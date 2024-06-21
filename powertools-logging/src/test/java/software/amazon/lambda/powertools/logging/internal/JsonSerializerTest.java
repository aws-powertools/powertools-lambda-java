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

package software.amazon.lambda.powertools.logging.internal;

import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.core.TreeNode;
import com.fasterxml.jackson.databind.JsonNode;
import java.io.IOException;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import software.amazon.lambda.powertools.logging.model.Basket;
import software.amazon.lambda.powertools.logging.model.Product;
import software.amazon.lambda.powertools.utilities.JsonConfig;

class JsonSerializerTest {
    private StringBuilder sb;
    private JsonSerializer generator;

    @BeforeEach
    void setUp() {
        sb = new StringBuilder();
        generator = new JsonSerializer(sb);
    }

    @Test
    void writeString_shouldWriteStringWithQuotes() throws IOException {
        generator.writeStringField("key", "StringValue");
        assertThat(sb.toString()).hasToString("\"key\":\"StringValue\"");
    }

    @Test
    void writeBoolean_shouldWriteBooleanValue() throws IOException {
        generator.writeBooleanField("key", true);
        assertThat(sb.toString()).hasToString("\"key\":true");
    }

    @Test
    void writeNull_shouldWriteNullValue() throws IOException {
        generator.writeNullField("key");
        assertThat(sb.toString()).hasToString("\"key\":null");
    }

    @Test
    void writeInt_shouldWriteIntValue() throws IOException {
        generator.writeNumberField("key", 1);
        assertThat(sb.toString()).hasToString("\"key\":1");
    }

    @Test
    void writeFloat_shouldWriteFloatValue() throws IOException {
        generator.writeNumberField("key", 2.4f);
        assertThat(sb.toString()).hasToString("\"key\":2.4");
        assertThat(sb.toString()).doesNotContain("F").doesNotContain("f"); // should not contain the F suffix for floats.
    }

    @Test
    void writeDouble_shouldWriteDoubleValue() throws IOException {
        generator.writeNumberField("key", 4.3);
        assertThat(sb.toString()).hasToString("\"key\":4.3");
    }

    @Test
    void writeLong_shouldWriteLongValue() throws IOException {
        generator.writeNumberField("key", 123456789L);
        assertThat(sb.toString()).hasToString("\"key\":123456789");
        assertThat(sb.toString()).doesNotContain("L").doesNotContain("l"); // should not contain the L suffix for longs.
    }

    @Test
    void writeBigDecimal_shouldWriteBigDecimal() throws IOException {
        generator.writeNumberField("key", BigDecimal.valueOf(432.1673254564546));
        assertThat(sb.toString()).hasToString("\"key\":432.1673254564546");
    }

    @Test
    void writeStringObject_shouldWriteStringWithQuotes() throws IOException {
        generator.writeObjectField("key","StringValue");
        assertThat(sb.toString()).hasToString("\"key\":\"StringValue\"");
    }

    @Test
    void writeMapObject_shouldWriteMapAsJson() throws IOException {
        Map<String, Object> map = new HashMap<>();
        map.put("string","StringValue");
        map.put("number", BigInteger.valueOf(1234567890L));
        generator.writeObjectField("map", map);
        assertThat(sb.toString()).hasToString("\"map\":{\"number\":1234567890,\"string\":\"StringValue\"}");
    }

    @Test
    void writeListObject_shouldWriteListAsArray() throws IOException {
        List<String> list = Arrays.asList("val1", "val2", "val3");
        generator.writeObjectField("list", list);
        assertThat(sb.toString()).hasToString("\"list\":[\"val1\",\"val2\",\"val3\"]");
    }

    @Test
    void writeCustomObject_shouldWriteObjectAsJson() throws IOException {
        Basket basket = new Basket();
        basket.add(new Product(42, "Nintendo DS", 299.45));
        basket.add(new Product(98, "Playstation 5", 499.99));
        generator.writeObjectField("basket", basket);
        assertThat(sb.toString()).hasToString("\"basket\":{\"products\":[{\"id\":42,\"name\":\"Nintendo DS\",\"price\":299.45},{\"id\":98,\"name\":\"Playstation 5\",\"price\":499.99}]}");
    }

    @Test
    void writeJsonNodeObject_shouldWriteObjectAsJson() throws IOException {
        JsonNode jsonNode = JsonConfig.get().getObjectMapper().readTree(
                "[{\"id\":42,\"name\":\"Nintendo DS\",\"price\":299.45},{\"id\":98,\"name\":\"Playstation 5\",\"price\":499.99}]");
        generator.writeObjectField("basket", jsonNode);
        assertThat(sb.toString()).hasToString("\"basket\":[{\"id\":42,\"name\":\"Nintendo DS\",\"price\":299.45},{\"id\":98,\"name\":\"Playstation 5\",\"price\":499.99}]");
    }

    @Test
    void writeTreeNodeArrayObject_shouldWriteObjectAsJson() throws IOException {
        TreeNode treeNode = JsonConfig.get().getObjectMapper().readTree(
                "[{\"id\":42,\"name\":\"Nintendo DS\",\"price\":299.45},{\"id\":98,\"name\":\"Playstation 5\",\"price\":499.99}]");
        generator.writeTree(treeNode);
        assertThat(sb.toString()).hasToString("[{\"id\":42,\"name\":\"Nintendo DS\",\"price\":299.45},{\"id\":98,\"name\":\"Playstation 5\",\"price\":499.99}]");
    }

    @Test
    void writeTreeNodeObject_shouldWriteObjectAsJson() throws IOException {
        TreeNode treeNode = JsonConfig.get().getObjectMapper().readTree(
                "{\"id\":42,\"name\":\"Nintendo DS\",\"price\":299.45}");
        generator.writeTree(treeNode);
        assertThat(sb.toString()).hasToString("{\"id\":42,\"name\":\"Nintendo DS\",\"price\":299.45}");
    }

}

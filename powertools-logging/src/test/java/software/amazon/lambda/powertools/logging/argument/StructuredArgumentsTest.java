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

package software.amazon.lambda.powertools.logging.argument;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import software.amazon.lambda.powertools.logging.internal.JsonSerializer;
import software.amazon.lambda.powertools.logging.model.Basket;
import software.amazon.lambda.powertools.logging.model.Product;

class StructuredArgumentsTest {
    private StringBuilder sb;
    private JsonSerializer serializer;

    @BeforeEach
    void setUp() {
        sb = new StringBuilder();
        serializer = new JsonSerializer(sb);
    }

    @Test
    void keyValueArgument() throws IOException {
        // GIVEN
        Basket basket = new Basket();
        basket.add(new Product(42, "Nintendo DS", 299.45));
        basket.add(new Product(98, "Playstation 5", 499.99));

        // WHEN
        StructuredArgument argument = StructuredArguments.entry("basket", basket);
        argument.writeTo(serializer);

        // THEN
        assertThat(sb.toString()).hasToString("\"basket\":{\"products\":[{\"id\":42,\"name\":\"Nintendo DS\",\"price\":299.45},{\"id\":98,\"name\":\"Playstation 5\",\"price\":499.99}]}");
        assertThat(argument.toString()).hasToString("basket=Basket{products=[Product{id=42, name='Nintendo DS', price=299.45}, Product{id=98, name='Playstation 5', price=499.99}]}");
    }

    @Test
    void mapArgument() throws IOException {
        // GIVEN
        Map<String, Product> catalog = new HashMap<>();
        catalog.put("nds", new Product(42, "Nintendo DS", 299.45));
        catalog.put("ps5", new Product(98, "Playstation 5", 499.99));

        // WHEN
        StructuredArgument argument = StructuredArguments.entries(catalog);
        argument.writeTo(serializer);

        // THEN
        assertThat(sb.toString())
                .contains("\"nds\":{\"id\":42,\"name\":\"Nintendo DS\",\"price\":299.45}")
                .contains("\"ps5\":{\"id\":98,\"name\":\"Playstation 5\",\"price\":499.99}");
        assertThat(argument.toString())
                .contains("nds=Product{id=42, name='Nintendo DS', price=299.45}")
                .contains("ps5=Product{id=98, name='Playstation 5', price=499.99}");
    }

    @Test
    void arrayArgument() throws IOException {
        // GIVEN
        Product[] products = new Product[]{
                new Product(42, "Nintendo DS", 299.45),
                new Product(98, "Playstation 5", 499.99)
        };

        // WHEN
        StructuredArgument argument = StructuredArguments.array("products", products);
        argument.writeTo(serializer);

        // THEN
        assertThat(sb.toString()).contains("\"products\":[{\"id\":42,\"name\":\"Nintendo DS\",\"price\":299.45},{\"id\":98,\"name\":\"Playstation 5\",\"price\":499.99}]");
        assertThat(argument.toString()).contains("products=[Product{id=42, name='Nintendo DS', price=299.45}, Product{id=98, name='Playstation 5', price=499.99}]");
    }

    @Test
    void jsonArgument() throws IOException {
        // GIVEN
        String rawJson = "{\"id\":42,\"name\":\"Nintendo DS\",\"price\":299.45}";

        // WHEN
        StructuredArgument argument = StructuredArguments.json("product", rawJson);
        argument.writeTo(serializer);

        // THEN
        assertThat(sb.toString()).contains("\"product\":{\"id\":42,\"name\":\"Nintendo DS\",\"price\":299.45}");
        assertThat(argument.toString()).contains("product={\"id\":42,\"name\":\"Nintendo DS\",\"price\":299.45}");
    }

}

/*
 * Copyright 2020 Amazon.com, Inc. or its affiliates.
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

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.JsonNodeType;
import io.burt.jmespath.Expression;
import org.junit.jupiter.api.Test;

import java.io.IOException;

import static org.assertj.core.api.Assertions.assertThat;

public class Base64FunctionTest {

    @Test
    public void testPowertoolsBase64() throws IOException {
        JsonNode event = ValidationConfig.get().getObjectMapper().readTree(this.getClass().getResourceAsStream("/custom_event.json"));
        Expression<JsonNode> expression = ValidationConfig.get().getJmesPath().compile("basket.powertools_base64(hiddenProduct)");
        JsonNode result = expression.search(event);
        assertThat(result.getNodeType()).isEqualTo(JsonNodeType.STRING);
        assertThat(result.asText()).isEqualTo("{\n" +
                "  \"id\": 43242,\n" +
                "  \"name\": \"FooBar XY\",\n" +
                "  \"price\": 258\n" +
                "}");
    }
}

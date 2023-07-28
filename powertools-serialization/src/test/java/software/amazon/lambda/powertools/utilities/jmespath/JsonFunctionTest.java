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

package software.amazon.lambda.powertools.utilities.jmespath;

import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.JsonNodeType;
import io.burt.jmespath.Expression;
import java.io.IOException;
import org.junit.jupiter.api.Test;
import software.amazon.lambda.powertools.utilities.JsonConfig;

public class JsonFunctionTest {

    @Test
    public void testJsonFunction() throws IOException {
        JsonNode event = JsonConfig.get().getObjectMapper()
                .readTree(this.getClass().getResourceAsStream("/custom_event_json.json"));
        Expression<JsonNode> expression = JsonConfig.get().getJmesPath().compile("powertools_json(body)");
        JsonNode result = expression.search(event);
        assertThat(result.getNodeType()).isEqualTo(JsonNodeType.OBJECT);
        assertThat(result.get("message").asText()).isEqualTo("Lambda rocks");
        assertThat(result.get("list").isArray()).isTrue();
        assertThat(result.get("list").size()).isEqualTo(2);
    }

    @Test
    public void testJsonFunctionChild() throws IOException {
        JsonNode event = JsonConfig.get().getObjectMapper()
                .readTree(this.getClass().getResourceAsStream("/custom_event_json.json"));
        Expression<JsonNode> expression = JsonConfig.get().getJmesPath().compile("powertools_json(body).list[0].item");
        JsonNode result = expression.search(event);
        assertThat(result.getNodeType()).isEqualTo(JsonNodeType.STRING);
        assertThat(result.asText()).isEqualTo("4gh345h");
    }
}

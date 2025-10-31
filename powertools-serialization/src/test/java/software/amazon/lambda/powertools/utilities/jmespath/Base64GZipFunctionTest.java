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

import java.io.IOException;

import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.JsonNodeType;

import io.burt.jmespath.Expression;
import io.burt.jmespath.JmesPathType;
import software.amazon.lambda.powertools.utilities.JsonConfig;

class Base64GZipFunctionTest {

    @Test
    void testConstructor() {
        Base64GZipFunction base64GZipFunction = new Base64GZipFunction();
        assertThat(base64GZipFunction.name()).isEqualTo("powertools_base64_gzip");
        assertThat(base64GZipFunction.argumentConstraints().expectedType().toLowerCase()).isEqualTo(
                JmesPathType.STRING.name().toLowerCase());
        assertThat(base64GZipFunction.argumentConstraints().minArity()).isEqualTo(1);
        assertThat(base64GZipFunction.argumentConstraints().maxArity()).isEqualTo(1);

    }

    @Test
    void testPowertoolsGzip() throws IOException {
        JsonNode event = JsonConfig.get().getObjectMapper()
                .readTree(this.getClass().getResourceAsStream("/custom_event_gzip.json"));
        Expression<JsonNode> expression = JsonConfig.get().getJmesPath()
                .compile("basket.powertools_base64_gzip(hiddenProduct)");
        JsonNode result = expression.search(event);
        assertThat(result.getNodeType()).isEqualTo(JsonNodeType.STRING);
        assertThat(result.asText()).isEqualTo("{  \"id\": 43242,  \"name\": \"FooBar XY\",  \"price\": 258}");
    }

    @Test
    void testPowertoolsGzipEmptyJsonAttribute() throws IOException {
        JsonNode event = JsonConfig.get().getObjectMapper()
                .readTree(this.getClass().getResourceAsStream("/custom_event_gzip.json"));
        Expression<JsonNode> expression = JsonConfig.get().getJmesPath().compile("basket.powertools_base64_gzip('')");
        JsonNode result = expression.search(event);
        assertThat(result.getNodeType()).isEqualTo(JsonNodeType.NULL);
    }

    @Test
    void testPowertoolsGzipWrongArgumentType() throws IOException {
        JsonNode event = JsonConfig.get().getObjectMapper()
                .readTree(this.getClass().getResourceAsStream("/custom_event_gzip.json"));
        Expression<JsonNode> expression = JsonConfig.get().getJmesPath().compile("basket.powertools_base64_gzip(null)");
        JsonNode result = expression.search(event);

        assertThat(result.getNodeType()).isEqualTo(JsonNodeType.NULL);
    }

    @Test
    void testBase64GzipDecompressNull() {
        String result = Base64GZipFunction.decompress(null);
        assertThat(result).isNull();
    }

    @Test
    void testPowertoolsGzipNotCompressedJsonAttribute() throws IOException {
        JsonNode event = JsonConfig.get().getObjectMapper()
                .readTree(this.getClass().getResourceAsStream("/custom_event_gzip.json"));
        Expression<JsonNode> expression = JsonConfig.get().getJmesPath()
                .compile("basket.powertools_base64_gzip(encodedString)");
        JsonNode result = expression.search(event);
        assertThat(result.getNodeType()).isEqualTo(JsonNodeType.STRING);
        assertThat(result.asText()).isEqualTo("test");
    }

}

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
        JsonNode event = ValidatorConfig.get().getObjectMapper().readTree(this.getClass().getResourceAsStream("/custom_event.json"));
        Expression<JsonNode> expression = ValidatorConfig.get().getJmesPath().compile("basket.powertools_base64(hiddenProduct)");
        JsonNode result = expression.search(event);
        assertThat(result.getNodeType()).isEqualTo(JsonNodeType.STRING);
        assertThat(result.asText()).isEqualTo("{\n" +
                "  \"id\": 43242,\n" +
                "  \"name\": \"FooBar XY\",\n" +
                "  \"price\": 258\n" +
                "}");
    }
}

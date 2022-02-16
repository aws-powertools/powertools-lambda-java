package software.amazon.lambda.powertools.utilities.jmespath;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.JsonNodeType;
import io.burt.jmespath.Expression;
import org.junit.jupiter.api.Test;
import software.amazon.lambda.powertools.utilities.JsonConfig;

import java.io.IOException;

import static org.assertj.core.api.Assertions.assertThat;

public class JsonFunctionTest {

    @Test
    public void testJsonFunction() throws IOException {
        JsonNode event = JsonConfig.get().getObjectMapper().readTree(this.getClass().getResourceAsStream("/custom_event_json.json"));
        Expression<JsonNode> expression = JsonConfig.get().getJmesPath().compile("powertools_json(body)");
        JsonNode result = expression.search(event);
        assertThat(result.getNodeType()).isEqualTo(JsonNodeType.OBJECT);
        assertThat(result.get("message").asText()).isEqualTo("Lambda rocks");
        assertThat(result.get("list").isArray()).isTrue();
        assertThat(result.get("list").size()).isEqualTo(2);
    }

    @Test
    public void testJsonFunctionChild() throws IOException {
        JsonNode event = JsonConfig.get().getObjectMapper().readTree(this.getClass().getResourceAsStream("/custom_event_json.json"));
        Expression<JsonNode> expression = JsonConfig.get().getJmesPath().compile("powertools_json(body).list[0].item");
        JsonNode result = expression.search(event);
        assertThat(result.getNodeType()).isEqualTo(JsonNodeType.STRING);
        assertThat(result.asText()).isEqualTo("4gh345h");
    }
}

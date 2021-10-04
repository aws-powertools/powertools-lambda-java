package software.amazon.lambda.powertools.cloudformation;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

public class ResponseTest {

    static class DummyBean {
        private final Object propertyWithLongName;

        DummyBean(Object propertyWithLongName) {
            this.propertyWithLongName = propertyWithLongName;
        }

        @SuppressWarnings("unused")
        public Object getPropertyWithLongName() {
            return propertyWithLongName;
        }
    }

    @Test
    void buildWithNoValueFails() {
        assertThatThrownBy(() -> Response.builder().build())
                .isInstanceOf(NullPointerException.class);
    }

    @Test
    void buildWithNullValueFails() {
        assertThatThrownBy(() -> Response.builder().value(null).build())
                .isInstanceOf(NullPointerException.class);
    }

    @Test
    void buildWithNoObjectMapperSucceeds() {
        Response response = Response.builder()
                .value("test")
                .build();

        assertThat(response).isNotNull();
        assertThat(response.getJsonNode()).isNotNull();
    }

    @Test
    void buildWithNullObjectMapperSucceeds() {
        Response response = Response.builder()
                .value(100)
                .objectMapper(null)
                .build();

        assertThat(response).isNotNull();
        assertThat(response.getJsonNode()).isNotNull();
    }

    @Test
    void jsonToStringWithDefaultMapperMapValue() {
        Map<String, String> value = new HashMap<>();
        value.put("foo", "bar");

        Response response = Response.builder()
                .value(value)
                .build();

        String expected = "{\"foo\":\"bar\"}";
        assertThat(response.toString()).isEqualTo(expected);
    }

    @Test
    void jsonToStringWithDefaultMapperObjectValue() {
        DummyBean value = new DummyBean("test");

        Response response = Response.builder()
                .value(value)
                .build();

        String expected = "{\"PropertyWithLongName\":\"test\"}";
        assertThat(response.toString()).isEqualTo(expected);
    }

    @Test
    void jsonToStringWithCustomMapper() {
        ObjectMapper customMapper = new ObjectMapper()
                .setPropertyNamingStrategy(PropertyNamingStrategies.KEBAB_CASE);

        DummyBean value = new DummyBean(10);
        Response response = Response.builder()
                .objectMapper(customMapper)
                .value(value)
                .build();

        String expected = "{\"property-with-long-name\":10}";
        assertThat(response.toString()).isEqualTo(expected);
    }
}

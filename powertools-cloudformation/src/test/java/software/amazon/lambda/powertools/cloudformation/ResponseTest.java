package software.amazon.lambda.powertools.cloudformation;

import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import java.util.HashMap;
import java.util.Map;
import org.junit.jupiter.api.Test;

public class ResponseTest {

    @Test
    void defaultValues() {
        Response response = Response.builder().build();

        assertThat(response).isNotNull();
        assertThat(response.getJsonNode()).isNull();
        assertThat(response.getStatus()).isEqualTo(Response.Status.SUCCESS);
        assertThat(response.getPhysicalResourceId()).isNull();
        assertThat(response.isNoEcho()).isFalse();

        assertThat(response.toString()).contains("JSON = null");
        assertThat(response.toString()).contains("Status = SUCCESS");
        assertThat(response.toString()).contains("PhysicalResourceId = null");
        assertThat(response.toString()).contains("NoEcho = false");
    }

    @Test
    void explicitNullValues() {
        Response response = Response.builder()
                .value(null)
                .objectMapper(null)
                .physicalResourceId(null)
                .status(null)
                .build();

        assertThat(response).isNotNull();
        assertThat(response.getJsonNode()).isNull();
        assertThat(response.getStatus()).isEqualTo(Response.Status.SUCCESS);
        assertThat(response.getPhysicalResourceId()).isNull();
        assertThat(response.isNoEcho()).isFalse();

        assertThat(response.toString()).contains("JSON = null");
        assertThat(response.toString()).contains("Status = SUCCESS");
        assertThat(response.toString()).contains("PhysicalResourceId = null");
        assertThat(response.toString()).contains("NoEcho = false");
    }

    @Test
    void customNonJsonRelatedValues() {
        Response response = Response.builder()
                .status(Response.Status.FAILED)
                .physicalResourceId("test")
                .noEcho(true)
                .build();

        assertThat(response).isNotNull();
        assertThat(response.getJsonNode()).isNull();
        assertThat(response.getStatus()).isEqualTo(Response.Status.FAILED);
        assertThat(response.getPhysicalResourceId()).isEqualTo("test");
        assertThat(response.isNoEcho()).isTrue();

        assertThat(response.toString()).contains("JSON = null");
        assertThat(response.toString()).contains("Status = FAILED");
        assertThat(response.toString()).contains("PhysicalResourceId = test");
        assertThat(response.toString()).contains("NoEcho = true");
    }

    @Test
    void jsonMapValueWithDefaultObjectMapper() {
        Map<String, String> value = new HashMap<>();
        value.put("foo", "bar");

        Response response = Response.builder()
                .value(value)
                .build();

        String expected = "{\"foo\":\"bar\"}";
        assertThat(response.getJsonNode()).isNotNull();
        assertThat(response.getJsonNode().toString()).isEqualTo(expected);
        assertThat(response.toString()).contains("JSON = " + expected);
    }

    @Test
    void jsonObjectValueWithDefaultObjectMapper() {
        DummyBean value = new DummyBean("test");

        Response response = Response.builder()
                .value(value)
                .build();

        String expected = "{\"PropertyWithLongName\":\"test\"}";
        assertThat(response.getJsonNode().toString()).isEqualTo(expected);
        assertThat(response.toString()).contains("JSON = " + expected);
    }

    @Test
    void jsonObjectValueWithNullObjectMapper() {
        DummyBean value = new DummyBean("test");

        Response response = Response.builder()
                .objectMapper(null)
                .value(value)
                .build();

        String expected = "{\"PropertyWithLongName\":\"test\"}";
        assertThat(response.getJsonNode().toString()).isEqualTo(expected);
        assertThat(response.toString()).contains("JSON = " + expected);
    }

    @Test
    void jsonObjectValueWithCustomObjectMapper() {
        ObjectMapper customMapper = new ObjectMapper()
                .setPropertyNamingStrategy(PropertyNamingStrategies.KEBAB_CASE);

        DummyBean value = new DummyBean(10);
        Response response = Response.builder()
                .objectMapper(customMapper)
                .value(value)
                .build();

        String expected = "{\"property-with-long-name\":10}";
        assertThat(response.getJsonNode().toString()).isEqualTo(expected);
        assertThat(response.toString()).contains("JSON = " + expected);
    }

    @Test
    void jsonObjectValueWithPostConfiguredObjectMapper() {
        ObjectMapper customMapper = new ObjectMapper()
                .setPropertyNamingStrategy(PropertyNamingStrategies.KEBAB_CASE);

        DummyBean value = new DummyBean(10);
        Response response = Response.builder()
                .objectMapper(customMapper)
                .value(value)
                .build();

        // changing the mapper config should not affect serialization
        customMapper.setPropertyNamingStrategy(PropertyNamingStrategies.UPPER_CAMEL_CASE);

        String expected = "{\"property-with-long-name\":10}";
        assertThat(response.getJsonNode().toString()).isEqualTo(expected);
        assertThat(response.toString()).contains("JSON = " + expected);
    }

    @Test
    void successFactoryMethod() {
        Response response = Response.success();

        assertThat(response).isNotNull();
        assertThat(response.getStatus()).isEqualTo(Response.Status.SUCCESS);
    }

    @Test
    void failedFactoryMethod() {
        Response response = Response.failed();

        assertThat(response).isNotNull();
        assertThat(response.getStatus()).isEqualTo(Response.Status.FAILED);
    }

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
}

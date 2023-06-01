package software.amazon.lambda.powertools;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.*;
import software.amazon.lambda.powertools.testutils.AppConfig;
import software.amazon.lambda.powertools.testutils.Infrastructure;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
public class ParametersE2ET {


    private final ObjectMapper objectMapper = new ObjectMapper();

    private Infrastructure infrastructure;
    private String functionName;

    private String appConfigAppName = "powertools-e2e-test-app";
    private String appConfigEnvironment = "e2etest";
    private final AppConfig appConfig;

    public ParametersE2ET() {
        Map<String,String> params = new HashMap<>();
        params.put("key1", "value1");
        params.put("key2", "value2");
        appConfig = new AppConfig("e2eApp", "e2etest", params);
    }
    @BeforeAll
    @Timeout(value = 5, unit = TimeUnit.MINUTES)
    public void setup() {
        infrastructure = Infrastructure.builder()
                .testName(ParametersE2ET.class.getSimpleName())
                .pathToFunction("parameters")
                .appConfig(appConfig)
                .environmentVariables(
                        Stream.of(new String[][]{
                                        {"POWERTOOLS_LOG_LEVEL", "INFO"},
                                        {"POWERTOOLS_SERVICE_NAME", ParametersE2ET.class.getSimpleName()}
                                })
                                .collect(Collectors.toMap(data -> data[0], data -> data[1])))
                .build();
        functionName = infrastructure.deploy();
    }

//    @AfterAll
//    public static void tearDown() {
//        if (infrastructure != null)
//            infrastructure.destroy();
//    }

    @Test
    public void test_getAppConfigValue() {

    }

}

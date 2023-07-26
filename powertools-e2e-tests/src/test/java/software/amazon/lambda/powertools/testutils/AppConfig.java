package software.amazon.lambda.powertools.testutils;

import java.util.Map;

/**
 * Defines configuration used to setup an AppConfig
 * deployment when the infrastructure is rolled out.
 * <p>
 * All fields are non-nullable.
 */
public class AppConfig {
    private String application;
    private String environment;
    private Map<String, String> configurationValues;

    public AppConfig(String application, String environment, Map<String, String> configurationValues) {
        this.application = application;
        this.environment = environment;
        this.configurationValues = configurationValues;
    }

    public String getApplication() {
        return application;
    }

    public String getEnvironment() {
        return environment;
    }

    public Map<String, String> getConfigurationValues() {
        return configurationValues;
    }
}

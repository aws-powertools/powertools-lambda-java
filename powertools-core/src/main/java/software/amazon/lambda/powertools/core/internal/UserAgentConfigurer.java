package software.amazon.lambda.powertools.core.internal;

import com.sun.org.slf4j.internal.Logger;
import com.sun.org.slf4j.internal.LoggerFactory;

import java.io.FileInputStream;
import java.io.IOException;
import java.net.URL;
import java.util.Properties;

import static software.amazon.lambda.powertools.core.internal.SystemWrapper.getenv;

public class UserAgentConfigurer {

    public static final String NA = "NA";
    public static final String VERSION_KEY = "powertools.version";
    public static final String PT_FEATURE_VARIABLE = "${PT_FEATURE}";
    public static final String PT_EXEC_ENV_VARIABLE = "${PT_EXEC_ENV}";
    public static final String VERSION_PROPERTIES_FILENAME = "version.properties";
    public static final String AWS_EXECUTION_ENV = "AWS_EXECUTION_ENV";
    private static final Logger LOG = LoggerFactory.getLogger(UserAgentConfigurer.class);
    private static final String NO_OP = "no-op";
    private static String PT_VERSION = getProjectVersion();
    private static String USER_AGENT_PATTERN = "PT/" + PT_FEATURE_VARIABLE + "/" + PT_VERSION + " PTEnv/" + PT_EXEC_ENV_VARIABLE;

    /**
     * Retrieves the project version from the version.properties file
     *
     * @return the project version
     */
    static String getProjectVersion() {
        return getVersionFromProperties(VERSION_PROPERTIES_FILENAME, VERSION_KEY);
    }


    /**
     * Retrieves the project version from a properties file.
     * The file should be in the resources folder.
     * The version is retrieved from the property with the given key.
     *
     * @param propertyFileName the name of the properties file
     * @param versionKey       the key of the property that contains the version
     * @return the version of the project as configured in the given properties file
     */
    static String getVersionFromProperties(String propertyFileName, String versionKey) {

        URL propertiesFileURI = Thread.currentThread().getContextClassLoader().getResource(propertyFileName);
        if (propertiesFileURI != null) {
            try (FileInputStream fis = new FileInputStream(propertiesFileURI.getPath())) {
                Properties properties = new Properties();
                properties.load(fis);
                String version = properties.getProperty(versionKey);
                if (version != null && !version.isEmpty()) {
                    return version;
                }
            } catch (IOException e) {
                LOG.warn("Unable to read {} file. Using default version.", propertyFileName);
                LOG.debug("Exception:", e);
            }
        }
        return NA;
    }

    /**
     * Retrieves the user agent string for the Powertools for AWS Lambda.
     * It follows the pattern PT/{PT_FEATURE}/{PT_VERSION} PTEnv/{PT_EXEC_ENV}
     * The version of the project is automatically retrieved.
     * The PT_EXEC_ENV is automatically retrieved from the AWS_EXECUTION_ENV environment variable.
     * If it AWS_EXECUTION_ENV is not set, PT_EXEC_ENV defaults to "NA"
     *
     * @param ptFeature a custom feature to be added to the user agent string (e.g. idempotency).
     *                  If null or empty, the default PT_FEATURE is used.
     *                  The default PT_FEATURE is "no-op".
     * @return the user agent string
     */
    public static String getUserAgent(String ptFeature) {

        String awsExecutionEnv = getenv(AWS_EXECUTION_ENV);
        String ptExecEnv = awsExecutionEnv != null ? awsExecutionEnv : NA;
        String userAgent = USER_AGENT_PATTERN.replace(PT_EXEC_ENV_VARIABLE, ptExecEnv);

        if (ptFeature == null || ptFeature.isEmpty()) {
            ptFeature = NO_OP;
        }
        return userAgent
                .replace(PT_FEATURE_VARIABLE, ptFeature)
                .replace(PT_EXEC_ENV_VARIABLE, ptExecEnv);
    }

}

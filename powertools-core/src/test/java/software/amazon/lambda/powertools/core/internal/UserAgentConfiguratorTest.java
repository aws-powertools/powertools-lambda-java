package software.amazon.lambda.powertools.core.internal;

import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;

import java.io.File;
import java.util.regex.Pattern;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mockStatic;
import static software.amazon.lambda.powertools.core.internal.SystemWrapper.getenv;
import static software.amazon.lambda.powertools.core.internal.UserAgentConfigurator.VERSION_KEY;
import static software.amazon.lambda.powertools.core.internal.UserAgentConfigurator.getVersionFromProperties;
import static software.amazon.lambda.powertools.core.internal.UserAgentConfigurator.VERSION_PROPERTIES_FILENAME;
import static software.amazon.lambda.powertools.core.internal.UserAgentConfigurator.AWS_EXECUTION_ENV;



public class UserAgentConfiguratorTest {

    private static final String SEM_VER_PATTERN = "^(0|[1-9]\\d*)\\.(0|[1-9]\\d*)\\.(0|[1-9]\\d*)(?:-((?:0|[1-9]\\d*|\\d*[a-zA-Z-][0-9a-zA-Z-]*)(?:\\.(?:0|[1-9]\\d*|\\d*[a-zA-Z-][0-9a-zA-Z-]*))*))?(?:\\+([0-9a-zA-Z-]+(?:\\.[0-9a-zA-Z-]+)*))?$";
    private static final String VERSION = UserAgentConfigurator.getProjectVersion();


    @Test
    public void testGetVersion() {

        assertThat(VERSION).isNotNull();
        assertThat(VERSION).isNotEmpty();
        assertThat(Pattern.matches(SEM_VER_PATTERN, VERSION)).isTrue();
    }

    @Test
    public void testGetVersionFromProperties_WrongKey() {
        String version = getVersionFromProperties(VERSION_PROPERTIES_FILENAME, "some invalid key");

        assertThat(version).isNotNull();
        assertThat(version).isEqualTo("NA");
    }

    @Test
    public void testGetVersionFromProperties_FileNotExist() {
        String version = getVersionFromProperties("some file", VERSION_KEY);

        assertThat(version).isNotNull();
        assertThat(version).isEqualTo("NA");
    }

    @Test
    public void testGetVersionFromProperties_InvalidFile() {
        File f = new File(Thread.currentThread().getContextClassLoader().getResource("unreadable.properties").getPath());
        f.setReadable(false);

        String version = getVersionFromProperties("unreadable.properties", VERSION_KEY);

        assertThat(version).isEqualTo("NA");
    }

    @Test
    public void testGetVersionFromProperties_EmptyVersion() {
        String version = getVersionFromProperties("test.properties", VERSION_KEY);

        assertThat(version).isEqualTo("NA");
    }

    @Test
    public void testGetUserAgent() {
        String userAgent = UserAgentConfigurator.getUserAgent("test-feature");

        assertThat(userAgent).isNotNull();
        assertThat(userAgent).isEqualTo("PT/test-feature/" + VERSION + " PTEnv/NA");

    }

    @Test
    public void testGetUserAgent_NoFeature() {
        String userAgent = UserAgentConfigurator.getUserAgent("");

        assertThat(userAgent).isNotNull();
        assertThat(userAgent).isEqualTo("PT/no-op/" + VERSION + " PTEnv/NA");
    }

    @Test
    public void testGetUserAgent_NullFeature() {
        String userAgent = UserAgentConfigurator.getUserAgent(null);

        assertThat(userAgent).isNotNull();
        assertThat(userAgent).isEqualTo("PT/no-op/" + VERSION + " PTEnv/NA");
    }

    @Test
    public void testGetUserAgent_SetAWSExecutionEnv() {
        try (MockedStatic<SystemWrapper> mockedSystemWrapper = mockStatic(SystemWrapper.class)) {
            mockedSystemWrapper.when(() -> getenv(AWS_EXECUTION_ENV)).thenReturn("AWS_Lambda_java8");
            String userAgent = UserAgentConfigurator.getUserAgent("test-feature");

            assertThat(userAgent).isNotNull();
            assertThat(userAgent).isEqualTo("PT/test-feature/" + VERSION + " PTEnv/AWS_Lambda_java8");
        }
    }

}

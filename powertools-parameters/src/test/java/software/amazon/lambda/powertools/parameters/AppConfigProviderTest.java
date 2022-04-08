package software.amazon.lambda.powertools.parameters;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junitpioneer.jupiter.SetEnvironmentVariable;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.Mockito;
import software.amazon.awssdk.core.SdkBytes;
import software.amazon.awssdk.services.appconfigdata.AppConfigDataClient;
import software.amazon.awssdk.services.appconfigdata.model.GetLatestConfigurationRequest;
import software.amazon.awssdk.services.appconfigdata.model.GetLatestConfigurationResponse;
import software.amazon.awssdk.services.appconfigdata.model.StartConfigurationSessionRequest;
import software.amazon.awssdk.services.appconfigdata.model.StartConfigurationSessionResponse;
import software.amazon.lambda.powertools.parameters.cache.CacheManager;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.net.HttpURLConnection;
import java.nio.charset.StandardCharsets;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.MockitoAnnotations.openMocks;

public class AppConfigProviderTest {

    @Mock
    AppConfigDataClient client;

    @Captor
    ArgumentCaptor<GetLatestConfigurationRequest> paramCaptor;
    @Captor
    ArgumentCaptor<StartConfigurationSessionRequest> sessionCaptor;

    CacheManager cacheManager;

    AppConfigProvider provider;

    @BeforeEach
    public void init() {
        openMocks(this);
        cacheManager = new CacheManager();
        provider = new AppConfigProvider(cacheManager, client);
    }

    @Test
    @SetEnvironmentVariable(key = "POWERTOOLS_APPCONFIG_EXTENSION", value = "true")
    public void getValue_withExtension() throws IOException {
        AppConfigProvider mockedProvider = Mockito.spy(AppConfigProvider.class);

        HttpURLConnection connection = Mockito.mock(HttpURLConnection.class);
        Mockito.when(connection.getResponseCode()).thenReturn(200);
        Mockito.when(connection.getInputStream()).thenReturn(new ByteArrayInputStream("{\"key\":\"value\"}".getBytes(StandardCharsets.UTF_8)));
        Mockito.when(mockedProvider.connectToExtension("app", "env", "key")).thenReturn(connection);

        String result = mockedProvider.getValue("/app/env/key");
        assertThat(result).isEqualTo("{\"key\":\"value\"}");
    }

    @Test
    public void getValue_withClient() {
        String key = "/app/env/Key1";
        String expectedValue = "Value1";

        StartConfigurationSessionResponse session = StartConfigurationSessionResponse.builder().initialConfigurationToken("fakeToken").build();
        Mockito.when(client.startConfigurationSession(sessionCaptor.capture())).thenReturn(session);

        GetLatestConfigurationResponse response = GetLatestConfigurationResponse.builder().configuration(SdkBytes.fromString(expectedValue, StandardCharsets.UTF_8)).build();
        Mockito.when(client.getLatestConfiguration(paramCaptor.capture())).thenReturn(response);

        String value = provider.getValue(key);

        assertThat(value).isEqualTo(expectedValue);
        assertThat(paramCaptor.getValue().configurationToken()).isEqualTo("fakeToken");
        assertThat(sessionCaptor.getValue().applicationIdentifier()).isEqualTo("app");
        assertThat(sessionCaptor.getValue().environmentIdentifier()).isEqualTo("env");
        assertThat(sessionCaptor.getValue().configurationProfileIdentifier()).isEqualTo("Key1");
    }

    @Test
    public void invalidKey() {
        String key = "keyWithoutAppEnvConfig";
        assertThatThrownBy(() -> {provider.getValue(key); })
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Your key is incorrect, please specify an 'application', an 'environment' and the 'configuration' separated with '/', eg. '/myapp/prod/myvar'");
    }

    @Test
    public void testExtensionUrl() {
        String extensionUrl = provider.getExtensionUrl("myApp", "prod", "config");
        assertThat(extensionUrl).isEqualTo("http://localhost:2772/applications/myApp/environments/prod/configurations/config");
    }
}

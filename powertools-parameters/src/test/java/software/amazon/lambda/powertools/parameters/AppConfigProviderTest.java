package software.amazon.lambda.powertools.parameters;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
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
import software.amazon.lambda.powertools.parameters.transform.TransformationManager;
import software.amazon.lambda.powertools.parameters.transform.Transformer;

import java.nio.charset.StandardCharsets;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.data.MapEntry.entry;
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
        provider = AppConfigProvider.builder().withCacheManager(cacheManager).withTransformationManager(new TransformationManager()).withClient(client).build();
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
    public void getWithTransformer() {
        String key = "/app/env/Key2";
        String expectedValue = "{\"foo\":\"Foo\", \"bar\":42, \"baz\":123456789}";

        StartConfigurationSessionResponse session = StartConfigurationSessionResponse.builder().initialConfigurationToken("sessionToken").build();
        Mockito.when(client.startConfigurationSession(sessionCaptor.capture())).thenReturn(session);

        GetLatestConfigurationResponse response = GetLatestConfigurationResponse.builder().configuration(SdkBytes.fromString(expectedValue, StandardCharsets.UTF_8)).build();
        Mockito.when(client.getLatestConfiguration(paramCaptor.capture())).thenReturn(response);

        Map<String, Object> map = provider.withTransformation(Transformer.json).get(key, Map.class);
        assertThat(map).contains(
                entry("foo", "Foo"),
                entry("bar", 42),
                entry("baz", 123456789));
        assertThat(paramCaptor.getValue().configurationToken()).isEqualTo("sessionToken");
        assertThat(sessionCaptor.getValue().applicationIdentifier()).isEqualTo("app");
        assertThat(sessionCaptor.getValue().environmentIdentifier()).isEqualTo("env");
        assertThat(sessionCaptor.getValue().configurationProfileIdentifier()).isEqualTo("Key2");
    }

    @Test
    public void invalidKey() {
        String key = "keyWithoutAppEnvConfig";
        assertThatThrownBy(() -> {provider.getValue(key); })
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Your key is incorrect, please specify an 'application', an 'environment' and the 'configuration' separated with '/', eg. '/myapp/prod/myvar'");
    }
}

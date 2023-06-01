package software.amazon.lambda.powertools.parameters;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.Mockito;
import software.amazon.awssdk.core.SdkBytes;
import software.amazon.awssdk.http.urlconnection.UrlConnectionHttpClient;
import software.amazon.awssdk.services.appconfigdata.AppConfigDataClient;
import software.amazon.awssdk.services.appconfigdata.model.GetLatestConfigurationRequest;
import software.amazon.awssdk.services.appconfigdata.model.GetLatestConfigurationResponse;
import software.amazon.awssdk.services.appconfigdata.model.StartConfigurationSessionRequest;
import software.amazon.awssdk.services.appconfigdata.model.StartConfigurationSessionResponse;
import software.amazon.lambda.powertools.parameters.cache.CacheManager;
import static org.assertj.core.api.Assertions.assertThat;

import java.util.Optional;

import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.MockitoAnnotations.openMocks;

public class AppConfigProviderTest {

    @Mock
    AppConfigDataClient client;

//    @Mock
//    GetEnvironmentResponse environment;

    private AppConfigProvider provider;

    @Captor
    ArgumentCaptor<StartConfigurationSessionRequest> startSessionRequestCaptor;

    @Captor
    ArgumentCaptor<GetLatestConfigurationRequest> getLatestConfigurationRequestCaptor;

    @Captor
    ArgumentCaptor<String> getValueForFieldCaptor;

    private final String environmentName = "test";

    private final String applicationName = "fakeApp";

    private final String defaultTestKey = "key1";

    @BeforeEach
    public void init() {
        openMocks(this);

        CacheManager cacheManager = new CacheManager();
        provider = new AppConfigProvider(cacheManager, client, environmentName, applicationName);
    }



    @Test
    public void getValueRetrievesValue() {
        // Arrange
        StartConfigurationSessionResponse firstSession = StartConfigurationSessionResponse.builder()
                .initialConfigurationToken("token1")
                .build();
        GetLatestConfigurationResponse firstResponse = GetLatestConfigurationResponse.builder()
                        .nextPollConfigurationToken("token2")
                        .configuration(SdkBytes.fromUtf8String("value1"))
                        .build();
        GetLatestConfigurationResponse secondResponse = GetLatestConfigurationResponse.builder()
                        .nextPollConfigurationToken("token3")
                                .configuration(SdkBytes.fromUtf8String("value2"))
                                .build();
        GetLatestConfigurationResponse thirdResponse = GetLatestConfigurationResponse.builder()
                        .nextPollConfigurationToken("token4")
                                .build();

        Mockito.when(client.startConfigurationSession(startSessionRequestCaptor.capture()))
                .thenReturn(firstSession);
        Mockito.when(client.getLatestConfiguration(getLatestConfigurationRequestCaptor.capture()))
                .thenReturn(firstResponse, secondResponse, thirdResponse);

        // Act
        String returnedValue1 = provider.getValue(defaultTestKey);
        String returnedValue2 = provider.getValue(defaultTestKey);
        String returnedValue3 = provider.getValue(defaultTestKey);

        // Assert
        assertThat(returnedValue1).isEqualTo(firstResponse.configuration().asUtf8String());
        assertThat(returnedValue2).isEqualTo(secondResponse.configuration().asUtf8String());
        assertThat(returnedValue3).isEqualTo(secondResponse.configuration().asUtf8String()); // Third response is mocked to return null and should re-use previous value
        assertThat(startSessionRequestCaptor.getValue().applicationIdentifier()).isEqualTo(applicationName);
        assertThat(startSessionRequestCaptor.getValue().environmentIdentifier()).isEqualTo(environmentName);
        assertThat(startSessionRequestCaptor.getValue().configurationProfileIdentifier()).isEqualTo(defaultTestKey);
        assertThat(getLatestConfigurationRequestCaptor.getAllValues().get(0).configurationToken()).isEqualTo(firstSession.initialConfigurationToken());
        assertThat(getLatestConfigurationRequestCaptor.getAllValues().get(1).configurationToken()).isEqualTo(firstResponse.nextPollConfigurationToken());

    }


    @Test
    public void stubIntegrationTest() {
        AppConfigDataClient appConfigClient = AppConfigDataClient.builder()
                .httpClientBuilder(UrlConnectionHttpClient.builder())
                .build();

        AppConfigProvider provider = AppConfigProvider.builder()
                .withCacheManager(new CacheManager())
                .withClient(appConfigClient)
                .withApplication("scottsapp")
                .withEnvironment("dev")
                .build();

        String value = provider.get("myfield");
        assertThat(value).isEqualTo("myvalue");
    }

}

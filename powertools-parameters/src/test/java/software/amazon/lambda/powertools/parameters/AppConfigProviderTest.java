package software.amazon.lambda.powertools.parameters;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.Mockito;
import software.amazon.awssdk.services.appconfig.AppConfigClient;
import software.amazon.awssdk.services.appconfig.model.GetEnvironmentRequest;
import software.amazon.awssdk.services.appconfig.model.GetEnvironmentResponse;
import software.amazon.awssdk.services.dynamodb.model.AttributeValue;
import software.amazon.lambda.powertools.parameters.cache.CacheManager;
import org.junit.jupiter.api.Assertions;
import static org.assertj.core.api.Assertions.assertThat;

import java.util.HashMap;
import java.util.Optional;

import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.MockitoAnnotations.openMocks;

public class AppConfigProviderTest {

    @Mock
    AppConfigClient client;

    @Mock
    GetEnvironmentResponse environment;

    private AppConfigProvider provider;

    @Captor
    ArgumentCaptor<GetEnvironmentRequest> getEnvironmentCaptor;

    @Captor
    ArgumentCaptor<String> getValueForFieldCaptor;

    private final String environmentName = "test";

    private final String applicationName = "fakeApp";

    @BeforeEach
    public void init() {
        openMocks(this);

        // getEnvironment gets called in the constructor, so we need to mock it now
        Mockito.when(client.getEnvironment(getEnvironmentCaptor.capture())).thenReturn(environment);

        CacheManager cacheManager = new CacheManager();
        provider = new AppConfigProvider(cacheManager, client, environmentName, applicationName);
    }

    @Test
    public void getEnvironmentReturnsEnvironment() {
        // getEnvironment gets called when we construct the AppConfigProvider. Let's make
        // sure the request included the right environment name

        // Assert
        assertThat(getEnvironmentCaptor.getValue().environmentId()).isEqualTo(environmentName);
    }

    @Test
    public void getValueReturnsValue() {
        // Arrange
        String key = "Key1";
        String expectedValue = "Value1";
        Mockito.when(environment.getValueForField(getValueForFieldCaptor.capture(), eq(String.class)))
                .thenReturn(Optional.of(expectedValue));

        // Act
        String returnedValue = provider.getValue(key);

        // Assert
        assertThat(returnedValue).isEqualTo(expectedValue);
        assertThat(getValueForFieldCaptor.getValue()).isEqualTo(key);
    }

}

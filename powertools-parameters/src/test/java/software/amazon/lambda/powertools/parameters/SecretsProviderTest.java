package software.amazon.lambda.powertools.parameters;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.Mockito;
import software.amazon.awssdk.services.secretsmanager.SecretsManagerClient;
import software.amazon.awssdk.services.secretsmanager.model.GetSecretValueRequest;
import software.amazon.awssdk.services.secretsmanager.model.GetSecretValueResponse;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.mockito.MockitoAnnotations.openMocks;

public class SecretsProviderTest {

    @Mock
    SecretsManagerClient client;

    @Captor
    ArgumentCaptor<GetSecretValueRequest> paramCaptor;

    SecretsProvider provider;

    @BeforeEach
    public void init() {
        openMocks(this);
        provider = new SecretsProvider(client);
    }

    @Test
    public void getValue() {
        String key = "Key1";
        String expectedValue = "Value1";
        GetSecretValueResponse response = GetSecretValueResponse.builder().secretString(expectedValue).build();
        Mockito.when(client.getSecretValue(paramCaptor.capture())).thenReturn(response);

        String value = provider.getValue(key);

        assertEquals(expectedValue, value);
        assertEquals(key, paramCaptor.getValue().secretId());
    }
}

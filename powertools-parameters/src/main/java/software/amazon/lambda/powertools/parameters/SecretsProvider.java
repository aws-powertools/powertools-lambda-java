package software.amazon.lambda.powertools.parameters;

import software.amazon.awssdk.services.secretsmanager.SecretsManagerClient;
import software.amazon.awssdk.services.secretsmanager.model.GetSecretValueRequest;

/**
 * AWS Secrets Manager Parameter Provider
 */
public class SecretsProvider extends BaseProvider {

    private final SecretsManagerClient client;

    public SecretsProvider() {
        this.client = SecretsManagerClient.create();
    }

    public SecretsProvider(SecretsManagerClient client) {
        this.client = client;
    }

    @Override
    String getValue(String key) {
        GetSecretValueRequest request = GetSecretValueRequest.builder().secretId(key).build();

        return client.getSecretValue(request).secretString();
    }
}

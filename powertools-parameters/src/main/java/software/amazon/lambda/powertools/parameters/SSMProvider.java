package software.amazon.lambda.powertools.parameters;

import software.amazon.awssdk.services.ssm.SsmClient;
import software.amazon.awssdk.services.ssm.model.GetParameterRequest;

/**
 * AWS Systems Manager Parameter Store Provider
 */
public class SSMProvider extends BaseProvider {

    private final SsmClient client;
    private boolean decrypt = false;

    public SSMProvider() {
        this(SsmClient.create());
    }

    public SSMProvider(SsmClient ssmClient) {
        this.client = ssmClient;
    }

    public <T extends BaseProvider> BaseProvider withDecrypt(boolean decrypt) {
        this.decrypt = decrypt;
        return this;
    }

    @Override
    String getValue(String key) {
        GetParameterRequest request = GetParameterRequest.builder()
                                        .name(key)
                                        .withDecryption(decrypt)
                                        .build();

        return client.getParameter(request).parameter().value();
    }
}

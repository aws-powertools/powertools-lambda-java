package software.amazon.lambda.powertools.parameters.ssm;

import software.amazon.awssdk.core.SdkSystemSetting;
import software.amazon.awssdk.core.client.config.ClientOverrideConfiguration;
import software.amazon.awssdk.core.client.config.SdkAdvancedClientOption;
import software.amazon.awssdk.http.urlconnection.UrlConnectionHttpClient;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.ssm.SsmClient;
import software.amazon.lambda.powertools.common.internal.UserAgentConfigurator;
import software.amazon.lambda.powertools.parameters.BaseProvider;
import software.amazon.lambda.powertools.parameters.cache.CacheManager;
import software.amazon.lambda.powertools.parameters.transform.TransformationManager;

public class SSMProviderBuilder {
    private SsmClient client;
    private CacheManager cacheManager;
    private TransformationManager transformationManager;

    private static SsmClient createClient() {
        return SsmClient.builder()
                .httpClientBuilder(UrlConnectionHttpClient.builder())
                .region(Region.of(System.getenv(SdkSystemSetting.AWS_REGION.environmentVariable())))
                .overrideConfiguration(ClientOverrideConfiguration.builder()
                        .putAdvancedOption(SdkAdvancedClientOption.USER_AGENT_SUFFIX,
                                UserAgentConfigurator.getUserAgent(BaseProvider.PARAMETERS)).build())
                .build();
    }

    /**
     * Create a {@link SSMProvider} instance.
     *
     * @return a {@link SSMProvider}
     */
    public SSMProvider build() {
        if (cacheManager == null) {
            // TODO - do we want to share this somehow?
            cacheManager = new CacheManager();
        }
        SSMProvider provider;
        if (client == null) {
            client = createClient();
        }

        provider = new SSMProvider(cacheManager, transformationManager, client);

        return provider;
    }

    /**
     * Set custom {@link SsmClient} to pass to the {@link SSMProvider}. <br/>
     * Use it if you want to customize the region or any other part of the client.
     *
     * @param client Custom client
     * @return the builder to chain calls (eg. <pre>builder.withClient().build()</pre>)
     */
    public SSMProviderBuilder withClient(SsmClient client) {
        this.client = client;
        return this;
    }

    /**
     * <b>Mandatory</b>. Provide a CacheManager to the {@link SSMProvider}
     *
     * @param cacheManager the manager that will handle the cache of parameters
     * @return the builder to chain calls (eg. <pre>builder.withCacheManager().build()</pre>)
     */
    public SSMProviderBuilder withCacheManager(CacheManager cacheManager) {
        this.cacheManager = cacheManager;
        return this;
    }

    /**
     * Provide a transformationManager to the {@link SSMProvider}
     *
     * @param transformationManager the manager that will handle transformation of parameters
     * @return the builder to chain calls (eg. <pre>builder.withTransformationManager().build()</pre>)
     */
    public SSMProviderBuilder withTransformationManager(TransformationManager transformationManager) {
        this.transformationManager = transformationManager;
        return this;
    }
}

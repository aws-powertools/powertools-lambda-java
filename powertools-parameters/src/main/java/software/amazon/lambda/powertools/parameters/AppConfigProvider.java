package software.amazon.lambda.powertools.parameters;

import software.amazon.awssdk.auth.credentials.EnvironmentVariableCredentialsProvider;
import software.amazon.awssdk.core.SdkSystemSetting;
import software.amazon.awssdk.http.urlconnection.UrlConnectionHttpClient;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.appconfig.AppConfigClient;
import software.amazon.lambda.powertools.parameters.cache.CacheManager;
import software.amazon.lambda.powertools.parameters.transform.TransformationManager;

import java.util.Map;

public class AppConfigProvider extends BaseProvider{

    private final AppConfigClient client;

    public AppConfigProvider(CacheManager cacheManager) {
        this(cacheManager, AppConfigClient.builder()
                        .httpClientBuilder(UrlConnectionHttpClient.builder())
                        .credentialsProvider(EnvironmentVariableCredentialsProvider.create())
                        .region(Region.of(System.getenv(SdkSystemSetting.AWS_REGION.environmentVariable())))
                        .build()
        );

    }

    AppConfigProvider(CacheManager cacheManager, AppConfigClient client) {
        super(cacheManager);
        this.client = client;
    }


    @Override
    protected String getValue(String key) {
        throw new RuntimeException("Not implemented");
    }

    @Override
    protected Map<String, String> getMultipleValues(String path) {
        throw new RuntimeException("Not implemented");
    }

    /**
     * Create a builder that can be used to configure and create a {@link AppConfigProvider}.
     *
     * @return a new instance of {@link AppConfigProvider.Builder}
     */
    public static AppConfigProvider.Builder builder() {
        return new AppConfigProvider.Builder();
    }

    static class Builder {
        private AppConfigClient client;
        private CacheManager cacheManager;
        private TransformationManager transformationManager;

        /**
         * Create a {@link AppConfigProvider} instance.
         *
         * @return a {@link AppConfigProvider}
         */
        public AppConfigProvider build() {
            if (cacheManager == null) {
                throw new IllegalStateException("No CacheManager provided; please provide one");
            }
            AppConfigProvider provider;
            if (client != null) {
                provider = new AppConfigProvider(cacheManager, client);
            } else {
                provider = new AppConfigProvider(cacheManager);
            }
            if (transformationManager != null) {
                provider.setTransformationManager(transformationManager);
            }
            return provider;
        }

        /**
         * Set custom {@link AppConfigProvider} to pass to the {@link AppConfigClient}. <br/>
         * Use it if you want to customize the region or any other part of the client.
         *
         * @param client Custom client
         * @return the builder to chain calls (eg. <pre>builder.withClient().build()</pre>)
         */
        public AppConfigProvider.Builder withClient(AppConfigClient client) {
            this.client = client;
            return this;
        }

        /**
         * <b>Mandatory</b>. Provide a CacheManager to the {@link AppConfigProvider}
         *
         * @param cacheManager the manager that will handle the cache of parameters
         * @return the builder to chain calls (eg. <pre>builder.withCacheManager().build()</pre>)
         */
        public AppConfigProvider.Builder withCacheManager(CacheManager cacheManager) {
            this.cacheManager = cacheManager;
            return this;
        }

        /**
         * <b>Mandatory</b>. Provide a DynamoDB table to the {@link AppConfigProvider}
         *
         * @param table the table that parameters will be retrieved from.
         * @return the builder to chain calls (eg. <pre>builder.withTable().build()</pre>)
         */
        public AppConfigProvider.Builder withTable(String table) {
            return this;
        }

        /**
         * Provide a transformationManager to the {@link AppConfigProvider}
         *
         * @param transformationManager the manager that will handle transformation of parameters
         * @return the builder to chain calls (eg. <pre>builder.withTransformationManager().build()</pre>)
         */
        public AppConfigProvider.Builder withTransformationManager(TransformationManager transformationManager) {
            this.transformationManager = transformationManager;
            return this;
        }
    }
}

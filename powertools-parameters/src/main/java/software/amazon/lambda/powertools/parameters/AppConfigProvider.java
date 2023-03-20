package software.amazon.lambda.powertools.parameters;

import software.amazon.awssdk.auth.credentials.EnvironmentVariableCredentialsProvider;
import software.amazon.awssdk.core.SdkSystemSetting;
import software.amazon.awssdk.http.urlconnection.UrlConnectionHttpClient;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.appconfig.AppConfigClient;
import software.amazon.awssdk.services.appconfig.model.GetEnvironmentRequest;
import software.amazon.awssdk.services.appconfig.model.GetEnvironmentResponse;
import software.amazon.lambda.powertools.parameters.cache.CacheManager;
import software.amazon.lambda.powertools.parameters.transform.TransformationManager;

import java.util.Map;
import java.util.Optional;

public class AppConfigProvider extends BaseProvider{

    private final AppConfigClient client;
    private final String environment;
    private final String application;

    public AppConfigProvider(CacheManager cacheManager, String environment, String application) {
        this(cacheManager, AppConfigClient.builder()
                        .httpClientBuilder(UrlConnectionHttpClient.builder())
                        .credentialsProvider(EnvironmentVariableCredentialsProvider.create())
                        .region(Region.of(System.getenv(SdkSystemSetting.AWS_REGION.environmentVariable())))
                        .build(),
                environment, application);
    }

    AppConfigProvider(CacheManager cacheManager, AppConfigClient client, String environment, String application) {
        super(cacheManager);
        this.client = client;
        this.environment = environment;
        this.application = application;
    }


    @Override
    protected String getValue(String key) {
        GetEnvironmentResponse env = client.getEnvironment(GetEnvironmentRequest.builder()
                        .environmentId(environment)
                        .applicationId(application)
                .build());

        Optional<String> val = env.getValueForField(key, String.class);
        return val.orElse(null);
    }

    @Override
    protected Map<String, String> getMultipleValues(String path) {
        // Retrieving multiple values is not supported with the AppConfig provider.
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
        private String environment;
        private String application;

        /**
         * Create a {@link AppConfigProvider} instance.
         *
         * @return a {@link AppConfigProvider}
         */
        public AppConfigProvider build() {
            if (cacheManager == null) {
                throw new IllegalStateException("No CacheManager provided; please provide one");
            }
            if (environment == null) {
                throw new IllegalStateException("No environment provided; please provide one");
            }
            if (application == null) {
                throw new IllegalStateException("No application provided; please provide one");
            }

            AppConfigProvider provider;
            if (client != null) {
                provider = new AppConfigProvider(cacheManager, client, environment, application);
            } else {
                provider = new AppConfigProvider(cacheManager, environment, application);
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
         * <b>Mandatory</b>. Provide an environment to the {@link AppConfigProvider}
         *
         * @param environment the AppConfig environment
         * @return the builder to chain calls (eg. <pre>builder.withCacheManager().build()</pre>)
         */
        public AppConfigProvider.Builder withEnvironment(String environment) {
            this.environment = environment;
            return this;
        }

        /**
         * <b>Mandatory</b>. Provide a CacheManager to the {@link AppConfigProvider}
         *
         * @param application the application to pull configuration from
         * @return the builder to chain calls (eg. <pre>builder.withCacheManager().build()</pre>)
         */
        public AppConfigProvider.Builder withApplication(String application) {
            this.application = application;
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

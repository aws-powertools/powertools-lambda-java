package software.amazon.lambda.powertools.parameters;

import software.amazon.awssdk.auth.credentials.EnvironmentVariableCredentialsProvider;
import software.amazon.awssdk.core.SdkSystemSetting;
import software.amazon.awssdk.http.urlconnection.UrlConnectionHttpClient;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.appconfigdata.AppConfigDataClient;
import software.amazon.awssdk.services.appconfigdata.model.GetLatestConfigurationRequest;
import software.amazon.awssdk.services.appconfigdata.model.GetLatestConfigurationResponse;
import software.amazon.awssdk.services.appconfigdata.model.StartConfigurationSessionRequest;
import software.amazon.lambda.powertools.parameters.cache.CacheManager;
import software.amazon.lambda.powertools.parameters.transform.TransformationManager;

import java.util.HashMap;
import java.util.Map;

public class AppConfigProvider extends BaseProvider{

    private static class EstablishedSession {
        private final String nextSessionToken;
        private final String lastConfigurationValue;

        private EstablishedSession(String nextSessionToken, String value) {
            this.nextSessionToken = nextSessionToken;
            this.lastConfigurationValue = value;
        }

        public String getNextSessionToken() {
            return nextSessionToken;
        }

        public String getLastConfigurationValue() {
            return lastConfigurationValue;
        }
    }

    private final AppConfigDataClient client;

    private final String application;

    private final String environment;

    private final HashMap<String, EstablishedSession> establishedSessions = new HashMap<>();

    public AppConfigProvider(CacheManager cacheManager, String environment, String application) {
        this(cacheManager, AppConfigDataClient.builder()
                        .httpClientBuilder(UrlConnectionHttpClient.builder())
                        .credentialsProvider(EnvironmentVariableCredentialsProvider.create())
                        .region(Region.of(System.getenv(SdkSystemSetting.AWS_REGION.environmentVariable())))
                        .build(),
                environment, application);
    }

    AppConfigProvider(CacheManager cacheManager, AppConfigDataClient client, String environment, String application) {
        super(cacheManager);
        this.client = client;
        this.application = application;
        this.environment = environment;
    }


    @Override
    protected String getValue(String key) {
        // Start a configuration session if we don't already have one to get the initial token
        EstablishedSession establishedSession = establishedSessions.getOrDefault(key, null);
        String sessionToken = establishedSession != null?
                establishedSession.nextSessionToken :
                client.startConfigurationSession(StartConfigurationSessionRequest.builder()
                            .applicationIdentifier(this.application)
                            .environmentIdentifier(this.environment)
                            .configurationProfileIdentifier(key)
                            .build())
                    .initialConfigurationToken();

        // Get the configuration
        GetLatestConfigurationResponse response = client.getLatestConfiguration(GetLatestConfigurationRequest.builder()
                        .configurationToken(sessionToken)
                .build());

        // Get the next token
        String nextSessionToken = response.nextPollConfigurationToken();

        // Get the value
        String value = response.configuration() != null?
                response.configuration().asUtf8String() : // if we have a new value, use it
                    establishedSession != null?
                            establishedSession.lastConfigurationValue : // if we don't but we have a previous value, use that
                            null; // otherwise we've got no value

        // Update the cache so we can get the next value later
        establishedSessions.put(key, new EstablishedSession(nextSessionToken, value));

        return value;
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
        private AppConfigDataClient client;
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
         * Set custom {@link AppConfigProvider} to pass to the {@link AppConfigDataClient}. <br/>
         * Use it if you want to customize the region or any other part of the client.
         *
         * @param client Custom client
         * @return the builder to chain calls (eg. <pre>builder.withClient().build()</pre>)
         */
        public AppConfigProvider.Builder withClient(AppConfigDataClient client) {
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

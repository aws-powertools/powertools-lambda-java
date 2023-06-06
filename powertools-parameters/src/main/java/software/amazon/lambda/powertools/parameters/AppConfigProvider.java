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

/**
 * Implements a {@link ParamProvider} on top of the AppConfig service. AppConfig provides
 * a mechanism to retrieve and update configuration of applications over time.
 * AppConfig requires the user to create an application, environment, and configuration profile.
 * The configuration profile's value can then retrieved, by key name, through this provider.
 *
 * Because AppConfig is designed to handle rollouts of configuration over time, we must first
 * establish a session for each key we wish to retrieve, and then poll the session for the latest
 * value when the user re-requests it. This means we must hold a keyed set of session tokens
 * and values.
 *
 * @see <a href="https://awslabs.github.io/aws-lambda-powertools-java/utilities/parameters">Parameters provider documentation</a>
 * @see <a href="https://docs.aws.amazon.com/appconfig/latest/userguide/appconfig-working.html">AppConfig documentation</a>
 */
public class AppConfigProvider extends BaseProvider{

    private static class EstablishedSession {
        private final String nextSessionToken;
        private final String lastConfigurationValue;

        private EstablishedSession(String nextSessionToken, String value) {
            this.nextSessionToken = nextSessionToken;
            this.lastConfigurationValue = value;
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


    /**
     * Retrieve the parameter value from the AppConfig parameter store.<br />
     *
     * @param key key of the parameter. This ties back to AppConfig's 'profile' concept
     * @return the value of the parameter identified by the key
     */
    @Override
    protected String getValue(String key) {
        // Start a configuration session if we don't already have one for the key requested
        // so that we can the initial token. If we already have a session, we can take
        // the next request token from there.
        EstablishedSession establishedSession = establishedSessions.getOrDefault(key, null);
        String sessionToken = establishedSession != null?
                establishedSession.nextSessionToken :
                client.startConfigurationSession(StartConfigurationSessionRequest.builder()
                            .applicationIdentifier(this.application)
                            .environmentIdentifier(this.environment)
                            .configurationProfileIdentifier(key)
                            .build())
                    .initialConfigurationToken();

        // Get the configuration using the token
        GetLatestConfigurationResponse response = client.getLatestConfiguration(GetLatestConfigurationRequest.builder()
                        .configurationToken(sessionToken)
                .build());

        // Get the next session token we'll use next time we are asked for this key
        String nextSessionToken = response.nextPollConfigurationToken();

        // Get the value of the key. Note that AppConfig will return null if the value
        // has not changed since we last asked for it in this session - in this case
        // we return the value we stashed at last request.
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
         * <b>Mandatory</b>. Provide an application to the {@link AppConfigProvider}
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

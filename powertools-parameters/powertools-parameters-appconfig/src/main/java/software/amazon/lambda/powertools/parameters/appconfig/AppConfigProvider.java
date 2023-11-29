/*
 * Copyright 2023 Amazon.com, Inc. or its affiliates.
 * Licensed under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *     http://www.apache.org/licenses/LICENSE-2.0
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 */

package software.amazon.lambda.powertools.parameters.appconfig;

import java.util.HashMap;
import java.util.Map;
import software.amazon.awssdk.services.appconfigdata.AppConfigDataClient;
import software.amazon.awssdk.services.appconfigdata.model.GetLatestConfigurationRequest;
import software.amazon.awssdk.services.appconfigdata.model.GetLatestConfigurationResponse;
import software.amazon.awssdk.services.appconfigdata.model.StartConfigurationSessionRequest;
import software.amazon.lambda.powertools.parameters.BaseProvider;
import software.amazon.lambda.powertools.parameters.ParamProvider;
import software.amazon.lambda.powertools.parameters.cache.CacheManager;
import software.amazon.lambda.powertools.parameters.transform.TransformationManager;

/**
 * Implements a {@link ParamProvider} on top of the AppConfig service. AppConfig provides
 * a mechanism to retrieve and update configuration of applications over time.
 * AppConfig requires the user to create an application, environment, and configuration profile.
 * The configuration profile's value can then be retrieved, by key name, through this provider.
 * <p>
 * Because AppConfig is designed to handle rollouts of configuration over time, we must first
 * establish a session for each key we wish to retrieve, and then poll the session for the latest
 * value when the user re-requests it. This means we must hold a keyed set of session tokens
 * and values.
 *
 * @see <a href="https://docs.powertools.aws.dev/lambda/java/utilities/parameters/">Parameters provider documentation</a>
 * @see <a href="https://docs.aws.amazon.com/appconfig/latest/userguide/appconfig-working.html">AppConfig documentation</a>
 */
public class AppConfigProvider extends BaseProvider {

    private final AppConfigDataClient client;
    private final String application;
    private final String environment;
    private final HashMap<String, EstablishedSession> establishedSessions = new HashMap<>();

    AppConfigProvider(CacheManager cacheManager, TransformationManager transformationManager,
                      AppConfigDataClient client, String environment, String application) {
        super(cacheManager, transformationManager);
        this.client = client;
        this.application = application;
        this.environment = environment;
    }

    /**
     * Create a builder that can be used to configure and create a {@link AppConfigProvider}.
     *
     * @return a new instance of {@link AppConfigProviderBuilder}
     */
    public static AppConfigProviderBuilder builder() {
        return new AppConfigProviderBuilder();
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
        String sessionToken = establishedSession != null ?
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
        String value = response.configuration() != null ?
                response.configuration().asUtf8String() : // if we have a new value, use it
                establishedSession != null ?
                        establishedSession.lastConfigurationValue :
                        // if we don't but we have a previous value, use that
                        null; // otherwise we've got no value

        // Update the cache so we can get the next value later
        establishedSessions.put(key, new EstablishedSession(nextSessionToken, value));

        return value;
    }

    @Override
    protected Map<String, String> getMultipleValues(String path) {
        // Retrieving multiple values is not supported with the AppConfig provider.
        throw new RuntimeException(
                "Retrieving multiple parameter values is not supported with the AWS App Config Provider");
    }

    private static class EstablishedSession {
        private final String nextSessionToken;
        private final String lastConfigurationValue;

        private EstablishedSession(String nextSessionToken, String value) {
            this.nextSessionToken = nextSessionToken;
            this.lastConfigurationValue = value;
        }
    }

}

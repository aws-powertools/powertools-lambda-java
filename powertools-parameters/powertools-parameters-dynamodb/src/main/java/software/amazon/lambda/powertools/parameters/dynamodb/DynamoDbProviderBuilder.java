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

package software.amazon.lambda.powertools.parameters.dynamodb;

import java.time.temporal.ChronoUnit;
import software.amazon.awssdk.core.SdkSystemSetting;
import software.amazon.awssdk.core.client.config.ClientOverrideConfiguration;
import software.amazon.awssdk.core.client.config.SdkAdvancedClientOption;
import software.amazon.awssdk.http.urlconnection.UrlConnectionHttpClient;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.dynamodb.DynamoDbClient;
import software.amazon.lambda.powertools.common.internal.UserAgentConfigurator;
import software.amazon.lambda.powertools.parameters.BaseProvider;
import software.amazon.lambda.powertools.parameters.cache.CacheManager;
import software.amazon.lambda.powertools.parameters.transform.TransformationManager;

public class DynamoDbProviderBuilder {
    private DynamoDbClient client;
    private String table;
    private CacheManager cacheManager;
    private TransformationManager transformationManager;

    static DynamoDbClient createClient() {
        return DynamoDbClient.builder()
                .httpClientBuilder(UrlConnectionHttpClient.builder())
                .region(Region.of(System.getenv(SdkSystemSetting.AWS_REGION.environmentVariable())))
                .overrideConfiguration(ClientOverrideConfiguration.builder()
                        .putAdvancedOption(SdkAdvancedClientOption.USER_AGENT_SUFFIX,
                                UserAgentConfigurator.getUserAgent(BaseProvider.PARAMETERS)).build())
                .build();
    }

    /**
     * Create a {@link DynamoDbProvider} instance.
     *
     * @return a {@link DynamoDbProvider}
     */
    public DynamoDbProvider build() {
        if (cacheManager == null) {
            cacheManager = new CacheManager();
        }
        if (table == null) {
            throw new IllegalStateException("No DynamoDB table name provided; please provide one");
        }
        DynamoDbProvider provider;
        if (client == null) {
            client = createClient();
        }
        provider = new DynamoDbProvider(cacheManager, transformationManager, client, table);

        return provider;
    }

    /**
     * Set custom {@link DynamoDbClient} to pass to the {@link DynamoDbClient}. <br/>
     * Use it if you want to customize the region or any other part of the client.
     *
     * @param client Custom client
     * @return the builder to chain calls (eg. <pre>builder.withClient().build()</pre>)
     */
    public DynamoDbProviderBuilder withClient(DynamoDbClient client) {
        this.client = client;
        return this;
    }

    /**
     * <b>Mandatory</b>. Provide a CacheManager to the {@link DynamoDbProvider}
     *
     * @param cacheManager the manager that will handle the cache of parameters
     * @return the builder to chain calls (eg. <pre>builder.withCacheManager().build()</pre>)
     */
    public DynamoDbProviderBuilder withCacheManager(CacheManager cacheManager) {
        this.cacheManager = cacheManager;
        return this;
    }

    /**
     * <b>Mandatory</b>. Provide a DynamoDB table to the {@link DynamoDbProvider}
     *
     * @param table the table that parameters will be retrieved from.
     * @return the builder to chain calls (eg. <pre>builder.withTable().build()</pre>)
     */
    public DynamoDbProviderBuilder withTable(String table) {
        this.table = table;
        return this;
    }

    /**
     * Provide a transformationManager to the {@link DynamoDbProvider}
     *
     * @param transformationManager the manager that will handle transformation of parameters
     * @return the builder to chain calls (eg. <pre>builder.withTransformationManager().build()</pre>)
     */
    public DynamoDbProviderBuilder withTransformationManager(TransformationManager transformationManager) {
        this.transformationManager = transformationManager;
        return this;
    }
}

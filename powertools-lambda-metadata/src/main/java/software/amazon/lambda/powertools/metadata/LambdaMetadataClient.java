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

package software.amazon.lambda.powertools.metadata;

import java.util.concurrent.atomic.AtomicReference;

import org.crac.Core;
import org.crac.Resource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import software.amazon.lambda.powertools.common.internal.ClassPreLoader;
import software.amazon.lambda.powertools.metadata.exception.LambdaMetadataException;
import software.amazon.lambda.powertools.metadata.internal.LambdaMetadataHttpClient;

/**
 * Client for accessing Lambda execution environment metadata.
 * <p>
 * This utility provides idiomatic access to the Lambda Metadata Endpoint (LMDS),
 * eliminating boilerplate code for retrieving execution environment metadata
 * like Availability Zone ID.
 * </p>
 * <p>
 * Features:
 * <ul>
 *   <li>Automatic caching for the sandbox lifetime</li>
 *   <li>Thread-safe access for concurrent executions</li>
 *   <li>SnapStart cache invalidation via CRaC</li>
 *   <li>Lazy loading on first access</li>
 * </ul>
 * </p>
 *
 * <h2>Basic Usage</h2>
 * <pre>{@code
 * public String handleRequest(Object input, Context context) {
 *     LambdaMetadata metadata = LambdaMetadataClient.get();
 *     String azId = metadata.getAvailabilityZoneId();
 *     return "{\"az\": \"" + azId + "\"}";
 * }
 * }</pre>
 *
 * <h2>Eager Loading</h2>
 * <pre>{@code
 * public class MyHandler implements RequestHandler<Object, String> {
 *     // Fetch during cold start
 *     private static final LambdaMetadata METADATA = LambdaMetadataClient.get();
 *
 *     public String handleRequest(Object input, Context context) {
 *         return "{\"az\": \"" + METADATA.getAvailabilityZoneId() + "\"}";
 *     }
 * }
 * }</pre>
 *
 * @see LambdaMetadata
 */
public final class LambdaMetadataClient implements Resource {

    private static final Logger LOG = LoggerFactory.getLogger(LambdaMetadataClient.class);

    private static final AtomicReference<LambdaMetadata> cachedInstance = new AtomicReference<>();
    private static final LambdaMetadataClient CRAC_INSTANCE = new LambdaMetadataClient();
    private static LambdaMetadataHttpClient httpClient = new LambdaMetadataHttpClient();

    // Register with CRaC for SnapStart cache invalidation
    static {
        Core.getGlobalContext().register(CRAC_INSTANCE);
    }

    private LambdaMetadataClient() {
        // Utility class
    }

    /**
     * Retrieves the cached metadata, fetching from the endpoint if not cached.
     * <p>
     * This method is thread-safe and handles concurrent access correctly.
     * The first call fetches metadata from the Lambda Metadata Endpoint,
     * subsequent calls return the cached value.
     * </p>
     *
     * @return the LambdaMetadata instance
     * @throws LambdaMetadataException if the metadata endpoint is unavailable or returns an error
     */
    public static LambdaMetadata get() {
        LambdaMetadata instance = cachedInstance.get();
        if (instance != null) {
            return instance;
        }

        LambdaMetadata newInstance = httpClient.fetchMetadata();
        // Use compareAndSet to handle race conditions - first writer wins
        if (cachedInstance.compareAndSet(null, newInstance)) {
            LOG.debug("Lambda metadata fetched and cached: availabilityZoneId={}",
                    newInstance.getAvailabilityZoneId());
            return newInstance;
        }
        // Another thread won the race, return their instance
        return cachedInstance.get();
    }

    /**
     * Forces a refresh of the cached metadata.
     * <p>
     * This method clears the cache and fetches fresh metadata from the endpoint.
     * Use this only for advanced use cases where you need to force a refresh.
     * </p>
     *
     * @return the refreshed LambdaMetadata instance
     * @throws LambdaMetadataException if the metadata endpoint is unavailable or returns an error
     */
    public static LambdaMetadata refresh() {
        cachedInstance.set(null);
        return get();
    }

    /**
     * Called before a CRaC checkpoint is taken.
     * Preloads classes to ensure faster restore.
     *
     * @param context the CRaC context
     */
    @Override
    public void beforeCheckpoint(org.crac.Context<? extends Resource> context) {
        // Preload classes for faster restore
        ClassPreLoader.preloadClasses();
        LOG.debug("Lambda metadata: beforeCheckpoint completed");
    }

    /**
     * Called after a CRaC restore.
     * Invalidates the cache since the AZ may have changed after cross-AZ restore.
     *
     * @param context the CRaC context
     */
    @Override
    public void afterRestore(org.crac.Context<? extends Resource> context) {
        resetCache();
        LOG.debug("Lambda metadata: cache invalidated after SnapStart restore");
    }

    /**
     * Sets the HTTP client (for testing purposes only).
     *
     * @param client the client to use
     */
    static void setHttpClient(LambdaMetadataHttpClient client) {
        httpClient = client;
        cachedInstance.set(null);
    }

    /**
     * Resets the cached instance (for testing purposes only).
     */
    static void resetCache() {
        cachedInstance.set(null);
    }
}

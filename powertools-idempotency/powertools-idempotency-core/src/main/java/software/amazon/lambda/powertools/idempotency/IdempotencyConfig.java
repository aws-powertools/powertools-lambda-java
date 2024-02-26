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

package software.amazon.lambda.powertools.idempotency;

import com.amazonaws.services.lambda.runtime.Context;
import java.time.Duration;
import software.amazon.lambda.powertools.idempotency.internal.cache.LRUCache;

/**
 * Configuration of the idempotency feature. Use the {@link Builder} to create an instance.
 */
public class IdempotencyConfig {
    private final int localCacheMaxItems;
    private final boolean useLocalCache;
    private final long expirationInSeconds;
    private final String eventKeyJMESPath;
    private final String payloadValidationJMESPath;
    private final boolean throwOnNoIdempotencyKey;
    private final String hashFunction;
    private Context lambdaContext;

    private IdempotencyConfig(String eventKeyJMESPath, String payloadValidationJMESPath,
                              boolean throwOnNoIdempotencyKey, boolean useLocalCache, int localCacheMaxItems,
                              long expirationInSeconds, String hashFunction) {
        this.localCacheMaxItems = localCacheMaxItems;
        this.useLocalCache = useLocalCache;
        this.expirationInSeconds = expirationInSeconds;
        this.eventKeyJMESPath = eventKeyJMESPath;
        this.payloadValidationJMESPath = payloadValidationJMESPath;
        this.throwOnNoIdempotencyKey = throwOnNoIdempotencyKey;
        this.hashFunction = hashFunction;
    }

    /**
     * Create a builder that can be used to configure and create a {@link IdempotencyConfig}.
     *
     * @return a new instance of {@link Builder}
     */
    public static Builder builder() {
        return new Builder();
    }

    public int getLocalCacheMaxItems() {
        return localCacheMaxItems;
    }

    public boolean useLocalCache() {
        return useLocalCache;
    }

    public long getExpirationInSeconds() {
        return expirationInSeconds;
    }

    public String getEventKeyJMESPath() {
        return eventKeyJMESPath;
    }

    public String getPayloadValidationJMESPath() {
        return payloadValidationJMESPath;
    }

    public boolean throwOnNoIdempotencyKey() {
        return throwOnNoIdempotencyKey;
    }

    public String getHashFunction() {
        return hashFunction;
    }

    public Context getLambdaContext() {
        return lambdaContext;
    }

    public void setLambdaContext(Context lambdaContext) {
        this.lambdaContext = lambdaContext;
    }

    public static class Builder {

        private int localCacheMaxItems = 256;
        private boolean useLocalCache = false;
        private long expirationInSeconds = 60 * 60L; // 1 hour
        private String eventKeyJMESPath;
        private String payloadValidationJMESPath;
        private boolean throwOnNoIdempotencyKey = false;
        private String hashFunction = "MD5";

        /**
         * Initialize and return an instance of {@link IdempotencyConfig}.<br>
         * Example:<br>
         * <pre>
         * IdempotencyConfig.builder().withUseLocalCache().build();
         * </pre>
         * This instance must then be passed to the {@link Idempotency.Config}:
         * <pre>
         * Idempotency.config().withConfig(config).configure();
         * </pre>
         *
         * @return an instance of {@link IdempotencyConfig}.
         */
        public IdempotencyConfig build() {
            return new IdempotencyConfig(
                    eventKeyJMESPath,
                    payloadValidationJMESPath,
                    throwOnNoIdempotencyKey,
                    useLocalCache,
                    localCacheMaxItems,
                    expirationInSeconds,
                    hashFunction);
        }

        /**
         * A JMESPath expression to extract the idempotency key from the event record. <br>
         * See <a href="https://jmespath.org/">https://jmespath.org/</a> for more details.<br>
         * Common paths are: <ul>
         * <li><code>powertools_json(body)</code> for APIGatewayProxyRequestEvent and APIGatewayV2HTTPEvent</li>
         * <li><code>Records[*].powertools_json(body)</code> for SQSEvent</li>
         * <li><code>Records[0].Sns.Message | powertools_json(@)</code> for SNSEvent</li>
         * <li><code>detail</code> for ScheduledEvent (EventBridge / CloudWatch events)</li>
         * <li><code>Records[*].kinesis.powertools_json(powertools_base64(data))</code> for KinesisEvent</li>
         * <li><code>Records[*].powertools_json(powertools_base64(data))</code> for KinesisFirehoseEvent</li>
         * <li>...</li>
         * </ul>
         *
         * @param eventKeyJMESPath path of the key in the Lambda event
         * @return the instance of the builder (to chain operations)
         */
        public Builder withEventKeyJMESPath(String eventKeyJMESPath) {
            this.eventKeyJMESPath = eventKeyJMESPath;
            return this;
        }

        /**
         * Set the maximum number of items to store in local cache, by default 256
         *
         * @param localCacheMaxItems maximum number of items to store in local cache
         * @return the instance of the builder (to chain operations)
         */
        public Builder withLocalCacheMaxItems(int localCacheMaxItems) {
            this.localCacheMaxItems = localCacheMaxItems;
            return this;
        }

        /**
         * Whether to locally cache idempotency results, by default false
         *
         * @param useLocalCache boolean that indicate if a local cache must be used in addition to the persistence store.
         *                      If set to true, will use the {@link LRUCache}
         * @return the instance of the builder (to chain operations)
         */
        public Builder withUseLocalCache(boolean useLocalCache) {
            this.useLocalCache = useLocalCache;
            return this;
        }

        /**
         * The number of seconds to wait before a record is expired
         *
         * @param expiration expiration of the record in the store
         * @return the instance of the builder (to chain operations)
         */
        public Builder withExpiration(Duration expiration) {
            this.expirationInSeconds = expiration.getSeconds();
            return this;
        }

        /**
         * A JMESPath expression to extract the payload to be validated from the event record. <br/>
         * See <a href="https://jmespath.org/">https://jmespath.org/</a> for more details.
         *
         * @param payloadValidationJMESPath JMES Path of a part of the payload to be used for validation
         * @return the instance of the builder (to chain operations)
         */
        public Builder withPayloadValidationJMESPath(String payloadValidationJMESPath) {
            this.payloadValidationJMESPath = payloadValidationJMESPath;
            return this;
        }

        /**
         * Whether to throw an exception if no idempotency key was found in the request, by default false
         *
         * @param throwOnNoIdempotencyKey boolean to indicate if we must throw an Exception when
         *                                idempotency key could not be found in the payload.
         * @return the instance of the builder (to chain operations)
         */
        public Builder withThrowOnNoIdempotencyKey(boolean throwOnNoIdempotencyKey) {
            this.throwOnNoIdempotencyKey = throwOnNoIdempotencyKey;
            return this;
        }

        /**
         * Throw an exception if no idempotency key was found in the request.
         * Shortcut for {@link #withThrowOnNoIdempotencyKey(boolean)}, forced as true
         *
         * @return the instance of the builder (to chain operations)
         */
        public Builder withThrowOnNoIdempotencyKey() {
            return withThrowOnNoIdempotencyKey(true);
        }

        /**
         * Function to use for calculating hashes, by default MD5.
         *
         * @param hashFunction Can be any algorithm supported by {@link java.security.MessageDigest}, most commons are<ul>
         *                     <li>MD5</li>
         *                     <li>SHA-1</li>
         *                     <li>SHA-256</li></ul>
         * @return the instance of the builder (to chain operations)
         */
        public Builder withHashFunction(String hashFunction) {
            this.hashFunction = hashFunction;
            return this;
        }
    }
}

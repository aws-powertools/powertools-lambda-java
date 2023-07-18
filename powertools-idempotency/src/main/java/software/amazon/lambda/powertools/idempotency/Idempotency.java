/*
 * Copyright 2022 Amazon.com, Inc. or its affiliates.
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
import software.amazon.lambda.powertools.idempotency.persistence.BasePersistenceStore;

/**
 * Holds the configuration for idempotency:
 *
 * <ul>
 *   <li>The persistence layer to use for persisting the request and response of the function
 *       (mandatory).
 *   <li>The general configuration for idempotency (optional, see {@link IdempotencyConfig.Builder}
 *       methods to see defaults values.
 * </ul>
 *
 * <br>
 * Use it before the function handler ({@link
 * com.amazonaws.services.lambda.runtime.RequestHandler#handleRequest(Object, Context)}) get called.
 * <br>
 * Example:
 *
 * <pre>
 *     Idempotency.config().withPersistenceStore(...).configure();
 * </pre>
 */
public class Idempotency {
    private IdempotencyConfig config;
    private BasePersistenceStore persistenceStore;

    private Idempotency() {}

    public IdempotencyConfig getConfig() {
        return config;
    }

    public BasePersistenceStore getPersistenceStore() {
        if (persistenceStore == null) {
            throw new IllegalStateException(
                    "Persistence Store is null, did you call 'configure()'?");
        }
        return persistenceStore;
    }

    private void setConfig(IdempotencyConfig config) {
        this.config = config;
    }

    private void setPersistenceStore(BasePersistenceStore persistenceStore) {
        this.persistenceStore = persistenceStore;
    }

    private static class Holder {
        private static final Idempotency instance = new Idempotency();
    }

    public static Idempotency getInstance() {
        return Holder.instance;
    }

    /**
     * Can be used in a method which is not the handler to capture the Lambda context, to calculate
     * the remaining time before the invocation times out.
     *
     * @param lambdaContext
     */
    public static void registerLambdaContext(Context lambdaContext) {
        getInstance().getConfig().setLambdaContext(lambdaContext);
    }

    /**
     * Acts like a builder that can be used to configure {@link Idempotency}
     *
     * @return a new instance of {@link Config}
     */
    public static Config config() {
        return new Config();
    }

    public static class Config {

        private IdempotencyConfig config;
        private BasePersistenceStore store;

        /**
         * Use this method after configuring persistence layer (mandatory) and idem potency
         * configuration (optional)
         */
        public void configure() {
            if (store == null) {
                throw new IllegalStateException(
                        "Persistence Layer is null, configure one with 'withPersistenceStore()'");
            }
            if (config == null) {
                config = IdempotencyConfig.builder().build();
            }
            Idempotency.getInstance().setConfig(config);
            Idempotency.getInstance().setPersistenceStore(store);
        }

        public Config withPersistenceStore(BasePersistenceStore persistenceStore) {
            this.store = persistenceStore;
            return this;
        }

        public Config withConfig(IdempotencyConfig config) {
            this.config = config;
            return this;
        }
    }
}

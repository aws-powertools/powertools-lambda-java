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

package software.amazon.lambda.powertools.idempotency.persistence.redis;

import redis.clients.jedis.DefaultJedisClientConfig;
import redis.clients.jedis.JedisClientConfig;

public class JedisConfig {

    private final String host;
    private final Integer port;
    private final JedisClientConfig jedisClientConfig;

    public JedisConfig(String host, Integer port, JedisClientConfig jedisClientConfig) {
        this.host = host;
        this.port = port;
        this.jedisClientConfig = jedisClientConfig;
    }

    String getHost() {
        return host;
    }

    Integer getPort() {
        return port;
    }

    public JedisClientConfig getJedisClientConfig() {
        return jedisClientConfig;
    }

    public static class Builder {
        private String host = "localhost";
        private Integer port = 6379;

        private JedisClientConfig jedisClientConfig = DefaultJedisClientConfig.builder().build();

        public static JedisConfig.Builder builder() {
            return new JedisConfig.Builder();
        }

        public JedisConfig build() {
            return new JedisConfig(host, port, jedisClientConfig);
        }

        /**
         * Host name of the redis deployment (optional), by default "localhost"
         *
         * @param host host name of the Redis deployment
         * @return the builder instance (to chain operations)
         */
        public JedisConfig.Builder withHost(String host) {
            this.host = host;
            return this;
        }

        /**
         * Port for the redis deployment (optional), by default 6379
         *
         * @param port port for the Redis deployment
         * @return the builder instance (to chain operations)
         */
        public JedisConfig.Builder withPort(Integer port) {
            this.port = port;
            return this;
        }

        /**
         * Custom configuration for the redis client, by default {@link DefaultJedisClientConfig}
         *
         * @param jedisClientConfig custom configuration for the redis client
         * @return the builder instance (to chain operations)
         */
        public JedisConfig.Builder withJedisClientConfig(JedisClientConfig jedisClientConfig) {
            this.jedisClientConfig = jedisClientConfig;
            return this;
        }
    }
}

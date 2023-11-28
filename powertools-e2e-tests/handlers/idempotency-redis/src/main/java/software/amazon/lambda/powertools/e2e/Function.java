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

package software.amazon.lambda.powertools.e2e;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestHandler;
import java.time.Duration;
import java.time.Instant;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.TimeZone;
import redis.clients.jedis.JedisPooled;
import software.amazon.lambda.powertools.idempotency.Idempotency;
import software.amazon.lambda.powertools.idempotency.IdempotencyConfig;
import software.amazon.lambda.powertools.idempotency.Idempotent;
import software.amazon.lambda.powertools.idempotency.redis.RedisPersistenceStore;
import software.amazon.lambda.powertools.logging.Logging;


public class Function implements RequestHandler<Input, String> {

    public Function() {
        this(new JedisPooled(System.getenv().get("REDIS_HOST"), Integer.parseInt(System.getenv().get("REDIS_PORT")), System.getenv().get("REDIS_USER"), System.getenv().get("REDIS_SECRET")));
    }

    public Function(JedisPooled client) {
        Idempotency.config().withConfig(
                        IdempotencyConfig.builder()
                                .withExpiration(Duration.of(10, ChronoUnit.SECONDS))
                                .build())
                .withPersistenceStore(
                        RedisPersistenceStore.builder()
                                .withJedisPooled(client)
                                .build()
                ).configure();
    }

    @Logging(logEvent = true)
    @Idempotent
    public String handleRequest(Input input, Context context) {
        DateTimeFormatter dtf = DateTimeFormatter.ISO_DATE_TIME.withZone(TimeZone.getTimeZone("UTC").toZoneId());
        return dtf.format(Instant.now());
    }
}
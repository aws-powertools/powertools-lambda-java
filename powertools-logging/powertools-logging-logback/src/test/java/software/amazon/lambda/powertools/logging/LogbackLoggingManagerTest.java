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

package software.amazon.lambda.powertools.logging;

import static org.assertj.core.api.Assertions.assertThat;
import static org.slf4j.event.Level.DEBUG;
import static org.slf4j.event.Level.ERROR;
import static org.slf4j.event.Level.WARN;

import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.event.Level;
import software.amazon.lambda.powertools.logging.logback.internal.LogbackLoggingManager;

class LogbackLoggingManagerTest {

    private static final Logger LOG = LoggerFactory.getLogger(LogbackLoggingManagerTest.class);
    private static final Logger ROOT = LoggerFactory.getLogger(Logger.ROOT_LOGGER_NAME);

    @Test
    void test() {
//        LOG.info("test", kv("key", "value"));
    }

    @Test
    @Order(1)
    void getLogLevel_shouldReturnConfiguredLogLevel() {
        LogbackLoggingManager manager = new LogbackLoggingManager();
        Level logLevel = manager.getLogLevel(LOG);
        assertThat(logLevel).isEqualTo(DEBUG);

        logLevel = manager.getLogLevel(ROOT);
        assertThat(logLevel).isEqualTo(WARN);
    }

    @Test
    @Order(2)
    void resetLogLevel() {
        LogbackLoggingManager manager = new LogbackLoggingManager();
        manager.setLogLevel(ERROR);

        Level logLevel = manager.getLogLevel(LOG);
        assertThat(logLevel).isEqualTo(ERROR);
    }
}

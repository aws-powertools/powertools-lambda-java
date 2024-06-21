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

package software.amazon.lambda.powertools.logging.handlers;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import software.amazon.lambda.powertools.logging.Logging;

public class PowertoolsLogEnabled implements RequestHandler<Object, Object> {
    private static final Logger LOG = LoggerFactory.getLogger(PowertoolsLogEnabled.class);
    private final boolean throwError;

    public PowertoolsLogEnabled(boolean throwError) {
        this.throwError = throwError;
    }

    public PowertoolsLogEnabled() {
        this(false);
    }

    @Override
    @Logging
    public Object handleRequest(Object input, Context context) {
        if (throwError) {
            throw new RuntimeException("Something went wrong");
        }
        LOG.error("Test error event");
        LOG.warn("Test warn event");
        LOG.info("Test event");
        LOG.debug("Test debug event");
        return "Bonjour le monde";
    }

    @Logging
    public void anotherMethod() {
        System.out.println("test");
    }

    public static Logger getLogger() {
        return LOG;
    }
}

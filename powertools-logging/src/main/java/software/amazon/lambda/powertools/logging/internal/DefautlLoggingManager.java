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

package software.amazon.lambda.powertools.logging.internal;

import org.slf4j.Logger;
import org.slf4j.event.Level;

/**
 * When no LoggingManager is found, setting a default one with no action on logging implementation
 * Powertools cannot change the log level based on the environment variable, will use the logger configuration
 */
public class DefautlLoggingManager implements LoggingManager {

    @Override
    public void setLogLevel(Level logLevel) {
        // do nothing
    }

    @Override
    public Level getLogLevel(Logger logger) {
        return Level.ERROR;
    }
}

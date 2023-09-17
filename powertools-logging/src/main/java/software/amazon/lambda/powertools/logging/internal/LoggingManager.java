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
 * Due to limitations of SLF4J, we need to rely on implementations for some operations:
 * <ul>
 *     <li>Accessing to all loggers and change their Level</li>
 *     <li>Retrieving the log Level of a Logger</li>
 * </ul>
 * <p>
 * Implementations are provided in submodules and loaded thanks to a {@link java.util.ServiceLoader}
 * (define a file named <code>software.amazon.lambda.powertools.logging.internal.LoggingManager</code> in <code>src/main/resources/META-INF/services</code> with the qualified name of the implementation).
 */
public interface LoggingManager {
    /**
     * Change the log Level of all loggers (named and root)
     *
     * @param logLevel the log Level (slf4j) to apply
     */
    void resetLogLevel(Level logLevel);

    /**
     * Retrieve the log Level of a specific logger
     *
     * @param logger the logger (slf4j) for which to retrieve the log Level
     * @return the Level (slf4j) of this logger. Note that SLF4J only support ERROR, WARN, INFO, DEBUG, TRACE while some frameworks may support others (OFF, FATAL, ...)
     */
    Level getLogLevel(Logger logger);
}

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

import software.amazon.lambda.powertools.logging.internal.BufferManager;
import software.amazon.lambda.powertools.logging.internal.LoggingManager;
import software.amazon.lambda.powertools.logging.internal.LoggingManagerRegistry;

/**
 * PowertoolsLogging provides a backend-independent API for log buffering operations.
 * This class abstracts away the underlying logging framework (Log4j2, Logback) and
 * provides a unified interface for buffer management.
 */
public final class PowertoolsLogging {

    private PowertoolsLogging() {
        // Utility class
    }

    /**
     * Flushes the log buffer for the current Lambda execution.
     * This method will flush any buffered logs to the output stream.
     * The operation is backend-independent and works with both Log4j2 and Logback.
     */
    public static void flushBuffer() {
        LoggingManager loggingManager = LoggingManagerRegistry.getLoggingManager();
        if (loggingManager instanceof BufferManager) {
            ((BufferManager) loggingManager).flushBuffer();
        }
    }

    /**
     * Clears the log buffer for the current Lambda execution.
     * This method will discard any buffered logs without outputting them.
     * The operation is backend-independent and works with both Log4j2 and Logback.
     */
    public static void clearBuffer() {
        LoggingManager loggingManager = LoggingManagerRegistry.getLoggingManager();
        if (loggingManager instanceof BufferManager) {
            ((BufferManager) loggingManager).clearBuffer();
        }
    }
}

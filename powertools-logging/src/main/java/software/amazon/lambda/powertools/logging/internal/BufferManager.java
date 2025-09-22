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

/**
 * Interface for logging managers that support buffer operations.
 * This extends the logging framework capabilities with buffer-specific functionality.
 */
public interface BufferManager {
    /**
     * Flushes the log buffer for the current Lambda execution.
     * This method will flush any buffered logs to the target appender.
     */
    void flushBuffer();

    /**
     * Clears the log buffer for the current Lambda execution.
     * This method will discard any buffered logs without outputting them.
     */
    void clearBuffer();
}

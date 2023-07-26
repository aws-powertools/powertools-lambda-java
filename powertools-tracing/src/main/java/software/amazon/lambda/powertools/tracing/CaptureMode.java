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

package software.amazon.lambda.powertools.tracing;

public enum CaptureMode {
    /**
     * Enables annotation to capture only response. If this mode is explicitly overridden
     * on {@link Tracing} annotation, it will override value of environment variable POWERTOOLS_TRACER_CAPTURE_RESPONSE
     */
    RESPONSE,
    /**
     * Enabled annotation to capture only error from the method. If this mode is explicitly overridden
     * on {@link Tracing} annotation, it will override value of environment variable POWERTOOLS_TRACER_CAPTURE_ERROR
     */
    ERROR,
    /**
     * Enabled annotation to capture both response error from the method. If this mode is explicitly overridden
     * on {@link Tracing} annotation, it will override value of environment variables POWERTOOLS_TRACER_CAPTURE_RESPONSE
     * and POWERTOOLS_TRACER_CAPTURE_ERROR
     */
    RESPONSE_AND_ERROR,
    /**
     * Disables annotation to capture both response and error from the method. If this mode is explicitly overridden
     * on {@link Tracing} annotation, it will override values of environment variable POWERTOOLS_TRACER_CAPTURE_RESPONSE
     * and POWERTOOLS_TRACER_CAPTURE_ERROR
     */
    DISABLED,
    /**
     * Enables/Disables annotation to capture response and error from the method based on the value of
     * environment variable POWERTOOLS_TRACER_CAPTURE_RESPONSE and POWERTOOLS_TRACER_CAPTURE_ERROR
     */
    ENVIRONMENT_VAR
}

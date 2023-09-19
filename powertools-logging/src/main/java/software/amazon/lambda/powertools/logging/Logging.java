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

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * {@code Logging} is used to signal that the annotated method should be
 * extended with Logging functionality.
 *
 * <p>{@code Logging} provides an opinionated logger with output structured as JSON.</p>
 *
 * <p>{@code Logging} should be used with the handleRequest method of a class
 * which implements either
 * {@code com.amazonaws.services.lambda.runtime.RequestHandler} or
 * {@code com.amazonaws.services.lambda.runtime.RequestStreamHandler}.</p>
 *
 * <p>By default {@code Logging} will load the following keys and values from the Lambda
 * {@code com.amazonaws.services.lambda.runtime.Context}</p>
 *
 * <ul>
 *     <li>function_name</li>
 *     <li>function_version</li>
 *     <li>function_arn</li>
 *     <li>MemoryLimitInMB</li>
 * </ul>
 *
 * <p>By default {@code Logging} will also create keys for:</p>
 *
 * <ul>
 *     <li>cold_start - True if this is the first invocation of this Lambda execution environment; else False</li>
 *     <li>service - The value of the 'POWER_TOOLS_SERVICE_NAME' environment variable or 'service_undefined'</li>
 *     <li>sampling_rate - The value of the 'POWERTOOLS_LOGGER_SAMPLE_RATE' environment variable or value of sampling_rate field or 0.
 *     Valid value is from 0.0 to 1.0. Value outside this range is silently ignored.</li>
 * </ul>
 *
 * <p>These keys and values will be joined with the existing Log4J log event and written as JSON.</p>
 *
 * <p>The data and time of the log event will be written using {@link java.time.format.DateTimeFormatter#ISO_ZONED_DATE_TIME}</p>
 *
 * <p>By default {@code Logging} will not log the event which has trigger the invoke of the Lambda function.
 * This can be enabled using {@code @Logging(logEvent = true)}.</p>
 *
 * <p>By default {@code Logging} all debug logs will follow log4j2 configuration unless configured via
 * POWERTOOLS_LOGGER_SAMPLE_RATE environment variable {@code @Logging(samplingRate = <0.0-1.0>)}.</p>
 *
 * <p>To append additional keys to each log entry you can use {@link LoggingUtils#appendKey(String, String)}</p>
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.METHOD)
public @interface Logging {

    boolean logEvent() default false;

    double samplingRate() default 0;

    /**
     * Json Pointer path to extract correlation id from.
     *
     * @see <a href=https://datatracker.ietf.org/doc/html/draft-ietf-appsawg-json-pointer-03/>
     */
    String correlationIdPath() default "";

    /**
     * Logger is commonly initialized in the global scope.
     * Due to Lambda Execution Context reuse, this means that custom keys can be persisted across invocations.
     * Set this attribute to true if you want all custom keys to be deleted on each request.
     */
    boolean clearState() default false;
}

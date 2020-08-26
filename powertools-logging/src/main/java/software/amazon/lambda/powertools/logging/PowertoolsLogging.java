/*
 * Copyright 2020 Amazon.com, Inc. or its affiliates.
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
 * {@code PowertoolsLogging} is used to signal that the annotated method should be
 * extended with PowertoolsLogging functionality.
 *
 * <p>{@code PowertoolsLogging} provides an opinionated logger with output structured as JSON.</p>
 *
 * <p>{@code PowertoolsLogging} should be used with the handleRequest method of a class
 * which implements either
 * {@code com.amazonaws.services.lambda.runtime.RequestHandler} or
 * {@code com.amazonaws.services.lambda.runtime.RequestStreamHandler}.</p>
 *
 * <p>By default {@code PowertoolsLogging} will load the following keys and values from the Lambda
 * {@code com.amazonaws.services.lambda.runtime.Context}</p>
 *
 * <ul>
 *     <li>FunctionName</li>
 *     <li>FunctionVersion</li>
 *     <li>InvokedFunctionArn</li>
 *     <li>MemoryLimitInMB</li>
 * </ul>
 *
 * <p>By default {@code PowertoolsLogging} will also create keys for:</p>
 *
 * <ul>
 *     <li>coldStart - True if this is the first invocation of this Lambda execution environment; else False</li>
 *     <li>service - The value of the 'POWER_TOOLS_SERVICE_NAME' environment variable or 'service_undefined'</li>
 *     <li>samplingRate - The value of the 'POWERTOOLS_LOGGER_SAMPLE_RATE' environment variable or value of samplingRate field or 0.
 *     Valid value is from 0.0 to 1.0. Value outside this range is silently ignored.</li>
 * </ul>
 *
 * <p>These keys and values will be joined with the existing Log4J log event and written as JSON.</p>
 *
 * <p>The data and time of the log event will be written using {@link java.time.format.DateTimeFormatter#ISO_ZONED_DATE_TIME}</p>
 *
 * <p>By default {@code PowertoolsLogging} will not log the event which has trigger the invoke of the Lambda function.
 * This can be enabled using {@code @PowertoolsLogging(logEvent = true)}.</p>
 *
 * <p>By default {@code PowertoolsLogging} all debug loggs will follow log4j2 configuration unless configured via
 * POWERTOOLS_LOGGER_SAMPLE_RATE environment variable {@code @PowertoolsLogging(samplingRate = <0.0-1.0>)}.</p>
 *
 * <p>To append additional keys to each log entry you can use {@link PowertoolsLogger#appendKey(String, String)}</p>
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.METHOD)
public @interface PowertoolsLogging {

    boolean logEvent() default false;

    double samplingRate() default 0;
}

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
 * {@code PowerToolsLogging} is used to signal that the annotated method should be
 * extended with PowerToolsLogging functionality.
 *
 * <p>{@code PowerToolsLogging} provides an opinionated logger with output structured as JSON.</p>
 *
 * <p>{@code PowerToolsLogging} should be used with handleRequest method of a class
 * which implements either
 * {@code com.amazonaws.services.lambda.runtime.RequestHandler} or
 * {@code com.amazonaws.services.lambda.runtime.RequestStreamHandler}.</p>
 *
 * <p>By default {@code PowerToolsLogging} will load the following keys and values from the Lambda
 * {@code com.amazonaws.services.lambda.runtime.Context}</p>
 *
 * <ul>
 *     <li>FunctionName</li>
 *     <li>FunctionVersion</li>
 *     <li>InvokedFunctionArn</li>
 *     <li>MemoryLimitInMB</li>
 * </ul>
 *
 * <p>By default {@code PowerToolsLogging} will also create keys for:</p>
 *
 * <ul>
 *     <li>coldStart - True if this is the first invocation of this Lambda execution environment; else False</li>
 *     <li>service - The value of the 'POWER_TOOLS_SERVICE_NAME' environment variable or 'service_undefined'</li>
 * </ul>
 *
 * <p>These keys and values will be joined with the existing Log4J log event and written as JSON.</p>
 *
 * <p>The data and time of the log event will be written using {@link java.time.format.DateTimeFormatter#ISO_ZONED_DATE_TIME}</p>
 *
 * <p>By default {@code PowerToolsLogging} will not log the event which has trigger the invoke of the Lambda function.
 * This can be enabled using {@code @PowerToolsLogging(logEvent = true)}.</p>
 *
 * <p>To append additional keys to each log entry you can use {@link PowerLogger#appendKey(String, String)}</p>
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.METHOD)
public @interface PowerToolsLogging {

    boolean logEvent() default false;
}

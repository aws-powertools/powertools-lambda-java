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
package software.amazon.lambda.powertools.tracing;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * {@code PowertoolsTracing} is used to signal that the annotated method should
 * be extended with the Powertools tracing functionality.
 *
 * <p>{@code PowertoolsTracing} provides functionality to reduce the overhead
 * of performing common tracing tasks.</p>
 *
 * <p>{@code PowertoolsTracing} should be used with the handleRequest method of a class
 * which implements either
 * {@code com.amazonaws.services.lambda.runtime.RequestHandler} or
 * {@code com.amazonaws.services.lambda.runtime.RequestStreamHandler}.</p>
 *
 * <p>By default {@code PowertoolsTracing} will capture responses and add them
 * to a sub segment named after the method.</p>
 *
 * <p>To disable this functionality you can specify {@code @PowertoolsTracing( captureRespones = false)}</p>
 *
 * <p>By default {@code PowertoolsTracing} will capture errors and add them
 * to a sub segment named after the method.</p>
 *
 * <p>To disable this functionality you can specify {@code @PowertoolsTracing( captureError = false)}</p>
 *e
 * <p>All traces have a namespace set. If {@code @PowertoolsTracing( namespace = "ExampleService")} is set
 * this takes precedent over any value set in the environment variable {@code POWER_TOOLS_SERVICE_NAME}.
 * If both are undefined then the value will default to {@code service_undefined}</p>
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.METHOD)
public @interface PowertoolsTracing {
    String namespace() default "";
    boolean captureResponse() default true;
    boolean captureError() default true;
}

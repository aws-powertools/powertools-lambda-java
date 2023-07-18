/*
 * Copyright 2022 Amazon.com, Inc. or its affiliates.
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
package software.amazon.lambda.powertools.idempotency;

import com.amazonaws.services.lambda.runtime.Context;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * &#64;Idempotent is used to signal that the annotated method is idempotent:<br>
 * Calling this method one or multiple times with the same parameter will always return the same
 * result.<br>
 * This annotation can be placed on the {@link
 * com.amazonaws.services.lambda.runtime.RequestHandler#handleRequest(Object, Context)} method of a
 * Lambda function:<br>
 *
 * <pre>
 *     &#64;Idempotent
 *     public String handleRequest(String event, Context ctx) {
 *         // ...
 *         return something;
 *     }
 * </pre>
 *
 * <br>
 * It can also be placed on another method. In that case you may need to use the &#64;{@link
 * IdempotencyKey}
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.METHOD)
public @interface Idempotent {}

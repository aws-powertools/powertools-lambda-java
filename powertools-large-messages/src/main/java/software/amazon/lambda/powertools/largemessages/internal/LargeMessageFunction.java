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

package software.amazon.lambda.powertools.largemessages.internal;

/**
 * Functional interface for large message processing.
 * <p>
 * This interface is similar to {@link java.util.function.Function} but throws {@link Throwable}
 * instead of no exceptions. This is necessary to support AspectJ's {@code ProceedingJoinPoint.proceed()}
 * which throws {@code Throwable}, allowing exceptions to bubble up naturally without wrapping.
 * <p>
 * This interface should not be exposed to user-facing APIs such as
 * {@link software.amazon.lambda.powertools.largemessages.LargeMessages}. These should use plain
 * {@link java.util.function.Function}.
 *
 * @param <T> the input type (message type)
 * @param <R> the return type of the function
 */
@FunctionalInterface
public interface LargeMessageFunction<T, R> {
    @SuppressWarnings("java:S112") // Throwable is required for AspectJ compatibility
    R apply(T processedMessage) throws Throwable;
}

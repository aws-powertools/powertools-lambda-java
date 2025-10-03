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
package software.amazon.lambda.powertools.validation.internal;

import software.amazon.awssdk.core.SdkRequest;
import software.amazon.awssdk.core.interceptor.Context;
import software.amazon.awssdk.core.interceptor.ExecutionAttributes;
import software.amazon.awssdk.core.interceptor.ExecutionInterceptor;
import software.amazon.lambda.powertools.common.internal.UserAgentConfigurator;

/**
 * Global interceptor that configures the User-Agent for all AWS SDK clients
 * when the powertools-validation module is on the classpath.
 */
public final class ValidationUserAgentInterceptor implements ExecutionInterceptor {
    static {
        UserAgentConfigurator.configureUserAgent("validation");
    }

    @Override
    public SdkRequest modifyRequest(Context.ModifyRequest context, ExecutionAttributes executionAttributes) {
        // This is a no-op interceptor. We use this class to configure the PT User-Agent in the static block. It is
        // loaded by AWS SDK Global Interceptors.
        return context.request();
    }
}

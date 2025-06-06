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

package software.amazon.lambda.powertools.metrics.testutils;

import com.amazonaws.services.lambda.runtime.Context;

/**
 * Simple Lambda context implementation for unit tests
 */
public class TestContext implements Context {
    @Override
    public String getAwsRequestId() {
        return "test-request-id";
    }

    @Override
    public String getLogGroupName() {
        return "test-log-group";
    }

    @Override
    public String getLogStreamName() {
        return "test-log-stream";
    }

    @Override
    public String getFunctionName() {
        return "test-function";
    }

    @Override
    public String getFunctionVersion() {
        return "test-version";
    }

    @Override
    public String getInvokedFunctionArn() {
        return "test-arn";
    }

    @Override
    public com.amazonaws.services.lambda.runtime.CognitoIdentity getIdentity() {
        return null;
    }

    @Override
    public com.amazonaws.services.lambda.runtime.ClientContext getClientContext() {
        return null;
    }

    @Override
    public int getRemainingTimeInMillis() {
        return 1000;
    }

    @Override
    public int getMemoryLimitInMB() {
        return 128;
    }

    @Override
    public com.amazonaws.services.lambda.runtime.LambdaLogger getLogger() {
        return null;
    }
}

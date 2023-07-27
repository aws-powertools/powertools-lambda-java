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

package software.amazon.lambda.powertools.testutils.lambda;

import java.time.Instant;
import software.amazon.awssdk.services.lambda.model.InvokeResponse;
import software.amazon.lambda.powertools.testutils.logging.InvocationLogs;

public class InvocationResult {

    private final InvocationLogs logs;
    private final String result;

    private final String requestId;
    private final Instant start;
    private final Instant end;

    public InvocationResult(InvokeResponse response, Instant start, Instant end) {
        requestId = response.responseMetadata().requestId();
        logs = new InvocationLogs(response.logResult(), requestId);
        result = response.payload().asUtf8String();
        this.start = start;
        this.end = end;
    }

    public InvocationLogs getLogs() {
        return logs;
    }

    public String getResult() {
        return result;
    }

    public String getRequestId() {
        return requestId;
    }

    public Instant getStart() {
        return start;
    }

    public Instant getEnd() {
        return end;
    }
}

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

package software.amazon.lambda.powertools.cloudformation;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.io.IOException;

import org.mockito.ArgumentMatcher;

import software.amazon.lambda.powertools.cloudformation.Response.Status;

/**
 * Creates a handler that will expect the Response to be sent with an expected
 * status. Will throw an AssertionError
 * if the method is sent with an unexpected status.
 */
public class ExpectedStatusResourceHandler extends NoOpCustomResourceHandler {
    private final Status expectedStatus;

    public ExpectedStatusResourceHandler(Status expectedStatus) {
        this.expectedStatus = expectedStatus;
    }

    CloudFormationResponse buildResponseClient() {
        // create a CloudFormationResponse that fails if invoked with unexpected status
        CloudFormationResponse cfnResponse = mock(CloudFormationResponse.class);
        try {
            when(cfnResponse.send(any(), any(), org.mockito.ArgumentMatchers.argThat(new ArgumentMatcher<Response>() {
                @Override
                public boolean matches(Response resp) {
                    return resp != null && resp.getStatus() != expectedStatus;
                }
            }))).thenThrow(new AssertionError("Expected response's status to be " + expectedStatus));
        } catch (IOException | CustomResourceResponseException e) {
            // this should never happen
            throw new RuntimeException("Unexpected mocking exception", e);
        }
        return cfnResponse;
    }
}

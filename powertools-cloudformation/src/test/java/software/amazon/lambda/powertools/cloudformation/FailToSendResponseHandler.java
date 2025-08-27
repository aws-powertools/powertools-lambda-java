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

/**
 * Always fails to send the response
 */
public class FailToSendResponseHandler extends NoOpCustomResourceHandler {
    @Override
    CloudFormationResponse buildResponseClient() {
        CloudFormationResponse cfnResponse = mock(CloudFormationResponse.class);
        try {
            when(cfnResponse.send(any(), any()))
                    .thenThrow(new IOException("Intentional send failure"));
            when(cfnResponse.send(any(), any(), any()))
                    .thenThrow(new IOException("Intentional send failure"));
        } catch (IOException | CustomResourceResponseException e) {
            // this should never happen
            throw new RuntimeException("Unexpected mocking exception", e);
        }
        return cfnResponse;
    }
}

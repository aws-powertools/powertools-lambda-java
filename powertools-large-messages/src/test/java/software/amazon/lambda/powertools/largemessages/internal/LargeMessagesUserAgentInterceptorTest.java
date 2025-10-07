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

import org.junit.jupiter.api.Test;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;

import static org.assertj.core.api.Assertions.assertThat;

class LargeMessagesUserAgentInterceptorTest {

    @Test
    void shouldConfigureUserAgentWhenCreatingAwsSdkClient() {
        // WHEN creating an AWS SDK client, the interceptor should be loaded
        // We use S3 client but it can be any arbitrary AWS SDK client
        S3Client.builder().region(Region.US_EAST_1).build();
        
        // THEN the user agent system property should be set
        String userAgent = System.getProperty("sdk.ua.appId");
        assertThat(userAgent).contains("PT/LARGE-MESSAGES/");
    }
}
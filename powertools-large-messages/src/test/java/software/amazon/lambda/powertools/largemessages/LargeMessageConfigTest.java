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

package software.amazon.lambda.powertools.largemessages;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;

public class LargeMessageConfigTest {

    @BeforeEach
    public void setup() {
        LargeMessageConfig.get().resetS3Client();
    }

    @AfterEach
    public void tearDown() {
        LargeMessageConfig.get().resetS3Client();
    }

    @Test
    public void singleton_shouldNotChangeWhenCalledMultipleTimes() {
        LargeMessageConfig.init().withS3Client(S3Client.builder().region(Region.US_EAST_1).build());
        LargeMessageConfig config = LargeMessageConfig.get();

        LargeMessageConfig.init().withS3Client(null);
        LargeMessageConfig config2 = LargeMessageConfig.get();

        assertThat(config2).isEqualTo(config);
    }

    @Test
    public void singletonWithDefaultClient_shouldNotChangeWhenCalledMultipleTimes() {
        S3Client s3Client = LargeMessageConfig.get().getS3Client();

        LargeMessageConfig.init().withS3Client(S3Client.create());
        S3Client s3Client2 = LargeMessageConfig.get().getS3Client();

        assertThat(s3Client2).isEqualTo(s3Client);
    }
}

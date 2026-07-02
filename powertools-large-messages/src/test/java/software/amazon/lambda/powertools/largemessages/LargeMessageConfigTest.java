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

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;

class LargeMessageConfigTest {

    @BeforeEach
    void setup() {
        LargeMessageConfig.get().resetS3Client();
    }

    @AfterEach
    void tearDown() {
        LargeMessageConfig.get().resetS3Client();
    }

    @Test
    void singleton_shouldNotChangeWhenCalledMultipleTimes() {
        LargeMessageConfig.init().withS3Client(S3Client.builder().region(Region.US_EAST_1).build());
        LargeMessageConfig config = LargeMessageConfig.get();

        LargeMessageConfig.init().withS3Client(null);
        LargeMessageConfig config2 = LargeMessageConfig.get();

        assertThat(config2).isEqualTo(config);
    }

    @Test
    void singletonWithDefaultClient_shouldNotChangeWhenCalledMultipleTimes() {
        S3Client s3Client = LargeMessageConfig.get().getS3Client();

        LargeMessageConfig.init().withS3Client(S3Client.create());
        S3Client s3Client2 = LargeMessageConfig.get().getS3Client();

        assertThat(s3Client2).isEqualTo(s3Client);
    }

    @Test
    void allowedBuckets_shouldDefaultToEmpty() {
        assertThat(LargeMessageConfig.get().getAllowedBuckets()).isEmpty();
    }

    @Test
    void withAllowedBuckets_shouldStoreAndReturnTheSet() {
        Set<String> buckets = new HashSet<>();
        buckets.add("bucket-a");
        buckets.add("bucket-b");

        LargeMessageConfig.init().withAllowedBuckets(buckets);

        assertThat(LargeMessageConfig.get().getAllowedBuckets()).containsExactlyInAnyOrder("bucket-a", "bucket-b");
    }

    @Test
    void withAllowedBuckets_null_shouldResultInEmptySet() {
        LargeMessageConfig.init().withAllowedBuckets(Collections.singleton("bucket-a"));
        LargeMessageConfig.init().withAllowedBuckets(null);

        assertThat(LargeMessageConfig.get().getAllowedBuckets()).isEmpty();
    }

    @Test
    void reset_shouldClearAllowedBuckets() {
        LargeMessageConfig.init().withAllowedBuckets(Collections.singleton("bucket-a"));

        LargeMessageConfig.get().resetS3Client();

        assertThat(LargeMessageConfig.get().getAllowedBuckets()).isEmpty();
    }
}

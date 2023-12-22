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

import static software.amazon.lambda.powertools.common.internal.LambdaConstants.AWS_REGION_ENV;

import software.amazon.awssdk.http.crt.AwsCrtHttpClient;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.S3ClientBuilder;

/**
 * Singleton instance for Large Message Config. We need this to provide a way to customize the S3 client configuration used by the annotation.
 * <br/>
 * Optional: Use it in your Lambda constructor to pass a custom {@link S3Client} to the {@link software.amazon.lambda.powertools.largemessages.internal.LargeMessageProcessor}
 * <br/>
 * If you don't use this, a default S3Client will be created.
 * <pre>
 * public MyLambdaHandler() {
 *     LargeMessageConfig.init().withS3Client(S3Client.create());
 * }
 * </pre>
 */
public class LargeMessageConfig {

    private static final LargeMessageConfig INSTANCE = new LargeMessageConfig();
    private S3Client s3Client;

    private LargeMessageConfig() {
    }

    /**
     * Retrieve the singleton instance (you generally don't need to use this one, used internally by the library)
     *
     * @return the singleton instance
     */
    public static LargeMessageConfig get() {
        return INSTANCE;
    }

    /**
     * Initialize the singleton instance
     *
     * @return the singleton instance
     */
    public static LargeMessageConfig init() {
        return INSTANCE;
    }

    public void withS3Client(S3Client s3Client) {
        if (this.s3Client == null) {
            this.s3Client = s3Client;
        }
    }

    // For tests purpose
    void resetS3Client() {
        this.s3Client = null;
    }

    // Getter needs to initialize if not done with setter
    public S3Client getS3Client() {
        if (this.s3Client == null) {
            S3ClientBuilder s3ClientBuilder = S3Client.builder()
                    .httpClient(AwsCrtHttpClient.builder().build())
                    .region(Region.of(System.getenv(AWS_REGION_ENV)));
            this.s3Client = s3ClientBuilder.build();
        }
        return this.s3Client;
    }
}

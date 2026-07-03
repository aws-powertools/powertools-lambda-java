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

import org.crac.Context;
import org.crac.Core;
import org.crac.Resource;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

import software.amazon.awssdk.http.urlconnection.UrlConnectionHttpClient;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.S3ClientBuilder;

/**
 * Singleton instance for Large Message Config. We need this to provide a way to customize the S3 client
 * configuration used by the annotation.
 * <br/>
 * Optional: Use it in your Lambda constructor to pass a custom {@link S3Client} to the
 * {@link software.amazon.lambda.powertools.largemessages.internal.LargeMessageProcessor}
 * <br/>
 * If you don't use this, a default S3Client will be created.
 * <pre>
 * public MyLambdaHandler() {
 *     LargeMessageConfig.init().withS3Client(S3Client.create());
 * }
 * </pre>
 */
public class LargeMessageConfig implements Resource {

    private static final LargeMessageConfig INSTANCE = new LargeMessageConfig();
    private S3Client s3Client;
    private Set<String> allowedBuckets = Collections.emptySet();

    private LargeMessageConfig() {
        Core.getGlobalContext().register(this);
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

    /**
     * Restrict the S3 buckets the utility is allowed to read from and delete.
     * <p>
     * The {@code s3BucketName} in the payload pointer is controlled by whoever sent the message. Without an
     * allowlist, any sender can redirect the Lambda function to fetch (and, when {@code deleteS3Object} is enabled,
     * delete) objects from an arbitrary bucket using the function's own credentials. When a non-empty allowlist is
     * configured, the utility rejects any message whose pointer references a bucket that is not in the allowlist,
     * before any S3 interaction.
     * <p>
     * An empty (or null) allowlist means no restriction is applied (default), preserving backward compatibility.
     * Configuring an allowlist is strongly recommended for security.
     * <pre>
     * public MyLambdaHandler() {
     *     LargeMessageConfig.init().withAllowedBuckets(Collections.singleton("my-offload-bucket"));
     * }
     * </pre>
     *
     * @param allowedBuckets the set of bucket names the utility is allowed to access (null is treated as empty)
     */
    public void withAllowedBuckets(Set<String> allowedBuckets) {
        this.allowedBuckets = allowedBuckets == null
                ? Collections.emptySet()
                : Collections.unmodifiableSet(new HashSet<>(allowedBuckets));
    }

    /**
     * Retrieve the configured set of allowed buckets. An empty set means no restriction is applied.
     *
     * @return the (unmodifiable) set of allowed bucket names, never null
     */
    public Set<String> getAllowedBuckets() {
        return Collections.unmodifiableSet(allowedBuckets);
    }

    // For tests purpose
    void resetS3Client() {
        this.s3Client = null;
        this.allowedBuckets = Collections.emptySet();
    }

    // Getter needs to initialize if not done with setter
    public S3Client getS3Client() {
        if (this.s3Client == null) {
            S3ClientBuilder s3ClientBuilder = S3Client.builder()
                    .httpClient(UrlConnectionHttpClient.builder().build())
                    .region(Region.of(System.getenv(AWS_REGION_ENV)));
            this.s3Client = s3ClientBuilder.build();
        }
        return this.s3Client;
    }

    @Override
    public void beforeCheckpoint(Context<? extends Resource> context) throws Exception {
        getS3Client();
    }

    @Override
    public void afterRestore(Context<? extends Resource> context) throws Exception {
        // No action needed after restore
    }
}

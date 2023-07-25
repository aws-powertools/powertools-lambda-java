package software.amazon.lambda.powertools.largemessages;

import software.amazon.awssdk.http.urlconnection.UrlConnectionHttpClient;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.S3ClientBuilder;

import static software.amazon.lambda.powertools.core.internal.LambdaConstants.AWS_REGION_ENV;

/**
 * Singleton instance for Large Message Config.
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

    public static LargeMessageConfig get() {
        return INSTANCE;
    }

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
                    .httpClient(UrlConnectionHttpClient.builder().build())
                    .region(Region.of(System.getenv(AWS_REGION_ENV)));
            this.s3Client = s3ClientBuilder.build();
        }
        return this.s3Client;
    }
}

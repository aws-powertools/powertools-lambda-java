package software.amazon.lambda.powertools.largemessages;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;

import static org.assertj.core.api.Assertions.assertThat;

public class LargeMessageConfigTest {

    @BeforeEach
    public void setup() {
        LargeMessageConfig.get().setS3Client(null);
    }

    @AfterEach
    public void tearDown() {
        LargeMessageConfig.get().setS3Client(null);
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

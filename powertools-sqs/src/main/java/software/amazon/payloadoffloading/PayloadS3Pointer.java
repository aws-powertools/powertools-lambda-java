package software.amazon.payloadoffloading;

import java.util.Optional;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectWriter;
import com.fasterxml.jackson.databind.SerializationFeature;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static java.util.Optional.empty;
import static java.util.Optional.ofNullable;

public class PayloadS3Pointer {
    private static final Logger LOG = LoggerFactory.getLogger(PayloadS3Pointer.class);
    private static final ObjectMapper objectMapper = new ObjectMapper();

    private String s3BucketName;
    private String s3Key;

    static {
        objectMapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
        objectMapper.configure(SerializationFeature.FAIL_ON_EMPTY_BEANS, false);
        objectMapper.activateDefaultTyping(objectMapper.getPolymorphicTypeValidator(), ObjectMapper.DefaultTyping.NON_FINAL);
    }

    private PayloadS3Pointer() {

    }

    public String getS3BucketName() {
        return this.s3BucketName;
    }

    public String getS3Key() {
        return this.s3Key;
    }

    public static Optional<PayloadS3Pointer> fromJson(String s3PointerJson) {
        try {
            return ofNullable(objectMapper.readValue(s3PointerJson, PayloadS3Pointer.class));
        } catch (Exception e) {
            LOG.error("Failed to read the S3 object pointer from given string.", e);
            return empty();
        }
    }

    public Optional<String> toJson() {
        try {
            ObjectWriter objectWriter = objectMapper.writer();
            return ofNullable(objectWriter.writeValueAsString(this));

        } catch (Exception e) {
            LOG.error("Failed to convert S3 object pointer to text.", e);
            return empty();
        }
    }
}

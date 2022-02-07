package software.amazon.lambda.powertools.aurora;

import com.amazonaws.services.lambda.runtime.events.KinesisEvent;
import software.amazon.awssdk.core.SdkBytes;
import software.amazon.awssdk.http.SdkHttpClient;
import software.amazon.awssdk.http.apache.ApacheHttpClient;

import software.amazon.awssdk.services.kms.KmsClientBuilder;
import software.amazon.awssdk.services.kms.model.DecryptRequest;
import software.amazon.awssdk.services.kms.model.DecryptResponse;
import software.amazon.awssdk.services.kms.KmsClient;
import software.amazon.lambda.powertools.aurora.internal.DecryptUtils;
import software.amazon.lambda.powertools.aurora.model.*;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.lang.reflect.Constructor;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.*;

public class AuroraUtils {

    private static final Logger LOG = LoggerFactory.getLogger(AuroraUtils.class);
    private static final ObjectMapper objectMapper = new ObjectMapper();

    private AuroraUtils(){
    }

    public static <R> List<R> process(KinesisEvent input, final Class<? extends DataStreamHandler<R>> handler) {

        return process(input, true, handler);
    }

    public static <R> List<R> process(KinesisEvent input, boolean skipHeartbeat,
                                      final Class<? extends DataStreamHandler<R>> handler) {

        DataStreamHandler<R> handlerInstance = instantiatedHandler(handler);
        final List<R> handlerReturn = new ArrayList<>();

        for (KinesisEvent.KinesisEventRecord record : input.getRecords()) {
            PostgresActivityRecords records = processPostgresActivity(record);
            for (PostgresActivityEvent event : records.getDatabaseActivityEventList()) {
                if (!skipHeartbeat || (!"heartbeat".equalsIgnoreCase(event.getType()) && skipHeartbeat)) {
                    LOG.debug("Process event " + event.toString());
                    handlerReturn.add(handlerInstance.process(event));
                }
            }
        }

        return handlerReturn;
    }

    private static PostgresActivityRecords processPostgresActivity(final KinesisEvent.KinesisEventRecord event) {

        final ByteBuffer bytes = event.getKinesis().getData();
        PostgresActivityRecords processedDatabaseActivity = null;

        try {

            final PostgresActivity activity = objectMapper.readValue(bytes.array(), PostgresActivity.class);
            Base64.Decoder decoder = Base64.getDecoder();

            byte[] decoded = decoder.decode(activity.getDatabaseActivityEvents().getBytes(StandardCharsets.UTF_8));
            byte[] datakey = decoder.decode(activity.getKey().getBytes(StandardCharsets.UTF_8));
            ByteBuffer key = ByteBuffer.wrap(datakey);

            DecryptRequest decryptRequest = DecryptRequest.builder()
                    .ciphertextBlob(SdkBytes.fromByteBuffer(key))
                    .encryptionContext(getContext(event.getEventSourceARN()))
                    .build();

            SdkHttpClient httpClient = ApacheHttpClient.builder().build();
            KmsClientBuilder client = KmsClient.builder().httpClient(httpClient);
            DecryptResponse decryptResult = client.build().decrypt(decryptRequest);

            final byte[] decompressed = DecryptUtils.decrypt(decoded, decryptResult.plaintext().asByteArray());

            processedDatabaseActivity = objectMapper.readValue(
                    new String(decompressed, StandardCharsets.UTF_8),
                    PostgresActivityRecords.class);

        } catch (IOException e) {
            LOG.error(e.getMessage());
        }

        return processedDatabaseActivity;
    }

    private static Map getContext(String arn) {

        Map<String, String> context = new HashMap<>();
        context.put("aws:rds:dbc-id", "cluster" + arn.substring(arn.lastIndexOf("-")));

        return context;
    }

    private static <R> DataStreamHandler<R> instantiatedHandler(final Class<? extends DataStreamHandler<R>> handler) {

        try {
            if (null == handler.getDeclaringClass()) {
                return handler.getDeclaredConstructor().newInstance();
            }

            final Constructor<? extends DataStreamHandler<R>> constructor = handler.getDeclaredConstructor(handler.getDeclaringClass());
            constructor.setAccessible(true);
            return constructor.newInstance(handler.getDeclaringClass().getDeclaredConstructor().newInstance());
        } catch (Exception e) {
            LOG.error("Failed creating handler instance", e);
            throw new RuntimeException("Unexpected error occurred. Please raise issue at " +
                    "https://github.com/awslabs/aws-lambda-powertools-java/issues", e);
        }
    }

}

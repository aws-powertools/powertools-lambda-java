package software.amazon.lambda.powertools.aurora;


import com.amazonaws.services.lambda.runtime.events.KinesisEvent;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import software.amazon.awssdk.http.SdkHttpClient;
import software.amazon.awssdk.services.kms.KmsClientBuilder;

import java.io.IOException;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static software.amazon.lambda.powertools.aurora.AuroraUtils.process;

class AuroraUtilsTest {

    private static final SdkHttpClient httpClient = mock(SdkHttpClient.class);
    private static KmsClientBuilder kmsClient = mock(KmsClientBuilder.class);
    private static final ObjectMapper MAPPER = new ObjectMapper();
    private KinesisEvent event;

    @BeforeEach
    void setUp() throws IOException {
        event = MAPPER.readValue(this.getClass().getResource("/kinesis_heartbeat.json"), KinesisEvent.class);
    }

    @ParameterizedTest
    @ValueSource(classes = {SampleStreamHandler.class})
    void shouldProcessHeartbeat(Class<? extends DataStreamHandler<Object>> handler) {
        List<Object> returnValues = process(event, false, handler);

        assertThat(returnValues).hasSize(1);
    }

    @ParameterizedTest
    @ValueSource(classes = {SampleStreamHandler.class})
    void shouldNotProcessHeartbeat(Class<? extends DataStreamHandler<Object>> handler) {
        List<Object> returnValues = process(event, true, handler);

        assertThat(returnValues).hasSize(0);
    }

}

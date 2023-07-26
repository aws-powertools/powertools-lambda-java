package software.amazon.lambda.powertools.testsuite;


import static java.util.Collections.emptyMap;
import static java.util.Collections.singletonList;
import static org.apache.commons.lang3.reflect.FieldUtils.writeStaticField;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.fail;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.mockito.MockitoAnnotations.openMocks;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.events.SQSEvent;
import com.amazonaws.services.lambda.runtime.events.models.s3.S3EventNotification;
import com.amazonaws.xray.AWSXRay;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.nio.channels.FileChannel;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.Map;
import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.ThreadContext;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import software.amazon.awssdk.core.ResponseInputStream;
import software.amazon.awssdk.http.AbortableInputStream;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.model.GetObjectResponse;
import software.amazon.lambda.powertools.core.internal.LambdaHandlerProcessor;
import software.amazon.lambda.powertools.logging.internal.LambdaLoggingAspect;
import software.amazon.lambda.powertools.sqs.SqsUtils;
import software.amazon.lambda.powertools.testsuite.handler.LoggingOrderMessageHandler;
import software.amazon.lambda.powertools.testsuite.handler.TracingLoggingStreamMessageHandler;

public class LoggingOrderTest {

    private static final String BUCKET_NAME = "ms-extended-sqs-client";
    private static final String BUCKET_KEY = "c71eb2ae-37e0-4265-8909-32f4153faddf";

    @Mock
    private Context context;

    @Mock
    private S3Client s3Client;

    @BeforeEach
    void setUp() throws IllegalAccessException, IOException, NoSuchMethodException, InvocationTargetException {
        openMocks(this);
        SqsUtils.overrideS3Client(s3Client);
        ThreadContext.clearAll();
        writeStaticField(LambdaHandlerProcessor.class, "IS_COLD_START", null, true);
        setupContext();
        //Make sure file is cleaned up before running full stack logging regression
        FileChannel.open(Paths.get("target/logfile.json"), StandardOpenOption.WRITE).truncate(0).close();
        resetLogLevel(Level.INFO);
        AWSXRay.beginSegment(LoggingOrderTest.class.getName());
    }

    @AfterEach
    void tearDown() {
        AWSXRay.endSegment();
    }

    /**
     * The SQSEvent payload will be altered by the @SqsLargeMessage annotation. Logging of the event should happen
     * after the event has been altered
     */
    @Test
    public void testThatLoggingAnnotationActsLast() throws IOException {
        ResponseInputStream<GetObjectResponse> s3Response =
                new ResponseInputStream<>(GetObjectResponse.builder().build(),
                        AbortableInputStream.create(new ByteArrayInputStream("A big message".getBytes())));

        when(s3Client.getObject(any(GetObjectRequest.class))).thenReturn(s3Response);
        SQSEvent sqsEvent = messageWithBody(
                "[\"software.amazon.payloadoffloading.PayloadS3Pointer\",{\"s3BucketName\":\"" + BUCKET_NAME +
                        "\",\"s3Key\":\"" + BUCKET_KEY + "\"}]");

        LoggingOrderMessageHandler requestHandler = new LoggingOrderMessageHandler();
        requestHandler.handleRequest(sqsEvent, context);

        assertThat(Files.lines(Paths.get("target/logfile.json")))
                .hasSize(2)
                .satisfies(line ->
                    {
                        Map<String, Object> actual = parseToMap(line.get(0));

                        String message = actual.get("message").toString();

                        assertThat(message)
                                .contains("A big message");
                    });
    }

    @Test
    public void testLoggingAnnotationActsAfterTracingForStreamingHandler() throws IOException {

        ByteArrayOutputStream output = new ByteArrayOutputStream();
        S3EventNotification s3EventNotification = s3EventNotification();

        TracingLoggingStreamMessageHandler handler = new TracingLoggingStreamMessageHandler();
        handler.handleRequest(new ByteArrayInputStream(new ObjectMapper().writeValueAsBytes(s3EventNotification)),
                output, context);

        assertThat(new String(output.toByteArray(), StandardCharsets.UTF_8))
                .isNotEmpty();
    }

    private void setupContext() {
        when(context.getFunctionName()).thenReturn("testFunction");
        when(context.getInvokedFunctionArn()).thenReturn("testArn");
        when(context.getFunctionVersion()).thenReturn("1");
        when(context.getMemoryLimitInMB()).thenReturn(10);
        when(context.getAwsRequestId()).thenReturn("RequestId");
    }

    private void resetLogLevel(Level level)
            throws NoSuchMethodException, IllegalAccessException, InvocationTargetException {
        Method resetLogLevels = LambdaLoggingAspect.class.getDeclaredMethod("resetLogLevels", Level.class);
        resetLogLevels.setAccessible(true);
        resetLogLevels.invoke(null, level);
        writeStaticField(LambdaLoggingAspect.class, "LEVEL_AT_INITIALISATION", level, true);
    }

    private Map<String, Object> parseToMap(String stringAsJson) {
        try {
            return new ObjectMapper().readValue(stringAsJson, Map.class);
        } catch (JsonProcessingException e) {
            fail("Failed parsing logger line " + stringAsJson);
            return emptyMap();
        }
    }

    private S3EventNotification s3EventNotification() {
        S3EventNotification.S3EventNotificationRecord record =
                new S3EventNotification.S3EventNotificationRecord("us-west-2",
                        "ObjectCreated:Put",
                        "aws:s3",
                        null,
                        "2.1",
                        new S3EventNotification.RequestParametersEntity("127.0.0.1"),
                        new S3EventNotification.ResponseElementsEntity("C3D13FE58DE4C810",
                                "FMyUVURIY8/IgAtTv8xRjskZQpcIZ9KG4V5Wp6S7S/JRWeUWerMUE5JgHvANOjpD"),
                        new S3EventNotification.S3Entity("testConfigRule",
                                new S3EventNotification.S3BucketEntity("mybucket",
                                        new S3EventNotification.UserIdentityEntity("A3NL1KOZZKExample"),
                                        "arn:aws:s3:::mybucket"),
                                new S3EventNotification.S3ObjectEntity("HappyFace.jpg",
                                        1024L,
                                        "d41d8cd98f00b204e9800998ecf8427e",
                                        "096fKKXTRTtl3on89fVO.nfljtsv6qko",
                                        "0055AED6DCD90281E5"),
                                "1.0"),
                        new S3EventNotification.UserIdentityEntity("AIDAJDPLRKLG7UEXAMPLE")
                );

        return new S3EventNotification(singletonList(record));
    }

    private SQSEvent messageWithBody(String messageBody) {
        SQSEvent.SQSMessage sqsMessage = new SQSEvent.SQSMessage();
        sqsMessage.setBody(messageBody);
        SQSEvent sqsEvent = new SQSEvent();
        sqsEvent.setRecords(singletonList(sqsMessage));
        return sqsEvent;
    }
}
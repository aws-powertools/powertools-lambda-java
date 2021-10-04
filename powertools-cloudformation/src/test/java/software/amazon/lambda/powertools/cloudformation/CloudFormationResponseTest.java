package software.amazon.lambda.powertools.cloudformation;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.events.CloudFormationCustomResourceEvent;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.junit.jupiter.api.Test;
import software.amazon.awssdk.http.AbortableInputStream;
import software.amazon.awssdk.http.ExecutableHttpRequest;
import software.amazon.awssdk.http.HttpExecuteRequest;
import software.amazon.awssdk.http.HttpExecuteResponse;
import software.amazon.awssdk.http.SdkHttpClient;
import software.amazon.awssdk.utils.IoUtils;
import software.amazon.lambda.powertools.cloudformation.CloudFormationResponse.ResponseBody;
import software.amazon.lambda.powertools.cloudformation.CloudFormationResponse.ResponseStatus;

import java.io.IOException;
import java.io.InputStream;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class CloudFormationResponseTest {

    /**
     * Creates a mock CloudFormationCustomResourceEvent with a non-null response URL.
     */
    static CloudFormationCustomResourceEvent mockCloudFormationCustomResourceEvent() {
        CloudFormationCustomResourceEvent event = mock(CloudFormationCustomResourceEvent.class);
        when(event.getResponseUrl()).thenReturn("https://aws.amazon.com");
        return event;
    }

    /**
     * Creates a mock CloudFormationResponse whose response body is the request body.
     */
    static CloudFormationResponse mockCloudFormationResponse() {
        SdkHttpClient client = mock(SdkHttpClient.class);
        ExecutableHttpRequest executableRequest = mock(ExecutableHttpRequest.class);

        when(client.prepareRequest(any(HttpExecuteRequest.class))).thenAnswer(args -> {
            HttpExecuteRequest request = args.getArgument(0, HttpExecuteRequest.class);
            assertThat(request.contentStreamProvider()).isPresent();

            InputStream inputStream = request.contentStreamProvider().get().newStream();
            HttpExecuteResponse response = mock(HttpExecuteResponse.class);
            when(response.responseBody()).thenReturn(Optional.of(AbortableInputStream.create(inputStream)));
            when(executableRequest.call()).thenReturn(response);
            return executableRequest;
        });

        return new CloudFormationResponse(client);
    }

    static String responseAsString(HttpExecuteResponse response) throws IOException {
        assertThat(response.responseBody()).isPresent();
        InputStream bodyStream = response.responseBody().orElse(null);
        return bodyStream == null ? null : IoUtils.toUtf8String(bodyStream);
    }

    @Test
    void clientRequiredToCreateInstance() {
        assertThatThrownBy(() -> new CloudFormationResponse(null))
                .isInstanceOf(NullPointerException.class);
    }

    @Test
    void eventRequiredToSend() {
        SdkHttpClient client = mock(SdkHttpClient.class);
        CloudFormationResponse response = new CloudFormationResponse(client);

        Context context = mock(Context.class);
        assertThatThrownBy(() -> response.send(null, context, ResponseStatus.SUCCESS))
                .isInstanceOf(ResponseException.class);
    }

    @Test
    void contextRequiredToSend() {
        SdkHttpClient client = mock(SdkHttpClient.class);
        CloudFormationResponse response = new CloudFormationResponse(client);

        Context context = mock(Context.class);
        assertThatThrownBy(() -> response.send(null, context, ResponseStatus.SUCCESS))
                .isInstanceOf(ResponseException.class);
    }

    @Test
    void eventResponseUrlRequiredToSend() {
        SdkHttpClient client = mock(SdkHttpClient.class);
        CloudFormationResponse response = new CloudFormationResponse(client);

        CloudFormationCustomResourceEvent event = mock(CloudFormationCustomResourceEvent.class);
        Context context = mock(Context.class);
        // not a ResponseSerializationException since the URL is not part of the response but
        // rather the location the response is sent to
        assertThatThrownBy(() -> response.send(event, context, ResponseStatus.SUCCESS))
                .isInstanceOf(RuntimeException.class);
    }

    @Test
    void defaultPhysicalResponseIdIsLogStreamName() {
        CloudFormationCustomResourceEvent event = mockCloudFormationCustomResourceEvent();
        when(event.getPhysicalResourceId()).thenReturn("This-Is-Ignored");

        String logStreamName = "My-Log-Stream-Name";
        Context context = mock(Context.class);
        when(context.getLogStreamName()).thenReturn(logStreamName);

        ResponseBody body = new ResponseBody(
                event, context, ResponseStatus.SUCCESS, null, false);
        assertThat(body.getPhysicalResourceId()).isEqualTo(logStreamName);
    }

    @Test
    void customPhysicalResponseId() {
        CloudFormationCustomResourceEvent event = mockCloudFormationCustomResourceEvent();
        when(event.getPhysicalResourceId()).thenReturn("This-Is-Ignored");

        Context context = mock(Context.class);
        when(context.getLogStreamName()).thenReturn("My-Log-Stream-Name");

        String customPhysicalResourceId = "Custom-Physical-Resource-ID";
        ResponseBody body = new ResponseBody(
                event, context, ResponseStatus.SUCCESS, customPhysicalResourceId, false);
        assertThat(body.getPhysicalResourceId()).isEqualTo(customPhysicalResourceId);
    }

    @Test
    void responseBodyWithNullDataNode() {
        CloudFormationCustomResourceEvent event = mockCloudFormationCustomResourceEvent();
        Context context = mock(Context.class);

        ResponseBody responseBody = new ResponseBody(event, context, ResponseStatus.FAILED, null, true);
        String actualJson = responseBody.toObjectNode(null).toString();

        String expectedJson = "{" +
                "\"Status\":\"FAILED\"," +
                "\"Reason\":\"See the details in CloudWatch Log Stream: null\"," +
                "\"PhysicalResourceId\":null," +
                "\"StackId\":null," +
                "\"RequestId\":null," +
                "\"LogicalResourceId\":null," +
                "\"NoEcho\":true," +
                "\"Data\":null" +
                "}";
        assertThat(actualJson).isEqualTo(expectedJson);
    }

    @Test
    void responseBodyWithNonNullDataNode() {
        CloudFormationCustomResourceEvent event = mockCloudFormationCustomResourceEvent();
        Context context = mock(Context.class);
        ObjectNode dataNode = ResponseBody.MAPPER.createObjectNode();
        dataNode.put("foo", "bar");
        dataNode.put("baz", 10);

        ResponseBody responseBody = new ResponseBody(event, context, ResponseStatus.FAILED, null, true);
        String actualJson = responseBody.toObjectNode(dataNode).toString();

        String expectedJson = "{" +
                "\"Status\":\"FAILED\"," +
                "\"Reason\":\"See the details in CloudWatch Log Stream: null\"," +
                "\"PhysicalResourceId\":null," +
                "\"StackId\":null," +
                "\"RequestId\":null," +
                "\"LogicalResourceId\":null," +
                "\"NoEcho\":true," +
                "\"Data\":{\"foo\":\"bar\",\"baz\":10}" +
                "}";
        assertThat(actualJson).isEqualTo(expectedJson);
    }

    @Test
    void defaultStatusIsSuccess() {
        CloudFormationCustomResourceEvent event = mockCloudFormationCustomResourceEvent();
        Context context = mock(Context.class);

        ResponseBody body = new ResponseBody(
                event, context, null, null, false);
        assertThat(body.getStatus()).isEqualTo("SUCCESS");
    }

    @Test
    void customStatus() {
        CloudFormationCustomResourceEvent event = mockCloudFormationCustomResourceEvent();
        Context context = mock(Context.class);

        ResponseBody body = new ResponseBody(
                event, context, ResponseStatus.FAILED, null, false);
        assertThat(body.getStatus()).isEqualTo("FAILED");
    }

    @Test
    void reasonIncludesLogStreamName() {
        CloudFormationCustomResourceEvent event = mockCloudFormationCustomResourceEvent();

        String logStreamName = "My-Log-Stream-Name";
        Context context = mock(Context.class);
        when(context.getLogStreamName()).thenReturn(logStreamName);

        ResponseBody body = new ResponseBody(
                event, context, ResponseStatus.SUCCESS, null, false);
        assertThat(body.getReason()).contains(logStreamName);
    }

    @Test
    public void sendWithNoResponseData() throws Exception {
        CloudFormationCustomResourceEvent event = mockCloudFormationCustomResourceEvent();
        Context context = mock(Context.class);
        CloudFormationResponse cfnResponse = mockCloudFormationResponse();

        HttpExecuteResponse response = cfnResponse.send(event, context, ResponseStatus.SUCCESS);

        String actualJson = responseAsString(response);
        String expectedJson = "{" +
                "\"Status\":\"SUCCESS\"," +
                "\"Reason\":\"See the details in CloudWatch Log Stream: null\"," +
                "\"PhysicalResourceId\":null," +
                "\"StackId\":null," +
                "\"RequestId\":null," +
                "\"LogicalResourceId\":null," +
                "\"NoEcho\":false," +
                "\"Data\":null" +
                "}";
        assertThat(actualJson).isEqualTo(expectedJson);
    }

    @Test
    public void sendWithNonNullResponseData() throws Exception {
        CloudFormationCustomResourceEvent event = mockCloudFormationCustomResourceEvent();
        Context context = mock(Context.class);
        CloudFormationResponse cfnResponse = mockCloudFormationResponse();

        Map<String, String> responseData = new LinkedHashMap<>();
        responseData.put("Property", "Value");
        Response resp = Response.builder().value(responseData).build();

        HttpExecuteResponse response = cfnResponse.send(event, context, ResponseStatus.SUCCESS, resp);

        String actualJson = responseAsString(response);
        String expectedJson = "{" +
                "\"Status\":\"SUCCESS\"," +
                "\"Reason\":\"See the details in CloudWatch Log Stream: null\"," +
                "\"PhysicalResourceId\":null," +
                "\"StackId\":null," +
                "\"RequestId\":null," +
                "\"LogicalResourceId\":null," +
                "\"NoEcho\":false," +
                "\"Data\":{\"Property\":\"Value\"}" +
                "}";
        assertThat(actualJson).isEqualTo(expectedJson);
    }
}

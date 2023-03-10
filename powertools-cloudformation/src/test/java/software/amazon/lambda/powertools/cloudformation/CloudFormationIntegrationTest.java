package software.amazon.lambda.powertools.cloudformation;

import com.amazonaws.services.lambda.runtime.ClientContext;
import com.amazonaws.services.lambda.runtime.CognitoIdentity;
import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.LambdaLogger;
import com.amazonaws.services.lambda.runtime.events.CloudFormationCustomResourceEvent;
import com.github.tomakehurst.wiremock.junit5.WireMockRuntimeInfo;
import com.github.tomakehurst.wiremock.junit5.WireMockTest;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import software.amazon.lambda.powertools.cloudformation.handlers.NoPhysicalResourceIdSetHandler;
import software.amazon.lambda.powertools.cloudformation.handlers.PhysicalResourceIdSetHandler;
import software.amazon.lambda.powertools.cloudformation.handlers.RuntimeExceptionThrownHandler;

import java.util.UUID;

import static com.github.tomakehurst.wiremock.client.WireMock.*;
import static org.assertj.core.api.Assertions.assertThat;

@WireMockTest
public class CloudFormationIntegrationTest {

    public static final String PHYSICAL_RESOURCE_ID = UUID.randomUUID().toString();
    public static final String LOG_STREAM_NAME = "FakeLogStreamName";

    @ParameterizedTest
    @ValueSource(strings = {"Update", "Delete"})
    void physicalResourceIdTakenFromRequestForUpdateOrDeleteWhenUserSpecifiesNull(String requestType, WireMockRuntimeInfo wmRuntimeInfo) {
        stubFor(put("/").willReturn(ok()));

        NoPhysicalResourceIdSetHandler handler = new NoPhysicalResourceIdSetHandler();
        int httpPort = wmRuntimeInfo.getHttpPort();

        CloudFormationCustomResourceEvent event = baseEvent(httpPort)
                .withPhysicalResourceId(PHYSICAL_RESOURCE_ID)
                .withRequestType(requestType)
                .build();

        handler.handleRequest(event, new FakeContext());

        verify(putRequestedFor(urlPathMatching("/"))
                .withRequestBody(matchingJsonPath("[?(@.Status == 'SUCCESS')]"))
                .withRequestBody(matchingJsonPath("[?(@.PhysicalResourceId == '" + PHYSICAL_RESOURCE_ID + "')]"))
        );
    }

    @ParameterizedTest
    @ValueSource(strings = {"Update", "Delete"})
    void physicalResourceIdDoesNotChangeWhenRuntimeExceptionThrownWhenUpdatingOrDeleting(String requestType, WireMockRuntimeInfo wmRuntimeInfo)  {
        stubFor(put("/").willReturn(ok()));

        RuntimeExceptionThrownHandler handler = new RuntimeExceptionThrownHandler();
        int httpPort = wmRuntimeInfo.getHttpPort();

        CloudFormationCustomResourceEvent event = baseEvent(httpPort)
                .withPhysicalResourceId(PHYSICAL_RESOURCE_ID)
                .withRequestType(requestType)
                .build();

        handler.handleRequest(event, new FakeContext());

        verify(putRequestedFor(urlPathMatching("/"))
                .withRequestBody(matchingJsonPath("[?(@.Status == 'FAILED')]"))
                .withRequestBody(matchingJsonPath("[?(@.PhysicalResourceId == '" + PHYSICAL_RESOURCE_ID + "')]"))
        );
    }

    @Test
    void runtimeExceptionThrownOnCreateSendsLogStreamNameAsPhysicalResourceId(WireMockRuntimeInfo wmRuntimeInfo)  {
        stubFor(put("/").willReturn(ok()));

        RuntimeExceptionThrownHandler handler = new RuntimeExceptionThrownHandler();
        CloudFormationCustomResourceEvent createEvent = baseEvent(wmRuntimeInfo.getHttpPort())
                .withRequestType("Create")
                .build();
        handler.handleRequest(createEvent, new FakeContext());

        verify(putRequestedFor(urlPathMatching("/"))
                .withRequestBody(matchingJsonPath("[?(@.Status == 'FAILED')]"))
                .withRequestBody(matchingJsonPath("[?(@.PhysicalResourceId == '" + LOG_STREAM_NAME + "')]"))
        );
    }

    @ParameterizedTest
    @ValueSource(strings = {"Update", "Delete"})
    void physicalResourceIdSetFromRequestOnUpdateOrDeleteWhenCustomerDoesntProvideAPhysicalResourceId(String requestType, WireMockRuntimeInfo wmRuntimeInfo) {
        stubFor(put("/").willReturn(ok()));

        NoPhysicalResourceIdSetHandler handler = new NoPhysicalResourceIdSetHandler();
        int httpPort = wmRuntimeInfo.getHttpPort();

        CloudFormationCustomResourceEvent event = baseEvent(httpPort)
                .withPhysicalResourceId(PHYSICAL_RESOURCE_ID)
                .withRequestType(requestType)
                .build();

        Response response = handler.handleRequest(event, new FakeContext());

        assertThat(response).isNotNull();
        verify(putRequestedFor(urlPathMatching("/"))
                .withRequestBody(matchingJsonPath("[?(@.Status == 'SUCCESS')]"))
                .withRequestBody(matchingJsonPath("[?(@.PhysicalResourceId == '" + PHYSICAL_RESOURCE_ID + "')]"))
        );
    }

    @Test
    void createNewResourceBecausePhysicalResourceIdNotSetByCustomerOnCreate(WireMockRuntimeInfo wmRuntimeInfo) {
        stubFor(put("/").willReturn(ok()));

        NoPhysicalResourceIdSetHandler handler = new NoPhysicalResourceIdSetHandler();
        CloudFormationCustomResourceEvent createEvent = baseEvent(wmRuntimeInfo.getHttpPort())
                .withRequestType("Create")
                .build();
        Response response = handler.handleRequest(createEvent, new FakeContext());

        assertThat(response).isNotNull();
        verify(putRequestedFor(urlPathMatching("/"))
                .withRequestBody(matchingJsonPath("[?(@.Status == 'SUCCESS')]"))
                .withRequestBody(matchingJsonPath("[?(@.PhysicalResourceId == '" + LOG_STREAM_NAME + "')]"))
        );
    }

    @ParameterizedTest
    @ValueSource(strings = {"Create", "Update", "Delete"})
    void physicalResourceIdReturnedFromSuccessToCloudformation(String requestType, WireMockRuntimeInfo wmRuntimeInfo) {

        String physicalResourceId = UUID.randomUUID().toString();

        PhysicalResourceIdSetHandler handler = new PhysicalResourceIdSetHandler(physicalResourceId, true);
        CloudFormationCustomResourceEvent createEvent = baseEvent(wmRuntimeInfo.getHttpPort())
                .withRequestType(requestType)
                .build();
        Response response = handler.handleRequest(createEvent, new FakeContext());

        assertThat(response).isNotNull();
        verify(putRequestedFor(urlPathMatching("/"))
                .withRequestBody(matchingJsonPath("[?(@.Status == 'SUCCESS')]"))
                .withRequestBody(matchingJsonPath("[?(@.PhysicalResourceId == '" + physicalResourceId + "')]"))
        );
    }

    @ParameterizedTest
    @ValueSource(strings = {"Create", "Update", "Delete"})
    void physicalResourceIdReturnedFromFailedToCloudformation(String requestType, WireMockRuntimeInfo wmRuntimeInfo) {

        String physicalResourceId = UUID.randomUUID().toString();

        PhysicalResourceIdSetHandler handler = new PhysicalResourceIdSetHandler(physicalResourceId, false);
        CloudFormationCustomResourceEvent createEvent = baseEvent(wmRuntimeInfo.getHttpPort())
                .withRequestType(requestType)
                .build();
        Response response = handler.handleRequest(createEvent, new FakeContext());

        assertThat(response).isNotNull();
        verify(putRequestedFor(urlPathMatching("/"))
                .withRequestBody(matchingJsonPath("[?(@.Status == 'FAILED')]"))
                .withRequestBody(matchingJsonPath("[?(@.PhysicalResourceId == '" + physicalResourceId + "')]"))
        );
    }

    private static CloudFormationCustomResourceEvent updateEventWithPhysicalResourceId(int httpPort, String physicalResourceId) {
        CloudFormationCustomResourceEvent.CloudFormationCustomResourceEventBuilder builder = baseEvent(httpPort);

        builder.withPhysicalResourceId(physicalResourceId);
        builder.withRequestType("Update");

        return builder.build();
    }

    private static CloudFormationCustomResourceEvent deleteEventWithPhysicalResourceId(int httpPort, String physicalResourceId) {
        CloudFormationCustomResourceEvent.CloudFormationCustomResourceEventBuilder builder = baseEvent(httpPort);

        builder.withPhysicalResourceId(physicalResourceId);
        builder.withRequestType("Delete");

        return builder.build();
    }

    private static CloudFormationCustomResourceEvent.CloudFormationCustomResourceEventBuilder baseEvent(int httpPort) {
        CloudFormationCustomResourceEvent.CloudFormationCustomResourceEventBuilder builder = CloudFormationCustomResourceEvent.builder()
                .withResponseUrl("http://localhost:" + httpPort + "/")
                .withStackId("123")
                .withRequestId("234")
                .withLogicalResourceId("345");

        return builder;
    }

    private static class FakeContext implements Context {
        @Override
        public String getAwsRequestId() {
            return null;
        }

        @Override
        public String getLogGroupName() {
            return null;
        }

        @Override
        public String getLogStreamName() {
            return LOG_STREAM_NAME;
        }

        @Override
        public String getFunctionName() {
            return null;
        }

        @Override
        public String getFunctionVersion() {
            return null;
        }

        @Override
        public String getInvokedFunctionArn() {
            return null;
        }

        @Override
        public CognitoIdentity getIdentity() {
            return null;
        }

        @Override
        public ClientContext getClientContext() {
            return null;
        }

        @Override
        public int getRemainingTimeInMillis() {
            return 0;
        }

        @Override
        public int getMemoryLimitInMB() {
            return 0;
        }

        @Override
        public LambdaLogger getLogger() {
            return null;
        }
    }
}

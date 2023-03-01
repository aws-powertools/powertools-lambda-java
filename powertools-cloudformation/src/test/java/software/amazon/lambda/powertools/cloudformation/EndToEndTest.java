// Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
// SPDX-License-Identifier: MIT-0

package software.amazon.lambda.powertools.cloudformation;

import com.amazonaws.services.lambda.runtime.ClientContext;
import com.amazonaws.services.lambda.runtime.CognitoIdentity;
import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.LambdaLogger;
import com.amazonaws.services.lambda.runtime.events.CloudFormationCustomResourceEvent;
import com.github.tomakehurst.wiremock.junit5.WireMockRuntimeInfo;
import com.github.tomakehurst.wiremock.junit5.WireMockTest;
import org.junit.jupiter.api.Test;
import software.amazon.lambda.powertools.cloudformation.handlers.NoPhysicalResourceIdSetHandler;
import software.amazon.lambda.powertools.cloudformation.handlers.UpdateCausesRuntimeException;

import java.util.UUID;

import static com.github.tomakehurst.wiremock.client.WireMock.*;
import static org.assertj.core.api.Assertions.assertThat;

@WireMockTest
public class EndToEndTest {

    public static final String PHYSICAL_RESOURCE_ID = UUID.randomUUID().toString();

    @Test
    void physicalResourceIdDoesNotChangeWhenRuntimeExceptionThrownWhenUpdating(WireMockRuntimeInfo wmRuntimeInfo)  {
        stubFor(put("/").willReturn(ok()));

        UpdateCausesRuntimeException handler = new UpdateCausesRuntimeException();
        CloudFormationCustomResourceEvent updateEvent = updateEventWithPhysicalResourceId(wmRuntimeInfo.getHttpPort(), PHYSICAL_RESOURCE_ID);
        Response response = handler.handleRequest(updateEvent, new FakeContext());

        assertThat(response).isNull();
        verify(putRequestedFor(urlPathMatching("/"))
                .withRequestBody(matchingJsonPath("[?(@.Status == 'FAILED')]"))
                .withRequestBody(matchingJsonPath("[?(@.PhysicalResourceId == '" + PHYSICAL_RESOURCE_ID + "')]"))
        );
    }

    // Existing functionality - could well be incorrect
    @Test
    void physicalResourceIdSetAsLogStreamOnUpdateWhenCustomerDoesntProvideAPhysicalResourceId(WireMockRuntimeInfo wmRuntimeInfo) {
        stubFor(put("/").willReturn(ok()));

        NoPhysicalResourceIdSetHandler handler = new NoPhysicalResourceIdSetHandler();
        CloudFormationCustomResourceEvent updateEvent = updateEventWithPhysicalResourceId(wmRuntimeInfo.getHttpPort(), PHYSICAL_RESOURCE_ID);
        Response response = handler.handleRequest(updateEvent, new FakeContext());

        assertThat(response).isNotNull();
        verify(putRequestedFor(urlPathMatching("/"))
                .withRequestBody(matchingJsonPath("[?(@.Status == 'SUCCESS')]"))
                .withRequestBody(matchingJsonPath("[?(@.PhysicalResourceId == 'FakeLogStreamName')]"))
        );
    }

    @Test
    void physicalResourceIdSetAsLogStreamOnDeleteWhenCustomerDoesntProvideAPhysicalResourceId(WireMockRuntimeInfo wmRuntimeInfo) {
        stubFor(put("/").willReturn(ok()));

        NoPhysicalResourceIdSetHandler handler = new NoPhysicalResourceIdSetHandler();
        CloudFormationCustomResourceEvent createEvent = deleteEventWithPhysicalResourceId(wmRuntimeInfo.getHttpPort(), PHYSICAL_RESOURCE_ID);
        Response response = handler.handleRequest(createEvent, new FakeContext());

        assertThat(response).isNotNull();
        verify(putRequestedFor(urlPathMatching("/"))
                .withRequestBody(matchingJsonPath("[?(@.Status == 'SUCCESS')]"))
                .withRequestBody(matchingJsonPath("[?(@.PhysicalResourceId == 'FakeLogStreamName')]"))
        );
    }

    @Test
    void createNewResourceBecausePhysicalResourceIdNoSetByCustomerOnCreate(WireMockRuntimeInfo wmRuntimeInfo) {
        stubFor(put("/").willReturn(ok()));

        NoPhysicalResourceIdSetHandler handler = new NoPhysicalResourceIdSetHandler();
        CloudFormationCustomResourceEvent createEvent = baseEvent(wmRuntimeInfo.getHttpPort())
                .withRequestType("Create")
                .build();
        Response response = handler.handleRequest(createEvent, new FakeContext());

        assertThat(response).isNotNull();
        verify(putRequestedFor(urlPathMatching("/"))
                .withRequestBody(matchingJsonPath("[?(@.Status == 'SUCCESS')]"))
                .withRequestBody(matchingJsonPath("[?(@.PhysicalResourceId == 'FakeLogStreamName')]"))
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
            return "FakeLogStreamName";
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

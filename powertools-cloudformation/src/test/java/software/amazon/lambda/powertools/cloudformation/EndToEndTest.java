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
        CloudFormationCustomResourceEvent event = eventWithPhysicalResourceId(wmRuntimeInfo.getHttpPort(), PHYSICAL_RESOURCE_ID);
        Response response = handler.handleRequest(event, new FakeContext());

        assertThat(response).isNull();
        verify(putRequestedFor(urlPathMatching("/"))
                .withRequestBody(matchingJsonPath("[?(@.PhysicalResourceId == '" + PHYSICAL_RESOURCE_ID + "')]"))
        );
    }

    private static CloudFormationCustomResourceEvent eventWithPhysicalResourceId(int httpPort, String physicalResourceId) {
        CloudFormationCustomResourceEvent event = new CloudFormationCustomResourceEvent();
        event.setRequestType("Update");
        event.setResponseUrl("http://localhost:" + httpPort + "/");
        event.setStackId("123");
        event.setRequestId("234");
        event.setLogicalResourceId("345");
        event.setPhysicalResourceId(physicalResourceId);
        return event;
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
            return "LogStreamName";
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

/*
 * Copyright 2023 Amazon.com, Inc. or its affiliates.
 * Licensed under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *     http://www.apache.org/licenses/LICENSE-2.0
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 */

package software.amazon.lambda.powertools.cloudformation;

import static com.github.tomakehurst.wiremock.client.WireMock.matchingJsonPath;
import static com.github.tomakehurst.wiremock.client.WireMock.ok;
import static com.github.tomakehurst.wiremock.client.WireMock.put;
import static com.github.tomakehurst.wiremock.client.WireMock.putRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.stubFor;
import static com.github.tomakehurst.wiremock.client.WireMock.urlPathMatching;
import static com.github.tomakehurst.wiremock.client.WireMock.verify;
import static org.assertj.core.api.Assertions.assertThat;

import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import com.amazonaws.services.lambda.runtime.events.CloudFormationCustomResourceEvent;
import com.github.tomakehurst.wiremock.junit5.WireMockRuntimeInfo;
import com.github.tomakehurst.wiremock.junit5.WireMockTest;

import software.amazon.lambda.powertools.cloudformation.handlers.NoPhysicalResourceIdSetHandler;
import software.amazon.lambda.powertools.cloudformation.handlers.PhysicalResourceIdSetHandler;
import software.amazon.lambda.powertools.cloudformation.handlers.RuntimeExceptionThrownHandler;
import software.amazon.lambda.powertools.common.stubs.TestLambdaContext;

@WireMockTest
public class CloudFormationIntegrationTest {

    public static final String PHYSICAL_RESOURCE_ID = UUID.randomUUID().toString();
    public static final String LOG_STREAM_NAME = "test-log-stream";

    private static CloudFormationCustomResourceEvent.CloudFormationCustomResourceEventBuilder baseEvent(int httpPort) {
        CloudFormationCustomResourceEvent.CloudFormationCustomResourceEventBuilder builder = CloudFormationCustomResourceEvent
                .builder()
                .withResponseUrl("http://localhost:" + httpPort + "/")
                .withStackId("123")
                .withRequestId("234")
                .withLogicalResourceId("345");

        return builder;
    }

    @ParameterizedTest
    @ValueSource(strings = { "Update", "Delete" })
    void physicalResourceIdTakenFromRequestForUpdateOrDeleteWhenUserSpecifiesNull(String requestType,
            WireMockRuntimeInfo wmRuntimeInfo) {
        stubFor(put("/").willReturn(ok()));

        NoPhysicalResourceIdSetHandler handler = new NoPhysicalResourceIdSetHandler();
        int httpPort = wmRuntimeInfo.getHttpPort();

        CloudFormationCustomResourceEvent event = baseEvent(httpPort)
                .withPhysicalResourceId(PHYSICAL_RESOURCE_ID)
                .withRequestType(requestType)
                .build();

        handler.handleRequest(event, new TestLambdaContext());

        verify(putRequestedFor(urlPathMatching("/"))
                .withRequestBody(matchingJsonPath("[?(@.Status == 'SUCCESS')]"))
                .withRequestBody(matchingJsonPath("[?(@.PhysicalResourceId == '" + PHYSICAL_RESOURCE_ID + "')]")));
    }

    @ParameterizedTest
    @ValueSource(strings = { "Update", "Delete" })
    void physicalResourceIdDoesNotChangeWhenRuntimeExceptionThrownWhenUpdatingOrDeleting(String requestType,
            WireMockRuntimeInfo wmRuntimeInfo) {
        stubFor(put("/").willReturn(ok()));

        RuntimeExceptionThrownHandler handler = new RuntimeExceptionThrownHandler();
        int httpPort = wmRuntimeInfo.getHttpPort();

        CloudFormationCustomResourceEvent event = baseEvent(httpPort)
                .withPhysicalResourceId(PHYSICAL_RESOURCE_ID)
                .withRequestType(requestType)
                .build();

        handler.handleRequest(event, new TestLambdaContext());

        verify(putRequestedFor(urlPathMatching("/"))
                .withRequestBody(matchingJsonPath("[?(@.Status == 'FAILED')]"))
                .withRequestBody(matchingJsonPath("[?(@.PhysicalResourceId == '" + PHYSICAL_RESOURCE_ID + "')]")));
    }

    @Test
    void runtimeExceptionThrownOnCreateSendsLogStreamNameAsPhysicalResourceId(WireMockRuntimeInfo wmRuntimeInfo) {
        stubFor(put("/").willReturn(ok()));

        RuntimeExceptionThrownHandler handler = new RuntimeExceptionThrownHandler();
        CloudFormationCustomResourceEvent createEvent = baseEvent(wmRuntimeInfo.getHttpPort())
                .withRequestType("Create")
                .build();
        handler.handleRequest(createEvent, new TestLambdaContext());

        verify(putRequestedFor(urlPathMatching("/"))
                .withRequestBody(matchingJsonPath("[?(@.Status == 'FAILED')]"))
                .withRequestBody(matchingJsonPath("[?(@.PhysicalResourceId == '" + LOG_STREAM_NAME + "')]")));
    }

    @ParameterizedTest
    @ValueSource(strings = { "Update", "Delete" })
    void physicalResourceIdSetFromRequestOnUpdateOrDeleteWhenCustomerDoesntProvideAPhysicalResourceId(
            String requestType, WireMockRuntimeInfo wmRuntimeInfo) {
        stubFor(put("/").willReturn(ok()));

        NoPhysicalResourceIdSetHandler handler = new NoPhysicalResourceIdSetHandler();
        int httpPort = wmRuntimeInfo.getHttpPort();

        CloudFormationCustomResourceEvent event = baseEvent(httpPort)
                .withPhysicalResourceId(PHYSICAL_RESOURCE_ID)
                .withRequestType(requestType)
                .build();

        Response response = handler.handleRequest(event, new TestLambdaContext());

        assertThat(response).isNotNull();
        verify(putRequestedFor(urlPathMatching("/"))
                .withRequestBody(matchingJsonPath("[?(@.Status == 'SUCCESS')]"))
                .withRequestBody(matchingJsonPath("[?(@.PhysicalResourceId == '" + PHYSICAL_RESOURCE_ID + "')]")));
    }

    @Test
    void createNewResourceBecausePhysicalResourceIdNotSetByCustomerOnCreate(WireMockRuntimeInfo wmRuntimeInfo) {
        stubFor(put("/").willReturn(ok()));

        NoPhysicalResourceIdSetHandler handler = new NoPhysicalResourceIdSetHandler();
        CloudFormationCustomResourceEvent createEvent = baseEvent(wmRuntimeInfo.getHttpPort())
                .withRequestType("Create")
                .build();
        Response response = handler.handleRequest(createEvent, new TestLambdaContext());

        assertThat(response).isNotNull();
        verify(putRequestedFor(urlPathMatching("/"))
                .withRequestBody(matchingJsonPath("[?(@.Status == 'SUCCESS')]"))
                .withRequestBody(matchingJsonPath("[?(@.PhysicalResourceId == '" + LOG_STREAM_NAME + "')]")));
    }

    @ParameterizedTest
    @ValueSource(strings = { "Create", "Update", "Delete" })
    void physicalResourceIdReturnedFromSuccessToCloudformation(String requestType, WireMockRuntimeInfo wmRuntimeInfo) {

        String physicalResourceId = UUID.randomUUID().toString();

        PhysicalResourceIdSetHandler handler = new PhysicalResourceIdSetHandler(physicalResourceId, true);
        CloudFormationCustomResourceEvent createEvent = baseEvent(wmRuntimeInfo.getHttpPort())
                .withRequestType(requestType)
                .build();
        Response response = handler.handleRequest(createEvent, new TestLambdaContext());

        assertThat(response).isNotNull();
        verify(putRequestedFor(urlPathMatching("/"))
                .withRequestBody(matchingJsonPath("[?(@.Status == 'SUCCESS')]"))
                .withRequestBody(matchingJsonPath("[?(@.PhysicalResourceId == '" + physicalResourceId + "')]")));
    }

    @ParameterizedTest
    @ValueSource(strings = { "Create", "Update", "Delete" })
    void physicalResourceIdReturnedFromFailedToCloudformation(String requestType, WireMockRuntimeInfo wmRuntimeInfo) {

        String physicalResourceId = UUID.randomUUID().toString();

        PhysicalResourceIdSetHandler handler = new PhysicalResourceIdSetHandler(physicalResourceId, false);
        CloudFormationCustomResourceEvent createEvent = baseEvent(wmRuntimeInfo.getHttpPort())
                .withRequestType(requestType)
                .build();
        Response response = handler.handleRequest(createEvent, new TestLambdaContext());

        assertThat(response).isNotNull();
        verify(putRequestedFor(urlPathMatching("/"))
                .withRequestBody(matchingJsonPath("[?(@.Status == 'FAILED')]"))
                .withRequestBody(matchingJsonPath("[?(@.PhysicalResourceId == '" + physicalResourceId + "')]")));
    }

}

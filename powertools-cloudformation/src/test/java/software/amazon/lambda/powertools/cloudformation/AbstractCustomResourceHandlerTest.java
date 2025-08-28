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

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.io.IOException;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.events.CloudFormationCustomResourceEvent;

import software.amazon.awssdk.http.SdkHttpClient;
import software.amazon.lambda.powertools.common.stubs.TestLambdaContext;

public class AbstractCustomResourceHandlerTest {

    /**
     * Builds a valid Event with the provide request type.
     */
    static CloudFormationCustomResourceEvent eventOfType(String requestType) {
        CloudFormationCustomResourceEvent event = new CloudFormationCustomResourceEvent();
        event.setResponseUrl("https://mandatory-url.amazon.com");
        event.setRequestType(requestType);
        return event;
    }

    @Test
    void defaultAndCustomSdkHttpClients() {
        AbstractCustomResourceHandler defaultClientHandler = new NullCustomResourceHandler();

        SdkHttpClient defaultClient = defaultClientHandler.buildResponseClient().getClient();
        assertThat(defaultClient).isNotNull();

        String customClientName = "mockCustomClient";
        SdkHttpClient customClientArg = mock(SdkHttpClient.class);
        when(customClientArg.clientName()).thenReturn(customClientName);
        AbstractCustomResourceHandler customClientHandler = new NullCustomResourceHandler(customClientArg);

        SdkHttpClient customClient = customClientHandler.buildResponseClient().getClient();
        assertThat(customClient).isNotNull();
        assertThat(customClient.clientName())
                .isEqualTo(customClientName);

        assertThat(customClient.clientName())
                .isNotEqualTo(defaultClient.clientName());
    }

    @ParameterizedTest
    @CsvSource(value = { "Create,1,0,0", "Update,0,1,0", "Delete,0,0,1" }, delimiter = ',')
    void eventsDelegateToCorrectHandlerMethod(String eventType, int createCount, int updateCount, int deleteCount) {
        AbstractCustomResourceHandler handler = spy(new NoOpCustomResourceHandler());

        Context context = new TestLambdaContext();
        handler.handleRequest(eventOfType(eventType), context);

        verify(handler, times(createCount)).create(any(), eq(context));
        verify(handler, times(updateCount)).update(any(), eq(context));
        verify(handler, times(deleteCount)).delete(any(), eq(context));
    }

    @Test
    void eventOfUnknownRequestTypeSendEmptySuccess() {
        AbstractCustomResourceHandler handler = spy(new NoOpCustomResourceHandler());

        Context context = new TestLambdaContext();
        CloudFormationCustomResourceEvent event = eventOfType("UNKNOWN");

        handler.handleRequest(event, context);

        verify(handler, times(0)).create(any(), any());
        verify(handler, times(0)).update(any(), any());
        verify(handler, times(0)).delete(any(), any());
    }

    @Test
    void defaultStatusResponseSendsSuccess() {
        SuccessResponseHandler handler = spy(new SuccessResponseHandler());

        Context context = new TestLambdaContext();
        CloudFormationCustomResourceEvent event = eventOfType("Create");

        Response response = handler.handleRequest(event, context);
        assertThat(response).isNotNull();
        assertThat(response.getJsonNode().textValue()).isEqualTo("whatever");
        verify(handler, times(0)).onSendFailure(any(), any(), any(), any());
    }

    @Test
    void explicitResponseWithStatusSuccessSendsSuccess() {
        ExplicitSuccessResponseHandler handler = spy(new ExplicitSuccessResponseHandler());

        Context context = new TestLambdaContext();
        CloudFormationCustomResourceEvent event = eventOfType("Create");

        Response response = handler.handleRequest(event, context);
        assertThat(response).isNotNull();
        assertThat(response.getJsonNode().textValue()).isEqualTo("whatever");
        verify(handler, times(0)).onSendFailure(any(), any(), any(), any());
    }

    @Test
    void explicitResponseWithStatusFailedSendsFailure() {
        FailedResponseHandler handler = spy(new FailedResponseHandler());

        Context context = new TestLambdaContext();
        CloudFormationCustomResourceEvent event = eventOfType("Create");

        Response response = handler.handleRequest(event, context);
        assertThat(response).isNotNull();
        assertThat(response.getJsonNode().textValue()).isEqualTo("whatever");
        verify(handler, times(0)).onSendFailure(any(), any(), any(), any());
    }

    @Test
    void exceptionWhenGeneratingResponseSendsFailure() {
        ExceptionThrowingHandler handler = spy(new ExceptionThrowingHandler());

        Context context = new TestLambdaContext();
        CloudFormationCustomResourceEvent event = eventOfType("Create");

        Response response = handler.handleRequest(event, context);
        assertThat(response)
                .withFailMessage("The response failed to build, so it must be null.")
                .isNull();
        verify(handler, times(0)).onSendFailure(any(), any(), any(), any());
    }

    @Test
    void exceptionWhenSendingResponseInvokesOnSendFailure() {
        // a custom handler that builds response successfully but fails to send it
        SuccessfulSendHandler handler = spy(new SuccessfulSendHandler());

        Context context = new TestLambdaContext();
        CloudFormationCustomResourceEvent event = eventOfType("Create");

        Response response = handler.handleRequest(event, context);
        assertThat(response).isNotNull();
        assertThat(response.getJsonNode().textValue()).isEqualTo("Failure happens on send");
        verify(handler, times(1))
                .onSendFailure(eq(event), eq(context), eq(response), any(IOException.class));
    }

    @Test
    void bothResponseGenerationAndSendFail() {
        // a custom handler that fails to build response _and_ fails to send a FAILED
        // response
        FailedSendHandler handler = spy(new FailedSendHandler());

        Context context = new TestLambdaContext();
        CloudFormationCustomResourceEvent event = eventOfType("Create");

        Response response = handler.handleRequest(event, context);
        assertThat(response).isNull();
        verify(handler, times(1))
                .onSendFailure(eq(event), eq(context), isNull(), any(IOException.class));
    }

}

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
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.events.CloudFormationCustomResourceEvent;
import java.io.IOException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import software.amazon.awssdk.http.SdkHttpClient;
import software.amazon.lambda.powertools.cloudformation.Response.Status;

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
    @CsvSource(value = {"Create,1,0,0", "Update,0,1,0", "Delete,0,0,1"}, delimiter = ',')
    void eventsDelegateToCorrectHandlerMethod(String eventType, int createCount, int updateCount, int deleteCount) {
        AbstractCustomResourceHandler handler = spy(new NoOpCustomResourceHandler());

        Context context = mock(Context.class);
        handler.handleRequest(eventOfType(eventType), context);

        verify(handler, times(createCount)).create(any(), eq(context));
        verify(handler, times(updateCount)).update(any(), eq(context));
        verify(handler, times(deleteCount)).delete(any(), eq(context));
    }

    @Test
    void eventOfUnknownRequestTypeSendEmptySuccess() {
        AbstractCustomResourceHandler handler = spy(new NoOpCustomResourceHandler());

        Context context = mock(Context.class);
        CloudFormationCustomResourceEvent event = eventOfType("UNKNOWN");

        handler.handleRequest(event, context);

        verify(handler, times(0)).create(any(), any());
        verify(handler, times(0)).update(any(), any());
        verify(handler, times(0)).delete(any(), any());
    }

    @Test
    void defaultStatusResponseSendsSuccess() {
        ExpectedStatusResourceHandler handler = spy(new ExpectedStatusResourceHandler(Status.SUCCESS) {
            @Override
            protected Response create(CloudFormationCustomResourceEvent event, Context context) {
                return Response.builder()
                        .value("whatever")
                        .build();
            }
        });

        Context context = mock(Context.class);
        CloudFormationCustomResourceEvent event = eventOfType("Create");

        Response response = handler.handleRequest(event, context);
        assertThat(response).isNotNull();
        assertThat(response.getJsonNode().textValue()).isEqualTo("whatever");
        verify(handler, times(0)).onSendFailure(any(), any(), any(), any());
    }

    @Test
    void explicitResponseWithStatusSuccessSendsSuccess() {
        ExpectedStatusResourceHandler handler = spy(new ExpectedStatusResourceHandler(Status.SUCCESS) {
            @Override
            protected Response create(CloudFormationCustomResourceEvent event, Context context) {
                return Response.builder()
                        .value("whatever")
                        .status(Status.SUCCESS)
                        .build();
            }
        });

        Context context = mock(Context.class);
        CloudFormationCustomResourceEvent event = eventOfType("Create");

        Response response = handler.handleRequest(event, context);
        assertThat(response).isNotNull();
        assertThat(response.getJsonNode().textValue()).isEqualTo("whatever");
        verify(handler, times(0)).onSendFailure(any(), any(), any(), any());
    }

    @Test
    void explicitResponseWithStatusFailedSendsFailure() {
        ExpectedStatusResourceHandler handler = spy(new ExpectedStatusResourceHandler(Status.FAILED) {
            @Override
            protected Response create(CloudFormationCustomResourceEvent event, Context context) {
                return Response.builder()
                        .value("whatever")
                        .status(Status.FAILED)
                        .build();
            }
        });

        Context context = mock(Context.class);
        CloudFormationCustomResourceEvent event = eventOfType("Create");

        Response response = handler.handleRequest(event, context);
        assertThat(response).isNotNull();
        assertThat(response.getJsonNode().textValue()).isEqualTo("whatever");
        verify(handler, times(0)).onSendFailure(any(), any(), any(), any());
    }

    @Test
    void exceptionWhenGeneratingResponseSendsFailure() {
        ExpectedStatusResourceHandler handler = spy(new ExpectedStatusResourceHandler(Status.FAILED) {
            @Override
            protected Response create(CloudFormationCustomResourceEvent event, Context context) {
                throw new RuntimeException("This exception is intentional for testing");
            }
        });

        Context context = mock(Context.class);
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
        FailToSendResponseHandler handler = spy(new FailToSendResponseHandler() {
            @Override
            protected Response create(CloudFormationCustomResourceEvent event, Context context) {
                return Response.builder().value("Failure happens on send").build();
            }
        });

        Context context = mock(Context.class);
        CloudFormationCustomResourceEvent event = eventOfType("Create");

        Response response = handler.handleRequest(event, context);
        assertThat(response).isNotNull();
        assertThat(response.getJsonNode().textValue()).isEqualTo("Failure happens on send");
        verify(handler, times(1))
                .onSendFailure(eq(event), eq(context), eq(response), any(IOException.class));
    }

    @Test
    void bothResponseGenerationAndSendFail() {
        // a custom handler that fails to build response _and_ fails to send a FAILED response
        FailToSendResponseHandler handler = spy(new FailToSendResponseHandler() {
            @Override
            protected Response create(CloudFormationCustomResourceEvent event, Context context) {
                throw new RuntimeException("This exception is intentional for testing");
            }
        });

        Context context = mock(Context.class);
        CloudFormationCustomResourceEvent event = eventOfType("Create");

        Response response = handler.handleRequest(event, context);
        assertThat(response).isNull();
        verify(handler, times(1))
                .onSendFailure(eq(event), eq(context), isNull(), any(IOException.class));
    }

    /**
     * Bare-bones implementation that returns null for abstract methods.
     */
    static class NullCustomResourceHandler extends AbstractCustomResourceHandler {
        NullCustomResourceHandler() {
        }

        NullCustomResourceHandler(SdkHttpClient client) {
            super(client);
        }

        @Override
        protected Response create(CloudFormationCustomResourceEvent event, Context context) {
            return null;
        }

        @Override
        protected Response update(CloudFormationCustomResourceEvent event, Context context) {
            return null;
        }

        @Override
        protected Response delete(CloudFormationCustomResourceEvent event, Context context) {
            return null;
        }
    }

    /**
     * Uses a mocked CloudFormationResponse to avoid sending actual HTTP requests.
     */
    static class NoOpCustomResourceHandler extends NullCustomResourceHandler {

        NoOpCustomResourceHandler() {
            super(mock(SdkHttpClient.class));
        }

        @Override
        protected CloudFormationResponse buildResponseClient() {
            return mock(CloudFormationResponse.class);
        }
    }

    /**
     * Creates a handler that will expect the Response to be sent with an expected status. Will throw an AssertionError
     * if the method is sent with an unexpected status.
     */
    static class ExpectedStatusResourceHandler extends NoOpCustomResourceHandler {
        private final Status expectedStatus;

        ExpectedStatusResourceHandler(Status expectedStatus) {
            this.expectedStatus = expectedStatus;
        }

        @Override
        protected CloudFormationResponse buildResponseClient() {
            // create a CloudFormationResponse that fails if invoked with unexpected status
            CloudFormationResponse cfnResponse = mock(CloudFormationResponse.class);
            try {
                when(cfnResponse.send(any(), any(), argThat(resp -> resp.getStatus() != expectedStatus)))
                        .thenThrow(new AssertionError("Expected response's status to be " + expectedStatus));
            } catch (IOException | CustomResourceResponseException e) {
                // this should never happen
                throw new RuntimeException("Unexpected mocking exception", e);
            }
            return cfnResponse;
        }
    }

    /**
     * Always fails to send the response
     */
    static class FailToSendResponseHandler extends NoOpCustomResourceHandler {
        @Override
        protected CloudFormationResponse buildResponseClient() {
            CloudFormationResponse cfnResponse = mock(CloudFormationResponse.class);
            try {
                when(cfnResponse.send(any(), any()))
                        .thenThrow(new IOException("Intentional send failure"));
                when(cfnResponse.send(any(), any(), any()))
                        .thenThrow(new IOException("Intentional send failure"));
            } catch (IOException | CustomResourceResponseException e) {
                // this should never happen
                throw new RuntimeException("Unexpected mocking exception", e);
            }
            return cfnResponse;
        }
    }
}

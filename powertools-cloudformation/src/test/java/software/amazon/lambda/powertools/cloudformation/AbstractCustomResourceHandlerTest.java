package software.amazon.lambda.powertools.cloudformation;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.events.CloudFormationCustomResourceEvent;
import org.junit.jupiter.api.Test;
import software.amazon.awssdk.http.SdkHttpClient;
import software.amazon.lambda.powertools.cloudformation.CloudFormationResponse.ResponseStatus;

import java.io.IOException;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class AbstractCustomResourceHandlerTest {

    /**
     * Uses a mocked CloudFormationResponse that will not send HTTP requests.
     */
    static abstract class NoOpCustomResourceHandler extends AbstractCustomResourceHandler {

        NoOpCustomResourceHandler() {
            super(mock(SdkHttpClient.class));
        }

        @Override
        protected CloudFormationResponse buildResponseClient() {
            return mock(CloudFormationResponse.class);
        }
    }

    /**
     * Counts invocations of different methods (and asserts expectations about the invoked methods' arguments)
     */
    static class InvocationCountingResourceHandler extends NoOpCustomResourceHandler {
        private final AtomicInteger createInvocations = new AtomicInteger(0);
        private final AtomicInteger updateInvocations = new AtomicInteger(0);
        private final AtomicInteger deleteInvocations = new AtomicInteger(0);
        private final AtomicInteger onSendFailureInvocations = new AtomicInteger(0);

        private Response delegate(CloudFormationCustomResourceEvent event,
                                  String expectedEventType,
                                  AtomicInteger invocationsCount) {
            assertThat(event.getRequestType()).isEqualTo(expectedEventType);
            invocationsCount.incrementAndGet();
            return null;
        }

        int getCreateInvocationCount() {
            return createInvocations.get();
        }

        int getUpdateInvocationCount() {
            return updateInvocations.get();
        }

        int getDeleteInvocationCount() {
            return deleteInvocations.get();
        }

        int getOnSendFailureInvocationCount() {
            return onSendFailureInvocations.get();
        }

        @Override
        protected Response create(CloudFormationCustomResourceEvent event, Context context) {
            return delegate(event, "Create", createInvocations);
        }

        @Override
        protected Response update(CloudFormationCustomResourceEvent event, Context context) {
            return delegate(event, "Update", updateInvocations);
        }

        @Override
        protected Response delete(CloudFormationCustomResourceEvent event, Context context) {
            return delegate(event, "Delete", deleteInvocations);
        }

        @Override
        protected void onSendFailure(CloudFormationCustomResourceEvent event,
                                     Context context,
                                     Response response,
                                     Exception exception) {
            assertThat(exception).isNotNull();
            onSendFailureInvocations.incrementAndGet();
        }
    }

    /**
     * Creates a handler that will expect the Response to be sent with an expected status. Will throw an AssertionError
     * if the method is sent with an unexpected status.
     */
    static class ExpectedStatusResourceHandler extends InvocationCountingResourceHandler {
        private final ResponseStatus expectedStatus;

        ExpectedStatusResourceHandler(ResponseStatus expectedStatus) {
            this.expectedStatus = expectedStatus;
        }

        @Override
        protected CloudFormationResponse buildResponseClient() {
            // create a CloudFormationResponse that fails if invoked with unexpected status
            CloudFormationResponse cfnResponse = mock(CloudFormationResponse.class);
            try {
                when(cfnResponse.send(any(), any(), argThat(status -> status != expectedStatus), any()))
                        .thenThrow(new AssertionError("Expected response status to be " + expectedStatus));
            } catch (IOException | ResponseException e) {
                // this should never happen
                throw new RuntimeException("Unexpected mocking exception", e);
            }
            return cfnResponse;
        }
    }

    /**
     * Always fails to send the response
     */
    static class FailToSendResponseHandler extends InvocationCountingResourceHandler {
        @Override
        protected CloudFormationResponse buildResponseClient() {
            // create a CloudFormationResponse that fails if invoked with unexpected status
            CloudFormationResponse cfnResponse = mock(CloudFormationResponse.class);
            try {
                when(cfnResponse.send(any(), any(), any()))
                        .thenThrow(new IOException("Intentional send failure"));
                when(cfnResponse.send(any(), any(), any(), any()))
                        .thenThrow(new IOException("Intentional send failure"));
            } catch (IOException | ResponseException e) {
                // this should never happen
                throw new RuntimeException("Unexpected mocking exception", e);
            }
            return cfnResponse;
        }
    }

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
    void eventsOfKnownRequestTypesDelegateProperly() {
        InvocationCountingResourceHandler handler = new InvocationCountingResourceHandler();

        // invoke handleRequest for different event requestTypes
        Context context = mock(Context.class);
        Stream.of("Create", "Create", "Create", "Update", "Update", "Delete")
                .map(AbstractCustomResourceHandlerTest::eventOfType)
                .forEach(event -> handler.handleRequest(event, context));

        assertThat(handler.getCreateInvocationCount()).isEqualTo(3);
        assertThat(handler.getUpdateInvocationCount()).isEqualTo(2);
        assertThat(handler.getDeleteInvocationCount()).isEqualTo(1);
    }

    @Test
    void eventOfUnknownRequestTypeSendEmptySuccess() {
        InvocationCountingResourceHandler handler = new InvocationCountingResourceHandler();

        Context context = mock(Context.class);
        CloudFormationCustomResourceEvent event = eventOfType("UNKNOWN");

        handler.handleRequest(event, context);

        assertThat(handler.getCreateInvocationCount()).isEqualTo(0);
        assertThat(handler.getUpdateInvocationCount()).isEqualTo(0);
        assertThat(handler.getDeleteInvocationCount()).isEqualTo(0);
    }

    @Test
    void nullResponseSendsSuccess() {
        ExpectedStatusResourceHandler handler = new ExpectedStatusResourceHandler(ResponseStatus.SUCCESS);

        Context context = mock(Context.class);
        CloudFormationCustomResourceEvent event = eventOfType("Create"); // could be any valid requestType

        Response response = handler.handleRequest(event, context);
        assertThat(response).isNull();
        assertThat(handler.getOnSendFailureInvocationCount()).isEqualTo(0);
    }

    @Test
    void nonNullResponseSendsSuccess() {
        ExpectedStatusResourceHandler handler = new ExpectedStatusResourceHandler(ResponseStatus.SUCCESS) {
            @Override
            protected Response create(CloudFormationCustomResourceEvent event, Context context) {
                return Response.builder().value("whatever").build();
            }
        };

        Context context = mock(Context.class);
        CloudFormationCustomResourceEvent event = eventOfType("Create");

        Response response = handler.handleRequest(event, context);
        assertThat(response).isNotNull();
        assertThat(response.getJsonNode().textValue()).isEqualTo("whatever");
        assertThat(handler.getOnSendFailureInvocationCount()).isEqualTo(0);
    }

    @Test
    void exceptionWhenGeneratingResponseSendsFailure() {
        ExpectedStatusResourceHandler handler = new ExpectedStatusResourceHandler(ResponseStatus.FAILED) {
            @Override
            protected Response create(CloudFormationCustomResourceEvent event, Context context) {
                throw new RuntimeException("This exception is intentional for testing");
            }
        };

        Context context = mock(Context.class);
        CloudFormationCustomResourceEvent event = eventOfType("Create");

        Response response = handler.handleRequest(event, context);
        assertThat(response)
                .withFailMessage("The response failed to build, so it must be null.")
                .isNull();
        assertThat(handler.getOnSendFailureInvocationCount())
                .withFailMessage("A failure to build a Response is not a failure to send it.")
                .isEqualTo(0);
    }

    @Test
    void exceptionWhenSendingResponseInvokesOnSendFailure() {
        // a custom handler that builds response successfully but fails to send it
        FailToSendResponseHandler handler = new FailToSendResponseHandler() {
            @Override
            protected Response create(CloudFormationCustomResourceEvent event, Context context) {
                return Response.builder().value("Failure happens on send").build();
            }
        };

        Context context = mock(Context.class);
        CloudFormationCustomResourceEvent event = eventOfType("Create");

        Response response = handler.handleRequest(event, context);
        assertThat(response).isNotNull();
        assertThat(response.getJsonNode().textValue()).isEqualTo("Failure happens on send");
        assertThat(handler.getOnSendFailureInvocationCount()).isEqualTo(1);
    }

    @Test
    void bothResponseGenerationAndSendFail() {
        // a custom handler that fails to build response _and_ fails to send a FAILED response
        FailToSendResponseHandler handler = new FailToSendResponseHandler() {
            @Override
            protected Response create(CloudFormationCustomResourceEvent event, Context context) {
                throw new RuntimeException("This exception is intentional for testing");
            }
        };

        Context context = mock(Context.class);
        CloudFormationCustomResourceEvent event = eventOfType("Create");

        Response response = handler.handleRequest(event, context);
        assertThat(response).isNull();
        assertThat(handler.getOnSendFailureInvocationCount()).isEqualTo(1);
    }
}

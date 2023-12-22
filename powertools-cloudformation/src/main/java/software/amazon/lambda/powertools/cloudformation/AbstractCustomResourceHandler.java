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

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestHandler;
import com.amazonaws.services.lambda.runtime.events.CloudFormationCustomResourceEvent;
import java.io.IOException;
import java.util.Objects;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import software.amazon.awssdk.http.SdkHttpClient;
import software.amazon.awssdk.http.crt.AwsCrtHttpClient;

/**
 * Handler base class providing core functionality for sending responses to custom CloudFormation resources after
 * receiving some event. Depending on the type of event, this class either invokes the crete, update, or delete method
 * and sends the returned Response object to the custom resource.
 */
public abstract class AbstractCustomResourceHandler
        implements RequestHandler<CloudFormationCustomResourceEvent, Response> {

    private static final Logger LOG = LoggerFactory.getLogger(AbstractCustomResourceHandler.class);

    private final SdkHttpClient client;

    /**
     * Creates a new Handler that uses the default HTTP client for communicating with custom CloudFormation resources.
     */
    protected AbstractCustomResourceHandler() {
        this.client = AwsCrtHttpClient.create();
    }

    /**
     * Creates a new Handler that uses the provided HTTP client for communicating with custom CloudFormation resources.
     *
     * @param client cannot be null
     */
    protected AbstractCustomResourceHandler(SdkHttpClient client) {
        this.client = Objects.requireNonNull(client, "SdkHttpClient cannot be null.");
    }

    /**
     * Generates the appropriate response object based on the event type and sends it as a response to the custom
     * cloud formation resource using the URL provided within the event.
     *
     * @param event   custom resources create/update/delete event
     * @param context lambda execution context
     * @return potentially null response object sent to the custom resource
     */
    @Override
    public final Response handleRequest(CloudFormationCustomResourceEvent event, Context context) {
        String responseUrl = Objects.requireNonNull(event.getResponseUrl(),
                "Event must have a non-null responseUrl to be able to send the response.");

        CloudFormationResponse client = buildResponseClient();

        Response response = null;
        try {
            response = getResponse(event, context);
            LOG.debug("Preparing to send response {} to {}.", response, responseUrl);
            client.send(event, context, response);
        } catch (IOException ioe) {
            LOG.error("Unable to send response {} to {}.", response, responseUrl, ioe);
            onSendFailure(event, context, response, ioe);
        } catch (CustomResourceResponseException rse) {
            LOG.error("Unable to generate response. Sending empty failure to {}", responseUrl, rse);
            try {
                // If the customers code throws an exception, Powertools for AWS Lambda (Java) should respond in a way that doesn't
                // change the CloudFormation resources.
                // In the case of a Update or Delete, a failure is sent with the existing PhysicalResourceId
                // indicating no change.
                // In the case of a Create, null will be set and changed to the Lambda LogStreamName before sending.
                client.send(event, context, Response.failed(event.getPhysicalResourceId()));
            } catch (Exception e) {
                // unable to generate response AND send the failure
                LOG.error("Unable to send failure response to {}.", responseUrl, e);
                onSendFailure(event, context, null, e);
            }
        }
        return response;
    }

    private Response getResponse(CloudFormationCustomResourceEvent event, Context context)
            throws CustomResourceResponseException {
        try {
            switch (event.getRequestType()) {
                case "Create":
                    return create(event, context);
                case "Update":
                    return update(event, context);
                case "Delete":
                    return delete(event, context);
                default:
                    LOG.warn("Unexpected request type \"" + event.getRequestType() + "\" for event " + event);
                    return null;
            }
        } catch (RuntimeException e) {
            throw new CustomResourceResponseException("Unable to get Response", e);
        }
    }

    /**
     * Builds a client for sending responses to the custom resource.
     *
     * @return a client for sending the response
     */
    CloudFormationResponse buildResponseClient() {
        return new CloudFormationResponse(client);
    }

    /**
     * Invoked when there is an error sending a response to the custom cloud formation resource. This method does not
     * get called if there are errors constructing the response itself, which instead is handled by sending an empty
     * FAILED response to the custom resource. This method will be invoked, however, if there is an error while sending
     * the FAILED response.
     * <p>
     * The method itself does nothing but subclasses may override to provide additional logging or handling logic. All
     * arguments provided are for contextual purposes.
     * <p>
     * Exceptions should not be thrown by this method.
     *
     * @param event     the event
     * @param context   execution context
     * @param response  the response object that was attempted to be sent to the custom resource
     * @param exception the exception caught when attempting to call the custom resource URL
     */
    @SuppressWarnings("unused")
    protected void onSendFailure(CloudFormationCustomResourceEvent event,
                                 Context context,
                                 Response response,
                                 Exception exception) {
        // intentionally empty
    }

    /**
     * Returns the response object to send to the custom CloudFormation resource upon its creation. If this method
     * returns null, then the handler will send a successful but empty response to the CloudFormation resource. If this
     * method throws a RuntimeException, the handler will send an empty failed response to the resource.
     *
     * @param event   an event of request type Create
     * @param context execution context
     * @return the response object or null
     */
    protected abstract Response create(CloudFormationCustomResourceEvent event, Context context);

    /**
     * Returns the response object to send to the custom CloudFormation resource upon its modification. If the method
     * returns null, then the handler will send a successful but empty response to the CloudFormation resource. If this
     * method throws a RuntimeException, the handler will send an empty failed response to the resource.
     *
     * @param event   an event of request type Update
     * @param context execution context
     * @return the response object or null
     */
    protected abstract Response update(CloudFormationCustomResourceEvent event, Context context);

    /**
     * Returns the response object to send to the custom CloudFormation resource upon its deletion. If this method
     * returns null, then the handler will send a successful but empty response to the CloudFormation resource. If this
     * method throws a RuntimeException, the handler will send an empty failed response to the resource.
     *
     * @param event   an event of request type Delete
     * @param context execution context
     * @return the response object or null
     */
    protected abstract Response delete(CloudFormationCustomResourceEvent event, Context context);
}

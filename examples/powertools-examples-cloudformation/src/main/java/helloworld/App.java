package helloworld;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.events.CloudFormationCustomResourceEvent;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import software.amazon.awssdk.awscore.exception.AwsServiceException;
import software.amazon.awssdk.core.exception.SdkClientException;
import software.amazon.awssdk.core.waiters.WaiterResponse;
import software.amazon.awssdk.http.apache.ApacheHttpClient;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.*;
import software.amazon.awssdk.services.s3.waiters.S3Waiter;
import software.amazon.lambda.powertools.cloudformation.AbstractCustomResourceHandler;
import software.amazon.lambda.powertools.cloudformation.Response;

import java.util.Objects;

/**
 * Handler for requests to Lambda function.
 */

public class App extends AbstractCustomResourceHandler {
    private final static Logger log = LogManager.getLogger(App.class);
    private final S3Client s3Client;

    public App() {
        super();
        s3Client = S3Client.builder().httpClientBuilder(ApacheHttpClient.builder()).build();
    }

    /**
     * This method is invoked when CloudFormation Creates the Custom Resource.
     * The method creates an Amazon S3 Bucket with the provided `BucketName`
     *
     * @param cloudFormationCustomResourceEvent Create Event from CloudFormation
     * @param context                           Lambda Context
     * @return Response to send to CloudFormation
     */
    @Override
    protected Response create(CloudFormationCustomResourceEvent cloudFormationCustomResourceEvent, Context context) {
        // Validate the CloudFormation Custom Resource event
        Objects.requireNonNull(cloudFormationCustomResourceEvent, "cloudFormationCustomResourceEvent cannot be null.");
        Objects.requireNonNull(cloudFormationCustomResourceEvent.getResourceProperties().get("BucketName"), "BucketName cannot be null.");

        log.info(cloudFormationCustomResourceEvent);
        String bucketName = (String) cloudFormationCustomResourceEvent.getResourceProperties().get("BucketName");
        log.info("Bucket Name {}", bucketName);
        try {
            // Create the S3 bucket with the given bucketName
            createBucket(bucketName);
            // Return a successful response with the bucketName as the physicalResourceId
            return Response.success(bucketName);
        } catch (AwsServiceException | SdkClientException e) {
            // In case of error, return a failed response, with the bucketName as the physicalResourceId
            log.error(e);
            return Response.failed(bucketName);
        }
    }

    /**
     * This method is invoked when CloudFormation Updates the Custom Resource.
     * The method creates an Amazon S3 Bucket with the provided `BucketName`, if the `BucketName` differs from the previous `BucketName`
     *
     * @param cloudFormationCustomResourceEvent Update Event from CloudFormation
     * @param context                           Lambda Context
     * @return Response to send to CloudFormation
     */
    @Override
    protected Response update(CloudFormationCustomResourceEvent cloudFormationCustomResourceEvent, Context context) {
        // Validate the CloudFormation Custom Resource event
        Objects.requireNonNull(cloudFormationCustomResourceEvent, "cloudFormationCustomResourceEvent cannot be null.");
        Objects.requireNonNull(cloudFormationCustomResourceEvent.getResourceProperties().get("BucketName"), "BucketName cannot be null.");

        log.info(cloudFormationCustomResourceEvent);
        // Get the physicalResourceId. physicalResourceId is the value returned to CloudFormation in the Create request, and passed in on subsequent requests (e.g. UPDATE or DELETE)
        String physicalResourceId = cloudFormationCustomResourceEvent.getPhysicalResourceId();
        log.info("Physical Resource ID {}", physicalResourceId);

        // Get the BucketName from the CloudFormation Event
        String newBucketName = (String) cloudFormationCustomResourceEvent.getResourceProperties().get("BucketName");

        // Check if the physicalResourceId equals the new BucketName
        if (!physicalResourceId.equals(newBucketName)) {
            // The bucket name has changed - create a new bucket
            try {
                // Create a new bucket with the newBucketName
                createBucket(newBucketName);
                // Return a successful response with the newBucketName
                return Response.success(newBucketName);
            } catch (AwsServiceException | SdkClientException e) {
                log.error(e);
                return Response.failed(newBucketName);
            }
        } else {
            // Bucket name has not changed, and no changes are needed.
            // Return a successful response with the previous physicalResourceId
            return Response.success(physicalResourceId);
        }
    }

    /**
     * This method is invoked when CloudFormation Deletes the Custom Resource.
     * NOTE: CloudFormation will DELETE a resource, if during the UPDATE a new physicalResourceId is returned.
     * Refer to the <a href="https://docs.powertools.aws.dev/lambda/java/utilities/custom_resources/#understanding-the-cloudformation-custom-resource-lifecycle">Powertools Java Documentation</a> for more details.
     *
     * @param cloudFormationCustomResourceEvent Delete Event from CloudFormation
     * @param context                           Lambda Context
     * @return Response to send to CloudFormation
     */
    @Override
    protected Response delete(CloudFormationCustomResourceEvent cloudFormationCustomResourceEvent, Context context) {
        // Validate the CloudFormation Custom Resource event
        Objects.requireNonNull(cloudFormationCustomResourceEvent, "cloudFormationCustomResourceEvent cannot be null.");
        Objects.requireNonNull(cloudFormationCustomResourceEvent.getPhysicalResourceId(), "PhysicalResourceId cannot be null.");

        log.info(cloudFormationCustomResourceEvent);
        // Get the physicalResourceId. physicalResourceId is the value provided to CloudFormation in the Create request.
        String bucketName = cloudFormationCustomResourceEvent.getPhysicalResourceId();
        log.info("Bucket Name {}", bucketName);

        // Check if a bucket with bucketName exists
        if (bucketExists(bucketName)) {
            try {
                // If it exists, delete the bucket
                s3Client.deleteBucket(DeleteBucketRequest.builder().bucket(bucketName).build());
                log.info("Bucket Deleted {}", bucketName);
                // Return a successful response with bucketName as the physicalResourceId
                return Response.success(bucketName);
            } catch (AwsServiceException | SdkClientException e) {
                // Return a failed response in case of errors during the bucket deletion
                log.error(e);
                return Response.failed(bucketName);
            }
        } else {
            // If the bucket does not exist, return a successful response with the bucketName as the physicalResourceId
            log.info("Bucket already deleted - no action");
            return Response.success(bucketName);
        }

    }

    private boolean bucketExists(String bucketName) {
        try {
            HeadBucketResponse headBucketResponse = s3Client.headBucket(HeadBucketRequest.builder().bucket(bucketName).build());
            if (headBucketResponse.sdkHttpResponse().isSuccessful()) {
                return true;
            }
        } catch (NoSuchBucketException e) {
            log.info("Bucket does not exist");
            return false;
        }
        return false;
    }

    private void createBucket(String bucketName) {
        S3Waiter waiter = s3Client.waiter();
        CreateBucketRequest createBucketRequest = CreateBucketRequest.builder().bucket(bucketName).build();
        s3Client.createBucket(createBucketRequest);
        WaiterResponse<HeadBucketResponse> waiterResponse = waiter.waitUntilBucketExists(HeadBucketRequest.builder().bucket(bucketName).build());
        waiterResponse.matched().response().ifPresent(log::info);
        log.info("Bucket Created {}", bucketName);
    }
}
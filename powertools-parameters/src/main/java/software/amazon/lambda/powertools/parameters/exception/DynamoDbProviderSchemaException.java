package software.amazon.lambda.powertools.parameters.exception;

/**
 * Thrown when the DynamoDbProvider comes across parameter data that
 * does not meet the DynamoDB parameters schema.
 */
public class DynamoDbProviderSchemaException extends RuntimeException {
    public DynamoDbProviderSchemaException(String msg) {
        super(msg);
    }
}

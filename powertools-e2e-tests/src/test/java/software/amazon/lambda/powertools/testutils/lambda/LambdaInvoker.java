package software.amazon.lambda.powertools.testutils.lambda;

import software.amazon.awssdk.core.SdkBytes;
import software.amazon.awssdk.http.SdkHttpClient;
import software.amazon.awssdk.http.urlconnection.UrlConnectionHttpClient;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.lambda.LambdaClient;
import software.amazon.awssdk.services.lambda.model.InvokeRequest;
import software.amazon.awssdk.services.lambda.model.InvokeResponse;
import software.amazon.awssdk.services.lambda.model.LogType;

import java.time.Clock;
import java.time.Instant;

import static java.time.temporal.ChronoUnit.MINUTES;

public class LambdaInvoker {
    private static final SdkHttpClient httpClient = UrlConnectionHttpClient.builder().build();
    private static final Region region = Region.of(System.getProperty("AWS_DEFAULT_REGION", "eu-west-1"));
    private static final LambdaClient lambda = LambdaClient.builder()
            .httpClient(httpClient)
                .region(region)
                .build();

    public static InvocationResult invokeFunction(String functionName, String input) {
        SdkBytes payload = SdkBytes.fromUtf8String(input);

        InvokeRequest request = InvokeRequest.builder()
                .functionName(functionName)
                .payload(payload)
                .logType(LogType.TAIL)
                .build();

        Instant start = Instant.now(Clock.systemUTC()).truncatedTo(MINUTES);
        InvokeResponse response = lambda.invoke(request);
        Instant end = start.plus(1, MINUTES);
        return new InvocationResult(response, start, end);
    }
}

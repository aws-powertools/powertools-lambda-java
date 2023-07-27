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

package software.amazon.lambda.powertools.testutils.lambda;

import static java.time.temporal.ChronoUnit.MINUTES;

import java.time.Clock;
import java.time.Instant;
import software.amazon.awssdk.core.SdkBytes;
import software.amazon.awssdk.http.SdkHttpClient;
import software.amazon.awssdk.http.urlconnection.UrlConnectionHttpClient;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.lambda.LambdaClient;
import software.amazon.awssdk.services.lambda.model.InvokeRequest;
import software.amazon.awssdk.services.lambda.model.InvokeResponse;
import software.amazon.awssdk.services.lambda.model.LogType;

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

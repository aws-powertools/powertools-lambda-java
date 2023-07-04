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
package software.amazon.lambda.powertools.validation.internal;

import com.amazonaws.services.lambda.runtime.events.*;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.ArgumentsProvider;

import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;

public class ResponseEventsArgumentsProvider implements ArgumentsProvider {

    @Override
    public Stream<? extends Arguments> provideArguments(ExtensionContext context) {

        String body = "{id";

        final APIGatewayProxyResponseEvent apiGWProxyResponseEvent = new APIGatewayProxyResponseEvent().withBody(body);

        APIGatewayV2HTTPResponse apiGWV2HTTPResponse = new APIGatewayV2HTTPResponse();
        apiGWV2HTTPResponse.setBody(body);

        APIGatewayV2WebSocketResponse apiGWV2WebSocketResponse = new APIGatewayV2WebSocketResponse();
        apiGWV2WebSocketResponse.setBody(body);

        ApplicationLoadBalancerResponseEvent albResponseEvent = new ApplicationLoadBalancerResponseEvent();
        albResponseEvent.setBody(body);

        KinesisAnalyticsInputPreprocessingResponse kaipResponse = new KinesisAnalyticsInputPreprocessingResponse();
        List records = new ArrayList<KinesisAnalyticsInputPreprocessingResponse.Record>();
        ByteBuffer buffer = ByteBuffer.wrap(body.getBytes(StandardCharsets.UTF_8));
        records.add(new KinesisAnalyticsInputPreprocessingResponse.Record("1", KinesisAnalyticsInputPreprocessingResponse.Result.Ok, buffer));
        kaipResponse.setRecords(records);

        return Stream.of(apiGWProxyResponseEvent, apiGWV2HTTPResponse, apiGWV2WebSocketResponse, albResponseEvent, kaipResponse).map(Arguments::of);
    }
}

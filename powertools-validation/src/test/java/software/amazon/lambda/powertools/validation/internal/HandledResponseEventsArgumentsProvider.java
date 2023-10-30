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

import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

import org.junit.jupiter.api.extension.ExtensionContext;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.ArgumentsProvider;

import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyResponseEvent;
import com.amazonaws.services.lambda.runtime.events.APIGatewayV2HTTPResponse;
import com.amazonaws.services.lambda.runtime.events.APIGatewayV2WebSocketResponse;
import com.amazonaws.services.lambda.runtime.events.ApplicationLoadBalancerResponseEvent;

public class HandledResponseEventsArgumentsProvider implements ArgumentsProvider {
    
    @Override
    public Stream<? extends Arguments> provideArguments(ExtensionContext context) {

        String body = "{id";

        final APIGatewayProxyResponseEvent apiGWProxyResponseEvent = new APIGatewayProxyResponseEvent()
          .withBody(body)
          .withHeaders(Map.of("header1", "value1,value2,value3"))
          .withMultiValueHeaders(Map.of("header1", List.of("value1", "value2", "value3")));

        APIGatewayV2HTTPResponse apiGWV2HTTPResponse = new APIGatewayV2HTTPResponse();
        apiGWV2HTTPResponse.setBody(body);
        apiGWV2HTTPResponse.setHeaders(Map.of("header1", "value1,value2,value3"));
        apiGWV2HTTPResponse.setMultiValueHeaders(Map.of("header1", List.of("value1", "value2", "value3")));

        APIGatewayV2WebSocketResponse apiGWV2WebSocketResponse = new APIGatewayV2WebSocketResponse();
        apiGWV2WebSocketResponse.setBody(body);

        ApplicationLoadBalancerResponseEvent albResponseEvent = new ApplicationLoadBalancerResponseEvent();
        albResponseEvent.setBody(body);

        return Stream.of(apiGWProxyResponseEvent, apiGWV2HTTPResponse).map(Arguments::of);
    }
}

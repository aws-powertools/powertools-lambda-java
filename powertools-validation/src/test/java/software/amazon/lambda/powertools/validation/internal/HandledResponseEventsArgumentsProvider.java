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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

import org.junit.jupiter.api.extension.ExtensionContext;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.ArgumentsProvider;

import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyResponseEvent;
import com.amazonaws.services.lambda.runtime.events.APIGatewayV2HTTPResponse;

/**
 * Provides test arguments that are used in unit tests.
 * It creates API Gateway response arguments that can be used to confirm
 * that @Validation validates responses and returns a response's headers even
 * when validation fails
 */
public class HandledResponseEventsArgumentsProvider implements ArgumentsProvider {

  @Override
  public Stream<? extends Arguments> provideArguments(ExtensionContext context) {

    String body = "{id";

    Map<String, String> headers = new HashMap<>();
    headers.put("header1", "value1,value2,value3");
    Map<String, List<String>> headersList = new HashMap<>();
    List<String> headerValues = new ArrayList<>();
    headerValues.add("value1");
    headerValues.add("value2");
    headerValues.add("value3");
    headersList.put("header1", headerValues);
    
    final APIGatewayProxyResponseEvent apiGWProxyResponseEvent = new APIGatewayProxyResponseEvent()
        .withBody(body)
        .withHeaders(headers)
        .withMultiValueHeaders(headersList);

    APIGatewayV2HTTPResponse apiGWV2HTTPResponse = new APIGatewayV2HTTPResponse();
    apiGWV2HTTPResponse.setBody(body);
    apiGWV2HTTPResponse.setHeaders(headers);
    apiGWV2HTTPResponse.setMultiValueHeaders(headersList);

    return Stream.of(apiGWProxyResponseEvent, apiGWV2HTTPResponse).map(Arguments::of);
  }
}

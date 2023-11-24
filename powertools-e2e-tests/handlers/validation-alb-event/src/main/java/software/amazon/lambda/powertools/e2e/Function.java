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

package software.amazon.lambda.powertools.e2e;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestHandler;
import com.amazonaws.services.lambda.runtime.events.ApplicationLoadBalancerRequestEvent;
import com.amazonaws.services.lambda.runtime.events.ApplicationLoadBalancerResponseEvent;
import com.amazonaws.services.lambda.runtime.events.SQSBatchResponse;

import software.amazon.lambda.powertools.validation.Validation;

public class Function
    implements RequestHandler<ApplicationLoadBalancerRequestEvent, ApplicationLoadBalancerResponseEvent> {
  @Validation(inboundSchema = "classpath:/validation/inbound_schema.json", outboundSchema = "classpath:/validation/outbound_schema.json")
  public ApplicationLoadBalancerResponseEvent handleRequest(ApplicationLoadBalancerRequestEvent input,
      Context context) {
    ApplicationLoadBalancerResponseEvent response = new ApplicationLoadBalancerResponseEvent();
    response.setBody(input.getBody());
    response.setStatusCode(200);
    response.setIsBase64Encoded(false);
    return response;
  }
}
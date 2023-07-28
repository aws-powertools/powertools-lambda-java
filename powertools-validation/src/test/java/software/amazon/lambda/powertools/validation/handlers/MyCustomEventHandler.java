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

package software.amazon.lambda.powertools.validation.handlers;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestHandler;
import software.amazon.lambda.powertools.validation.Validation;
import software.amazon.lambda.powertools.validation.model.MyCustomEvent;

public class MyCustomEventHandler implements RequestHandler<MyCustomEvent, String> {

    @Override
    @Validation(inboundSchema = "classpath:/schema_v7.json", envelope = "basket.products[*]")
    public String handleRequest(MyCustomEvent input, Context context) {
        return "OK";
    }
}

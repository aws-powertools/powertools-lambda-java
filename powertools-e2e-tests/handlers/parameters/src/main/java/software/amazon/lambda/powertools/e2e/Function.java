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
import software.amazon.lambda.powertools.logging.Logging;
import software.amazon.lambda.powertools.parameters.appconfig.AppConfigProvider;

public class Function implements RequestHandler<Input, String> {

    @Logging
    public String handleRequest(Input input, Context context) {
        AppConfigProvider provider = AppConfigProvider.builder()
                .withApplication(input.getApp())
                .withEnvironment(input.getEnvironment())
                .build();

        //(input.getEnvironment(), input.getApp());
        return provider.get(input.getKey());

    }
}
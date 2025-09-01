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

package software.amazon.lambda.powertools.cloudformation;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.events.CloudFormationCustomResourceEvent;

import software.amazon.awssdk.http.SdkHttpClient;

/**
 * Bare-bones implementation that returns null for abstract methods.
 */
public class NullCustomResourceHandler extends AbstractCustomResourceHandler {
    public NullCustomResourceHandler() {
    }

    public NullCustomResourceHandler(SdkHttpClient client) {
        super(client);
    }

    @Override
    protected Response create(CloudFormationCustomResourceEvent event, Context context) {
        return null;
    }

    @Override
    protected Response update(CloudFormationCustomResourceEvent event, Context context) {
        return null;
    }

    @Override
    protected Response delete(CloudFormationCustomResourceEvent event, Context context) {
        return null;
    }
}

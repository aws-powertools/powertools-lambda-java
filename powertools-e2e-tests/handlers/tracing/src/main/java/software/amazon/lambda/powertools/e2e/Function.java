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
import software.amazon.lambda.powertools.tracing.Tracing;
import software.amazon.lambda.powertools.tracing.TracingUtils;

public class Function implements RequestHandler<Input, String> {

    @Tracing
    public String handleRequest(Input input, Context context) {
        try {
            Thread.sleep(100); // simulate stuff
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
        String message = buildMessage(input.getMessage(), context.getFunctionName());

        TracingUtils.withSubsegment("internal_stuff", subsegment ->
        {
            try {
                Thread.sleep(100); // simulate stuff
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
        });

        return message;
    }

    @Tracing
    private String buildMessage(String message, String funcName) {
        TracingUtils.putAnnotation("message", message);
        try {
            Thread.sleep(150); // simulate other stuff
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
        return String.format("%s (%s)", message, funcName);
    }
}
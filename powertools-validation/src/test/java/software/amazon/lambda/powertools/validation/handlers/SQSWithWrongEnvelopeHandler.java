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
import com.amazonaws.services.lambda.runtime.events.SQSEvent;
import software.amazon.lambda.powertools.validation.Validation;

public class SQSWithWrongEnvelopeHandler implements RequestHandler<SQSEvent, String> {

    @Override
    // real event contains Records with big R (https://docs.aws.amazon.com/lambda/latest/dg/with-sqs.html)
    @Validation(inboundSchema = "classpath:/schema_v7.json", envelope = "records[*].powertools_json(body).powertools_json(Message)")
    public String handleRequest(SQSEvent input, Context context) {
        return "OK";
    }
}

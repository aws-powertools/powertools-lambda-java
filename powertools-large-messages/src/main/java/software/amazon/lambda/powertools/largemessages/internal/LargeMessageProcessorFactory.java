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

package software.amazon.lambda.powertools.largemessages.internal;

import java.util.Optional;

import com.amazonaws.services.lambda.runtime.events.SNSEvent.SNSRecord;
import com.amazonaws.services.lambda.runtime.events.SQSEvent.SQSMessage;

public final class LargeMessageProcessorFactory {

    private LargeMessageProcessorFactory() {
        // Utility class
    }

    public static Optional<LargeMessageProcessor<?>> get(Object message) {
        if (message instanceof SQSMessage) {
            return Optional.of(new LargeSQSMessageProcessor());
        } else if (message instanceof SNSRecord) {
            return Optional.of(new LargeSNSMessageProcessor());
        } else {
            return Optional.empty();
        }
    }
}

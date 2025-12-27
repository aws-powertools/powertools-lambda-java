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

package helloworld;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestHandler;
import com.amazonaws.services.lambda.runtime.events.SQSEvent;
import com.amazonaws.services.lambda.runtime.events.SQSEvent.SQSMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import software.amazon.lambda.powertools.largemessages.LargeMessages;
import software.amazon.lambda.powertools.logging.Logging;

/**
 * Example handler showing how to use LargeMessageProcessor functionally.
 * This approach gives you more control than the @LargeMessage annotation.
 */
public final class App implements RequestHandler<SQSEvent, String> {

    private static final Logger LOG = LoggerFactory.getLogger(App.class);

    @Logging
    @Override
    public String handleRequest(final SQSEvent event, final Context context) {
        LOG.info("Received event with {} records", event.getRecords().size());

        for (SQSMessage message : event.getRecords()) {
            LargeMessages.processLargeMessage(message, (processedMessage) -> {
                LOG.info("Processing message ID: {}", processedMessage.getMessageId());
                LOG.info("Processing body content: {}", processedMessage.getBody());
                return "Processed";
            });
        }

        return "SUCCESS";
    }
}

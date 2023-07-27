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

import com.amazonaws.services.lambda.runtime.events.SNSEvent.MessageAttribute;
import com.amazonaws.services.lambda.runtime.events.SNSEvent.SNSRecord;
import java.util.HashMap;
import java.util.Map;

class LargeSNSMessageProcessor extends LargeMessageProcessor<SNSRecord> {

    @Override
    protected String getMessageId(SNSRecord message) {
        return message.getSNS().getMessageId();
    }

    @Override
    protected String getMessageContent(SNSRecord message) {
        return message.getSNS().getMessage();
    }

    @Override
    protected void updateMessageContent(SNSRecord message, String messageContent) {
        message.getSNS().setMessage(messageContent);
    }

    @Override
    protected boolean isLargeMessage(SNSRecord message) {
        Map<String, MessageAttribute> msgAttributes = message.getSNS().getMessageAttributes();
        return msgAttributes != null && msgAttributes.containsKey(RESERVED_ATTRIBUTE_NAME);
    }

    @Override
    protected void removeLargeMessageAttributes(SNSRecord message) {
        // message.getSNS().getMessageAttributes() does not support remove operation, copy to new map
        Map<String, MessageAttribute> newAttributes = new HashMap<>(message.getSNS().getMessageAttributes());
        newAttributes.remove(RESERVED_ATTRIBUTE_NAME);
        message.getSNS().setMessageAttributes(newAttributes);
    }
}

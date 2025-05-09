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

package software.amazon.lambda.powertools.logging.internal.handler;

import static software.amazon.lambda.powertools.logging.internal.PowertoolsLoggedFields.CORRELATION_ID;

import java.util.HashMap;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestHandler;
import com.amazonaws.services.lambda.runtime.events.SQSEvent;
import com.fasterxml.jackson.core.JsonProcessingException;

import software.amazon.lambda.powertools.logging.Logging;
import software.amazon.lambda.powertools.logging.argument.StructuredArguments;
import software.amazon.lambda.powertools.logging.internal.PowertoolsLoggedFields;
import software.amazon.lambda.powertools.utilities.JsonConfig;

public class PowertoolsArguments implements RequestHandler<SQSEvent.SQSMessage, String> {
    private final Logger LOG = LoggerFactory.getLogger(PowertoolsArguments.class);
    private final ArgumentFormat argumentFormat;

    public PowertoolsArguments(ArgumentFormat argumentFormat) {
        this.argumentFormat = argumentFormat;
    }

    @Override
    @Logging(clearState = true)
    public String handleRequest(SQSEvent.SQSMessage input, Context context) {
        try {
            MDC.put(CORRELATION_ID.getName(), input.getMessageId());
            if (argumentFormat == ArgumentFormat.JSON) {
                LOG.debug("SQS Event",
                        StructuredArguments.json("input",
                                JsonConfig.get().getObjectMapper().writeValueAsString(input)),
                        // function_name is a reserved key by PowertoolsLoggedFields and should be omitted
                        StructuredArguments.entry("function_name", "shouldBeIgnored"));
            } else {
                LOG.debug("SQS Event",
                        StructuredArguments.entry("input", input),
                        // function_name is a reserved key by PowertoolsLoggedFields and should be omitted
                        StructuredArguments.entry("function_name", "shouldBeIgnored"));
            }

            // Attempt logging all reserved keys, the values should not be overwritten by "shouldBeIgnored"
            final Map<String, String> reservedKeysMap = new HashMap<>();
            for (String field : PowertoolsLoggedFields.stringValues()) {
                reservedKeysMap.put(field, "shouldBeIgnored");
            }
            reservedKeysMap.put("message", "shouldBeIgnored");
            reservedKeysMap.put("level", "shouldBeIgnored");
            reservedKeysMap.put("timestamp", "shouldBeIgnored");
            LOG.debug("Reserved keys", StructuredArguments.entries(reservedKeysMap));
            LOG.debug("{}", input.getMessageId());
            LOG.warn("Message body = {} and id = \"{}\"", input.getBody(), input.getMessageId());
        } catch (JsonProcessingException e) {
            throw new RuntimeException(e);
        }
        return input.getMessageId();
    }

    public enum ArgumentFormat {
        JSON, ENTRY
    }
}

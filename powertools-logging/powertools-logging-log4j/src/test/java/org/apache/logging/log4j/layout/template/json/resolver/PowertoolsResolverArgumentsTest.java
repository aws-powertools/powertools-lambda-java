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

package org.apache.logging.log4j.layout.template.json.resolver;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.contentOf;
import static org.mockito.Mockito.when;
import static org.mockito.MockitoAnnotations.openMocks;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.events.SQSEvent;
import java.io.File;
import java.io.IOException;
import java.nio.channels.FileChannel;
import java.nio.file.NoSuchFileException;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.Arrays;
import java.util.Collections;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.slf4j.MDC;

import software.amazon.lambda.powertools.logging.internal.PowertoolsLoggedFields;
import software.amazon.lambda.powertools.logging.internal.handler.PowertoolsArguments;

@Order(2)
class PowertoolsResolverArgumentsTest {

    @Mock
    private Context context;

    @BeforeEach
    void setUp() throws IOException {
        openMocks(this);
        MDC.clear();
        setupContext();

        try {
            FileChannel.open(Paths.get("target/logfile.json"), StandardOpenOption.WRITE).truncate(0).close();
            FileChannel.open(Paths.get("target/ecslogfile.json"), StandardOpenOption.WRITE).truncate(0).close();
        } catch (NoSuchFileException e) {
            // may not be there in the first run
        }
    }

    @AfterEach
    void cleanUp() throws IOException {
        //Make sure file is cleaned up before running full stack logging regression
        FileChannel.open(Paths.get("target/logfile.json"), StandardOpenOption.WRITE).truncate(0).close();
        FileChannel.open(Paths.get("target/ecslogfile.json"), StandardOpenOption.WRITE).truncate(0).close();
    }

    @Test
    void shouldLogArgumentsAsJsonWhenUsingRawJson() {
        // GIVEN
        PowertoolsArguments requestHandler = new PowertoolsArguments(PowertoolsArguments.ArgumentFormat.JSON);
        SQSEvent.SQSMessage msg = new SQSEvent.SQSMessage();
        msg.setMessageId("1212abcd");
        msg.setBody("plop");
        msg.setEventSource("eb");
        msg.setAwsRegion("eu-west-1");
        SQSEvent.MessageAttribute attribute = new SQSEvent.MessageAttribute();
        attribute.setStringListValues(Arrays.asList("val1", "val2", "val3"));
        msg.setMessageAttributes(Collections.singletonMap("keyAttribute", attribute));

        // WHEN
        requestHandler.handleRequest(msg, context);

        // THEN
        File logFile = new File("target/logfile.json");
        assertThat(contentOf(logFile))
                .contains(
                        "\"input\":{\"awsRegion\":\"eu-west-1\",\"body\":\"plop\",\"eventSource\":\"eb\",\"messageAttributes\":{\"keyAttribute\":{\"stringListValues\":[\"val1\",\"val2\",\"val3\"]}},\"messageId\":\"1212abcd\"}")
                .contains("\"message\":\"1212abcd\"")
                .contains("\"message\":\"Message body = plop and id = \\\"1212abcd\\\"\"");
        // Reserved keys should be ignored
        PowertoolsLoggedFields.stringValues().stream().forEach(reservedKey -> {
            assertThat(contentOf(logFile)).doesNotContain("\"" + reservedKey + "\":\"shouldBeIgnored\"");
            assertThat(contentOf(logFile)).contains(
                    "\"message\":\"Attempted to use reserved key '" + reservedKey
                            + "' in structured argument. This key will be ignored.\"");
        });
    }

    @Test
    void shouldLogArgumentsAsJsonWhenUsingKeyValue() {
        // GIVEN
        PowertoolsArguments requestHandler = new PowertoolsArguments(PowertoolsArguments.ArgumentFormat.ENTRY);
        SQSEvent.SQSMessage msg = new SQSEvent.SQSMessage();
        msg.setMessageId("1212abcd");
        msg.setBody("plop");
        msg.setEventSource("eb");
        msg.setAwsRegion("eu-west-1");
        SQSEvent.MessageAttribute attribute = new SQSEvent.MessageAttribute();
        attribute.setStringListValues(Arrays.asList("val1", "val2", "val3"));
        msg.setMessageAttributes(Collections.singletonMap("keyAttribute", attribute));

        // WHEN
        requestHandler.handleRequest(msg, context);

        // THEN
        File logFile = new File("target/logfile.json");
        assertThat(contentOf(logFile))
                .contains(
                        "\"input\":{\"awsRegion\":\"eu-west-1\",\"body\":\"plop\",\"eventSource\":\"eb\",\"messageAttributes\":{\"keyAttribute\":{\"stringListValues\":[\"val1\",\"val2\",\"val3\"]}},\"messageId\":\"1212abcd\"}")
                .contains("\"message\":\"1212abcd\"")
                .contains("\"message\":\"Message body = plop and id = \\\"1212abcd\\\"\"");

        // Reserved keys should be ignored
        PowertoolsLoggedFields.stringValues().stream().forEach(reservedKey -> {
            assertThat(contentOf(logFile)).doesNotContain("\"" + reservedKey + "\":\"shouldBeIgnored\"");
            assertThat(contentOf(logFile)).contains(
                    "\"message\":\"Attempted to use reserved key '" + reservedKey
                            + "' in structured argument. This key will be ignored.\"");
        });
    }

    private void setupContext() {
        when(context.getFunctionName()).thenReturn("testFunction");
        when(context.getInvokedFunctionArn()).thenReturn("testArn");
        when(context.getFunctionVersion()).thenReturn("1");
        when(context.getMemoryLimitInMB()).thenReturn(10);
        when(context.getAwsRequestId()).thenReturn("RequestId");
    }
}

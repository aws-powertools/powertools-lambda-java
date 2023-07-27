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

package org.demo.serialization;

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.events.SQSEvent;
import java.util.ArrayList;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

class SQSEventDeserializationFunctionTest {

    @Mock
    private Context context;
    private SQSEventDeserializationFunction deserializationFunction;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        deserializationFunction = new SQSEventDeserializationFunction();
    }

    @Test
    public void shouldReturnNumberOfReceivedMessages() {
        SQSEvent.SQSMessage message1 = messageWithBody("{  \"id\": 1234,  \"name\": \"product\",  \"price\": 42}");
        SQSEvent.SQSMessage message2 = messageWithBody("{  \"id\": 12345,  \"name\": \"product5\",  \"price\": 45}");
        SQSEvent event = new SQSEvent();
        event.setRecords(new ArrayList<SQSEvent.SQSMessage>() {{
            add(message1);
            add(message2);
        }});

        String response = deserializationFunction.handleRequest(event, context);

        assertEquals("Number of received messages: 2", response);
    }

    private SQSEvent.SQSMessage messageWithBody(String body) {
        SQSEvent.SQSMessage record1 = new SQSEvent.SQSMessage();
        record1.setBody(body);
        return record1;
    }
}
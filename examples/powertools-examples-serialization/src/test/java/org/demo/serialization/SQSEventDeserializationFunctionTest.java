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
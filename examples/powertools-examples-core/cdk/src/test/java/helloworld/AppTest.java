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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyResponseEvent;
import com.amazonaws.xray.AWSXRay;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

public class AppTest {

    @BeforeEach
    public void setup() {
        if (null == System.getenv("LAMBDA_TASK_ROOT")) {
            AWSXRay.beginSegment("test");
        }
    }

    @AfterEach
    public void tearDown() {
        if (AWSXRay.getCurrentSubsegmentOptional().isPresent()) {
            AWSXRay.endSubsegment();
        }

        if (null == System.getenv("LAMBDA_TASK_ROOT")) {
            AWSXRay.endSegment();
        }
    }

    @Test
    public void successfulResponse() {
        App app = new App();
        APIGatewayProxyResponseEvent result = app.handleRequest(null, null);
        assertEquals(200, result.getStatusCode());
        assertEquals("application/json", result.getHeaders().get("Content-Type"));
        String content = result.getBody();
        assertNotNull(content);
        assertTrue(content.contains("\"message\""));
        assertTrue(content.contains("\"hello world\""));
        assertTrue(content.contains("\"location\""));
    }
}

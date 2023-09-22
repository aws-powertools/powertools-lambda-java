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

package software.amazon.lambda.powertools.logging.internal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.contentOf;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.when;
import static org.mockito.MockitoAnnotations.openMocks;
import static software.amazon.lambda.powertools.common.internal.SystemWrapper.getenv;

import com.amazonaws.services.lambda.runtime.Context;
import java.io.File;
import java.io.IOException;
import java.nio.channels.FileChannel;
import java.nio.file.NoSuchFileException;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.slf4j.MDC;
import software.amazon.lambda.powertools.common.internal.SystemWrapper;
import software.amazon.lambda.powertools.logging.internal.handler.PowertoolsLogEnabled;

@Order(1)
class LambdaLoggingAspectTest {

    @Mock
    private Context context;

    @BeforeEach
    void setUp() throws IOException {
        openMocks(this);
        MDC.clear();
        setupContext();
        //Make sure file is cleaned up before running full stack logging regression
        try {
            FileChannel.open(Paths.get("target/logfile.json"), StandardOpenOption.WRITE).truncate(0).close();
        } catch (NoSuchFileException e) {
            // file might not be there for the first run
        }
    }

    @AfterEach
    void cleanUp() throws IOException {
//        FileChannel.open(Paths.get("target/logfile.json"), StandardOpenOption.WRITE).truncate(0).close();
    }

    @Test
    void testSlf4jBinding() {
        try (MockedStatic<SystemWrapper> mocked = mockStatic(SystemWrapper.class)) {
            mocked.when(() -> getenv("POWERTOOLS_SERVICE_NAME"))
                    .thenReturn("testLogback");

            PowertoolsLogEnabled handler = new PowertoolsLogEnabled();
            handler.handleRequest("Input", context);

            File logFile = new File("target/logfile.json");
            assertThat(contentOf(logFile))
                    .contains("slf4j.binding is set to ch.qos.logback.classic.spi.LogbackServiceProvider")
                    .contains("Loading software.amazon.lambda.powertools.logging.internal.LogbackLoggingManager");
        }
    }

    private void setupContext() {
        when(context.getFunctionName()).thenReturn("testFunction");
        when(context.getInvokedFunctionArn()).thenReturn(
                "arn:aws:lambda:eu-west-1:012345678910:function:testFunction:1");
        when(context.getFunctionVersion()).thenReturn("1");
        when(context.getMemoryLimitInMB()).thenReturn(1024);
        when(context.getAwsRequestId()).thenReturn("RequestId");
    }
}
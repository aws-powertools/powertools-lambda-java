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

import static org.assertj.core.api.Assertions.assertThat;

import com.amazonaws.services.lambda.runtime.events.KinesisEvent;
import com.amazonaws.services.lambda.runtime.events.SNSEvent;
import com.amazonaws.services.lambda.runtime.events.SQSEvent;
import org.junit.jupiter.api.Test;

public class LargeMessageProcessorFactoryTest {

    @Test
    public void createLargeSQSMessageProcessor() {
        assertThat(LargeMessageProcessorFactory.get(new SQSEvent.SQSMessage()))
                .isPresent()
                .get()
                .isInstanceOf(LargeSQSMessageProcessor.class);
    }

    @Test
    public void createLargeSNSMessageProcessor() {
        assertThat(LargeMessageProcessorFactory.get(new SNSEvent.SNSRecord()))
                .isPresent()
                .get()
                .isInstanceOf(LargeSNSMessageProcessor.class);
    }

    @Test
    public void createUnknownMessageProcessor() {
        assertThat(LargeMessageProcessorFactory.get(new KinesisEvent.KinesisEventRecord())).isNotPresent();
    }
}

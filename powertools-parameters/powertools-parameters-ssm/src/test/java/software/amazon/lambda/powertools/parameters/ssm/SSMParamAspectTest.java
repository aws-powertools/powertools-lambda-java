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

package software.amazon.lambda.powertools.parameters.ssm;

import static org.apache.commons.lang3.reflect.FieldUtils.writeStaticField;
import static org.assertj.core.api.Assertions.assertThat;

import java.util.function.Supplier;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
public class SSMParamAspectTest {

    // This class tests the SSM Param aspect in the same fashion
    // as the tests for the aspects for the other providers.

    @Test
    public void parameterInjectedByProvider() throws Exception {

        String key = "myKey";
        String value = "mySecretValue";
        SSMProvider provider = Mockito.mock(SSMProvider.class);

        Supplier<SSMProvider> providerBuilder = () -> provider;
        writeStaticField(SSMParamAspect.class, "providerBuilder", providerBuilder, true);

        // Setup our mocked SSMProvider to return a value for our test data
        Mockito.when(provider.get(key)).thenReturn(value);

        // Create an instance of a class and let the SSMParamAspect inject it
        MyInjectedClass obj = new MyInjectedClass();
        assertThat(obj.mySecret).isEqualTo(value);
    }

    class MyInjectedClass {
        @SSMParam(key = "myKey")
        public String mySecret;
    }

}

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

package software.amazon.lambda.powertools.parameters.dynamodb;

import static org.apache.commons.lang3.reflect.FieldUtils.writeStaticField;
import static org.assertj.core.api.Assertions.assertThat;

import java.util.function.Function;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

public class DynamoDbParamAspectTest {

    @Test
    public void parameterInjectedByProvider() throws Exception {
        // Setup our aspect to return a mocked DynamoDbProvider
        String tableName = "my-test-tablename";
        String key = "myKey";
        String value = "myValue";
        DynamoDbProvider provider = Mockito.mock(DynamoDbProvider.class);

        Function<String, DynamoDbProvider> providerBuilder = (String table) -> {
            if (table.equals(tableName)) {
                return provider;
            }
            throw new RuntimeException("Whoops! Asked for an app/env that we weren't configured for");
        };
        writeStaticField(DynamoDbParamAspect.class, "providerBuilder", providerBuilder, true);

        // Setup our mocked DynamoDbProvider to return a value for our test data
        Mockito.when(provider.get(key)).thenReturn(value);

        // Create an instance of a class and let the AppConfigParametersAspect inject it
        MyInjectedClass obj = new MyInjectedClass();
        assertThat(obj.myParameter).isEqualTo(value);
    }

    class MyInjectedClass {
        @DynamoDbParam(table = "my-test-tablename", key = "myKey")
        public String myParameter;
    }

}

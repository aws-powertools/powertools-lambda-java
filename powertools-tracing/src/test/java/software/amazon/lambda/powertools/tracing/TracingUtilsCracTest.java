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

package software.amazon.lambda.powertools.tracing;

import static org.assertj.core.api.Assertions.assertThatNoException;
import static org.mockito.Mockito.mock;

import java.lang.reflect.Field;
import org.crac.Context;
import org.crac.Resource;
import org.junit.jupiter.api.Test;

class TracingUtilsCracTest {

    Context<Resource> context = mock(Context.class);

    @Test
    void testPrimeMethodDoesNotThrowException() {
        assertThatNoException().isThrownBy(() -> TracingUtils.prime());
    }

    @Test
    void testTracingUtilsLoadsSuccessfully() {
        // Simply calling TracingUtils.prime() should trigger CRaC registration
        assertThatNoException().isThrownBy(() -> TracingUtils.prime());
        
        // Verify that TracingUtils class is loaded and accessible
        assertThatNoException().isThrownBy(() -> TracingUtils.objectMapper());
    }
}

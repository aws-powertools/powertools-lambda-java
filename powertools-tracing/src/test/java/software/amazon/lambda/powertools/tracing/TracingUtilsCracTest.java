/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy of
 * this software and associated documentation files (the "Software"), to deal in
 * the Software without restriction, including without limitation the rights to
 * use, copy, modify, merge, publish, distribute, sublicense, and/or sell copies of
 * the Software, and to permit persons to whom the Software is furnished to do so.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS
 * FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR
 * COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER
 * IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN
 * CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
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

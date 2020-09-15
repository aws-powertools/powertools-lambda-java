/*
 * Copyright 2020 Amazon.com, Inc. or its affiliates.
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
package software.amazon.lambda.powertools.parameters.transform;

import org.junit.jupiter.api.Test;
import software.amazon.lambda.powertools.parameters.exception.TransformationException;

import java.util.Base64;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;

public class Base64TransformerTest {

    @Test
    public void transform_base64_shouldTransformInString() {
        Base64Transformer transformer = new Base64Transformer();

        String s = transformer.applyTransformation(Base64.getEncoder().encodeToString("foobar".getBytes()));

        assertThat(s).isEqualTo("foobar");
    }

    @Test
    public void transform_base64WrongFormat_shouldThrowException() {
        Base64Transformer transformer = new Base64Transformer();

        assertThatExceptionOfType(TransformationException.class)
                .isThrownBy(() -> transformer.applyTransformation("foobarbaz"));
    }
}

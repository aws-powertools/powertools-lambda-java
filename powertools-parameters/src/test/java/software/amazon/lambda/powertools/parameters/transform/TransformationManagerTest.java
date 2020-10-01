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

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Base64;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalStateException;
import static software.amazon.lambda.powertools.parameters.transform.Transformer.base64;
import static software.amazon.lambda.powertools.parameters.transform.Transformer.json;

public class TransformationManagerTest {

    TransformationManager manager;

    @BeforeEach
    public void setup() {
        manager = new TransformationManager();
    }

    @Test
    public void setTransformer_shouldTransform() {
        manager.setTransformer(json);

        assertThat(manager.shouldTransform()).isTrue();
    }

    @Test
    public void notSetTransformer_shouldNotTransform() {
        assertThat(manager.shouldTransform()).isFalse();
    }

    @Test
    public void performBasicTransformation_noTransformer_shouldThrowException() {
        assertThatIllegalStateException()
                .isThrownBy(() -> manager.performBasicTransformation("value"));
    }

    @Test
    public void performBasicTransformation_notBasicTransformer_shouldThrowException() {
        manager.setTransformer(json);

        assertThatIllegalStateException()
                .isThrownBy(() -> manager.performBasicTransformation("value"));
    }

    @Test
    public void performBasicTransformation_shouldPerformTransformation() {
        manager.setTransformer(base64);

        String expectedValue = "bar";
        String value = manager.performBasicTransformation(Base64.getEncoder().encodeToString(expectedValue.getBytes()));

        assertThat(value).isEqualTo(expectedValue);
    }

    @Test
    public void performComplexTransformation_noTransformer_shouldThrowException() {
        assertThatIllegalStateException()
                .isThrownBy(() -> manager.performComplexTransformation("value", ObjectToDeserialize.class));
    }

    @Test
    public void performComplexTransformation_shouldPerformTransformation() {
        manager.setTransformer(json);

        ObjectToDeserialize object = manager.performComplexTransformation("{\"foo\":\"Foo\", \"bar\":42, \"baz\":123456789}", ObjectToDeserialize.class);

        assertThat(object).isNotNull();
    }
}

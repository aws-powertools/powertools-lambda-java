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

package software.amazon.lambda.powertools.parameters.internal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.MockitoAnnotations.openMocks;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import software.amazon.lambda.powertools.parameters.Param;
import software.amazon.lambda.powertools.parameters.ParamManager;
import software.amazon.lambda.powertools.parameters.SSMProvider;
import software.amazon.lambda.powertools.parameters.exception.TransformationException;
import software.amazon.lambda.powertools.parameters.transform.Base64Transformer;
import software.amazon.lambda.powertools.parameters.transform.JsonTransformer;
import software.amazon.lambda.powertools.parameters.transform.ObjectToDeserialize;

public class LambdaParametersAspectTest {

    @Mock
    private SSMProvider defaultProvider;

    @Param(key = "/default")
    private String defaultValue;

    @Param(key = "/simple", provider = CustomProvider.class)
    private String param;

    @Param(key = "/base64", provider = CustomProvider.class, transformer = Base64Transformer.class)
    private String basicTransform;

    @Param(key = "/json", provider = CustomProvider.class, transformer = JsonTransformer.class)
    private ObjectToDeserialize complexTransform;

    @Param(key = "/json", provider = CustomProvider.class, transformer = JsonTransformer.class)
    private AnotherObject wrongTransform;

    @BeforeEach
    public void init() {
        openMocks(this);
    }

    @Test
    public void testDefault_ShouldUseSSMProvider() {
        try (MockedStatic<ParamManager> mocked = mockStatic(ParamManager.class)) {
            mocked.when(() -> ParamManager.getProvider(SSMProvider.class)).thenReturn(defaultProvider);
            when(defaultProvider.get("/default")).thenReturn("value");

            assertThat(defaultValue).isEqualTo("value");
            mocked.verify(() -> ParamManager.getProvider(SSMProvider.class), times(1));
            verify(defaultProvider, times(1)).get("/default");

            mocked.reset();
        }
    }

    @Test
    public void testSimple() {
        assertThat(param).isEqualTo("value");
    }

    @Test
    public void testWithBasicTransform() {
        assertThat(basicTransform).isEqualTo("value");
    }

    @Test
    public void testWithComplexTransform() {
        assertThat(complexTransform)
                .isInstanceOf(ObjectToDeserialize.class)
                .matches(
                        o -> o.getFoo().equals("Foo") &&
                                o.getBar() == 42 &&
                                o.getBaz() == 123456789);
    }

    @Test
    public void testWithComplexTransformWrongTargetClass_ShouldThrowException() {
        assertThatExceptionOfType(TransformationException.class)
                .isThrownBy(() ->
                {
                    AnotherObject obj = wrongTransform;
                });
    }

}

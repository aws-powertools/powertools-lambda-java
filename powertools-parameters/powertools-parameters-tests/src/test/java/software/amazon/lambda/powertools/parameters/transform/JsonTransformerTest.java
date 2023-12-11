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

package software.amazon.lambda.powertools.parameters.transform;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.assertj.core.data.MapEntry.entry;

import java.util.Map;
import org.junit.jupiter.api.Test;
import software.amazon.lambda.powertools.parameters.exception.TransformationException;

public class JsonTransformerTest {

    @Test
    public void transform_json_shouldTransformInObject() throws TransformationException {
        JsonTransformer<ObjectToDeserialize> transformation = new JsonTransformer<>();

        ObjectToDeserialize objectToDeserialize =
                transformation.applyTransformation("{\"foo\":\"Foo\", \"bar\":42, \"baz\":123456789}",
                        ObjectToDeserialize.class);
        assertThat(objectToDeserialize).matches(
                o -> o.getFoo().equals("Foo")
                        && o.getBar() == 42
                        && o.getBaz() == 123456789);
    }

    @Test
    public void transform_json_shouldTransformInHashMap() throws TransformationException {
        JsonTransformer<Map> transformation = new JsonTransformer<>();

        Map<String, Object> map =
                transformation.applyTransformation("{\"foo\":\"Foo\", \"bar\":42, \"baz\":123456789}", Map.class);
        assertThat(map).contains(
                entry("foo", "Foo"),
                entry("bar", 42),
                entry("baz", 123456789));
    }

    @Test
    public void transform_badJson_shouldThrowException() {
        JsonTransformer<ObjectToDeserialize> transformation = new JsonTransformer<>();

        assertThatExceptionOfType(TransformationException.class)
                .isThrownBy(() -> transformation.applyTransformation("{\"fo\":\"Foo\", \"bat\":42, \"bau\":123456789}",
                        ObjectToDeserialize.class));
    }
}

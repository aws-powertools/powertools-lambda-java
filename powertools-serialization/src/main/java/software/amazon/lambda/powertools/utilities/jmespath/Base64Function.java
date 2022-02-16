/*
 * Copyright 2022 Amazon.com, Inc. or its affiliates.
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
package software.amazon.lambda.powertools.utilities.jmespath;

import io.burt.jmespath.Adapter;
import io.burt.jmespath.JmesPathType;
import io.burt.jmespath.function.ArgumentConstraints;
import io.burt.jmespath.function.BaseFunction;
import io.burt.jmespath.function.FunctionArgument;

import java.nio.ByteBuffer;
import java.util.Base64;
import java.util.List;

import static java.nio.charset.StandardCharsets.UTF_8;

/**
 * Function used by JMESPath to decode a Base64 encoded String into a decoded String
 */
public class Base64Function extends BaseFunction {

    public Base64Function() {
        super("powertools_base64", ArgumentConstraints.typeOf(JmesPathType.STRING));
    }

    @Override
    protected <T> T callFunction(Adapter<T> runtime, List<FunctionArgument<T>> arguments) {
        T value = arguments.get(0).value();
        String encodedString = runtime.toString(value);

        String decodedString = decode(encodedString);

        return runtime.createString(decodedString);
    }

    public static String decode(String encodedString) {
        return new String(decode(encodedString.getBytes(UTF_8)), UTF_8);
    }

    public static String decode(ByteBuffer byteBuffer) {
        return UTF_8.decode(byteBuffer).toString();
    }

    public static byte[] decode(byte[] encoded) {
        return Base64.getDecoder().decode(encoded);
    }
}

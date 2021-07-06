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
package software.amazon.lambda.powertools.validation.jmespath;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.Arrays;
import java.util.List;
import java.util.zip.GZIPInputStream;

import io.burt.jmespath.Adapter;
import io.burt.jmespath.JmesPathType;
import io.burt.jmespath.function.ArgumentConstraints;
import io.burt.jmespath.function.BaseFunction;
import io.burt.jmespath.function.FunctionArgument;

import static java.nio.charset.StandardCharsets.UTF_8;
import static software.amazon.lambda.powertools.validation.jmespath.Base64Function.decode;

/**
 * Function used by JMESPath to decode a Base64 encoded GZipped String into a decoded String
 */
public class Base64GZipFunction extends BaseFunction {

    public Base64GZipFunction() {
        super("powertools_base64_gzip", ArgumentConstraints.typeOf(JmesPathType.STRING));
    }

    @Override
    protected <T> T callFunction(Adapter<T> runtime, List<FunctionArgument<T>> arguments) {
        T value = arguments.get(0).value();
        String encodedString = runtime.toString(value);

        String decompressString = decompress(decode(encodedString.getBytes(UTF_8)));

        return runtime.createString(decompressString);
    }

    public static String decompress(byte[] compressed) {
        if ((compressed == null) || (compressed.length == 0)) {
            return "";
        }
        try {
            StringBuilder out = new StringBuilder();
            if (isCompressed(compressed)) {
                GZIPInputStream gzipStream = new GZIPInputStream(new ByteArrayInputStream(compressed));
                BufferedReader bf = new BufferedReader(new InputStreamReader(gzipStream, UTF_8));

                String line;
                while ((line = bf.readLine()) != null) {
                    out.append(line);
                }
            } else {
                out.append(Arrays.toString(compressed));
            }
            return out.toString();
        } catch (IOException e) {
            return new String(compressed, UTF_8);
        }
    }

    public static boolean isCompressed(final byte[] compressed) {
        return (compressed[0] == (byte) (GZIPInputStream.GZIP_MAGIC)) && (compressed[1] == (byte) (GZIPInputStream.GZIP_MAGIC >> 8));
    }
}

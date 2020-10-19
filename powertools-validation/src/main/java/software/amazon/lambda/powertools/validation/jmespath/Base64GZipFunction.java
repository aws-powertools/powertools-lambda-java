package software.amazon.lambda.powertools.validation.jmespath;

import io.burt.jmespath.Adapter;
import io.burt.jmespath.JmesPathType;
import io.burt.jmespath.function.ArgumentConstraints;
import io.burt.jmespath.function.BaseFunction;
import io.burt.jmespath.function.FunctionArgument;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.Arrays;
import java.util.List;
import java.util.zip.GZIPInputStream;

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
            return new String(compressed);
        }
    }

    public static boolean isCompressed(final byte[] compressed) {
        return (compressed[0] == (byte) (GZIPInputStream.GZIP_MAGIC)) && (compressed[1] == (byte) (GZIPInputStream.GZIP_MAGIC >> 8));
    }
}

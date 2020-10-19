package software.amazon.lambda.powertools.validation.jmespath;

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
        return new String(decode(encodedString.getBytes(UTF_8)));
    }

    public static String decode(ByteBuffer byteBuffer) {
        return UTF_8.decode(byteBuffer).toString();
    }

    public static byte[] decode(byte[] encoded) {
        return Base64.getDecoder().decode(encoded);
    }
}

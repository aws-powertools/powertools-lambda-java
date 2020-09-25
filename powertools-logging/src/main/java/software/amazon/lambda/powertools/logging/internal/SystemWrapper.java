package software.amazon.lambda.powertools.logging.internal;

public class SystemWrapper {
    public SystemWrapper() {
    }

    public static String getenv(String name) {
        return System.getenv(name);
    }
}

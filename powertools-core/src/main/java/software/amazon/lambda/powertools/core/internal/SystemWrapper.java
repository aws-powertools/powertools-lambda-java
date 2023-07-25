package software.amazon.lambda.powertools.core.internal;

public class SystemWrapper {
    private SystemWrapper() {
        // avoid instantiation, static methods
    }

    public static String getenv(String name) {
        return System.getenv(name);
    }
}

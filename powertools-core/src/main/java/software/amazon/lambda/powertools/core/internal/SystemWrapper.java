package software.amazon.lambda.powertools.core.internal;

public class SystemWrapper {
    private SystemWrapper() {
    }

    public static String getenv(String name) {
        return System.getenv(name);
    }
}

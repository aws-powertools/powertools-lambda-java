package software.amazon.lambda.powertools.logging.internal;

class SystemWrapper {
    private SystemWrapper() {
    }

    public static String getenv(String name) {
        return System.getenv(name);
    }
}

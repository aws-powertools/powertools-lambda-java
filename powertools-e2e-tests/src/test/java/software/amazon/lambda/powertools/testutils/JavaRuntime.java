package software.amazon.lambda.powertools.testutils;

import software.amazon.awscdk.services.lambda.Runtime;

public enum JavaRuntime {
    JAVA8("java8", Runtime.JAVA_8, "1.8"),
    JAVA8AL2("java8.al2", Runtime.JAVA_8_CORRETTO, "1.8"),
    JAVA11("java11", Runtime.JAVA_11, "11");

    private final String runtime;
    private final Runtime cdkRuntime;

    private final String mvnProperty;

    JavaRuntime(String runtime, Runtime cdkRuntime, String mvnProperty) {
        this.runtime = runtime;
        this.cdkRuntime = cdkRuntime;
        this.mvnProperty = mvnProperty;
    }

    public Runtime getCdkRuntime() {
        return cdkRuntime;
    }

    public String getRuntime() {
        return runtime;
    }

    @Override
    public String toString() {
        return runtime;
    }

    public String getMvnProperty() {
        return mvnProperty;
    }
}

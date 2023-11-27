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

package software.amazon.lambda.powertools.testutils;

import software.amazon.awscdk.services.lambda.Runtime;

public enum JavaRuntime {
    JAVA8("java8", Runtime.JAVA_8, "1.8"),
    JAVA8AL2("java8.al2", Runtime.JAVA_8_CORRETTO, "1.8"),
    JAVA11("java11", Runtime.JAVA_11, "11"),
    JAVA17("java17", Runtime.JAVA_17, "17"),
    JAVA21("java21", Runtime.JAVA_21, "21");

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

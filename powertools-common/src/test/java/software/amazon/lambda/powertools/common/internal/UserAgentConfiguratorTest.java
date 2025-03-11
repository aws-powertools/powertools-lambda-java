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

package software.amazon.lambda.powertools.common.internal;

import static org.assertj.core.api.Assertions.assertThat;
import static software.amazon.lambda.powertools.common.internal.UserAgentConfigurator.AWS_EXECUTION_ENV;
import static software.amazon.lambda.powertools.common.internal.UserAgentConfigurator.VERSION_KEY;
import static software.amazon.lambda.powertools.common.internal.UserAgentConfigurator.VERSION_PROPERTIES_FILENAME;
import static software.amazon.lambda.powertools.common.internal.UserAgentConfigurator.getVersionFromProperties;

import java.io.File;
import java.util.Objects;
import java.util.regex.Pattern;
import org.junit.jupiter.api.Test;
import org.junitpioneer.jupiter.SetEnvironmentVariable;

class UserAgentConfiguratorTest {

    private static final String SEM_VER_PATTERN =
            "^(0|[1-9]\\d*)\\.(0|[1-9]\\d*)\\.(0|[1-9]\\d*)(?:-((?:0|[1-9]\\d*|\\d*[a-zA-Z-][0-9a-zA-Z-]*)(?:\\.(?:0|[1-9]\\d*|\\d*[a-zA-Z-][0-9a-zA-Z-]*))*))?(?:\\+([0-9a-zA-Z-]+(?:\\.[0-9a-zA-Z-]+)*))?$";
    private static final String VERSION = UserAgentConfigurator.getProjectVersion();


    @Test
    void testGetVersion() {

        assertThat(VERSION)
                .isNotNull()
                .isNotEmpty();
        assertThat(Pattern.matches(SEM_VER_PATTERN, VERSION)).isTrue();
    }

    @Test
    void testGetVersionFromProperties_WrongKey() {
        String version = getVersionFromProperties(VERSION_PROPERTIES_FILENAME, "some invalid key");

        assertThat(version)
                .isNotNull()
                .isEqualTo("NA");
    }

    @Test
    void testGetVersionFromProperties_FileNotExist() {
        String version = getVersionFromProperties("some file", VERSION_KEY);

        assertThat(version)
                .isNotNull()
                .isEqualTo("NA");
    }

    @Test
    void testGetVersionFromProperties_InvalidFile() {
        File f = new File(Objects.requireNonNull(Thread.currentThread().getContextClassLoader()
                .getResource("unreadable.properties")).getPath());
        f.setReadable(false);

        String version = getVersionFromProperties("unreadable.properties", VERSION_KEY);

        assertThat(version).isEqualTo("NA");
    }

    @Test
    void testGetVersionFromProperties_EmptyVersion() {
        String version = getVersionFromProperties("test.properties", VERSION_KEY);

        assertThat(version).isEqualTo("NA");
    }

    @Test
    void testGetUserAgent() {
        String userAgent = UserAgentConfigurator.getUserAgent("test-feature");

        assertThat(userAgent)
                .isNotNull()
                .isEqualTo("PT/test-feature/" + VERSION + " PTEnv/NA");

    }

    @Test
    void testGetUserAgent_NoFeature() {
        String userAgent = UserAgentConfigurator.getUserAgent("");

        assertThat(userAgent)
                .isNotNull()
                .isEqualTo("PT/no-op/" + VERSION + " PTEnv/NA");
    }

    @Test
    void testGetUserAgent_NullFeature() {
        String userAgent = UserAgentConfigurator.getUserAgent(null);

        assertThat(userAgent)
                .isNotNull()
                .isEqualTo("PT/no-op/" + VERSION + " PTEnv/NA");
    }

    @Test
    @SetEnvironmentVariable(key = AWS_EXECUTION_ENV, value = "AWS_Lambda_java8")
    void testGetUserAgent_SetAWSExecutionEnv() {
        String userAgent = UserAgentConfigurator.getUserAgent("test-feature");

        assertThat(userAgent)
                .isNotNull()
                .isEqualTo("PT/test-feature/" + VERSION + " PTEnv/AWS_Lambda_java8");
    }

}

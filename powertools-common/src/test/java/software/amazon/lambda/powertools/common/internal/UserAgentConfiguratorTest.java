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
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.regex.Pattern;

import org.junit.jupiter.api.Test;
import org.junitpioneer.jupiter.SetEnvironmentVariable;

class UserAgentConfiguratorTest {

    private static final String SEM_VER_PATTERN = "^(0|[1-9]\\d*)\\.(0|[1-9]\\d*)\\.(0|[1-9]\\d*)(?:-((?:0|[1-9]\\d*|\\d*[a-zA-Z-][0-9a-zA-Z-]*)(?:\\.(?:0|[1-9]\\d*|\\d*[a-zA-Z-][0-9a-zA-Z-]*))*))?(?:\\+([0-9a-zA-Z-]+(?:\\.[0-9a-zA-Z-]+)*))?$";
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
    void testGetVersionFromProperties_InvalidFile() throws IOException {
        Path tempFile = Files.createTempFile("unreadable", ".properties");
        File f = tempFile.toFile();
        f.setReadable(false);

        String version = getVersionFromProperties(f.getName(), VERSION_KEY);

        assertThat(version).isEqualTo("NA");

        // Cleanup
        f.setReadable(true);
        Files.deleteIfExists(tempFile);
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
                .isEqualTo("PT/TEST-FEATURE/" + VERSION + " PTENV/NA");

    }

    @Test
    void testGetUserAgent_NoFeature() {
        String userAgent = UserAgentConfigurator.getUserAgent("");

        assertThat(userAgent)
                .isNotNull()
                .isEqualTo("PT/NO-OP/" + VERSION + " PTENV/NA");
    }

    @Test
    void testGetUserAgent_NullFeature() {
        String userAgent = UserAgentConfigurator.getUserAgent(null);

        assertThat(userAgent)
                .isNotNull()
                .isEqualTo("PT/NO-OP/" + VERSION + " PTENV/NA");
    }

    @Test
    @SetEnvironmentVariable(key = AWS_EXECUTION_ENV, value = "AWS_Lambda_java8")
    void testGetUserAgent_SetAWSExecutionEnv() {
        String userAgent = UserAgentConfigurator.getUserAgent("test-feature");

        assertThat(userAgent)
                .isNotNull()
                .isEqualTo("PT/TEST-FEATURE/" + VERSION + " PTENV/AWS_Lambda_java8");
    }

    @Test
    void testConfigureUserAgent() {
        System.clearProperty("sdk.ua.appId");
        UserAgentConfigurator.configureUserAgent("test-feature");

        assertThat(System.getProperty("sdk.ua.appId"))
                .isEqualTo("PT/TEST-FEATURE/" + VERSION + " PTENV/NA");
    }

    @Test
    void testConfigureUserAgent_WithExistingUserValue() {
        System.setProperty("sdk.ua.appId", "UserValueABC123");
        UserAgentConfigurator.configureUserAgent("test-feature");

        assertThat(System.getProperty("sdk.ua.appId"))
                .isEqualTo("UserValueABC123/PT/TEST-FEATURE/" + VERSION + " PTENV/NA");
    }

    @Test
    void testConfigureUserAgent_ReplacePowertoolsUserAgent() {
        System.setProperty("sdk.ua.appId", "PT/BATCH/" + VERSION + " PTENV/NA");
        UserAgentConfigurator.configureUserAgent("logging-log4j");

        assertThat(System.getProperty("sdk.ua.appId"))
                .isEqualTo("PT/LOGGING-LOG4J/" + VERSION + " PTENV/NA");
    }

    @Test
    void testConfigureUserAgent_PreserveUserValueAndReplacePowertools() {
        System.setProperty("sdk.ua.appId", "UserValue/PT/BATCH/" + VERSION + " PTENV/NA");
        UserAgentConfigurator.configureUserAgent("tracing");

        assertThat(System.getProperty("sdk.ua.appId"))
                .isEqualTo("UserValue/PT/TRACING/" + VERSION + " PTENV/NA");
    }

    @Test
    void testConfigureUserAgent_ExceedsLimit() {
        System.setProperty("sdk.ua.appId", "VeryLongUserValueThatExceedsTheLimitWhenCombined");
        UserAgentConfigurator.configureUserAgent("test-feature");

        // Should not update if it would exceed 50 characters
        assertThat(System.getProperty("sdk.ua.appId"))
                .isEqualTo("VeryLongUserValueThatExceedsTheLimitWhenCombined");
    }

    @Test
    void testExtractUserValue_NoUserValue() {
        String result = UserAgentConfigurator.extractUserValue("PT/BATCH/" + VERSION + " PTENV/NA");
        assertThat(result).isEmpty();
    }

    @Test
    void testExtractUserValue_WithUserValue() {
        String result = UserAgentConfigurator.extractUserValue("UserValue/PT/BATCH/" + VERSION + " PTENV/NA");
        assertThat(result).isEqualTo("UserValue");
    }

    @Test
    void testExtractUserValue_EmptyString() {
        String result = UserAgentConfigurator.extractUserValue("");
        assertThat(result).isEmpty();
    }

    @Test
    void testExtractUserValue_NullString() {
        String result = UserAgentConfigurator.extractUserValue(null);
        assertThat(result).isEmpty();
    }

    @Test
    void testExtractUserValue_OnlyUserValue() {
        String result = UserAgentConfigurator.extractUserValue("MyCustomValue");
        assertThat(result).isEqualTo("MyCustomValue");
    }

    @Test
    void testConfigureUserAgent_WithEmptyExistingValue() {
        System.setProperty("sdk.ua.appId", "");
        UserAgentConfigurator.configureUserAgent("test-feature");

        assertThat(System.getProperty("sdk.ua.appId"))
                .isEqualTo("PT/TEST-FEATURE/" + VERSION + " PTENV/NA");
    }

    @Test
    @SetEnvironmentVariable(key = AWS_EXECUTION_ENV, value = "AWS_Lambda_java11")
    void testConfigureUserAgent_MultipleUtilities() {
        System.clearProperty("sdk.ua.appId");

        // First utility
        UserAgentConfigurator.configureUserAgent("batch");
        assertThat(System.getProperty("sdk.ua.appId"))
                .isEqualTo("PT/BATCH/" + VERSION + " PTENV/AWS_Lambda_java11");

        // Second utility - should replace, not append
        UserAgentConfigurator.configureUserAgent("logging-log4j");
        assertThat(System.getProperty("sdk.ua.appId"))
                .isEqualTo("PT/LOGGING-LOG4J/" + VERSION + " PTENV/AWS_Lambda_java11");

        // Third utility - should replace again
        UserAgentConfigurator.configureUserAgent("tracing");
        assertThat(System.getProperty("sdk.ua.appId"))
                .isEqualTo("PT/TRACING/" + VERSION + " PTENV/AWS_Lambda_java11");
    }

}

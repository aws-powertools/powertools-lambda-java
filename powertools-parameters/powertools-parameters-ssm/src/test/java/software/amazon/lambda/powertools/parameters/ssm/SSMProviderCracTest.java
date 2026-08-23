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

package software.amazon.lambda.powertools.parameters.ssm;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatNoException;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import org.crac.Context;
import org.crac.Core;
import org.crac.Resource;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;
import software.amazon.awssdk.services.ssm.SsmClient;
import software.amazon.lambda.powertools.parameters.cache.CacheManager;
import software.amazon.lambda.powertools.parameters.transform.TransformationManager;

class SSMProviderCracTest {

    @Test
    void shouldRegisterWithGlobalCracContext() {
        RecordingContext recordingContext = new RecordingContext();

        try (MockedStatic<Core> mockedCore = mockStatic(Core.class)) {
            mockedCore.when(Core::getGlobalContext).thenReturn(recordingContext);

            SSMProvider.registerWithGlobalContext();
        }

        assertThat(recordingContext.registeredResources())
                .anyMatch(SSMProvider.class::isInstance);
    }

    @Test
    void testBeforeCheckpointDoesNotThrowException() {
        SSMProvider provider = primingProvider();
        Context<Resource> context = mock(Context.class);

        assertThatNoException().isThrownBy(() -> provider.beforeCheckpoint(context));
    }

    @Test
    void testAfterRestoreDoesNotThrowException() {
        SSMProvider provider = primingProvider();
        Context<Resource> context = mock(Context.class);

        assertThatNoException().isThrownBy(() -> provider.afterRestore(context));
    }

    @Test
    void classesloadedFileShouldContainSsmClassesAndExcludeTestOnlyAndS3() throws Exception {
        List<String> classes = loadClassesLoaded();

        assertThat(classes).contains(
                "software.amazon.lambda.powertools.parameters.ssm.SSMProvider",
                "software.amazon.lambda.powertools.parameters.ssm.SSMProviderBuilder",
                "software.amazon.lambda.powertools.parameters.cache.CacheManager",
                "software.amazon.awssdk.services.ssm.SsmClient");
        assertThat(classes).noneMatch(className -> className.startsWith("software.amazon.awssdk.services.s3"));
        assertThat(classes).noneMatch(className -> className.startsWith("org.junit"));
        assertThat(classes).noneMatch(className -> className.startsWith("org.mockito"));
        assertThat(classes).noneMatch(className -> className.startsWith("net.bytebuddy"));
        assertThat(classes).noneMatch(className -> className.startsWith("org.assertj"));
        assertThat(classes).noneMatch(className -> className.contains("MockitoMock"));
        assertThat(classes).noneMatch(className -> className.startsWith("opened:"));
        assertThat(classes).noneMatch(className -> className.startsWith("org.slf4j.simple"));
        assertThat(classes).noneMatch(className -> className.startsWith("org.apache.http"));
        assertThat(classes).noneMatch(className -> className.startsWith("org.apache.commons.logging"));
        assertThat(classes).noneMatch(className -> className.startsWith("org.apache.commons.lang3"));
        assertThat(classes).noneMatch(className -> className.startsWith("software.amazon.awssdk.http.apache"));
        assertThat(classes).noneMatch(className -> className.startsWith("software.amazon.awssdk.protocols.xml"));
        assertThat(classes).doesNotContain("org.apache.commons.logging.impl.Log4JLogger");
        assertThat(classes).noneMatch(className -> className.contains("SSMProviderTest")
                || className.contains("SSMProviderCracTest")
                || className.contains("SSMParamAspectTest")
                || className.contains("ParametersSsmUserAgentInterceptorTest"));
    }

    @Test
    void classesloadedFileShouldNotRequireAspectJ() throws Exception {
        List<String> classes = loadClassesLoaded();

        assertThat(classes).noneMatch(className -> className.startsWith("org.aspectj"));
        assertThat(classes).noneMatch(className -> className.contains("SSMParamAspect"));
        assertThat(classes).noneMatch(className -> className.contains("BaseParamAspect"));
    }

    private static SSMProvider primingProvider() {
        return new SSMProvider(new CacheManager(), new TransformationManager(), mock(SsmClient.class));
    }

    private static List<String> loadClassesLoaded() throws Exception {
        try (InputStream input = SSMProvider.class.getClassLoader().getResourceAsStream("classesloaded.txt")) {
            assertThat(input).isNotNull();
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(input, StandardCharsets.UTF_8))) {
                List<String> classes = new ArrayList<>();
                String line;
                while ((line = reader.readLine()) != null) {
                    if (!line.isBlank()) {
                        classes.add(line.strip());
                    }
                }
                return classes;
            }
        }
    }

    private static final class RecordingContext extends Context<Resource> {
        private final List<Resource> registeredResources = new ArrayList<>();

        @Override
        public void beforeCheckpoint(Context<? extends Resource> context) {
            // no-op
        }

        @Override
        public void afterRestore(Context<? extends Resource> context) {
            // no-op
        }

        @Override
        public void register(Resource resource) {
            registeredResources.add(resource);
        }

        List<Resource> registeredResources() {
            return registeredResources;
        }
    }
}

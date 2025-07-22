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

import java.nio.file.Paths;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

import software.amazon.awscdk.BundlingOptions;
import software.amazon.awscdk.BundlingOutput;
import software.amazon.awscdk.DockerImage;
import software.amazon.awscdk.DockerVolume;

/**
 * Configuration class for managing build environments and Docker settings
 * used during Lambda function compilation.
 */
public class DockerConfiguration {
    private final String baseImage;
    private final List<String> buildArgs;
    private final Map<String, String> environmentVariables;
    private final List<DockerVolume> volumes;

    private DockerConfiguration(Builder builder) {
        this.baseImage = builder.baseImage;
        this.buildArgs = builder.buildArgs;
        this.environmentVariables = builder.environmentVariables;
        this.volumes = builder.volumes;
    }

    public static Builder builder() {
        return new Builder();
    }

    public String getBaseImage() {
        return baseImage;
    }

    public List<String> getBuildArgs() {
        return buildArgs;
    }

    public Map<String, String> getEnvironmentVariables() {
        return environmentVariables;
    }

    public List<DockerVolume> getVolumes() {
        return volumes;
    }

    /**
     * Creates bundling options for GraalVM native image compilation.
     */
    public BundlingOptions createGraalVMBundlingOptions(String pathToFunction, JavaRuntime runtime) {
        List<String> packagingInstruction = Arrays.asList(
                "/bin/sh",
                "-c",
                "cd " + pathToFunction +
                        " && timeout -s SIGKILL 10m mvn clean package -Pnative-image -ff" +
                        " -Dmaven.test.skip=true" +
                        " -Dmaven.compiler.source=" + runtime.getMvnProperty() +
                        " -Dmaven.compiler.target=" + runtime.getMvnProperty() +
                        " && mkdir -p /tmp/lambda-package" +
                        " && cp /asset-input/" + pathToFunction + "/target/handler /tmp/lambda-package/" +
                        " && chmod +x /tmp/lambda-package/handler" +
                        " && echo '#!/bin/bash\nset -e\n./handler $_HANDLER' > /tmp/lambda-package/bootstrap" +
                        " && chmod +x /tmp/lambda-package/bootstrap" +
                        " && cd /tmp/lambda-package" +
                        " && zip -r /asset-output/function.zip .");

        return BundlingOptions.builder()
                .command(packagingInstruction)
                .image(DockerImage.fromRegistry(baseImage))
                .volumes(volumes)
                .environment(environmentVariables)
                .user("root")
                .outputType(BundlingOutput.ARCHIVED)
                .build();
    }

    /**
     * Creates bundling options for standard JVM compilation.
     */
    public BundlingOptions createJVMBundlingOptions(String pathToFunction, JavaRuntime runtime) {
        List<String> packagingInstruction = Arrays.asList(
                "/bin/sh",
                "-c",
                "cd " + pathToFunction +
                        " && timeout -s SIGKILL 5m mvn clean install -ff" +
                        " -Dmaven.test.skip=true" +
                        " -Dmaven.compiler.source=" + runtime.getMvnProperty() +
                        " -Dmaven.compiler.target=" + runtime.getMvnProperty() +
                        " && cp /asset-input/" + pathToFunction + "/target/function.jar /asset-output/");

        return BundlingOptions.builder()
                .command(packagingInstruction)
                .image(DockerImage.fromRegistry(baseImage))
                .volumes(volumes)
                .user("root")
                .outputType(BundlingOutput.ARCHIVED)
                .build();
    }

    /**
     * Creates a default Docker configuration for GraalVM native image compilation.
     */
    public static DockerConfiguration createGraalVMDefault(JavaRuntime runtime) {
        // Use custom Dockerfile for GraalVM
        String dockerDir = Paths.get(System.getProperty("user.dir"), "src", "test", "resources", "docker").toString();
        DockerImage customImage = DockerImage.fromBuild(dockerDir);

        return builder()
                .baseImage(customImage.getImage())
                .environmentVariables(Map.of("JAVA_VERSION", runtime.getMvnProperty()))
                .volumes(List.of(
                        DockerVolume.builder()
                                .hostPath(System.getProperty("user.home") + "/.m2/")
                                .containerPath("/root/.m2/")
                                .build()))
                .build();
    }

    /**
     * Creates a default Docker configuration for standard JVM compilation.
     */
    public static DockerConfiguration createJVMDefault(JavaRuntime runtime) {
        return builder()
                .baseImage(runtime.getCdkRuntime().getBundlingImage().getImage())
                .volumes(List.of(
                        DockerVolume.builder()
                                .hostPath(System.getProperty("user.home") + "/.m2/")
                                .containerPath("/root/.m2/")
                                .build()))
                .build();
    }

    public static class Builder {
        private String baseImage;
        private List<String> buildArgs;
        private Map<String, String> environmentVariables;
        private List<DockerVolume> volumes;

        public Builder baseImage(String baseImage) {
            this.baseImage = baseImage;
            return this;
        }

        public Builder buildArgs(List<String> buildArgs) {
            this.buildArgs = buildArgs;
            return this;
        }

        public Builder environmentVariables(Map<String, String> environmentVariables) {
            this.environmentVariables = environmentVariables;
            return this;
        }

        public Builder volumes(List<DockerVolume> volumes) {
            this.volumes = volumes;
            return this;
        }

        public DockerConfiguration build() {
            return new DockerConfiguration(this);
        }
    }
}

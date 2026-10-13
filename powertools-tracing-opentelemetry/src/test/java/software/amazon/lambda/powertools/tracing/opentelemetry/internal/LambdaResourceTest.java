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

package software.amazon.lambda.powertools.tracing.opentelemetry.internal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import io.opentelemetry.api.common.Attributes;
import io.opentelemetry.sdk.resources.Resource;
import org.junit.jupiter.api.Test;
import org.junitpioneer.jupiter.SetEnvironmentVariable;

class LambdaResourceTest {

    @Test
    @SetEnvironmentVariable(key = "AWS_LAMBDA_FUNCTION_NAME", value = "my-function")
    @SetEnvironmentVariable(key = "AWS_LAMBDA_FUNCTION_VERSION", value = "1")
    @SetEnvironmentVariable(key = "AWS_LAMBDA_FUNCTION_MEMORY_SIZE", value = "512")
    @SetEnvironmentVariable(key = "AWS_LAMBDA_LOG_STREAM_NAME", value = "2023/01/01/[$LATEST]abcd1234")
    @SetEnvironmentVariable(key = "AWS_REGION", value = "us-east-1")
    @SetEnvironmentVariable(key = "AWS_LAMBDA_FUNCTION_ARN", value = "arn:aws:lambda:us-east-1:123456789012:function" +
            ":my-function")
    void testCreateWithAllEnvironmentVariables() {

        Resource resource = LambdaResource.create();
        Attributes attributes = resource.getAttributes();

        assertThat(attributes.get(io.opentelemetry.api.common.AttributeKey.stringKey("cloud.provider")))
                .isEqualTo("aws");
        assertThat(attributes.get(io.opentelemetry.api.common.AttributeKey.stringKey("cloud.region")))
                .isEqualTo("us-east-1");
        assertThat(attributes.get(io.opentelemetry.api.common.AttributeKey.stringKey("cloud.account.id")))
                .isEqualTo("123456789012");
        assertThat(attributes.get(io.opentelemetry.api.common.AttributeKey.stringKey("service.name")))
                .isEqualTo("my-function");
        assertThat(attributes.get(io.opentelemetry.api.common.AttributeKey.stringKey("service.version")))
                .isEqualTo("1");
        assertThat(attributes.get(io.opentelemetry.api.common.AttributeKey.stringKey("faas.name")))
                .isEqualTo("my-function");
        assertThat(attributes.get(io.opentelemetry.api.common.AttributeKey.stringKey("faas.version")))
                .isEqualTo("1");
        assertThat(attributes.get(io.opentelemetry.api.common.AttributeKey.stringKey("faas.instance")))
                .isEqualTo("2023/01/01/[$LATEST]abcd1234");
        assertThat(attributes.get(io.opentelemetry.api.common.AttributeKey.longKey("faas.max_memory")))
                .isEqualTo(512L);
        assertThat(attributes.get(io.opentelemetry.api.common.AttributeKey.stringKey("telemetry.sdk.name")))
                .isEqualTo("opentelemetry");
        assertThat(attributes.get(io.opentelemetry.api.common.AttributeKey.stringKey("telemetry.distro.name")))
                .isEqualTo("powertools-for-aws-lambda");
        assertThat(attributes.get(io.opentelemetry.api.common.AttributeKey.stringKey("telemetry.sdk.language")))
                .isEqualTo("java");
    }

    @Test
    void testCreateWithNoEnvironmentVariables() {

        Resource resource = LambdaResource.create();
        Attributes attributes = resource.getAttributes();

        assertThat(attributes.get(io.opentelemetry.api.common.AttributeKey.stringKey("cloud.provider")))
                .isEqualTo("aws");
        assertThat(attributes.get(io.opentelemetry.api.common.AttributeKey.stringKey("telemetry.sdk.name")))
                .isEqualTo("opentelemetry");
        assertThat(attributes.get(io.opentelemetry.api.common.AttributeKey.stringKey("telemetry.distro.name")))
                .isEqualTo("powertools-for-aws-lambda");
        assertThat(attributes.get(io.opentelemetry.api.common.AttributeKey.stringKey("telemetry.sdk.language")))
                .isEqualTo("java");

        assertThat(attributes.get(io.opentelemetry.api.common.AttributeKey.stringKey("cloud.region"))).isNull();
        assertThat(attributes.get(io.opentelemetry.api.common.AttributeKey.stringKey("cloud.account.id"))).isNull();
        assertThat(attributes.get(io.opentelemetry.api.common.AttributeKey.stringKey("service.name"))).isNull();
        assertThat(attributes.get(io.opentelemetry.api.common.AttributeKey.stringKey("service.version"))).isNull();
        assertThat(attributes.get(io.opentelemetry.api.common.AttributeKey.stringKey("faas.name"))).isNull();
        assertThat(attributes.get(io.opentelemetry.api.common.AttributeKey.stringKey("faas.version"))).isNull();
        assertThat(attributes.get(io.opentelemetry.api.common.AttributeKey.stringKey("faas.instance"))).isNull();
        assertThat(attributes.get(io.opentelemetry.api.common.AttributeKey.longKey("faas.max_memory"))).isNull();
    }

    @Test
    @SetEnvironmentVariable(key = "AWS_LAMBDA_FUNCTION_NAME", value = "")
    @SetEnvironmentVariable(key = "AWS_REGION", value = "   ")
    void testCreateWithEmptyAndBlankEnvironmentVariables() {

        Resource resource = LambdaResource.create();
        Attributes attributes = resource.getAttributes();

        assertThat(attributes.get(io.opentelemetry.api.common.AttributeKey.stringKey("service.name"))).isNull();
        assertThat(attributes.get(io.opentelemetry.api.common.AttributeKey.stringKey("faas.name"))).isNull();
        assertThat(attributes.get(io.opentelemetry.api.common.AttributeKey.stringKey("cloud.region"))).isNull();
    }

    @Test
    @SetEnvironmentVariable(key = "AWS_LAMBDA_FUNCTION_MEMORY_SIZE", value = "1024")
    void testCreateWithMemorySize() {

        Resource resource = LambdaResource.create();
        Attributes attributes = resource.getAttributes();

        assertThat(attributes.get(io.opentelemetry.api.common.AttributeKey.longKey("faas.max_memory")))
                .isEqualTo(1024L);
    }

    @Test
    @SetEnvironmentVariable(key = "AWS_LAMBDA_FUNCTION_MEMORY_SIZE", value = "invalid")
    void testCreateWithInvalidMemorySize() {
        assertThatThrownBy(() -> LambdaResource.create())
                .isInstanceOf(NumberFormatException.class);
    }

    @Test
    @SetEnvironmentVariable(key = "AWS_LAMBDA_FUNCTION_ARN", value = "arn:aws:lambda:us-east-1:123456789012:function" +
            ":my-function")
    void testExtractAccountIdFromValidArn() {

        Resource resource = LambdaResource.create();
        Attributes attributes = resource.getAttributes();

        assertThat(attributes.get(io.opentelemetry.api.common.AttributeKey.stringKey("cloud.account.id")))
                .isEqualTo("123456789012");
    }

    @Test
    @SetEnvironmentVariable(key = "AWS_LAMBDA_FUNCTION_ARN", value = "arn:aws:lambda:us-west-2:999888777666:function" +
            ":another-function:1")
    void testExtractAccountIdFromArnWithVersion() {
        Resource resource = LambdaResource.create();
        Attributes attributes = resource.getAttributes();

        assertThat(attributes.get(io.opentelemetry.api.common.AttributeKey.stringKey("cloud.account.id")))
                .isEqualTo("999888777666");
    }

    @Test
    @SetEnvironmentVariable(key = "AWS_LAMBDA_FUNCTION_ARN", value = "arn:aws:lambda:eu-central-1:111222333444" +
            ":function:test-function:$LATEST")
    void testExtractAccountIdFromArnWithLatestAlias() {

        Resource resource = LambdaResource.create();
        Attributes attributes = resource.getAttributes();

        assertThat(attributes.get(io.opentelemetry.api.common.AttributeKey.stringKey("cloud.account.id")))
                .isEqualTo("111222333444");
    }

    @Test
    @SetEnvironmentVariable(key = "AWS_LAMBDA_FUNCTION_ARN", value = "arn:aws:lambda:us-east-1")
    void testExtractAccountIdFromIncompleteArn() {

        Resource resource = LambdaResource.create();
        Attributes attributes = resource.getAttributes();

        assertThat(attributes.get(io.opentelemetry.api.common.AttributeKey.stringKey("cloud.account.id")))
                .isNull();
    }

    @Test
    @SetEnvironmentVariable(key = "AWS_LAMBDA_FUNCTION_ARN", value = "arn:aws:lambda")
    void testExtractAccountIdFromVeryShortArn() {

        Resource resource = LambdaResource.create();
        Attributes attributes = resource.getAttributes();

        assertThat(attributes.get(io.opentelemetry.api.common.AttributeKey.stringKey("cloud.account.id")))
                .isNull();
    }

    @Test
    @SetEnvironmentVariable(key = "AWS_LAMBDA_FUNCTION_ARN", value = "invalid-arn")
    void testExtractAccountIdFromInvalidArn() {

        Resource resource = LambdaResource.create();
        Attributes attributes = resource.getAttributes();

        assertThat(attributes.get(io.opentelemetry.api.common.AttributeKey.stringKey("cloud.account.id")))
                .isNull();
    }

    @Test
    @SetEnvironmentVariable(key = "AWS_LAMBDA_FUNCTION_ARN", value = "")
    void testExtractAccountIdFromEmptyArn() {

        Resource resource = LambdaResource.create();
        Attributes attributes = resource.getAttributes();

        assertThat(attributes.get(io.opentelemetry.api.common.AttributeKey.stringKey("cloud.account.id")))
                .isNull();
    }

    @Test
    @SetEnvironmentVariable(key = "AWS_LAMBDA_FUNCTION_NAME", value = "test-function")
    @SetEnvironmentVariable(key = "AWS_LAMBDA_FUNCTION_VERSION", value = "$LATEST")
    void testCreateWithLatestVersion() {

        Resource resource = LambdaResource.create();
        Attributes attributes = resource.getAttributes();

        assertThat(attributes.get(io.opentelemetry.api.common.AttributeKey.stringKey("service.version")))
                .isEqualTo("$LATEST");
        assertThat(attributes.get(io.opentelemetry.api.common.AttributeKey.stringKey("faas.version")))
                .isEqualTo("$LATEST");
    }

    @Test
    @SetEnvironmentVariable(key = "AWS_LAMBDA_LOG_STREAM_NAME", value = "2023/12/31/[$LATEST]xyz789")
    void testCreateWithLogStreamName() {

        Resource resource = LambdaResource.create();
        Attributes attributes = resource.getAttributes();

        assertThat(attributes.get(io.opentelemetry.api.common.AttributeKey.stringKey("faas.instance")))
                .isEqualTo("2023/12/31/[$LATEST]xyz789");
    }

    @Test
    void testCreateReturnsResourceWithAttributes() {

        Resource resource = LambdaResource.create();

        assertThat(resource).isNotNull();
        assertThat(resource.getAttributes()).isNotNull();
        assertThat(resource.getAttributes().size()).isGreaterThan(0);
    }

    @Test
    @SetEnvironmentVariable(key = "AWS_REGION", value = "ap-southeast-1")
    void testCreateWithDifferentRegion() {

        Resource resource = LambdaResource.create();
        Attributes attributes = resource.getAttributes();

        assertThat(attributes.get(io.opentelemetry.api.common.AttributeKey.stringKey("cloud.region")))
                .isEqualTo("ap-southeast-1");
    }

    @Test
    void testTelemetryAttributesAreAlwaysPresent() {

        Resource resource = LambdaResource.create();
        Attributes attributes = resource.getAttributes();

        assertThat(attributes.get(io.opentelemetry.api.common.AttributeKey.stringKey("cloud.provider")))
                .isNotNull()
                .isEqualTo("aws");
        assertThat(attributes.get(io.opentelemetry.api.common.AttributeKey.stringKey("telemetry.sdk.name")))
                .isNotNull()
                .isEqualTo("opentelemetry");
        assertThat(attributes.get(io.opentelemetry.api.common.AttributeKey.stringKey("telemetry.distro.name")))
                .isNotNull()
                .isEqualTo("powertools-for-aws-lambda");
        assertThat(attributes.get(io.opentelemetry.api.common.AttributeKey.stringKey("telemetry.sdk.language")))
                .isNotNull()
                .isEqualTo("java");
    }

    @Test
    @SetEnvironmentVariable(key = "AWS_LAMBDA_FUNCTION_ARN", value = "arn:aws:lambda:us-east-1:123456789012:function" +
            ":my-function:alias-name")
    void testCreateWithArnContainingAlias() {

        Resource resource = LambdaResource.create();
        Attributes attributes = resource.getAttributes();

        assertThat(attributes.get(io.opentelemetry.api.common.AttributeKey.stringKey("cloud.account.id")))
                .isEqualTo("123456789012");
    }

    @Test
    @SetEnvironmentVariable(key = "AWS_LAMBDA_FUNCTION_MEMORY_SIZE", value = "128")
    void testCreateWithMinimumMemorySize() {
        Resource resource = LambdaResource.create();
        Attributes attributes = resource.getAttributes();

        assertThat(attributes.get(io.opentelemetry.api.common.AttributeKey.longKey("faas.max_memory")))
                .isEqualTo(128L);
    }

    @Test
    @SetEnvironmentVariable(key = "AWS_LAMBDA_FUNCTION_MEMORY_SIZE", value = "10240")
    void testCreateWithMaximumMemorySize() {
        Resource resource = LambdaResource.create();
        Attributes attributes = resource.getAttributes();

        assertThat(attributes.get(io.opentelemetry.api.common.AttributeKey.longKey("faas.max_memory")))
                .isEqualTo(10240L);
    }
}
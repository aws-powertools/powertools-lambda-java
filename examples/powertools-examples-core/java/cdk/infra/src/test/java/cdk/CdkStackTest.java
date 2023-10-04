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

package cdk;

import java.util.HashMap;
import java.util.Map;
import org.junit.jupiter.api.Test;
import software.amazon.awscdk.App;
import software.amazon.awscdk.assertions.Template;

public class CdkStackTest {

    @Test
    public void testStack() {
        App app = new App();
        CdkStack stack = new CdkStack(app, "test");

        Template template = Template.fromStack(stack);

        // There should be 2 lambda functions, one to handle regular input, and another for streaming
        template.resourceCountIs("AWS::Lambda::Function", 2);

        // API Gateway should exist
        template.resourceCountIs("AWS::ApiGateway::RestApi", 1);

        // API Gateway should have a path pointing to the regular Lambda
        Map<String, String> resourceProperties = new HashMap<>();
        resourceProperties.put("PathPart", "hello");
        template.hasResourceProperties("AWS::ApiGateway::Resource", resourceProperties);

        // API Gateway should have a path pointing to the streaming Lambda
        resourceProperties = new HashMap<>();
        resourceProperties.put("PathPart", "hellostream");
        template.hasResourceProperties("AWS::ApiGateway::Resource", resourceProperties);
    }
}

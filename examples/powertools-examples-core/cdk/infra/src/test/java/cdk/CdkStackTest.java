package cdk;

import java.util.HashMap;
import org.junit.jupiter.api.Test;
import software.amazon.awscdk.App;
import software.amazon.awscdk.assertions.Template;

import java.io.IOException;
import java.util.Map;

public class CdkStackTest {

    @Test
    public void testStack() throws IOException {
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

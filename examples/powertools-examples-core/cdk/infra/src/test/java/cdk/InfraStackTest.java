package cdk;

import org.junit.jupiter.api.Test;
import software.amazon.awscdk.App;
import software.amazon.awscdk.assertions.Template;

import java.io.IOException;
import java.util.Map;

public class InfraStackTest {

    @Test
    public void testStack() throws IOException {
        App app = new App();
        CdkStack stack = new CdkStack(app, "test");

        Template template = Template.fromStack(stack);

        // The Lambda function should exist
        template.resourceCountIs("AWS::Lambda::Function", 1);

        // API Gateway should exist
        template.resourceCountIs("AWS::ApiGateway::RestApi", 1);

        // API Gateway should have a path pointing to the Lambda
        template.hasResourceProperties("AWS::ApiGateway::Resource", Map.of("PathPart", "hello"));
    }
}

 package com.myorg;

 import software.amazon.awscdk.App;
 import software.amazon.awscdk.assertions.Template;
 import java.io.IOException;

 import java.util.HashMap;

 import org.junit.jupiter.api.Test;

 public class PowertoolsExamplesCloudformationCdkTest {

     @Test
     public void testStack() throws IOException {
         App app = new App();
         PowertoolsExamplesCloudformationCdkStack stack = new PowertoolsExamplesCloudformationCdkStack(app, "test");

         Template template = Template.fromStack(stack);

         template.hasResourceProperties("AWS::Lambda::Function", new HashMap<String, Number>() {{
           put("MemorySize", 512);
         }});
     }
 }

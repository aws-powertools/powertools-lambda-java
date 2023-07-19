package com.myorg;

import software.amazon.awscdk.App;
import software.amazon.awscdk.StackProps;

public class PowertoolsExamplesCloudformationCdkApp {
    public static void main(final String[] args) {
        App app = new App();

        new PowertoolsExamplesCloudformationCdkStack(app, "PowertoolsExamplesCloudformationCdkStack", StackProps.builder()
                .build());

        app.synth();
    }
}


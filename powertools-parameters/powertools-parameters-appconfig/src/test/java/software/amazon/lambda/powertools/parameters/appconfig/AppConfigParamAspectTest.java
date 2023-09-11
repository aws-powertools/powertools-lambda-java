package software.amazon.lambda.powertools.parameters.appconfig;

import static org.apache.commons.lang3.reflect.FieldUtils.writeStaticField;
import static org.assertj.core.api.Assertions.assertThat;

import java.util.function.BiFunction;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

public class AppConfigParamAspectTest {

    @Test
    public void parameterInjectedByProvider() throws Exception {
        // Setup our aspect to return a mocked AppConfigProvider
        String environment = "myEnvironment";
        String appName = "myApp";
        String key = "myKey";
        String value = "myValue";
        AppConfigProvider provider = Mockito.mock(AppConfigProvider.class);
        BiFunction<String, String, AppConfigProvider> providerBuilder = (String env, String app) -> {
            if (env.equals(environment) && app.equals(appName)) {
                return provider;
            }
            throw new RuntimeException("Whoops! Asked for an app/env that we weren't configured for");
        };
        writeStaticField(AppConfigParametersAspect.class, "providerBuilder", providerBuilder, true);

        // Setup our mocked AppConfigProvider to return a value for our test data
        Mockito.when(provider.get(key)).thenReturn(value);

        // Create an instance of a class and let the AppConfigParametersAspect inject it
        MyInjectedClass obj = new MyInjectedClass();
        assertThat(obj.myParameter).isEqualTo(value);
    }

    class MyInjectedClass {
        @AppConfigParam(application = "myApp", environment = "myEnvironment", key = "myKey")
        public String myParameter;
    }

}

package software.amazon.lambda.powertools.parameters.appconfig;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import software.amazon.lambda.powertools.parameters.BaseProvider;
import software.amazon.lambda.powertools.parameters.transform.Transformer;

@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.FIELD)
public @interface AppConfigParam {
    String key();

    String environment();

    String application();

    Class<? extends BaseProvider> provider();

    Class<? extends Transformer> transformer() default Transformer.class;
}

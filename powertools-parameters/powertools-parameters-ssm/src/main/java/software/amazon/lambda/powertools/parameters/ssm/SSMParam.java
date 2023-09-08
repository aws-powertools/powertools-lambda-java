package software.amazon.lambda.powertools.parameters.ssm;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import software.amazon.lambda.powertools.parameters.transform.Transformer;

@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.FIELD)
public @interface SSMParam {
    String key();

    Class<? extends Transformer> transformer() default Transformer.class;
}

package software.amazon.lambda.tracing;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.METHOD)
public @interface PowerToolTracing {
    String namespace() default "";
    boolean captureResponse() default true;
    boolean captureError() default true;
}

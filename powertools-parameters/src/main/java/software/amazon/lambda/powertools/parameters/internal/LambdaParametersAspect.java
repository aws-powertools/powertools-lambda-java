package software.amazon.lambda.powertools.parameters.internal;

import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Pointcut;
import org.aspectj.lang.reflect.FieldSignature;
import software.amazon.lambda.powertools.parameters.*;

@Aspect
public class LambdaParametersAspect {

    @Pointcut("get(* *) && @annotation(paramAnnotation)")
    public void getParam(Param paramAnnotation) {
    }

    @Around("getParam(paramAnnotation)")
    public Object injectParam(final ProceedingJoinPoint joinPoint, final Param paramAnnotation) {
        if(null == paramAnnotation.provider()) {
            throw new IllegalArgumentException("provider for Param annotation cannot be null!");
        }
        BaseProvider provider = ParamManager.getProvider(paramAnnotation.provider());

        if(paramAnnotation.transformer().isInterface()) {
            // No transformation
            return provider.get(paramAnnotation.key());
        } else {
            FieldSignature s = (FieldSignature) joinPoint.getSignature();
            if(String.class.isAssignableFrom(s.getFieldType())) {
                // Basic transformation
                return provider
                        .withTransformation(paramAnnotation.transformer())
                        .get(paramAnnotation.key());
            } else {
                // Complex transformation
                return provider
                        .withTransformation(paramAnnotation.transformer())
                        .get(paramAnnotation.key(), s.getFieldType());
            }
        }
    }

}

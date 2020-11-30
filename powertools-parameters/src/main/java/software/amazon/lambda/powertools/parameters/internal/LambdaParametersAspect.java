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
        BaseProvider provider = ParamManager.getProvider(paramAnnotation.provider());
        if(null == provider) {
            throw new IllegalArgumentException(String.format("ParamProvider %s not supported.", paramAnnotation.provider().getName()));
        }
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

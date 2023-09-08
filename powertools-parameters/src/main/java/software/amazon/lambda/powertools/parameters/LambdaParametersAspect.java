/*
 * Copyright 2023 Amazon.com, Inc. or its affiliates.
 * Licensed under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *     http://www.apache.org/licenses/LICENSE-2.0
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 */

package software.amazon.lambda.powertools.parameters;

import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Pointcut;
import org.aspectj.lang.reflect.FieldSignature;
import software.amazon.lambda.powertools.parameters.BaseProvider;
import software.amazon.lambda.powertools.parameters.Param;
import software.amazon.lambda.powertools.parameters.ParamManager;

@Aspect
public class LambdaParametersAspect {

    @Pointcut("get(* *) && @annotation(paramAnnotation)")
    public void getParam(Param paramAnnotation) {
    }

    // TODO - get rid of this

//    @Around("getParam(paramAnnotation)")
//    public Object injectParam(final ProceedingJoinPoint joinPoint, final Param paramAnnotation) {
//        BaseProvider provider = ParamManager.getProvider(paramAnnotation.provider());
//
//        if (paramAnnotation.transformer().isInterface()) {
//            // No transformation
//            return provider.get(paramAnnotation.key());
//        } else {
//            FieldSignature s = (FieldSignature) joinPoint.getSignature();
//            if (String.class.isAssignableFrom(s.getFieldType())) {
//                // Basic transformation
//                return provider
//                        .withTransformation(paramAnnotation.transformer())
//                        .get(paramAnnotation.key());
//            } else {
//                // Complex transformation
//                return provider
//                        .withTransformation(paramAnnotation.transformer())
//                        .get(paramAnnotation.key(), s.getFieldType());
//            }
//        }
//    }

}

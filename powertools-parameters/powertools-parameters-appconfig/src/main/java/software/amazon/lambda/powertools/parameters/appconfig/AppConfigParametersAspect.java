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

package software.amazon.lambda.powertools.parameters.appconfig;

import java.util.function.BiFunction;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Pointcut;
import org.aspectj.lang.reflect.FieldSignature;
import software.amazon.lambda.powertools.parameters.BaseParamAspect;

/**
 * Provides the AppConfig parameter aspect. This aspect is responsible for injecting
 * parameters from AppConfig into fields annotated with @AppConfigParam. See the
 * README and Powertools for Lambda (Java) documentation for information on using this feature.
 */
@Aspect
public class AppConfigParametersAspect extends BaseParamAspect {

    private static BiFunction<String, String, AppConfigProvider> providerBuilder =
        (String env, String app) -> AppConfigProvider.builder()
                .withEnvironment(env)
                .withApplication(app)
                .build();


    @Pointcut("get(* *) && @annotation(appConfigParamAnnotation)")
    public void getParam(AppConfigParam appConfigParamAnnotation) {
    }

    @Around("getParam(appConfigParamAnnotation)")
    public Object injectParam(final ProceedingJoinPoint joinPoint, final AppConfigParam appConfigParamAnnotation) {

        AppConfigProvider provider = providerBuilder.apply
                (appConfigParamAnnotation.environment(), appConfigParamAnnotation.application());

        return getAndTransform(appConfigParamAnnotation.key(), appConfigParamAnnotation.transformer(), provider,
                (FieldSignature) joinPoint.getSignature());
    }

}

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

import software.amazon.lambda.powertools.parameters.cache.CacheManager;
import software.amazon.lambda.powertools.parameters.transform.TransformationManager;

/**
 * Utility class to retrieve instances of parameter providers.
 * Each instance is unique (singleton).
 */
public final class ParamManager {

    private static final CacheManager cacheManager = new CacheManager();
    private static final TransformationManager transformationManager = new TransformationManager();

    public static CacheManager getCacheManager() {
        return cacheManager;
    }

    public static TransformationManager getTransformationManager() {
        return transformationManager;
    }


}

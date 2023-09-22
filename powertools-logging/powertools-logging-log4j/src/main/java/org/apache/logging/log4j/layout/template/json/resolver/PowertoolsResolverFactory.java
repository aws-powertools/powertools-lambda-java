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

package org.apache.logging.log4j.layout.template.json.resolver;

import org.apache.logging.log4j.core.LogEvent;
import org.apache.logging.log4j.core.config.plugins.Plugin;
import org.apache.logging.log4j.core.config.plugins.PluginFactory;

/**
 * Factory for {@link PowertoolsResolver}. Log4j plugin to process powertools fields in the layout.json
 */
@Plugin(name = "PowertoolsResolverFactory", category = TemplateResolverFactory.CATEGORY)
public final class PowertoolsResolverFactory implements EventResolverFactory {

    private static final PowertoolsResolverFactory INSTANCE = new PowertoolsResolverFactory();
    private static final String RESOLVER_NAME = "powertools";

    private PowertoolsResolverFactory() {
    }

    @PluginFactory
    public static PowertoolsResolverFactory getInstance() {
        return INSTANCE;
    }

    @Override
    public String getName() {
        return RESOLVER_NAME;
    }

    @Override
    public TemplateResolver<LogEvent> create(EventResolverContext context,
                                             TemplateResolverConfig config) {
        return new PowertoolsResolver(config);
    }
}

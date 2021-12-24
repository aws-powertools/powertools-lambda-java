package software.amazon.lambda.powertools.logging;

import org.apache.logging.log4j.core.LogEvent;
import org.apache.logging.log4j.core.config.plugins.Plugin;
import org.apache.logging.log4j.core.config.plugins.PluginFactory;
import org.apache.logging.log4j.layout.template.json.resolver.EventResolverContext;
import org.apache.logging.log4j.layout.template.json.resolver.EventResolverFactory;
import org.apache.logging.log4j.layout.template.json.resolver.TemplateResolver;
import org.apache.logging.log4j.layout.template.json.resolver.TemplateResolverConfig;
import org.apache.logging.log4j.layout.template.json.resolver.TemplateResolverFactory;

@Plugin(name = "PowertoolsResolverFactory", category = TemplateResolverFactory.CATEGORY)
public final class PowertoolsResolverFactory implements EventResolverFactory {

    private static final PowertoolsResolverFactory INSTANCE = new PowertoolsResolverFactory();

    private PowertoolsResolverFactory() {}

    @PluginFactory
    public static PowertoolsResolverFactory getInstance() {
        return INSTANCE;
    }

    @Override
    public String getName() {
        return PowertoolsResolver.getName();
    }

    @Override
    public TemplateResolver<LogEvent> create(EventResolverContext context,
                                             TemplateResolverConfig config) {
        return new PowertoolsResolver();
    }
}

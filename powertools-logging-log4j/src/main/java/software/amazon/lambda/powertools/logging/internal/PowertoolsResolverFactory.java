package software.amazon.lambda.powertools.logging.internal;

import org.apache.logging.log4j.core.LogEvent;
import org.apache.logging.log4j.core.config.plugins.Plugin;
import org.apache.logging.log4j.core.config.plugins.PluginFactory;
import org.apache.logging.log4j.layout.template.json.resolver.*;

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
        return new PowertoolsResolver(config);
    }
}

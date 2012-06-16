package org.broadinstitute.sequel.infrastructure.deployment;


import java.util.HashMap;
import java.util.Map;


/**
 * Managed beans can not be parameterized types, so rather than extending a BaseConfigurationProducer class,
 * the configuration system delegates the containment of configuration instances to this class.
 * ConfigurationHolder tries to take some of the pain out of the restriction that managed beans cannot be
 * parameterized types, but we still end up with a slew of non-DRY @Produces methods on the ConfigurationProducer
 * implementations that delegate to the ConfigurationHolder.  I believe there is currently no way around this, the
 * @Produces methods need to be on the implementing class.
 *
 * @param <T>
 */
public class ConfigurationHolder<T extends BaseConfiguration> {


    private Map<Deployment, T> configurations;


    public ConfigurationHolder() {
        configurations = new HashMap<Deployment, T>();
    }


    public void add(Deployment deployment, T configuration) {
        configuration.setDeployment(deployment);
        configurations.put(deployment, configuration);
    }


    public T get(Deployment deployment) {
        return configurations.get(deployment);
    }

}

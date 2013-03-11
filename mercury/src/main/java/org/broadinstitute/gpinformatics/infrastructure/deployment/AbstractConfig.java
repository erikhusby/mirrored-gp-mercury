package org.broadinstitute.gpinformatics.infrastructure.deployment;


import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import javax.inject.Inject;

/**
 * Base class of concrete configurations.
 */
public abstract class AbstractConfig {

    private static final Log log = LogFactory.getLog(AbstractConfig.class);

    /**
     * This appears to be unused but has proven useful in past debugging to explicitly identify the external deployment.
     */
    @SuppressWarnings({"FieldCanBeLocal", "UnusedDeclaration"})
    private Deployment externalDeployment;


    @Inject
    private Deployment mercuryDeployment;


    public void setExternalDeployment(Deployment externalDeployment) {
        this.externalDeployment = externalDeployment;
    }


    /**
     * Return the AbstractConfig appropriate to the explicitly specified Mercury deployment.
     *
     * @param deployment Explicitly specified Mercury deployment.
     * @param <C> The type of the AbstractConfig-derived configuration class.
     * @return Appropriately configured AbstractConfig-derived instance.
     */
    protected static <C extends AbstractConfig> C produce(Class<C> clazz, Deployment deployment) {

        //noinspection unchecked
        return (C) MercuryConfiguration.getInstance().getConfig(clazz, deployment);
    }

}

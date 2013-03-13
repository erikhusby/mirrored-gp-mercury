package org.broadinstitute.gpinformatics.infrastructure.deployment;


import org.apache.commons.beanutils.BeanUtils;

import javax.annotation.Nullable;
import java.lang.reflect.InvocationTargetException;

/**
 * Base class of concrete configurations.
 */
public abstract class AbstractConfig {

    protected AbstractConfig(@Nullable Deployment mercuryDeployment) {

        this.mercuryDeployment = mercuryDeployment;

        if (mercuryDeployment != null && mercuryDeployment != Deployment.STUBBY) {
            AbstractConfig source = produce(getClass(), mercuryDeployment);
            try {
                BeanUtils.copyProperties(this, source);
            } catch (IllegalAccessException e) {
                throw new RuntimeException(e);
            } catch (InvocationTargetException e) {
                throw new RuntimeException(e);
            }
        }
    }


    /**
     * This appears to be unused but has proven useful in past debugging to explicitly identify the external deployment.
     */
    @SuppressWarnings({"FieldCanBeLocal", "UnusedDeclaration"})
    private Deployment externalDeployment;

    /**
     * Useful for debugging.
     */
    @SuppressWarnings({"FieldCanBeLocal", "UnusedDeclaration"})
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

package org.broadinstitute.gpinformatics.infrastructure.deployment;


import org.apache.commons.beanutils.BeanUtils;

import javax.annotation.Nonnull;
import java.lang.reflect.InvocationTargetException;

/**
 * Base class of concrete configurations.  Be careful on usage when trying to @Inject since YAML needs to get a servlet
 * or file protocol handler for {@link org.scannotation.AnnotationDB}.  Its possible to call this in an EJB or @Startup
 * so that won't have the web application deployed, The annotation scanning will only have VFS and the Mercury war will
 * fail to deploy with an error.
 */
public abstract class AbstractConfig {

    public AbstractConfig(){}

    protected AbstractConfig(@Nonnull Deployment deploymentConfig) {
        if (deploymentConfig != Deployment.STUBBY) {
            AbstractConfig source = produce(getClass(), deploymentConfig);
            // Only get the properties if the configuration is supported.
            if (source != null) {
                try {
                    BeanUtils.copyProperties(this, source);
                } catch (IllegalAccessException | InvocationTargetException | IllegalArgumentException e) {
                    throw new RuntimeException(e);
                }
            }
        }

        this.deploymentConfig = deploymentConfig;
    }

    /**
     * Check to see if a particular service is supported (i.e.running) for this configuration.  If it is supported, then
     * it will return true, otherwise false.
     * <p/>
     * TODO: Replace this with something more elegant like a NotSupported configuration object.
     *
     * @param config The configuration to see if it's supported
     *
     * @return true if configuration is supported.
     */
    public static boolean isSupported(AbstractConfig config) {
        return (config != null);
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
    private Deployment deploymentConfig;

    public void setExternalDeployment(Deployment externalDeployment) {
        this.externalDeployment = externalDeployment;
    }

    /**
     * Return the AbstractConfig appropriate to the explicitly specified Mercury deployment.
     *
     * @param deployment Explicitly specified Mercury deployment.
     * @param <C> The type of the AbstractConfig-derived configuration class.
     *
     * @return Appropriately configured AbstractConfig-derived instance.
     */
    protected static <C extends AbstractConfig> C produce(Class<C> clazz, Deployment deployment) {
        @SuppressWarnings({"UnnecessaryLocalVariable", "unchecked"})
        C config = (C) MercuryConfiguration.getInstance().getConfig(clazz, deployment);
        return config;
    }

    public Deployment getDeploymentConfig() {
        return deploymentConfig;
    }

    public static String getHttpScheme() {
        return "http://";
    }

    public Deployment getExternalDeployment() {
        return externalDeployment;
    }
}

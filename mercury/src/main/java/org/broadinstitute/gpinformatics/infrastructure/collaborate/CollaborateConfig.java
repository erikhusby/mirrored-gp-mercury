package org.broadinstitute.gpinformatics.infrastructure.collaborate;

import org.broadinstitute.gpinformatics.infrastructure.deployment.AbstractConfig;
import org.broadinstitute.gpinformatics.infrastructure.deployment.ConfigKey;
import org.broadinstitute.gpinformatics.infrastructure.deployment.Deployment;

import javax.annotation.Nonnull;
import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

/**
 * The YAML config associated object that gets populated from the YAML files.
 */
@SuppressWarnings("UnusedDeclaration")
@ConfigKey("collaborate")
@ApplicationScoped
public class CollaborateConfig extends AbstractConfig {

    private String urlBase;

    public CollaborateConfig(){}

    @Inject
    public CollaborateConfig(@Nonnull Deployment deployment) {
        super(deployment);
    }

    public String getUrlBase() {
        return urlBase;
    }

    public void setUrlBase(String urlBase) {
        this.urlBase = urlBase;
    }

    public static CollaborateConfig produce(Deployment deployment) {
        return produce(CollaborateConfig.class, deployment);
    }
}

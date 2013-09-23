package org.broadinstitute.gpinformatics.infrastructure.portal;

import org.broadinstitute.gpinformatics.infrastructure.deployment.AbstractConfig;
import org.broadinstitute.gpinformatics.infrastructure.deployment.ConfigKey;
import org.broadinstitute.gpinformatics.infrastructure.deployment.Deployment;

import javax.annotation.Nonnull;
import javax.inject.Inject;
import java.io.Serializable;

@SuppressWarnings("UnusedDeclaration")
@ConfigKey("portal")
public class PortalConfig extends AbstractConfig implements Serializable {
    private String urlBase;

    @Inject
    public PortalConfig(@Nonnull Deployment deployment) {
        super(deployment);
    }

    public String getUrlBase() {
        return urlBase;
    }

    public void setUrlBase(String urlBaseIn) {
        urlBase = urlBaseIn;
    }

    public static PortalConfig produce(Deployment deployment) {
        return produce(PortalConfig.class, deployment);
    }
}

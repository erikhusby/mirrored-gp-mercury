package org.broadinstitute.gpinformatics.infrastructure.squid;


import org.broadinstitute.gpinformatics.infrastructure.deployment.AbstractConfig;
import org.broadinstitute.gpinformatics.infrastructure.deployment.ConfigKey;
import org.broadinstitute.gpinformatics.infrastructure.deployment.Deployment;

import javax.annotation.Nullable;
import javax.enterprise.inject.Default;
import javax.enterprise.inject.Produces;
import javax.inject.Inject;
import java.io.Serializable;

/**
 * Configuration to look up Squid connection parameters, currently limited to the base URL.
 *
 */
@ConfigKey("squid")
public class SquidConfig extends AbstractConfig implements Serializable {

    private String url;

    @Inject
    public SquidConfig(@Nullable Deployment deployment) {
        super(deployment);
    }


    public String getUrl() {
        return url;
    }

    public void setUrl(String url) {
        this.url = url;
    }


    public static SquidConfig produce(Deployment deployment) {
        return produce(SquidConfig.class, deployment);
    }
}

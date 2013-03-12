package org.broadinstitute.gpinformatics.infrastructure.deployment;

import javax.annotation.Nullable;
import javax.enterprise.inject.Default;
import javax.enterprise.inject.Produces;
import javax.inject.Inject;
import java.io.Serializable;

/**
 * @author breilly
 */
// called "app" because "mercury" has a special meaning in the YAML file
@ConfigKey("app")
public class MercuryConfig extends AbstractConfig implements Serializable {

    @Inject
    public MercuryConfig(@Nullable Deployment deployment) {
        super(deployment);
    }

    private String url;

    public String getUrl() {
        return url;
    }

    public void setUrl(String url) {
        this.url = url;
    }
}

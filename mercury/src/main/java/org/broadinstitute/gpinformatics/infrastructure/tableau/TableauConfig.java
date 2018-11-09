package org.broadinstitute.gpinformatics.infrastructure.tableau;

import org.broadinstitute.gpinformatics.infrastructure.deployment.AbstractConfig;
import org.broadinstitute.gpinformatics.infrastructure.deployment.ConfigKey;
import org.broadinstitute.gpinformatics.infrastructure.deployment.Deployment;

import javax.annotation.Nonnull;
import javax.enterprise.context.Dependent;
import javax.inject.Inject;
import java.io.Serializable;

/**
 * Configuration for Tableau.
 */
@ConfigKey("tableau")
@Dependent
public class TableauConfig extends AbstractConfig implements Serializable {

    private String url;

    @Inject
    public TableauConfig(@Nonnull Deployment mercuryDeployment) {
        super(mercuryDeployment);
    }

    public String getUrl() {
        return url;
    }

    public void setUrl(String url) {
        this.url = url;
    }
}

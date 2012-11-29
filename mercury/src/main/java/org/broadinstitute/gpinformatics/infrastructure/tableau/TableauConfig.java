package org.broadinstitute.gpinformatics.infrastructure.tableau;

import org.broadinstitute.gpinformatics.infrastructure.deployment.AbstractConfig;
import org.broadinstitute.gpinformatics.infrastructure.deployment.ConfigKey;

import java.io.Serializable;

@ConfigKey("tableau")
public class TableauConfig extends AbstractConfig implements Serializable {

    private String urlBase;

    public TableauConfig() {}

    public String getUrlBase() {
        return urlBase;
    }

    public void setUrlBase(String urlBaseIn) {
        urlBase = urlBaseIn;
    }

}

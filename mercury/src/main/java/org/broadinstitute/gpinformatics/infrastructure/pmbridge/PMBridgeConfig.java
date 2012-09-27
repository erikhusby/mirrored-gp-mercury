package org.broadinstitute.gpinformatics.infrastructure.pmbridge;


import org.broadinstitute.gpinformatics.infrastructure.deployment.AbstractConfig;
import org.broadinstitute.gpinformatics.infrastructure.deployment.ConfigKey;

import java.io.Serializable;

@ConfigKey("pmbridge")
public class PMBridgeConfig extends AbstractConfig implements Serializable {


    private String url;

    public PMBridgeConfig() {
    }

    public String getUrl() {
        return url;
    }

    public void setUrl(String url) {
        this.url = url;
    }
}

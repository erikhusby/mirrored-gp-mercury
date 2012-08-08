package org.broadinstitute.sequel.infrastructure.pmbridge;


import org.broadinstitute.sequel.infrastructure.deployment.AbstractConfig;
import org.broadinstitute.sequel.infrastructure.deployment.ConfigKey;

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

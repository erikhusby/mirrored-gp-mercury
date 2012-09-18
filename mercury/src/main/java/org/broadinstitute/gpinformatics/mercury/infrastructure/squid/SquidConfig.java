package org.broadinstitute.gpinformatics.mercury.infrastructure.squid;


import org.broadinstitute.gpinformatics.mercury.infrastructure.deployment.AbstractConfig;
import org.broadinstitute.gpinformatics.mercury.infrastructure.deployment.ConfigKey;

import java.io.Serializable;

/**
 * Configuration to look up Squid connection parameters, currently limited to the base URL.
 *
 */
@ConfigKey("squid")
public class SquidConfig extends AbstractConfig implements Serializable {


    private String url;


    public SquidConfig() {}


    public String getUrl() {
        return url;
    }

    public void setUrl(String url) {
        this.url = url;
    }
}

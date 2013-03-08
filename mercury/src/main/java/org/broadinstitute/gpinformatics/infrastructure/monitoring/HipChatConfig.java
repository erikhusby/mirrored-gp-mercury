package org.broadinstitute.gpinformatics.infrastructure.monitoring;

import org.broadinstitute.gpinformatics.infrastructure.deployment.AbstractConfig;
import org.broadinstitute.gpinformatics.infrastructure.deployment.ConfigKey;

@ConfigKey("hipchat")
public class HipChatConfig extends AbstractConfig {

    private String baseUrl;

    public HipChatConfig() {
    }

    public String getBaseUrl() {
        return baseUrl;
    }

    public void setBaseUrl(String baseUrl) {
        this.baseUrl = baseUrl;
    }
}

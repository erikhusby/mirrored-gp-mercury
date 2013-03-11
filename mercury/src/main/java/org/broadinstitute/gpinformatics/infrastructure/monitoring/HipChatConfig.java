package org.broadinstitute.gpinformatics.infrastructure.monitoring;

import org.broadinstitute.gpinformatics.infrastructure.deployment.AbstractConfig;
import org.broadinstitute.gpinformatics.infrastructure.deployment.ConfigKey;

import java.io.Serializable;

@ConfigKey("hipchat")
public class HipChatConfig extends AbstractConfig implements Serializable {

    // todo arz add other fields to config, in particular the room,
    // authorization key, and from value

    private String gpLimsRoom;

    private String baseUrl;

    public HipChatConfig() {
    }

    public String getBaseUrl() {
        return baseUrl;
    }

    public void setBaseUrl(String baseUrl) {
        this.baseUrl = baseUrl;
    }

    public String getGpLimsRoom() {
        return gpLimsRoom;
    }

    public void setGpLimsRoom(String gpLimsRoom) {
        this.gpLimsRoom = gpLimsRoom;
    }
}

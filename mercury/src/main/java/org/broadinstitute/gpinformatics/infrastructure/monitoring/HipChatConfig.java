package org.broadinstitute.gpinformatics.infrastructure.monitoring;

import org.broadinstitute.gpinformatics.infrastructure.deployment.AbstractConfig;
import org.broadinstitute.gpinformatics.infrastructure.deployment.ConfigKey;
import org.broadinstitute.gpinformatics.infrastructure.deployment.Deployment;

import javax.annotation.Nullable;
import javax.inject.Inject;
import java.io.Serializable;

@ConfigKey("hipchat")
public class HipChatConfig extends AbstractConfig implements Serializable {

    private String authorizationToken;

    private String gpLimsRoom;

    private String baseUrl;

    @Inject
    public HipChatConfig(@Nullable Deployment deployment) {
        super(deployment);
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

    public String getAuthorizationToken() {
        return authorizationToken;
    }

    public void setAuthorizationToken(String authorizationToken) {
        this.authorizationToken = authorizationToken;
    }
}

package org.broadinstitute.gpinformatics.infrastructure.monitoring;

import org.broadinstitute.gpinformatics.infrastructure.deployment.AbstractConfig;
import org.broadinstitute.gpinformatics.infrastructure.deployment.ConfigKey;
import org.broadinstitute.gpinformatics.infrastructure.deployment.Deployment;

import javax.annotation.Nonnull;
import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import java.io.Serializable;

@SuppressWarnings("UnusedDeclaration")
@ConfigKey("hipchat")
@ApplicationScoped
public class HipChatConfig extends AbstractConfig implements Serializable {

    private String authorizationToken;

    private String gpLimsRoom;

    private String baseUrl;

    public HipChatConfig(){}

    @Inject
    public HipChatConfig(@Nonnull Deployment deployment) {
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

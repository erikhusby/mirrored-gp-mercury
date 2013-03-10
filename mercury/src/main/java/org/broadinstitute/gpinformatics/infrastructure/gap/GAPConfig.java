package org.broadinstitute.gpinformatics.infrastructure.gap;


import org.broadinstitute.gpinformatics.infrastructure.deployment.AbstractConfig;
import org.broadinstitute.gpinformatics.infrastructure.deployment.ConfigKey;
import org.broadinstitute.gpinformatics.mercury.control.LoginAndPassword;

import javax.enterprise.inject.Default;
import javax.enterprise.inject.Produces;
import java.io.Serializable;

@ConfigKey("gap")
public class GAPConfig extends AbstractConfig implements LoginAndPassword, Serializable {

    private String login;

    private String password;

    private String url;


    public GAPConfig() {}

    public String getLogin() {
        return login;
    }

    public void setLogin(String login) {
        this.login = login;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public String getUrl() {
        return url;
    }

    public void setUrl(String url) {
        this.url = url;
    }

    @Produces
    @Default
    public GAPConfig produce() {

        return produce(GAPConfig.class);
    }
}

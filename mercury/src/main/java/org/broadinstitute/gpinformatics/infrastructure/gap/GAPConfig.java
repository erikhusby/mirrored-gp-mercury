package org.broadinstitute.gpinformatics.infrastructure.gap;

import org.broadinstitute.gpinformatics.infrastructure.deployment.AbstractConfig;
import org.broadinstitute.gpinformatics.infrastructure.deployment.ConfigKey;
import org.broadinstitute.gpinformatics.infrastructure.deployment.Deployment;
import org.broadinstitute.gpinformatics.mercury.control.LoginAndPassword;

import javax.annotation.Nonnull;
import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import java.io.Serializable;

/**
 * Connection information for the Genetic Analysis Platform (Arrays / Genotyping) web application.
 */
@SuppressWarnings("UnusedDeclaration")
@ConfigKey("gap")
@ApplicationScoped
public class GAPConfig extends AbstractConfig implements LoginAndPassword, Serializable {

    private String login;

    private String password;

    private String host;

    private int port;

    public GAPConfig(){}

    @Inject
    public GAPConfig(@Nonnull Deployment deployment) {
        super(deployment);
    }

    @Override
    public String getLogin() {
        return login;
    }

    @Override
    public String getPassword() {
        return password;
    }

    public String getHost() {
        return host;
    }

    public int getPort() {
        return port;
    }

    public void setLogin(String login) {
        this.login = login;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public void setHost(String host) {
        this.host = host;
    }

    public void setPort(int port) {
        this.port = port;
    }

    public String getUrl(String suffix) {
        return String.format("%s%s:%d/%s", getHttpScheme(),getHost(), getPort(), suffix);
    }

    public static GAPConfig produce(Deployment deployment) {
        return produce(GAPConfig.class, deployment);
    }

}
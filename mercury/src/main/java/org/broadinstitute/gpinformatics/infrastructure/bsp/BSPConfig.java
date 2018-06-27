package org.broadinstitute.gpinformatics.infrastructure.bsp;

import org.broadinstitute.gpinformatics.infrastructure.deployment.AbstractConfig;
import org.broadinstitute.gpinformatics.infrastructure.deployment.ConfigKey;
import org.broadinstitute.gpinformatics.infrastructure.deployment.Deployment;
import org.broadinstitute.gpinformatics.mercury.control.LoginAndPassword;

import javax.annotation.Nonnull;
import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import java.io.Serializable;


@SuppressWarnings("UnusedDeclaration")
@ConfigKey("bsp")
@ApplicationScoped
public class BSPConfig extends AbstractConfig implements LoginAndPassword, Serializable {

    /** Use this path to perform a search on a BSP barcode. */
    public static final String SEARCH_PATH = "collection/find.action?barcode= ";

    private String login;

    private String password;

    private String host;

    private int port;

    public BSPConfig(){}

    @Inject
    public BSPConfig(@Nonnull Deployment deployment) {
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
        return String.format("%s%s:%d/BSP/%s", getHttpScheme(),getHost(), getPort(), suffix);
    }

    public String getWSUrl(String suffix) {
        return String.format("%s%s:%d/ws/bsp/%s", getHttpScheme(), getHost(), getPort(), suffix);
    }

    /**
     * @return the web service URL used by JAX RS web services in BSP
     */
    public String getJaxRsWebServiceUrl(String suffix) {
        return getUrl("rest/" + suffix);
    }

    public static BSPConfig produce(Deployment deployment) {
        return produce(BSPConfig.class, deployment);
    }

    public String getWorkRequestLink(String workRequestId) {
        return getUrl(BSPConfig.SEARCH_PATH + workRequestId);
    }
}

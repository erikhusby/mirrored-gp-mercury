/*
 * The Broad Institute
 * SOFTWARE COPYRIGHT NOTICE AGREEMENT
 * This software and its documentation are copyright 2014 by the
 * Broad Institute/Massachusetts Institute of Technology. All rights are reserved.
 *
 * This software is supplied without any warranty or guaranteed support
 * whatsoever. Neither the Broad Institute nor MIT can be responsible for its
 * use, misuse, or functionality.
 */

package org.broadinstitute.gpinformatics.infrastructure.submission;

import org.broadinstitute.gpinformatics.infrastructure.deployment.AbstractConfig;
import org.broadinstitute.gpinformatics.infrastructure.deployment.ConfigKey;
import org.broadinstitute.gpinformatics.infrastructure.deployment.Deployment;
import org.broadinstitute.gpinformatics.mercury.control.LoginAndPassword;

import javax.annotation.Nonnull;
import javax.inject.Inject;
import java.io.Serializable;


@SuppressWarnings("UnusedDeclaration")
@ConfigKey("submission")
public class SubmissionConfig extends AbstractConfig implements LoginAndPassword, Serializable {
    private String login;

    private String password;

    private String host;

    private int port;

    @Inject
    public SubmissionConfig(@Nonnull Deployment deployment) {
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
    public static String getHttpScheme() {
            return "https://";
        }
    public String getUrl(String suffix) {
        return getWSUrl(suffix);
    }
    public String getWSUrl(String suffix) {
        return String.format("%s%s:%d/%s", getHttpScheme(),getHost(), getPort(), suffix);
    }

    /**
     * @return the web service URL used by JAX RS web services in BSP
     */
    public static SubmissionConfig produce(Deployment deployment) {
        return produce(SubmissionConfig.class, deployment);
    }
}

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
public class SubmissionConfig extends AbstractConfig implements Serializable {

    private String host;

    private int port;

    @Inject
    public SubmissionConfig(@Nonnull Deployment deployment) {
        super(deployment);
    }

    public String getHost() {
        return host;
    }

    public int getPort() {
        return port;
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
    public String getUrl() {
        return getWSUrl();
    }
    public String getWSUrl() {
        return String.format("%s%s:%d/", getHttpScheme(),getHost(), getPort());
    }

    /**
     * @return the web service URL used by JAX RS web services in BSP
     */
    public static SubmissionConfig produce(Deployment deployment) {
        return produce(SubmissionConfig.class, deployment);
    }
}

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

import javax.annotation.Nonnull;
import javax.enterprise.context.Dependent;
import javax.inject.Inject;
import java.io.Serializable;


@SuppressWarnings("UnusedDeclaration")
@ConfigKey("submission")
@Dependent
public class SubmissionConfig extends AbstractConfig implements Serializable {
    public static final String LIST_BIOPROJECTS_ACTION="bioproject/all";
    public static final String SUBMIT_ACTION="submission/submitrequest";
    public static final String SUBMISSIONS_STATUS_URI = "submission/status";
    public static final String SUBMISSION_SAMPLES_ACTION = "bioproject/biosamples";
    public static final String ALL_SUBMISSION_SITES = "site/all";
    public static final String SUBMISSION_TYPES = "submissiondatatypes";
    public static final String SUBMISSION_BAM_LOCATIONS = "submissionbamlocations";

    private String login;

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
        return "http://";
    }

    public String getUrl() {
        return getWSUrl();
    }

    public String getWSUrl() {
        return String.format("%s%s:%d/", getHttpScheme(), getHost(), getPort());
    }

    public String getWSUrl(String suffix) {
        return String.format("%s%s:%d/%s", getHttpScheme(), getHost(), getPort(), suffix);
    }

    /**
     * @return the web service URL used by JAX RS web services in BSP
     */
    public static SubmissionConfig produce(Deployment deployment) {
        return produce(SubmissionConfig.class, deployment);
    }
}

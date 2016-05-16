/*
 * The Broad Institute
 * SOFTWARE COPYRIGHT NOTICE AGREEMENT
 * This software and its documentation are copyright 2016 by the
 * Broad Institute/Massachusetts Institute of Technology. All rights are reserved.
 *
 * This software is supplied without any warranty or guaranteed support
 * whatsoever. Neither the Broad Institute nor MIT can be responsible for its
 * use, misuse, or functionality.
 */

package org.broadinstitute.gpinformatics.infrastructure.bass;

import org.broadinstitute.gpinformatics.infrastructure.deployment.Deployment;
import org.broadinstitute.gpinformatics.infrastructure.submission.SubmissionsService;
import org.broadinstitute.gpinformatics.infrastructure.submission.SubmissionsServiceImpl;
import org.broadinstitute.gpinformatics.infrastructure.submission.SubmissionsServiceStub;

import javax.enterprise.context.RequestScoped;
import javax.enterprise.inject.Default;
import javax.enterprise.inject.New;
import javax.enterprise.inject.Produces;
import javax.inject.Inject;

/**
 * TODO scottmat fill in javadoc!!!
 */
public class BassSearchServiceProducer {

    @Inject
    private Deployment deployment;

    public static BassSearchService stubInstance() {
        return new BassSearchServiceStub();
    }


    @Produces
    @Default
    @RequestScoped
    public BassSearchService produce(@New BassSearchServiceStub stub, @New BassSearchServiceImpl impl) {
        if (deployment == Deployment.STUBBY) {
            return stub;
        }
        return impl;
    }
}

package org.broadinstitute.gpinformatics.infrastructure.submission;

import org.broadinstitute.gpinformatics.infrastructure.deployment.Deployment;

import javax.enterprise.context.RequestScoped;
import javax.enterprise.inject.Default;
import javax.enterprise.inject.New;
import javax.enterprise.inject.Produces;
import javax.inject.Inject;

/**
 * TODO scottmat fill in javadoc!!!
 */
public class SubmissionsServiceProducer {

    @Inject
    private Deployment deployment;

    public static SubmissionsService stubInstance() {
        return new SubmissionsServiceStub();
    }


    @Produces
    @Default
    @RequestScoped
    public SubmissionsService produce(@New SubmissionsServiceStub stub, @New SubmissionsServiceImpl impl) {
        if (deployment == Deployment.STUBBY) {
            return stub;
        }
        return impl;
    }
}

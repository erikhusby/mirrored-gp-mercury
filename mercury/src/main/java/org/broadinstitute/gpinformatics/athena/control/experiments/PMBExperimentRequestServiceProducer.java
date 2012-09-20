package org.broadinstitute.gpinformatics.athena.control.experiments;

import org.broadinstitute.gpinformatics.infrastructure.deployment.Deployment;

import javax.enterprise.context.SessionScoped;
import javax.enterprise.inject.Default;
import javax.enterprise.inject.New;
import javax.enterprise.inject.Produces;
import javax.inject.Inject;

import static org.broadinstitute.gpinformatics.infrastructure.deployment.Deployment.STUBBY;

public class PMBExperimentRequestServiceProducer {

        @Inject
        private Deployment deployment;


        @Produces
        @Default
        @SessionScoped
        public PMBExperimentRequestService produce(@New PMBExperimentRequestServiceImpl impl) {

            // there currently is no stub
            if ( deployment == STUBBY )
                return null;

            return impl;
        }

    }

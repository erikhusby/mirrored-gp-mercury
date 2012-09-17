package org.broadinstitute.sequel.infrastructure.squid;


import org.broadinstitute.sequel.infrastructure.deployment.Deployment;
import org.broadinstitute.sequel.infrastructure.deployment.TestInstance;
import org.broadinstitute.sequel.infrastructure.pmbridge.AbstractConfigProducer;

import javax.enterprise.inject.Default;
import javax.enterprise.inject.Produces;
import javax.inject.Inject;

import static org.broadinstitute.sequel.infrastructure.deployment.Deployment.TEST;


public class SquidConfigProducer extends AbstractConfigProducer<SquidConfig> {

    @Inject
    private Deployment deployment;


    @Produces
    @TestInstance
    public SquidConfig testInstance() {
        return produce( TEST );
    }


    @Produces
    @Default
    public SquidConfig produce() {
        return produce( deployment );

    }


    public static SquidConfig getConfig( Deployment deployment ) {
        return new SquidConfigProducer().produce( deployment );
    }


}

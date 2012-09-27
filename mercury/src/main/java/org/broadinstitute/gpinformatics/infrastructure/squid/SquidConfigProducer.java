package org.broadinstitute.gpinformatics.infrastructure.squid;


import org.broadinstitute.gpinformatics.infrastructure.deployment.Deployment;
import org.broadinstitute.gpinformatics.infrastructure.deployment.TestInstance;
import org.broadinstitute.gpinformatics.infrastructure.pmbridge.AbstractConfigProducer;

import javax.enterprise.inject.Default;
import javax.enterprise.inject.Produces;
import javax.inject.Inject;

import static org.broadinstitute.gpinformatics.infrastructure.deployment.Deployment.TEST;


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

package org.broadinstitute.gpinformatics.mercury.infrastructure.thrift;

import org.broadinstitute.gpinformatics.mercury.infrastructure.deployment.Deployment;
import org.broadinstitute.gpinformatics.mercury.infrastructure.pmbridge.AbstractConfigProducer;

import javax.enterprise.inject.Default;
import javax.enterprise.inject.Produces;
import javax.inject.Inject;

public class ThriftConfigProducer extends AbstractConfigProducer<ThriftConfig> {


    @Inject
    private Deployment deployment;


    @Produces
    @Default
    public ThriftConfig produce() {

        return produce( deployment );
    }


    public static ThriftConfig getConfig( Deployment deployment ) {
        return new ThriftConfigProducer().produce( deployment );
    }

}

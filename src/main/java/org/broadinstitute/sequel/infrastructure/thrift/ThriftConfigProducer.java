package org.broadinstitute.sequel.infrastructure.thrift;

import org.broadinstitute.sequel.infrastructure.deployment.Deployment;

import javax.enterprise.inject.Default;
import javax.enterprise.inject.Produces;
import javax.inject.Inject;

public class ThriftConfigProducer {


    @Inject
    private Deployment deployment;


    @Produces
    @Default
    public ThriftConfig produce() {

        return produce( deployment );
    }


    public static ThriftConfig produce( Deployment deployment ) {

        switch ( deployment ) {
            // not sure if DEV and TEST should use the QA system?
            case DEV:
            case TEST:
            case QA:
                return new ThriftConfig(
                    "seqtest04", 9090
                );

            case PROD:

                return new ThriftConfig(
                    "seqlims", 9090
                );

            default:

                throw new RuntimeException( "Don't know how to make ThriftConfig for deployment " + deployment );
        }

    }
}

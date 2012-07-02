package org.broadinstitute.sequel.infrastructure.bsp;

import org.broadinstitute.sequel.infrastructure.deployment.Deployment;

import javax.enterprise.inject.Produces;
import javax.inject.Inject;

public class BSPConfigProducer {

    @Inject
    private Deployment deployment;


    @Produces
    public BSPConfig produce() {

        return produce( deployment );
    }



    public static BSPConfig produce( Deployment deployment ) {

        switch ( deployment ) {

            case DEV:
            case TEST:

                return new BSPConfig( "seqsystem", "bspbsp",
                        "gapdev2.broadinstitute.org",
                        8080
                        );


            case QA:

                return new BSPConfig( "seqsystem", "bspbsp",
                        "gapqa3.broadinstitute.org",
                        8080
                );


            case PROD:

                return new BSPConfig( "seqsystem", "bspbsp",
                        "bsp.broadinstitute.org",
                        80
                        );


            default:

                throw new RuntimeException( "Asked to make BSPConfig for deployment " + deployment );

        }
    }
}

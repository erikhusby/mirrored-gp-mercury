package org.broadinstitute.sequel.infrastructure.pmbridge;


import org.broadinstitute.sequel.infrastructure.deployment.Deployment;
import org.broadinstitute.sequel.infrastructure.deployment.TestInstance;

import javax.enterprise.inject.Default;
import javax.enterprise.inject.Produces;
import javax.inject.Inject;

import static org.broadinstitute.sequel.infrastructure.deployment.Deployment.TEST;

public class PMBridgeConfigProducer {



    @Inject
    private Deployment deployment;


    // PMBridge has only a DEV and PROD instance

    private static final String DEV_URL = "http://pmbridgedev.broadinstitute.org/PMBridge";

    private static final String PROD_URL = "http://pmbridge.broadinstitute.org/PMBridge";




    @Produces
    @TestInstance
    public PMBridgeConfig testInstance() {
        return produce( TEST );
    }



    @Produces
    @Default
    public PMBridgeConfig produce() {
        return  produce( deployment );
    }



    public static PMBridgeConfig produce(Deployment deployment) {


        switch ( deployment ) {

            case DEV:
            case TEST:
            case QA:

                return new PMBridgeConfig( DEV_URL );

            case PROD:

                return new PMBridgeConfig( PROD_URL );

            default:

                throw new RuntimeException( "Asked to make PMBridgeConnectionParameters for deployment " + deployment);
        }

    }

}

package org.broadinstitute.sequel.infrastructure.squid;


import org.broadinstitute.sequel.infrastructure.deployment.Deployment;
import org.broadinstitute.sequel.infrastructure.deployment.TestInstance;

import javax.enterprise.inject.Default;
import javax.enterprise.inject.Produces;
import javax.inject.Inject;

import static org.broadinstitute.sequel.infrastructure.deployment.Deployment.TEST;


public class SquidConnectionParametersProducer {

    @Inject
    private Deployment deployment;


    @Produces
    @TestInstance
    public SquidConnectionParameters testInstance() {
        return produce( TEST );
    }



    @Produces
    @Default
    public SquidConnectionParameters produce() {
        return produce( deployment );

    }


    /**
     * The use case for this method is the SOAP service impl where direct @Injection of a @Singleton @Startup
     * SquidConfiguration currently does not work.
     * See {@link org.broadinstitute.sequel.boundary.pass.PassSOAPServiceImpl}.
     *
     * @param deployment
     *
     * @return
     */
    public static SquidConnectionParameters produce(Deployment deployment) {

        switch ( deployment ) {

            case DEV :

                return new SquidConnectionParameters(

                    "http://localhost:8080/squid"

                );

            case TEST:

                return new SquidConnectionParameters(

                    "http://prodinfobuild.broadinstitute.org:8020/squid"

                );


            case QA:

                return new SquidConnectionParameters(

                    "http://vsquidrc.broadinstitute.org:8000/squid"

                );


            case PROD:

                return new SquidConnectionParameters(

                    "http://squid-ui.broadinstitute.org:8000/squid"

                );


            default:

                throw new RuntimeException("Asked to make SquidConnectionParameters for deployment " + deployment);

        }


    }


}

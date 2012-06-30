package org.broadinstitute.sequel.infrastructure.squid;


import org.broadinstitute.sequel.infrastructure.deployment.Deployment;
import org.broadinstitute.sequel.infrastructure.deployment.TestInstance;

import javax.enterprise.inject.Default;
import javax.enterprise.inject.Produces;
import javax.inject.Inject;

import static org.broadinstitute.sequel.infrastructure.deployment.Deployment.TEST;


public class SquidConfigProducer {

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


    /**
     * The use case for this method is the SOAP service impl where direct @Injection of a @Singleton @Startup
     * SquidConfiguration currently does not work.
     * See {@link org.broadinstitute.sequel.boundary.pass.PassServiceImpl}.
     *
     * @param deployment
     *
     * @return
     */
    public static SquidConfig produce(Deployment deployment) {

        switch ( deployment ) {

            case DEV :

                return new SquidConfig(

                    "http://localhost:8080/squid"

                );

            case TEST:

                return new SquidConfig(

                    "http://prodinfobuild.broadinstitute.org:8020/squid"

                );


            case QA:

                return new SquidConfig(

                    "http://vsquidrc.broadinstitute.org:8000/squid"

                );


            case PROD:

                return new SquidConfig(

                    "http://squid-ui.broadinstitute.org:8000/squid"

                );


            default:

                throw new RuntimeException("Asked to make SquidConnectionParameters for deployment " + deployment);

        }


    }


}

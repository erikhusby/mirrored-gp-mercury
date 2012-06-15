package org.broadinstitute.sequel.infrastructure.squid;


import org.apache.commons.logging.Log;
import org.broadinstitute.sequel.infrastructure.deployment.*;

import javax.enterprise.inject.Produces;
import javax.inject.Inject;

import static org.broadinstitute.sequel.infrastructure.deployment.Deployment.*;

public class SquidConfigurationProducer {


    @Inject
    private Log log;


    @Inject
    private Deployment deployment;


    @Produces
    @TestInstance
    public SquidConfiguration testInstance() {

        return new SquidConfiguration(
                TEST,
                "http://prodinfobuild.broadinstitute.org:8020/squid");
    }


    @Produces
    @DevInstance
    public SquidConfiguration devInstance() {

        // How do we allow for local overrides of DEV parameters?
        return new SquidConfiguration(
                DEV,
                "http://localhost:8080/squid");
    }


    @Produces
    @QAInstance
    public SquidConfiguration qaInstance() {

        return new SquidConfiguration(
                QA,
                "http://vsquidrc.broadinstitute.org:8000/squid");
    }


    @Produces
    @ProdInstance
    public SquidConfiguration prodInstance() {

        return new SquidConfiguration(
                PROD,
                "http://squid-ui.broadinstitute.org:8000/squid");
    }


    @Produces
    @StubInstance
    public SquidConfiguration stubInstance() {

        return new SquidConfiguration(
                STUBBY,
                null);
    }


    public static SquidConfiguration getTestInstance() {
        return new SquidConfigurationProducer().testInstance();
    }



    @Produces
    public SquidConfiguration produce() {

        switch (deployment) {

            case DEV:
                return devInstance();

            case TEST:
                return testInstance();

            case QA:
                return qaInstance();

            case PROD:
                return prodInstance();

            case STUBBY:
                return stubInstance();

            default:
                throw new RuntimeException("Unrecognized Deployment: " + deployment);

        }

    }



}

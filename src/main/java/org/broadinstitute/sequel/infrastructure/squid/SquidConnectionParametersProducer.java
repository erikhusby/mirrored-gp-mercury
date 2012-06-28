package org.broadinstitute.sequel.infrastructure.squid;


import org.broadinstitute.sequel.infrastructure.deployment.*;

import javax.enterprise.inject.Default;
import javax.enterprise.inject.Produces;
import java.util.HashMap;
import java.util.Map;

import static org.broadinstitute.sequel.infrastructure.deployment.Deployment.*;

public class SquidConnectionParametersProducer
        extends AbstractProducer implements InstanceSpecificProducer<SquidConnectionParameters> {


    private Map<Deployment, SquidConnectionParameters> connectionParametersMap;


    public SquidConnectionParametersProducer() {

        SquidConnectionParameters[] connectionParameterses = new SquidConnectionParameters[] {

                new SquidConnectionParameters(
                        DEV,
                        "http://localhost:8080/squid"
                ),

                new SquidConnectionParameters(
                        TEST,
                        "http://prodinfobuild.broadinstitute.org:8020/squid"
                ),

                new SquidConnectionParameters(
                        QA,
                        "http://vsquidrc.broadinstitute.org:8000/squid"
                ),

                new SquidConnectionParameters(
                        PROD,
                        "http://squid-ui.broadinstitute.org:8000/squid"
                ),

                new SquidConnectionParameters(
                        STUBBY,
                        null
                )

        };

        connectionParametersMap = new HashMap<Deployment, SquidConnectionParameters>();


        for (SquidConnectionParameters connectionParameters : connectionParameterses)
            connectionParametersMap.put(connectionParameters.getDeployment(), connectionParameters);


    }


    @Override
    @Produces
    @DevInstance
    public SquidConnectionParameters devInstance() {
        log.info("explicitly asked for DEV instance");
        return connectionParametersMap.get(DEV);
    }



    @Override
    @Produces
    @TestInstance
    public SquidConnectionParameters testInstance() {
        log.info("explicitly asked for TEST instance");
        return connectionParametersMap.get(TEST);
    }



    @Override
    @Produces
    @QAInstance
    public SquidConnectionParameters qaInstance() {
        log.info("explicitly asked for QA instance");
        return connectionParametersMap.get(QA);
    }



    @Override
    @Produces
    @ProdInstance
    public SquidConnectionParameters prodInstance() {
        log.info("explicitly asked for PROD instance");
        return connectionParametersMap.get(PROD);
    }



    @Override
    @Produces
    @StubInstance
    public SquidConnectionParameters stubInstance() {
        throw new RuntimeException("explicitly asked for STUBBY instance of SquidConfiguration, should not happen!");
    }


    @Override
    @Produces
    @Default
    public SquidConnectionParameters produce() {
        log.info("Returning SquidConfiguration for deployment " + getDeployment());
        return connectionParametersMap.get(getDeployment());

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
        return new SquidConnectionParametersProducer().connectionParametersMap.get(deployment);
    }


}

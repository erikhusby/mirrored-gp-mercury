package org.broadinstitute.sequel.infrastructure.pmbridge;


import org.broadinstitute.sequel.infrastructure.deployment.*;

import javax.enterprise.inject.Default;
import javax.enterprise.inject.Produces;
import java.util.HashMap;
import java.util.Map;

import static org.broadinstitute.sequel.infrastructure.deployment.Deployment.*;

public class PMBridgeConnectionParametersProducer extends AbstractProducer implements InstanceSpecificProducer<PMBridgeConnectionParameters> {


    private Map<Deployment, PMBridgeConnectionParameters> connectionParametersMap;


    // PMBridge has only a DEV and PROD instance

    private static final String DEV_URL = "http://pmbridgedev.broadinstitute.org/PMBridge";

    private static final String PROD_URL = "http://pmbridge.broadinstitute.org/PMBridge";


    public PMBridgeConnectionParametersProducer() {

        PMBridgeConnectionParameters [] connectionParameterses = new PMBridgeConnectionParameters[] {

                new PMBridgeConnectionParameters(
                        DEV,
                        DEV_URL
                ),

                new PMBridgeConnectionParameters(
                        TEST,
                        DEV_URL
                ),

                new PMBridgeConnectionParameters(
                        QA,
                        DEV_URL
                ),

                new PMBridgeConnectionParameters(
                        PROD,
                        PROD_URL
                ),

                new PMBridgeConnectionParameters(
                        STUBBY,
                        null
                )

        };


        connectionParametersMap = new HashMap<Deployment, PMBridgeConnectionParameters>();

        for (PMBridgeConnectionParameters connectionParameters : connectionParameterses)
            connectionParametersMap.put(connectionParameters.getDeployment(), connectionParameters);

    }

    @Override
    @Produces
    @DevInstance
    public PMBridgeConnectionParameters devInstance() {
        return connectionParametersMap.get(DEV);
    }

    @Override
    @Produces
    @TestInstance
    public PMBridgeConnectionParameters testInstance() {
        return connectionParametersMap.get(TEST);
    }

    @Override
    @Produces
    @QAInstance
    public PMBridgeConnectionParameters qaInstance() {
        return connectionParametersMap.get(QA);
    }

    @Override
    @Produces
    @ProdInstance
    public PMBridgeConnectionParameters prodInstance() {
        return connectionParametersMap.get(PROD);
    }

    @Override
    @Produces
    @StubInstance
    public PMBridgeConnectionParameters stubInstance() {
        return connectionParametersMap.get(STUBBY);
    }

    @Override
    @Produces
    @Default
    public PMBridgeConnectionParameters produce() {
        return connectionParametersMap.get(getDeployment());
    }


    public static PMBridgeConnectionParameters produce(Deployment deployment) {
        return new PMBridgeConnectionParametersProducer().connectionParametersMap.get(deployment);
    }

}

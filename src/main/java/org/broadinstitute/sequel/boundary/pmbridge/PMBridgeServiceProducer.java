package org.broadinstitute.sequel.boundary.pmbridge;


import org.apache.commons.logging.Log;
import org.broadinstitute.sequel.infrastructure.deployment.*;

import javax.enterprise.inject.Default;
import javax.enterprise.inject.Produces;
import javax.inject.Inject;

import static org.broadinstitute.sequel.infrastructure.deployment.Deployment.*;

public class PMBridgeServiceProducer implements InstanceSpecificProducer<PMBridgeService> {

    @Inject
    private Log log;

    @Inject
    private Deployment deployment;

    @Inject
    @Impl
    private PMBridgeService impl;

    @Inject
    @Stub
    private PMBridgeService stub;




    @Override
    @Produces
    @DevInstance
    public PMBridgeService devInstance() {
        return new PMBridgeServiceImpl(DEV);
    }

    @Override
    @Produces
    @TestInstance
    public PMBridgeService testInstance() {
        return new PMBridgeServiceImpl(TEST);
    }

    @Override
    @Produces
    @QAInstance
    public PMBridgeService qaInstance() {
        return new PMBridgeServiceImpl(QA);
    }

    @Override
    @Produces
    @ProdInstance
    public PMBridgeService prodInstance() {
        return new PMBridgeServiceImpl(PROD);
    }

    @Override
    @Produces
    @StubInstance
    public PMBridgeService stubInstance() {
        return stub;
    }



    @Override
    @Produces
    @Default
    public PMBridgeService produce() {

        if (deployment == STUBBY)
            return stub;

        return impl;

    }
}

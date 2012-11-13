package org.broadinstitute.gpinformatics.infrastructure.athena;

import org.broadinstitute.gpinformatics.infrastructure.deployment.Deployment;

import javax.enterprise.inject.New;
import javax.inject.Inject;

/**
 * @author Scott Matthews
 *         Date: 10/24/12
 *         Time: 4:46 PM
 */
public class AthenaClientProducer {

    @Inject
    private Deployment deployment;

    public AthenaClientService produce(@New AthenaClientServiceStub stub, @New AthenaClientServiceImpl impl) {

        if(deployment == Deployment.STUBBY)
            return stub;

        return impl;
    }

    public static AthenaClientService stubInstance() {
        return new AthenaClientServiceStub();
    }

}

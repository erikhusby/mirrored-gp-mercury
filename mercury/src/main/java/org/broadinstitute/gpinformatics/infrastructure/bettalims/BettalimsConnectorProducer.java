package org.broadinstitute.gpinformatics.infrastructure.bettalims;

import org.broadinstitute.gpinformatics.infrastructure.deployment.Deployment;

import javax.enterprise.context.SessionScoped;
import javax.enterprise.inject.Default;
import javax.enterprise.inject.New;
import javax.enterprise.inject.Produces;
import javax.inject.Inject;

/**
 * CDI producer for BettalimsConnector
 */
public class BettalimsConnectorProducer {

    @Inject
    private Deployment deployment;

    public static BettalimsConnector stubInstance() {
        return new BettalimsConnectorStub();
    }

    @Produces
    @Default
    @SessionScoped
    public BettalimsConnector produce(@New BettalimsConnectorStub stub, @New BettalimsConnectorImpl impl) {
        if ( deployment == Deployment.STUBBY ) {
            return stub;
        }
        return impl;
    }
}

package org.broadinstitute.gpinformatics.infrastructure.bettalims;

import org.broadinstitute.gpinformatics.infrastructure.deployment.Deployment;

import javax.enterprise.context.RequestScoped;
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

    // Can't call SessionScoped beans from a message driven bean, so use RequestScoped
    @Produces
    @Default
    @RequestScoped
    public BettalimsConnector produce(@New BettalimsConnectorStub stub, @New BettalimsConnectorImpl impl) {
        if ( deployment == Deployment.STUBBY ) {
            return stub;
        }
        return impl;
    }
}

package org.broadinstitute.gpinformatics.infrastructure.squid;

import org.broadinstitute.gpinformatics.infrastructure.bettalims.BettalimsConnector;
import org.broadinstitute.gpinformatics.infrastructure.bettalims.BettalimsConnectorImpl;
import org.broadinstitute.gpinformatics.infrastructure.bettalims.BettalimsConnectorStub;
import org.broadinstitute.gpinformatics.infrastructure.deployment.Deployment;

import javax.enterprise.context.RequestScoped;
import javax.enterprise.inject.Default;
import javax.enterprise.inject.New;
import javax.enterprise.inject.Produces;
import javax.inject.Inject;

/**
 * CDI producer for SquidConnector
 */
public class SquidConnectorProducer {

    @Inject
    private Deployment deployment;

    public static SquidConnector stubInstance() {
        return new SquidConnectorStub();
    }

    // Can't call SessionScoped beans from a message driven bean, so use RequestScoped
    @Produces
    @Default
    @RequestScoped
    public SquidConnector produce(@New SquidConnectorStub stub, @New SquidConnectorImpl impl) {
        if ( deployment == Deployment.STUBBY ) {
            return stub;
        }
        return impl;
    }
}

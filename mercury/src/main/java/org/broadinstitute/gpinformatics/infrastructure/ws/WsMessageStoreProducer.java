package org.broadinstitute.gpinformatics.infrastructure.ws;

import org.broadinstitute.gpinformatics.infrastructure.deployment.Deployment;

import javax.enterprise.context.RequestScoped;
import javax.enterprise.inject.Default;
import javax.enterprise.inject.New;
import javax.enterprise.inject.Produces;
import javax.inject.Inject;

/**
 * CDI Producer
 */
public class WsMessageStoreProducer {

    @Inject
    private Deployment deployment;

    // Can't call SessionScoped beans from a message drive bean, so use RequestScoped
    @Produces
    @Default
    @RequestScoped
    public WsMessageStore produce(@New WsMessageStoreStub stub, @New WsMessageStoreImpl impl) {
        if ( deployment == Deployment.STUBBY ) {
            return stub;
        }
        return impl;
    }
}

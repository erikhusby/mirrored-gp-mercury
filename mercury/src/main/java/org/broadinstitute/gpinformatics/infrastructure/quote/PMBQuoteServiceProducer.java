package org.broadinstitute.gpinformatics.infrastructure.quote;

import org.broadinstitute.gpinformatics.infrastructure.deployment.Deployment;

import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.inject.Default;
import javax.enterprise.inject.New;
import javax.enterprise.inject.Produces;
import javax.inject.Inject;

import static org.broadinstitute.gpinformatics.infrastructure.deployment.Deployment.DEV;
import static org.broadinstitute.gpinformatics.infrastructure.deployment.Deployment.STUBBY;

public class PMBQuoteServiceProducer {

    @Inject
    private Deployment deployment;

    @Produces
    @Default
    @ApplicationScoped
    public PMBQuoteService produce(@New PMBQuoteServiceStub stub, @New PMBQuoteServiceImpl impl) {

        if ( deployment == STUBBY )
            return stub;

        return impl;
    }

    public static PMBQuoteService stubInstance() {
        return new PMBQuoteServiceStub();
    }

    /**
     * Creates a PMBQuoteServiceImpl with plain old new operator for container-free testing,
     * not a managed bean!
     *
     * @return
     */
    public static PMBQuoteService testInstance() {

        QuoteConfig bspConfig = QuoteConfig.produce(DEV);

        return new PMBQuoteServiceImpl( bspConfig );

    }
}

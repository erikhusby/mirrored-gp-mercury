package org.broadinstitute.gpinformatics.infrastructure.quote;


import org.broadinstitute.gpinformatics.infrastructure.deployment.Deployment;

import javax.enterprise.context.SessionScoped;
import javax.enterprise.inject.Default;
import javax.enterprise.inject.New;
import javax.enterprise.inject.Produces;
import javax.inject.Inject;

import static org.broadinstitute.gpinformatics.infrastructure.deployment.Deployment.DEV;


public class QuoteServiceProducer {

    @Inject
    private Deployment deployment;


    public static QuoteService testInstance() {

        QuoteConfig quoteConfig = QuoteConfig.produce(DEV);

        return new QuoteServiceImpl(quoteConfig);
    }


    public static QuoteService stubInstance() {
        return new QuoteServiceStub();
    }


    @Produces
    @Default
    public QuoteService produce(@New QuoteServiceStub stub, @New QuoteServiceImpl impl) {

        if (deployment == Deployment.STUBBY) {
            return stub;
        }
        return impl;
    }
}

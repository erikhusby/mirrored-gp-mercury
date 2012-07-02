package org.broadinstitute.sequel.infrastructure.quote;


import org.broadinstitute.sequel.infrastructure.deployment.Deployment;

import javax.enterprise.context.SessionScoped;
import javax.enterprise.inject.Default;
import javax.enterprise.inject.New;
import javax.enterprise.inject.Produces;
import javax.inject.Inject;

import static org.broadinstitute.sequel.infrastructure.deployment.Deployment.QA;

public class QuoteServiceProducer {

    @Inject
    private Deployment deployment;


    public static QuoteService qaInstance() {

        QuoteConfig quoteConfig = QuoteConfigProducer.produce( QA );

        return new QuoteServiceImpl( quoteConfig );
    }


    @Produces
    @Default
    @SessionScoped
    public QuoteService produce(@New QuoteServiceStub stub, @New QuoteServiceImpl impl) {

        if ( deployment == Deployment.STUBBY )
            return stub;

        return impl;

    }
}

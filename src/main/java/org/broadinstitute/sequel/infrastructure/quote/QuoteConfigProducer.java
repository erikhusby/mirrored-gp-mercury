package org.broadinstitute.sequel.infrastructure.quote;

import org.broadinstitute.sequel.infrastructure.deployment.Deployment;
import org.broadinstitute.sequel.infrastructure.pmbridge.AbstractConfigProducer;

import javax.enterprise.inject.Default;
import javax.enterprise.inject.Produces;
import javax.inject.Inject;

public class QuoteConfigProducer extends AbstractConfigProducer<QuoteConfig> {

    @Inject
    private Deployment deployment;


    @Produces
    @Default
    public QuoteConfig produce() {

        return produce( deployment );

    }


    public static QuoteConfig getConfig( Deployment deployment ) {
        return new QuoteConfigProducer().produce( deployment );
    }
}

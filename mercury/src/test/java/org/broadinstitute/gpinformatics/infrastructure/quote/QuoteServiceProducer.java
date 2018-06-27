package org.broadinstitute.gpinformatics.infrastructure.quote;


import static org.broadinstitute.gpinformatics.infrastructure.deployment.Deployment.DEV;

/**
 * This is NOT a CDI producer!
 * It's used in testing only when an explicit test or stub QuoteService is required
 */
public class QuoteServiceProducer {

    public static QuoteService testInstance() {
        return new QuoteServiceImpl(QuoteConfig.produce(DEV));
    }

    public static QuoteService stubInstance() {
        return new QuoteServiceStub();
    }

}

package org.broadinstitute.sequel.infrastructure.quote;

import org.broadinstitute.sequel.infrastructure.deployment.Deployment;

import javax.enterprise.inject.Default;
import javax.enterprise.inject.Produces;
import javax.inject.Inject;

public class QuoteConfigProducer {

    @Inject
    private Deployment deployment;


    public static QuoteConfig produce( Deployment deployment ) {

        // Rich Nordin's login credentials
        final String RICH_LOGIN    = "rnordin@broadinstitute.org";
        final String RICH_PASSWORD = "Squ1d_us3r";

        switch ( deployment ) {

            case DEV:
                return new QuoteConfig(
                        RICH_LOGIN,
                        RICH_PASSWORD,
                        "http://quoteqa:8080"
                );
            case TEST:
                return new QuoteConfig(
                        RICH_LOGIN,
                        RICH_PASSWORD,
                        "http://quoteqa:8080"
                );
            case QA:

                return new QuoteConfig(
                        RICH_LOGIN,
                        RICH_PASSWORD,
                        "http://quoteqa:8080"
                );


            case PROD:
                throw new RuntimeException("We're not ready for production");

            default:

                throw new RuntimeException("Don't know how to make QuoteConfig for deployment " + deployment);

        }

    }


    @Produces
    @Default
    public QuoteConfig produce() {

        return produce( deployment );

    }
}

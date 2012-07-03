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
            case TEST:
            case QA:

                return new QuoteConfig(
                        RICH_LOGIN,
                        RICH_PASSWORD,
                        "https://broadinstitute.org"
                );


            case PROD:

                return new QuoteConfig(
                        RICH_LOGIN,
                        RICH_PASSWORD,
                        "http://quoteqa:8080"
                );

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

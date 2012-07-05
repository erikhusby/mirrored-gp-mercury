package org.broadinstitute.sequel.infrastructure.jira;

import org.broadinstitute.sequel.infrastructure.deployment.Deployment;

import javax.enterprise.inject.Default;
import javax.enterprise.inject.Produces;
import javax.inject.Inject;


public class JiraConfigProducer {


    @Inject
    private Deployment deployment;


    public static JiraConfig produce( Deployment deployment ) {

        switch ( deployment ) {

            case DEV:
                return new JiraConfig(
                        "vsquid00.broadinstitute.org",
                        8020,
                        "squid",
                        "squid"
                );
            case TEST:
                return new JiraConfig(
                        "vsquid00.broadinstitute.org",
                        8020,
                        "squid",
                        "squid"
                );
            case QA:
                return new JiraConfig(
                        "vsquid00.broadinstitute.org",
                        8020,
                        "squid",
                        "squid"
                );
            case PROD:
                throw new RuntimeException("We're not ready for production");


            default:

                throw new RuntimeException( "Don't know how to make JiraConfig for deployment " + deployment);

        }

    }



    @Produces
    @Default
    public JiraConfig produce() {

        return produce( deployment );

    }
}

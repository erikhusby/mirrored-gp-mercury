package org.broadinstitute.gpinformatics.mercury.infrastructure.jira;

import org.broadinstitute.gpinformatics.mercury.infrastructure.deployment.Deployment;
import org.broadinstitute.gpinformatics.mercury.infrastructure.pmbridge.AbstractConfigProducer;

import javax.enterprise.inject.Default;
import javax.enterprise.inject.Produces;
import javax.inject.Inject;


public class JiraConfigProducer extends AbstractConfigProducer<JiraConfig> {


    @Inject
    private Deployment deployment;


    @Produces
    @Default
    public JiraConfig produce() {

        return produce( deployment );

    }


    public static JiraConfig getConfig( Deployment deployment ) {

        return new JiraConfigProducer().produce( deployment );

    }
}

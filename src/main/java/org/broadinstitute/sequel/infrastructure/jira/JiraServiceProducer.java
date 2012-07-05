package org.broadinstitute.sequel.infrastructure.jira;


import org.broadinstitute.sequel.infrastructure.deployment.Deployment;

import javax.enterprise.context.SessionScoped;
import javax.enterprise.inject.Default;
import javax.enterprise.inject.New;
import javax.enterprise.inject.Produces;
import javax.inject.Inject;

public class JiraServiceProducer {


    @Inject
    private Deployment deployment;


    public static JiraService testInstance() {

        JiraConfig jiraConfig = JiraConfigProducer.produce( Deployment.TEST );

        return new JiraServiceImpl( jiraConfig );

    }


    public static JiraService stubInstance() {

        return new JiraServiceStub();
    }



    @Produces
    @Default
    @SessionScoped
    public JiraService produce(@New JiraServiceStub stub, @New JiraServiceImpl impl) {

        if ( deployment == Deployment.STUBBY )
            return stub;

        return impl;

    }
}

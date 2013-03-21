package org.broadinstitute.gpinformatics.infrastructure.jira;


import org.broadinstitute.gpinformatics.infrastructure.deployment.Deployment;

import javax.enterprise.context.SessionScoped;
import javax.enterprise.inject.Default;
import javax.enterprise.inject.New;
import javax.enterprise.inject.Produces;
import javax.inject.Inject;

public class JiraServiceProducer {

    @Inject
    private Deployment deployment;

    private static JiraService testInstance;

    public static JiraService testInstance() {

        if (testInstance == null) {
            synchronized (JiraService.class) {
                if (testInstance == null) {
                    JiraConfig jiraConfig = JiraConfig.produce(Deployment.TEST);
                    testInstance = new JiraServiceImpl(jiraConfig);
                }
            }
        }

        return testInstance;
    }


    public static JiraService stubInstance() {

        return new JiraServiceStub();
    }


    @Produces
    @Default
    @SessionScoped
    public JiraService produce(@New JiraServiceStub stub, @New JiraServiceImpl impl) {

        if (deployment == Deployment.STUBBY) {
            return stub;
        }

        return impl;
    }
}

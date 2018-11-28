package org.broadinstitute.gpinformatics.infrastructure.jira;

import org.broadinstitute.gpinformatics.infrastructure.deployment.Deployment;

/**
 * <strong>Not a CDI producer!</strong><br/>
 * Creates non-CDI test instances only.
 */
public class JiraServiceTestProducer {

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

}

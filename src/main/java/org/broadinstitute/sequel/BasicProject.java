package org.broadinstitute.sequel;

import org.apache.commons.logging.LogFactory;
import org.apache.commons.logging.Log;

public class BasicProject extends AbstractProject {
    
    private static Log gLog = LogFactory.getLog(BasicProject.class);

    public BasicProject(String projectName,JiraTicket jiraTicket) {
        setJiraTicket(jiraTicket);
        setProjectName(projectName);
        setActive(true);
    }


    
}

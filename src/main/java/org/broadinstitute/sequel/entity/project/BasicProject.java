package org.broadinstitute.sequel.entity.project;

import org.apache.commons.logging.LogFactory;
import org.apache.commons.logging.Log;
import org.broadinstitute.sequel.entity.person.Person;

import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.ManyToOne;

@Entity
public class BasicProject extends Project {
    
    private static Log gLog = LogFactory.getLog(BasicProject.class);

    @ManyToOne(fetch = FetchType.LAZY)
    private Person platformOwner;
    
    public BasicProject(String projectName,JiraTicket jiraTicket) {
        setJiraTicket(jiraTicket);
        setProjectName(projectName);
        setActive(true);
    }

    public BasicProject(Person platformOwner,String projectName,JiraTicket jiraTicket) {
       this(projectName,jiraTicket);
       this.platformOwner = platformOwner;

    }

    protected BasicProject() {
    }

    public Person getPlatformOwner() {
        return platformOwner;
    }
}

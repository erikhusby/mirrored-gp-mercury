package org.broadinstitute.gpinformatics.mercury.entity.workflow;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;

/**
 * A jira transition defined by workflow
 */
@XmlAccessorType(XmlAccessType.FIELD)
public class JiraTransitionType {

    private String project;

    private String statusTransition;

    private String endStatus;

    /* For JAXB */
    public JiraTransitionType() {
    }

    public String getProject ()
    {
        return project;
    }

    public void setProject (String project)
    {
        this.project = project;
    }

    public String getStatusTransition ()
    {
        return statusTransition;
    }

    public void setStatusTransition (String statusTransition)
    {
        this.statusTransition = statusTransition;
    }

    public String getEndStatus() {
        return endStatus;
    }

    public void setEndStatus(String endStatus) {
        this.endStatus = endStatus;
    }
}

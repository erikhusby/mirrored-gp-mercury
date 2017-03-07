package org.broadinstitute.gpinformatics.mercury.entity.workflow;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import java.util.ArrayList;
import java.util.List;

/**
 * A jira transition defined by workflow
 */
@XmlAccessorType(XmlAccessType.FIELD)
public class JiraTransitionType {

    private String project;

    private String statusTransition;

    private List<String> endStatus;

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

    public List<String> getEndStatus() {
        if (endStatus == null) {
            endStatus = new ArrayList<>();
        }
        return endStatus;
    }

    public void setEndStatus(List<String> endStatus) {
        this.endStatus = endStatus;
    }
}

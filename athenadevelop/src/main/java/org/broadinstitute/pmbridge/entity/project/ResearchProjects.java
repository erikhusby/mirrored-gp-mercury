package org.broadinstitute.pmbridge.entity.project;

import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlRootElement;
import java.util.List;

/**
 * Created by IntelliJ IDEA.
 * User: mccrory
 * Date: 5/10/12
 * Time: 1:16 PM
 */
@XmlRootElement(name = "Projects")
public class ResearchProjects {

    @XmlAttribute(name = "projects")
    private List<ResearchProject> projects;

    public ResearchProjects() {
    }

    public ResearchProjects(List<ResearchProject> projects) {
        this.projects = projects;
    }

    public List<ResearchProject> getProjects() {
        return projects;
    }

    public void setProjects(List<ResearchProject> projects) {
        this.projects = projects;
    }



}

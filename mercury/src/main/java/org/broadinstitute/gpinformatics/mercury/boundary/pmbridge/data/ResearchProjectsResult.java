package org.broadinstitute.gpinformatics.mercury.boundary.pmbridge.data;


import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;
import java.util.ArrayList;
import java.util.List;

@XmlRootElement(name = "projects")
public class ResearchProjectsResult {

    private List<ResearchProject> researchProjects = new ArrayList<ResearchProject>();


    @XmlElement(name = "project")
    public List<ResearchProject> getResearchProjects() {
        return researchProjects;
    }


    public void setResearchProjects(List<ResearchProject> researchProjects) {
        this.researchProjects = researchProjects;
    }


}

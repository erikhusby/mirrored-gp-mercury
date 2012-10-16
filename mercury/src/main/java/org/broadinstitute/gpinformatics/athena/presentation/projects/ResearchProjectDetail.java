package org.broadinstitute.gpinformatics.athena.presentation.projects;

import org.apache.commons.lang.StringUtils;
import org.broadinstitute.gpinformatics.athena.control.dao.ResearchProjectDao;
import org.broadinstitute.gpinformatics.athena.entity.person.RoleType;
import org.broadinstitute.gpinformatics.athena.entity.project.ResearchProject;
import org.broadinstitute.gpinformatics.mercury.presentation.AbstractJsfBean;

import javax.enterprise.context.RequestScoped;
import javax.faces.context.FacesContext;
import javax.inject.Inject;
import javax.inject.Named;
import java.util.Arrays;
import java.util.List;

/**
 * This is the UI backing for the research project.
 *
 * @author hrafal
 */
@Named
@RequestScoped
public class ResearchProjectDetail extends AbstractJsfBean {

    private ResearchProject project;

    private String[] selectedPersonnel;

    public void initEmptyProject() {
        project = new ResearchProject();
    }

    public ResearchProject getProject() {
        return project;
    }

    public void setProject(ResearchProject project) {
        this.project = project;
    }

    public String getSponsoredScientists() {
        return StringUtils.join(project.getPeople(RoleType.SCIENTIST), ", ");
    }

    public String getFundingSources() {
        return StringUtils.join(project.getFundingIds(), ", ");
    }

    public String getCohorts() {
        return StringUtils.join(project.getCohortIds(), ", ");
    }

    public List<String> getStatuses() {
        return ResearchProject.Status.getNames();
    }

    public String getIrbNumberString() {
        return StringUtils.join(project.getIrbNumbers(), ", ");
    }

    public String[] getSelectedPersonnel() {
        return selectedPersonnel;
    }

    public void setSelectedPersonnel(String[] selectedPersonnel) {
        this.selectedPersonnel = selectedPersonnel;
    }
}

package org.broadinstitute.gpinformatics.athena.presentation.projects;

import org.broadinstitute.gpinformatics.athena.control.dao.ResearchProjectDao;
import org.broadinstitute.gpinformatics.athena.entity.person.RoleType;
import org.broadinstitute.gpinformatics.athena.entity.project.ResearchProject;
import org.broadinstitute.gpinformatics.mercury.presentation.AbstractJsfBean;

import javax.enterprise.context.RequestScoped;
import javax.inject.Inject;
import javax.inject.Named;
import java.util.Arrays;
import java.util.List;
import java.util.Set;

/**
 * This is the UI backing for the research project.
 *
 * @author hrafal
 */
@Named
@RequestScoped
public class ResearchProjectDetail extends AbstractJsfBean {

    @Inject
    private ResearchProjectDao researchProjectDao;

    private String researchProjectTitle;

    private ResearchProject project;

    private List<String> personnel = Arrays.asList("Person1", "Person2", "Person3");

    private String[] selectedPersonnel;

    public void initEmptyProject() {
        project = new ResearchProject(null, null, null);
    }

    public void loadProject() {
        if (project == null) {
            project = researchProjectDao.findByTitle(researchProjectTitle);
        }
    }

    public String getResearchProjectTitle() {
        return researchProjectTitle;
    }

    public void setResearchProjectTitle(String researchProjectTitle) {
        this.researchProjectTitle = researchProjectTitle;
    }

    public ResearchProject getProject() {
        return project;
    }

    public String getSponsoredScientists() {
        Set<Long> scientistIds = project.getPeople(RoleType.SCIENTIST);
        return "waiting for web service for people";
    }

    public String getFundingSources() {
        Set<String> fundingIds = project.getFundingIds();
        return "waiting for web service for funding";
    }

    public String getCohorts() {
        Set<String> cohortIds = project.getCohortIds();
        return "waiting for web service for cohorts";
    }

    public List<String> getStatuses() {
        return ResearchProject.Status.getNames();
    }

    public List<String> getPersonnel() {
        return personnel;
    }

    public void setPersonnel(List<String> personnel) {
        this.personnel = personnel;
    }

    public String[] getSelectedPersonnel() {
        return selectedPersonnel;
    }

    public void setSelectedPersonnel(String[] selectedPersonnel) {
        this.selectedPersonnel = selectedPersonnel;
    }
}

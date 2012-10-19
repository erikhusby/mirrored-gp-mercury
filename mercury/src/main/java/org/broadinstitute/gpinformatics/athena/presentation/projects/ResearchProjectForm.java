package org.broadinstitute.gpinformatics.athena.presentation.projects;

import org.apache.commons.logging.Log;
import org.broadinstitute.bsp.client.users.BspUser;
import org.broadinstitute.gpinformatics.athena.control.dao.ResearchProjectDao;
import org.broadinstitute.gpinformatics.athena.entity.person.RoleType;
import org.broadinstitute.gpinformatics.athena.entity.project.*;
import org.broadinstitute.gpinformatics.infrastructure.bsp.BSPCohortList;
import org.broadinstitute.gpinformatics.infrastructure.bsp.BSPUserList;
import org.broadinstitute.gpinformatics.infrastructure.quote.Funding;
import org.broadinstitute.gpinformatics.mercury.presentation.AbstractJsfBean;
import org.broadinstitute.gpinformatics.mercury.presentation.UserBean;

import javax.enterprise.context.RequestScoped;
import javax.faces.context.FacesContext;
import javax.inject.Inject;
import javax.inject.Named;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * @author breilly
 */
@Named
@RequestScoped
public class ResearchProjectForm extends AbstractJsfBean {

    @Inject
    private Log log;

    @Inject
    private ResearchProjectDetail detail;

    @Inject
    private ResearchProjectDao researchProjectDao;

    @Inject
    private BSPUserList bspUserList;

    @Inject
    private BSPCohortList cohortList;

    @Inject
    private FacesContext facesContext;

    @Inject
    private UserBean userBean;

    private List<BspUser> projectManagers;

    private List<BspUser> broadPIs;

    private List<BspUser> scientists;

    private List<BspUser> externalCollaborators;

    private List<Funding> fundingSources;

    private List<Cohort> sampleCohorts;

    // TODO: change to a text field to be parsed as comma-separated
    private List<Long> irbs;

    public void initForm() {
        // Only initialize the form on postback. Otherwise, we'll leave the form as the user submitted it.
        if (!facesContext.isPostback()) {

            // Add current user as a PM only if this is a new research project being created
            if (detail.getProject().getResearchProjectId() == null) {
                projectManagers = new ArrayList<BspUser>();
                projectManagers.add(userBean.getBspUser());
            } else {
                projectManagers = makeBspUserList(detail.getProject().getProjectManagers());
                broadPIs = makeBspUserList(detail.getProject().getBroadPIs());
                scientists = makeBspUserList(detail.getProject().getScientists());
                externalCollaborators = makeBspUserList(detail.getProject().getExternalCollaborators());
            }
        }
    }

    private List<BspUser> makeBspUserList(Long[] userIds) {
        List<BspUser> users = new ArrayList<BspUser>();
        for (Long userId : userIds) {
            BspUser user = bspUserList.getById(userId);
            if (user != null) {
                users.add(user);
            }
        }
        return users;
    }

    public String save() {
        ResearchProject project = detail.getProject();
        if (project.getResearchProjectId() == null) {
            return create();
        } else {
            return edit();
        }
    }

    public String create() {
        // TODO: move some of this logic down into boundary, or deeper!

        ResearchProject project = detail.getProject();
        addPeople(project, RoleType.PM, projectManagers);
        addPeople(project, RoleType.BROAD_PI, broadPIs);
        addPeople(project, RoleType.SCIENTIST, scientists);
        addPeople(project, RoleType.EXTERNAL, externalCollaborators);

        if (fundingSources != null) {
            for (Funding fundingSource : fundingSources) {
                project.addFunding(new ResearchProjectFunding(project, fundingSource.getFundingTypeAndName()));
            }
        }

        if (sampleCohorts != null) {
            for (Cohort cohort : sampleCohorts) {
                project.addCohort(new ResearchProjectCohort(project, cohort.getCohortId()));
            }
        }

        if (irbs != null) {
            for (Long irb : irbs) {
                // TODO: use correct IRB type
                project.addIrbNumber(new ResearchProjectIRB(project, ResearchProjectIRB.IrbType.OTHER, irb.toString()));
            }
        }
        project.setCreatedBy(userBean.getBspUser().getUserId());
        project.recordModification(userBean.getBspUser().getUserId());

/* disabled until JIRA issue creation is working
        try {
            project.submit();
        } catch (IOException e) {
            log.error("Error creating JIRA ticket for research project", e);
            addErrorMessage("Error creating JIRA issue", "Unable to create JIRA issue: " + e.getMessage());
            // redisplay create view
            return null;
        }
*/
        researchProjectDao.persist(project);
        addInfoMessage("Research project created.", "Research project \"" + project.getTitle() + "\" has been created.");
        return redirect("list");
    }

    private void addPeople(ResearchProject project, RoleType role, List<BspUser> people) {
        if (people != null) {
            for (BspUser projectManager : people) {
                project.addPerson(role, projectManager.getUserId());
            }
        }
    }

    public String edit() {
        // TODO: try to do away with merge
        // TODO: manage changes in personnel
        List<Long> projectManagerIds = new ArrayList<Long>();
        for (BspUser projectManager : projectManagers) {
            projectManagerIds.add(projectManager.getUserId());
        }
        ResearchProject project = detail.getProject();
        project.setProjectManagers(projectManagerIds.toArray(new Long[projectManagerIds.size()]));
        researchProjectDao.getEntityManager().merge(project);
        return redirect("list");
    }

    public List<Long> completeFundingSource(String query) {
        return generateIds(query, 5);
    }

    public List<Long> completeSampleCohorts(String query) {
        return generateIds(query, 5);
    }

    public List<Long> completeIrbs(String query) {
        return generateIds(query, 5);
    }

    private List<Long> generateIds(String prefix, int num) {
        List<Long> ids = new ArrayList<Long>();
        for (int i = 0; i < 3; i++) {
            ids.add(Long.parseLong(prefix + i));
        }
        return ids;
    }

    public List<BspUser> getProjectManagers() {
        return projectManagers;
    }

    public void setProjectManagers(List<BspUser> projectManagers) {
        this.projectManagers = projectManagers;
    }

    public List<BspUser> getBroadPIs() {
        return broadPIs;
    }

    public void setBroadPIs(List<BspUser> broadPIs) {
        this.broadPIs = broadPIs;
    }

    public List<BspUser> getScientists() {
        return scientists;
    }

    public void setScientists(List<BspUser> scientists) {
        this.scientists = scientists;
    }

    public List<BspUser> getExternalCollaborators() {
        return externalCollaborators;
    }

    public void setExternalCollaborators(List<BspUser> externalCollaborators) {
        this.externalCollaborators = externalCollaborators;
    }

    public List<Funding> getFundingSources() {
        return fundingSources;
    }

    public void setFundingSources(List<Funding> fundingSources) {
        this.fundingSources = fundingSources;
    }

    public List<Cohort> getSampleCohorts() {
        return sampleCohorts;
    }

    public void setSampleCohorts(List<Cohort> sampleCohorts) {
        this.sampleCohorts = sampleCohorts;
    }

    public List<Long> getIrbs() {
        return irbs;
    }

    public void setIrbs(List<Long> irbs) {
        this.irbs = irbs;
    }
}

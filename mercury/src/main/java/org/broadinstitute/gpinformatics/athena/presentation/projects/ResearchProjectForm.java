package org.broadinstitute.gpinformatics.athena.presentation.projects;

import org.apache.commons.logging.Log;
import org.broadinstitute.bsp.client.users.BspUser;
import org.broadinstitute.gpinformatics.athena.control.dao.ResearchProjectDao;
import org.broadinstitute.gpinformatics.athena.entity.person.RoleType;
import org.broadinstitute.gpinformatics.athena.entity.project.*;
import org.broadinstitute.gpinformatics.athena.presentation.converter.IrbConverter;
import org.broadinstitute.gpinformatics.infrastructure.bsp.BSPCohortList;
import org.broadinstitute.gpinformatics.infrastructure.bsp.BSPUserList;
import org.broadinstitute.gpinformatics.infrastructure.jpa.GenericDao;
import org.broadinstitute.gpinformatics.infrastructure.quote.Funding;
import org.broadinstitute.gpinformatics.infrastructure.quote.QuoteFundingList;
import org.broadinstitute.gpinformatics.mercury.presentation.AbstractJsfBean;
import org.broadinstitute.gpinformatics.mercury.presentation.UserBean;

import javax.enterprise.context.RequestScoped;
import javax.faces.context.FacesContext;
import javax.inject.Inject;
import javax.inject.Named;
import java.io.IOException;
import java.text.MessageFormat;
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
    private QuoteFundingList quoteFundingList;

    @Inject
    private BSPCohortList cohortList;

    @Inject
    private IrbConverter irbConverter;

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

    private List<Irb> irbs;

    public void initForm() {
        // Only initialize the form if not a postback. Otherwise, we'll leave the form as the user submitted it.
        if (!facesContext.isPostback()) {

            // Add current user as a PM only if this is a new research project being created
            if (isCreating()) {
                projectManagers = new ArrayList<BspUser>();
                projectManagers.add(userBean.getBspUser());
            } else {
                projectManagers = makeBspUserList(detail.getProject().getProjectManagers());
                broadPIs = makeBspUserList(detail.getProject().getBroadPIs());
                scientists = makeBspUserList(detail.getProject().getScientists());
                externalCollaborators = makeBspUserList(detail.getProject().getExternalCollaborators());

                fundingSources = makeFundingSources(detail.getProject().getFundingIds());
                sampleCohorts = makeCohortList(detail.getProject().getCohortIds());
                irbs = makeIrbs(detail.getProject().getIrbNumbers());
            }
        }
    }

    private List<Irb> makeIrbs(String[] irbNumbers) {
        List<Irb> irbs = new ArrayList<Irb> ();
        for (String irbNumber : irbNumbers) {
            irbs.add((Irb) irbConverter.getAsObject(null, null, irbNumber));
        }

        return irbs;
    }

    private List<Funding> makeFundingSources(String[] fundingIds) {
        List<Funding> fundingList = new ArrayList<Funding> ();
        for (String fundingId : fundingIds) {
            fundingList.add(quoteFundingList.getByFullString(fundingId));
        }

        return fundingList;
    }

    private List<Cohort> makeCohortList(String[] cohortIds) {
        List<Cohort> cohorts = new ArrayList<Cohort> ();
        for (String cohortId : cohortIds) {
            cohorts.add(cohortList.getById(cohortId));
        }

        return cohorts;
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
        if (isCreating()) {
            return create();
        } else {
            return edit();
        }
    }

    public String create() {
        // TODO: move some of this logic down into boundary, or deeper!

        ResearchProject project = detail.getProject();

        addPeople(project);

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
            for (Irb irb : irbs) {
                project.addIrbNumber(new ResearchProjectIRB(project, irb.getIrbType(), irb.getName()));
            }
        }

        project.setCreatedBy(userBean.getBspUser().getUserId());
        project.recordModification(userBean.getBspUser().getUserId());

        try {
            project.submit();
        } catch (IOException e) {
            log.error("Error creating JIRA ticket for research project", e);
            addErrorMessage("Error creating JIRA issue", "Unable to create JIRA issue: " + e.getMessage());
            return null;
        }

        try {
            researchProjectDao.persist(project);
        } catch (Exception e ) {
            String errorMessage = MessageFormat.format("Unknown exception occurred while persisting Product.", null);
            if (GenericDao.IsConstraintViolationException(e)) {
                errorMessage = MessageFormat.format("The project name ''{0}'' is not unique. Project not created", detail.getProject().getTitle());
            }
            addErrorMessage("name", errorMessage, "Name is not unique.");
            return null;
        }

        addInfoMessage("Research project created.", "Research project \"" + project.getTitle() + "\" has been created.");
        return "view";
    }

    private void addPeople(ResearchProject project) {
        project.clearPeople();
        addPeople(project, RoleType.PM, projectManagers);
        addPeople(project, RoleType.BROAD_PI, broadPIs);
        addPeople(project, RoleType.SCIENTIST, scientists);
        addPeople(project, RoleType.EXTERNAL, externalCollaborators);
    }

    private void addPeople(ResearchProject project, RoleType role, List<BspUser> people) {
        if (people != null) {
            for (BspUser projectManager : people) {
                project.addPerson(role, projectManager.getUserId());
            }
        }
    }

    private boolean isCreating() {
        return detail.getProject().getResearchProjectId() == null;
    }

    public String edit() {

        ResearchProject project = detail.getProject();
        addPeople(project);

        project.populateCohorts(sampleCohorts);
        project.populateFunding(fundingSources);
        project.populateIrbs(irbs);

        try {
            researchProjectDao.getEntityManager().merge(project);
        } catch (Exception e ) {
            String errorMessage = MessageFormat.format("Unknown exception occurred while persisting Product.", null);
            if (GenericDao.IsConstraintViolationException(e)) {
                errorMessage = MessageFormat.format("The project name ''{0}'' is not unique. Project not updated.", detail.getProject().getTitle());
            }
            addErrorMessage("name", errorMessage, "Name is not unique");
            return null;
        }

        addInfoMessage("Research project updated.", "Research project \"" + project.getTitle() + "\" has been updated.");
        return "view";
    }

    public List<Irb> completeIrbs(String query) {
        List<Irb> irbsForQuery = new ArrayList<Irb>();
        for (ResearchProjectIRB.IrbType type : ResearchProjectIRB.IrbType.values()) {
            irbsForQuery.add(new Irb(query, type));
        }

        return irbsForQuery;
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

    public List<Irb> getIrbs() {
        return irbs;
    }

    public void setIrbs(List<Irb> irbs) {
        this.irbs = irbs;
    }
}

package org.broadinstitute.gpinformatics.athena.presentation.projects;

import org.apache.commons.logging.Log;
import org.broadinstitute.bsp.client.users.BspUser;
import org.broadinstitute.gpinformatics.athena.boundary.projects.ResearchProjectManager;
import org.broadinstitute.gpinformatics.athena.entity.person.RoleType;
import org.broadinstitute.gpinformatics.athena.entity.project.Cohort;
import org.broadinstitute.gpinformatics.athena.entity.project.Irb;
import org.broadinstitute.gpinformatics.athena.entity.project.ResearchProject;
import org.broadinstitute.gpinformatics.athena.presentation.converter.IrbConverter;
import org.broadinstitute.gpinformatics.infrastructure.bsp.BSPCohortList;
import org.broadinstitute.gpinformatics.infrastructure.bsp.BSPUserList;
import org.broadinstitute.gpinformatics.infrastructure.quote.Funding;
import org.broadinstitute.gpinformatics.infrastructure.quote.QuoteFundingList;
import org.broadinstitute.gpinformatics.mercury.presentation.AbstractJsfBean;
import org.broadinstitute.gpinformatics.mercury.presentation.UserBean;

import javax.enterprise.context.RequestScoped;
import javax.faces.context.FacesContext;
import javax.inject.Inject;
import javax.inject.Named;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

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
    private ResearchProjectManager researchProjectManager;

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
        ResearchProject project = detail.getProject();
        addCollections(project);

        project.setCreatedBy(userBean.getBspUser().getUserId());
        project.recordModification(userBean.getBspUser().getUserId());

        try {
            researchProjectManager.createResearchProject(project);
        } catch (Exception e) {
            addErrorMessage(e.getMessage(), null);
            return null;
        }

        addInfoMessage("The Research Project \"" + project.getTitle() + "\" has been created.", "Research Project");
        return redirect("view");
    }

    private void addCollections(ResearchProject project) {
        project.clearPeople();
        project.addPeople(RoleType.PM, projectManagers);
        project.addPeople(RoleType.BROAD_PI, broadPIs);
        project.addPeople(RoleType.SCIENTIST, scientists);
        project.addPeople(RoleType.EXTERNAL, externalCollaborators);

        project.populateCohorts(sampleCohorts);
        project.populateFunding(fundingSources);
        project.populateIrbs(irbs);
    }

    private boolean isCreating() {
        return detail.getProject().getResearchProjectId() == null;
    }

    public String edit() {
        ResearchProject project = detail.getProject();
        addCollections(project);

        project.recordModification(userBean.getBspUser().getUserId());

        try {
            researchProjectManager.updateResearchProject(project);
        } catch (Exception e) {
            addErrorMessage(e.getMessage(), null);
            return null;
        }

        addInfoMessage("The Research Project \"" + project.getTitle() + "\" has been updated.", "Research Project");
        return redirect("view");
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

    public void changeUsers(List<BspUser> users, String componentId) {
        // The users ARE THE ACTUAL MEMBERS THAT ARE THE APPROPRIATE LIST
        Set<BspUser> uniqueUsers = new HashSet<BspUser>();

        // Since this is called after a single add, at most there is one duplicate
        BspUser duplicate = null;
        for (BspUser user : users) {
            if (uniqueUsers.contains(user)) {
                duplicate = user;
            } else {
                uniqueUsers.add(user);
            }
        }

        users.clear();
        users.addAll(uniqueUsers);

        if (duplicate != null) {
            String message = String.format("%s was already in the list", duplicate.getFirstName() + " " + duplicate.getLastName());
            addInfoMessage(componentId, "Duplicate item removed.", message);
        }
    }

    public void changeFunding(List<Funding> fundingList, String componentId) {
        // The users ARE THE ACTUAL MEMBERS THAT ARE THE APPROPRIATE LIST
        Set<Funding> uniqueFunding = new HashSet<Funding>();

        // Since this is called after a single add, at most there is one duplicate
        Funding duplicate = null;
        for (Funding funding : fundingList) {
            if (uniqueFunding.contains(funding)) {
                duplicate = funding;
            } else {
                uniqueFunding.add(funding);
            }
        }

        fundingList.clear();
        fundingList.addAll(uniqueFunding);

        if (duplicate != null) {
            String message = String.format("%s was already in the list", duplicate.getFundingTypeAndName());
            addInfoMessage(componentId, "Duplicate item removed.", message);
        }
    }

    public void changeCohorts(List<Cohort> cohorts, String componentId) {
        // The users ARE THE ACTUAL MEMBERS THAT ARE THE APPROPRIATE LIST
        Set<Cohort> uniqueCohorts = new HashSet<Cohort>();

        // Since this is called after a single add, at most there is one duplicate
        Cohort duplicate = null;
        for (Cohort cohort : cohorts) {
            if (uniqueCohorts.contains(cohort)) {
                duplicate = cohort;
            } else {
                uniqueCohorts.add(cohort);
            }
        }

        cohorts.clear();
        cohorts.addAll(uniqueCohorts);

        if (duplicate != null) {
            String message = String.format("%s was already in the list", duplicate.getName());
            addInfoMessage(componentId, "Duplicate item removed.", message);
        }
    }

}

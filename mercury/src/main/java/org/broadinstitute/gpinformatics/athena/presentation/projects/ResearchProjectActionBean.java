package org.broadinstitute.gpinformatics.athena.presentation.projects;

import net.sourceforge.stripes.action.*;
import net.sourceforge.stripes.controller.LifecycleStage;
import net.sourceforge.stripes.validation.*;
import org.apache.commons.lang3.StringUtils;
import org.broadinstitute.bsp.client.users.BspUser;
import org.broadinstitute.gpinformatics.athena.boundary.CohortListBean;
import org.broadinstitute.gpinformatics.athena.boundary.FundingListBean;
import org.broadinstitute.gpinformatics.athena.control.dao.ResearchProjectDao;
import org.broadinstitute.gpinformatics.athena.entity.project.Cohort;
import org.broadinstitute.gpinformatics.athena.entity.project.ResearchProject;
import org.broadinstitute.gpinformatics.athena.presentation.links.JiraLink;
import org.broadinstitute.gpinformatics.infrastructure.AutoCompleteToken;
import org.broadinstitute.gpinformatics.infrastructure.bsp.BSPUserList;
import org.broadinstitute.gpinformatics.infrastructure.quote.Funding;
import org.broadinstitute.gpinformatics.mercury.presentation.CoreActionBean;
import org.json.JSONArray;

import javax.enterprise.inject.Default;
import javax.inject.Inject;
import java.io.StringReader;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Map;

/**
 * This class is for ...
 *
 * @author <a href="mailto:dinsmore@broadinstitute.org">Michael Dinsmore</a>
 */
@UrlBinding("/projects/project.action")
public class ResearchProjectActionBean extends CoreActionBean {
    private static final String CREATE = "Create New Project";
    private static final String EDIT = "Edit Project: ";

    public static final String PROJECT_CREATE_PAGE = "/projects/create.jsp";
    public static final String PROJECT_LIST_PAGE = "/projects/list.jsp";
    public static final String PROJECT_VIEW_PAGE = "/projects/view.jsp";

    @Inject
    private ResearchProjectDao researchProjectDao;

    @Inject
    private BSPUserList bspUserList;

    @Inject
    private CohortListBean cohortListBean;

    @Inject
    private FundingListBean fundingList;

    private String businessKey;

    @ValidateNestedProperties({
            @Validate(field = "title", maxlength = 4000, on = {"save"}),
            @Validate(field = "synopsis", maxlength = 4000, on = {"save"}),
            @Validate(field = "comments", maxlength = 2000, on = {"save"})
    })
    private ResearchProject editResearchProject;

    private String submitString;

    @Inject
    private JiraLink jiraLink;

    /*
     * The search query.
     */
    private String q;

    /**
     * All research projects, fetched once and stored per-request (as a result of this bean being @RequestScoped).
     */
    private List<ResearchProject> allResearchProjects;

    /**
     * On demand counts of orders on the project. Map of business key to count value *
     */
    private Map<String, Long> projectOrderCounts;

    /**
     * Fetch the complete list of research projects.
     */
    @After(stages = LifecycleStage.BindingAndValidation, on = {"list"})
    public void listInit() {
        allResearchProjects = researchProjectDao.findAllResearchProjects();
    }

    /**
     * Initialize the project with the passed in key for display in the form
     */
    @Before(stages = LifecycleStage.BindingAndValidation, on = {"view", "edit", "save"})
    public void init() {
        businessKey = getContext().getRequest().getParameter("businessKey");
        if (businessKey != null) {
            editResearchProject = researchProjectDao.findByBusinessKey(businessKey);
        }
    }

    /**
     * Validation of project name.
     *
     * @param errors
     */
    @ValidationMethod(on = "save")
    public void createUniqueNameValidation(ValidationErrors errors) {
        // If the research project has no original title, then it was not fetched from hibernate, so this is a create
        // OR if this was fetched and the title has been changed
        if ((editResearchProject.getOriginalTitle() == null) ||
                (!editResearchProject.getTitle().equalsIgnoreCase(editResearchProject.getOriginalTitle()))) {

            // Check if there is an existing research project and error out if it already exists
            ResearchProject existingProject = researchProjectDao.findByTitle(editResearchProject.getTitle());
            if (existingProject != null) {
                errors.add("title", new SimpleError("A research project already exists with this name."));
            }
        }
    }

    public Map<String, Long> getResearchProjectCounts() {
        if (projectOrderCounts == null) {
            projectOrderCounts = researchProjectDao.getProjectOrderCounts();
        }

        return projectOrderCounts;
    }

    /**
     * Returns a list of all research project statuses.
     *
     * @return list of all research project statuses
     */
    public List<ResearchProject.Status> getAllProjectStatuses() {
        return Arrays.asList(ResearchProject.Status.values());
    }

    @Default
    public Resolution list() {
        return new ForwardResolution(PROJECT_LIST_PAGE);
    }


    public Resolution create() {
        submitString = CREATE;
        return new ForwardResolution(PROJECT_CREATE_PAGE);
    }

    public Resolution edit() {
        submitString = EDIT;
        return new ForwardResolution(PROJECT_CREATE_PAGE);
    }

    public Resolution save() throws Exception {
        researchProjectDao.persist(editResearchProject);
        this.addMessage("The research project '" + editResearchProject.getTitle() + "' has been saved.");
        return new ForwardResolution(PROJECT_VIEW_PAGE);
    }


    public Resolution view() {
        return new ForwardResolution(PROJECT_VIEW_PAGE);
    }

    public String getSumbitString() {
        return submitString;
    }

    public List<ResearchProject> getAllResearchProjects() {
        return allResearchProjects;
    }

    private String getUserListString(Long[] ids) {
        String listString = "";

        if (ids != null) {
            String[] nameList = new String[ids.length];
            int i = 0;
            for (Long id : ids) {
                BspUser user = bspUserList.getById(id);
                nameList[i++] = user.getFirstName() + " " + user.getLastName();
            }

            listString = StringUtils.join(nameList, ", ");
        }

        return listString;
    }

    /**
     * Get a comma separated list of all the project managers for the current project.
     *
     * @return string of the list of project managers
     */
    public String getManagersListString() {
        String listString = "";
        if (editResearchProject != null) {
            listString = getUserListString(editResearchProject.getProjectManagers());
        }
        return listString;
    }

    public String getBroadPIsListString() {
        String listString = "";
        if (editResearchProject != null) {
            listString = getUserListString(editResearchProject.getBroadPIs());
        }
        return listString;
    }

    public String getExternalCollaboratorsListString() {
        String listString = "";
        if (editResearchProject != null) {
            listString = getUserListString(editResearchProject.getExternalCollaborators());
        }
        return listString;
    }

    public String getScientistsListString() {
        String listString = "";
        if (editResearchProject != null) {
            listString = getUserListString(editResearchProject.getScientists());
        }
        return listString;
    }

    public ResearchProject getResearchProject() {
        return editResearchProject;
    }

    /**
     * Get the full name of the creator of the project.
     *
     * @return name of the Creator
     */
    public String getResearchProjectCreatorString() {
        if (editResearchProject == null) {
            return "";
        }
        return bspUserList.getUserFullName(editResearchProject.getCreatedBy());
    }

    public String getCohortsListString() {
        if (editResearchProject == null) {
            return "";
        }

        return cohortListBean.getCohortListString(editResearchProject.getCohortIds());
    }

    public String getFundingSourcesListString() {
        if (editResearchProject == null) {
            return "";
        }
        return fundingList.getFundingListString(editResearchProject.getFundingIds());
    }

    /**
     * Get the fully qualified Jira URL.
     *
     * @return URL string
     */
    public String getJiraUrl() {
        if (jiraLink == null) {
            return "";
        }
        return jiraLink.browseUrl();
    }

    public String getQ() {
        return q;
    }

    public void setQ(String q) {
        this.q = q;
    }

    /**
     * Handles the autocomplete for the jQuery Token plugin.
     *
     * @return
     * @throws Exception
     */
    @HandlesEvent("autocomplete")
    public Resolution autocomplete() throws Exception {
        Collection<ResearchProject> projects = researchProjectDao.searchProjects(getQ());

        JSONArray itemList = new JSONArray();
        for (ResearchProject project : projects) {
            itemList.put(new AutoCompleteToken(project.getBusinessKey(), project.getTitle(), false).getJSONObject());
        }

        return new StreamingResolution("text", new StringReader(itemList.toString()));
    }

    /**
     * Used for AJAX autocomplete (i.e. from create.jsp page).
     *
     * @return JSON list of matching users
     * @throws Exception
     */
    @HandlesEvent("usersAutocomplete")
    public Resolution usersAutocomplete() throws Exception {
        List<BspUser> bspUsers = bspUserList.find(getQ());

        JSONArray itemList = new JSONArray();
        for (BspUser bspUser : bspUsers) {
            String fullName = bspUser.getFirstName() + " " + bspUser.getLastName();
            itemList.put(new AutoCompleteToken(String.valueOf(bspUser.getUserId()), fullName, false).getJSONObject());
        }

        return new StreamingResolution("text", new StringReader(itemList.toString()));
    }

    public String getBroadPICompleteData() throws Exception {
        if (editResearchProject == null) {
            return "";
        }

        JSONArray itemList = new JSONArray();
        for (Long userId : editResearchProject.getBroadPIs()) {
            BspUser bspUser = bspUserList.getById(userId);
            String fullName = bspUser.getFirstName() + " " + bspUser.getLastName();
            itemList.put(new AutoCompleteToken(String.valueOf(bspUser.getUserId()), fullName, false).getJSONObject());
        }

        return itemList.toString();
    }

    public String getExternalCollaboratorCompleteData() throws Exception {
        if (editResearchProject == null) {
            return "";
        }

        JSONArray itemList = new JSONArray();
        for (Long userId : editResearchProject.getExternalCollaborators()) {
            BspUser bspUser = bspUserList.getById(userId);
            String fullName = bspUser.getFirstName() + " " + bspUser.getLastName();
            itemList.put(new AutoCompleteToken(String.valueOf(bspUser.getUserId()), fullName, false).getJSONObject());
        }

        return itemList.toString();
    }

    public String getScientistCompleteData() throws Exception {
        if (editResearchProject == null) {
            return "";
        }

        JSONArray itemList = new JSONArray();
        for (Long userId : editResearchProject.getScientists()) {
            BspUser bspUser = bspUserList.getById(userId);
            String fullName = bspUser.getFirstName() + " " + bspUser.getLastName();
            itemList.put(new AutoCompleteToken(String.valueOf(bspUser.getUserId()), fullName, false).getJSONObject());
        }

        return itemList.toString();
    }

    public String getProjectManagerCompleteData() throws Exception {
        if (editResearchProject == null) {
            return "";
        }

        JSONArray itemList = new JSONArray();
        for (Long userId : editResearchProject.getProjectManagers()) {
            BspUser bspUser = bspUserList.getById(userId);
            String fullName = bspUser.getFirstName() + " " + bspUser.getLastName();
            itemList.put(new AutoCompleteToken(String.valueOf(bspUser.getUserId()), fullName, false).getJSONObject());
        }

        return itemList.toString();
    }

    @Inject
    public String getFundingSourcesCompleteData() throws Exception {
        if (editResearchProject == null) {
            return "";
        }

        JSONArray itemList = new JSONArray();
        for (String fundingId : editResearchProject.getFundingIds()) {
            Funding funding = fundingList.getById(fundingId);
            itemList.put(new AutoCompleteToken(fundingId, funding.getDisplayName(), false).getJSONObject());
        }

        return itemList.toString();
    }

    public String getCohortsCompleteData() throws Exception {
        if (editResearchProject == null) {
            return "";
        }

        JSONArray itemList = new JSONArray();
        for (String cohortId : editResearchProject.getCohortIds()) {
            Cohort cohort = cohortListBean.getCohortById(cohortId);
            itemList.put(new AutoCompleteToken(cohortId, cohort.getDisplayName(), false).getJSONObject());
        }

        return itemList.toString();
    }

    public String getIrbsCompleteData() throws Exception {
        if (editResearchProject == null) {
            return "";
        }

        JSONArray itemList = new JSONArray();
        for (String irbNumber : editResearchProject.getIrbNumbers()) {
            itemList.put(new AutoCompleteToken(irbNumber, irbNumber, false).getJSONObject());
        }

        return itemList.toString();
    }
}

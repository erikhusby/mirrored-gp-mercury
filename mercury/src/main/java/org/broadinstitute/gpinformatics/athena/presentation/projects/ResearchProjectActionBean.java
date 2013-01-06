package org.broadinstitute.gpinformatics.athena.presentation.projects;

import net.sourceforge.stripes.action.*;
import net.sourceforge.stripes.controller.LifecycleStage;
import net.sourceforge.stripes.validation.*;
import org.apache.commons.lang3.StringUtils;
import org.broadinstitute.bsp.client.users.BspUser;
import org.broadinstitute.gpinformatics.athena.boundary.CohortListBean;
import org.broadinstitute.gpinformatics.athena.boundary.FundingListBean;
import org.broadinstitute.gpinformatics.athena.control.dao.ResearchProjectDao;
import org.broadinstitute.gpinformatics.athena.entity.person.RoleType;
import org.broadinstitute.gpinformatics.athena.entity.project.Cohort;
import org.broadinstitute.gpinformatics.athena.entity.project.Irb;
import org.broadinstitute.gpinformatics.athena.entity.project.ResearchProject;
import org.broadinstitute.gpinformatics.athena.entity.project.ResearchProjectIRB;
import org.broadinstitute.gpinformatics.athena.presentation.converter.IrbConverter;
import org.broadinstitute.gpinformatics.athena.presentation.links.JiraLink;
import org.broadinstitute.gpinformatics.infrastructure.AutoCompleteToken;
import org.broadinstitute.gpinformatics.infrastructure.bsp.BSPUserList;
import org.broadinstitute.gpinformatics.infrastructure.quote.Funding;
import org.broadinstitute.gpinformatics.mercury.presentation.CoreActionBean;
import org.json.JSONArray;
import org.json.JSONException;

import javax.enterprise.inject.Default;
import javax.inject.Inject;
import java.io.StringReader;
import java.util.*;

/**
 * This class is for research projects action bean / web page.
 *
 * @author <a href="mailto:dinsmore@broadinstitute.org">Michael Dinsmore</a>
 */
@UrlBinding("/projects/project.action")
public class ResearchProjectActionBean extends CoreActionBean {

    private static final int IRB_NAME_MAX_LENGTH = 250;

    private static final String CURRENT_OBJECT = "Project";
    private static final String CREATE_PROJECT = CoreActionBean.CREATE + CURRENT_OBJECT;
    private static final String EDIT_PROJECT = CoreActionBean.EDIT + CURRENT_OBJECT + ": ";

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

    @Validate(required = true, on={"edit", "view"})
    private String businessKey;

    @ValidateNestedProperties({
            @Validate(field = "title", maxlength = 4000, on = {"save"}),
            @Validate(field = "synopsis", maxlength = 4000, on = {"save"}),
            @Validate(field = "comments", maxlength = 2000, on = {"save"})
    })
    private ResearchProject editResearchProject;

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

    // These are the fields for catching the input tokens
    private String projectManagerList = "";
    private String scientistList = "";
    private String externalCollaboratorList = "";
    private String broadPiList = "";
    private String fundingSourceList = "";
    private String cohortsList = "";
    private String irbList = "";

    /**
     * Fetch the complete list of research projects.
     */
    @After(stages = LifecycleStage.BindingAndValidation, on = {"list"})
    public void listInit() {
        allResearchProjects = researchProjectDao.findAllResearchProjects();
    }

    /**
     * Initialize the project with the passed in key for display in the form.  Need to handle in @Before so we can
     * get the OriginalTitle on the project for validation.
     */
    @Before(stages = LifecycleStage.BindingAndValidation, on = {"view", "edit", "save"})
    public void init() {
        businessKey = getContext().getRequest().getParameter("businessKey");
        if (businessKey != null) {
            editResearchProject = researchProjectDao.findByBusinessKey(businessKey);
        } else {
            editResearchProject = new ResearchProject(getUserBean().getBspUser());
        }
    }

    /**
     * Validation of project name.
     *
     * @param errors The errors object
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
    @HandlesEvent("list")
    public Resolution list() {
        return new ForwardResolution(PROJECT_LIST_PAGE);
    }


    public Resolution create() {
        setSubmitString(CREATE_PROJECT);
        return new ForwardResolution(PROJECT_CREATE_PAGE);
    }

    public Resolution edit() {
        setSubmitString(EDIT_PROJECT);
        return new ForwardResolution(PROJECT_CREATE_PAGE);
    }

    public Resolution save() throws Exception {
        populateTokenListFields();

        researchProjectDao.persist(editResearchProject);
        addMessage("The research project '" + editResearchProject.getTitle() + "' has been saved.");
        return new RedirectResolution(ResearchProjectActionBean.class, "view").addParameter("businessKey", editResearchProject.getBusinessKey());
    }

    private void populateTokenListFields() {
        editResearchProject.clearPeople();
        editResearchProject.addPeople(RoleType.BROAD_PI, getBspUser(broadPiList));
        editResearchProject.addPeople(RoleType.EXTERNAL, getBspUser(externalCollaboratorList));
        editResearchProject.addPeople(RoleType.SCIENTIST, getBspUser(scientistList));
        editResearchProject.addPeople(RoleType.PM, getBspUser(projectManagerList));

        editResearchProject.populateCohorts(getCohorts());
        editResearchProject.populateFunding(getFundingSources());
        editResearchProject.populateIrbs(IrbConverter.getIrbs(irbList));
    }

    private List<BspUser> getBspUser(String userIdList) {
        if (userIdList == null) {
            return Collections.emptyList();
        }

        String[] userIds = userIdList.split(",");
        List<BspUser> bspUsers = new ArrayList<BspUser>();
        for (String userIdString : userIds) {
            long userId = Long.valueOf(userIdString);
            bspUsers.add(bspUserList.getById(userId));
        }

        return bspUsers;
    }

    private List<Funding> getFundingSources() {
        if (fundingSourceList == null) {
            return Collections.emptyList();
        }

        String[] fundingArray = fundingSourceList.split(",");
        List<Funding> fundings = new ArrayList<Funding> ();
        for (String funding : fundingArray) {
            fundings.add(fundingList.getById(funding));
        }

        return fundings;
    }
    private List<Cohort> getCohorts() {
        if (cohortsList == null) {
            return Collections.emptyList();
        }

        String[] cohortArray = cohortsList.split(",");
        List<Cohort> cohorts = new ArrayList<Cohort> ();
        for (String cohort : cohortArray) {
            cohorts.add(cohortListBean.getCohortById(cohort));
        }

        return cohorts;
    }

    public Resolution view() {
        return new ForwardResolution(PROJECT_VIEW_PAGE);
    }

    public List<ResearchProject> getAllResearchProjects() {
        return allResearchProjects;
    }

    /**
     * Get a comma separated list of all the project managers for the current project.
     *
     * @return string of the list of project managers
     */
    public String getManagersListString() {
        String listString = "";
        if (editResearchProject != null) {
            listString = bspUserList.getCsvFullNameList(editResearchProject.getProjectManagers());
        }
        return listString;
    }

    public String getBroadPIsListString() {
        String listString = "";
        if (editResearchProject != null) {
            listString = bspUserList.getCsvFullNameList(editResearchProject.getBroadPIs());
        }
        return listString;
    }

    public String getExternalCollaboratorsListString() {
        String listString = "";
        if (editResearchProject != null) {
            listString = bspUserList.getCsvFullNameList(editResearchProject.getExternalCollaborators());
        }
        return listString;
    }

    public String getScientistsListString() {
        String listString = "";
        if (editResearchProject != null) {
            listString = bspUserList.getCsvFullNameList(editResearchProject.getScientists());
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
     * @return The ajax resolution
     * @throws Exception
     */
    @HandlesEvent("autocomplete")
    public Resolution autocomplete() throws Exception {
        Collection<ResearchProject> projects = researchProjectDao.searchProjects(getQ());

        String completeString = getAutoCompleteJsonString(projects);
        return new StreamingResolution("text", new StringReader(completeString));
    }

    public static String getAutoCompleteJsonString(Collection<ResearchProject> projects) throws JSONException {
        JSONArray itemList = new JSONArray();
        for (ResearchProject project : projects) {
            itemList.put(new AutoCompleteToken(project.getBusinessKey(), project.getTitle(), false).getJSONObject());
        }

        return itemList.toString();
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

    /**
     * Used for AJAX autocomplete (i.e. from create.jsp page).
     *
     * @return JSON list of matching users
     * @throws Exception
     */
    @HandlesEvent("cohortAutocomplete")
    public Resolution cohortAutocomplete() throws Exception {
        List<Cohort> cohorts = cohortListBean.searchActiveCohort(getQ());

        JSONArray itemList = new JSONArray();
        for (Cohort cohort : cohorts) {
            String fullName = cohort.getDisplayName();
            itemList.put(new AutoCompleteToken(cohort.getCohortId(), fullName, false).getJSONObject());
        }

        return new StreamingResolution("text", new StringReader(itemList.toString()));
    }

    /**
     * Used for AJAX autocomplete (i.e. from create.jsp page).
     *
     * @return JSON list of matching users
     * @throws Exception
     */
    @HandlesEvent("fundingAutocomplete")
    public Resolution fundingAutocomplete() throws Exception {
        List<Funding> fundings = fundingList.searchFunding(getQ());

        JSONArray itemList = new JSONArray();
        for (Funding funding : fundings) {
            String fullName = funding.getDisplayName();
            itemList.put(new AutoCompleteToken(String.valueOf(funding.getDisplayName()), fullName, false).getJSONObject());
        }

        return new StreamingResolution("text", new StringReader(itemList.toString()));
    }

    /**
     * Used for AJAX autocomplete (i.e. from create.jsp page).
     *
     * @return JSON list of matching users
     * @throws Exception
     */
    @HandlesEvent("irbAutocomplete")
    public Resolution irbAutocomplete() throws Exception {
        JSONArray itemList = new JSONArray();

        String trimmedQuery = getQ().trim();
        if (!StringUtils.isBlank(trimmedQuery)) {
            for (ResearchProjectIRB.IrbType type : ResearchProjectIRB.IrbType.values()) {
                Irb irb = createIrb(trimmedQuery, type, IRB_NAME_MAX_LENGTH);
                itemList.put(new AutoCompleteToken(irb.getDisplayName(), irb.getDisplayName(), false).getJSONObject());
            }
        }

        return new StreamingResolution("text", new StringReader(itemList.toString()));
    }

    /**
     * This creates a valid IRB object out of the type.
     *
     * @param irbName The name of the irb
     * @param type The irb type
     * @param irbNameMaxLength The maximum length name that can be display
     *
     * @return The irb object
     */
    public Irb createIrb(String irbName, ResearchProjectIRB.IrbType type, int irbNameMaxLength) {

        // If the type + the space-colon-space is longer than max length, then we cannot have a unique name.
        if (type.getDisplayName().length() + 4 > irbNameMaxLength) {
            throw new IllegalArgumentException("IRB type: " + type.getDisplayName() + " is too long to allow for a name");
        }

        // Strip off any long name to the maximum number of characters
        String returnName = irbName;
        int lengthOfFullString = getFullIrbString(irbName, type.getDisplayName()).length();
        if (lengthOfFullString > irbNameMaxLength) {
            returnName = irbName.substring(0, irbName.length() - (lengthOfFullString - irbNameMaxLength));
        }

        return new Irb(returnName, type);
    }

    private String getFullIrbString(String irbName, String irbType) {
        String returnValue = irbName;
        if (irbType != null) {
            returnValue += " : " + irbType;
        }

        return returnValue;
    }

    public String getBroadPICompleteData() throws Exception {
        if (editResearchProject == null) {
            return "";
        }

        return getUserCompleteData(editResearchProject.getBroadPIs());
    }

    private String getUserCompleteData(Long[] userIds) throws JSONException {

        JSONArray itemList = new JSONArray();
        for (Long userId : userIds) {
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

        return getUserCompleteData(editResearchProject.getExternalCollaborators());
    }

    public String getScientistCompleteData() throws Exception {
        if (editResearchProject == null) {
            return "";
        }

        return getUserCompleteData(editResearchProject.getScientists());
    }

    public String getProjectManagerCompleteData() throws Exception {
        if (editResearchProject == null) {
            return "";
        }

        return getUserCompleteData(editResearchProject.getProjectManagers());
    }

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

    public String getIrbList() {
        return irbList;
    }

    public void setIrbList(String irbList) {
        this.irbList = irbList;
    }

    public String getCohortsList() {
        return cohortsList;
    }

    public void setCohortsList(String cohortsList) {
        this.cohortsList = cohortsList;
    }

    public String getFundingSourceList() {
        return fundingSourceList;
    }

    public void setFundingSourceList(String fundingSourceList) {
        this.fundingSourceList = fundingSourceList;
    }

    public String getBroadPiList() {
        return broadPiList;
    }

    public void setBroadPiList(String broadPiList) {
        this.broadPiList = broadPiList;
    }

    public String getExternalCollaboratorList() {
        return externalCollaboratorList;
    }

    public void setExternalCollaboratorList(String externalCollaboratorList) {
        this.externalCollaboratorList = externalCollaboratorList;
    }

    public String getScientistList() {
        return scientistList;
    }

    public void setScientistList(String scientistList) {
        this.scientistList = scientistList;
    }

    public String getProjectManagerList() {
        return projectManagerList;
    }

    public void setProjectManagerList(String projectManagerList) {
        this.projectManagerList = projectManagerList;
    }
}

package org.broadinstitute.gpinformatics.athena.presentation.projects;

import net.sourceforge.stripes.action.*;
import net.sourceforge.stripes.controller.LifecycleStage;
import net.sourceforge.stripes.validation.*;
import org.apache.commons.lang3.StringUtils;
import org.broadinstitute.gpinformatics.athena.boundary.CohortListBean;
import org.broadinstitute.gpinformatics.athena.control.dao.ResearchProjectDao;
import org.broadinstitute.gpinformatics.athena.entity.person.RoleType;
import org.broadinstitute.gpinformatics.athena.entity.project.ResearchProject;
import org.broadinstitute.gpinformatics.athena.presentation.converter.IrbConverter;
import org.broadinstitute.gpinformatics.athena.presentation.links.JiraLink;
import org.broadinstitute.gpinformatics.athena.presentation.links.TableauLink;
import org.broadinstitute.gpinformatics.athena.presentation.tokenimporters.CohortTokenInput;
import org.broadinstitute.gpinformatics.athena.presentation.tokenimporters.FundingTokenInput;
import org.broadinstitute.gpinformatics.athena.presentation.tokenimporters.UserTokenInput;
import org.broadinstitute.gpinformatics.infrastructure.bsp.BSPUserList;
import org.broadinstitute.gpinformatics.infrastructure.common.TokenInput;
import org.broadinstitute.gpinformatics.mercury.presentation.CoreActionBean;
import org.broadinstitute.gpinformatics.mercury.presentation.UserBean;
import org.json.JSONArray;
import org.json.JSONException;

import javax.inject.Inject;
import java.text.MessageFormat;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Map;

/**
 * This class is for research projects action bean / web page.
 *
 * @author <a href="mailto:dinsmore@broadinstitute.org">Michael Dinsmore</a>
 */
@UrlBinding(ResearchProjectActionBean.ACTIONBEAN_URL_BINDING)
public class ResearchProjectActionBean extends CoreActionBean {
    public static final String ACTIONBEAN_URL_BINDING = "/projects/project.action";
    public static final String RESEARCH_PROJECT_PARAMETER = "researchProject";

    private static final int IRB_NAME_MAX_LENGTH = 250;

    private static final String CURRENT_OBJECT = "Research Project";
    public static final String CREATE_PROJECT = CoreActionBean.CREATE + CURRENT_OBJECT;
    public static final String EDIT_PROJECT = CoreActionBean.EDIT + CURRENT_OBJECT;

    public static final String PROJECT_CREATE_PAGE = "/projects/create.jsp";
    public static final String PROJECT_LIST_PAGE = "/projects/list.jsp";
    public static final String PROJECT_VIEW_PAGE = "/projects/view.jsp";

    @Inject
    private TableauLink tableauLink;

    @Inject
    private ResearchProjectDao researchProjectDao;

    @Inject
    private BSPUserList bspUserList;

    @Inject
    private CohortListBean cohortListBean;

    @Validate(required = true, on = {EDIT_ACTION, VIEW_ACTION})
    private String researchProject;

    @ValidateNestedProperties({
            @Validate(field = "title", label = "Project", required = true, maxlength = 4000, on = {SAVE_ACTION}),
            @Validate(field = "synopsis", label = "Synopsis", required = true, maxlength = 4000, on = {SAVE_ACTION}),
            @Validate(field = "irbNotes", label = "IRB Notes", required = false, maxlength = 255, on = {SAVE_ACTION}),
            @Validate(field = "comments", label = "Comments", maxlength = 2000, on = {SAVE_ACTION})
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
    @ValidateNestedProperties({
        @Validate(field = "listOfKeys", label = "Project Managers", required = true, on = {SAVE_ACTION})
    })
    @Inject
    private UserTokenInput projectManagerList;

    @Inject
    private UserTokenInput scientistList;

    @Inject
    private UserTokenInput externalCollaboratorList;

    @Inject
    private UserTokenInput broadPiList;

    @Inject
    private FundingTokenInput fundingSourceList;

    @Inject
    private CohortTokenInput cohortsList;

    @Inject
    private UserBean userBean;

    private String irbList = "";

    /**
     * Fetch the complete list of research projects.
     */
    @After(stages = LifecycleStage.BindingAndValidation, on = {LIST_ACTION})
    public void listInit() {
        allResearchProjects = researchProjectDao.findAllResearchProjects();
    }

    /**
     * Initialize the project with the passed in key for display in the form.  Need to handle in @Before so we can
     * get the OriginalTitle on the project for validation. Create is needed so that token inputs don't have to check
     * for existence.
     */
    @Before(stages = LifecycleStage.BindingAndValidation, on = {VIEW_ACTION, EDIT_ACTION, CREATE_ACTION, SAVE_ACTION})
    public void init() {
        researchProject = getContext().getRequest().getParameter(RESEARCH_PROJECT_PARAMETER);
        if (!StringUtils.isBlank(researchProject)) {
            editResearchProject = researchProjectDao.findByBusinessKey(researchProject);
        } else {
            if (getUserBean().isValidBspUser()) {
                editResearchProject = new ResearchProject(getUserBean().getBspUser());
            } else {
                editResearchProject = new ResearchProject();
            }
        }
    }

    /**
     * Validation of project name.
     *
     * @param errors The errors object
     */
    @ValidationMethod(on = SAVE_ACTION)
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

    @DefaultHandler
    @HandlesEvent(LIST_ACTION)
    public Resolution list() {
        return new ForwardResolution(PROJECT_LIST_PAGE);
    }

    private void validateUser(String validatingFor) {
        if (!userBean.ensureUserValid()) {
            addGlobalValidationError(MessageFormat.format(UserBean.LOGIN_WARNING, validatingFor + " a research project"));
        }
    }

    @HandlesEvent(CREATE_ACTION)
    public Resolution create() {
        validateUser("create");
        setSubmitString(CREATE_PROJECT);
        populateTokenListsFromObjectData();
        return new ForwardResolution(PROJECT_CREATE_PAGE);
    }

    @HandlesEvent(EDIT_ACTION)
    public Resolution edit() {
        validateUser("edit");
        setSubmitString(EDIT_PROJECT);
        populateTokenListsFromObjectData();
        return new ForwardResolution(PROJECT_CREATE_PAGE);
    }

    /**
     * For the prepopulate to work on opening create and edit page, we need to take values from the editOrder. After,
     * the pages have the values passed in.
     */
    private void populateTokenListsFromObjectData() {
        projectManagerList.setup(editResearchProject.getProjectManagers());
        scientistList.setup(editResearchProject.getScientists());
        externalCollaboratorList.setup(editResearchProject.getExternalCollaborators());
        broadPiList.setup(editResearchProject.getBroadPIs());
        fundingSourceList.setup(editResearchProject.getFundingIds());
        cohortsList.setup(editResearchProject.getCohortIds());
    }

    public Resolution save() throws Exception {
        populateTokenListFields();

        // Do the jira jig
        try {
            editResearchProject.submit();
        } catch (Exception ex) {
            addGlobalValidationError("Error creating JIRA ticket for research project");
            return new ForwardResolution(getContext().getSourcePage());
        }

        // Set the modified by and date
        editResearchProject.recordModification(userBean.getBspUser().getUserId());

        try {
            researchProjectDao.persist(editResearchProject);

            // TODO: Force as much work here as possible to catch conditions where we would want to close the JIRA ticket?
            // researchProjectDao.flush();
            addMessage("The research project '" + editResearchProject.getTitle() + "' has been saved.");
        } catch (RuntimeException e) {
            if (researchProject == null) {
                // only reset Jira ticket info when creating a project, new projects don't have business key passed in
                editResearchProject.rollbackPersist();
            }

            // TODO: close already-created JIRA ticket, should we redirect to the page with a Stripes error?
            throw e;
        }

        return new RedirectResolution(ResearchProjectActionBean.class, VIEW_ACTION).addParameter(RESEARCH_PROJECT_PARAMETER, editResearchProject.getBusinessKey());
    }

    private void populateTokenListFields() {
        editResearchProject.clearPeople();
        editResearchProject.addPeople(RoleType.BROAD_PI, broadPiList.getTokenObjects());
        editResearchProject.addPeople(RoleType.EXTERNAL, externalCollaboratorList.getTokenObjects());
        editResearchProject.addPeople(RoleType.SCIENTIST, scientistList.getTokenObjects());
        editResearchProject.addPeople(RoleType.PM, projectManagerList.getTokenObjects());

        editResearchProject.populateCohorts(cohortsList.getTokenObjects());
        editResearchProject.populateFunding(fundingSourceList.getTokenObjects());
        editResearchProject.populateIrbs(IrbConverter.getIrbs(irbList));
    }

    @HandlesEvent("view")
    public Resolution view() {
        return new ForwardResolution(PROJECT_VIEW_PAGE);
    }

    public List<ResearchProject> getAllResearchProjects() {
        return allResearchProjects;
    }

    /**
     * @return The string parameter name of the business key.
     */
    public String getResearchProject() {
        return researchProject;
    }

    public void setResearchProject(String researchProject) {
        this.researchProject = researchProject;
    }

    public ResearchProject getEditResearchProject() {
        return editResearchProject;
    }

    public String getCohortsListString() {
        // FIXME: fix to use function calls in JSP.
        return cohortListBean.getCohortListString(editResearchProject.getCohortIds());
    }

    public String getFundingSourcesListString() {
        String[] fundingIds = editResearchProject.getFundingIds();
        if (fundingIds == null) {
            return "";
        }

        return StringUtils.join(fundingIds, ", ");
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

    public static String getAutoCompleteJsonString(Collection<ResearchProject> projects) throws JSONException {
        JSONArray itemList = new JSONArray();
        for (ResearchProject project : projects) {
            itemList.put(TokenInput.getJSONObject(project.getBusinessKey(), project.getTitle(), false));
        }

        return itemList.toString();
    }

    // Autocomplete events for streaming in the appropriate data. Using project manager list (token input) but can use any one for this
    @HandlesEvent("usersAutocomplete")
    public Resolution usersAutocomplete() throws Exception {
        return createTextResolution(projectManagerList.getJsonString(getQ()));
    }

    @HandlesEvent("cohortAutocomplete")
    public Resolution cohortAutocomplete() throws Exception {
        return createTextResolution(cohortsList.getJsonString(getQ()));
    }

    @HandlesEvent("fundingAutocomplete")
    public Resolution fundingAutocomplete() throws Exception {
        return createTextResolution(fundingSourceList.getJsonString(getQ()));
    }

    @HandlesEvent("irbAutocomplete")
    public Resolution irbAutocomplete() throws Exception {
        return createTextResolution(IrbConverter.getJsonString(getQ()));
    }

    // Complete Data getters are for the prepopulates on the create.jsp

    public String getIrbsCompleteData() throws Exception {
        return IrbConverter.getIrbCompleteData(editResearchProject.getIrbNumbers());
    }

    public String getIrbList() {
        return irbList;
    }

    public void setIrbList(String irbList) {
        this.irbList = irbList;
    }

    public CohortTokenInput getCohortsList() {
        return cohortsList;
    }

    public void setCohortsList(CohortTokenInput cohortsList) {
        this.cohortsList = cohortsList;
    }

    public FundingTokenInput getFundingSourceList() {
        return fundingSourceList;
    }

    public void setFundingSourceList(FundingTokenInput fundingSourceList) {
        this.fundingSourceList = fundingSourceList;
    }

    public UserTokenInput getBroadPiList() {
        return broadPiList;
    }

    public void setBroadPiList(UserTokenInput broadPiList) {
        this.broadPiList = broadPiList;
    }

    public UserTokenInput getExternalCollaboratorList() {
        return externalCollaboratorList;
    }

    public void setExternalCollaboratorList(UserTokenInput externalCollaboratorList) {
        this.externalCollaboratorList = externalCollaboratorList;
    }

    public UserTokenInput getScientistList() {
        return scientistList;
    }

    public void setScientistList(UserTokenInput scientistList) {
        this.scientistList = scientistList;
    }

    public UserTokenInput getProjectManagerList() {
        return projectManagerList;
    }

    public void setProjectManagerList(UserTokenInput projectManagerList) {
        this.projectManagerList = projectManagerList;
    }

    public String getQ() {
        return q;
    }

    public void setQ(String q) {
        this.q = q;
    }

    public String getTableauLink() {
        return tableauLink.passReportUrl(editResearchProject.getTitle());
    }

    /**
     * @return true if Save is a valid operation.
     */
    public boolean getCanSave() {
        // User must be logged into JIRA to create or edit a Research Project.
        return userBean.isValidUser();
    }

}

package org.broadinstitute.gpinformatics.athena.presentation.projects;

import net.sourceforge.stripes.action.*;
import net.sourceforge.stripes.controller.LifecycleStage;
import net.sourceforge.stripes.validation.*;
import org.apache.commons.lang3.StringUtils;
import org.broadinstitute.gpinformatics.athena.boundary.CohortListBean;
import org.broadinstitute.gpinformatics.athena.boundary.FundingListBean;
import org.broadinstitute.gpinformatics.athena.control.dao.ResearchProjectDao;
import org.broadinstitute.gpinformatics.athena.entity.person.RoleType;
import org.broadinstitute.gpinformatics.athena.entity.project.ResearchProject;
import org.broadinstitute.gpinformatics.athena.presentation.converter.IrbConverter;
import org.broadinstitute.gpinformatics.athena.presentation.links.JiraLink;
import org.broadinstitute.gpinformatics.athena.presentation.links.TableauLink;
import org.broadinstitute.gpinformatics.athena.presentation.tokenimporters.CohortTokenInput;
import org.broadinstitute.gpinformatics.athena.presentation.tokenimporters.FundingTokenInput;
import org.broadinstitute.gpinformatics.athena.presentation.tokenimporters.UserTokenInput;
import org.broadinstitute.gpinformatics.infrastructure.AutoCompleteToken;
import org.broadinstitute.gpinformatics.infrastructure.bsp.BSPUserList;
import org.broadinstitute.gpinformatics.mercury.presentation.CoreActionBean;
import org.json.JSONArray;
import org.json.JSONException;

import javax.enterprise.inject.Default;
import javax.inject.Inject;
import java.io.StringReader;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Map;

/**
 * This class is for research projects action bean / web page.
 *
 * @author <a href="mailto:dinsmore@broadinstitute.org">Michael Dinsmore</a>
 */
@UrlBinding("/projects/project.action")
public class ResearchProjectActionBean extends CoreActionBean {

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

    @Inject
    private FundingListBean fundingList;

    @Validate(required = true, on={EDIT_ACTION, VIEW_ACTION})
    private String researchProject;

    @ValidateNestedProperties({
            @Validate(field = "title", maxlength = 4000, on = {SAVE_ACTION}),
            @Validate(field = "synopsis", maxlength = 4000, on = {SAVE_ACTION}),
            @Validate(field = "comments", maxlength = 2000, on = {SAVE_ACTION})
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
        researchProject = getContext().getRequest().getParameter("researchProject");
        if (!StringUtils.isBlank(researchProject)) {
            editResearchProject = researchProjectDao.findByBusinessKey(researchProject);
        } else {
            editResearchProject = new ResearchProject(getUserBean().getBspUser());
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

    @Default
    @HandlesEvent(LIST_ACTION)
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

        // Do the jira jig
        try {
            editResearchProject.submit();
        } catch (Exception ex) {
            addGlobalValidationError("Error creating JIRA ticket for research project");
            return new ForwardResolution(getContext().getSourcePage());
        }

        researchProjectDao.persist(editResearchProject);
        addMessage("The research project '" + editResearchProject.getTitle() + "' has been saved.");
        return new RedirectResolution(ResearchProjectActionBean.class, VIEW_ACTION).addParameter("researchProject", editResearchProject.getBusinessKey());
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

    public Resolution view() {
        return new ForwardResolution(PROJECT_VIEW_PAGE);
    }

    public List<ResearchProject> getAllResearchProjects() {
        return allResearchProjects;
    }

    /**
     * The string paramater name of the business key.
     *
     * @return
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
        // FIXME: fix to use function calls in JSP.
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

    // Autocomplete events for streaming in the appropriate data
    @HandlesEvent("usersAutocomplete")
    public Resolution usersAutocomplete() throws Exception {
        return new StreamingResolution("text", new StringReader(UserTokenInput.getJsonString(bspUserList, getQ())));
    }

    @HandlesEvent("cohortAutocomplete")
    public Resolution cohortAutocomplete() throws Exception {
        return new StreamingResolution("text", new StringReader(CohortTokenInput.getJsonString(cohortListBean, getQ())));
    }

    @HandlesEvent("fundingAutocomplete")
    public Resolution fundingAutocomplete() throws Exception {
        return new StreamingResolution("text", new StringReader(FundingTokenInput.getJsonString(fundingList, getQ())));
    }

    @HandlesEvent("irbAutocomplete")
    public Resolution irbAutocomplete() throws Exception {
        return new StreamingResolution("text", new StringReader(IrbConverter.getJsonString(getQ())));
    }

    // Complete Data getters are for the prepoulates on the create.jsp
    public String getBroadPICompleteData() throws Exception {
        return UserTokenInput.getUserCompleteData(bspUserList, editResearchProject.getBroadPIs());
    }

    public String getExternalCollaboratorCompleteData() throws Exception {
        return UserTokenInput.getUserCompleteData(bspUserList, editResearchProject.getExternalCollaborators());
    }

    public String getScientistCompleteData() throws Exception {
        return UserTokenInput.getUserCompleteData(bspUserList, editResearchProject.getScientists());
    }

    public String getProjectManagerCompleteData() throws Exception {
        return UserTokenInput.getUserCompleteData(bspUserList, editResearchProject.getProjectManagers());
    }

    public String getFundingSourcesCompleteData() throws Exception {
        return FundingTokenInput.getFundingCompleteData(fundingList, editResearchProject.getFundingIds());
    }

    public String getCohortsCompleteData() throws Exception {
        return CohortTokenInput.getCohortCompleteData(cohortListBean, editResearchProject.getCohortIds());
    }

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
}

package org.broadinstitute.gpinformatics.athena.presentation.projects;

import net.sourceforge.stripes.action.After;
import net.sourceforge.stripes.action.Before;
import net.sourceforge.stripes.action.DefaultHandler;
import net.sourceforge.stripes.action.ForwardResolution;
import net.sourceforge.stripes.action.HandlesEvent;
import net.sourceforge.stripes.action.RedirectResolution;
import net.sourceforge.stripes.action.Resolution;
import net.sourceforge.stripes.action.UrlBinding;
import net.sourceforge.stripes.controller.LifecycleStage;
import net.sourceforge.stripes.validation.SimpleError;
import net.sourceforge.stripes.validation.Validate;
import net.sourceforge.stripes.validation.ValidateNestedProperties;
import net.sourceforge.stripes.validation.ValidationErrors;
import net.sourceforge.stripes.validation.ValidationMethod;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.broadinstitute.gpinformatics.athena.boundary.orders.CompletionStatusFetcher;
import org.broadinstitute.gpinformatics.athena.boundary.projects.RegulatoryInfoEjb;
import org.broadinstitute.gpinformatics.athena.boundary.projects.ResearchProjectEjb;
import org.broadinstitute.gpinformatics.athena.control.dao.orders.ProductOrderDao;
import org.broadinstitute.gpinformatics.athena.control.dao.projects.RegulatoryInfoDao;
import org.broadinstitute.gpinformatics.athena.control.dao.projects.ResearchProjectDao;
import org.broadinstitute.gpinformatics.athena.entity.orders.ProductOrder;
import org.broadinstitute.gpinformatics.athena.entity.person.RoleType;
import org.broadinstitute.gpinformatics.athena.entity.project.RegulatoryInfo;
import org.broadinstitute.gpinformatics.athena.entity.project.ResearchProject;
import org.broadinstitute.gpinformatics.athena.presentation.DisplayableItem;
import org.broadinstitute.gpinformatics.athena.presentation.converter.IrbConverter;
import org.broadinstitute.gpinformatics.athena.presentation.tokenimporters.CohortTokenInput;
import org.broadinstitute.gpinformatics.athena.presentation.tokenimporters.FundingTokenInput;
import org.broadinstitute.gpinformatics.athena.presentation.tokenimporters.ProjectTokenInput;
import org.broadinstitute.gpinformatics.athena.presentation.tokenimporters.UserTokenInput;
import org.broadinstitute.gpinformatics.infrastructure.bsp.BSPCohortList;
import org.broadinstitute.gpinformatics.infrastructure.bsp.BSPUserList;
import org.broadinstitute.gpinformatics.infrastructure.common.TokenInput;
import org.broadinstitute.gpinformatics.infrastructure.mercury.MercuryClientService;
import org.broadinstitute.gpinformatics.mercury.presentation.CoreActionBean;
import org.broadinstitute.gpinformatics.mercury.presentation.UserBean;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import javax.inject.Inject;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;

/**
 * This class is for research projects action bean / web page.
 */
@UrlBinding(ResearchProjectActionBean.ACTIONBEAN_URL_BINDING)
public class ResearchProjectActionBean extends CoreActionBean {
    private static Log logger = LogFactory.getLog(ResearchProjectActionBean.class);

    public static final String ACTIONBEAN_URL_BINDING = "/projects/project.action";
    public static final String RESEARCH_PROJECT_PARAMETER = "researchProject";

    private static final String PROJECT = "Research Project";
    public static final String CREATE_PROJECT = CoreActionBean.CREATE + PROJECT;
    public static final String EDIT_PROJECT = CoreActionBean.EDIT + PROJECT;

    public static final String ADD_REGULATORY_INFO_TO_RESEARCH_PROJECT_ACTION = "addRegulatoryInfoToResearchProject";
    public static final String ADD_NEW_REGULATORY_INFO = "addNewRegulatoryInfo";
    public static final String REMOVE_REGULATORY_INFO_ACTION = "removeRegulatoryInfo";
    public static final String EDIT_REGULATORY_INFO_ACTION = "editRegulatoryInfo";

    public static final String PROJECT_CREATE_PAGE = "/projects/create.jsp";
    public static final String PROJECT_LIST_PAGE = "/projects/list.jsp";
    public static final String PROJECT_VIEW_PAGE = "/projects/view.jsp";

    // Reference sequence that will be used for Exome projects.
    private static final String DEFAULT_REFERENCE_SEQUENCE = "Homo_sapiens_assembly19|1";

    @Inject
    private MercuryClientService mercuryClientService;

    @Inject
    private ResearchProjectDao researchProjectDao;

    @Inject
    private BSPUserList bspUserList;

    @Inject
    private BSPCohortList cohortList;

    @Inject
    private ProjectTokenInput projectTokenInput;

    @Inject
    private RegulatoryInfoDao regulatoryInfoDao;

    @Inject
    private RegulatoryInfoEjb regulatoryInfoEjb;

    @Validate(required = true, on = {EDIT_ACTION, VIEW_ACTION})
    private String researchProject;

    @ValidateNestedProperties({
            @Validate(field = "title", label = "Project", required = true, maxlength = 4000, on = {SAVE_ACTION}),
            @Validate(field = "synopsis", label = "Synopsis", required = true, maxlength = 4000, on = {SAVE_ACTION}),
            @Validate(field = "irbNotes", label = "IRB Notes", required = false, maxlength = 255, on = {SAVE_ACTION}),
            @Validate(field = "comments", label = "Comments", maxlength = 2000, on = {SAVE_ACTION})
    })
    private ResearchProject editResearchProject;

    /*
     * The search query.
     */
    private String q;

    private Long regulatoryInfoId;

    private String regulatoryInfoIdentifier;

    private RegulatoryInfo.Type regulatoryInfoType;

    private String regulatoryInfoAlias;

    /**
     * All research projects, fetched once and stored per-request (as a result of this bean being @RequestScoped).
     */
    private List<ResearchProject> allResearchProjects;

    /**
     * On demand counts of orders on the project. Map of business key to count value.
     */
    private Map<String, Long> projectOrderCounts;

    @ValidateNestedProperties(
            @Validate(field = "listOfKeys", label = "Project Managers", required = true, on = {SAVE_ACTION})
    )
    @Inject
    private UserTokenInput projectManagerList;

    @Inject
    private UserTokenInput scientistList;

    @Inject
    private UserTokenInput externalCollaboratorList;

    @Inject
    private UserTokenInput broadPiList;

    @Inject
    private UserTokenInput otherUserList;

    @Inject
    private FundingTokenInput fundingSourceList;

    @Inject
    private CohortTokenInput cohortsList;

    @Inject
    private ProductOrderDao productOrderDao;

    @Inject
    ResearchProjectEjb researchProjectEjb;

    private String irbList = "";

    private CompletionStatusFetcher progressFetcher = new CompletionStatusFetcher();

    public ResearchProjectActionBean() {
        super(CREATE_PROJECT, EDIT_PROJECT, RESEARCH_PROJECT_PARAMETER);
    }

    /**
     * Fetch the complete list of research projects.
     */
    @After(stages = LifecycleStage.BindingAndValidation, on = {LIST_ACTION})
    public void listInit() {
        allResearchProjects = researchProjectDao.findAllResearchProjects();
        Collections.sort(allResearchProjects, ResearchProject.BY_DATE);
    }

    /**
     * Initialize the project with the passed in key for display in the form.  Need to handle in @Before so we can
     * get the OriginalTitle on the project for validation. Create is needed so that token inputs don't have to check
     * for existence.
     */
    @Before(stages = LifecycleStage.BindingAndValidation,
            on = {VIEW_ACTION, EDIT_ACTION, CREATE_ACTION, SAVE_ACTION, ADD_REGULATORY_INFO_TO_RESEARCH_PROJECT_ACTION,
                    ADD_NEW_REGULATORY_INFO, REMOVE_REGULATORY_INFO_ACTION, EDIT_REGULATORY_INFO_ACTION})
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
        if (StringUtils.isBlank(editResearchProject.getReferenceSequenceKey())) {
            editResearchProject.setReferenceSequenceKey(DEFAULT_REFERENCE_SEQUENCE);
        }

        // Get the totals for the order
        Collection<Long> productOrderIds = new ArrayList<>();
        for (ProductOrder order : editResearchProject.getProductOrders()) {
            productOrderIds.add(order.getProductOrderId());
        }

        progressFetcher.loadProgress(productOrderDao, productOrderIds);
    }

    /**
     * Validation of project name.
     *
     * @param errors The errors object
     */
    @ValidationMethod(on = SAVE_ACTION)
    public void createUniqueNameValidation(ValidationErrors errors) {
        // If the research project has no original title, then it was not fetched from hibernate, so this is a create
        // OR if this was fetched and the title has been changed.
        if ((editResearchProject.getOriginalTitle() == null) ||
                (!editResearchProject.getTitle().equalsIgnoreCase(editResearchProject.getOriginalTitle()))) {

            // Check if there is an existing research project and error out if it already exists.
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
        otherUserList.setup(editResearchProject.getOther());
        fundingSourceList.setup(editResearchProject.getFundingIds());
        cohortsList.setup(editResearchProject.getCohortIds());
        // The parent research project doesn't need to be defined, so only pre-populate if it's present.
        if (editResearchProject.getParentResearchProject() != null) {
            projectTokenInput.setup(editResearchProject.getParentResearchProject().getBusinessKey());
        }
    }

    public Resolution save() throws Exception {
        populateTokenListFields();
        String createOrUpdate = "creating";
        if (editResearchProject.isSavedInJira()) {
            createOrUpdate = "updating";
        }
        try {
            researchProjectEjb.submitToJira(editResearchProject);
        } catch (Exception ex) {
            String errorMessage = "Error " + createOrUpdate + " JIRA ticket for research project: " + ex.getMessage();
            logger.error(errorMessage, ex);
            addGlobalValidationError(errorMessage);
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
        editResearchProject.addPeople(RoleType.OTHER, otherUserList.getTokenObjects());

        editResearchProject.populateCohorts(cohortsList.getTokenObjects());
        editResearchProject.populateFunding(fundingSourceList.getTokenObjects());
        editResearchProject.populateIrbs(IrbConverter.getIrbs(irbList));

        ResearchProject tokenProject = projectTokenInput.getTokenObject();
        editResearchProject.setParentResearchProject(tokenProject != null ? researchProjectDao.findByBusinessKey(tokenProject.getBusinessKey()) : null);
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
        return cohortList.getCohortListString(editResearchProject.getCohortIds());
    }

    public String getFundingSourcesListString() {
        String[] fundingIds = editResearchProject.getFundingIds();
        if (fundingIds == null) {
            return "";
        }

        return StringUtils.join(fundingIds, ", ");
    }

    public static String getAutoCompleteJsonString(Collection<ResearchProject> projects) throws JSONException {
        JSONArray itemList = new JSONArray();
        for (ResearchProject project : projects) {
            itemList.put(TokenInput.getJSONObject(project.getBusinessKey(), project.getTitle()));
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

    @HandlesEvent("projectAutocomplete")
    public Resolution projectAutocomplete() throws Exception {
        return createTextResolution(projectTokenInput.getJsonString(getQ()));
    }

    @HandlesEvent("projectHierarchyAwareAutocomplete")
    public Resolution projectHierarchyAwareAutocomplete() throws Exception {
        researchProject = getContext().getRequest().getParameter(RESEARCH_PROJECT_PARAMETER);

        if (StringUtils.isBlank(researchProject)) {
            // Calling method when no project parameter is supplied, so just return full list.
            return projectAutocomplete();
        }

        editResearchProject = researchProjectDao.findByBusinessKey(researchProject);
        Collection<ResearchProject> childResearchProjects = editResearchProject.getAllChildren();
        childResearchProjects.add(editResearchProject);
        return createTextResolution(projectTokenInput.getJsonString(getQ(), childResearchProjects));
    }

    /**
     * Handles an AJAX action event to search for regulatory information, case-insensitively, from the value in this.q.
     * The JSONObjects in the returned array will have the following properties:
     *
     * <dl>
     *     <dt>id</dt>
     *     <dd>the RegulatoryInfo's primary key</dd>
     *     <dt>identifier</dt>
     *     <dd>the externally assigned identifier, such as IRB Protocol #</dd>
     *     <dt>type</dt>
     *     <dd>the type of regulatory information, such as IRB Protocol</dd>
     *     <dt>alias</dt>
     *     <dd>the name given to this information for the benefit of Mercury users</dd>
     * </dl>
     *
     * @return the regulatory information search results
     * @throws Exception
     */
    @HandlesEvent("regulatoryInfoQuery")
    public Resolution queryRegulatoryInfo() throws JSONException {
        List<RegulatoryInfo> infos = regulatoryInfoDao.findByIdentifier(q);
        JSONArray results = new JSONArray();
        for (RegulatoryInfo info : infos) {
            results.put(regulatoryInfoToJSONObject(info));
        }
        return createTextResolution(results.toString());
    }

    private JSONObject regulatoryInfoToJSONObject(RegulatoryInfo regulatoryInfo) throws JSONException {
        JSONObject object = new JSONObject();
        object.put("id", regulatoryInfo.getRegulatoryInfoId());
        object.put("identifier", regulatoryInfo.getIdentifier());
        object.put("type", regulatoryInfo.getType().getName());
        object.put("alias", regulatoryInfo.getName());
        return object;
    }

    /**
     * Associates regulatory information with a research project. The RegulatoryInformation is looked up by the value in
     * this.regulatoryInfoId.
     *
     * @return a redirect to the research project view page
     */
    @HandlesEvent(ADD_REGULATORY_INFO_TO_RESEARCH_PROJECT_ACTION)
    public Resolution addRegulatoryInfoToResearchProject() {
        regulatoryInfoEjb.addRegulatoryInfoToResearchProject(regulatoryInfoId, editResearchProject);
        return new RedirectResolution(ResearchProjectActionBean.class, VIEW_ACTION)
                .addParameter(RESEARCH_PROJECT_PARAMETER, editResearchProject.getBusinessKey());
    }

    /**
     * Creates a new regulatory information record and adds it to the research project currently being viewed.
     *
     * @return a redirect to the research project view page
     */
    @HandlesEvent(ADD_NEW_REGULATORY_INFO)
    public Resolution addNewRegulatoryInfo() {
        RegulatoryInfo regulatoryInfo = regulatoryInfoEjb
                .createRegulatoryInfo(regulatoryInfoIdentifier, regulatoryInfoType, regulatoryInfoAlias);
        regulatoryInfoEjb.addRegulatoryInfoToResearchProject(regulatoryInfo.getRegulatoryInfoId(), editResearchProject);
        return new RedirectResolution(ResearchProjectActionBean.class, VIEW_ACTION)
                .addParameter(RESEARCH_PROJECT_PARAMETER, editResearchProject.getBusinessKey());
    }

    /**
     * Removes a regulatory info record from the current research project. The ID of the regulatory info comes from
     * this.regulatoryInfoId.
     *
     * @return a redirect to the research project view page
     */
    @HandlesEvent(REMOVE_REGULATORY_INFO_ACTION)
    public Resolution removeRegulatoryInfo() {
        regulatoryInfoEjb.removeRegulatoryInfoFromResearchProject(regulatoryInfoId, editResearchProject);
        return new RedirectResolution(ResearchProjectActionBean.class, VIEW_ACTION)
                .addParameter(RESEARCH_PROJECT_PARAMETER, editResearchProject.getBusinessKey());
    }

    /**
     * Edits a regulatory info record, specifically the title (alias, name). The ID of the regulatory info comes from
     * this.regulatoryInfoId and the new title comes from this.regulatoryInfoAlias.
     *
     * @return a redirect to the research project view page
     */
    @HandlesEvent(EDIT_REGULATORY_INFO_ACTION)
    public Resolution editRegulatoryInfo() {
        regulatoryInfoEjb.editRegulatoryInfo(regulatoryInfoId, regulatoryInfoAlias);
        return new RedirectResolution(ResearchProjectActionBean.class, VIEW_ACTION)
                .addParameter(RESEARCH_PROJECT_PARAMETER, editResearchProject.getBusinessKey());
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

    public UserTokenInput getOtherUserList() {
        return otherUserList;
    }

    public void setOtherUserList(UserTokenInput otherUserList) {
        this.otherUserList = otherUserList;
    }

    public String getQ() {
        return q;
    }

    public void setQ(String q) {
        this.q = q;
    }

    public Long getRegulatoryInfoId() {
        return regulatoryInfoId;
    }

    public void setRegulatoryInfoId(Long regulatoryInfoId) {
        this.regulatoryInfoId = regulatoryInfoId;
    }

    public String getRegulatoryInfoIdentifier() {
        return regulatoryInfoIdentifier;
    }

    public void setRegulatoryInfoIdentifier(String regulatoryInfoIdentifier) {
        this.regulatoryInfoIdentifier = regulatoryInfoIdentifier;
    }

    public RegulatoryInfo.Type getRegulatoryInfoType() {
        return regulatoryInfoType;
    }

    public void setRegulatoryInfoType(RegulatoryInfo.Type regulatoryInfoType) {
        this.regulatoryInfoType = regulatoryInfoType;
    }

    public String getRegulatoryInfoAlias() {
        return regulatoryInfoAlias;
    }

    public void setRegulatoryInfoAlias(String regulatoryInfoAlias) {
        this.regulatoryInfoAlias = regulatoryInfoAlias;
    }

    /**
     * @return true if Save is a valid operation.
     */
    public boolean getCanSave() {
        // User must be logged into JIRA to create or edit a Research Project.
        return userBean.isValidUser();
    }

    public CompletionStatusFetcher getProgressFetcher() {
        return progressFetcher;
    }

    public ProjectTokenInput getProjectTokenInput() {
        return projectTokenInput;
    }

    /**
     * Get the list of available sequence aligners.
     *
     * @return List of strings representing the sequence aligners
     */
    public Collection<DisplayableItem> getSequenceAligners() {
        return mercuryClientService.getSequenceAligners();
    }

    /**
     * Get the sequence aligner.
     *
     * @param businessKey the businessKey
     * @return UI helper object {@link DisplayableItem} representing the sequence aligner
     */
    public DisplayableItem getSequenceAligner(String businessKey) {
        return mercuryClientService.getSequenceAligner(businessKey);
    }

    /**
     * Get the reference sequence.
     *
     * @param businessKey the businessKey
     * @return UI helper object {@link DisplayableItem} representing the reference sequence
     */
    public DisplayableItem getReferenceSequence(String businessKey) {
        return mercuryClientService.getReferenceSequence(businessKey);
    }

    /**
     * Get the list of available reference sequences.
     *
     * @return List of strings representing the reference sequences
     */
    public Collection<DisplayableItem> getReferenceSequences() {
        return mercuryClientService.getReferenceSequences();
    }

    /**
     * @return Show the create title if this is a developer or PDM.
     */
    @Override
    public boolean isCreateAllowed() {
        return isEditAllowed();
    }

    /**
     * @return Show the edit title if this is a developer or PDM.
     */
    @Override
    public boolean isEditAllowed() {
        return getUserBean().isDeveloperUser() || getUserBean().isPMUser() || getUserBean().isPDMUser();
    }
}

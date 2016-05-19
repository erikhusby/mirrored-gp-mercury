package org.broadinstitute.gpinformatics.athena.presentation.projects;

import com.google.common.collect.Collections2;
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
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.commons.validator.routines.EmailValidator;
import org.broadinstitute.gpinformatics.athena.boundary.orders.CompletionStatusFetcher;
import org.broadinstitute.gpinformatics.athena.boundary.projects.CollaborationService;
import org.broadinstitute.gpinformatics.athena.boundary.projects.RegulatoryInfoEjb;
import org.broadinstitute.gpinformatics.athena.boundary.projects.ResearchProjectEjb;
import org.broadinstitute.gpinformatics.athena.boundary.projects.SampleKitRecipient;
import org.broadinstitute.gpinformatics.athena.control.dao.orders.ProductOrderDao;
import org.broadinstitute.gpinformatics.athena.control.dao.projects.RegulatoryInfoDao;
import org.broadinstitute.gpinformatics.athena.control.dao.projects.ResearchProjectDao;
import org.broadinstitute.gpinformatics.athena.entity.orders.ProductOrder;
import org.broadinstitute.gpinformatics.athena.entity.person.RoleType;
import org.broadinstitute.gpinformatics.athena.entity.project.CollaborationData;
import org.broadinstitute.gpinformatics.athena.entity.project.RegulatoryInfo;
import org.broadinstitute.gpinformatics.athena.entity.project.ResearchProject;
import org.broadinstitute.gpinformatics.athena.entity.project.SubmissionTuple;
import org.broadinstitute.gpinformatics.athena.presentation.DisplayableItem;
import org.broadinstitute.gpinformatics.athena.presentation.converter.IrbConverter;
import org.broadinstitute.gpinformatics.athena.presentation.tokenimporters.BioProjectTokenInput;
import org.broadinstitute.gpinformatics.athena.presentation.tokenimporters.CohortTokenInput;
import org.broadinstitute.gpinformatics.athena.presentation.tokenimporters.FundingTokenInput;
import org.broadinstitute.gpinformatics.athena.presentation.tokenimporters.ProjectTokenInput;
import org.broadinstitute.gpinformatics.athena.presentation.tokenimporters.UserTokenInput;
import org.broadinstitute.gpinformatics.infrastructure.ValidationException;
import org.broadinstitute.gpinformatics.infrastructure.bass.BassDTO;
import org.broadinstitute.gpinformatics.infrastructure.bass.BassFileType;
import org.broadinstitute.gpinformatics.infrastructure.bioproject.BioProject;
import org.broadinstitute.gpinformatics.infrastructure.bioproject.BioProjectList;
import org.broadinstitute.gpinformatics.infrastructure.bsp.BSPCohortList;
import org.broadinstitute.gpinformatics.infrastructure.bsp.BSPUserList;
import org.broadinstitute.gpinformatics.infrastructure.collaborate.CollaborationNotFoundException;
import org.broadinstitute.gpinformatics.infrastructure.collaborate.CollaborationPortalException;
import org.broadinstitute.gpinformatics.infrastructure.common.TokenInput;
import org.broadinstitute.gpinformatics.infrastructure.submission.SubmissionData;
import org.broadinstitute.gpinformatics.infrastructure.submission.SubmissionDataBean;
import org.broadinstitute.gpinformatics.infrastructure.submission.SubmissionDto;
import org.broadinstitute.gpinformatics.infrastructure.submission.SubmissionDtoFetcher;
import org.broadinstitute.gpinformatics.infrastructure.submission.SubmissionLibraryDescriptor;
import org.broadinstitute.gpinformatics.infrastructure.submission.SubmissionRepository;
import org.broadinstitute.gpinformatics.infrastructure.submission.SubmissionStatusDetailBean;
import org.broadinstitute.gpinformatics.infrastructure.submission.SubmissionsService;
import org.broadinstitute.gpinformatics.mercury.boundary.InformaticsServiceException;
import org.broadinstitute.gpinformatics.mercury.control.dao.analysis.AlignerDao;
import org.broadinstitute.gpinformatics.mercury.control.dao.analysis.ReferenceSequenceDao;
import org.broadinstitute.gpinformatics.mercury.presentation.CoreActionBean;
import org.broadinstitute.gpinformatics.mercury.presentation.UserBean;
import org.codehaus.jackson.map.ObjectMapper;
import org.codehaus.jackson.map.RuntimeJsonMappingException;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import javax.inject.Inject;
import java.io.IOException;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * This class is for research projects action bean / web page.
 */
@SuppressWarnings("unused")
@UrlBinding(ResearchProjectActionBean.ACTIONBEAN_URL_BINDING)
public class ResearchProjectActionBean extends CoreActionBean {
    private static final Log log = LogFactory.getLog(ResearchProjectActionBean.class);

    public static final String ACTIONBEAN_URL_BINDING = "/projects/project.action";
    public static final String RESEARCH_PROJECT_PARAMETER = "researchProject";
    public static final String RESEARCH_PROJECT_TAB_PARAMETER = "rpSelectedTab";
    public static final String RESEARCH_PROJECT_DEFAULT_TAB = "0";
    public static final String RESEARCH_PROJECT_SUBMISSIONS_TAB = "1";

    private static final String PROJECT = "Research Project";
    public static final String CREATE_PROJECT = CoreActionBean.CREATE + PROJECT;
    public static final String EDIT_PROJECT = CoreActionBean.EDIT + PROJECT;

    public static final String REGULATORY_INFO_QUERY_ACTION = "regulatoryInfoQuery";
    public static final String ADD_REGULATORY_INFO_TO_RESEARCH_PROJECT_ACTION = "addRegulatoryInfoToResearchProject";
    public static final String ADD_NEW_REGULATORY_INFO_ACTION = "addNewRegulatoryInfo";
    public static final String REMOVE_REGULATORY_INFO_ACTION = "removeRegulatoryInfo";
    public static final String VIEW_REGULATORY_INFO_ACTION = "viewRegulatoryInfo";
    public static final String EDIT_REGULATORY_INFO_ACTION = "editRegulatoryInfo";
    public static final String VALIDATE_TITLE_ACTION = "validateTitle";
    public static final String VIEW_SUBMISSIONS_ACTION = "viewSubmissions";
    public static final String POST_SUBMISSIONS_ACTION = "postSubmissions";
    public static final String GET_SUBMISSION_STATUSES_ACTION = "getSubmissionStatuses";

    public static final String PROJECT_CREATE_PAGE = "/projects/create.jsp";
    public static final String PROJECT_LIST_PAGE = "/projects/list.jsp";
    public static final String PROJECT_VIEW_PAGE = "/projects/view.jsp";
    public static final String PROJECT_SUBMISSIONS_PAGE = "/projects/submissions.jsp";

    private static final String BEGIN_COLLABORATION_ACTION = "beginCollaboration";

    private static final String RESEND_INVITATION_ACTION = "resendInvitation";

    // Reference sequence that will be used for Exome projects.
    private static final String DEFAULT_REFERENCE_SEQUENCE = "Homo_sapiens_assembly19|1";
    public static final String SESSION_SAMPLES_KEY = "SUBMISSIONS";

    @Inject
    private ResearchProjectDao researchProjectDao;

    @Inject
    private BSPUserList bspUserList;

    @Inject
    private BSPCohortList cohortList;

    @Inject
    private ProjectTokenInput projectTokenInput;

    @Inject
    private BioProjectTokenInput bioProjectTokenInput;

    @Inject
    private SubmissionsService submissionsService;

    private List<SubmissionRepository> submissionRepositories;

    private List<SubmissionLibraryDescriptor> submissionLibraryDescriptors;

    private SubmissionLibraryDescriptor submissionLibraryDescriptor;

    private SubmissionRepository submissionRepository;

    private String selectedSubmissionLibraryDescriptor;
    private String selectedSubmissionRepository;

    @Validate(required = true, on = {EDIT_ACTION, VIEW_ACTION, BEGIN_COLLABORATION_ACTION})
    @Inject
    private RegulatoryInfoDao regulatoryInfoDao;

    @Inject
    private RegulatoryInfoEjb regulatoryInfoEjb;

    @Inject
    private SubmissionDtoFetcher submissionDtoFetcher;
    /**
     * The research project business key
     */
    @Validate(required = true, on = {EDIT_ACTION, VIEW_ACTION})
    private String researchProject;

    private Long selectedCollaborator;
    private String specifiedCollaborator;
    private String collaborationMessage;

    @Validate(required = true, on = {BEGIN_COLLABORATION_ACTION})
    private String collaborationQuoteId;

    /**
     * This defines where kits will be sent for orders placed from the collaboration portal.
     */
    @Validate(required = true, on = BEGIN_COLLABORATION_ACTION)
    private SampleKitRecipient sampleKitRecipient = SampleKitRecipient.COLLABORATOR;

    @ValidateNestedProperties({
            @Validate(field = "title", label = "Project", required = true, maxlength = 4000, on = {SAVE_ACTION}),
            @Validate(field = "synopsis", label = "Synopsis", required = true, maxlength = 4000, on = {SAVE_ACTION}),
            @Validate(field = "irbNotes", label = "IRB Notes", required = false, maxlength = 255, on = {SAVE_ACTION}),
            @Validate(field = "comments", label = "Comments", maxlength = 2000, on = {SAVE_ACTION}),
            @Validate(field = "regulatoryDesignation", label = "Regulatory Designation", required = true, on = {SAVE_ACTION})
    })
    private ResearchProject editResearchProject;

    private List<SubmissionData> submissionSamples = new ArrayList<>();
    /*
     * The search query.
     */
    private String q;

    private List<RegulatoryInfo> searchResults;

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

    private List<String> selectedSubmissionSamples;

    @Inject
    private AlignerDao alignerDao;

    public Map<String, String> getBioSamples() {
        return bioSamples;
    }

    public void setBioSamples(Map<String, String> bioSamples) {
        this.bioSamples = bioSamples;
    }

    private Map<String, String> bioSamples=new HashMap<>();

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
    private ResearchProjectEjb researchProjectEjb;

    @Inject
    private CollaborationService collaborationService;

    @Inject
    private ReferenceSequenceDao referenceSequenceDao;

    private String irbList;

    private CompletionStatusFetcher progressFetcher;

    private CollaborationData collaborationData;

    private boolean validCollaborationPortal;

    private String rpSelectedTab;

    @Inject
    private BioProjectList bioProjectList;

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
            on = {VIEW_ACTION, EDIT_ACTION, CREATE_ACTION, SAVE_ACTION, REGULATORY_INFO_QUERY_ACTION,
                    ADD_REGULATORY_INFO_TO_RESEARCH_PROJECT_ACTION, ADD_NEW_REGULATORY_INFO_ACTION,
                    REMOVE_REGULATORY_INFO_ACTION, EDIT_REGULATORY_INFO_ACTION, BEGIN_COLLABORATION_ACTION,
                    RESEND_INVITATION_ACTION, VIEW_SUBMISSIONS_ACTION, POST_SUBMISSIONS_ACTION,
                    GET_SUBMISSION_STATUSES_ACTION})
    public void init() throws Exception {
        researchProject = getContext().getRequest().getParameter(RESEARCH_PROJECT_PARAMETER);
        if (!StringUtils.isBlank(researchProject)) {
            editResearchProject = researchProjectDao.findByBusinessKey(researchProject);
            try {
                collaborationData = collaborationService.getCollaboration(researchProject);
                validCollaborationPortal = true;
            } catch (CollaborationNotFoundException | CollaborationPortalException ex) {
                // If there is no collaboration service, for whatever reason, set the data to null so that we
                collaborationData = null;
                validCollaborationPortal = false;
            }
            if (submissionLibraryDescriptor == null) {
                submissionLibraryDescriptor = findDefaultSubmissionType(editResearchProject);
                if (submissionLibraryDescriptor != null) {
                    selectedSubmissionLibraryDescriptor = submissionLibraryDescriptor.getName();
                }
            }
            if (submissionRepository == null) {
                submissionRepository = editResearchProject.getSubmissionRepository();
                selectedSubmissionRepository = submissionRepository.getName();
            }
        } else {
            if (getUserBean().isValidBspUser()) {
                editResearchProject = new ResearchProject(getUserBean().getBspUser());
            } else {
                editResearchProject = new ResearchProject();
            }
        }

        setSubmissionLibraryDescriptors(submissionsService.getSubmissionLibraryDescriptors());
        setSubmissionRepositories(submissionsService.getSubmissionRepositories());
        populateTokenListsFromObjectData();

        if (StringUtils.isBlank(editResearchProject.getReferenceSequenceKey())) {
            editResearchProject.setReferenceSequenceKey(DEFAULT_REFERENCE_SEQUENCE);
        }

        // Get the totals for the order
        Collection<Long> productOrderIds = new ArrayList<>();
        for (ProductOrder order : editResearchProject.getProductOrders()) {
            productOrderIds.add(order.getProductOrderId());
        }

        progressFetcher = new CompletionStatusFetcher(productOrderDao.getProgress(productOrderIds));
    }

    private SubmissionLibraryDescriptor findDefaultSubmissionType(ResearchProject researchProject) {
        SubmissionLibraryDescriptor defaultSubmissionLibraryDescriptor = null;
        Set<SubmissionLibraryDescriptor> projectSubmissionLibraryDescriptors =new HashSet<>();
        for (ProductOrder productOrder : researchProject.getProductOrders()) {
            SubmissionLibraryDescriptor submissionType =
                    productOrder.getProduct().getProductFamily().getSubmissionType();
            if (submissionType != null) {
                projectSubmissionLibraryDescriptors.add(submissionType);
            }
        }
        if (projectSubmissionLibraryDescriptors.size() == 1) {
            defaultSubmissionLibraryDescriptor = projectSubmissionLibraryDescriptors.iterator().next();
        }
        return defaultSubmissionLibraryDescriptor;
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

    /**
     * Validation of project name.
     *
     * @param errors The errors object
     */
    @ValidationMethod(on = BEGIN_COLLABORATION_ACTION)
    public void validateCollaborationInformation(ValidationErrors errors) {
        String ERROR_PREFIX = "Cannot create a collaboration because ";
        String RP_ERROR_PREFIX = ERROR_PREFIX + "this research project ";


        // Cannot start a collaboration with an outside user if there is no PM specified.
        if (editResearchProject.getProjectManagers().length == 0) {
            addGlobalValidationError(RP_ERROR_PREFIX + "does not have a Project Manager.");
        }

        if (editResearchProject.getBroadPIs().length == 0) {
            addGlobalValidationError(RP_ERROR_PREFIX + "does not have a Primary Investigator.");
        }

        if (editResearchProject.getCohortIds().length == 0) {
            addGlobalValidationError(RP_ERROR_PREFIX + "does not have a Sample Cohort.");
        }

        if (editResearchProject.getCohortIds().length > 1) {
            addGlobalValidationError(RP_ERROR_PREFIX + "has more than one Sample Cohort.");
        }

        if (editResearchProject.getRegulatoryInfos().isEmpty()) {
            addGlobalValidationError(RP_ERROR_PREFIX + "has no Regulatory Information.");
        }

        if (editResearchProject.getRegulatoryInfos().size() > 1) {
            addGlobalValidationError(RP_ERROR_PREFIX + "has more than one Regulatory Information.");
        }

        if ((specifiedCollaborator == null) && (selectedCollaborator == null)) {
            addGlobalValidationError(ERROR_PREFIX + "an existing collaborator or an email address is required.");
        }

        if (specifiedCollaborator != null && !EmailValidator.getInstance(false).isValid(specifiedCollaborator)) {
            addGlobalValidationError(ERROR_PREFIX + "''{2}'' is not a valid email address.", specifiedCollaborator);
        }

        validateQuoteId(collaborationQuoteId);
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
            addGlobalValidationError(
                    MessageFormat.format(UserBean.LOGIN_WARNING, validatingFor + " a research project"));
        }
    }

    @HandlesEvent(CREATE_ACTION)
    public Resolution create() {
        validateUser("create");
        setSubmitString(CREATE_PROJECT);
        return new ForwardResolution(PROJECT_CREATE_PAGE);
    }

    @HandlesEvent(EDIT_ACTION)
    public Resolution edit() {
        validateUser("edit");
        setSubmitString(EDIT_PROJECT);
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
        bioProjectTokenInput.setup(bioProjectList.getBioProjectMap().keySet());
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
            log.error(errorMessage, ex);
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

        return new RedirectResolution(ResearchProjectActionBean.class, VIEW_ACTION)
                .addParameter(RESEARCH_PROJECT_PARAMETER, editResearchProject.getBusinessKey());
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
        editResearchProject.setParentResearchProject(
                tokenProject != null ? researchProjectDao.findByBusinessKey(tokenProject.getBusinessKey()) : null);
    }

    @HandlesEvent(VIEW_ACTION)
    public Resolution view() {

        return new ForwardResolution(PROJECT_VIEW_PAGE);
    }

    @HandlesEvent(BEGIN_COLLABORATION_ACTION)
    public Resolution beginCollaboration() throws Exception {
        try {
            collaborationService.beginCollaboration(editResearchProject, selectedCollaborator, specifiedCollaborator,
                    collaborationQuoteId, sampleKitRecipient, collaborationMessage);
            addMessage("Collaboration created successfully");
        } catch (Exception e) {
            log.error(BEGIN_COLLABORATION_ACTION, e);
            addGlobalValidationError("Could not begin the Collaboration: {2}", e.getMessage());
        }

        // Call init again so that any changed data due to calling beginCollaboration is retrieved.
        init();
        return view();
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

    public String getCollaborationMessage() {
        return collaborationMessage;
    }

    public void setCollaborationMessage(String collaborationMessage) {
        this.collaborationMessage = collaborationMessage;
    }

    public String getSpecifiedCollaborator() {
        return specifiedCollaborator;
    }

    public void setSpecifiedCollaborator(String specifiedCollaborator) {
        this.specifiedCollaborator = specifiedCollaborator;
    }

    public Long getSelectedCollaborator() {
        return selectedCollaborator;
    }

    public void setSelectedCollaborator(Long selectedCollaborator) {
        this.selectedCollaborator = selectedCollaborator;
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

    @HandlesEvent("bioProjectAutocomplete")
    public Resolution bioProjectAutocomplete() throws Exception {
        return createTextResolution(bioProjectTokenInput.getJsonString(getQ()));
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
     */
    @HandlesEvent(REGULATORY_INFO_QUERY_ACTION)
    public Resolution queryRegulatoryInfoReturnHtmlSnippet() {
        searchResults = regulatoryInfoDao.findByIdentifier(q);
        regulatoryInfoIdentifier = q;
        return new ForwardResolution("regulatory_info_dialog_sheet_2.jsp");
    }

    /**
     * Determines whether or not the given regulatory information is already associated with the current research
     * project. This is used to determine whether an existing regulatory information can be added to a project.
     *
     * @param regulatoryInfo the regulatory info to check
     *
     * @return true if the regulatory info is associated with the research project; false otherwise
     */
    public boolean isRegulatoryInfoInResearchProject(RegulatoryInfo regulatoryInfo) {
        return editResearchProject.getRegulatoryInfos().contains(regulatoryInfo);
    }

    /**
     * Determines whether or not the given regulatory information is being used for a product order for the current
     * research project. This is used to determine whether regulatory information can be safely disassociated with the
     * project.
     *
     * @param regulatoryInfo the regulatory info to check
     *
     * @return true if the regulatory info is in use for an order; false otherwise
     */
    public boolean isRegulatoryInfoInProductOrdersForThisResearchProject(RegulatoryInfo regulatoryInfo) {
        for (ProductOrder productOrder : editResearchProject.getProductOrders()) {
            if (productOrder.getRegulatoryInfos().contains(regulatoryInfo)) {
                return true;
            }
        }
        return false;
    }

    /**
     * Determines whether or not the regulatory information form represents a new record.
     *
     * @return true if creating a new regulatory information; false otherwise
     */
    public boolean isRegulatoryInformationNew() {
        return regulatoryInfoId == null;
    }

    /**
     * Returns all of the possible regulatory information types. This is used to access the enumeration values because
     * stripes:options-enumeration does not support a "disabled" attribute.
     *
     * @return a collection of all values from the {@link RegulatoryInfo.Type} enum
     */
    public RegulatoryInfo.Type[] getAllTypes() {
        return RegulatoryInfo.Type.values();
    }

    /**
     * Determines whether or not there are any regulatory information types that can be added for the queried
     * identifier. If there is already regulatory information for every type for the queried identifier, the UI should
     * not prompt to create a new record.
     *
     * @return true if the user should be allowed to create new regulatory info; false otherwise
     */
    public boolean isAddRegulatoryInfoAllowed() {
        EnumSet<RegulatoryInfo.Type> types = EnumSet.allOf(RegulatoryInfo.Type.class);
        for (RegulatoryInfo regulatoryInfo : searchResults) {
            types.remove(regulatoryInfo.getType());
        }
        return !types.isEmpty();
    }

    /**
     * Determines whether or not a regulatory information of the given type already exists with the queried identifier.
     *
     * @param type the type to check
     *
     * @return true if the type is already in use for the identifier; false otherwise
     */
    public boolean isTypeInUseForIdentifier(RegulatoryInfo.Type type) {
        for (RegulatoryInfo searchResult : searchResults) {
            if (searchResult.getType() == type) {
                return true;
            }
        }
        return false;
    }

    /**
     * Loads an existing regulatory information and pre-populates the regulatory information form.
     *
     * @return a resolution to the regulatory information form
     */
    @HandlesEvent(VIEW_REGULATORY_INFO_ACTION)
    public Resolution viewRegulatoryInfo() {
        RegulatoryInfo regulatoryInfo = regulatoryInfoDao.findById(RegulatoryInfo.class, regulatoryInfoId);
        if (regulatoryInfo != null) {
            regulatoryInfoIdentifier = regulatoryInfo.getIdentifier();
            searchResults = regulatoryInfoDao.findByIdentifier(regulatoryInfoIdentifier);
            regulatoryInfoType = regulatoryInfo.getType();
            regulatoryInfoAlias = regulatoryInfo.getName();
        }
        return new ForwardResolution("regulatory_info_form.jsp");
    }

    private static JSONObject regulatoryInfoToJSONObject(RegulatoryInfo regulatoryInfo, boolean alreadyAdded)
            throws JSONException {
        JSONObject object = new JSONObject();
        object.put("id", regulatoryInfo.getRegulatoryInfoId());
        object.put("identifier", regulatoryInfo.getIdentifier());
        object.put("type", regulatoryInfo.getType().getName());
        object.put("alias", regulatoryInfo.getName());
        object.put("alreadyAdded", alreadyAdded);
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
    @HandlesEvent(ADD_NEW_REGULATORY_INFO_ACTION)
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

    @HandlesEvent(VALIDATE_TITLE_ACTION)
    public Resolution validateTitle() {
        String result = "";
        if (StringUtils.isBlank(regulatoryInfoAlias)) {
            result = "Protocol Title is required.";
        } else if (regulatoryInfoAlias.length() > RegulatoryInfo.PROTOCOL_TITLE_MAX_LENGTH) {
            result = String.format("Protocol title exceeds maximum length of %d with %d.",
                    RegulatoryInfo.PROTOCOL_TITLE_MAX_LENGTH, regulatoryInfoAlias.length());
        } else {
            List<RegulatoryInfo> infos = regulatoryInfoDao.findByName(regulatoryInfoAlias);
            for (RegulatoryInfo info : infos) {
                if (!info.getRegulatoryInfoId().equals(regulatoryInfoId)) {
                    result = String.format("Title is already in use. If you are adding an ORSP # to use in place of an IRB #, please try appending the ORSP # to the title.");
                    break;
                }
            }
        }
        return createTextResolution(result);
    }

    @HandlesEvent(VIEW_SUBMISSIONS_ACTION)
    public Resolution viewSubmissions() throws IOException, JSONException {
        ObjectMapper objectMapper = new ObjectMapper();
        Map<String, Object> submissionSamplesMap = new HashMap<String, Object>() {{
            put("aaData", submissionSamples);//iDisplayLength  iRecordsTotal
            put("iTotalRecords", submissionSamples.size());
        }};
        String submissionSamplesData = objectMapper.writeValueAsString(submissionSamplesMap);

        return createTextResolution(submissionSamplesData);
    }

    /**
     * Forces an update of all submission DTOs associated with the current Research Project
     */
    @Before(stages = LifecycleStage.EventHandling, on = {VIEW_SUBMISSIONS_ACTION})
    public void initializeForSubmissions() {
        updateSubmissionSamples();
    }

    @SuppressWarnings("unchecked")
    private void updateSubmissionSamples() {
        if (editResearchProject != null) {
            submissionSamples = getSamplesFromCache(researchProject);
            if (submissionSamples.isEmpty()) {
                for (SubmissionDto submissionDto : submissionDtoFetcher.fetch(editResearchProject)) {
                    SubmissionData submissionData = submissionDto.submissionData();
                    submissionSamples.add(submissionData);
                }
            } else {
                submissionDtoFetcher.refreshSubmissionStatuses(submissionSamples);
                log.info("submissionSamples retrieved from cache.");
            }
            addSamplesToCache(researchProject, submissionSamples);
        }
    }

    /**
     * Clear existing sampleData from the session and add new samples for businessKey.
     *
     * @param businessKey The research project businessKey
     * @param sampleData  A list of SampleData to add to the cache.
     */
    private void addSamplesToCache(String businessKey, List<SubmissionData> sampleData) {
        // remove
        getContext().getSession().removeAttribute(SESSION_SAMPLES_KEY);
        String jsonBean;
        ObjectMapper objectMapper = new ObjectMapper();
        try {
            jsonBean = objectMapper.writeValueAsString(new SubmissionDataBean(sampleData));
            if (jsonBean == null) {
                throw new RuntimeJsonMappingException("Could not serialize sampleData");
            }
            Map<String, String> submissionMap = new HashMap<>();
            submissionMap.put(researchProject, jsonBean);
            getContext().getSession().setAttribute(SESSION_SAMPLES_KEY, submissionMap);

        } catch (IOException e) {
            log.error("Could not serialize sample data", e);
        }
        log.info(String.format("%d submissionSamples saved in session attribute %s.", sampleData.size(),
                SESSION_SAMPLES_KEY));

    }

    /**
     * Retrieve samples from the session
     *
     * @param businessKey The research project businessKey
     *
     * @return List of SubmissionData for this RP.
     */
    @SuppressWarnings("unchecked")
    private List<SubmissionData> getSamplesFromCache(String businessKey) {
        // Samples are stored as a Map<String, List<>> with the key being the businessKey.
        // This ensures the samples returned are for this RP
        Map<String, String> submissionSamplesMap =
                (Map<String, String>) getContext().getSession().getAttribute(SESSION_SAMPLES_KEY);
        if (submissionSamplesMap != null) {
            String json = submissionSamplesMap.get(researchProject);
            if (json != null) {
                try {
                    ObjectMapper objectMapper = new ObjectMapper();
                    SubmissionDataBean submissionDataBean = objectMapper.readValue(json, SubmissionDataBean.class);
                    if (submissionDataBean.getSubmissionData() != null) {
                        List<SubmissionData> submissionData = submissionDataBean.getSubmissionData();
                        log.info(String.format("%d submissionSamples retrieved from cache.", submissionData.size()));
                        return submissionData;
                    }
                } catch (IOException e) {
                    log.error("Could not deserialize sample data", e);
                }
            }
        }
        return new ArrayList<>();
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

    /**
     * Handles a users request to submit samples to the submissions serverice
     *
     * @return
     */
    @HandlesEvent(POST_SUBMISSIONS_ACTION)
    public Resolution postSubmissions() {
        boolean errors = false;

        rpSelectedTab = RESEARCH_PROJECT_SUBMISSIONS_TAB;

        if (CollectionUtils.isEmpty(selectedSubmissionSamples)) {
            addGlobalValidationError("You must select at least one sample in order to post for submission.");
            errors = true;
        }
        BioProject selectedProject = bioProjectTokenInput.getTokenObject();

        if (selectedProject == null) {
            addGlobalValidationError("You must select a BioProject in order to post for submissions.");
            errors = true;
        }
        submissionLibraryDescriptor = submissionsService.findLibraryDescriptorTypeByKey(selectedSubmissionLibraryDescriptor);
        if (submissionLibraryDescriptor == null) {
            addGlobalValidationError("You must select a submission type in order to post for submissions.");
            errors = true;
        }

        submissionRepository = submissionsService.findRepositoryByKey(selectedSubmissionRepository);

        if (submissionRepository == null) {
            addGlobalValidationError("You must select a submission repository in order to post for submissions.");
            errors = true;
        }
        if (!errors) {
            List<SubmissionDto> selectedSubmissions = new ArrayList<>();
            Map<String, BassDTO> bassDtoMap = submissionDtoFetcher.fetchBassDtos(editResearchProject,
                    selectedSubmissionSamples.toArray(new String[selectedSubmissionSamples.size()]));
            for (BassDTO bassDTO : bassDtoMap.values()) {

                // All required data are in the bassDTO
                selectedSubmissions.add(new SubmissionDto(bassDTO, null, editResearchProject.getProductOrders(), null));
            }

            try {
                Collection<SubmissionStatusDetailBean> submissionStatuses =
                        researchProjectEjb
                                .processSubmissions(researchProject, new BioProject(selectedProject.getAccession()),
                                        selectedSubmissions, submissionRepository, submissionLibraryDescriptor);
                Map<SubmissionTuple, SubmissionDto> newSubmissionDtoMap = new HashMap<>(selectedSubmissions.size());
                for (SubmissionDto selectedSubmission : selectedSubmissions) {
                    newSubmissionDtoMap.put(selectedSubmission.getBassDTO().getTuple(), selectedSubmission);
                }
                submissionSamples = getSamplesFromCache(researchProject);
                for (SubmissionData submissionSample : submissionSamples) {
                    SubmissionTuple tuple = new SubmissionTuple(submissionSample.getSampleName(),
                            BassFileType.byBassValue(submissionSample.getFileTypeString()),
                            String.valueOf(submissionSample.getVersion()));
                    SubmissionDto submissionDto = newSubmissionDtoMap.get(tuple);
                    if (submissionDto != null) {
                        submissionSample.updateStatusDetail(submissionDto.getStatusDetailBean());
                    }
                }
                addSamplesToCache(SESSION_SAMPLES_KEY, submissionSamples);
                addMessage("The selected samples for submission have been successfully posted to NCBI.  See the " +
                           "Submission Requests tab for further details");
            } catch (InformaticsServiceException | ValidationException e) {
                addGlobalValidationError(e.getMessage());
            }
        }
        return new RedirectResolution(ResearchProjectActionBean.class, VIEW_ACTION)
                .addParameter(RESEARCH_PROJECT_PARAMETER, researchProject)
                .addParameter(RESEARCH_PROJECT_TAB_PARAMETER, RESEARCH_PROJECT_SUBMISSIONS_TAB);
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

    public List<RegulatoryInfo> getSearchResults() {
        return searchResults;
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

    public String getCollaborationQuoteId() {
        return collaborationQuoteId;
    }

    public void setCollaborationQuoteId(String collaborationQuoteId) {
        this.collaborationQuoteId = collaborationQuoteId;
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
        return makeDisplayableItemCollection(alignerDao.findAll());
    }

    /**
     * Get the sequence aligner.
     *
     * @param businessKey the businessKey
     *
     * @return UI helper object {@link DisplayableItem} representing the sequence aligner
     */
    public DisplayableItem getSequenceAligner(String businessKey) {
        return getDisplayableItemInfo(businessKey, alignerDao);
    }

    /**
     * Get the reference sequence.
     *
     * @param businessKey the businessKey
     *
     * @return UI helper object {@link DisplayableItem} representing the reference sequence
     */
    public DisplayableItem getReferenceSequence(String businessKey) {
        return getDisplayableItemInfo(businessKey, referenceSequenceDao);
    }

    /**
     * Get the list of available reference sequences.
     *
     * @return List of strings representing the reference sequences
     */
    public Collection<DisplayableItem> getReferenceSequences() {
        return makeDisplayableItemCollection(referenceSequenceDao.findAllCurrent());
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

    /**
     * @return true if the current user is allowed to request data submissions for the current research project
     */
    public boolean isSubmissionAllowed() {
        if (getUserBean().isDeveloperUser()) {
            return true;
        }
        Collection<Long> projectManagerIds = Arrays.asList(editResearchProject.getPeople(RoleType.PM));
        return getUserBean().isPMUser() && projectManagerIds.contains(getUserBean().getBspUser().getUserId());
    }

    public CollaborationData getCollaborationData() {
        return collaborationData;
    }

    /**
     * This checks whether there is an invitation pending.
     *
     * @return If the invitation is exists and is pending, this will be true.
     */
    public boolean isInvitationPending() {
        // Invitation pending means that an email is attached to this and there is no collaborating user.
        return collaborationData != null && collaborationData.getExpirationDate() != null;
    }

    @HandlesEvent(RESEND_INVITATION_ACTION)
    public Resolution resendInvitation() throws Exception {
        try {
            collaborationService.resendInvitation(researchProject);
            addMessage("Invitation resent successfully");
        } catch (Exception e) {
            addGlobalValidationError("Could not resend invitation due to an error: {2}", e.getMessage());
        }

        // Call init again so that the updated project is retrieved.
        init();
        return view();
    }

    public boolean isValidCollaborationPortal() {
        return validCollaborationPortal;
    }

    public List<SubmissionData> getSubmissionSamples() {
        return submissionSamples;
    }

    public void setSubmissionSamples(List<SubmissionData> submissionSamples) {
        this.submissionSamples = submissionSamples;
    }

    public List<SubmissionRepository> getSubmissionRepositories() {
        return submissionRepositories;
    }

    public void setSubmissionRepositories(List<SubmissionRepository> submissionRepositories) {
        this.submissionRepositories = submissionRepositories;
    }

    public List<SubmissionLibraryDescriptor> getSubmissionLibraryDescriptors() {
        return submissionLibraryDescriptors;
    }

    public void setSubmissionLibraryDescriptors(List<SubmissionLibraryDescriptor> submissionLibraryDescriptors) {
        this.submissionLibraryDescriptors = submissionLibraryDescriptors;
    }

    public BioProjectTokenInput getBioProjectTokenInput() {
        return bioProjectTokenInput;
    }


    public void setBioProjectTokenInput(BioProjectTokenInput bioProjectTokenInput) {
        this.bioProjectTokenInput = bioProjectTokenInput;
    }

    public void setSelectedSubmissionSamples(List<String> selectedSubmissionSamples) {
        this.selectedSubmissionSamples = selectedSubmissionSamples;
    }

    public List<String> getSelectedSubmissionSamples() {
        return selectedSubmissionSamples;
    }

    public String getRpSelectedTab() {
        return rpSelectedTab;
    }

    public void setRpSelectedTab(String rpSelectedTab) {
        this.rpSelectedTab = rpSelectedTab;
    }

    /**
     * A collaboration with the portal can only be started by PMs or Developers for research projects.
     *
     * @return True if you can start a collaboration.
     */
    public boolean isCanBeginCollaborations() {
        return isResearchOnly() && (getUserBean().isDeveloperUser() || getUserBean().isPMUser());
    }

    private boolean isResearchOnly() {
        return ((editResearchProject != null) &&
               (editResearchProject.getRegulatoryDesignation() == ResearchProject.RegulatoryDesignation.RESEARCH_ONLY));
    }

    public SampleKitRecipient getSampleKitRecipient() {
        return sampleKitRecipient;
    }

    public void setSampleKitRecipient(SampleKitRecipient sampleKitRecipient) {
        this.sampleKitRecipient = sampleKitRecipient;
    }

    public String getComplianceStatement() {
        return String.format(ResearchProject.REGULATORY_COMPLIANCE_STATEMENT,
                "orders created from this Research Project involve");
    }

    public SubmissionLibraryDescriptor getSubmissionLibraryDescriptor() {
        return submissionLibraryDescriptor;
    }

    public void setSubmissionLibraryDescriptor(SubmissionLibraryDescriptor submissionLibraryDescriptor) {
        this.submissionLibraryDescriptor = submissionLibraryDescriptor;
    }

    public SubmissionRepository getSubmissionRepository() {
        return submissionRepository;
    }

    public void setSubmissionRepository(SubmissionRepository submissionRepository) {
        this.submissionRepository = submissionRepository;
    }

    public String getSelectedSubmissionLibraryDescriptor() {
        return selectedSubmissionLibraryDescriptor;
    }

    public void setSelectedSubmissionLibraryDescriptor(String selectedSubmissionLibraryDescriptor) {
        this.selectedSubmissionLibraryDescriptor = selectedSubmissionLibraryDescriptor;
    }

    public String getSelectedSubmissionRepository() {
        return selectedSubmissionRepository;
    }

    public void setSelectedSubmissionRepository(String selectedSubmissionRepository) {
        this.selectedSubmissionRepository = selectedSubmissionRepository;
    }


    public Collection<SubmissionRepository> getActiveRepositories() {
        return Collections2.filter(getSubmissionRepositories(), SubmissionRepository.activeRepositoryPredicate);
    }

    public String getPreselectedStatusesJson() {
        JSONArray itemList = new JSONArray();
        Collection<SubmissionStatusDetailBean.Status> preselected =
                EnumSet.complementOf(EnumSet.of(SubmissionStatusDetailBean.Status.FAILURE));

        for (SubmissionStatusDetailBean.Status status : preselected) {
            itemList.put(status.getLabel());
        }
        return itemList.toString();
    }

    public String getSubmissionStatusesJson() {
        JSONArray itemList=new JSONArray();
        for (SubmissionStatusDetailBean.Status status : SubmissionStatusDetailBean.Status.values()) {
            itemList.put(status.getLabel());
        }
        return itemList.toString();
    }
}

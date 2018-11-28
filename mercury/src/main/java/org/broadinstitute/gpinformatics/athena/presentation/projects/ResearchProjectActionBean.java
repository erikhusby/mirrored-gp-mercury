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
import net.sourceforge.stripes.validation.ValidationError;
import net.sourceforge.stripes.validation.ValidationErrorHandler;
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
import org.broadinstitute.gpinformatics.infrastructure.analytics.OrspProjectDao;
import org.broadinstitute.gpinformatics.infrastructure.analytics.entity.OrspProject;
import org.broadinstitute.gpinformatics.infrastructure.bioproject.BioProject;
import org.broadinstitute.gpinformatics.infrastructure.bioproject.BioProjectList;
import org.broadinstitute.gpinformatics.infrastructure.bsp.BSPCohortList;
import org.broadinstitute.gpinformatics.infrastructure.bsp.BSPUserList;
import org.broadinstitute.gpinformatics.infrastructure.collaborate.CollaborationNotFoundException;
import org.broadinstitute.gpinformatics.infrastructure.collaborate.CollaborationPortalException;
import org.broadinstitute.gpinformatics.infrastructure.common.TokenInput;
import org.broadinstitute.gpinformatics.infrastructure.quote.QuoteNotFoundException;
import org.broadinstitute.gpinformatics.infrastructure.quote.QuoteServerException;
import org.broadinstitute.gpinformatics.infrastructure.quote.QuoteService;
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
import org.broadinstitute.gpinformatics.mercury.presentation.cache.SessionCache;
import org.broadinstitute.gpinformatics.mercury.presentation.cache.SessionCacheException;
import org.codehaus.jackson.map.ObjectMapper;
import org.codehaus.jackson.type.TypeReference;
import org.json.JSONArray;
import org.json.JSONException;

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
public class ResearchProjectActionBean extends CoreActionBean implements ValidationErrorHandler {
    private static final Log log = LogFactory.getLog(ResearchProjectActionBean.class);

    public static final String ACTIONBEAN_URL_BINDING = "/projects/project.action";
    public static final String RESEARCH_PROJECT_PARAMETER = "researchProject";
    public static final String RESEARCH_PROJECT_TAB_PARAMETER = "rpSelectedTab";
    private static final String LIBRARY_DESCRIPTOR_PARAMETER = "selectedSubmissionLibraryDescriptor";
    private static final String REPOSITORY_PARAMETER = "selectedSubmissionRepository";
    public static final String SUBMISSION_TUPLES_PARAMETER = "selectedSubmissionTuples";
    public static final String RESEARCH_PROJECT_ORDERS_TAB = "ordersTab";
    public static final String RESEARCH_PROJECT_SUBMISSIONS_TAB = "submissionsTab";

    private static final String PROJECT = "Research Project";
    public static final String CREATE_PROJECT = CoreActionBean.CREATE + PROJECT;
    public static final String EDIT_PROJECT = CoreActionBean.EDIT + PROJECT;

    public static final String REMOVE_REGULATORY_INFO_ACTION = "removeRegulatoryInfo";
    public static final String VIEW_SUBMISSIONS_ACTION = "viewSubmissions";
    public static final String POST_SUBMISSIONS_ACTION = "postSubmissions";
    public static final String GET_SUBMISSION_STATUSES_ACTION = "getSubmissionStatuses";

    public static final String PROJECT_CREATE_PAGE = "/projects/create.jsp";
    public static final String PROJECT_LIST_PAGE = "/projects/list.jsp";
    public static final String PROJECT_VIEW_PAGE = "/projects/view.jsp";
    public static final String PROJECT_SUBMISSIONS_PAGE = "/projects/submissions.jsp";
    public static final String BIOPROJECT_PARAMETER = "bioProjectTokenInput.listOfKeys";
    static final String SUBMISSIONS_UNAVAILABLE = "Submissions are temporarily unavailable.";
    public boolean supressValidationErrors;
    private static final String BEGIN_COLLABORATION_ACTION = "beginCollaboration";

    private static final String RESEND_INVITATION_ACTION = "resendInvitation";

    // Reference sequence that will be used for Exome projects.
    private static final String DEFAULT_REFERENCE_SEQUENCE = "Homo_sapiens_assembly19|1";
    public static final String SESSION_SAMPLES_KEY = "SUBMISSIONS";
    public static final String STRIPES_WARNING = "warning";
    public static final String STRIPES_ERRORS = "error";
    public static final String STRIPES_MESSAGES_KEY = "stripesMessages";
    public static final String STRIPES_MESSAGE_TYPE = "messageType";

    private ResearchProjectDao researchProjectDao;

    private BSPUserList bspUserList;

    private BSPCohortList cohortList;

    private ProjectTokenInput projectTokenInput;

    private BioProjectTokenInput bioProjectTokenInput;

    private SubmissionsService submissionsService;

    private List<SubmissionRepository> submissionRepositories=new ArrayList<>();

    private List<SubmissionLibraryDescriptor> submissionLibraryDescriptors=new ArrayList<>();

    private SubmissionLibraryDescriptor submissionLibraryDescriptor;

    private SubmissionRepository submissionRepository;

    private String selectedSubmissionLibraryDescriptor;
    private String selectedSubmissionRepository;

    private RegulatoryInfoEjb regulatoryInfoEjb;

    private SubmissionDtoFetcher submissionDtoFetcher;

    private OrspProjectDao orspProjectDao;

    /**
     * The research project business key
     */
    @Validate(required = true, on = {EDIT_ACTION, VIEW_ACTION, BEGIN_COLLABORATION_ACTION})
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

    private List<SubmissionDto> submissionSamples = new ArrayList<>();
    /*
     * The search query.
     */
    private String q;

    private Long regulatoryInfoId;

    /**
     * All research projects, fetched once and stored per-request (as a result of this bean being @RequestScoped).
     */
    private List<ResearchProject> allResearchProjects;

    /**
     * On demand counts of orders on the project. Map of business key to count value.
     */
    private Map<String, Long> projectOrderCounts;

    @Validate(converter = SubmissionTupleTypeConverter.class)
    private List<SubmissionTuple> selectedSubmissionTuples=new ArrayList<>();

    private AlignerDao alignerDao;

    private SessionCache<List<SubmissionDto>> sessionCache;
    public static final TypeReference<List<SubmissionDto>> SUBMISSION_SAMPLES_TYPE_REFERENCE =
            new TypeReference<List<SubmissionDto>>() {
            };
    private Boolean submissionsServiceAvailable=null;

    private QuoteService quoteService;

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

    private UserTokenInput projectManagerList;

    private UserTokenInput scientistList;

    private UserTokenInput externalCollaboratorList;

    private UserTokenInput broadPiList;

    private UserTokenInput otherUserList;

    private FundingTokenInput fundingSourceList;

    private CohortTokenInput cohortsList;

    private ProductOrderDao productOrderDao;

    private ResearchProjectEjb researchProjectEjb;

    private CollaborationService collaborationService;

    private ReferenceSequenceDao referenceSequenceDao;

    private String irbList;

    private CompletionStatusFetcher progressFetcher;

    private CollaborationData collaborationData;

    private boolean validCollaborationPortal;

    private String rpSelectedTab = RESEARCH_PROJECT_ORDERS_TAB;

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
            on = {VIEW_ACTION, EDIT_ACTION, CREATE_ACTION, SAVE_ACTION, BEGIN_COLLABORATION_ACTION,
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


            if (sessionCache == null) {
                sessionCache = new SessionCache<>(getContext().getRequest().getSession(), VIEW_SUBMISSIONS_ACTION,
                        SUBMISSION_SAMPLES_TYPE_REFERENCE);
            }

        } else {
            if (getUserBean().isValidBspUser()) {
                editResearchProject = new ResearchProject(getUserBean().getBspUser());
            } else {
                editResearchProject = new ResearchProject();
            }
            if (StringUtils.isNotBlank(selectedSubmissionRepository)) {
                editResearchProject.setSubmissionRepositoryName(selectedSubmissionRepository);
            }
        }

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

    @After(stages = LifecycleStage.BindingAndValidation,
            on = {VIEW_ACTION, EDIT_ACTION, CREATE_ACTION, SAVE_ACTION, VIEW_SUBMISSIONS_ACTION,
                    POST_SUBMISSIONS_ACTION, GET_SUBMISSION_STATUSES_ACTION})
    public void initSubmissions() {
        if (isSubmissionServiceAvailable()) {
            setSubmissionLibraryDescriptors(submissionsService.getSubmissionLibraryDescriptors());
            setSubmissionRepositories(submissionsService.getSubmissionRepositories());

            if (submissionRepository == null && StringUtils.isBlank(selectedSubmissionRepository)) {
                selectedSubmissionRepository = editResearchProject.getSubmissionRepositoryName();
                if (StringUtils.isNotBlank(selectedSubmissionRepository)) {
                    submissionRepository = submissionsService.findRepositoryByKey(selectedSubmissionRepository);
                }
            }
            if (submissionRepository != null && !submissionRepository.isActive() && getContext().getEventName()
                    .equals(VIEW_SUBMISSIONS_ACTION)) {
                addMessage("Selected submission site ''{0}'' is not active.",
                        submissionRepository.getDescription());
            }
            if (submissionLibraryDescriptor == null) {
                if (StringUtils.isNotBlank(selectedSubmissionLibraryDescriptor)) {
                    submissionLibraryDescriptor =
                            submissionsService.findLibraryDescriptorTypeByKey(selectedSubmissionLibraryDescriptor);
                } else {
                    submissionLibraryDescriptor = findDefaultSubmissionType(editResearchProject);
                    if (submissionLibraryDescriptor != null) {
                        selectedSubmissionLibraryDescriptor = submissionLibraryDescriptor.getName();
                    }
                }
            }
        }
    }

    SubmissionLibraryDescriptor findDefaultSubmissionType(ResearchProject researchProject) {
        SubmissionLibraryDescriptor defaultSubmissionLibraryDescriptor = null;
        Set<SubmissionLibraryDescriptor> projectSubmissionLibraryDescriptors =new HashSet<>();
        for (ProductOrder productOrder : researchProject.getProductOrders()) {
            if (productOrder.getOrderStatus() != ProductOrder.OrderStatus.Draft) {
                SubmissionLibraryDescriptor submissionType =
                        productOrder.getProduct().getProductFamily().getSubmissionType();
                if (submissionType != null) {
                    projectSubmissionLibraryDescriptors.add(submissionType);
                }
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

    private void validateQuoteId(String quoteId) {
        try {
            quoteService.getQuoteByAlphaId(quoteId);
        } catch (QuoteServerException e) {
            addGlobalValidationError("The quote ''{2}'' is not valid: {3}", quoteId, e.getMessage());
        } catch (QuoteNotFoundException e) {
            addGlobalValidationError("The quote ''{2}'' was not found ", quoteId);
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
     * Determines whether or not the given regulatory information can be edited. The only property that can be changed
     * is the name/title. However, if this record matches what is in the ORSP Portal, we don't want it to be changed.
     *
     * @param regulatoryInfo    the regulatory info to check
     *
     * @return true if the regulatory info can be edited; false otherwise
     */
    public boolean isRegulatoryInfoEditAllowed(RegulatoryInfo regulatoryInfo) {
        OrspProject orspProject = orspProjectDao.findByKey(regulatoryInfo.getIdentifier());
        return !getUserBean().isViewer() && !(orspProject != null && regulatoryInfo.getName().equals(orspProject.getName()));
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
     * Removes a regulatory info record from the current research project. The ID of the regulatory info comes from
     * this.regulatoryInfoId.
     *
     * @return a redirect to the research project view page
     */
    @HandlesEvent(REMOVE_REGULATORY_INFO_ACTION)
    public Resolution removeRegulatoryInfo() {
        regulatoryInfoEjb.removeRegulatoryInfoFromResearchProject(regulatoryInfoId, researchProject);
        return new RedirectResolution(ResearchProjectActionBean.class, VIEW_ACTION)
                .addParameter(RESEARCH_PROJECT_PARAMETER, researchProject);
    }

    @HandlesEvent(VIEW_SUBMISSIONS_ACTION)
    public Resolution viewSubmissions() throws IOException, JSONException {
        String submissionSamplesData = getSubmissionJson(getFormattedMessages(), STRIPES_WARNING);
        return createTextResolution(submissionSamplesData);
    }

    private String getSubmissionJson(List<String> validationMessages, final String errorType) throws IOException {
        ObjectMapper objectMapper = new ObjectMapper();
        Map<String, Object> submissionSamplesMap = new HashMap<>();
        submissionSamplesMap.put("aaData", submissionSamples);
        submissionSamplesMap.put("iTotalRecords", submissionSamples.size());
        if (!validationMessages.isEmpty()) {
            submissionSamplesMap.put(STRIPES_MESSAGES_KEY, validationMessages);
            submissionSamplesMap.put(STRIPES_MESSAGE_TYPE, errorType);
        }

        return objectMapper.writeValueAsString(submissionSamplesMap);
    }

    /**
     * (Copied From {@link ValidationErrorHandler}) <br/>
     * Implementing this interface gives ActionBeans the chance to modify what happens when the binding and/or
     * validation phase(s) generate errors. <b>The handleValidationErrors method is invoked after all validation has
     * completed</b> - i.e. after annotation based validation and any ValidationMethods that are applicable for the
     * current request. Invocation only happens when one or more validation errors exist. Also, note that setContext()
     * will always have been invoked prior to handleValidationErrors(ValidationErrors), allowing the bean access to the
     * event name and other information.
     *
     * @see ValidationErrorHandler
     */
    @Override
    public Resolution handleValidationErrors(ValidationErrors errors) throws Exception {
        boolean isSubmission = false;
        String eventName = getContext().getEventName();
        if (StringUtils.isNotBlank(eventName)) {
            if (eventName.equals(VIEW_SUBMISSIONS_ACTION)) {
                isSubmission = true;
            } else if (eventName.equals(POST_SUBMISSIONS_ACTION)) {
                isSubmission = true;
            }
        }
        final List<String> errorList = new ArrayList<>();
        if (isSubmission) {
            String submissionSamplesData;
            List<String> validationErrors = new ArrayList<>();
            for (List<ValidationError> fieldErrors : errors.values()) {
                for (ValidationError error : fieldErrors) {
                    errorList.add(error.getMessage(getContext().getLocale()));
                }
            }
        } else {
            return null;
        }

        String submissionSamplesData = getSubmissionJson(errorList, STRIPES_ERRORS);
        return createTextResolution(submissionSamplesData);
    }

    /**
     * Forces an update of all submission DTOs associated with the current Research Project
     */
    @Before(stages = LifecycleStage.EventHandling, on = {VIEW_SUBMISSIONS_ACTION})
    public void initializeForSubmissions() {
        updateSubmissionSamples();
    }

    public boolean validateViewOrPostSubmissions(boolean suppressValidationErrors) {
        this.supressValidationErrors=suppressValidationErrors;
        return validateViewOrPostSubmissions();
    }

    @ValidationMethod(on = {VIEW_SUBMISSIONS_ACTION, POST_SUBMISSIONS_ACTION}, priority=0)
    public boolean isSubmissionServiceAvailable() {
        if (submissionsServiceAvailable==null){
            boolean errorLogged=false;
            List<SubmissionLibraryDescriptor> libraryDescriptors;
            try {
                libraryDescriptors = submissionsService.getSubmissionLibraryDescriptors();
                submissionsServiceAvailable = CollectionUtils.isNotEmpty(libraryDescriptors);
            }catch (Exception e){
                log.error(SUBMISSIONS_UNAVAILABLE, e);
                errorLogged=true;
                submissionsServiceAvailable=false;
            }
            if (!submissionsServiceAvailable) {
                // Only need to do this once per request.
                if (!errorLogged) {
                    log.error(SUBMISSIONS_UNAVAILABLE);
                }
                addMessage(SUBMISSIONS_UNAVAILABLE);
            }
        }
        return submissionsServiceAvailable;
    }

    @ValidationMethod(on = {VIEW_SUBMISSIONS_ACTION, POST_SUBMISSIONS_ACTION}, priority=1)
    public boolean validateViewOrPostSubmissions() {
        if (getUserBean().isDeveloperUser()) {
            return true;
        }
        List<String> accessRestriction = new ArrayList<>();
        if (!isProjectAllowsSubmission()) {
            accessRestriction.add("research projects only");
        }
        Collection<Long> projectManagerIds = Arrays.asList(editResearchProject.getPeople(RoleType.PM));

        // Test both the user's role and whether or not they are listed as a project manager in the project. This
        // protects from the case where the user's role has been revoked, but the project people haven't been updated.
        boolean isPm = (getUserBean().isPMUser() || getUserBean().isGPPMUser()) && projectManagerIds.contains(getUserBean().getBspUser().getUserId());
        if (!isPm) {
            accessRestriction.add(String.format("Project Managers of %s", researchProject));
        }
        if (accessRestriction.isEmpty()) {
            return true;
        }

        if (!supressValidationErrors) {
            addGlobalValidationError(
                    String.format("Data submissions are available for %s.", StringUtils.join(accessRestriction, " and ")));
        }
        return false;
    }

    @SuppressWarnings("unchecked")
    private void updateSubmissionSamples() {
        if (editResearchProject != null) {
            try {
                populateSubmissionSamples(true);
            } catch (SessionCacheException e) {
                log.error("Error retrieving samples from cache", e);
            } catch (Exception e) {
                log.error("Error retrieving samples data", e);
                addMessage(e.getLocalizedMessage());
            }
        }
    }

    /**
     * Populate the submissionSamples variable with either cached or fresh values if they are not in the cache.
     *
     * @param refreshSubmissionStatus if true the SubmissionStatus is updated.
     */
    private void populateSubmissionSamples(boolean refreshSubmissionStatus) {
        try {
            submissionSamples = sessionCache.get(researchProject);
        } catch (SessionCacheException e) {
            submissionSamples = null;
            log.error("Could not load samples from cache.", e);
        }
        if (submissionSamples == null) {
            submissionSamples = submissionDtoFetcher.fetch(editResearchProject, this);
        } else {

            // When getting cached submissions update the submissionStatus if requested.
            if (!submissionSamples.isEmpty() && refreshSubmissionStatus) {
                submissionDtoFetcher.refreshSubmissionStatuses(editResearchProject, submissionSamples);
            }
        }
        sessionCache.put(researchProject, submissionSamples);
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

        if (CollectionUtils.isEmpty(selectedSubmissionTuples)) {
            addGlobalValidationError("You must select at least one sample in order to post for submission.");
            errors = true;
        }

        Map<SubmissionTuple, String> tupleToSampleMap=new HashMap<>();
        for (SubmissionTuple submissionTuple : selectedSubmissionTuples) {
            if (submissionTuple!=null) {
                tupleToSampleMap.put(submissionTuple, submissionTuple.getSampleName());
            }
        }
        BioProject selectedProject = bioProjectTokenInput.getTokenObject();

        if (selectedProject == null) {
            addGlobalValidationError("You must select a BioProject in order to post for submissions.");
            errors = true;
        }
        submissionLibraryDescriptor = submissionsService.findLibraryDescriptorTypeByKey(selectedSubmissionLibraryDescriptor);
        if (submissionLibraryDescriptor == null) {
            addGlobalValidationError("You must select a submission library in order to post for submissions.");
            errors = true;
        }

        submissionRepository = submissionsService.findRepositoryByKey(selectedSubmissionRepository);

        if (submissionRepository == null) {
            addGlobalValidationError("You must select a submission site in order to post for submissions.");
            errors = true;
        }
        List<SubmissionDto> selectedSubmissions = new ArrayList<>();
        if (!errors) {
            populateSubmissionSamples(false);
            for (SubmissionDto submissionDto : submissionSamples) {
                if (tupleToSampleMap.containsKey(submissionDto.getSubmissionTuple())) {
                    // All required data are in the submissionDto
                    selectedSubmissions.add(submissionDto);

                    if (!Arrays.asList(selectedSubmissionLibraryDescriptor, "N/A")
                        .contains(submissionDto.getDataType())) {
                        addGlobalValidationError(
                            "Data selected for submission of ''{2}'' is ''{3}'' but library ''{4}'' was selected.",
                            submissionDto.getSampleName(), submissionDto.getDataType(),
                            selectedSubmissionLibraryDescriptor);
                        errors = true;
                    }
                }
            }
        }
        if (!errors){
            try {
                Collection<SubmissionStatusDetailBean> submissionStatuses =
                        researchProjectEjb
                                .processSubmissions(researchProject, new BioProject(selectedProject.getAccession()),
                                        selectedSubmissions, submissionRepository, submissionLibraryDescriptor);
                addMessage("The selected samples for submission have been successfully posted to ''{0}''. " +
                           "See the Submission Requests tab for further details",
                    submissionsService.findRepositoryByKey(selectedSubmissionRepository).getDescription());
            } catch (InformaticsServiceException | ValidationException e) {
                log.error(e.getMessage(), e);
                addGlobalValidationError(e.getMessage());
            } catch (SessionCacheException e) {
                log.error("Error accessing cache", e);
            }
            updateUuid(selectedSubmissions);
        }
        return new ForwardResolution(ResearchProjectActionBean.class, VIEW_ACTION)
                .addParameter(RESEARCH_PROJECT_PARAMETER, researchProject)
                .addParameter(BIOPROJECT_PARAMETER, bioProjectTokenInput.getListOfKeys())
                .addParameter(LIBRARY_DESCRIPTOR_PARAMETER, selectedSubmissionLibraryDescriptor)
                .addParameter(REPOSITORY_PARAMETER, selectedSubmissionRepository)
                .addParameter(SUBMISSION_TUPLES_PARAMETER, selectedSubmissionTuples)
                .addParameter(RESEARCH_PROJECT_TAB_PARAMETER, RESEARCH_PROJECT_SUBMISSIONS_TAB);
    }

    /**
     * Update the actionBean's UUIDs with values in provided selectedSubmissions
     */
    private void updateUuid(List<SubmissionDto> selectedSubmissions) {
        populateSubmissionSamples(false);

        Map<SubmissionTuple, String> tupleToUuidMap = new HashMap<>(submissionSamples.size());
        for (SubmissionDto selectedSubmission : selectedSubmissions) {
            SubmissionTuple submissionTuple = selectedSubmission.getSubmissionTuple();
            if (submissionTuple != null && StringUtils.isNotBlank(selectedSubmission.getUuid())) {
                tupleToUuidMap.put(submissionTuple, selectedSubmission.getUuid());
            }
        }

        for (SubmissionDto submissionSample : submissionSamples) {
            String uuid = tupleToUuidMap.get(submissionSample.getSubmissionTuple());
            if (uuid != null) {
                submissionSample.setUuid(uuid);
            }
        }
        sessionCache.put(researchProject, submissionSamples);
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

    @Inject
    public void setCohortsList(CohortTokenInput cohortsList) {
        this.cohortsList = cohortsList;
    }

    public FundingTokenInput getFundingSourceList() {
        return fundingSourceList;
    }

    @Inject
    public void setFundingSourceList(FundingTokenInput fundingSourceList) {
        this.fundingSourceList = fundingSourceList;
    }

    public UserTokenInput getBroadPiList() {
        return broadPiList;
    }

    @Inject
    void setBroadPiList(UserTokenInput broadPiList) {
        this.broadPiList = broadPiList;
    }

    public UserTokenInput getExternalCollaboratorList() {
        return externalCollaboratorList;
    }

    @Inject
    public void setExternalCollaboratorList(UserTokenInput externalCollaboratorList) {
        this.externalCollaboratorList = externalCollaboratorList;
    }

    public UserTokenInput getScientistList() {
        return scientistList;
    }

    @Inject
    public void setScientistList(UserTokenInput scientistList) {
        this.scientistList = scientistList;
    }

    public UserTokenInput getProjectManagerList() {
        return projectManagerList;
    }

    @Inject
    public void setProjectManagerList(UserTokenInput projectManagerList) {
        this.projectManagerList = projectManagerList;
    }

    public UserTokenInput getOtherUserList() {
        return otherUserList;
    }

    @Inject
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
        UserBean userBean = getUserBean();
        return isEditAllowed(userBean);
    }

    public static boolean isEditAllowed(UserBean userBean) {
        return userBean.isDeveloperUser() || userBean.isPMUser() || userBean.isPDMUser() || userBean.isGPPMUser();
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

    public List<SubmissionDto> getSubmissionSamples() {
        return submissionSamples;
    }

    public void setSubmissionSamples(List<SubmissionDto> submissionSamples) {
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

    @Inject
    public void setBioProjectTokenInput(BioProjectTokenInput bioProjectTokenInput) {
        this.bioProjectTokenInput = bioProjectTokenInput;
    }

    public List<SubmissionTuple> getSelectedSubmissionTuples() {
        return selectedSubmissionTuples;
    }

    public void setSelectedSubmissionTuples(List<SubmissionTuple> selectedSubmissionTuples) {
        this.selectedSubmissionTuples = selectedSubmissionTuples;
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
        if (isResearchOnly()) {
            return getUserBean().isDeveloperUser() || getUserBean().isPMUser() || getUserBean().isGPPMUser();
        }
        return false;
    }

    /**
     * Returns true if the research project allows submissions.
     *
     * Currently only "Research Grade" projects are allowed to submit data.
     */
    public boolean isProjectAllowsSubmission() {
        return isResearchOnly();
    }

    private boolean isResearchOnly() {
        return editResearchProject != null && editResearchProject.isResearchOnly();
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

    public String getSubmissionTabHelpText() {
        if (validateViewOrPostSubmissions(false)) {
            return "Click to view data submissions";
        } else {
            return StringUtils.join(getFormattedErrors(), "<br/>");
        }
    }

    @Inject
    public void setProjectTokenInput(ProjectTokenInput projectTokenInput) {
        this.projectTokenInput = projectTokenInput;
    }

    @Inject
    public void setRegulatoryInfoEjb(RegulatoryInfoEjb regulatoryInfoEjb) {
        this.regulatoryInfoEjb = regulatoryInfoEjb;
    }

    @Inject
    public void setSubmissionDtoFetcher(SubmissionDtoFetcher submissionDtoFetcher) {
        this.submissionDtoFetcher = submissionDtoFetcher;
    }

    @Inject
    public void setOrspProjectDao(OrspProjectDao orspProjectDao) {
        this.orspProjectDao = orspProjectDao;
    }

    @Inject
    public void setAlignerDao(AlignerDao alignerDao) {
        this.alignerDao = alignerDao;
    }

    @Inject
    public void setProductOrderDao(ProductOrderDao productOrderDao) {
        this.productOrderDao = productOrderDao;
    }

    @Inject
    public void setResearchProjectEjb(ResearchProjectEjb researchProjectEjb) {
        this.researchProjectEjb = researchProjectEjb;
    }

    @Inject
    public void setReferenceSequenceDao(ReferenceSequenceDao referenceSequenceDao) {
        this.referenceSequenceDao = referenceSequenceDao;
    }

    @Inject
    public void setCohortList(BSPCohortList cohortList) {
        this.cohortList = cohortList;
    }

    @Inject
    public void setCollaborationService(CollaborationService collaborationService) {
        this.collaborationService = collaborationService;
    }

    void setEditResearchProject(ResearchProject editResearchProject) {
        this.editResearchProject = editResearchProject;
    }

    @Inject
    void setUserBean(UserBean userBean) {
        this.userBean = userBean;
    }

    @Inject
    void setBspUserList(BSPUserList bspUserList) {
        this.bspUserList = bspUserList;
    }

    @Inject
    void setSubmissionsService(SubmissionsService submissionsService) {
        this.submissionsService = submissionsService;
    }

    public boolean isSupressValidationErrors() {
        return supressValidationErrors;
    }

    public void setSupressValidationErrors(boolean supressValidationErrors) {
        this.supressValidationErrors = supressValidationErrors;
    }

    @Inject
    public void setResearchProjectDao(ResearchProjectDao researchProjectDao) {
        this.researchProjectDao = researchProjectDao;
    }

    @Inject
    public void setBioProjectList(BioProjectList bioProjectList) {
        this.bioProjectList = bioProjectList;
    }

    public QuoteService getQuoteService() {
        return quoteService;
    }

    @Inject
    public void setQuoteService(QuoteService quoteService) {
        this.quoteService = quoteService;
    }
}

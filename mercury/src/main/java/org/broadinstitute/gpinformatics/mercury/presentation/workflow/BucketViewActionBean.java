package org.broadinstitute.gpinformatics.mercury.presentation.workflow;

import com.google.common.collect.HashMultimap;
import com.google.common.collect.Multimap;
import net.sourceforge.stripes.action.After;
import net.sourceforge.stripes.action.Before;
import net.sourceforge.stripes.action.DefaultHandler;
import net.sourceforge.stripes.action.ForwardResolution;
import net.sourceforge.stripes.action.HandlesEvent;
import net.sourceforge.stripes.action.Resolution;
import net.sourceforge.stripes.action.StreamingResolution;
import net.sourceforge.stripes.action.UrlBinding;
import net.sourceforge.stripes.controller.LifecycleStage;
import net.sourceforge.stripes.validation.Validate;
import net.sourceforge.stripes.validation.ValidationMethod;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.broadinstitute.gpinformatics.athena.boundary.orders.ProductOrderEjb;
import org.broadinstitute.gpinformatics.athena.control.dao.orders.ProductOrderDao;
import org.broadinstitute.gpinformatics.athena.control.dao.preference.PreferenceDao;
import org.broadinstitute.gpinformatics.athena.control.dao.preference.PreferenceEjb;
import org.broadinstitute.gpinformatics.athena.entity.orders.ProductOrder;
import org.broadinstitute.gpinformatics.athena.entity.preference.NameValueDefinitionValue;
import org.broadinstitute.gpinformatics.athena.entity.preference.Preference;
import org.broadinstitute.gpinformatics.athena.entity.preference.PreferenceType;
import org.broadinstitute.gpinformatics.athena.presentation.tokenimporters.BucketEntryProductOrderTokenInput;
import org.broadinstitute.gpinformatics.athena.presentation.tokenimporters.JiraUserTokenInput;
import org.broadinstitute.gpinformatics.athena.presentation.tokenimporters.MaterialTypeTokenInput;
import org.broadinstitute.gpinformatics.infrastructure.ValidationException;
import org.broadinstitute.gpinformatics.infrastructure.jira.issue.CreateFields;
import org.broadinstitute.gpinformatics.mercury.boundary.bucket.BucketEjb;
import org.broadinstitute.gpinformatics.mercury.boundary.vessel.LabBatchEjb;
import org.broadinstitute.gpinformatics.mercury.control.dao.bucket.BucketDao;
import org.broadinstitute.gpinformatics.mercury.control.dao.bucket.BucketEntryDao;
import org.broadinstitute.gpinformatics.mercury.control.dao.rapsheet.ReworkEjb;
import org.broadinstitute.gpinformatics.mercury.control.dao.vessel.LabVesselDao;
import org.broadinstitute.gpinformatics.mercury.control.dao.workflow.LabBatchDao;
import org.broadinstitute.gpinformatics.mercury.entity.bucket.Bucket;
import org.broadinstitute.gpinformatics.mercury.entity.bucket.BucketCount;
import org.broadinstitute.gpinformatics.mercury.entity.bucket.BucketEntry;
import org.broadinstitute.gpinformatics.mercury.entity.vessel.LabVessel;
import org.broadinstitute.gpinformatics.mercury.entity.vessel.MaterialType;
import org.broadinstitute.gpinformatics.mercury.entity.workflow.LabBatch;
import org.broadinstitute.gpinformatics.mercury.entity.workflow.ProductWorkflowDef;
import org.broadinstitute.gpinformatics.mercury.entity.workflow.ProductWorkflowDefVersion;
import org.broadinstitute.gpinformatics.mercury.entity.workflow.Workflow;
import org.broadinstitute.gpinformatics.mercury.entity.workflow.WorkflowBucketDef;
import org.broadinstitute.gpinformatics.mercury.entity.workflow.WorkflowConfig;
import org.broadinstitute.gpinformatics.mercury.presentation.CoreActionBean;
import org.broadinstitute.gpinformatics.mercury.presentation.UserBean;
import org.broadinstitute.gpinformatics.mercury.presentation.datatables.Column;
import org.broadinstitute.gpinformatics.mercury.presentation.datatables.State;
import org.codehaus.jackson.map.ObjectMapper;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.jvnet.inflector.Noun;

import javax.inject.Inject;
import java.io.IOException;
import java.io.StringReader;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.stream.Collectors;

@UrlBinding(value = "/workflow/bucketView.action")
public class BucketViewActionBean extends CoreActionBean {
    private static final String VIEW_PAGE = "/workflow/bucket_view.jsp";
    private static final String SEARCH_PAGE = "/workflow/bucket_search.jsp";
    private static final String ADD_TO_BATCH_ACTION = "addToBatch";
    private static final String CREATE_BATCH_ACTION = "createBatch";
    public static final String FIND_BUCKET_ENTRIES = "findBucketEntries";
    private static final String EXISTING_TICKET = "existingTicket";
    private static final String NEW_TICKET = "newTicket";
    private static final String REWORK_CONFIRMED_ACTION = "reworkConfirmed";
    private static final String REMOVE_FROM_BUCKET_ACTION = "removeFromBucket";
    private static final String CONFIRM_REMOVE_FROM_BUCKET_ACTION = "confirmRemoveFromBucket";
    private static final String CHANGE_PDO = "changePdo";
    private static final String FIND_PDO = "findPdo";
    public static final String VIEW_BUCKET_ACTION = "viewBucket";
    public static final String SEARCH_BUCKET_ACTION = "searchBucket";
    public static final String SAVE_SEARCH_DATA = "saveSearchData";
    public static final String LOAD_SEARCH_DATA = "loadSearchData";
    public static final String SELECTED_BUCKET_KEY = "selectedBucket";
    public static final String TABLE_STATE_KEY = "tableState";
    public static final String SELECT_NEXT_SIZE = "selectNextSize";

    @Inject
    private JiraUserTokenInput jiraUserTokenInput;
    @Inject
    private BucketEntryProductOrderTokenInput productOrderTokenInput;
    @Inject
    private MaterialTypeTokenInput materialTypeTokenInput;
    @Inject
    private WorkflowConfig workflowConfig;
    @Inject
    private BucketDao bucketDao;
    @Inject
    private ProductOrderDao productOrderDao;
    @Inject
    private LabBatchEjb labBatchEjb;
    @Inject
    private BucketEjb bucketEjb;
    @Inject
    private ReworkEjb reworkEjb;
    @Inject
    private ProductOrderEjb productOrderEjb;
    @Inject
    private UserBean userBean;
    @Inject
    private LabBatchDao labBatchDao;
    @Inject
    private BucketEntryDao bucketEntryDao;
    @Inject
    LabVesselDao labVesselDao;
    @Inject
    private PreferenceEjb preferenceEjb;
    @Inject
    private PreferenceDao preferenceDao;

    private static final Log log = LogFactory.getLog(BucketViewActionBean.class);

    @Validate(required = true, on = {CREATE_BATCH_ACTION})
    private String selectedBucket;
    private String selectedWorkflow;
    private Bucket bucket;
    private String jiraTicketId;
    private final Set<String> buckets = new HashSet<>();
    private final List<Long> bucketEntryIds = new ArrayList<>();
    private final List<Long> reworkEntryIds = new ArrayList<>();
    private Set<BucketEntry> collectiveEntries = new HashSet<>();
    private final Map<String, String> mapBucketToJiraProject = new HashMap<>();
    private Map<String, Collection<String>> mapBucketToWorkflows;
    private List<BucketEntry> selectedEntries = new ArrayList<>();

    private String selectedPdo;
    private List<ProductOrder> availablePdos;
    private List<Long> selectedEntryIds = new ArrayList<>();
    private Set<String> possibleWorkflows = new TreeSet<>();

    private String important;
    private String description;
    private String summary;
    private Date dueDate;
    private String selectedLcset;
    private LabBatch batch;
    private String jiraUserQuery;
    private Map<String, BucketCount> mapBucketToBucketEntryCount;
    private CreateFields.ProjectType projectType = null;

    // tableState is a JSON structure used by DataTables which stores the state of a table (sorting, visibility, etc)
    private String tableState = "{}";
    private int selectNextSize = 92;
    private String searchKey;
    private static final ObjectMapper objectMapper = new ObjectMapper();
    private String searchString;

    public String getSlowColumns() {
        return new JSONArray(Arrays.asList("Material Type", "Workflow", "Receipt Date", "Rework Reason",
            "Rework Comment", "Rework User", "Rework Date")).toString();
    }

    private Map<String, Boolean> headerVisibilityMap = new HashMap<>();

    @Before(stages = LifecycleStage.BindingAndValidation, on = SEARCH_BUCKET_ACTION)
    public void initSearchBucket() {
        materialTypeTokenInput.setup();
    }

    @Before(stages = LifecycleStage.BindingAndValidation, on = {SEARCH_BUCKET_ACTION, VIEW_BUCKET_ACTION})
    public void loadTableState() {
        try {
            NameValueDefinitionValue nameValueDefinitionValue = loadSearchData();
            List<String> selectedBucketPreferenceValue = nameValueDefinitionValue.getDataMap().get(SELECTED_BUCKET_KEY);
            if (CollectionUtils.isNotEmpty(selectedBucketPreferenceValue)) {
                selectedBucket = selectedBucketPreferenceValue.iterator().next();
            }
            List<String> tableStatePreferenceValue = nameValueDefinitionValue.getDataMap().get(TABLE_STATE_KEY);
            if (CollectionUtils.isNotEmpty(tableStatePreferenceValue)) {
                tableState = tableStatePreferenceValue.iterator().next();
                State state = objectMapper.readValue(tableState, State.class);
                buildHeaderVisibilityMap(state);
            }
            List<String> selectNextPreferenceValue = nameValueDefinitionValue.getDataMap().get(SELECT_NEXT_SIZE);
            if (CollectionUtils.isNotEmpty(selectNextPreferenceValue)) {
                selectNextSize = Integer.parseInt(selectNextPreferenceValue.iterator().next());
            }
        } catch (Exception e) {
            log.error("Load table state preference", e);
        }

    }

    @Before(stages = LifecycleStage.BindingAndValidation)
    public void init() {
        // Gets bucket names for supported products (workflows), and associates workflow(s) for each bucket.
        Multimap<String, String> bucketWorkflows = HashMultimap.create();
        for (Workflow workflow : Workflow.SUPPORTED_WORKFLOWS) {
            ProductWorkflowDef workflowDef = workflowConfig.getWorkflowByName(workflow.getWorkflowName());
            ProductWorkflowDefVersion workflowVersion = workflowDef.getEffectiveVersion();
            for (WorkflowBucketDef bucket : workflowVersion.getCreationBuckets()) {
                String bucketName = bucket.getName();
                // The jiraProjectType is stored in ProductWorkflowDefVersion but can be overridden in WorkflowBucketDef
                // so check there first.
                String jiraProjectType = bucket.getBatchJiraProjectType();
                if (StringUtils.isBlank(jiraProjectType)) {
                    jiraProjectType = workflowVersion.getProductWorkflowDefBatchJiraProjectType();
                }
                buckets.add(bucketName);
                mapBucketToJiraProject.put(bucketName, jiraProjectType);
                bucketWorkflows.put(bucketName, workflow.getWorkflowName());
            }
        }
        mapBucketToWorkflows = bucketWorkflows.asMap();
    }

    @After(stages = LifecycleStage.BindingAndValidation)
    public void initBucket() {
        if (selectedBucket != null) {
            bucket = bucketDao.findByName(selectedBucket);
            possibleWorkflows.addAll(mapBucketToWorkflows.get(selectedBucket));
        }
    }

    private void buildHeaderVisibilityMap(State state) {
        for (Column column : state.getColumns()) {
            boolean visible = true;
            if (column!=null){
                visible = column.isVisible();
            }
            String headerName = column.getHeaderName();
            if (StringUtils.isNotBlank(headerName)) {
                headerVisibilityMap.put(headerName, visible);
            }
        }
    }

    @Before(stages = LifecycleStage.ResolutionExecution)
    public void updateBucketCounts() {
        mapBucketToBucketEntryCount = initBucketCountsMap(bucketEntryDao.getBucketCounts());
    }

    @HandlesEvent("watchersAutoComplete")
    public Resolution watchersAutoComplete() throws JSONException {
        return createTextResolution(jiraUserTokenInput.getJsonString(getJiraUserQuery()));
    }

    @HandlesEvent("materialTypeAutoComplete")
    public Resolution materialTypeAutoComplete() throws JSONException {
        return createTextResolution(materialTypeTokenInput.getJsonString(searchKey));
    }

    @HandlesEvent("productOrderAutoComplete")
    public Resolution productOrderAutoComplete() throws JSONException {
        return createTextResolution(productOrderTokenInput.getJsonString(bucket, searchKey));
    }

    @ValidationMethod(on = CREATE_BATCH_ACTION)
    public void createBatchValidation() {
        if (StringUtils.isBlank(selectedWorkflow)) {
            addValidationError("selectedWorkflow", "You must choose a workflow to create a batch");
            viewBucket();
        }
        if (!getUserBean().isValidJiraUser()) {
            addValidationError("jiraTicketId", "You must be A valid Jira user to create a batch.");
            viewBucket();
        }

        if (CollectionUtils.isEmpty(selectedEntryIds)) {
            addValidationError("selectedEntryIds",
                               "At least one vessel or rework must be selected to create a batch.");
            viewBucket();
        }

        if (StringUtils.isBlank(summary)) {
            addValidationError("summary", "You must provide at least a summary to create a Jira Ticket.");
            viewBucket();
        }
    }

    @ValidationMethod(on = ADD_TO_BATCH_ACTION)
    public void addReworkToBatchValidation() {
        if (StringUtils.isBlank(selectedLcset)) {
            addValidationError("selectedLcset", "You must provide an LCSET to add to a batch.");
            entrySearch();
        }
    }

    @ValidationMethod(on = {ADD_TO_BATCH_ACTION})
    public void batchValidation() {
        if (CollectionUtils.isEmpty(selectedEntryIds)) {
            addValidationError("selectedEntryIds", "At least one item must be selected.");
            addValidationError("bucketEntryView", "At least one sample must be selected to add to the batch.");
            entrySearch();
        }
    }

    @ValidationMethod(on = REMOVE_FROM_BUCKET_ACTION)
    public void removeSampleFromBucketValidation() {
        if (CollectionUtils.isEmpty(selectedEntryIds)) {
            addValidationError("bucketEntryView", "At least one sample must be selected to remove from the bucket.");
            viewBucket();
        }
    }

    @HandlesEvent(SAVE_SEARCH_DATA)
    public Resolution saveSearchData() throws Exception {
        JSONObject jsonObject = new JSONObject(tableState);
        State state = objectMapper.readValue(tableState, State.class);
        if (state.getColumns() == null) {
            state = new State();
        }
        saveSearchData(state);
        return new StreamingResolution("application/json", jsonObject.toString());
    }

    public void saveSearchData(State state) throws Exception {
        NameValueDefinitionValue definitionValue = loadSearchData();
        if (selectedBucket != null) {
            definitionValue.put(SELECTED_BUCKET_KEY, selectedBucket);
        }
        definitionValue.put(SELECT_NEXT_SIZE, String.valueOf(selectNextSize));
        if (state.getLength() > 0) {
            definitionValue.put(TABLE_STATE_KEY, Collections.singletonList(objectMapper.writeValueAsString(state)));
        }

        preferenceEjb.add(userBean.getBspUser().getUserId(), PreferenceType.BUCKET_PREFERENCES, definitionValue);
    }


    public NameValueDefinitionValue loadSearchData() throws Exception {
        List<Preference> preferences = preferenceDao.getPreferences(getUserBean().getBspUser().getUserId(),
                PreferenceType.BUCKET_PREFERENCES);
        if (!preferences.isEmpty()) {
            Preference preference = preferences.iterator().next();
            try {
                NameValueDefinitionValue definitionValue =
                        (NameValueDefinitionValue) preference.getPreferenceDefinition().getDefinitionValue();

                return definitionValue;
            } catch (Throwable t) {
                addMessage("Could not read search preference");
            }
        }
        return new NameValueDefinitionValue();
    }

    @DefaultHandler
    @HandlesEvent(SEARCH_BUCKET_ACTION)
    public Resolution searchBucketEntries() {
        return new ForwardResolution(SEARCH_PAGE);
    }

    @HandlesEvent(VIEW_BUCKET_ACTION)
    public Resolution viewBucket() {
        return new ForwardResolution(VIEW_PAGE);
    }

    @HandlesEvent(FIND_BUCKET_ENTRIES)
    public Resolution entrySearch() {
        List<String> searchStrings = new ArrayList<>();
        if (StringUtils.isNotBlank(searchString)) {
            searchStrings = Arrays.stream(searchString.split("[,|\\s]+")).collect(Collectors.toList());
        }
        Set<BucketEntry> bucketEntries =
            new HashSet<>(bucketEntryDao.findBucketEntries(bucket, productOrderTokenInput.getTokenBusinessKeys(),
                searchStrings));
        if (bucketEntries.isEmpty()) {

            // If we got here by hitting the "search" button then we were expecting results and didn't find any.
            if (StringUtils.isNotBlank(getContext().getRequest().getParameter(FIND_BUCKET_ENTRIES))) {
                addGlobalValidationError("No bucket entries found matching this search criteria.");
            }
            return new ForwardResolution(SEARCH_PAGE);
        }
        String jiraProjectType = mapBucketToJiraProject.get(selectedBucket);
        projectType = CreateFields.ProjectType.fromKeyPrefix(jiraProjectType);
        preFetchSampleData(bucketEntries);
        List<MaterialType> selectedMaterialTypes = materialTypeTokenInput.getTokenObjects();

        if (CollectionUtils.isNotEmpty(selectedMaterialTypes)) {
            long begin = System.currentTimeMillis();
            collectiveEntries.addAll(bucketEntries.stream()
                .filter(o -> selectedMaterialTypes.contains(o.getLabVessel().getLatestMaterialType()))
                .collect(Collectors.toList()));
            log.info(String.format("contains material type took %d", System.currentTimeMillis() - begin));
        } else {
            collectiveEntries.addAll(bucketEntries);
        }
        return new ForwardResolution(VIEW_PAGE);
    }

    private void preFetchSampleData(Set<BucketEntry> collectiveEntries) {
        Set<LabVessel> labVessels = new HashSet<>();
        for (BucketEntry entry : collectiveEntries) {
            labVessels.add(entry.getLabVessel());
        }
        LabVessel.loadSampleDataForBuckets(labVessels);
    }

    public String getConfirmationPageTitle() {
        return String.format("Confirm adding %d new and %d rework entries to %s.",
                bucketEntryIds.size(), reworkEntryIds.size(), selectedBucket);
    }

    private void loadReworkVessels() {
        batch = labBatchDao.findByBusinessKey(selectedLcset);
        separateEntriesByType();
    }

    @HandlesEvent(ADD_TO_BATCH_ACTION)
    public Resolution addToBatch() {
        separateEntriesByType();

        try {
            labBatchEjb.updateLabBatch(selectedLcset, bucketEntryIds, reworkEntryIds, Collections.<Long>emptyList(),
                    selectedBucket, this, jiraUserTokenInput.getTokenBusinessKeys());

            // clears tokenInput selections when the page returns
            jiraUserTokenInput.setup();
        } catch (IOException e) {
            addGlobalValidationError("IOException contacting JIRA service." + e.getMessage());
            return new ForwardResolution(VIEW_PAGE);
        } catch (ValidationException e) {
            addGlobalValidationError(e.getMessage());
            return entrySearch();
        }

        addMessage(String.format("Successfully added %d %s and %d %s to batch '%s' from bucket '%s'.",
                bucketEntryIds.size(), Noun.pluralOf("sample", bucketEntryIds.size()),
                reworkEntryIds.size(), Noun.pluralOf("rework", reworkEntryIds.size()),
                getLink(selectedLcset), selectedBucket));
        return entrySearch();
    }

    /**
     * Supports the submission for the page.  Will forward to confirmation page on success
     *
     * @return The resolution
     */
    @HandlesEvent(CREATE_BATCH_ACTION)
    public Resolution createBatch() {
        separateEntriesByType();
        try {
            batch = labBatchEjb.createLabBatchAndRemoveFromBucket(LabBatch.LabBatchType.WORKFLOW, selectedWorkflow,
                    bucketEntryIds, reworkEntryIds, summary.trim(), description, dueDate, important,
                    userBean.getBspUser().getUsername(), selectedBucket, this,
                    jiraUserTokenInput.getTokenBusinessKeys());
        } catch (ValidationException e) {
            addGlobalValidationError(e.getMessage());
            return viewBucket();
        }
        String batchName = batch.getJiraTicket().getTicketName();
        String link = getLink(batchName);
        addMessage(MessageFormat.format("Lab batch ''{0}'' has been created.", link));
        return entrySearch();
    }

    public String getLink(String batchName) {
        String jiraUrl = jiraUrl(batchName);
        return String.format("<a target='JIRA' title='%s' href='%s' class='external'>%s</a>", batchName, jiraUrl,
                batchName);
    }

    @HandlesEvent(REMOVE_FROM_BUCKET_ACTION)
    public Resolution removeFromBucket() {
        separateEntriesByType();
        bucketEjb.removeEntriesByIds(selectedEntryIds, "");

        addMessage(String.format("Successfully removed %d sample(s) and %d rework(s) from bucket '%s'.",
                                 bucketEntryIds.size(), reworkEntryIds.size(), selectedBucket));

        return viewBucket();
    }

    public Set<String> findPotentialPdos() {
        // Use a TreeSet so it can returned sorted;
        return new TreeSet<>(reworkEjb.findBucketCandidatePdos(selectedEntryIds));
    }

    private void separateEntriesByType() {
        // Iterate through the selected entries and separate the pdo entries from rework entries.
        selectedEntries = bucketEntryDao.findByIds(selectedEntryIds);
        for (BucketEntry entry : selectedEntries) {
            switch (entry.getEntryType()) {
            case PDO_ENTRY:
                bucketEntryIds.add(entry.getBucketEntryId());
                break;
            case REWORK_ENTRY:
                reworkEntryIds.add(entry.getBucketEntryId());
                break;
            }
        }
    }

    @HandlesEvent(FIND_PDO)
    public Resolution findPdoForVessel() throws JSONException {
        JSONArray pdoArray = new JSONArray(findPotentialPdos());
        return new StreamingResolution("text", new StringReader(pdoArray.toString()));
    }

    @HandlesEvent(CHANGE_PDO)
    public Resolution changePdo() throws JSONException {
        String newPdoValue = getContext().getRequest().getParameter("newPdoValue");
        List<BucketEntry> bucketEntries = bucketEntryDao.findByIds(selectedEntryIds);
        bucketEjb.updateEntryPdo(bucketEntries, newPdoValue);

        ProductOrder newPdo = productOrderDao.findByBusinessKey(newPdoValue);
        JSONObject newPdoValues = new JSONObject();

        newPdoValues.put("jiraKey", newPdoValue);
        newPdoValues.put("pdoOwner", getUserFullName(newPdo.getCreatedBy()));
        newPdoValues.put("pdoTitle", newPdo.getTitle());

        return new StreamingResolution("text", new StringReader(newPdoValues.toString()));
    }

    private Map<String, BucketCount> initBucketCountsMap(Map<String, BucketCount> bucketCountMap) {
        Map<String, BucketCount> resultBucketCountMap = new TreeMap<>();
        for (Workflow workflow : Workflow.SUPPORTED_WORKFLOWS) {
            ProductWorkflowDef workflowDef = workflowConfig.getWorkflowByName(workflow.getWorkflowName());
            ProductWorkflowDefVersion workflowVersion = workflowDef.getEffectiveVersion();
            for (WorkflowBucketDef bucket : workflowVersion.getCreationBuckets()) {
                BucketCount bucketCount = bucketCountMap.get(bucket.getName());
                if (bucketCount == null) {
                    bucketCount = new BucketCount(bucket.getName());
                }
                resultBucketCountMap.put(bucket.getName(), bucketCount);
            }
        }

        return resultBucketCountMap;
    }


    public String getSelectedBucket() {
        return selectedBucket;
    }

    public void setSelectedBucket(String selectedBucket) {
        this.selectedBucket = selectedBucket;
    }

    public String getSelectedLcset() {
        return selectedLcset;
    }

    public void setSelectedLcset(String selectedLcset) {
        this.selectedLcset = selectedLcset;
    }

    public Collection<BucketEntry> getCollectiveEntries() {
        return collectiveEntries;
    }

    public List<BucketEntry> getSelectedEntries() {
        return selectedEntries;
    }

    public Date getDueDate() {
        return dueDate;
    }

    public String getImportant() {
        return important;
    }

    public String getDescription() {
        return description;
    }

    public String getSummary() {
        return summary;
    }

    public List<Long> getSelectedEntryIds() {
        return selectedEntryIds;
    }

    public void setSelectedEntryIds(List<Long> selectedEntryIds) {
        this.selectedEntryIds = selectedEntryIds;
    }

    public String getExistingJiraTicketValue() {
        return EXISTING_TICKET;
    }

    public String getNewJiraTicketValue() {
        return NEW_TICKET;
    }

    public void setSummary(String summary) {
        this.summary = summary;
    }

    public void setDueDate(Date dueDate) {
        this.dueDate = dueDate;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public void setImportant(String important) {
        this.important = important;
    }

    public String getJiraTicketId() {
        return jiraTicketId;
    }

    public void setJiraTicketId(String jiraTicketId) {
        this.jiraTicketId = jiraTicketId;
    }

    public List<Long> getBucketEntryIds() {
        return bucketEntryIds;
    }

    public List<Long> getReworkEntryIds() {
        return reworkEntryIds;
    }

    public LabBatch getBatch() {
        return batch;
    }

    public void setBatch(LabBatch batch) {
        this.batch = batch;
    }

    public void setSelectedWorkflow(String selectedWorkflow) {
        this.selectedWorkflow = selectedWorkflow;
    }

    public String getSelectedWorkflow() {
        return selectedWorkflow;
    }

    public Set<String> getPossibleWorkflows() {
        return possibleWorkflows;
    }

    public Map<String, BucketCount> getMapBucketToBucketEntryCount() {
        return mapBucketToBucketEntryCount;
    }

    public Resolution projectTypeMatches() {
        return new StreamingResolution("text/plain",
                String.valueOf(projectType == CreateFields.ProjectType.fromIssueKey(jiraTicketId)));
    }

    public CreateFields.ProjectType getProjectType() {
        return projectType;
    }

    public void setProjectType(CreateFields.ProjectType projectType) {
        this.projectType = projectType;
    }

    public String getJiraUserQuery() {
        return jiraUserQuery;
    }

    public void setJiraUserQuery(String jiraUserQuery) {
        this.jiraUserQuery = jiraUserQuery;
    }

    public JiraUserTokenInput getJiraUserTokenInput() {
        return jiraUserTokenInput;
    }

    public void setJiraUserTokenInput(JiraUserTokenInput jiraUserTokenInput) {
        this.jiraUserTokenInput = jiraUserTokenInput;
    }

    public BucketEntryProductOrderTokenInput getProductOrderTokenInput() {
        return productOrderTokenInput;
    }

    public void setProductOrderTokenInput(BucketEntryProductOrderTokenInput productOrderTokenInput) {
        this.productOrderTokenInput = productOrderTokenInput;
    }

    public MaterialTypeTokenInput getMaterialTypeTokenInput() {
        return materialTypeTokenInput;
    }

    public void setMaterialTypeTokenInput(MaterialTypeTokenInput materialTypeTokenInput) {
        this.materialTypeTokenInput = materialTypeTokenInput;
    }

    public String getTableState() {
        return tableState;
    }

    public void setTableState(String tableState) {
        this.tableState = tableState;
    }

    public int getSelectNextSize() {
        return selectNextSize;
    }

    public void setSelectNextSize(int selectNextSize) {
        this.selectNextSize = selectNextSize;
    }

    public String getSearchKey() {
        return searchKey;
    }

    public void setSearchKey(String searchKey) {
        this.searchKey = searchKey;
    }

    public String getSearchString() {
        return searchString;
    }

    public void setSearchString(String searchString) {
        this.searchString = searchString;
    }

    public List<String> bucketWorkflowNames(BucketEntry bucketEntry) {
        List<String> workflowNames = new ArrayList<>();
        for (String workflow : bucketEntry.getWorkflows(workflowConfig)) {
            workflowNames.add(workflow);
        }
        return workflowNames;
    }

    public boolean showHeader(String columnName) {
        return headerVisibilityMap.isEmpty() || (headerVisibilityMap.get(columnName) != null && headerVisibilityMap
            .get(columnName));
    }

}

package org.broadinstitute.gpinformatics.mercury.presentation.workflow;

import com.google.common.collect.HashMultimap;
import com.google.common.collect.Multimap;
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
import org.broadinstitute.gpinformatics.athena.boundary.orders.ProductOrderEjb;
import org.broadinstitute.gpinformatics.athena.control.dao.orders.ProductOrderDao;
import org.broadinstitute.gpinformatics.athena.entity.orders.ProductOrder;
import org.broadinstitute.gpinformatics.athena.presentation.tokenimporters.JiraUserTokenInput;
import org.broadinstitute.gpinformatics.infrastructure.ValidationException;
import org.broadinstitute.gpinformatics.mercury.boundary.bucket.BucketEjb;
import org.broadinstitute.gpinformatics.mercury.boundary.vessel.LabBatchEjb;
import org.broadinstitute.gpinformatics.mercury.control.dao.bucket.BucketDao;
import org.broadinstitute.gpinformatics.mercury.control.dao.bucket.BucketEntryDao;
import org.broadinstitute.gpinformatics.mercury.control.dao.rapsheet.ReworkEjb;
import org.broadinstitute.gpinformatics.mercury.control.dao.vessel.LabVesselDao;
import org.broadinstitute.gpinformatics.mercury.control.dao.workflow.LabBatchDao;
import org.broadinstitute.gpinformatics.mercury.control.workflow.WorkflowLoader;
import org.broadinstitute.gpinformatics.mercury.entity.bucket.Bucket;
import org.broadinstitute.gpinformatics.mercury.entity.bucket.BucketCount;
import org.broadinstitute.gpinformatics.mercury.entity.bucket.BucketEntry;
import org.broadinstitute.gpinformatics.mercury.entity.vessel.LabVessel;
import org.broadinstitute.gpinformatics.mercury.entity.workflow.LabBatch;
import org.broadinstitute.gpinformatics.mercury.entity.workflow.ProductWorkflowDef;
import org.broadinstitute.gpinformatics.mercury.entity.workflow.ProductWorkflowDefVersion;
import org.broadinstitute.gpinformatics.mercury.entity.workflow.Workflow;
import org.broadinstitute.gpinformatics.mercury.entity.workflow.WorkflowBucketDef;
import org.broadinstitute.gpinformatics.mercury.entity.workflow.WorkflowConfig;
import org.broadinstitute.gpinformatics.mercury.presentation.CoreActionBean;
import org.broadinstitute.gpinformatics.mercury.presentation.UserBean;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.jvnet.inflector.Noun;

import javax.inject.Inject;
import java.io.IOException;
import java.io.StringReader;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;

@UrlBinding(value = "/workflow/bucketView.action")
public class BucketViewActionBean extends CoreActionBean {
    private static final String VIEW_PAGE = "/workflow/bucket_view.jsp";
    private static final String ADD_TO_BATCH_ACTION = "addToBatch";
    private static final String CREATE_BATCH_ACTION = "createBatch";
    private static final String EXISTING_TICKET = "existingTicket";
    private static final String NEW_TICKET = "newTicket";
    private static final String REWORK_CONFIRMED_ACTION = "reworkConfirmed";
    private static final String REMOVE_FROM_BUCKET_ACTION = "removeFromBucket";
    private static final String CONFIRM_REMOVE_FROM_BUCKET_ACTION = "confirmRemoveFromBucket";
    private static final String CHANGE_PDO = "changePdo";
    private static final String FIND_PDO = "findPdo";
    public static final String VIEW_BUCKET_ACTION = "viewBucket";

    @Inject
    JiraUserTokenInput jiraUserTokenInput;
    @Inject
    private WorkflowLoader workflowLoader;
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
    @Validate(required = true, on = {CREATE_BATCH_ACTION})
    private String selectedBucket;
    private String selectedWorkflow;
    private Bucket bucket;
    private String jiraTicketId;
    private final Set<String> buckets = new HashSet<>();
    private final List<Long> bucketEntryIds = new ArrayList<>();
    private final List<Long> reworkEntryIds = new ArrayList<>();
    private final Set<BucketEntry> collectiveEntries = new HashSet<>();
    private final Map<String, WorkflowBucketDef> mapBucketToBucketDef = new HashMap<>();
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

    @Before(stages = LifecycleStage.BindingAndValidation)
    public void init() {
        WorkflowConfig workflowConfig = workflowLoader.load();
        // Gets bucket names for supported products (workflows), and associates workflow(s) for each bucket.
        Multimap<String, String> bucketWorkflows = HashMultimap.create();
        for (Workflow workflow : Workflow.SUPPORTED_WORKFLOWS) {
            ProductWorkflowDef workflowDef = workflowConfig.getWorkflowByName(workflow.getWorkflowName());
            ProductWorkflowDefVersion workflowVersion = workflowDef.getEffectiveVersion();
            for (WorkflowBucketDef bucket : workflowVersion.getCreationBuckets()) {
                String bucketName = bucket.getName();
                buckets.add(bucketName);
                mapBucketToBucketDef.put(bucketName, bucket);
                bucketWorkflows.put(bucketName, workflow.getWorkflowName());
            }
        }
        mapBucketToWorkflows = bucketWorkflows.asMap();
    }

    @Before(stages = LifecycleStage.ResolutionExecution)
    public void updateBucketCounts() {
        mapBucketToBucketEntryCount = initBucketCountsMap(bucketEntryDao.getBucketCounts());
    }

    @HandlesEvent("watchersAutoComplete")
    public Resolution watchersAutoComplete() throws JSONException {
        return createTextResolution(jiraUserTokenInput.getJsonString(getJiraUserQuery()));
    }

    @DefaultHandler
    public Resolution view() {
        return new ForwardResolution(VIEW_PAGE);
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
            viewBucket();
        }
    }

    @ValidationMethod(on = {ADD_TO_BATCH_ACTION})
    public void batchValidation() {
        if (CollectionUtils.isEmpty(selectedEntryIds)) {
            addValidationError("selectedEntryIds", "At least one item must be selected.");
            addValidationError("bucketEntryView", "At least one sample must be selected to add to the batch.");
            viewBucket();
        }
    }

    @ValidationMethod(on = REMOVE_FROM_BUCKET_ACTION)
    public void removeSampleFromBucketValidation() {
        if (CollectionUtils.isEmpty(selectedEntryIds)) {
            addValidationError("bucketEntryView", "At least one sample must be selected to remove from the bucket.");
            viewBucket();
        }
    }

    @HandlesEvent(VIEW_BUCKET_ACTION)
    public Resolution viewBucket() {
        if (selectedBucket != null) {
            bucket = bucketDao.findByName(selectedBucket);
            possibleWorkflows.addAll(mapBucketToWorkflows.get(selectedBucket));

            // Gets the bucket entries that are in the selected bucket.
            if (bucket != null) {
                collectiveEntries.addAll(bucket.getBucketEntries());
                collectiveEntries.addAll(bucket.getReworkEntries());
                preFetchSampleData(collectiveEntries);

                // preselect workflow if it is ambiguous.
                if (possibleWorkflows.size() == 1 && StringUtils.isBlank(selectedWorkflow)) {
                    selectedWorkflow = possibleWorkflows.iterator().next();
                }
            }
        }
        return view();
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
            labBatchEjb.addToLabBatch(selectedLcset, bucketEntryIds, reworkEntryIds, selectedBucket, this,
                    jiraUserTokenInput.getTokenBusinessKeys());

            // clears tokenInput selections when the page returns
            jiraUserTokenInput.setup();
        } catch (IOException e) {
            addGlobalValidationError("IOException contacting JIRA service." + e.getMessage());
            return new ForwardResolution(VIEW_PAGE);
        } catch (ValidationException e) {
            addGlobalValidationError(e.getMessage());
            return viewBucket();
        }

        addMessage(String.format("Successfully added %d %s and %d %s to batch '%s' from bucket '%s'.",
                bucketEntryIds.size(), Noun.pluralOf("sample", bucketEntryIds.size()),
                reworkEntryIds.size(), Noun.pluralOf("rework", reworkEntryIds.size()),
                getLink(selectedLcset), selectedBucket));
        return viewBucket();
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
            return view();
        }
        String batchName = batch.getJiraTicket().getTicketName();
        String link = getLink(batchName);
        addMessage(MessageFormat.format("Lab batch ''{0}'' has been created.", link));

        return viewBucket();
    }

    public String getLink(String batchName) {
        String jiraUrl = jiraUrl(batchName);;
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

    private static Map<String, BucketCount> initBucketCountsMap(Map<String, BucketCount> bucketCountMap) {
        Map<String, BucketCount> resultBucketCountMap = new TreeMap<>();
        WorkflowConfig workflowConfig = new WorkflowLoader().load();
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
}

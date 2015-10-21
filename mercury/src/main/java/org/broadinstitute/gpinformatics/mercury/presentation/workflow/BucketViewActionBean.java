package org.broadinstitute.gpinformatics.mercury.presentation.workflow;

import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.ListMultimap;
import net.sourceforge.stripes.action.Before;
import net.sourceforge.stripes.action.DefaultHandler;
import net.sourceforge.stripes.action.ForwardResolution;
import net.sourceforge.stripes.action.HandlesEvent;
import net.sourceforge.stripes.action.RedirectResolution;
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
import org.broadinstitute.gpinformatics.infrastructure.ValidationException;
import org.broadinstitute.gpinformatics.mercury.boundary.bucket.BucketEjb;
import org.broadinstitute.gpinformatics.mercury.boundary.vessel.LabBatchEjb;
import org.broadinstitute.gpinformatics.mercury.control.dao.bucket.BucketDao;
import org.broadinstitute.gpinformatics.mercury.control.dao.bucket.BucketEntryDao;
import org.broadinstitute.gpinformatics.mercury.control.dao.rapsheet.ReworkEjb;
import org.broadinstitute.gpinformatics.mercury.control.dao.workflow.LabBatchDao;
import org.broadinstitute.gpinformatics.mercury.control.workflow.WorkflowLoader;
import org.broadinstitute.gpinformatics.mercury.entity.bucket.Bucket;
import org.broadinstitute.gpinformatics.mercury.entity.bucket.BucketEntry;
import org.broadinstitute.gpinformatics.mercury.entity.sample.SampleInstance;
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
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

@UrlBinding(value = "/view/bucketView.action?{$event}")
public class BucketViewActionBean extends CoreActionBean {

    private static final String VIEW_PAGE = "/workflow/bucket_view.jsp";
    private static final String ADD_TO_BATCH_ACTION = "addToBatch";
    private static final String CONFIRMATION_PAGE = "/workflow/rework_confirmation.jsp";
    private static final String BATCH_CONFIRM_PAGE = "/batch/batch_confirm.jsp";
    private static final String EXISTING_TICKET = "existingTicket";
    private static final String NEW_TICKET = "newTicket";
    private static final String CREATE_BATCH_ACTION = "createBatch";
    private static final String REWORK_CONFIRMED_ACTION = "reworkConfirmed";
    private static final String REMOVE_FROM_BUCKET_ACTION = "removeFromBucket";
    private static final String REMOVE_FROM_BUCKET_CONFIRM_PAGE = "/workflow/remove_from_bucket_confirm.jsp";
    private static final String CONFIRM_REMOVE_FROM_BUCKET_ACTION = "confirmRemoveFromBucket";
    private static final String CHANGE_PDO = "changePdo";
    private static final String FIND_PDO = "findPdo";

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

    @Validate(required = true, on = {CREATE_BATCH_ACTION, "viewBucket"})
    private String selectedBucket;
    private ProductWorkflowDef selectedWorkflowDef;
    private Bucket bucket;

    private String jiraTicketId;

    private final Set<String> buckets = new HashSet<>();
    private final List<Long> bucketEntryIds = new ArrayList<>();
    private final List<Long> reworkEntryIds = new ArrayList<>();
    private final List<BucketEntry> bucketEntries = new ArrayList<>();
    private final List<BucketEntry> reworkEntries = new ArrayList<>();
    private final List<BucketEntry> collectiveEntries = new ArrayList<>();
    private final ListMultimap<String, ProductWorkflowDef> mapBucketToWorkflowDefs = ArrayListMultimap.create();
    private final Map<String, WorkflowBucketDef> mapBucketToBucketDef = new HashMap<>();

    private List<BucketEntry> selectedEntries = new ArrayList<>();
    private String selectedPdo;
    private List<ProductOrder> availablePdos;

    private List<Long> selectedEntryIds = new ArrayList<>();
    private List<ProductWorkflowDef> possibleWorkflows;

    private boolean jiraEnabled = false;
    private String important;
    private String description;
    private String summary;
    private Date dueDate;
    private String selectedLcset;
    private LabBatch batch;

    @Before(stages = LifecycleStage.BindingAndValidation)
    public void init() {
        WorkflowConfig workflowConfig = workflowLoader.load();
        // Gets bucket names for supported products (workflows), and associates workflow(s) for each bucket.
        for (Workflow workflow : Workflow.SUPPORTED_WORKFLOWS) {
            ProductWorkflowDef workflowDef = workflowConfig.getWorkflowByName(workflow.getWorkflowName());
            ProductWorkflowDefVersion workflowVersion = workflowDef.getEffectiveVersion();
            for (WorkflowBucketDef bucket : workflowVersion.getCreationBuckets()) {
                String bucketName = bucket.getName();
                buckets.add(bucketName);
                mapBucketToBucketDef.put(bucketName, bucket);
                mapBucketToWorkflowDefs.put(bucketName, workflowDef);
            }
        }
    }

    @DefaultHandler
    public Resolution view() {
        if (StringUtils.isNotBlank(selectedBucket)) {
            possibleWorkflows = mapBucketToWorkflowDefs.get(selectedBucket);
        }
        return new ForwardResolution(VIEW_PAGE);
    }

    @ValidationMethod(on = CREATE_BATCH_ACTION)
    public void createBatchValidation() {

        if (!getUserBean().isValidJiraUser()) {
            addValidationError("jiraTicketId", "You must be A valid Jira user to create an LCSet.");
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

    @HandlesEvent("viewBucket")
    public Resolution viewBucket() {
        if (selectedBucket != null) {
            bucket = bucketDao.findByName(selectedBucket);
            if (selectedWorkflowDef != null) {
                possibleWorkflows = mapBucketToWorkflowDefs.get(selectedBucket);

                // Gets the bucket entries that are in the selected bucket.
                if (bucket != null) {
                    bucket.initializeSampleData();
                    collectiveEntries.clear();

                    collectiveEntries.addAll(bucket.getBucketEntries());
                    collectiveEntries.addAll(bucket.getReworkEntries());


                    // Filters out entries whose product workflow or add-on's workflow doesn't match the selected workflow.
                    for (Iterator<BucketEntry> iter = collectiveEntries.iterator(); iter.hasNext(); ) {
                        BucketEntry entry = iter.next();
                        Workflow bucketWorkflow = entry.getWorkflow();
                        if (!selectedWorkflowDef.getName().equals(bucketWorkflow.getWorkflowName())) {
                            iter.remove();
                        }
                    }
                    // Doesn't show JIRA details if there are no bucket entries.
                    jiraEnabled = !collectiveEntries.isEmpty();
                }
            }
        }
        return view();
    }

    private String findWorkflowName(BucketEntry entry) {
        if (StringUtils.isNotBlank(entry.getWorkflowName())) {
            return entry.getWorkflowName();
        }
        return entry.getProductOrder().getProduct().getWorkflow().getWorkflowName();
    }

    public Set<String> getSampleNames(LabVessel vessel) {
        Set<SampleInstance> allSamples = vessel.getAllSamples();
        Set<String> sampleNames = new HashSet<>();
        for (SampleInstance sampleInstance : allSamples) {
            sampleNames.add(sampleInstance.getStartingSample().getSampleKey());
        }
        return sampleNames;
    }

    @HandlesEvent(ADD_TO_BATCH_ACTION)
    public Resolution addToBatch() {
        loadReworkVessels();
        if (batch == null) {
            addValidationError("selectedLcset", String.format("Could not find %s.", selectedLcset));
            return viewBucket();
        }
        // Cannot mix workfows in an LCSET.
        Set<String> batchWorkflows = getWorkflowNames();
        if (batchWorkflows.contains(selectedWorkflowDef.getName()) || batchWorkflows.isEmpty()) {
            return new ForwardResolution(CONFIRMATION_PAGE);
        }
        addValidationError("incompatibleWorkflows",
                "The selected workflow (" + selectedWorkflowDef.getName() +
                ") is different than the Batch's workflow (" + StringUtils.join(batchWorkflows, ", ") + ")");
        return viewBucket();
    }

    public String getConfirmationPageTitle() {
        return String.format("%d bucket and %d rework entries.", bucketEntryIds.size(), reworkEntryIds.size());
    }

    private void loadReworkVessels() {
        batch = labBatchDao.findByBusinessKey(selectedLcset);
        separateEntriesByType();
    }

    // Returns the workflow name for entries in the batch.
    private Set<String> getWorkflowNames() {
        Set<String> pdoKeys = new HashSet<>();
        Set<String> workflowNames = new HashSet<>();
        // Workflow names in the bucketEntry is a new feature with the addition of Extractions, which look at
        // the add-on's product's workflow. (continued)
        for (BucketEntry entry : batch.getBucketEntries()) {
            pdoKeys.add(entry.getProductOrder().getBusinessKey());
            workflowNames.add(entry.getWorkflowName());
        }
        // Therefore, if workflowNames is populated we know we are dealing with a post-extraction workflow.
        // If workflowNames is empty we get the workflowName from the product itself. This is all
        // in liu of back populating bucketEntry.workflowName.
        if (workflowNames.isEmpty()) {
            Collection<ProductOrder> pdos = productOrderDao.findListByBusinessKeys(pdoKeys);
            for (ProductOrder pdo : pdos) {
                workflowNames.add(pdo.getProduct().getWorkflow().getWorkflowName());
            }
        }
        return workflowNames;
    }

    @HandlesEvent(REWORK_CONFIRMED_ACTION)
    public Resolution reworkConfirmed() {
        separateEntriesByType();
        try {
            labBatchEjb.addToLabBatch(selectedLcset, bucketEntryIds, reworkEntryIds, selectedBucket, this);
        } catch (IOException e) {
            addGlobalValidationError("IOException contacting JIRA service." + e.getMessage());
            return new RedirectResolution(VIEW_PAGE);
        }
        addMessage(String.format("Successfully added %d %s and %d %s to batch '%s' from bucket '%s'.",
                bucketEntryIds.size(), Noun.pluralOf("sample", bucketEntryIds.size()),
                reworkEntryIds.size(), Noun.pluralOf("rework", reworkEntryIds.size()),
                selectedLcset, selectedBucket));
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
            batch = labBatchEjb
                    .createLabBatchAndRemoveFromBucket(LabBatch.LabBatchType.WORKFLOW,
                                                       selectedWorkflowDef.getName(), bucketEntryIds, reworkEntryIds,
                                                       summary.trim(),
                                                       description, dueDate, important,
                                                       userBean.getBspUser().getUsername(), selectedBucket, this);
        } catch (ValidationException e) {
            addGlobalValidationError(e.getMessage());
            return view();
        }

        addMessage(MessageFormat.format("Lab batch ''{0}'' has been created.", batch.getJiraTicket().getTicketName()));

        return new ForwardResolution(BATCH_CONFIRM_PAGE);
    }

    @HandlesEvent(REMOVE_FROM_BUCKET_ACTION)
    public Resolution removeFromBucket() {

        separateEntriesByType();

        return new ForwardResolution(REMOVE_FROM_BUCKET_CONFIRM_PAGE);
    }

    @HandlesEvent(CONFIRM_REMOVE_FROM_BUCKET_ACTION)
    public Resolution confirmRemoveFromBucket() {
        separateEntriesByType();

        bucketEjb.removeEntriesByIds(selectedEntryIds, "");

        addMessage(String.format("Successfully removed %d sample(s) and %d rework(s) from bucket '%s'.",
                                 bucketEntryIds.size(), reworkEntryIds.size(), selectedBucket));

        return new RedirectResolution(BucketViewActionBean.class, VIEW_ACTION);
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

    public String getSelectedBucket() {
        return selectedBucket;
    }

    public void setSelectedBucket(String selectedBucket) {
        this.selectedBucket = selectedBucket;
    }

    public Collection<BucketEntry> getBucketEntries() {
        return bucketEntries;
    }

    public boolean isJiraEnabled() {
        return jiraEnabled;
    }

    public void setJiraEnabled(boolean jiraEnabled) {
        this.jiraEnabled = jiraEnabled;
    }

    public Collection<BucketEntry> getReworkEntries() {
        return reworkEntries;
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

    public List<ProductWorkflowDef> getPossibleWorkflows() {
        return possibleWorkflows;
    }

    public ProductWorkflowDef getSelectedWorkflowDef() {
        return selectedWorkflowDef;
    }

    public void setSelectedWorkflowDef(ProductWorkflowDef selectedWorkflowDef) {
        this.selectedWorkflowDef = selectedWorkflowDef;
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

    public List<String> getBuckets() {
        List<String> list = new ArrayList<>(buckets);
        Collections.sort(list);
        return list;
    }

    public int totalBucketEntries() {
        if (bucket != null) {
            return bucket.getBucketEntries().size() + bucket.getReworkEntries().size();
        }
        return 0;
    }
}

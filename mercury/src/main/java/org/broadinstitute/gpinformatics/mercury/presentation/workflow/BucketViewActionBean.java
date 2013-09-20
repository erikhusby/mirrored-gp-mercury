package org.broadinstitute.gpinformatics.mercury.presentation.workflow;

import net.sourceforge.stripes.action.Before;
import net.sourceforge.stripes.action.DefaultHandler;
import net.sourceforge.stripes.action.ForwardResolution;
import net.sourceforge.stripes.action.HandlesEvent;
import net.sourceforge.stripes.action.RedirectResolution;
import net.sourceforge.stripes.action.Resolution;
import net.sourceforge.stripes.action.UrlBinding;
import net.sourceforge.stripes.controller.LifecycleStage;
import net.sourceforge.stripes.validation.Validate;
import net.sourceforge.stripes.validation.ValidationMethod;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.broadinstitute.gpinformatics.athena.entity.orders.ProductOrder;
import org.broadinstitute.gpinformatics.infrastructure.ValidationException;
import org.broadinstitute.gpinformatics.infrastructure.athena.AthenaClientService;
import org.broadinstitute.gpinformatics.mercury.boundary.vessel.LabBatchEjb;
import org.broadinstitute.gpinformatics.mercury.control.dao.bucket.BucketDao;
import org.broadinstitute.gpinformatics.mercury.control.dao.bucket.BucketEntryDao;
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

import javax.inject.Inject;
import java.io.IOException;
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

@UrlBinding(value = "/view/bucketView.action?{$event}")
public class BucketViewActionBean extends CoreActionBean {

    private static final String VIEW_PAGE = "/workflow/bucket_view.jsp";
    public static final String ADD_TO_BATCH_ACTION = "addToBatch";
    private static final String CONFIRMATION_PAGE = "/workflow/rework_confirmation.jsp";
    private static final String BATCH_CONFIRM_PAGE = "/batch/batch_confirm.jsp";

    @Inject
    private WorkflowLoader workflowLoader;
    @Inject
    private BucketDao bucketDao;
    @Inject
    private AthenaClientService athenaClientService;
    @Inject
    private LabBatchEjb labBatchEjb;
    @Inject
    private UserBean userBean;
    @Inject
    private LabBatchDao labBatchDao;
    @Inject
    private BucketEntryDao bucketEntryDao;

    public static final String EXISTING_TICKET = "existingTicket";
    public static final String NEW_TICKET = "newTicket";
    public static final String CREATE_BATCH_ACTION = "createBatch";
    private static final String REWORK_CONFIRMED_ACTION = "reworkConfirmed";

    @Validate(required = true, on = {CREATE_BATCH_ACTION, "viewBucket"})
    private String selectedBucket;
    @Validate(required = true, on = {CREATE_BATCH_ACTION, "viewBucket"})
    private ProductWorkflowDef selectedWorkflowDef;

    private String jiraTicketId;

    private final Set<String> buckets = new HashSet<>();
    private final List<Long> bucketEntryIds = new ArrayList<>();
    private final List<Long> reworkEntryIds = new ArrayList<>();
    private final List<BucketEntry> bucketEntries = new ArrayList<>();
    private final List<BucketEntry> reworkEntries = new ArrayList<>();
    private final List<BucketEntry> collectiveEntries = new ArrayList<>();
    private final Map<String, ProductOrder> mapPdoKeyToPdo = new HashMap<>();
    private final Map<String, Workflow> mapPdoKeyToWorkflow = new HashMap<>();
    private final Map<String, List<ProductWorkflowDef>> mapBucketToWorkflowDefs = new HashMap<>();

    private List<BucketEntry> selectedEntries = new ArrayList<>();
    private List<Long> selectedEntryIds = new ArrayList<>();
    private List<ProductWorkflowDef> possibleWorkflows;

    private boolean jiraEnabled = false;
    private String important;
    private String description;
    private String summary;
    private Date dueDate;
    private String selectedLcset;
    private LabBatch batch;

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

    @Before(stages = LifecycleStage.BindingAndValidation)
    public void init() {
        WorkflowConfig workflowConfig = workflowLoader.load();
        // Gets bucket names for supported products (workflows), and associates workflow(s) for each bucket.
        for (Workflow workflow : Workflow.SUPPORTED_WORKFLOWS) {
            ProductWorkflowDef workflowDef = workflowConfig.getWorkflowByName(workflow.getWorkflowName());
            ProductWorkflowDefVersion workflowVersion = workflowDef.getEffectiveVersion();
            for (WorkflowBucketDef bucket : workflowVersion.getCreationBuckets()) {
                String bucketName = bucket.getName();
                List<ProductWorkflowDef> bucketWorkflows;
                if (buckets.add(bucketName)) {
                    bucketWorkflows = new ArrayList<>();
                    mapBucketToWorkflowDefs.put(bucketName, bucketWorkflows);
                } else {
                    bucketWorkflows = mapBucketToWorkflowDefs.get(bucketName);
                }
                bucketWorkflows.add(workflowDef);
            }
        }
    }

    @DefaultHandler
    public Resolution view() {
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
        if (CollectionUtils.isEmpty(selectedEntryIds)) {
            addValidationError("selectedReworks", "At least one rework must be selected to add to the batch.");
            viewBucket();
        }
    }

    @HandlesEvent("setBucket")
    public Resolution setBucket() {
        if (selectedBucket != null) {
            // Sets the workflow selection list for this bucket.
            possibleWorkflows = mapBucketToWorkflowDefs.get(selectedBucket);
        }
        return view();
    }

    @HandlesEvent("viewBucket")
    public Resolution viewBucket() {
        if (selectedBucket != null && selectedWorkflowDef != null) {
            possibleWorkflows = mapBucketToWorkflowDefs.get(selectedBucket);

            // Gets the bucket entries that are in the selected bucket.
            Bucket bucket = bucketDao.findByName(selectedBucket);
            if (bucket != null) {
                bucketEntries.clear();
                reworkEntries.clear();
                collectiveEntries.clear();

                bucketEntries.addAll(bucket.getBucketEntries());
                reworkEntries.addAll(bucket.getReworkEntries());
                collectiveEntries.addAll(bucketEntries);
                collectiveEntries.addAll(reworkEntries);

                // Filters out entries whose product workflow doesn't match the selected workflow.
                Set<String> pdoKeys = new HashSet<>();
                for (BucketEntry entry : collectiveEntries) {
                    pdoKeys.add(entry.getPoBusinessKey());
                }
                Collection<ProductOrder> pdos = athenaClientService.retrieveMultipleProductOrderDetails(pdoKeys);
                for (ProductOrder pdo : pdos) {
                    mapPdoKeyToPdo.put(pdo.getBusinessKey(), pdo);
                    mapPdoKeyToWorkflow.put(pdo.getBusinessKey(), pdo.getProduct().getWorkflow());
                }
                for (Iterator<BucketEntry> iter = collectiveEntries.iterator(); iter.hasNext(); ) {
                    BucketEntry entry = iter.next();
                    if (!selectedWorkflowDef.getName().equals(
                            mapPdoKeyToWorkflow.get(entry.getPoBusinessKey()).getWorkflowName())) {
                        iter.remove();
                        bucketEntries.remove(entry);
                        reworkEntries.remove(entry);
                    }
                }
                // Doesn't show JIRA details if there are no bucket entries.
                jiraEnabled = !collectiveEntries.isEmpty();
            }
        }
        return view();
    }


    public ProductOrder getPDODetails(String pdoKey) {
        if (!mapPdoKeyToPdo.containsKey(pdoKey)) {
            mapPdoKeyToPdo.put(pdoKey, athenaClientService.retrieveProductOrderDetails(pdoKey));
        }
        return mapPdoKeyToPdo.get(pdoKey);
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
            return new ForwardResolution(VIEW_PAGE);
        }
        // Cannot mix workfows in an LCSET.
        Set<String> batchWorkflows = getWorkflowNames(batch);
        if (!batchWorkflows.contains(selectedWorkflowDef.getName())) {
            addValidationError("incompatibleWorkflows",
                    "The selected workflow (" + selectedWorkflowDef.getName() +
                    ") is different than LCSET's workflow (" + StringUtils.join(batchWorkflows, ", ") + ")");
            return new ForwardResolution(VIEW_PAGE);
        }
        return new ForwardResolution(CONFIRMATION_PAGE);
    }

    private void loadReworkVessels() {
        if (!selectedLcset.startsWith("LCSET-")) {
            selectedLcset = "LCSET-" + selectedLcset;
        }
        batch = labBatchDao.findByBusinessKey(selectedLcset);
        selectedEntries = bucketEntryDao.findByIds(selectedEntryIds);
        seperateEntriesByType();
    }


    // Returns the workflow name for entries in the batch.
    private Set<String> getWorkflowNames(LabBatch batch) {
        Set<String> pdoKeys = new HashSet<>();
        for (BucketEntry entry : batch.getBucketEntries()) {
            pdoKeys.add(entry.getPoBusinessKey());
        }
        Collection<ProductOrder> pdos = athenaClientService.retrieveMultipleProductOrderDetails(pdoKeys);
        Set<String> workflowNames = new HashSet<>();
        for (ProductOrder pdo : pdos) {
            workflowNames.add(pdo.getProduct().getWorkflow().getWorkflowName());
        }
        return workflowNames;
    }

    @HandlesEvent(REWORK_CONFIRMED_ACTION)
    public Resolution reworkConfirmed() {
        seperateEntriesByType();
        try {
            if (!selectedLcset.startsWith("LCSET-")) {
                selectedLcset = "LCSET-" + selectedLcset;
            }
            labBatchEjb.addToLabBatch(selectedLcset, bucketEntryIds, reworkEntryIds);
        } catch (IOException e) {
            addGlobalValidationError("IOException contacting JIRA service." + e.getMessage());
            return new RedirectResolution(VIEW_PAGE);
        }
        addMessage(String.format("Successfully added %d sample(s) and %d rework(s) to %s at the '%s'.",
                bucketEntryIds.size(), reworkEntryIds.size(), selectedLcset, selectedBucket));
        return new RedirectResolution(BucketViewActionBean.class, VIEW_ACTION);
    }

    /**
     * Supports the submission for the page.  Will forward to confirmation page on success
     *
     * @return The resolution
     */
    @HandlesEvent(CREATE_BATCH_ACTION)
    public Resolution createBatch() {
        seperateEntriesByType();
        try {
            batch = labBatchEjb
                    .createLabBatchAndRemoveFromBucket(LabBatch.LabBatchType.WORKFLOW,
                            selectedWorkflowDef.getName(), bucketEntryIds, reworkEntryIds, summary.trim(),
                            description, dueDate, important, userBean.getBspUser().getUsername());
        } catch (ValidationException e) {
            addGlobalValidationError(e.getMessage());
            return view();
        }

        addMessage(MessageFormat
                .format("Lab batch ''{0}'' has been created.", batch.getJiraTicket().getTicketName()));

        return new ForwardResolution(BATCH_CONFIRM_PAGE);
    }

    private void seperateEntriesByType() {
        // Iterate through the selected entries and separate the pdo entries from rework entries.
        selectedEntries = bucketEntryDao.findByIds(selectedEntryIds);
        for (BucketEntry entry : selectedEntries) {
            if (BucketEntry.BucketEntryType.PDO_ENTRY.equals(entry.getEntryType())) {
                bucketEntryIds.add(entry.getBucketEntryId());
            } else if (BucketEntry.BucketEntryType.REWORK_ENTRY.equals(entry.getEntryType())) {
                reworkEntryIds.add(entry.getBucketEntryId());
            }
        }
    }

}

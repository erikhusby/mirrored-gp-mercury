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
import org.broadinstitute.gpinformatics.infrastructure.jira.JiraService;
import org.broadinstitute.gpinformatics.mercury.boundary.bucket.BucketEjb;
import org.broadinstitute.gpinformatics.mercury.boundary.vessel.LabBatchEjb;
import org.broadinstitute.gpinformatics.mercury.control.dao.bucket.BucketDao;
import org.broadinstitute.gpinformatics.mercury.control.dao.bucket.BucketEntryDao;
import org.broadinstitute.gpinformatics.mercury.control.dao.rapsheet.ReworkEjb;
import org.broadinstitute.gpinformatics.mercury.control.dao.vessel.LabVesselDao;
import org.broadinstitute.gpinformatics.mercury.control.dao.workflow.LabBatchDao;
import org.broadinstitute.gpinformatics.mercury.control.workflow.WorkflowLoader;
import org.broadinstitute.gpinformatics.mercury.entity.bucket.Bucket;
import org.broadinstitute.gpinformatics.mercury.entity.bucket.BucketEntry;
import org.broadinstitute.gpinformatics.mercury.entity.labevent.LabEvent;
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
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
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
    private ReworkEjb reworkEjb;
    @Inject
    private LabVesselDao labVesselDao;
    @Inject
    private UserBean userBean;
    @Inject
    private LabBatchDao labBatchDao;
    @Inject
    private JiraService jiraService;
    @Inject
    private BucketEntryDao bucketEntryDao;
    @Inject
    private BucketEjb bucketEjb;

    public static final String EXISTING_TICKET = "existingTicket";
    public static final String NEW_TICKET = "newTicket";
    public static final String CREATE_BATCH_ACTION = "createBatch";
    private static final String REWORK_CONFIRMED_ACTION = "reworkConfirmed";

    private List<WorkflowBucketDef> buckets = new ArrayList<>();
    private List<Long> selectedEntryIds = new ArrayList<>();
    private List<BucketEntry> selectedEntries = new ArrayList<>();

    @Validate(required = true, on = {CREATE_BATCH_ACTION, "viewBucket"})
    private String selectedBucket;

    private Collection<BucketEntry> bucketEntries;
    private Collection<BucketEntry> reworkEntries;
    private Collection<BucketEntry> collectiveEntries;

    private Map<String, ProductOrder> pdoByKeyMap = new HashMap<>();

    private boolean jiraEnabled = false;

    private String jiraTicketId;

    private String important;
    private String description;
    private String summary;
    private Date dueDate;
    private String selectedProductWorkflowDef;
    private List<ProductWorkflowDef> allProductWorkflowDefs = new ArrayList<>();

    private String selectedLcset;
    private LabBatch batch;
    private List<Long> bucketEntryIds = new ArrayList<>();
    private List<Long> reworkEntryIds = new ArrayList<>();

    public LabBatch getBatch() {
        return batch;
    }

    public void setBatch(LabBatch batch) {
        this.batch = batch;
    }

    public List<WorkflowBucketDef> getBuckets() {
        return buckets;
    }

    public void setBuckets(List<WorkflowBucketDef> buckets) {
        this.buckets = buckets;
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

    public void setBucketEntries(Collection<BucketEntry> bucketEntries) {
        this.bucketEntries = bucketEntries;
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

    public void setReworkEntries(Collection<BucketEntry> reworkEntries) {
        this.reworkEntries = reworkEntries;
    }

    public String getSelectedProductWorkflowDef() {
        return selectedProductWorkflowDef;
    }

    public void setSelectedProductWorkflowDef(String selectedProductWorkflowDef) {
        this.selectedProductWorkflowDef = selectedProductWorkflowDef;
    }

    public List<ProductWorkflowDef> getAllProductWorkflowDefs() {
        return allProductWorkflowDefs;
    }

    public void setAllProductWorkflowDefs(List<ProductWorkflowDef> allProductWorkflowDefs) {
        this.allProductWorkflowDefs = allProductWorkflowDefs;
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

    public void setCollectiveEntries(Collection<BucketEntry> collectiveEntries) {
        this.collectiveEntries = collectiveEntries;
    }

    public List<BucketEntry> getSelectedEntries() {
        return selectedEntries;
    }

    public void setSelectedEntries(List<BucketEntry> selectedEntries) {
        this.selectedEntries = selectedEntries;
    }

    public Date getDueDate() {
        return dueDate;
    }

    public String getJiraTicketId() {
        return jiraTicketId;
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

    public void setSelectedEntryIds(List<Long> selectedEntryIds) {
        this.selectedEntryIds = selectedEntryIds;
    }

    @Before(stages = LifecycleStage.BindingAndValidation)
    public void init() {
        WorkflowConfig workflowConfig = workflowLoader.load();
        List<ProductWorkflowDef> workflowDefs = workflowConfig.getProductWorkflowDefs();
        //currently only do ExEx
        for (ProductWorkflowDef workflowDef : workflowDefs) {
            ProductWorkflowDefVersion workflowVersion = workflowDef.getEffectiveVersion();
            if (workflowDef.getName().equals(WorkflowName.EXOME_EXPRESS.getWorkflowName())) {
                allProductWorkflowDefs.add(workflowDef);
                WorkflowBucketDef blankBucketDef = new WorkflowBucketDef("");
                buckets.add(blankBucketDef);
                buckets.addAll(workflowVersion.getCreationBuckets());
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

    public Resolution viewBucket() {
        if (selectedBucket != null) {
            Bucket bucket = bucketDao.findByName(selectedBucket);
            if (bucket != null) {
                bucketEntries = bucket.getBucketEntries();
                reworkEntries = bucket.getReworkEntries();
            } else {
                bucketEntries = new ArrayList<>();
                reworkEntries = new ArrayList<>();
            }
            if (!bucketEntries.isEmpty() || !reworkEntries.isEmpty()) {
                jiraEnabled = true;

                List<String> poKeys = new ArrayList<>();
                collectiveEntries = new ArrayList<>(bucketEntries);
                collectiveEntries.addAll(reworkEntries);

                for (BucketEntry entryForPO : collectiveEntries) {
                    poKeys.add(entryForPO.getPoBusinessKey());
                }

                Collection<ProductOrder> foundOrders = athenaClientService.retrieveMultipleProductOrderDetails(poKeys);

                for (ProductOrder orderEntry : foundOrders) {
                    pdoByKeyMap.put(orderEntry.getBusinessKey(), orderEntry);
                }
            } else {
                jiraEnabled = false;
            }
        }
        return view();
    }


    public ProductOrder getPDODetails(String pdoKey) {
        if (!pdoByKeyMap.containsKey(pdoKey)) {
            pdoByKeyMap.put(pdoKey, athenaClientService.retrieveProductOrderDetails(pdoKey));
        }
        return pdoByKeyMap.get(pdoKey);
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

    @HandlesEvent(REWORK_CONFIRMED_ACTION)
    public Resolution reworkConfirmed() {
        seperateEntriesByType();
        try {
            if (!selectedLcset.startsWith("LCSET-")) {
                selectedLcset = "LCSET-" + selectedLcset;
            }
            labBatchEjb.updateBatchWithReworks(selectedLcset, reworkEntryIds);
        } catch (IOException e) {
            addGlobalValidationError("IOException contacting JIRA service." + e.getMessage());
            return new RedirectResolution(VIEW_PAGE);
        }
        addMessage(String.format("Successfully added %d reworks to %s at the '%s'.", reworkEntryIds.size(),
                selectedLcset, selectedBucket));
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
                    .createLabBatchAndRemoveFromBucket(LabBatch.LabBatchType.WORKFLOW, selectedProductWorkflowDef,
                            bucketEntryIds, reworkEntryIds, summary.trim(), description, dueDate, important,
                            userBean.getBspUser().getUsername(), LabEvent.UI_EVENT_LOCATION);
        } catch (ValidationException e) {
            addGlobalValidationError(e.getMessage());
            return view();
        }

        addMessage(MessageFormat
                .format("Lab batch ''{0}'' has been created.", batch.getJiraTicket().getTicketName()));

        return new ForwardResolution(BATCH_CONFIRM_PAGE);
    }

    private void seperateEntriesByType() {
        //iterate through the selected entries and seperate the pdo entries from rework entries
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

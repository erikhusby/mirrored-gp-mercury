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
import org.broadinstitute.gpinformatics.infrastructure.athena.AthenaClientService;
import org.broadinstitute.gpinformatics.mercury.boundary.vessel.LabBatchEjb;
import org.broadinstitute.gpinformatics.mercury.control.dao.bucket.BucketDao;
import org.broadinstitute.gpinformatics.mercury.control.dao.rapsheet.ReworkEjb;
import org.broadinstitute.gpinformatics.mercury.control.dao.vessel.LabVesselDao;
import org.broadinstitute.gpinformatics.mercury.control.workflow.WorkflowLoader;
import org.broadinstitute.gpinformatics.mercury.entity.bucket.Bucket;
import org.broadinstitute.gpinformatics.mercury.entity.bucket.BucketEntry;
import org.broadinstitute.gpinformatics.mercury.entity.labevent.LabEvent;
import org.broadinstitute.gpinformatics.mercury.entity.rapsheet.RapSheet;
import org.broadinstitute.gpinformatics.mercury.entity.sample.SampleInstance;
import org.broadinstitute.gpinformatics.mercury.entity.vessel.LabVessel;
import org.broadinstitute.gpinformatics.mercury.entity.workflow.LabBatch;
import org.broadinstitute.gpinformatics.mercury.entity.workflow.ProductWorkflowDef;
import org.broadinstitute.gpinformatics.mercury.entity.workflow.ProductWorkflowDefVersion;
import org.broadinstitute.gpinformatics.mercury.entity.workflow.WorkflowBucketDef;
import org.broadinstitute.gpinformatics.mercury.entity.workflow.WorkflowConfig;
import org.broadinstitute.gpinformatics.mercury.entity.workflow.WorkflowName;
import org.broadinstitute.gpinformatics.mercury.presentation.CoreActionBean;
import org.broadinstitute.gpinformatics.mercury.presentation.UserBean;
import org.broadinstitute.gpinformatics.mercury.presentation.search.CreateBatchActionBean;

import javax.inject.Inject;
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

    public static final String EXISTING_TICKET = "existingTicket";
    public static final String NEW_TICKET = "newTicket";
    public static final String CREATE_BATCH_ACTION = "createBatch";

    private List<WorkflowBucketDef> buckets = new ArrayList<>();
    private List<String> selectedVesselLabels = new ArrayList<>();
    private List<LabVessel> selectedBatchVessels;
    private List<String> selectedReworks = new ArrayList<>();

    @Validate(required = true, on = {CREATE_BATCH_ACTION, "viewBucket"})
    private String selectedBucket;

    private Collection<BucketEntry> bucketEntries;
    private Collection<LabVessel> reworkEntries;

    private Map<String, ProductOrder> pdoByKeyMap = new HashMap<>();

    private boolean jiraEnabled = false;

    private String jiraTicketId;

    private String important;
    private String description;
    private String summary;
    private Date dueDate;
    private String selectedProductWorkflowDef;
    private List<ProductWorkflowDef> allProductWorkflowDefs = new ArrayList<>();

    @Before(stages = LifecycleStage.BindingAndValidation)
    public void init() {
        WorkflowConfig workflowConfig = workflowLoader.load();
        List<ProductWorkflowDef> workflowDefs = workflowConfig.getProductWorkflowDefs();
        //currently only do ExEx
        for (ProductWorkflowDef workflowDef : workflowDefs) {
            ProductWorkflowDefVersion workflowVersion = workflowDef.getEffectiveVersion();
            if (workflowDef.getName().equals(WorkflowName.EXOME_EXPRESS.getWorkflowName())) {
                allProductWorkflowDefs.add(workflowDef);
                buckets.addAll(workflowVersion.getBuckets());
            }
        }
        //set the initial bucket to the first in the list and load it
        if (!buckets.isEmpty()) {
            selectedBucket = buckets.get(0).getName();
            viewBucket();
        }
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

    public List<String> getSelectedReworks() {
        return selectedReworks;
    }

    public void setSelectedReworks(List<String> selectedReworks) {
        this.selectedReworks = selectedReworks;
    }

    public Collection<LabVessel> getReworkEntries() {
        return reworkEntries;
    }

    public void setReworkEntries(Collection<LabVessel> reworkEntries) {
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

        if (CollectionUtils.isEmpty(selectedVesselLabels) && CollectionUtils.isEmpty(selectedReworks)) {
            addValidationError("selectedVesselLabels",
                    "At least one vessel or rework must be selected to create a batch.");
            viewBucket();
        }

        if (StringUtils.isBlank(summary)) {
            addValidationError("summary", "You must provide at least a summary to create a Jira Ticket.");
            viewBucket();
        }
    }

    public Resolution viewBucket() {
        reworkEntries = reworkEjb.getVesselsForRework();

        if (selectedBucket != null) {
            Bucket bucket = bucketDao.findByName(selectedBucket);
            if (bucket != null) {
                bucketEntries = bucket.getBucketEntries();
            } else {
                bucketEntries = new ArrayList<>();
            }
            if (!bucketEntries.isEmpty() || !reworkEntries.isEmpty()) {
                jiraEnabled = true;
                for (BucketEntry bucketEntry : bucketEntries) {
                    pdoByKeyMap.put(bucketEntry.getPoBusinessKey(),
                            athenaClientService.retrieveProductOrderDetails(bucketEntry.getPoBusinessKey()));
                }
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

    public String getSinglePDOBusinessKey(LabVessel vessel) {
        if (vessel.getPdoKeys().size() == 1) {
            return vessel.getPdoKeys().iterator().next();
        }
        return "Multiple PDOs";
    }

    private RapSheet getRapSheet(LabVessel vessel) {
        for (SampleInstance sampleInstance : vessel.getAllSamples()) {
            return sampleInstance.getStartingSample().getRapSheet();
        }

        return null;
    }

    public String getReworkReason(LabVessel vessel) {
        return getRapSheet(vessel)
                .getCurrentReworkEntry().getReworkReason().name();
    }

    public String getReworkComment(LabVessel vessel) {
        return getRapSheet(vessel)
                .getCurrentReworkEntry().getLabVesselComment().getComment();
    }

    public Set<String> getSampleNames(LabVessel vessel) {
        Set<SampleInstance> allSamples = vessel.getAllSamples();
        Set<String> sampleNames = new HashSet<>();
        for (SampleInstance sampleInstance : allSamples) {
            sampleNames.add(sampleInstance.getStartingSample().getSampleKey());
        }
        return sampleNames;
    }

    public Long getReworkOperator(LabVessel vessel) {
        return getRapSheet(vessel)
                .getCurrentReworkEntry().getLabVesselComment().getLabEvent().getEventOperator();
    }


    public Date getReworkLogDate(LabVessel vessel) {
        return getRapSheet(vessel)
                .getCurrentReworkEntry().getLabVesselComment().getLogDate();
    }

    /**
     * Supports the submission for the page.  Will forward to confirmation page on success
     *
     * @return The resolution
     */
    @HandlesEvent(CREATE_BATCH_ACTION)
    public Resolution createBatch() throws Exception {

//        ProductWorkflowDef workflowDef = workflowLoader.load().getWorkflowByName(selectedProductWorkflowDef);

        Set<LabVessel> vesselSet = new HashSet<>(labVesselDao.findByListIdentifiers(selectedVesselLabels));

        Set<LabVessel> reworks = new HashSet<>(labVesselDao.findByListIdentifiers(selectedReworks));

        LabBatch batchObject = new LabBatch(summary.trim(), vesselSet, LabBatch.LabBatchType.WORKFLOW, description,
                dueDate, important);
        batchObject.setWorkflowName(selectedProductWorkflowDef);
        batchObject.addReworks(reworks);

        labBatchEjb.createLabBatchAndRemoveFromBucket(batchObject, userBean.getBspUser().getUsername(), selectedBucket,
                LabEvent.UI_EVENT_LOCATION);

        addMessage(MessageFormat
                .format("Lab batch ''{0}'' has been created.", batchObject.getJiraTicket().getTicketName()));

        return new RedirectResolution(CreateBatchActionBean.class, CreateBatchActionBean.CONFIRM_ACTION)
                .addParameter("batchLabel", batchObject.getBatchName());
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

    public List<LabVessel> getSelectedBatchVessels() {
        return selectedBatchVessels;
    }

    public List<String> getSelectedVesselLabels() {
        return selectedVesselLabels;
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

    public void setSelectedBatchVessels(List<LabVessel> selectedBatchVessels) {
        this.selectedBatchVessels = selectedBatchVessels;
    }

    public void setSelectedVesselLabels(List<String> selectedVesselLabels) {
        this.selectedVesselLabels = selectedVesselLabels;
    }
}

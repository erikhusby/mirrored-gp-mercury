package org.broadinstitute.gpinformatics.mercury.presentation.workflow;

import net.sourceforge.stripes.action.*;
import net.sourceforge.stripes.controller.LifecycleStage;
import net.sourceforge.stripes.validation.Validate;
import net.sourceforge.stripes.validation.ValidationMethod;
import org.apache.commons.lang3.StringUtils;
import org.broadinstitute.gpinformatics.athena.entity.orders.ProductOrder;
import org.broadinstitute.gpinformatics.infrastructure.athena.AthenaClientService;
import org.broadinstitute.gpinformatics.mercury.boundary.vessel.LabBatchEjb;
import org.broadinstitute.gpinformatics.mercury.control.dao.bucket.BucketDao;
import org.broadinstitute.gpinformatics.mercury.control.dao.rework.ReworkEntryDao;
import org.broadinstitute.gpinformatics.mercury.control.dao.vessel.LabVesselDao;
import org.broadinstitute.gpinformatics.mercury.control.dao.workflow.LabBatchDAO;
import org.broadinstitute.gpinformatics.mercury.control.labevent.LabEventHandler;
import org.broadinstitute.gpinformatics.mercury.control.workflow.WorkflowLoader;
import org.broadinstitute.gpinformatics.mercury.entity.bucket.Bucket;
import org.broadinstitute.gpinformatics.mercury.entity.bucket.BucketEntry;
import org.broadinstitute.gpinformatics.mercury.entity.labevent.LabEvent;
import org.broadinstitute.gpinformatics.mercury.entity.rework.ReworkEntry;
import org.broadinstitute.gpinformatics.mercury.entity.sample.MercurySample;
import org.broadinstitute.gpinformatics.mercury.entity.vessel.LabVessel;
import org.broadinstitute.gpinformatics.mercury.entity.workflow.*;
import org.broadinstitute.gpinformatics.mercury.presentation.CoreActionBean;
import org.broadinstitute.gpinformatics.mercury.presentation.UserBean;
import org.broadinstitute.gpinformatics.mercury.presentation.search.CreateBatchActionBean;

import javax.inject.Inject;
import java.text.MessageFormat;
import java.util.*;

@UrlBinding(value = "/view/bucketView.action")
public class BucketViewActionBean extends CoreActionBean {

    private static final String VIEW_PAGE = "/resources/workflow/bucketView.jsp";
    @Inject
    private WorkflowLoader workflowLoader;
    @Inject
    private BucketDao bucketDao;
    @Inject
    private ReworkEntryDao reworkEntryDao;
    @Inject
    private AthenaClientService athenaClientService;
    @Inject
    private LabBatchEjb labBatchEjb;
    @Inject
    private LabVesselDao labVesselDao;
    @Inject
    private UserBean userBean;

    public static final String EXISTING_TICKET = "existingTicket";
    public static final String NEW_TICKET = "newTicket";
    public static final String CREATE_BATCH_ACTION = "createBatch";

    private List<WorkflowBucketDef> buckets = new ArrayList<WorkflowBucketDef>();
    private List<String> selectedVesselLabels;
    private List<LabVessel> selectedBatchVessels;
    private List<String> selectedReworks;

    @Validate(required = true, on = {CREATE_BATCH_ACTION, "viewBucket"})
    private String selectedBucket;

    private Collection<BucketEntry> bucketEntries;
    private Collection<ReworkEntry> reworkEntries;

    private Map<String, ProductOrder> pdoByKeyMap = new HashMap<String, ProductOrder>();

    private boolean jiraEnabled = false;

    private String jiraTicketId;

    private String important;
    private String description;
    private String summary;
    private Date dueDate;

    @Before(stages = LifecycleStage.BindingAndValidation)
    public void init() {
        WorkflowConfig workflowConfig = workflowLoader.load();
        List<ProductWorkflowDef> workflowDefs = workflowConfig.getProductWorkflowDefs();
        //currently only do ExEx
        for (ProductWorkflowDef workflowDef : workflowDefs) {
            ProductWorkflowDefVersion workflowVersion = workflowDef.getEffectiveVersion();
            if (workflowDef.getName().equals(WorkflowName.EXOME_EXPRESS.getWorkflowName())) {
                buckets.addAll(workflowVersion.getBuckets());
            }
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

    public Collection<ReworkEntry> getReworkEntries() {
        return reworkEntries;
    }

    public void setReworkEntries(Collection<ReworkEntry> reworkEntries) {
        this.reworkEntries = reworkEntries;
    }

    @DefaultHandler
    public Resolution view() {
        return new ForwardResolution(VIEW_PAGE);
    }

    @ValidationMethod(on = CREATE_BATCH_ACTION)
    public void createBatchValidation() {

        if (!getUserBean().isValidJiraUser()) {
            addValidationError("jiraTicketId", "You must be A valid Jira user to create an LCSet");
            viewBucket();
        }

        if (selectedVesselLabels == null || selectedVesselLabels.isEmpty()) {
            addValidationError("selectedVesselLabels", "At least one vessel must be selected to create a batch");
            viewBucket();
        }

        if (StringUtils.isBlank(summary)) {
            addValidationError("summary", "You must provide at least a summary to create a Jira Ticket");
            viewBucket();
        }
    }

    public Resolution viewBucket() {
        if (selectedBucket != null) {
            Bucket bucket = bucketDao.findByName(selectedBucket);
            if (bucket != null) {
                bucketEntries = bucket.getBucketEntries();
            } else {
                bucketEntries = new ArrayList<BucketEntry>();
            }
            if (bucketEntries.size() > 0) {
                jiraEnabled = true;
                for (BucketEntry bucketEntry : bucketEntries) {
                    pdoByKeyMap.put(bucketEntry.getPoBusinessKey(),
                            athenaClientService.retrieveProductOrderDetails(bucketEntry.getPoBusinessKey()));
                }
            }
            reworkEntries = reworkEntryDao.findAll(ReworkEntry.class);
        }

        //TODO jac populate the rework entries

        return view();
    }

    public ProductOrder getPDODetails(String pdoKey) {
        if (!pdoByKeyMap.containsKey(pdoKey)) {
            pdoByKeyMap.put(pdoKey, athenaClientService.retrieveProductOrderDetails(pdoKey));
        }
        return pdoByKeyMap.get(pdoKey);
    }

    public List<String> getPDOBusinessKeys(ReworkEntry entry) {
        List<String> businessKeys = new ArrayList<String>();
        for (MercurySample sample : entry.getLabVesselPosition().getMercurySamples()) {
            businessKeys.add(sample.getProductOrderKey());
        }
        return businessKeys;
    }

    public String getSinglePDOBusinessKey(ReworkEntry entry) {
        List<String> keys = getPDOBusinessKeys(entry);
        if (keys.size() == 1) {
            return keys.get(0);
        }
        return "Multiple PDOs";
    }

    public List<MercurySample> getMercurySamplesForBucketEntry(BucketEntry entry) {
        List<MercurySample> mercurySamplesForEntry = new ArrayList<MercurySample>();
        for (MercurySample sample : entry.getLabVessel().getMercurySamples()) {
            if (StringUtils.equals(entry.getPoBusinessKey(), sample.getProductOrderKey())) {
                mercurySamplesForEntry.add(sample);
            }
        }
        return mercurySamplesForEntry;
    }

    /**
     * Supports the submission for the page.  Will forward to confirmation page on success
     *
     * @return The resolution
     */
    @HandlesEvent(CREATE_BATCH_ACTION)
    public Resolution createBatch() throws Exception {
        LabBatch batchObject;

        Set<LabVessel> vesselSet = new HashSet<LabVessel>(labVesselDao.findByListIdentifiers(selectedVesselLabels));

        Set<LabVessel> reworks = new HashSet<LabVessel>(labVesselDao.findByListIdentifiers(selectedReworks));

        batchObject = new LabBatch(summary.trim(), vesselSet,LabBatch.LabBatchType.WORKFLOW, description, dueDate, important);
        // fixme here is where we just dump the reworks into the ticket
        // this should be persistent
        batchObject.addReworks(reworks);

        labBatchEjb.createLabBatchAndRemoveFromBucket(batchObject, userBean.getBspUser().getUsername(),selectedBucket, LabEvent.UI_EVENT_LOCATION);

        addMessage(MessageFormat.format("Lab batch ''{0}'' has been created.",batchObject.getJiraTicket().getTicketName()));

        return new RedirectResolution(CreateBatchActionBean.class, CreateBatchActionBean.CONFIRM_ACTION).addParameter("batchLabel", batchObject.getBatchName());
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

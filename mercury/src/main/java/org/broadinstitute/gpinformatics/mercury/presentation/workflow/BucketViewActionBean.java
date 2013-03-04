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
import org.apache.commons.lang3.StringUtils;
import org.broadinstitute.gpinformatics.athena.entity.orders.ProductOrder;
import org.broadinstitute.gpinformatics.infrastructure.athena.AthenaClientService;
import org.broadinstitute.gpinformatics.mercury.boundary.vessel.LabBatchEjb;
import org.broadinstitute.gpinformatics.mercury.control.dao.bucket.BucketDao;
import org.broadinstitute.gpinformatics.mercury.control.dao.vessel.LabVesselDao;
import org.broadinstitute.gpinformatics.mercury.control.dao.workflow.LabBatchDAO;
import org.broadinstitute.gpinformatics.mercury.control.labevent.LabEventHandler;
import org.broadinstitute.gpinformatics.mercury.control.workflow.WorkflowLoader;
import org.broadinstitute.gpinformatics.mercury.entity.bucket.Bucket;
import org.broadinstitute.gpinformatics.mercury.entity.bucket.BucketEntry;
import org.broadinstitute.gpinformatics.mercury.entity.labevent.LabEvent;
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

@UrlBinding(value = "/view/bucketView.action")
public class BucketViewActionBean extends CoreActionBean {

    private static final String VIEW_PAGE = "/resources/workflow/bucketView.jsp";
    @Inject
    private LabEventHandler     labEventHandler;
    @Inject
    private WorkflowLoader      workflowLoader;
    @Inject
    private BucketDao           bucketDao;
    @Inject
    private AthenaClientService athenaClientService;
    @Inject
    private LabBatchEjb         labBatchEjb;
    @Inject
    private LabVesselDao        labVesselDao;
    @Inject
    private LabBatchDAO         labBatchDAO;
    @Inject
    private UserBean            userBean;

    public static final String EXISTING_TICKET     = "existingTicket";
    public static final String NEW_TICKET          = "newTicket";
    public static final String CREATE_BATCH_ACTION = "createBatch";

    private List<WorkflowBucketDef> buckets = new ArrayList<WorkflowBucketDef>();
    private List<String>    selectedVesselLabels;
    private List<LabVessel> selectedBatchVessels;

    @Validate(required = true, on = {CREATE_BATCH_ACTION, "viewBucket"})
    private String selectedBucket;

    private Collection<BucketEntry> bucketEntries;

    private Map<String, ProductOrder> pdoByKeyMap = new HashMap<String, ProductOrder>();

    private boolean jiraEnabled = false;

    private String jiraTicketId;

    private String important;
    private String description;
    private String summary;
    private Date   dueDate;

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
                                           athenaClientService
                                                   .retrieveProductOrderDetails(bucketEntry.getPoBusinessKey()));
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

    /**
     * Supports the submission for the page.  Will forward to confirmation page on success
     *
     * @return The resolution
     */
    @HandlesEvent(CREATE_BATCH_ACTION)
    public Resolution createBatch() throws Exception {
        LabBatch batchObject;

        Set<LabVessel> vesselSet =
                new HashSet<LabVessel>(labVesselDao.findByListIdentifiers(selectedVesselLabels));

        /*
           If a new ticket is to be created, pass the description, summary, due date and important info in a batch
           object acting as a DTO
        */
        batchObject = new LabBatch(summary.trim(), vesselSet, LabBatch.LabBatchType.WORKFLOW, description, dueDate,
                                          important);

        labBatchEjb.createLabBatchAndRemoveFromBucket(batchObject, userBean.getBspUser().getUsername(),
                                                             selectedBucket, LabEvent.UI_EVENT_LOCATION);

        addMessage(MessageFormat.format("Lab batch ''{0}'' has been created.",
                                               batchObject.getJiraTicket().getTicketName()));

        //Forward
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

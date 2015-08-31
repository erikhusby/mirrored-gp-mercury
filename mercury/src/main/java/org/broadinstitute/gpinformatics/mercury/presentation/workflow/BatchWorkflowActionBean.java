package org.broadinstitute.gpinformatics.mercury.presentation.workflow;

import net.sourceforge.stripes.action.DefaultHandler;
import net.sourceforge.stripes.action.ForwardResolution;
import net.sourceforge.stripes.action.HandlesEvent;
import net.sourceforge.stripes.action.Resolution;
import net.sourceforge.stripes.action.UrlBinding;
import org.apache.commons.lang3.StringUtils;
import org.broadinstitute.gpinformatics.mercury.control.dao.workflow.LabBatchDao;
import org.broadinstitute.gpinformatics.mercury.control.workflow.WorkflowLoader;
import org.broadinstitute.gpinformatics.mercury.control.workflow.WorkflowMatcher;
import org.broadinstitute.gpinformatics.mercury.entity.labevent.LabEvent;
import org.broadinstitute.gpinformatics.mercury.entity.labevent.LabEventType;
import org.broadinstitute.gpinformatics.mercury.entity.reagent.GenericReagent;
import org.broadinstitute.gpinformatics.mercury.entity.workflow.LabBatch;
import org.broadinstitute.gpinformatics.mercury.entity.workflow.ProductWorkflowDef;
import org.broadinstitute.gpinformatics.mercury.entity.workflow.ProductWorkflowDefVersion;
import org.broadinstitute.gpinformatics.mercury.entity.workflow.WorkflowStepDef;
import org.broadinstitute.gpinformatics.mercury.presentation.CoreActionBean;
import org.broadinstitute.gpinformatics.mercury.presentation.labevent.ManualTransferActionBean;

import javax.annotation.Nonnull;
import javax.inject.Inject;
import java.util.Date;
import java.util.List;

/**
 * Displays the workflow for a batch, with matched events.
 */
@UrlBinding(value = "/workflow/BatchWorkflow.action")
public class BatchWorkflowActionBean extends CoreActionBean {

    private static final String VIEW_PAGE = "/workflow/batch_workflow.jsp";
    public static final String BATCH_EVENT_ACTION = "batchEvent";
    public static final String BATCH_REAGENT_ACTION = "batchReagent";

    /** URL parameter. */
    private String batchName;

    /** Posted in form. */
    private String workflowProcessName;

    /** Posted in form. */
    private String workflowStepName;

    /** Posted in form. */
    private Date workflowEffectiveDate;

    /** Posted in form. */
    private LabEventType labEventType;

    /** Posted in form. */
    private String workflowQualifier;

    /** Posted in form. */
    private List<String> reagentNames;

    /** Posted in form. */
    private List<String> reagentLots;

    /** Posted in form. */
    private List<Date> reagentExpirations;

    /** Fetched from database. */
    private LabBatch labBatch;

    /** Fetched from labBatch. */
    private ProductWorkflowDefVersion effectiveWorkflowDef;


    @Inject
    private LabBatchDao labBatchDao;

    @Inject
    private WorkflowLoader workflowLoader;

    @Inject
    private WorkflowMatcher workflowMatcher;
    private List<WorkflowMatcher.WorkflowEvent> workflowEvents;

    @DefaultHandler
    @HandlesEvent(VIEW_ACTION)
    public Resolution view() {
        if (batchName != null) {
            labBatch = labBatchDao.findByName(batchName);
            fetchWorkflow();
        }
        return new ForwardResolution(VIEW_PAGE);
    }

    private void fetchWorkflow() {
        ProductWorkflowDef workflowDef = workflowLoader.load().getWorkflowByName(labBatch.getWorkflowName());
        effectiveWorkflowDef = workflowDef.getEffectiveVersion(labBatch.getCreatedOn());
        // Set relationship between steps and process
        effectiveWorkflowDef.buildLabEventGraph();
        workflowEvents = workflowMatcher.match(effectiveWorkflowDef, labBatch);
    }

    @HandlesEvent(BATCH_EVENT_ACTION)
    public Resolution batchEvent() {
        labBatch = labBatchDao.findByName(batchName);
        buildLabEvent();
        labBatchDao.flush();

        fetchWorkflow();
        return new ForwardResolution(VIEW_PAGE);
    }

    @HandlesEvent(BATCH_REAGENT_ACTION)
    public Resolution batchReagent() {
        labBatch = labBatchDao.findByName(batchName);
        WorkflowStepDef workflowStepDef = ManualTransferActionBean.loadWorkflowStepDef(workflowEffectiveDate,
                workflowLoader, workflowProcessName, workflowStepName);
        if (reagentNames.size() != workflowStepDef.getReagentTypes().size() ||
                !reagentNames.containsAll(workflowStepDef.getReagentTypes())) {
            addGlobalValidationError("Mismatch in reagent names between form and workflow");
        }

        LabEvent labEvent = buildLabEvent();
        for (int i = 0; i < reagentNames.size(); i++) {
            if (StringUtils.isEmpty(reagentLots.get(i))) {
                addGlobalValidationError("Barcode is required for " + reagentNames.get(i));
            }
            if (reagentExpirations.get(i) == null) {
                addGlobalValidationError("Expiration date is required for " + reagentNames.get(i));
            }
            labEvent.addReagent(new GenericReagent(reagentNames.get(i), reagentLots.get(i), reagentExpirations.get(i)));
        }

        if (getValidationErrors().isEmpty()) {
            labBatchDao.flush();
        }

        fetchWorkflow();
        return new ForwardResolution(VIEW_PAGE);
    }

    @Nonnull
    private LabEvent buildLabEvent() {
        LabEvent labEvent = new LabEvent(labEventType, new Date(), LabEvent.UI_EVENT_LOCATION,
                1L, getUserBean().getBspUser().getUserId(), LabEvent.UI_PROGRAM_NAME);
        labEvent.setWorkflowQualifier(workflowQualifier);
        labEvent.setLabBatch(labBatch);
        return labEvent;
    }

    public void setBatchName(String batchName) {
        this.batchName = batchName;
    }

    public LabBatch getLabBatch() {
        return labBatch;
    }

    public ProductWorkflowDefVersion getEffectiveWorkflowDef() {
        return effectiveWorkflowDef;
    }

    public List<WorkflowMatcher.WorkflowEvent> getWorkflowEvents() {
        return workflowEvents;
    }

    public String getBatchName() {
        return batchName;
    }

    public String getWorkflowProcessName() {
        return workflowProcessName;
    }

    public void setWorkflowProcessName(String workflowProcessName) {
        this.workflowProcessName = workflowProcessName;
    }

    public String getWorkflowStepName() {
        return workflowStepName;
    }

    public void setWorkflowStepName(String workflowStepName) {
        this.workflowStepName = workflowStepName;
    }

    public Date getWorkflowEffectiveDate() {
        return workflowEffectiveDate;
    }

    public void setWorkflowEffectiveDate(Date workflowEffectiveDate) {
        this.workflowEffectiveDate = workflowEffectiveDate;
    }

    public String getBatchEventAction() {
        return BATCH_EVENT_ACTION;
    }

    public String getBatchReagentAction() {
        return BATCH_REAGENT_ACTION;
    }

    public void setLabEventType(LabEventType labEventType) {
        this.labEventType = labEventType;
    }

    public void setWorkflowQualifier(String workflowQualifier) {
        this.workflowQualifier = workflowQualifier;
    }

    public List<String> getReagentNames() {
        return reagentNames;
    }

    public void setReagentNames(List<String> reagentNames) {
        this.reagentNames = reagentNames;
    }

    public List<String> getReagentLots() {
        return reagentLots;
    }

    public void setReagentLots(List<String> reagentLots) {
        this.reagentLots = reagentLots;
    }

    public List<Date> getReagentExpirations() {
        return reagentExpirations;
    }

    public void setReagentExpirations(List<Date> reagentExpirations) {
        this.reagentExpirations = reagentExpirations;
    }
}

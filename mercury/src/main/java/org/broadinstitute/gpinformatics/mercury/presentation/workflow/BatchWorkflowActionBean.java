package org.broadinstitute.gpinformatics.mercury.presentation.workflow;

import net.sourceforge.stripes.action.DefaultHandler;
import net.sourceforge.stripes.action.ForwardResolution;
import net.sourceforge.stripes.action.HandlesEvent;
import net.sourceforge.stripes.action.Resolution;
import net.sourceforge.stripes.action.UrlBinding;
import org.broadinstitute.gpinformatics.mercury.control.dao.workflow.LabBatchDao;
import org.broadinstitute.gpinformatics.mercury.control.workflow.WorkflowLoader;
import org.broadinstitute.gpinformatics.mercury.control.workflow.WorkflowMatcher;
import org.broadinstitute.gpinformatics.mercury.entity.labevent.LabEvent;
import org.broadinstitute.gpinformatics.mercury.entity.labevent.LabEventType;
import org.broadinstitute.gpinformatics.mercury.entity.workflow.LabBatch;
import org.broadinstitute.gpinformatics.mercury.entity.workflow.ProductWorkflowDef;
import org.broadinstitute.gpinformatics.mercury.entity.workflow.ProductWorkflowDefVersion;
import org.broadinstitute.gpinformatics.mercury.presentation.CoreActionBean;

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

    /** URL parameter. */
    private String batchName;

    /** Fetched from database. */
    private LabBatch labBatch;

    /** Fetched from labBatch. */
    private ProductWorkflowDefVersion effectiveWorkflowDef;

    /** Posted in form. */
    private LabEventType labEventType;

    /** Posted in form. */
    private String workflowQualifer;

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
            ProductWorkflowDef workflowDef = workflowLoader.load().getWorkflowByName(labBatch.getWorkflowName());
            effectiveWorkflowDef = workflowDef.getEffectiveVersion(labBatch.getCreatedOn());
            // Set relationship between steps and process
            effectiveWorkflowDef.buildLabEventGraph();
            workflowEvents = workflowMatcher.match(effectiveWorkflowDef, labBatch);
        }
        return new ForwardResolution(VIEW_PAGE);
    }

    @HandlesEvent(BATCH_EVENT_ACTION)
    public Resolution batchEvent() {
        labBatch = labBatchDao.findByName(batchName);

        LabEvent labEvent = new LabEvent(labEventType, new Date(), LabEvent.UI_EVENT_LOCATION,
                1L, getUserBean().getBspUser().getUserId(), LabEvent.UI_PROGRAM_NAME);
        labEvent.setWorkflowQualifier(workflowQualifer);
        labEvent.setLabBatch(labBatch);
        labBatchDao.flush();

        ProductWorkflowDef workflowDef = workflowLoader.load().getWorkflowByName(labBatch.getWorkflowName());
        effectiveWorkflowDef =  workflowDef.getEffectiveVersion(labBatch.getCreatedOn());
        workflowEvents = workflowMatcher.match(effectiveWorkflowDef, labBatch);
        return new ForwardResolution(VIEW_PAGE);
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

    public String getBatchEventAction() {
        return BATCH_EVENT_ACTION;
    }

    public void setLabEventType(LabEventType labEventType) {
        this.labEventType = labEventType;
    }

    public void setWorkflowQualifer(String workflowQualifer) {
        this.workflowQualifer = workflowQualifer;
    }
}

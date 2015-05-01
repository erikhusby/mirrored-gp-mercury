package org.broadinstitute.gpinformatics.mercury.presentation.workflow;

import net.sourceforge.stripes.action.DefaultHandler;
import net.sourceforge.stripes.action.ForwardResolution;
import net.sourceforge.stripes.action.HandlesEvent;
import net.sourceforge.stripes.action.Resolution;
import net.sourceforge.stripes.action.UrlBinding;
import org.broadinstitute.gpinformatics.mercury.control.dao.workflow.LabBatchDao;
import org.broadinstitute.gpinformatics.mercury.control.workflow.WorkflowLoader;
import org.broadinstitute.gpinformatics.mercury.control.workflow.WorkflowMatcher;
import org.broadinstitute.gpinformatics.mercury.entity.workflow.LabBatch;
import org.broadinstitute.gpinformatics.mercury.entity.workflow.ProductWorkflowDef;
import org.broadinstitute.gpinformatics.mercury.presentation.CoreActionBean;

import javax.inject.Inject;
import java.util.List;

/**
 * Displays the workflow for a batch, with matched events.
 */
@UrlBinding(value = "/workflow/BatchWorkflow.action")
public class BatchWorkflowActionBean extends CoreActionBean {

    private static final String VIEW_PAGE = "/workflow/batch_workflow.jsp";

    /** URL parameter */
    private String batchName;

    /** Fetched from database */
    private LabBatch labBatch;

    /** Fetched from labBatch */
    private ProductWorkflowDef workflowDef;

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
            workflowDef = workflowLoader.load().getWorkflowByName(labBatch.getWorkflowName());
            workflowEvents = workflowMatcher.match(workflowDef.getEffectiveVersion(labBatch.getCreatedOn()), labBatch);
        }
        return new ForwardResolution(VIEW_PAGE);
    }

    public void setBatchName(String batchName) {
        this.batchName = batchName;
    }

    public LabBatch getLabBatch() {
        return labBatch;
    }

    public ProductWorkflowDef getWorkflowDef() {
        return workflowDef;
    }

    public List<WorkflowMatcher.WorkflowEvent> getWorkflowEvents() {
        return workflowEvents;
    }
}

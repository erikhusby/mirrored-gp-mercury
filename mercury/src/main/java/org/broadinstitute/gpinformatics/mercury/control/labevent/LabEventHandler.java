package org.broadinstitute.gpinformatics.mercury.control.labevent;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.broadinstitute.gpinformatics.mercury.control.labevent.eventhandlers.AmbiguousLcsetException;
import org.broadinstitute.gpinformatics.mercury.control.vessel.JiraCommentUtil;
import org.broadinstitute.gpinformatics.mercury.control.workflow.WorkflowLoader;
import org.broadinstitute.gpinformatics.mercury.entity.OrmUtil;
import org.broadinstitute.gpinformatics.mercury.entity.labevent.LabEvent;
import org.broadinstitute.gpinformatics.mercury.entity.labevent.LabEventType;
import org.broadinstitute.gpinformatics.mercury.entity.workflow.ProductWorkflowDef;
import org.broadinstitute.gpinformatics.mercury.entity.workflow.ProductWorkflowDefVersion;
import org.broadinstitute.gpinformatics.mercury.entity.workflow.Workflow;
import org.broadinstitute.gpinformatics.mercury.entity.workflow.WorkflowBucketDef;
import org.broadinstitute.gpinformatics.mercury.entity.workflow.WorkflowConfig;
import org.broadinstitute.gpinformatics.mercury.entity.workflow.WorkflowProcessDef;
import org.broadinstitute.gpinformatics.mercury.entity.workflow.WorkflowProcessDefVersion;
import org.broadinstitute.gpinformatics.mercury.entity.workflow.WorkflowStepDef;

import javax.inject.Inject;
import java.io.Serializable;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

// Implements Serializable because it's used by a Stateful session bean.
public class LabEventHandler implements Serializable {

    public enum HandlerResponse {
        OK,
        ERROR /* further refine to "out of order", "bad molecular envelope", critical, warning, etc. */
    }

    private static final Log LOG = LogFactory.getLog(LabEventHandler.class);

    private WorkflowLoader workflowLoader;

    @Inject
    private JiraCommentUtil jiraCommentUtil;

    LabEventHandler() {
    }

    @Inject
    public LabEventHandler(WorkflowLoader workflowLoader) {
        this.workflowLoader = workflowLoader;
    }

    public HandlerResponse processEvent(LabEvent labEvent) {

        // Updates JIRA with the event info.
        if (jiraCommentUtil != null) {
            try {
                jiraCommentUtil.postUpdate(labEvent);
            } catch (Exception e) {
                // This is not fatal, so don't rethrow
                LOG.error("Failed to update JIRA", e);
            }
        }

        // Validates that the sample instances in the target vessels all have an unambiguous LCSET
        // if the lab event is configured to check for it.
        if (eventCanFollowBucket(labEvent) && !labEvent.vesselsHaveSingleLcsets()) {
            throw new AmbiguousLcsetException("Vessels have ambiguous LCSET in " + labEvent.getLabEventType().getName()
                                              + " (id " + labEvent.getLabEventId() + ")");
        }

        return HandlerResponse.OK;
    }

    private static Set<LabEventType> eventTypesThatCanFollowBucket = new HashSet<>();

    public boolean eventCanFollowBucket(LabEvent labEvent) {
        if (eventTypesThatCanFollowBucket.size() == 0) {
            WorkflowConfig workflowConfig = workflowLoader.load();
            for (Workflow workflow : Workflow.SUPPORTED_WORKFLOWS) {
                ProductWorkflowDef workflowDef  = workflowConfig.getWorkflowByName(workflow.getWorkflowName());
                ProductWorkflowDefVersion effectiveWorkflow = workflowDef.getEffectiveVersion();
                boolean collectEvents = false;
                for (WorkflowProcessDef processDef : effectiveWorkflow.getWorkflowProcessDefs()) {
                    WorkflowProcessDefVersion effectiveProcess = processDef.getEffectiveVersion();
                    for (WorkflowStepDef step : effectiveProcess.getWorkflowStepDefs()) {
                        if (OrmUtil.proxySafeIsInstance(step, WorkflowBucketDef.class)) {
                            // We've hit a bucket. Set the flag to start collecting step's events.
                            collectEvents = true;
                        } else if (collectEvents) {
                            eventTypesThatCanFollowBucket.addAll(step.getLabEventTypes());
                            if (!step.isOptional()) {
                                collectEvents = false;
                            }
                        }
                    }
                }
            }
        }
        return eventTypesThatCanFollowBucket.contains(labEvent.getLabEventType());
    }

}

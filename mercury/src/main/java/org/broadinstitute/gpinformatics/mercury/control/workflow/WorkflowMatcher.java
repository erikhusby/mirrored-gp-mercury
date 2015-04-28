package org.broadinstitute.gpinformatics.mercury.control.workflow;

import org.broadinstitute.gpinformatics.mercury.entity.labevent.LabEvent;
import org.broadinstitute.gpinformatics.mercury.entity.labevent.LabEventType;
import org.broadinstitute.gpinformatics.mercury.entity.vessel.LabVessel;
import org.broadinstitute.gpinformatics.mercury.entity.workflow.LabBatch;
import org.broadinstitute.gpinformatics.mercury.entity.workflow.ProductWorkflowDefVersion;
import org.broadinstitute.gpinformatics.mercury.entity.workflow.WorkflowProcessDef;
import org.broadinstitute.gpinformatics.mercury.entity.workflow.WorkflowProcessDefVersion;
import org.broadinstitute.gpinformatics.mercury.entity.workflow.WorkflowStepDef;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Takes a workflow definition and fits events to it.
 */
public class WorkflowMatcher {

    public static class WorkflowEvent {
        private WorkflowStepDef workflowStepDef;
        private List<LabEvent> labEvents;

        public WorkflowEvent(WorkflowStepDef workflowStepDef, List<LabEvent> labEvents) {
            this.workflowStepDef = workflowStepDef;
            this.labEvents = labEvents;
        }

        public WorkflowStepDef getWorkflowStepDef() {
            return workflowStepDef;
        }

        public List<LabEvent> getLabEvents() {
            return labEvents;
        }
    }

    public List<WorkflowEvent> match(ProductWorkflowDefVersion productWorkflowDefVersion, LabBatch labBatch) {
        List<WorkflowEvent> workflowEvents = new ArrayList<>();

        Map<LabEventType, List<LabEvent>> mapTypeToEvent = new HashMap<>();

        labBatch.getLabEvents();
        for (LabVessel labVessel : labBatch.getStartingBatchLabVessels()) {
            // get descendant events
        }

        // match events to steps
        for (WorkflowProcessDef workflowProcessDef : productWorkflowDefVersion.getWorkflowProcessDefs()) {
            WorkflowProcessDefVersion effectiveVersion = workflowProcessDef.getEffectiveVersion();
            for (WorkflowStepDef workflowStepDef : effectiveVersion.getWorkflowStepDefs()) {
                workflowEvents.add(new WorkflowEvent(workflowStepDef, null));
            }
        }

        // Add non-matched events in chronological order

        return workflowEvents;
    }
}

package org.broadinstitute.gpinformatics.mercury.control.workflow;

import org.broadinstitute.gpinformatics.mercury.entity.labevent.LabEvent;
import org.broadinstitute.gpinformatics.mercury.entity.labevent.LabEventType;
import org.broadinstitute.gpinformatics.mercury.entity.vessel.LabVessel;
import org.broadinstitute.gpinformatics.mercury.entity.workflow.LabBatch;
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
    }

    public List<WorkflowEvent> match(WorkflowProcessDefVersion workflowProcessDefVersion, LabBatch labBatch) {
        List<WorkflowEvent> workflowEvents = new ArrayList<>();

        Map<LabEventType, List<LabEvent>> mapTypeToEvent = new HashMap<>();

        labBatch.getLabEvents();
        for (LabVessel labVessel : labBatch.getStartingBatchLabVessels()) {
            // get descendant events
        }

        // match events to steps
        for (WorkflowStepDef workflowStepDef : workflowProcessDefVersion.getWorkflowStepDefs()) {

        }

        // Add non-matched events in chronological order

        return workflowEvents;
    }
}

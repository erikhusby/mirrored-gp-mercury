package org.broadinstitute.gpinformatics.mercury.control.workflow;

import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;
import org.broadinstitute.gpinformatics.mercury.entity.labevent.LabEvent;
import org.broadinstitute.gpinformatics.mercury.entity.labevent.LabEventType;
import org.broadinstitute.gpinformatics.mercury.entity.vessel.LabVessel;
import org.broadinstitute.gpinformatics.mercury.entity.vessel.TransferTraverserCriteria;
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

    public static class LabEventKey {
        private final LabEventType labEventType;
        private final String workflowQualifier;

        public LabEventKey(LabEvent labEvent) {
            labEventType = labEvent.getLabEventType();
            workflowQualifier = labEvent.getWorkflowQualifier();
        }

        public LabEventKey(LabEventType labEventType, String workflowQualifier) {
            this.labEventType = labEventType;
            this.workflowQualifier = workflowQualifier;
        }

        public LabEventType getLabEventType() {
            return labEventType;
        }

        public String getWorkflowQualifier() {
            return workflowQualifier;
        }

        @Override
        public boolean equals(Object obj) {
            if (this == obj) {
                return true;
            }

            if (obj == null || getClass() != obj.getClass()) {
                return false;
            }

            LabEventKey that = (LabEventKey) obj;

            return new EqualsBuilder()
                    .append(labEventType, that.labEventType)
                    .append(workflowQualifier, that.workflowQualifier)
                    .isEquals();
        }

        @Override
        public int hashCode() {
            return new HashCodeBuilder(17, 37)
                    .append(labEventType)
                    .append(workflowQualifier)
                    .toHashCode();
        }
    }

    public List<WorkflowEvent> match(ProductWorkflowDefVersion productWorkflowDefVersion, LabBatch labBatch) {
        List<WorkflowEvent> workflowEvents = new ArrayList<>();

        Map<LabEventKey, List<LabEvent>> mapTypeToEvent = new HashMap<>();

        for (LabEvent labEvent : labBatch.getLabEvents()) {
            LabEventKey labEventKey = new LabEventKey(labEvent);
            List<LabEvent> labEvents = mapTypeToEvent.get(labEventKey);
            if (labEvents == null) {
                labEvents = new ArrayList<>();
                mapTypeToEvent.put(labEventKey, labEvents);
            }
            labEvents.add(labEvent);
        }

        // add vessel descendant events
        for (LabVessel labVessel : labBatch.getStartingBatchLabVessels()) {
            TransferTraverserCriteria.LabEventDescendantCriteria labEventDescendantCriteria =
                    new TransferTraverserCriteria.LabEventDescendantCriteria();
            labVessel.evaluateCriteria(labEventDescendantCriteria,
                    TransferTraverserCriteria.TraversalDirection.Descendants);
            for (LabEvent labEvent : labEventDescendantCriteria.getAllEvents()) {
                LabEventKey labEventKey = new LabEventKey(labEvent);
                List<LabEvent> labEvents = mapTypeToEvent.get(labEventKey);
                if (labEvents == null) {
                    labEvents = new ArrayList<>();
                    mapTypeToEvent.put(labEventKey, labEvents);
                }
                labEvents.add(labEvent);
            }
        }

        // match events to steps
        for (WorkflowProcessDef workflowProcessDef : productWorkflowDefVersion.getWorkflowProcessDefs()) {
            WorkflowProcessDefVersion effectiveVersion = workflowProcessDef.getEffectiveVersion();
            for (WorkflowStepDef workflowStepDef : effectiveVersion.getWorkflowStepDefs()) {
                for (LabEventType labEventType : workflowStepDef.getLabEventTypes()) {
                    workflowEvents.add(new WorkflowEvent(workflowStepDef, mapTypeToEvent.get(
                            new LabEventKey(labEventType, workflowStepDef.getWorkflowQualifier()))));
                }
            }
        }

        // Add non-matched events in chronological order

        return workflowEvents;
    }
}

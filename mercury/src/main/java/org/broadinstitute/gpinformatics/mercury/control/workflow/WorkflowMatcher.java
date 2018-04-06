package org.broadinstitute.gpinformatics.mercury.control.workflow;

import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;
import org.broadinstitute.gpinformatics.mercury.entity.labevent.LabEvent;
import org.broadinstitute.gpinformatics.mercury.entity.labevent.LabEventType;
import org.broadinstitute.gpinformatics.mercury.entity.vessel.LabVessel;
import org.broadinstitute.gpinformatics.mercury.entity.vessel.TransferTraverserCriteria;
import org.broadinstitute.gpinformatics.mercury.entity.workflow.LabBatch;
import org.broadinstitute.gpinformatics.mercury.entity.workflow.ProductWorkflowDefVersion;
import org.broadinstitute.gpinformatics.mercury.entity.workflow.WorkflowBucketDef;
import org.broadinstitute.gpinformatics.mercury.entity.workflow.WorkflowProcessDef;
import org.broadinstitute.gpinformatics.mercury.entity.workflow.WorkflowProcessDefVersion;
import org.broadinstitute.gpinformatics.mercury.entity.workflow.WorkflowStepDef;

import javax.enterprise.context.Dependent;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

/**
 * Takes a workflow definition and fits events to it.
 */
@Dependent
public class WorkflowMatcher {

    /**
     * Associates workflow steps with events.
     */
    public static class WorkflowEvent {
        private WorkflowStepDef workflowStepDef;
        private List<LabEvent> labEvents;
        private boolean skipped;

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

        public boolean isSkipped() {
            return skipped;
        }

        public void setSkipped(boolean skipped) {
            this.skipped = skipped;
        }
    }

    /**
     * Key in a map that allows random access to LabEvents from workflow steps.
     */
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

    /**
     * For a given workflow and batch, returns a list of workflow steps, with associated events from the batch.
     */
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
        TransferTraverserCriteria.LabEventDescendantCriteria labEventDescendantCriteria =
                new TransferTraverserCriteria.LabEventDescendantCriteria();
        for (LabVessel labVessel : labBatch.getStartingBatchLabVessels()) {
            labVessel.evaluateCriteria(labEventDescendantCriteria,
                    TransferTraverserCriteria.TraversalDirection.Descendants);
        }
        for (LabEvent labEvent : labEventDescendantCriteria.getAllEvents()) {
            if (labEvent.getEventDate().before(labBatch.getCreatedOn())) {
                continue;
            }
            LabEventKey labEventKey = new LabEventKey(labEvent);
            List<LabEvent> labEvents = mapTypeToEvent.get(labEventKey);
            if (labEvents == null) {
                labEvents = new ArrayList<>();
                mapTypeToEvent.put(labEventKey, labEvents);
            }
            labEvents.add(labEvent);
        }

        // match events to steps
        for (WorkflowProcessDef workflowProcessDef : productWorkflowDefVersion.getWorkflowProcessDefs()) {
            WorkflowProcessDefVersion effectiveVersion = workflowProcessDef.getEffectiveVersion();
            for (WorkflowStepDef workflowStepDef : effectiveVersion.getWorkflowStepDefs()) {
                for (LabEventType labEventType : workflowStepDef.getLabEventTypes()) {
                    List<LabEvent> labEvents = mapTypeToEvent.remove(
                            new LabEventKey(labEventType, workflowStepDef.getWorkflowQualifier()));
                    // Don't include bucket event, it's artificial and confuses the users
                    if (!(workflowStepDef instanceof WorkflowBucketDef)) {
                        workflowEvents.add(new WorkflowEvent(
                                workflowStepDef,
                                labEvents));
                    }
                }
            }
        }

        // Add non-matched events in chronological order
        Iterator<Map.Entry<LabEventKey, List<LabEvent>>> iterator = mapTypeToEvent.entrySet().iterator();
        while (iterator.hasNext()) {
            Map.Entry<LabEventKey, List<LabEvent>> labEventKeyListEntry =  iterator.next();
            Date unMatchedEventDate = labEventKeyListEntry.getValue().get(0).getEventDate();
            for (int i = 0; i < workflowEvents.size(); i++) {
                if (workflowEvents.get(i).getLabEvents() == null) {
                    continue;
                }
                if (workflowEvents.get(i).getLabEvents().get(0).getEventDate().after(unMatchedEventDate)) {
                    workflowEvents.add(i, new WorkflowEvent(null, labEventKeyListEntry.getValue()));
                    iterator.remove();
                    break;
                }
            }
        }

        boolean foundEvents = false;
        for (int i = workflowEvents.size() - 1; i >= 0; i--) {
            WorkflowEvent workflowEvent = workflowEvents.get(i);
            if (!CollectionUtils.isEmpty(workflowEvent.getLabEvents())) {
                foundEvents = true;
            } else if (foundEvents) {
                if (CollectionUtils.isEmpty(workflowEvent.getLabEvents())) {
                    workflowEvent.setSkipped(true);
                }
            }
        }

        // Add any remainders to the end
        for (Map.Entry<LabEventKey, List<LabEvent>> labEventKeyListEntry : mapTypeToEvent.entrySet()) {
            workflowEvents.add(new WorkflowEvent(null, labEventKeyListEntry.getValue()));
        }

        return workflowEvents;
    }
}

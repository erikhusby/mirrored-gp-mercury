package org.broadinstitute.sequel.entity.project;

import org.broadinstitute.sequel.entity.labevent.GenericLabEvent;
import org.broadinstitute.sequel.entity.labevent.LabEvent;
import org.broadinstitute.sequel.entity.vessel.LabVessel;
import org.broadinstitute.sequel.entity.workflow.WorkflowAnnotation;
import org.broadinstitute.sequel.entity.workflow.WorkflowState;
import org.broadinstitute.sequel.entity.workflow.WorkflowTransition;
import org.broadinstitute.sequel.infrastructure.jira.issue.CreateIssueRequest;
import org.broadinstitute.sequel.infrastructure.quote.PriceItem;
import org.broadinstitute.sequel.entity.labevent.LabEventName;

import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.SequenceGenerator;
import javax.persistence.Transient;
import java.util.*;

/**
 * A stub description of the end-to-end
 * workflow.  Things like "Hybrid Selection v8.0"
 * and "WGS 7.2"
 */
@Entity
public class WorkflowDescription {

    @Id
    @SequenceGenerator(name = "SEQ_WORKFLOW_DESCRIPTION", sequenceName = "SEQ_WORKFLOW_DESCRIPTION")
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "SEQ_WORKFLOW_DESCRIPTION")
    private Long workflowDescriptionId;

    private String workflowName;

    // todo jmt fix this
    @Transient
    private Map<LabEventName,PriceItem> priceItemForEvent = new HashMap<LabEventName, PriceItem>();

    private CreateIssueRequest.Fields.Issuetype issueType;

    // todo jmt fix this
    @Transient
    private Map<String, List<WorkflowTransition>> mapNameToTransitionList = new HashMap<String, List<WorkflowTransition>>();
    // todo jmt fix this
    @Transient
    private WorkflowState startState;

    /**
     *
     * @param workflowName
     * @param billableEvents
     * @param issueType the <i>first</i> type of issue
 *                  created when this stuff hits
 *                  the lab.  Subsequent ticket
 *                  creation is owned by either {@link org.broadinstitute.sequel.entity.workflow.WorkflowEngine}
 *                  or {@link org.broadinstitute.sequel.entity.queue.LabWorkQueue}.
     */
    public WorkflowDescription(String workflowName,
                               Map<LabEventName, PriceItem> billableEvents,
                               CreateIssueRequest.Fields.Issuetype issueType) {
        if (workflowName == null) {
             throw new IllegalArgumentException("workflowName must be non-null in WorkflowDescription.WorkflowDescription");
        }
        this.workflowName = workflowName;
        this.priceItemForEvent = billableEvents;
        this.issueType = issueType;
    }

    protected WorkflowDescription() {
    }

    public String getWorkflowName() {
        return workflowName;
    }

    public PriceItem getPriceItem(LabEventName eventName) {
        if (eventName == null) {
            throw new NullPointerException("eventName cannot be null");
        }
        return priceItemForEvent.get(eventName);
    }

    public String getJiraProjectPrefix() {
        return "LCSET";
    }

    public CreateIssueRequest.Fields.Issuetype getJiraIssueType() {
        return issueType;
    }

    public Collection<WorkflowAnnotation> getAnnotations(String eventTypeName) {
        final Collection<WorkflowAnnotation> workflowAnnotations = new ArrayList<WorkflowAnnotation>();
        for (WorkflowTransition workflowTransition : mapNameToTransitionList.get(eventTypeName)) {
            workflowAnnotations.addAll(workflowTransition.getWorkflowAnnotations());
        }
        return workflowAnnotations;
    }

    public List<String> validate(List<LabVessel> labVessels, String nextEventTypeName) {
        List<String> errors = new ArrayList<String>();
        List<WorkflowTransition> workflowTransitions = this.mapNameToTransitionList.get(nextEventTypeName);
        Set<String> validPredecessorEventNames = new HashSet<String>();
        boolean start = false;
        for (WorkflowTransition workflowTransition : workflowTransitions) {
            if(workflowTransition.getFromState().getState().equals("Start Event")) {
                start = true;
            }
            for (WorkflowTransition predecessor : workflowTransition.getFromState().getEntries()) {
                validPredecessorEventNames.add(predecessor.getEventTypeName());
            }
        }

        for (LabVessel labVessel : labVessels) {
            Set<String> actualEventNames = new HashSet<String>();
            boolean found = false;
            found = validateTransfers(nextEventTypeName, errors, validPredecessorEventNames, labVessel, actualEventNames,
                    found, labVessel.getTransfersFrom());

            if (!found) {
                found = validateTransfers(nextEventTypeName, errors, validPredecessorEventNames, labVessel, actualEventNames,
                        found, labVessel.getTransfersTo());
            }
            if(!found) {
                found = validateTransfers(nextEventTypeName, errors, validPredecessorEventNames, labVessel, actualEventNames,
                        found, labVessel.getInPlaceEvents());
            }
            if(!found && !start) {
                errors.add("Vessel " + labVessel.getLabCentricName() + " has actual events " + actualEventNames +
                        ", but none are predecessors to " + nextEventTypeName + ": " + validPredecessorEventNames);
            }
        }
        return errors;
    }

    private boolean validateTransfers(String nextEventTypeName, List<String> errors, Set<String> validPredecessorEventNames,
            LabVessel labVessel, Set<String> actualEventNames, boolean found, Set<LabEvent> transfers) {
        for (LabEvent labEvent : transfers) {
            GenericLabEvent genericLabEvent = (GenericLabEvent) labEvent;
            String actualEventName = genericLabEvent.getLabEventType().getName();
            actualEventNames.add(actualEventName);
            if(actualEventName.equals(nextEventTypeName)) {
                errors.add("For vessel " + labVessel.getLabCentricName() + ", event " + nextEventTypeName + " has already occurred");
            }
            if(validPredecessorEventNames.contains(actualEventName)) {
                found = true;
                break;
            }
        }
        return found;
    }

    public void setStartState(WorkflowState startState) {
        this.startState = startState;
    }

    public void setMapNameToTransitionList(Map<String, List<WorkflowTransition>> mapNameToTransitionList) {
        this.mapNameToTransitionList = mapNameToTransitionList;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        WorkflowDescription that = (WorkflowDescription) o;

        if (workflowName != null ? !workflowName.equals(that.workflowName) : that.workflowName != null) return false;

        return true;
    }

    @Override
    public int hashCode() {
        return workflowName != null ? workflowName.hashCode() : 0;
    }
}

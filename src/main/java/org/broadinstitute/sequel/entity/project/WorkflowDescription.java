package org.broadinstitute.sequel.entity.project;

import org.broadinstitute.sequel.entity.labevent.GenericLabEvent;
import org.broadinstitute.sequel.entity.labevent.LabEvent;
import org.broadinstitute.sequel.entity.vessel.LabVessel;
import org.broadinstitute.sequel.entity.workflow.WorkflowState;
import org.broadinstitute.sequel.entity.workflow.WorkflowTransition;
import org.broadinstitute.sequel.infrastructure.jira.issue.CreateIssueRequest;
import org.broadinstitute.sequel.infrastructure.quote.PriceItem;
import org.broadinstitute.sequel.entity.labevent.LabEventName;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * A stub description of the end-to-end
 * workflow.  Things like "Hybrid Selection v8.0"
 * and "WGS 7.2"
 */
public class WorkflowDescription {

    private final String workflowName;

    private final String version;

    private Map<LabEventName,PriceItem> priceItemForEvent = new HashMap<LabEventName, PriceItem>();

    private CreateIssueRequest.Fields.Issuetype issueType;

    private Map<String, List<WorkflowTransition>> mapNameToTransitionList = new HashMap<String, List<WorkflowTransition>>();
    private WorkflowState startState;

    /**
     * 
     * @param workflowName
     * @param version
     * @param billableEvents
     * @param issueType the <i>first</i> type of issue
     *                  created when this stuff hits
     *                  the lab.  Subsequent ticket
     *                  creation is owned by either {@link org.broadinstitute.sequel.entity.workflow.WorkflowEngine}
     *                  or {@link org.broadinstitute.sequel.entity.queue.LabWorkQueue}.
     */
    public WorkflowDescription(String workflowName,
                               String version,
                               Map<LabEventName,PriceItem> billableEvents,
                               CreateIssueRequest.Fields.Issuetype issueType) {
        if (workflowName == null) {
             throw new IllegalArgumentException("workflowName must be non-null in WorkflowDescription.WorkflowDescription");
        }
        this.workflowName = workflowName;
        this.version = version;
        this.priceItemForEvent = billableEvents;
        this.issueType = issueType;
    }
    
    public String getWorkflowName() {
        return workflowName;
    }
    
    public String getWorkflowVersion() {
        return version;
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
            if(!found && !start) {
                errors.add("Vessel " + labVessel.getLabCentricName() + " has actual events " + actualEventNames +
                        ", but none are predecessors to " + nextEventTypeName + ": " + validPredecessorEventNames);
            }
        }
        return errors;
    }

    private boolean validateTransfers(String nextEventTypeName, List<String> errors, Set<String> validPredecessorEventNames,
            LabVessel labVessel, Set<String> actualEventNames, boolean found, Collection<LabEvent> transfersFrom) {
        for (LabEvent labEvent : transfersFrom) {
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
}

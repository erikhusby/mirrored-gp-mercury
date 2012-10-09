package org.broadinstitute.gpinformatics.mercury.entity.project;

import org.broadinstitute.gpinformatics.mercury.control.workflow.WorkflowParser;
import org.broadinstitute.gpinformatics.mercury.entity.labevent.GenericLabEvent;
import org.broadinstitute.gpinformatics.mercury.entity.labevent.LabEvent;
import org.broadinstitute.gpinformatics.mercury.entity.vessel.LabVessel;
import org.broadinstitute.gpinformatics.mercury.entity.workflow.WorkflowAnnotation;
import org.broadinstitute.gpinformatics.mercury.entity.workflow.WorkflowState;
import org.broadinstitute.gpinformatics.mercury.entity.workflow.WorkflowTransition;
import org.broadinstitute.gpinformatics.infrastructure.jira.issue.CreateIssueRequest;
import org.broadinstitute.gpinformatics.infrastructure.quote.PriceItem;
import org.hibernate.envers.Audited;

import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.SequenceGenerator;
import javax.persistence.Table;
import javax.persistence.Transient;
import java.io.InputStream;
import java.util.*;

/**
 * A stub description of the end-to-end
 * workflow.  Things like "Hybrid Selection v8.0"
 * and "WGS 7.2"
 */
@Entity
@Audited
@Table(schema = "mercury")
public class WorkflowDescription {

    @Id
    @SequenceGenerator(name = "SEQ_WORKFLOW_DESCRIPTION", schema = "mercury", sequenceName = "SEQ_WORKFLOW_DESCRIPTION")
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "SEQ_WORKFLOW_DESCRIPTION")
    private Long workflowDescriptionId;

    private String workflowName;

    // todo jmt fix this
    @Transient
    private PriceItem priceItem;

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
     * @param priceItem
     * @param issueType the <i>first</i> type of issue
 *                  created when this stuff hits
 *                  the lab.  Subsequent ticket
 *                  creation is owned by either {@link org.broadinstitute.gpinformatics.mercury.entity.workflow.WorkflowEngine}
 *                  or {@link org.broadinstitute.gpinformatics.mercury.entity.queue.LabWorkQueue}.
     */
    public WorkflowDescription(String workflowName,
                               PriceItem priceItem,
                               CreateIssueRequest.Fields.Issuetype issueType) {
        if (workflowName == null) {
             throw new IllegalArgumentException("workflowName must be non-null in WorkflowDescription.WorkflowDescription");
        }
        this.workflowName = workflowName;
        this.priceItem = priceItem;
        this.issueType = issueType;
    }

    protected WorkflowDescription() {
    }

    public String getWorkflowName() {
        return workflowName;
    }

    public PriceItem getPriceItem() {
        return priceItem;
    }

    public String getJiraProjectPrefix() {
        return "LCSET";
    }

    public CreateIssueRequest.Fields.Issuetype getJiraIssueType() {
        return issueType;
    }

    public Collection<WorkflowAnnotation> getAnnotations(String eventTypeName) {
        final Collection<WorkflowAnnotation> workflowAnnotations = new ArrayList<WorkflowAnnotation>();
        if (mapNameToTransitionList.get(eventTypeName) != null) {
            for (WorkflowTransition workflowTransition : mapNameToTransitionList.get(eventTypeName)) {
                workflowAnnotations.addAll(workflowTransition.getWorkflowAnnotations());
            }
        }
        return workflowAnnotations;
    }

    public List<String> validate(List<LabVessel> labVessels, String nextEventTypeName) {
        List<String> errors = new ArrayList<String>();
        List<WorkflowTransition> workflowTransitions = this.mapNameToTransitionList.get(nextEventTypeName);
        if(workflowTransitions == null) {
            throw new RuntimeException("Failed to find transitions for event " + nextEventTypeName);
        }
        Set<String> validPredecessorEventNames = new HashSet<String>();
        boolean start = false;
        if (workflowTransitions != null) {
            for (WorkflowTransition workflowTransition : workflowTransitions) {
                if(workflowTransition.getFromState().getState().equals("Start Event")) {
                    start = true;
                }
                for (WorkflowTransition predecessor : workflowTransition.getFromState().getEntries()) {
                    validPredecessorEventNames.add(predecessor.getEventTypeName());
                }
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

    public void initFromFile(String filename) {
        InputStream bpmnStream = Thread.currentThread().getContextClassLoader().getResourceAsStream(filename);
        if (bpmnStream == null) {
            throw new NullPointerException(filename + " could not be loaded");
        }
        WorkflowParser workflowParser = new WorkflowParser(bpmnStream);
        this.startState = workflowParser.getStartState();
        this.mapNameToTransitionList = workflowParser.getMapNameToTransitionList();
    }
}

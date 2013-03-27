package org.broadinstitute.gpinformatics.mercury.entity.workflow;

import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;
import org.broadinstitute.gpinformatics.mercury.entity.OrmUtil;
import org.broadinstitute.gpinformatics.mercury.entity.labevent.LabEvent;
import org.broadinstitute.gpinformatics.mercury.entity.labevent.LabEventType;
import org.broadinstitute.gpinformatics.mercury.entity.vessel.LabVessel;

import javax.xml.bind.Unmarshaller;
import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlIDREF;
import java.io.Serializable;
import java.util.*;

/**
 * A version of a product workflow definition
 */
@XmlAccessorType(XmlAccessType.FIELD)
public class ProductWorkflowDefVersion implements Serializable {

    private static final long serialVersionUID = 20130101L;

    private String version;
    private Date effectiveDate;
    /** e.g. Library Construction */
    // When serializing, we want to refer to WorkflowConfig.workflowProcessDefs, not make copies of them
    @XmlIDREF
    private List<WorkflowProcessDef> workflowProcessDefs = new ArrayList<WorkflowProcessDef>();
    private transient Map<String, WorkflowProcessDef> processDefsByName = new HashMap<String, WorkflowProcessDef>();

    private List<String> entryPointsUsed = new ArrayList<String>();
    private transient Map<String, LabEventNode> mapNameToLabEvent;
    private transient LabEventNode rootLabEventNode;
    private transient ProductWorkflowDef productWorkflowDef;

    /** For JAXB */
    @SuppressWarnings("UnusedDeclaration")
    ProductWorkflowDefVersion() {
    }

    public ProductWorkflowDefVersion(String version, Date effectiveDate) {
        this.version = version;
        this.effectiveDate = effectiveDate;
    }

    public String getVersion() {
        return version;
    }

    public Date getEffectiveDate() {
        return effectiveDate;
    }

    public List<WorkflowProcessDef> getWorkflowProcessDefs() {
        return workflowProcessDefs;
    }

    public WorkflowProcessDef getProcessDefsByName (String processDefName) {
        return processDefsByName.get(processDefName);
    }

    public List<String> getEntryPointsUsed() {
        return entryPointsUsed;
    }

    public void addWorkflowProcessDef(WorkflowProcessDef workflowProcessDef) {
        this.workflowProcessDefs.add(workflowProcessDef);
        this.processDefsByName.put ( workflowProcessDef.getName (), workflowProcessDef );
        workflowProcessDef.setProductWorkflowDef(this);
    }

    public void addEntryPointUsed(String entryPoint) {
        this.entryPointsUsed.add(entryPoint);
    }

    public LabEventNode getRootLabEventNode() {
        return rootLabEventNode;
    }

    public List<WorkflowBucketDef> getBuckets() {
        List<WorkflowBucketDef> workflowBucketDefs = new ArrayList<WorkflowBucketDef>();
        for (WorkflowProcessDef workflowProcessDef : workflowProcessDefs) {
            workflowBucketDefs.addAll(workflowProcessDef.getEffectiveVersion().getBuckets());
        }
        return workflowBucketDefs;
    }

    public static class LabEventNode {
        private final LabEventType labEventType;
        private List<LabEventNode> predecessors = new ArrayList<LabEventNode>();
        private List<LabEventNode> successors = new ArrayList<LabEventNode>();

        private final WorkflowStepDef stepDef;

        LabEventNode ( LabEventType labEventType, WorkflowStepDef stepDef ) {
            this.labEventType = labEventType;
            this.stepDef = stepDef;
        }

        public LabEventType getLabEventType() {
            return labEventType;
        }

        public List<LabEventNode> getPredecessors() {
            return predecessors;
        }

        public List<LabEventNode> getSuccessors() {
            return successors;
        }

        void addPredecessor(LabEventNode predecessor) {
            predecessors.add(predecessor);
        }

        void addSuccessor(LabEventNode successor) {
            successors.add(successor);
        }

        public WorkflowStepDef getStepDef() {
            return stepDef;
        }
    }

    /**
     * This method catalogues all Workflow steps found in the workflow.  The workflow steps are catalogued in
     * a map indexed by an eventType which is affiliated with the step.  The steps are also associated with steps
     * that are considered their successors and predecessors.  This makes it easy to navigate through the workflow
     * when a user has at least one step
     */
    public void buildLabEventGraph() {
        LabEventNode previousNode = null;
        for (WorkflowProcessDef workflowProcessDef : workflowProcessDefs) {
            WorkflowProcessDefVersion effectiveProcessDef = workflowProcessDef.getEffectiveVersion();
            for (WorkflowStepDef workflowStepDef : effectiveProcessDef.getWorkflowStepDefs()) {
                workflowStepDef.setProcessDefVersion(effectiveProcessDef);
                workflowStepDef.setProcessDef(workflowProcessDef);
                for (LabEventType labEventType : workflowStepDef.getLabEventTypes()) {
                    // todo jmt optional should probably be on the message, not the step
                    LabEventNode labEventNode = new LabEventNode(labEventType, workflowStepDef );
                    if(mapNameToLabEvent.put(labEventType.getName(), labEventNode) != null) {
                        throw new RuntimeException("Duplicate lab event in workflow, " + labEventType.getName());
                    }
                    if(rootLabEventNode == null) {
                        rootLabEventNode = labEventNode;
                    }
                    if (previousNode != null) {
                        labEventNode.addPredecessor(previousNode);
                        previousNode.addSuccessor(labEventNode);
                    }
                    previousNode = labEventNode;
                }
            }
        }
    }

    /**
     * This method will find the workflow step that is associated with the given Event type
     * @param eventTypeName name of an event to use as an index to the cataloged workflow steps
     * @return
     */
    public LabEventNode findStepByEventType(String eventTypeName) {
        if (mapNameToLabEvent == null) {
            mapNameToLabEvent = new HashMap<String, LabEventNode>();
            buildLabEventGraph();
        }
        return mapNameToLabEvent.get(eventTypeName);
    }

    /**
     * Based on the name of an event type, determine if the known previous workflow step is a bucket
     *
     * @param eventTypeName Event name associated with workflow process step
     * @return true if the step before the one associated with with the given event name is defined as a bucket by the
     *         workflow
     */
    public boolean isPreviousStepBucket(String eventTypeName) {
        WorkflowStepDef previousStep = getPreviousStep(eventTypeName);

        return OrmUtil.proxySafeIsInstance(previousStep, WorkflowBucketDef.class);
    }

    /**
     * Based on the name of an event type, determine if the known next workflow step is a bucket
     *
     * @param eventTypeName Event name associated with workflow process step
     * @return true if the step after the one associated with with the given event name is defined as a bucket by the
     *         workflow
     */
    public boolean isNextStepBucket(String eventTypeName) {
        WorkflowStepDef nextStep = getNextStep(eventTypeName);

        return OrmUtil.proxySafeIsInstance(nextStep, WorkflowBucketDef.class);
    }

    /**
     * Similar to {@link #isNextStepBucket(String)}, This method uses the ability to navigates the workflow forward to
     * determine if the next workflow step, that is not found on a "Branch", is bucket step
     * @param eventTypeName
     * @return
     */
    public boolean isNextNonDeadBranchStepBucket(String eventTypeName) {
         WorkflowStepDef nextStep = getNextNonDeadBranchStep(eventTypeName);

         return OrmUtil.proxySafeIsInstance(nextStep, WorkflowBucketDef.class);
    }

    /**
     * This convenience method performs both a search of a workflow step based on an event and checks if that step
     * is defined as being on a workflow branch that dead ends
     *
     * @param eventTypeName
     * @return
     */
    public boolean isStepDeadBranch(String eventTypeName) {

        LabEventNode stepNode =findStepByEventType(eventTypeName);

        WorkflowStepDef step = null;
        boolean result = false;
        if(stepNode != null) {
             step = stepNode.getStepDef();
            result = step.isDeadEndBranch();
        }

        return result;
    }

    /**
     * Similar to {@link #isNextNonDeadBranchStepBucket(String)} this method navigates forward in the Workflow to find
     * the next step that is not located on a branch of the workflow that dead ends.
     *
     * @param eventTypeName
     * @return
     */
    public WorkflowStepDef getNextNonDeadBranchStep(String eventTypeName) {
        WorkflowStepDef nextStep = getNextStep(eventTypeName);
        while(nextStep != null &&
              nextStep.isDeadEndBranch()) {
            WorkflowStepDef tempStep = getNextStep(nextStep.getLabEventTypes().get(nextStep.getLabEventTypes().size()-1).getName());
            nextStep = tempStep;
        }
        return nextStep;
    }

    /**
     * Based on an event type name which is associated with a particular workflow process step, this method will return
     * the configured workflow step which is defined as the given event types' immediate predecessor
     *
     * @param eventTypeName Event name associated with workflow process step
     * @return an instance of a {@link WorkflowStepDef} that is associated with the step that is defined as the given
     *         event types' predecessor
     */
    public WorkflowStepDef getPreviousStep(String eventTypeName) {

        WorkflowStepDef foundStep = null;
        if(!(findStepByEventType(eventTypeName) == null) &&
           !findStepByEventType(eventTypeName).getPredecessors().isEmpty()) {
            foundStep = findStepByEventType(eventTypeName).getPredecessors().get(0).getStepDef();
        }

        return foundStep;
    }

    /**
     * Based on an event type name which is associated witha  particular workflow process step, this method will return
     * the configured workflow step which is defined as the given event types' immediate Successor
     *
     * @param eventTypeName Event name associated with workflow process step
     * @return an instance of a {@link WorkflowStepDef} that is associated with the step that is defined as the given
     *         event types' immediate predecessor
     */
    public WorkflowStepDef getNextStep(String eventTypeName) {

        WorkflowStepDef foundStep = null;

        if(!(findStepByEventType(eventTypeName) == null) &&
           !findStepByEventType(eventTypeName).getSuccessors().isEmpty()) {

            foundStep = findStepByEventType(eventTypeName).getSuccessors().get(0).getStepDef();

        }
        return foundStep;
    }

    /**
     * Determine whether the given next event is valid for the given lab vessel.
     * @param labVessel vessel, typically with event history
     * @param nextEventTypeName the event that the lab intends to do next
     * @return list of errors, empty if event is valid
     */
    public List<String> validate(LabVessel labVessel, String nextEventTypeName) {
        List<String> errors = new ArrayList<String>();

        LabEventNode labEventNode = findStepByEventType(nextEventTypeName);
        if(labEventNode == null) {
            errors.add("Failed to find " + nextEventTypeName + " in " + productWorkflowDef.getName() + " version " + getVersion());
        } else {
            Set<String> actualEventNames = new HashSet<String>();

            boolean start = labEventNode.getPredecessors().isEmpty();
            Set<String> validPredecessorEventNames = new HashSet<String>();
            for (LabEventNode predecessorNode : labEventNode.getPredecessors()) {
                validPredecessorEventNames.add(predecessorNode.getLabEventType().getName());
                // todo jmt recurse
                if(predecessorNode.getStepDef().isOptional()) {
                    start = predecessorNode.getPredecessors().isEmpty();
                    for (LabEventNode predPredEventNode : predecessorNode.getPredecessors()) {
                        validPredecessorEventNames.add(predPredEventNode.getLabEventType().getName());
                    }
                }
            }

            boolean found = false;
            found = validateTransfers(nextEventTypeName, errors, validPredecessorEventNames, labVessel, actualEventNames,
                    found, labVessel.getTransfersFrom(), labEventNode);

            if (!found) {
                found = validateTransfers(nextEventTypeName, errors, validPredecessorEventNames, labVessel, actualEventNames,
                        found, labVessel.getTransfersTo(), labEventNode);
            }
            if(!found) {
                found = validateTransfers(nextEventTypeName, errors, validPredecessorEventNames, labVessel, actualEventNames,
                        found, labVessel.getInPlaceEvents(), labEventNode);
            }
            if(!found && !start) {
                errors.add("Vessel " + labVessel.getLabCentricName() + ":" + labVessel.getType() + " has actual events " + actualEventNames +
                        ", but none are predecessors to " + nextEventTypeName + ": " + validPredecessorEventNames);
            }
        }
        return errors;
    }

    private boolean validateTransfers(String nextEventTypeName, List<String> errors, Set<String> validPredecessorEventNames,
            LabVessel labVessel, Set<String> actualEventNames, boolean found, Set<LabEvent> transfers,
            LabEventNode labEventNode) {
        for (LabEvent labEvent : transfers) {
            String actualEventName = labEvent.getLabEventType().getName();
            actualEventNames.add(actualEventName);
            if (false) {
                if(actualEventName.equals(nextEventTypeName) && labEventNode.getStepDef().getNumberOfRepeats() == 0) {
                    errors.add("For vessel " + labVessel.getLabCentricName() + ", event " + nextEventTypeName +
                            " has already occurred");
                }
            }
            if(actualEventName.equals(nextEventTypeName) || validPredecessorEventNames.contains(actualEventName)) {
                found = true;
                break;
            }
        }
        return found;
    }

    /**
     * Called by JAXB, sets relationship to parent.
     * @param unmarshaller JAXB
     * @param parent enclosing XML element
     */
    @SuppressWarnings("UnusedDeclaration")
    void afterUnmarshal(Unmarshaller unmarshaller, Object parent) {
        this.productWorkflowDef = (ProductWorkflowDef) parent;
    }

    @Override
    public boolean equals ( Object o ) {
        if ( this == o )
            return true;
        if ( !( o instanceof ProductWorkflowDefVersion ) )
            return false;

        ProductWorkflowDefVersion that = ( ProductWorkflowDefVersion ) o;

        return new EqualsBuilder().append(effectiveDate, that.effectiveDate).append(version, that.version).isEquals();
    }

    @Override
    public int hashCode () {

        return new HashCodeBuilder().append(version).append(effectiveDate).toHashCode();
    }
}

package org.broadinstitute.gpinformatics.mercury.entity.workflow;

import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.HashMultimap;
import com.google.common.collect.Multimap;
import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;
import org.broadinstitute.gpinformatics.athena.entity.orders.ProductOrder;
import org.broadinstitute.gpinformatics.mercury.boundary.lims.SystemRouter;
import org.broadinstitute.gpinformatics.mercury.control.workflow.WorkflowLoader;
import org.broadinstitute.gpinformatics.mercury.entity.OrmUtil;
import org.broadinstitute.gpinformatics.mercury.entity.labevent.LabEvent;
import org.broadinstitute.gpinformatics.mercury.entity.labevent.LabEventType;
import org.broadinstitute.gpinformatics.mercury.entity.vessel.LabVessel;

import javax.annotation.Nonnull;
import javax.xml.bind.Unmarshaller;
import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlIDREF;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * A version of a product workflow definition
 */
@XmlAccessorType(XmlAccessType.FIELD)
public class ProductWorkflowDefVersion implements Serializable {

    private static final long serialVersionUID = 20130101L;

    private String version;
    private Date effectiveDate;
    /**
     * e.g. Library Construction
     */
    // When serializing, we want to refer to WorkflowConfig.workflowProcessDefs, not make copies of them
    @XmlIDREF
    private final List<WorkflowProcessDef> workflowProcessDefs = new ArrayList<>();
    @XmlIDREF
    private final List<SequencingConfigDef> sequencingConfigDefs = new ArrayList<>();
    private transient Map<String, WorkflowProcessDef> processDefsByName = new HashMap<>();

    private final List<String> entryPointsUsed = new ArrayList<>();
    private transient Multimap<String, LabEventNode> mapNameToLabEvents;
    private transient LabEventNode rootLabEventNode;
    private transient ProductWorkflowDef productWorkflowDef;

    /**
     * e.g. SQUID, MERCURY or BOTH
     */
    private String routingRule;

    private String batchJiraIssueType;

    private String batchJiraProjectType;

    /**
     * True if a validation LCSET is expected in the near future.   When a product is first rolled out,
     * routingRule be BOTH, and inValidation will be false. After a few weeks, inValidation will be
     * set to true. After a few more weeks, inValidation will be set to false, and systemOfRecord will be set to
     * MERCURY.
     */
    private Boolean inValidation;

    /**
     * For ProductWorkflow level Jira Issue types
     */
    public String getProductWorkflowDefBatchJiraIssueType() {
        return batchJiraIssueType;
    }

    /**
     * For ProductWorkflow level Jira Project types
     */
    public String getProductWorkflowDefBatchJiraProjectType() {
        return batchJiraProjectType;
    }

    /**
     * For JAXB
     */
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

    public WorkflowProcessDef getProcessDefsByName(String processDefName) {
        return processDefsByName.get(processDefName);
    }

    public List<String> getEntryPointsUsed() {
        return entryPointsUsed;
    }

    public List<SequencingConfigDef> getSequencingConfigDefs() {
        return sequencingConfigDefs;
    }

    public void addWorkflowProcessDef(WorkflowProcessDef workflowProcessDef) {
        workflowProcessDefs.add(workflowProcessDef);
        processDefsByName.put(workflowProcessDef.getName(), workflowProcessDef);
        workflowProcessDef.setProductWorkflowDef(this);
    }

    public void addEntryPointUsed(String entryPoint) {
        entryPointsUsed.add(entryPoint);
    }

    public LabEventNode getRootLabEventNode() {
        return rootLabEventNode;
    }

    public ProductWorkflowDef getProductWorkflowDef() {
        return productWorkflowDef;
    }

    public List<WorkflowBucketDef> getBuckets() {
        List<WorkflowBucketDef> workflowBucketDefs = new ArrayList<>();
        for (WorkflowProcessDef workflowProcessDef : workflowProcessDefs) {
            workflowBucketDefs.addAll(workflowProcessDef.getEffectiveVersion().getBuckets());
        }
        return workflowBucketDefs;
    }

    public List<WorkflowBucketDef> getCreationBuckets() {
        List<WorkflowBucketDef> workflowBucketDefs = new ArrayList<>();
        for (WorkflowProcessDef workflowProcessDef : workflowProcessDefs) {
            workflowBucketDefs.addAll(workflowProcessDef.getEffectiveVersion().getBuckets());
        }
        return workflowBucketDefs;
    }

    public List<WorkflowBucketDef> getReworkBuckets() {
        List<WorkflowBucketDef> workflowBucketDefs = new ArrayList<>();
        for (WorkflowProcessDef workflowProcessDef : workflowProcessDefs) {
            workflowBucketDefs.addAll(workflowProcessDef.getEffectiveVersion().getBuckets());
        }
        return workflowBucketDefs;
    }

    /**
     * Accessor for the routing logic associated with a workflow instance.  The routing logic helps the system determine
     * which LIMS system ({@see MercuryOrSquid}) should be considered the primary system of record.  Based on this
     * value, mercury will either keep all LIMS related information to itself, share that information with another
     * system, or pass all information to another system
     *
     * @return String to represent the routing intent.  Values are based on enums found in
     *         {@link org.broadinstitute.gpinformatics.mercury.boundary.lims.SystemRouter.System}
     */
    public String getRoutingRule() {
        return routingRule;
    }

    /**
     * This method is an extension of {@link #getRoutingRule()}.  Since the values defined in the routingRule are
     * based on {@link org.broadinstitute.gpinformatics.mercury.boundary.lims.SystemRouter.System},
     * this method helps solidify that point.  It provides the user with an interpretation of the routing rule
     * in the form of a MercuryOrSquid enum
     *
     * @return an instance of
     *         {@link org.broadinstitute.gpinformatics.mercury.boundary.lims.SystemRouter.System} that
     *         corresponds to the String value found in the routing rule.
     */
    public SystemRouter.System getRouting() {
        return SystemRouter.System.valueOf(getRoutingRule());
    }

    public boolean getInValidation() {
        return inValidation == null ? false : inValidation;
    }

    public static class LabEventNode {
        private final LabEventType labEventType;
        private final List<LabEventNode> predecessors = new ArrayList<>();
        private final List<LabEventNode> successors = new ArrayList<>();

        private final WorkflowStepDef stepDef;

        LabEventNode(LabEventType labEventType, WorkflowStepDef stepDef) {
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
        mapNameToLabEvents = ArrayListMultimap.create();
        LabEventNode previousNode = null;
        for (WorkflowProcessDef workflowProcessDef : workflowProcessDefs) {
            WorkflowProcessDefVersion effectiveProcessDef = workflowProcessDef.getEffectiveVersion();
            for (WorkflowStepDef workflowStepDef : effectiveProcessDef.getWorkflowStepDefs()) {
                workflowStepDef.setProcessDefVersion(effectiveProcessDef);
                workflowStepDef.setProcessDef(workflowProcessDef);
                for (LabEventType labEventType : workflowStepDef.getLabEventTypes()) {
                    // todo jmt optional should probably be on the message, not the step
                    LabEventNode labEventNode = new LabEventNode(labEventType, workflowStepDef);
                    mapNameToLabEvents.put(labEventType.getName(), labEventNode);
                    if (rootLabEventNode == null) {
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
     *
     * @param eventTypeName name of an event to use as an index to the cataloged workflow steps
     *
     * @return found node
     */
    public LabEventNode findStepByEventType(String eventTypeName) {
        if (mapNameToLabEvents == null) {
            buildLabEventGraph();
        }
        Collection<LabEventNode> labEventNodes = mapNameToLabEvents.get(eventTypeName);
        if (labEventNodes.size() > 1) {
            throw new RuntimeException("More than one lab event for " + eventTypeName);
        }
        if (labEventNodes.isEmpty()){
            return null;
        }
        return labEventNodes.iterator().next();
    }

    /**
     * Finds the workflow steps that are associated with the given Event type
     *
     * @param eventTypeName name of an event to use as an index to the cataloged workflow steps
     *
     * @return found nodes
     */
    public Collection<LabEventNode> findStepsByEventType(String eventTypeName) {
        if (mapNameToLabEvents == null) {
            buildLabEventGraph();
        }
        return mapNameToLabEvents.get(eventTypeName);
    }

    public enum BucketingSource {
        PDO_SUBMISSION,
        LAB_BATCH_WS
    }

    /**
     * Scans the workflow's processes for a matching bucket.
     *
     * @return a map of buckets to vessels.
     */
    public Map<WorkflowBucketDef, Collection<LabVessel>> getInitialBucket(ProductOrder productOrder,
            List<LabVessel> labVessels, BucketingSource bucketingSource) {

        Multimap<WorkflowBucketDef, LabVessel> vesselBuckets = HashMultimap.create();
        for (WorkflowProcessDef workflowProcessDef : workflowProcessDefs) {
            for (WorkflowBucketDef bucketDef : workflowProcessDef.getEffectiveVersion().getBuckets()) {
                if (bucketingSource != BucketingSource.PDO_SUBMISSION || bucketDef.isAutoBucketFromPdoSubmission()) {
                    for (LabVessel vessel : labVessels) {
                        if (!vesselBuckets.containsValue(vessel)) {
                            if (bucketDef.meetsBucketCriteria(vessel, productOrder)) {
                                vesselBuckets.put(bucketDef, vessel);
                            }
                        }
                    }
                }
            }
        }
        return vesselBuckets.asMap();
    }

    /**
     * Finds the workflow bucket step with the given bucket name.
     *
     * @param bucketName the name of the bucket to find
     *
     * @return the bucket, or null if not found
     */
    public WorkflowBucketDef findBucketDefByName(String bucketName) {
        WorkflowBucketDef bucketDef = null;
        for (WorkflowProcessDef workflowProcessDef : workflowProcessDefs) {
            WorkflowProcessDefVersion workflowProcessDefVersion = workflowProcessDef.getEffectiveVersion();
            bucketDef = workflowProcessDefVersion.getBucketByName(bucketName);
            if (bucketDef != null) {
                break;
            }
        }
        return bucketDef;
    }

    /**
     * Based on the name of an event type, determine if the known previous workflow step is a bucket
     *
     * @param eventTypeName Event name associated with workflow process step
     *
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
     *
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
     */
    public boolean isNextNonDeadBranchStepBucket(String eventTypeName) {
        WorkflowStepDef nextStep = getNextNonDeadBranchStep(eventTypeName);

        return OrmUtil.proxySafeIsInstance(nextStep, WorkflowBucketDef.class);
    }

    /**
     * This convenience method performs both a search of a workflow step based on an event and checks if that step
     * is defined as being on a workflow branch that dead ends
     */
    public boolean isStepDeadBranch(String eventTypeName) {

        LabEventNode stepNode = findStepByEventType(eventTypeName);

        boolean result = false;
        if (stepNode != null) {
            WorkflowStepDef step = stepNode.getStepDef();
            result = step.isDeadEndBranch();
        }

        return result;
    }

    /**
     * Similar to {@link #isNextNonDeadBranchStepBucket(String)} this method navigates forward in the Workflow to find
     * the next step that is not located on a branch of the workflow that dead ends.
     */
    public WorkflowStepDef getNextNonDeadBranchStep(String eventTypeName) {
        WorkflowStepDef nextStep = getNextStep(eventTypeName);
        while (nextStep != null && nextStep.isDeadEndBranch()) {
            nextStep = getNextStep(nextStep.getLabEventTypes().get(nextStep.getLabEventTypes().size() - 1).getName());
        }
        return nextStep;
    }

    /**
     * Based on an event type name which is associated with a particular workflow process step, this method will return
     * the configured workflow step which is defined as the given event types' immediate predecessor
     *
     * @param eventTypeName Event name associated with workflow process step
     *
     * @return an instance of a {@link WorkflowStepDef} that is associated with the step that is defined as the given
     *         event types' predecessor
     */
    public WorkflowStepDef getPreviousStep(String eventTypeName) {

        WorkflowStepDef foundStep = null;
        LabEventNode stepByEventType = findStepByEventType(eventTypeName);
        if ((stepByEventType != null) && !stepByEventType.getPredecessors().isEmpty()) {
            foundStep = stepByEventType.getPredecessors().get(0).getStepDef();
        }

        return foundStep;
    }

    /**
     * Based on an event type name which is associated witha  particular workflow process step, this method will return
     * the configured workflow step which is defined as the given event types' immediate Successor
     *
     * @param eventTypeName Event name associated with workflow process step
     *
     * @return an instance of a {@link WorkflowStepDef} that is associated with the step that is defined as the given
     *         event types' immediate predecessor
     */
    public WorkflowStepDef getNextStep(String eventTypeName) {

        WorkflowStepDef foundStep = null;

        LabEventNode stepByEventType = findStepByEventType(eventTypeName);
        if ((stepByEventType != null) && !stepByEventType.getSuccessors().isEmpty()) {
            foundStep = stepByEventType.getSuccessors().get(0).getStepDef();
        }
        return foundStep;
    }

    public static class ValidationError {
        private final String message;
        private final Set<String> actualEventNames;
        private final Set<String> expectedEventNames;

        public ValidationError(String message, Set<String> actualEventNames, Set<String> expectedEventNames) {
            this.message = message;
            this.actualEventNames = actualEventNames;
            this.expectedEventNames = expectedEventNames;
        }

        public ValidationError(String message) {
            this(message, Collections.<String>emptySet(), Collections.<String>emptySet());
        }

        public String getMessage() {
            return message;
        }

        public Set<String> getActualEventNames() {
            return actualEventNames;
        }

        public Set<String> getExpectedEventNames() {
            return expectedEventNames;
        }
    }

    /**
     * Determine whether the given next event is valid for the given lab vessel.
     *
     * @param labVessel         vessel, typically with event history.
     * @param nextEventTypeNames ordered set of events the lab intends to do next.
     *
     * @return list of errors, empty if event is valid
     */
    public List<ValidationError> validate(LabVessel labVessel, Set<String> nextEventTypeNames) {
        List<ValidationError> errors = new ArrayList<>();

        Iterator<String> nextEventIterator = nextEventTypeNames.iterator();
        String nextEventTypeName = nextEventIterator.next();
        Collection<LabEventNode> labEventNodes = findStepsByEventType(nextEventTypeName);
        if (labEventNodes.isEmpty()) {
            errors.add(new ValidationError("Failed to find " + nextEventTypeName + " in " +
                                           productWorkflowDef.getName() + " version " + getVersion()));
        } else {
            Set<String> actualEventNames = new HashSet<>();

            boolean found = false;
            Set<String> validPredecessorEventNames = null;
            boolean start = false;
            LabEventNode foundLabEvent = null;
            for (LabEventNode labEventNode : labEventNodes) {
                validPredecessorEventNames = new HashSet<>();
                start = recurseToNonOptional(validPredecessorEventNames, labEventNode);

                found = validateTransfers(nextEventTypeName, errors, validPredecessorEventNames, actualEventNames,
                        found, labVessel.getTransfersFrom(), labEventNode);

                if (!found) {
                    found = validateTransfers(nextEventTypeName, errors, validPredecessorEventNames,
                            actualEventNames,
                            found, labVessel.getTransfersToWithReArrays(), labEventNode);
                }
                if (!found) {
                    found = validateTransfers(nextEventTypeName, errors, validPredecessorEventNames,
                            actualEventNames,
                            found, labVessel.getInPlaceEventsWithContainers(), labEventNode);
                }
                if (!found) {
                    // e.g. PicoBufferAddition after PicoTransfer
                    for (LabEvent labEvent : labVessel.getTransfersFrom()) {
                        for (LabVessel vessel : labEvent.getTargetLabVessels()) {
                            found = validateTransfers(nextEventTypeName, errors, validPredecessorEventNames,
                                    actualEventNames,
                                    found, vessel.getInPlaceLabEvents(), labEventNode);
                        }
                    }
                }
                if (found || start) {
                    foundLabEvent = labEventNode;
                }
            }
            if (!found && !start) {
                errors.add(new ValidationError("", actualEventNames, validPredecessorEventNames));
            } else {
                // Walk up workflow successor list to check
                List<LabEventNode> successors = foundLabEvent.getSuccessors();
                while(nextEventIterator.hasNext()) {
                    nextEventTypeName = nextEventIterator.next();
                    found = false;
                    for (LabEventNode labEventNode: successors) {
                        if (labEventNode.labEventType.getName().equals(nextEventTypeName)) {
                            found = true;
                            break;
                        }
                    }
                    if (!found) {
                        errors.add(new ValidationError("", Collections.singleton(nextEventTypeName),
                                Collections.singleton(foundLabEvent.getLabEventType().name())));
                    }
                }
            }
        }

        return errors;
    }

    /**
     * Add predecessors to list of valid events, and if they're optional, keep adding predecessors until find one
     * that is not optional.
     *
     * @param validPredecessorEventNames this method adds predecessors to this collection
     * @param labEventNode               node for which to evaluate predecessors
     *
     * @return true if reached start of workflow
     */
    private boolean recurseToNonOptional(Set<String> validPredecessorEventNames, LabEventNode labEventNode) {
        boolean start = labEventNode.getPredecessors().isEmpty();
        for (LabEventNode predecessorNode : labEventNode.getPredecessors()) {
            validPredecessorEventNames.add(predecessorNode.getLabEventType().getName());
            if (predecessorNode.getStepDef().isOptional()) {
                start = recurseToNonOptional(validPredecessorEventNames, predecessorNode);
            }
        }
        return start;
    }

    private static boolean validateTransfers(String nextEventTypeName, List<ValidationError> errors,
                                             Set<String> validPredecessorEventNames, Set<String> actualEventNames,
                                             boolean found, Set<LabEvent> transfers,
                                             LabEventNode labEventNode) {
        for (LabEvent labEvent : transfers) {
            String actualEventName = labEvent.getLabEventType().getName();
            actualEventNames.add(actualEventName);
            if (false) {
                if (actualEventName.equals(nextEventTypeName) && labEventNode.getStepDef().getNumberOfRepeats() == 0) {
                    errors.add(new ValidationError("Event " + nextEventTypeName + " has already occurred"));
                }
            }
            if (/*actualEventName.equals(nextEventTypeName) || */validPredecessorEventNames.contains(actualEventName)) {
                found = true;
                break;
            }
        }
        return found;
    }

    /**
     * Called by JAXB, sets relationship to parent.
     *
     * @param unmarshaller JAXB
     * @param parent       enclosing XML element
     */
    @SuppressWarnings("UnusedDeclaration")
    void afterUnmarshal(Unmarshaller unmarshaller, Object parent) {
        productWorkflowDef = (ProductWorkflowDef) parent;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof ProductWorkflowDefVersion)) {
            return false;
        }

        ProductWorkflowDefVersion that = (ProductWorkflowDefVersion) o;

        return new EqualsBuilder().append(effectiveDate, that.getEffectiveDate()).append(version, that.getVersion()).
                isEquals();
    }

    @Override
    public int hashCode() {
        return new HashCodeBuilder().append(version).append(effectiveDate).toHashCode();
    }
}

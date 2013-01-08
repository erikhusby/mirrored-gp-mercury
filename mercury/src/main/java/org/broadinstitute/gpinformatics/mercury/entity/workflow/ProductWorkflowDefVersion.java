package org.broadinstitute.gpinformatics.mercury.entity.workflow;

import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;
import org.broadinstitute.gpinformatics.mercury.entity.OrmUtil;
import org.broadinstitute.gpinformatics.mercury.entity.labevent.LabEventType;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlIDREF;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

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

    static class LabEventNode {
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

    public void buildLabEventGraph() {
        LabEventNode previousNode = null;
        for (WorkflowProcessDef workflowProcessDef : workflowProcessDefs) {
            WorkflowProcessDefVersion effectiveProcessDef = workflowProcessDef.getEffectiveVersion();
            for (WorkflowStepDef workflowStepDef : effectiveProcessDef.getWorkflowStepDefs()) {
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

    public LabEventNode findStepByEventType(String eventTypeName) {
        if (mapNameToLabEvent == null) {
            mapNameToLabEvent = new HashMap<String, LabEventNode>();
            buildLabEventGraph();
        }
        return mapNameToLabEvent.get(eventTypeName);
    }

    /**
     * Based on the name of an event type, determine if the known previous workflow step is a bucket
     * @param eventTypeName Event name associated with workflow process step
     * @return true if the step before the one associated with with the given event name is defined as a bucket by the
     * workflow
     */
    public boolean isPreviousStepBucket(String eventTypeName) {
        WorkflowStepDef previousStep = getPreviousStep(eventTypeName);

        return OrmUtil.proxySafeIsInstance ( previousStep, WorkflowBucketDef.class );
    }

    /**
     * Based on an event type name which is associated with a particular workflow process step, this method will return
     * the configured workflow step which is defined as the given event types' predecessor
     * @param eventTypeName Event name associated with workflow process step
     * @return an instance of a {@link WorkflowStepDef} that is associated with the step that is defined as the given
     * event types' predecessor
     */
    public WorkflowStepDef getPreviousStep(String eventTypeName) {
        return findStepByEventType(eventTypeName).getPredecessors().get(0).getStepDef();
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

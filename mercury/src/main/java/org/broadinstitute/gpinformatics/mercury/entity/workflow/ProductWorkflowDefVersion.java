package org.broadinstitute.gpinformatics.mercury.entity.workflow;

import org.broadinstitute.gpinformatics.mercury.entity.labevent.LabEventType;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlIDREF;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * A version of a product workflow definition
 */
@XmlAccessorType(XmlAccessType.FIELD)
public class ProductWorkflowDefVersion {

    private String version;
    private Date effectiveDate;
    /** e.g. Library Construction */
    // When serializing, we want to refer to WorkflowConfig.workflowProcessDefs, not make copies of them
    @XmlIDREF
    private List<WorkflowProcessDef> workflowProcessDefs = new ArrayList<WorkflowProcessDef>();

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

    public List<String> getEntryPointsUsed() {
        return entryPointsUsed;
    }

    public void addWorkflowProcessDef(WorkflowProcessDef workflowProcessDef) {
        this.workflowProcessDefs.add(workflowProcessDef);
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
        private LabEventType labEventType;
        private boolean optional;
        private List<LabEventNode> predecessors = new ArrayList<LabEventNode>();
        private List<LabEventNode> successors = new ArrayList<LabEventNode>();

        LabEventNode(LabEventType labEventType, boolean optional) {
            this.labEventType = labEventType;
            this.optional = optional;
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

        public boolean isOptional() {
            return optional;
        }
    }

    public void buildLabEventGraph() {
        LabEventNode previousNode = null;
        for (WorkflowProcessDef workflowProcessDef : workflowProcessDefs) {
            WorkflowProcessDefVersion effectiveProcessDef = workflowProcessDef.getEffectiveVersion();
            for (WorkflowStepDef workflowStepDef : effectiveProcessDef.getWorkflowStepDefs()) {
                for (LabEventType labEventType : workflowStepDef.getLabEventTypes()) {
                    // todo jmt optional should probably be on the message, not the step
                    LabEventNode labEventNode = new LabEventNode(labEventType, workflowStepDef.isOptional());
                    mapNameToLabEvent.put(labEventType.getName(), labEventNode);
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
}

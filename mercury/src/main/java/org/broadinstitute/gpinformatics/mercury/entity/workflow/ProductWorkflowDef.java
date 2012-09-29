package org.broadinstitute.gpinformatics.mercury.entity.workflow;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlIDREF;
import java.util.ArrayList;
import java.util.List;

/**
 * The workflow definition for a product, composed of processes
 */
@XmlAccessorType(XmlAccessType.FIELD)
public class ProductWorkflowDef {

    /** e.g. Exome Express */
    private String name;

    /** e.g. Library Construction */
    // When serializing, we want to refer to WorkflowConfig.workflowProcessDefs, not make copies of them
    @XmlIDREF
    private List<WorkflowProcessDef> workflowProcessDefs = new ArrayList<WorkflowProcessDef>();

    private List<String> entryPointsUsed = new ArrayList<String>();

    public ProductWorkflowDef(String name) {
        this.name = name;
    }

    /** For JAXB */
    ProductWorkflowDef() {
    }

    public String getName() {
        return name;
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

    public List<WorkflowBucketDef> getBuckets() {
        List<WorkflowBucketDef> workflowBucketDefs = new ArrayList<WorkflowBucketDef>();
        for (WorkflowProcessDef workflowProcessDef : workflowProcessDefs) {
            workflowBucketDefs.addAll(workflowProcessDef.getBuckets());
        }
        return workflowBucketDefs;
    }
}

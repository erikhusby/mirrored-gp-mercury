package org.broadinstitute.gpinformatics.mercury.entity.workflow;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlIDREF;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

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

    /** For JAXB */
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

    public List<WorkflowBucketDef> getBuckets() {
        List<WorkflowBucketDef> workflowBucketDefs = new ArrayList<WorkflowBucketDef>();
        for (WorkflowProcessDef workflowProcessDef : workflowProcessDefs) {
            workflowBucketDefs.addAll(workflowProcessDef.getCurrentVersion().getBuckets());
        }
        return workflowBucketDefs;
    }
}

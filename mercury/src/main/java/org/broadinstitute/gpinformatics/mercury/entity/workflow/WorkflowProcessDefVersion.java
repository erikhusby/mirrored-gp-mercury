package org.broadinstitute.gpinformatics.mercury.entity.workflow;

import org.broadinstitute.gpinformatics.mercury.entity.OrmUtil;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

/**
 * Represents a specific version of a workflow process definition
 */
@XmlAccessorType(XmlAccessType.FIELD)
public class WorkflowProcessDefVersion {
    private String version;
    private Date effectiveDate;
    /** Process graphs tend to be straight lines, with one or two optional steps (e.g. Normalization is optional in QTP,
     * because the concentration may be fine as is).  (In Squid workflow, new transitions are often initially optional,
     * to allow the automation to be rolled out, but this could be addressed in Mercury by versioning.)
     * Treating steps as lists would simplify visualization and editing. */
    private List<WorkflowStepDef> workflowStepDefs = new ArrayList<WorkflowStepDef>();

    /** For JAXB */
    WorkflowProcessDefVersion() {
    }

    public WorkflowProcessDefVersion(String version, Date effectiveDate) {
        this.version = version;
        this.effectiveDate = effectiveDate;
    }

    public void addStep(WorkflowStepDef workflowStepDef) {
        workflowStepDefs.add(workflowStepDef);
    }

    public List<WorkflowStepDef> getWorkflowStepDefs() {
        return workflowStepDefs;
    }

    /** At a QC review, the user needs to know the options for re-entry */
    public List<WorkflowStepDef> getReEntryPoints() {
        List<WorkflowStepDef> reEntryPoints = new ArrayList<WorkflowStepDef>();
        for (WorkflowStepDef workflowStepDef : workflowStepDefs) {
            if(workflowStepDef.isReEntryPoint()) {
                reEntryPoints.add(workflowStepDef);
            }
        }
        return reEntryPoints;
    }

    public List<WorkflowBucketDef> getBuckets() {
        List<WorkflowBucketDef> workflowBucketDefs = new ArrayList<WorkflowBucketDef>();
        for (WorkflowStepDef workflowStepDef : workflowStepDefs) {
            if(OrmUtil.proxySafeIsInstance(workflowStepDef, WorkflowBucketDef.class)) {
                workflowBucketDefs.add(OrmUtil.proxySafeCast(workflowStepDef, WorkflowBucketDef.class));
            }
        }
        return workflowBucketDefs;
    }
}

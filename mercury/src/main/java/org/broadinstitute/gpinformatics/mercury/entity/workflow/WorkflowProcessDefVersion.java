package org.broadinstitute.gpinformatics.mercury.entity.workflow;

import org.broadinstitute.gpinformatics.mercury.entity.OrmUtil;

import javax.xml.bind.Unmarshaller;
import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Represents a specific version of a workflow process definition
 */
@XmlAccessorType(XmlAccessType.FIELD)
public class WorkflowProcessDefVersion implements Serializable {
    private static final long serialVersionUID = 20130101L;

    private String version;
    private Date effectiveDate;
    /**
     * Process graphs tend to be straight lines, with one or two optional steps (e.g. Normalization is optional in QTP,
     * because the concentration may be fine as is).  (In Squid workflow, new transitions are often initially optional,
     * to allow the automation to be rolled out, but this could be addressed in Mercury by versioning.)
     * Treating steps as lists would simplify visualization and editing.
     */
    private List<WorkflowStepDef> workflowStepDefs = new ArrayList<>();
    private Map<String, WorkflowStepDef> workflowStepsByName = new HashMap<>();
    private WorkflowProcessDef workflowProcessDef;

    /**
     * For JAXB
     */
    WorkflowProcessDefVersion() {
    }

    public WorkflowProcessDefVersion(String version, Date effectiveDate) {
        this.version = version;
        this.effectiveDate = effectiveDate;
    }

    public void addStep(WorkflowStepDef workflowStepDef) {
        workflowStepDefs.add(workflowStepDef);
        workflowStepsByName.put(workflowStepDef.getName(), workflowStepDef);
        workflowStepDef.setProcessDefVersion(this);
    }

    public List<WorkflowStepDef> getWorkflowStepDefs() {
        return workflowStepDefs;
    }

    public String getVersion() {
        return version;
    }

    public Date getEffectiveDate() {
        return effectiveDate;
    }

    public WorkflowProcessDef getWorkflowProcessDef() {
        return workflowProcessDef;
    }

    /**
     * At a QC review, the user needs to know the options for re-entry
     */
    public List<WorkflowStepDef> getReEntryPoints() {
        List<WorkflowStepDef> reEntryPoints = new ArrayList<>();
        for (WorkflowStepDef workflowStepDef : workflowStepDefs) {
            if (workflowStepDef.isReEntryPoint()) {
                reEntryPoints.add(workflowStepDef);
            }
        }
        return reEntryPoints;
    }

    public List<WorkflowBucketDef> getBuckets() {
        List<WorkflowBucketDef> workflowBucketDefs = new ArrayList<>();
        for (WorkflowStepDef workflowStepDef : workflowStepDefs) {
            if (OrmUtil.proxySafeIsInstance(workflowStepDef, WorkflowBucketDef.class)) {
                workflowBucketDefs.add(OrmUtil.proxySafeCast(workflowStepDef, WorkflowBucketDef.class));
            }
        }
        return workflowBucketDefs;
    }

    /**
     * Returns batchJiraIssueType to override the value from productWorkflowDefs
     */
    public String getBatchJiraIssueType() {
        List<String> jiraIssueTypes = new ArrayList<>();
        for (WorkflowStepDef workflowStepDef : workflowStepDefs) {
            workflowStepDef.getBatchJiraIssueType();
            jiraIssueTypes.add(workflowStepDef.getBatchJiraIssueType());
        }
        return jiraIssueTypes.get(0);
    }

    /**
     * Returns batchJiraProjectType to override the value from productWorkflowDefs
     */
    public String getBatchJiraProjectType() {
        List<String> jiraProjectTypes = new ArrayList<>();
        for (WorkflowStepDef workflowStepDef : workflowStepDefs) {
            workflowStepDef.getBatchJiraIssueType();
            jiraProjectTypes.add(workflowStepDef.getBatchJiraProjectType());
        }
        return jiraProjectTypes.get(0);
    }

    /**
     * Returns the bucket with the specified name, or null of no bucket is found.
     *
     * @return the named bucket
     */
    public WorkflowBucketDef getBucketByName(String bucketName) {
        for (WorkflowBucketDef bucketDef : getBuckets()) {
            if (bucketDef.getName().equals(bucketName)) {
                return bucketDef;
            }
        }
        return null;
    }

    /**
     * Called by JAXB, sets relationship to parent.
     *
     * @param unmarshaller JAXB
     * @param parent       enclosing XML element
     */
    @SuppressWarnings("UnusedDeclaration")
    void afterUnmarshal(Unmarshaller unmarshaller, Object parent) {
        workflowProcessDef = (WorkflowProcessDef) parent;
    }

}

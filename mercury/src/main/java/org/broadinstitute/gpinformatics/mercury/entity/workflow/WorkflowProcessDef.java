package org.broadinstitute.gpinformatics.mercury.entity.workflow;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlID;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * A lab process, or team, e.g. QTP
 */
@XmlAccessorType(XmlAccessType.FIELD)
public class WorkflowProcessDef {

    @XmlID
    private String name;
    private List<WorkflowProcessDefVersion> workflowProcessDefVersions = new ArrayList<WorkflowProcessDefVersion>();
    private transient Map<String, WorkflowProcessDefVersion> processDefVersionsByVersion =
            new HashMap<String, WorkflowProcessDefVersion>();
    private transient List<WorkflowProcessDefVersion> processVersionsDescEffDate;

    private transient ProductWorkflowDefVersion productWorkflowDef;

    /** For JAXB */
    @SuppressWarnings("UnusedDeclaration")
    WorkflowProcessDef() {
    }

    public WorkflowProcessDef(String name) {
        this.name = name;
    }

    public String getName() {
        return name;
    }

    public void addWorkflowProcessDefVersion(WorkflowProcessDefVersion workflowProcessDefVersion) {
        this.workflowProcessDefVersions.add(workflowProcessDefVersion);
        this.processDefVersionsByVersion.put ( workflowProcessDefVersion.getVersion (), workflowProcessDefVersion );
    }

    public WorkflowProcessDefVersion getEffectiveVersion() {
        if (processVersionsDescEffDate == null) {
            processVersionsDescEffDate = new ArrayList<WorkflowProcessDefVersion>(workflowProcessDefVersions);
            Collections.sort(processVersionsDescEffDate, new Comparator<WorkflowProcessDefVersion>() {
                @Override
                public int compare(WorkflowProcessDefVersion o1, WorkflowProcessDefVersion o2) {
                    return o2.getEffectiveDate().compareTo(o1.getEffectiveDate());
                }
            });
        }
        Date now = new Date();
        WorkflowProcessDefVersion effectiveProcessDef = null;
        for (WorkflowProcessDefVersion workflowProcessDefVersion : processVersionsDescEffDate) {
            if(workflowProcessDefVersion.getEffectiveDate().before(now)) {
                effectiveProcessDef = workflowProcessDefVersion;
                break;
            }
        }
        assert effectiveProcessDef != null;
        return effectiveProcessDef;
    }

    public WorkflowProcessDefVersion getByVersion(String version) {
        return processDefVersionsByVersion.get(version);
    }

    public ProductWorkflowDefVersion getProductWorkflowDef () {
        return productWorkflowDef;
    }

    public void setProductWorkflowDef ( ProductWorkflowDefVersion productWorkflowDef ) {
        this.productWorkflowDef = productWorkflowDef;
    }
}

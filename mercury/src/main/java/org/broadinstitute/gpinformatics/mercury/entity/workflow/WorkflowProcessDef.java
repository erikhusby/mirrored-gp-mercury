package org.broadinstitute.gpinformatics.mercury.entity.workflow;

import org.broadinstitute.gpinformatics.mercury.entity.OrmUtil;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlID;
import java.util.ArrayList;
import java.util.List;

/**
 * A lab process, or team, e.g. QTP
 */
@XmlAccessorType(XmlAccessType.FIELD)
public class WorkflowProcessDef {

    @XmlID
    private String name;
    private List<WorkflowProcessDefVersion> workflowProcessDefVersions = new ArrayList<WorkflowProcessDefVersion>();

    /** For JAXB */
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
    }

    public WorkflowProcessDefVersion getCurrentVersion() {
        return workflowProcessDefVersions.get(0);
    }
}

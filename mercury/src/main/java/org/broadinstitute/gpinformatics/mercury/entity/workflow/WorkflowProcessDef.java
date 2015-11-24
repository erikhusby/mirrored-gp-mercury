package org.broadinstitute.gpinformatics.mercury.entity.workflow;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlID;
import java.io.Serializable;
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
public class WorkflowProcessDef implements Serializable {

    private static final long serialVersionUID = 20130101L;

    @XmlID
    private String name;
    private List<WorkflowProcessDefVersion> workflowProcessDefVersions = new ArrayList<>();
    private transient Map<String, WorkflowProcessDefVersion> processDefVersionsByVersion =
            new HashMap<>();
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

    /** Returns a list of all process defs sorted by decreasing effective date. */
    public List<WorkflowProcessDefVersion> getProcessVersionsDescEffDate() {
        if (processVersionsDescEffDate == null) {
            processVersionsDescEffDate = new ArrayList<>(workflowProcessDefVersions);
            Collections.sort(processVersionsDescEffDate, new Comparator<WorkflowProcessDefVersion>() {
                @Override
                public int compare(WorkflowProcessDefVersion o1, WorkflowProcessDefVersion o2) {
                    return o2.getEffectiveDate().compareTo(o1.getEffectiveDate());
                }
            });
        }
        return processVersionsDescEffDate;
    }

    /** Returns the process def that is in effect for the given date. */
    public WorkflowProcessDefVersion getEffectiveVersion(Date eventDate) {
        WorkflowProcessDefVersion effectiveProcessDef = null;
        for (WorkflowProcessDefVersion workflowProcessDefVersion : getProcessVersionsDescEffDate()) {
            // Should select workflow when effectiveDate <= eventDate.
            if (!workflowProcessDefVersion.getEffectiveDate().after(eventDate)) {
                effectiveProcessDef = workflowProcessDefVersion;
                break;
            }
        }
        // Breaks DBFree tests using mocks
        // assert effectiveProcessDef != null;
        return effectiveProcessDef;
    }

    /** Returns the process def for events that happen now. */
    public WorkflowProcessDefVersion getEffectiveVersion() {
        return getEffectiveVersion(new Date());
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

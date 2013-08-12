package org.broadinstitute.gpinformatics.mercury.entity.workflow;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlTransient;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Container for all workflow definition objects
 */
@XmlRootElement
@XmlAccessorType(XmlAccessType.FIELD)
public class WorkflowConfig {

    /** List of processes, or lab teams */
    private final List<WorkflowProcessDef> workflowProcessDefs = new ArrayList<>();

    /** List of product workflows, each composed of process definitions */
    private final List<ProductWorkflowDef> productWorkflowDefs = new ArrayList<>();
    @XmlTransient
    private Map<String, ProductWorkflowDef> mapNameToWorkflow;

    /** List of sequencing configs,  */
    private final List<SequencingConfigDef> sequencingConfigDefs = new ArrayList<>();
    @XmlTransient
    private Map<String, SequencingConfigDef> mapNameToSequencingConfig;

    public List<WorkflowProcessDef> getWorkflowProcessDefs() {
        return workflowProcessDefs;
    }

    public List<ProductWorkflowDef> getProductWorkflowDefs() {
        return productWorkflowDefs;
    }

    void addWorkflowProcessDef(WorkflowProcessDef workflowProcessDef) {
        workflowProcessDefs.add(workflowProcessDef);
    }

    void addProductWorkflowDef(ProductWorkflowDef productWorkflowDef) {
        productWorkflowDefs.add(productWorkflowDef);
    }

    public SequencingConfigDef getSequencingConfigByName(String sequencingConfigName) {
        if (mapNameToSequencingConfig == null) {
            mapNameToSequencingConfig = new HashMap<>();
            for (SequencingConfigDef sequencingConfigDef : sequencingConfigDefs) {
                mapNameToSequencingConfig.put(sequencingConfigDef.getName(), sequencingConfigDef);
            }
        }
        SequencingConfigDef sequencingConfigDef = mapNameToSequencingConfig.get(sequencingConfigName);
        if (sequencingConfigDef == null) {
            throw new WorkflowException("Failed to find sequencing config " + sequencingConfigName);
        }
        return sequencingConfigDef;
    }

    public ProductWorkflowDef getWorkflow(Workflow workflow) {
        return getWorkflowByName(workflow.getWorkflowName());
    }

    public ProductWorkflowDef getWorkflowByName(String workflowName) {
        if (mapNameToWorkflow == null) {
            mapNameToWorkflow = new HashMap<>();
            for (ProductWorkflowDef productWorkflowDef : productWorkflowDefs) {
                mapNameToWorkflow.put(productWorkflowDef.getName(), productWorkflowDef);
            }
        }
        ProductWorkflowDef productWorkflowDef = mapNameToWorkflow.get(workflowName);
        if (productWorkflowDef == null) {
            throw new WorkflowException("Failed to find workflow " + workflowName);
        }
        return productWorkflowDef;
    }

    public ProductWorkflowDefVersion getWorkflowVersionByName(String workflowName, Date effectiveDate) {
        ProductWorkflowDef workflowByName = getWorkflowByName(workflowName);
        return workflowByName.getEffectiveVersion(effectiveDate);
    }
}

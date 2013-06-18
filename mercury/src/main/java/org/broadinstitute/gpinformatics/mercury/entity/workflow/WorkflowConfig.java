package org.broadinstitute.gpinformatics.mercury.entity.workflow;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlRootElement;
import java.util.ArrayList;
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
    private List<WorkflowProcessDef> workflowProcessDefs = new ArrayList<>();

    /** List of product workflows, each composed of process definitions */
    private List<ProductWorkflowDef> productWorkflowDefs = new ArrayList<>();
    private transient Map<String, ProductWorkflowDef> mapNameToWorkflow;

    /** List of sequencing configs,  */
    private List<SequencingConfigDef> sequencingConfigDefs = new ArrayList<>();
    private transient Map<String, SequencingConfigDef> mapNameToSequencingConfig;

    public List<WorkflowProcessDef> getWorkflowProcessDefs() {
        return workflowProcessDefs;
    }

    public List<ProductWorkflowDef> getProductWorkflowDefs() {
        return productWorkflowDefs;
    }

    public void setWorkflowProcessDefs(List<WorkflowProcessDef> defs) {
        workflowProcessDefs = defs;
    }

    public void setProductWorkflowDefs(List<ProductWorkflowDef> defs) {
        productWorkflowDefs = defs;
    }

    void addWorkflowProcessDef(WorkflowProcessDef workflowProcessDef) {
        this.workflowProcessDefs.add(workflowProcessDef);
    }

    void addProductWorkflowDef(ProductWorkflowDef productWorkflowDef) {
        this.productWorkflowDefs.add(productWorkflowDef);
    }

    public SequencingConfigDef getSequencingConfigByName(String sequencingConfigName) {
        if (mapNameToSequencingConfig == null) {
            mapNameToSequencingConfig = new HashMap<String, SequencingConfigDef>();
            for (SequencingConfigDef sequencingConfigDef : sequencingConfigDefs) {
                mapNameToSequencingConfig.put(sequencingConfigDef.getName(), sequencingConfigDef);
            }
        }
        SequencingConfigDef sequencingConfigDef = mapNameToSequencingConfig.get(sequencingConfigName);
        if(sequencingConfigDef == null) {
            throw new WorkflowException("Failed to find sequencing config " + sequencingConfigName);
        }
        return sequencingConfigDef;
    }

    public ProductWorkflowDef getWorkflowByName(String workflowName) {
        if (mapNameToWorkflow == null) {
            mapNameToWorkflow = new HashMap<String, ProductWorkflowDef>();
            for (ProductWorkflowDef productWorkflowDef : productWorkflowDefs) {
                mapNameToWorkflow.put(productWorkflowDef.getName(), productWorkflowDef);
            }
        }
        ProductWorkflowDef productWorkflowDef = mapNameToWorkflow.get(workflowName);
        if(productWorkflowDef == null) {
            throw new WorkflowException("Failed to find workflow " + workflowName);
        }
        return productWorkflowDef;
    }
}

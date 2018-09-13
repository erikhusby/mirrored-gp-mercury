package org.broadinstitute.gpinformatics.mercury.entity.workflow;

import com.google.common.collect.HashMultimap;
import org.broadinstitute.gpinformatics.athena.entity.orders.ProductOrder;
import org.broadinstitute.gpinformatics.athena.entity.preference.PreferenceDefinitionCreator;
import org.broadinstitute.gpinformatics.athena.entity.preference.PreferenceDefinitionValue;

import javax.annotation.Nonnull;
import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Unmarshaller;
import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlTransient;
import java.io.Reader;
import java.io.Serializable;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Container for all workflow definition objects
 * Persistable in preferences
 */
@XmlRootElement
@XmlAccessorType(XmlAccessType.FIELD)
public class WorkflowConfig implements PreferenceDefinitionValue, Serializable {

    // JAXBContext is threadsafe
    @XmlTransient
    private static JAXBContext JAXB_CTX = null;

    /** List of processes, or lab teams */
    private final List<WorkflowProcessDef> workflowProcessDefs;

    /** List of product workflows, each composed of process definitions */
    private final List<ProductWorkflowDef> productWorkflowDefs;
    @XmlTransient
    private Map<String, ProductWorkflowDef> mapNameToWorkflow;

    public Map<WorkflowStepDef, Collection<ProductWorkflowDef>> getMapProcessDefToWorkflow() {
        return mapBucketToProductWorkflows;
    }

    @XmlTransient
    private Map<WorkflowStepDef, Collection<ProductWorkflowDef>> mapBucketToProductWorkflows;

    /** List of sequencing configs,  */
    private final List<SequencingConfigDef> sequencingConfigDefs = new ArrayList<>();
    @XmlTransient
    private Map<String, SequencingConfigDef> mapNameToSequencingConfig;

    public WorkflowConfig() {
        this(new ArrayList<WorkflowProcessDef>(), new ArrayList<ProductWorkflowDef>());
    }

    // Only called directly from test code.
    public WorkflowConfig(List<WorkflowProcessDef> workflowProcessDefs, List<ProductWorkflowDef> productWorkflowDefs) {
        this.workflowProcessDefs = workflowProcessDefs;
        this.productWorkflowDefs = productWorkflowDefs;
    }

    public List<ProductWorkflowDef> getProductWorkflowDefs() {
        return productWorkflowDefs;
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

    public ProductWorkflowDef getWorkflow(@Nonnull Workflow workflow) {
        return getWorkflowByName(workflow.getWorkflowName());
    }

    public ProductWorkflowDef getWorkflowByName(String workflowName) {
        HashMultimap<WorkflowStepDef, ProductWorkflowDef> bucketWorkflowsMap = HashMultimap.create();

        if (mapNameToWorkflow == null) {
            mapNameToWorkflow = new HashMap<>();
            for (ProductWorkflowDef productWorkflowDef : productWorkflowDefs) {
                mapNameToWorkflow.put(productWorkflowDef.getName(), productWorkflowDef);
                for (WorkflowBucketDef workflowBucketDef : productWorkflowDef.getEffectiveVersion().getBuckets()) {
                    bucketWorkflowsMap.put(workflowBucketDef, productWorkflowDef);
                }
            }
            mapBucketToProductWorkflows = bucketWorkflowsMap.asMap();
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

    /**
     * Return a sequential list of all steps in a workflow
     * @param productWorkflowName The workflow to extract
     * @param effectiveDate Required to determine the workflow and process version
     * @return A list of all workflow steps, otherwise an empty list if no workflows for name/date provided
     */
    public List<WorkflowStepDef> getSequentialWorkflowSteps( String productWorkflowName, Date effectiveDate){

        List<WorkflowStepDef> workflowStepList = new ArrayList<>();

        // Workflow parent with process definitions as children
        ProductWorkflowDefVersion workflowDef = getWorkflowVersionByName(
                productWorkflowName, effectiveDate );

        if( workflowDef == null) {
            return workflowStepList;
        }

        for( WorkflowProcessDef processDef : workflowDef.getWorkflowProcessDefs() ){
            // Process definition version sequential steps
            // Stop at the workflow step corresponding to the current event
            WorkflowProcessDefVersion workflowProcessDefVersion =  processDef.getEffectiveVersion(effectiveDate);
            if( workflowProcessDefVersion != null ) {
                for( WorkflowStepDef workflowStepDef : processDef.getEffectiveVersion(effectiveDate).getWorkflowStepDefs() ) {
                    workflowStepList.add(workflowStepDef);
                }
            }
        }

        return workflowStepList;
    }

    /**
     * Produce XML to store in preference table
     *
     * @return
     */
    @Override
    public String marshal() {
        // As of 05/2015, there are circular relationships created in the object hierarchy which fail marshall.
        // (WorkflowConfig is not application editable so this method is not called)
        throw new RuntimeException( "Marshalling of WorkflowConfig to Preference.data XML not supported");
    }

    /**
     * Produce WorkflowConfig object from XML data in preference table
     * @return
     */
    @Override
    public PreferenceDefinitionValue unmarshal(String xml) {
        PreferenceDefinitionValue workflowConfig = null;
        Reader reader = null;
        try {
            reader = new StringReader(xml);
            if( JAXB_CTX == null ) {
                JAXB_CTX = JAXBContext.newInstance(WorkflowConfig.class, WorkflowBucketDef.class);
            }
            Unmarshaller unmarshaller = JAXB_CTX.createUnmarshaller();
            workflowConfig = (WorkflowConfig) unmarshaller.unmarshal(reader);
        } catch (JAXBException e) {
            throw new RuntimeException(e);
        }
        return workflowConfig;
    }

    /**
     * Property of PreferenceType enum used to create instance of WorkflowConfig from preference
     */
    public static class WorkflowConfigPreferenceDefinitionCreator implements PreferenceDefinitionCreator {
        @Override
        public PreferenceDefinitionValue create(String xml) throws Exception {
            WorkflowConfig workflowConfig = new WorkflowConfig();
            return workflowConfig.unmarshal(xml);
        }
    }

    public WorkflowStepDef getStep(String workflowProcessName, String workflowStepName, Date workflowEffectiveDate) {
        for (WorkflowProcessDef workflowProcessDef : workflowProcessDefs) {
            if (workflowProcessDef.getName().equals(workflowProcessName)) {
                WorkflowProcessDefVersion effectiveVersion = workflowProcessDef.getEffectiveVersion(
                        workflowEffectiveDate);
                for (WorkflowStepDef workflowStepDef : effectiveVersion.getWorkflowStepDefs()) {
                    if (workflowStepDef.getName().equals(workflowStepName)) {
                        return workflowStepDef;
                    }
                }
            }
        }
        return null;
    }

    public WorkflowBucketDef findWorkflowBucketDef(@Nonnull ProductOrder productOrder, String bucketName) {
        for (Workflow productWorkflow : productOrder.getProductWorkflows()) {
            ProductWorkflowDefVersion workflowDefVersion = getWorkflow(productWorkflow)
                    .getEffectiveVersion();
            WorkflowBucketDef bucketDef = workflowDefVersion.findBucketDefByName(bucketName);
            if (bucketDef != null) {
                return bucketDef;
            }
        }
        return null;
    }

}

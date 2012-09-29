package org.broadinstitute.gpinformatics.mercury.entity.workflow;

import junit.framework.Assert;
import org.broadinstitute.gpinformatics.mercury.entity.labevent.LabEventType;
import org.testng.annotations.Test;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBElement;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Marshaller;
import javax.xml.bind.Unmarshaller;
import javax.xml.namespace.QName;
import java.util.ArrayList;

/**
 * Test product workflow, processes and steps
 */
public class WorkflowTest {

    @Test
    public void testMessageValidation() {
        // Quote
        // Send kits
        // BSP portal -> Athena Product Order
        // Athena Product Order -> Mercury
        // Receive samples in BSP
        // BSP samples -> Mercury
        // Web service to advance workflow without message
        // Associate starters with workflow
        // Validate first message
        // Perform first message
        // Validate second message
        buildProcesses();
    }

    public void buildProcesses() {
        ArrayList<WorkflowStepDef> workflowStepDefs = new ArrayList<WorkflowStepDef>();
        WorkflowProcessDef sampleReceipt = new WorkflowProcessDef("Sample Receipt");
        new WorkflowProcessDef("Extraction");
        new WorkflowProcessDef("Finger Printing");
        new WorkflowProcessDef("Samples Pico / Plating");
        WorkflowProcessDef preLc = new WorkflowProcessDef("Pre-Library Construction");
        preLc.addStep(new WorkflowBucketDef("Pre-LC Bucket"));

        WorkflowBucketDef workflowBucketDef = new WorkflowBucketDef("Preflight Bucket");
        workflowBucketDef.setEntryMaterialType(WorkflowBucketDef.MaterialType.GENOMIC_DNA);

        WorkflowProcessDef libraryConstruction = new WorkflowProcessDef("Library Construction");
        libraryConstruction.addStep(workflowBucketDef);
        libraryConstruction.addStep(new WorkflowStepDef("EndRepair").addLabEvent(LabEventType.END_REPAIR));
        libraryConstruction.addStep(new WorkflowStepDef("EndRepairCleanup").addLabEvent(LabEventType.END_REPAIR_CLEANUP));
        libraryConstruction.addStep(new WorkflowStepDef("ABase").addLabEvent(LabEventType.A_BASE));
        libraryConstruction.addStep(new WorkflowStepDef("ABaseCleanup").addLabEvent(LabEventType.A_BASE_CLEANUP));

        new WorkflowProcessDef("Post-Library Construction");
        new WorkflowProcessDef("QTP");
        new WorkflowProcessDef("HiSeq");

        WorkflowConfig workflowConfig = new WorkflowConfig();
        ProductWorkflowDef exomeExpressWorkflowDef = new ProductWorkflowDef("Exome Express");
        exomeExpressWorkflowDef.addWorkflowProcessDef(libraryConstruction);

        workflowConfig.addProductWorkflowDef(exomeExpressWorkflowDef);
        workflowConfig.addWorkflowProcessDef(libraryConstruction);

        try {
            // Have to explicitly include WorkflowStepDef subclasses, otherwise JAXB doesn't find them
            JAXBContext jc = JAXBContext.newInstance(WorkflowConfig.class, WorkflowBucketDef.class);

            Marshaller marshaller = jc.createMarshaller();
            marshaller.setProperty(Marshaller.JAXB_FORMATTED_OUTPUT, true);
            JAXBElement<WorkflowConfig> jaxbElement = new JAXBElement<WorkflowConfig>(new QName("workflowConfig"),
                    WorkflowConfig.class, workflowConfig);
            marshaller.marshal(jaxbElement, System.out);
        } catch (JAXBException e) {
            throw new RuntimeException(e);
        }

        parseWorkflow();
    }

    public void parseWorkflow() {
        try {
            JAXBContext jc = JAXBContext.newInstance(WorkflowConfig.class, WorkflowBucketDef.class);
            Unmarshaller unmarshaller = jc.createUnmarshaller();
            WorkflowConfig workflowConfig = (WorkflowConfig) unmarshaller.unmarshal(
                    Thread.currentThread().getContextClassLoader().getResourceAsStream("WorkflowConfig.xml"));
            Assert.assertFalse("No workflow defs", workflowConfig.getProductWorkflowDefs().isEmpty());
        } catch (JAXBException e) {
            throw new RuntimeException(e);
        }
    }
}

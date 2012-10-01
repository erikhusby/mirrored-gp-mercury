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
import java.util.Date;

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
        WorkflowProcessDef preLcProcess = new WorkflowProcessDef("Pre-Library Construction");
        WorkflowProcessDefVersion preLcProcessVersion = new WorkflowProcessDefVersion("1.0", new Date());
        preLcProcess.addWorkflowProcessDefVersion(preLcProcessVersion);
        preLcProcessVersion.addStep(new WorkflowBucketDef("Pre-LC Bucket"));

        WorkflowBucketDef workflowBucketDef = new WorkflowBucketDef("Preflight Bucket");
        workflowBucketDef.setEntryMaterialType(WorkflowBucketDef.MaterialType.GENOMIC_DNA);

        WorkflowProcessDef libraryConstructionProcess = new WorkflowProcessDef("Library Construction");
        WorkflowProcessDefVersion libraryConstructionProcessVersion = new WorkflowProcessDefVersion("1.0", new Date());
        libraryConstructionProcess.addWorkflowProcessDefVersion(libraryConstructionProcessVersion);
        libraryConstructionProcessVersion.addStep(workflowBucketDef);
        libraryConstructionProcessVersion.addStep(new WorkflowStepDef("EndRepair").addLabEvent(LabEventType.END_REPAIR));
        libraryConstructionProcessVersion.addStep(new WorkflowStepDef("EndRepairCleanup").addLabEvent(LabEventType.END_REPAIR_CLEANUP));
        libraryConstructionProcessVersion.addStep(new WorkflowStepDef("ABase").addLabEvent(LabEventType.A_BASE));
        libraryConstructionProcessVersion.addStep(new WorkflowStepDef("ABaseCleanup").addLabEvent(LabEventType.A_BASE_CLEANUP));

        WorkflowProcessDef hybridSelectionProcess = new WorkflowProcessDef("Hybrid Selection");
        WorkflowProcessDefVersion hybridSelectionProcessVersion = new WorkflowProcessDefVersion("1.0", new Date());
        hybridSelectionProcess.addWorkflowProcessDefVersion(hybridSelectionProcessVersion);
        WorkflowStepDef capture = new WorkflowStepDef("Capture");
        capture.addLabEvent(LabEventType.AP_WASH);
        capture.addLabEvent(LabEventType.GS_WASH_1);
        hybridSelectionProcessVersion.addStep(capture);

        new WorkflowProcessDef("QTP");
        new WorkflowProcessDef("HiSeq");

        WorkflowConfig workflowConfig = new WorkflowConfig();
        ProductWorkflowDef exomeExpressProduct = new ProductWorkflowDef("Exome Express");
        ProductWorkflowDefVersion exomeExpressProductVersion = new ProductWorkflowDefVersion("1.0", new Date());
        exomeExpressProduct.addProductWorkflowDefVersion(exomeExpressProductVersion);
        exomeExpressProductVersion.addWorkflowProcessDef(libraryConstructionProcess);
        exomeExpressProductVersion.addWorkflowProcessDef(hybridSelectionProcess);

        workflowConfig.addProductWorkflowDef(exomeExpressProduct);
        workflowConfig.addWorkflowProcessDef(libraryConstructionProcess);
        workflowConfig.addWorkflowProcessDef(hybridSelectionProcess);

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

package org.broadinstitute.gpinformatics.mercury.entity.workflow;

import org.broadinstitute.gpinformatics.infrastructure.bsp.BSPSampleDTO;
import org.broadinstitute.gpinformatics.infrastructure.bsp.BSPSampleSearchColumn;
import org.broadinstitute.gpinformatics.infrastructure.test.TestGroups;
import org.broadinstitute.gpinformatics.mercury.control.workflow.WorkflowLoader;
import org.broadinstitute.gpinformatics.mercury.entity.labevent.LabEventType;
import org.broadinstitute.gpinformatics.mercury.entity.sample.MercurySample;
import org.broadinstitute.gpinformatics.mercury.entity.vessel.TwoDBarcodedTube;
import org.testng.Assert;
import org.testng.annotations.Test;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBElement;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Marshaller;
import javax.xml.namespace.QName;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

/**
 * Test product workflow, processes and steps
 */
@Test(groups = TestGroups.DATABASE_FREE)
public class WorkflowTest {

    private WorkflowConfig workflowConfig;
    private ProductWorkflowDef exomeExpressProduct;
    private ProductWorkflowDefVersion exomeExpressProductVersion;
    private WorkflowProcessDef libraryConstructionProcess;
    private WorkflowProcessDefVersion libraryConstructionProcessVersion;
    private String exomeExpressProductName;
    private WorkflowProcessDef preLcProcess;
    private WorkflowProcessDefVersion preLcProcessVersion;
    private WorkflowProcessDef picoProcess;
    private WorkflowProcessDefVersion picoProcessVersion;

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


        Assert.assertEquals(exomeExpressProductName, exomeExpressProduct.getName());

        Assert.assertNotNull(exomeExpressProduct);
        Assert.assertNotNull(exomeExpressProduct.getByVersion(exomeExpressProductVersion.getVersion()));

        Assert.assertEquals(exomeExpressProductVersion, exomeExpressProduct
                .getByVersion(exomeExpressProductVersion.getVersion())
        );

        Assert.assertNotNull(exomeExpressProductVersion.getProcessDefsByName(
                libraryConstructionProcess.getName()));
        Assert.assertEquals(libraryConstructionProcess, exomeExpressProductVersion
                .getProcessDefsByName(libraryConstructionProcess.getName())
        );

        Assert.assertNotNull(exomeExpressProductVersion.findStepByEventType(
                LabEventType.END_REPAIR_CLEANUP.getName()));

        Assert.assertNotNull(exomeExpressProductVersion.getPreviousStep(
                LabEventType.END_REPAIR_CLEANUP.getName()));
        Assert.assertEquals(LabEventType.END_REPAIR.getName(), exomeExpressProductVersion
                .getPreviousStep(LabEventType.END_REPAIR_CLEANUP.getName())
                .getName());

        Assert.assertEquals(1, preLcProcessVersion.getBuckets().size());

        Assert.assertTrue(exomeExpressProductVersion.isPreviousStepBucket(LabEventType.PLATING_TO_SHEARING_TUBES
                .getName()));
        Assert.assertFalse(exomeExpressProductVersion
                .isNextStepBucket(LabEventType.PLATING_TO_SHEARING_TUBES.getName()));

        Assert.assertFalse(exomeExpressProductVersion
                .isPreviousStepBucket(LabEventType.PICO_PLATING_POST_NORM_PICO.getName()));
        Assert.assertTrue(exomeExpressProductVersion
                .isNextStepBucket(LabEventType.PICO_PLATING_POST_NORM_PICO.getName()));

    }

    public void buildProcesses() {
        ArrayList<WorkflowStepDef> workflowStepDefs = new ArrayList<WorkflowStepDef>();
        WorkflowProcessDef sampleReceipt = new WorkflowProcessDef("Sample Receipt");
        new WorkflowProcessDef("Extraction");
        new WorkflowProcessDef("Finger Printing");
        picoProcess = new WorkflowProcessDef("Samples Pico / Plating");
        picoProcessVersion = new WorkflowProcessDefVersion("1.0", new Date());
        picoProcess.addWorkflowProcessDefVersion(picoProcessVersion);

        WorkflowBucketDef picoBucket = new WorkflowBucketDef(LabEventType.PICO_PLATING_BUCKET.getName());
        picoBucket.addLabEvent(LabEventType.PICO_PLATING_BUCKET);

        picoProcessVersion.addStep(picoBucket);
        picoProcessVersion.addStep(new WorkflowStepDef(LabEventType.PICO_PLATING_QC.getName())
                .addLabEvent(LabEventType.PICO_PLATING_QC));

        picoProcessVersion.addStep(new WorkflowStepDef("Pico / Plating Setup")
                .addLabEvent(LabEventType.PICO_DILUTION_TRANSFER).addLabEvent(LabEventType.PICO_BUFFER_ADDITION)
                .addLabEvent(LabEventType.PICO_MICROFLUOR_TRANSFER).addLabEvent(LabEventType.PICO_STANDARDS_TRANSFER));

        picoProcessVersion.addStep(new WorkflowStepDef(LabEventType.SAMPLES_NORMALIZATION_TRANSFER.getName())
                .addLabEvent(LabEventType.SAMPLES_NORMALIZATION_TRANSFER));
        picoProcessVersion.addStep(new WorkflowStepDef(LabEventType.PICO_PLATING_POST_NORM_PICO.getName())
                .addLabEvent(LabEventType.PICO_PLATING_POST_NORM_PICO));

        preLcProcess = new WorkflowProcessDef("Pre-Library Construction");
        preLcProcessVersion = new WorkflowProcessDefVersion("1.0", new Date());
        preLcProcess.addWorkflowProcessDefVersion(preLcProcessVersion);
//        preLcProcessVersion.addStep ( new WorkflowBucketDef ( "Pre-LC Bucket" ) );

        WorkflowBucketDef shearingBucketDef = new WorkflowBucketDef(LabEventType.SHEARING_BUCKET.getName());
        shearingBucketDef.addLabEvent(LabEventType.SHEARING_BUCKET);

        preLcProcessVersion.addStep(shearingBucketDef);
        preLcProcessVersion.addStep(new WorkflowStepDef(LabEventType.PLATING_TO_SHEARING_TUBES.getName()).addLabEvent(
                LabEventType.PLATING_TO_SHEARING_TUBES));
        preLcProcessVersion.addStep(new WorkflowStepDef(LabEventType.COVARIS_LOADED.getName()).addLabEvent(
                LabEventType.COVARIS_LOADED));
        preLcProcessVersion
                .addStep(new WorkflowStepDef(LabEventType.POST_SHEARING_TRANSFER_CLEANUP.getName()).addLabEvent(
                        LabEventType.POST_SHEARING_TRANSFER_CLEANUP));
        preLcProcessVersion.addStep(new WorkflowStepDef(LabEventType.SHEARING_QC.getName()).addLabEvent(
                LabEventType.SHEARING_QC));

        libraryConstructionProcess = new WorkflowProcessDef("Library Construction");
        libraryConstructionProcessVersion = new WorkflowProcessDefVersion("1.0", new Date());
        libraryConstructionProcess.addWorkflowProcessDefVersion(libraryConstructionProcessVersion);
        libraryConstructionProcessVersion.addStep(new WorkflowStepDef("EndRepair").addLabEvent(
                LabEventType.END_REPAIR));
        libraryConstructionProcessVersion.addStep(new WorkflowStepDef("EndRepairCleanup").addLabEvent(
                LabEventType.END_REPAIR_CLEANUP));
        libraryConstructionProcessVersion.addStep(new WorkflowStepDef("ABase").addLabEvent(
                LabEventType.A_BASE));
        libraryConstructionProcessVersion.addStep(new WorkflowStepDef("ABaseCleanup").addLabEvent(
                LabEventType.A_BASE_CLEANUP));

        WorkflowProcessDef hybridSelectionProcess = new WorkflowProcessDef(WorkflowName.HYBRID_SELECTION.getWorkflowName());
        WorkflowProcessDefVersion hybridSelectionProcessVersion = new WorkflowProcessDefVersion("1.0", new Date());
        hybridSelectionProcess.addWorkflowProcessDefVersion(hybridSelectionProcessVersion);
        WorkflowStepDef capture = new WorkflowStepDef("Capture");
        capture.addLabEvent(LabEventType.AP_WASH);
        capture.addLabEvent(LabEventType.GS_WASH_1);
        hybridSelectionProcessVersion.addStep(capture);

        new WorkflowProcessDef("QTP");
        new WorkflowProcessDef("HiSeq");

        workflowConfig = new WorkflowConfig();
        exomeExpressProductName = WorkflowName.EXOME_EXPRESS.getWorkflowName();
        exomeExpressProduct = new ProductWorkflowDef(exomeExpressProductName);
        exomeExpressProductVersion = new ProductWorkflowDefVersion("1.0", new Date());
        exomeExpressProduct.addProductWorkflowDefVersion(exomeExpressProductVersion);
        exomeExpressProductVersion.addWorkflowProcessDef(picoProcess);
        exomeExpressProductVersion.addWorkflowProcessDef(preLcProcess);
        exomeExpressProductVersion.addWorkflowProcessDef(libraryConstructionProcess);
        exomeExpressProductVersion.addWorkflowProcessDef(hybridSelectionProcess);

        workflowConfig.addProductWorkflowDef(exomeExpressProduct);
        workflowConfig.addWorkflowProcessDef(picoProcess);
        workflowConfig.addWorkflowProcessDef(preLcProcess);
        workflowConfig.addWorkflowProcessDef(libraryConstructionProcess);
        workflowConfig.addWorkflowProcessDef(hybridSelectionProcess);

        try {
            // Have to explicitly include WorkflowStepDef subclasses, otherwise JAXB doesn't find them
            JAXBContext jc = JAXBContext.newInstance(WorkflowConfig.class, WorkflowBucketDef.class);

            Marshaller marshaller = jc.createMarshaller();
            marshaller.setProperty(Marshaller.JAXB_FORMATTED_OUTPUT, true);
            JAXBElement<WorkflowConfig> jaxbElement = new JAXBElement<WorkflowConfig>(new QName("workflowConfig"),
                    WorkflowConfig.class,
                    workflowConfig);
            marshaller.marshal(jaxbElement, System.out);
        } catch (JAXBException e) {
            throw new RuntimeException(e);
        }
    }
    
    @Test
    public void testEntryExpression() {

        Map<BSPSampleSearchColumn, String> dataMap = new HashMap<BSPSampleSearchColumn, String>(){{
            put(BSPSampleSearchColumn.PRIMARY_DISEASE, "Cancer");
            put(BSPSampleSearchColumn.LSID, "org.broad:SM-1234");
            put(BSPSampleSearchColumn.MATERIAL_TYPE, new String("DNA:DNA Genomic"));  //need to avoid interning string
            put(BSPSampleSearchColumn.COLLABORATOR_SAMPLE_ID, "4321");
            put(BSPSampleSearchColumn.SPECIES, "Homo Sapiens");
            put(BSPSampleSearchColumn.PARTICIPANT_ID, "PT-1234");
        }};

        TwoDBarcodedTube twoDBarcodedTube = new TwoDBarcodedTube("00001234");
        twoDBarcodedTube.addSample(new MercurySample("SM-1234", new BSPSampleDTO(dataMap)));

        WorkflowLoader workflowLoader = new WorkflowLoader();
        WorkflowConfig workflowConfig1 = workflowLoader.load();
        ProductWorkflowDef exomeExpressWorkflow = workflowConfig1.getWorkflowByName("Exome Express");
        boolean meetsCriteria = false;
        for (WorkflowBucketDef workflowBucketDef : exomeExpressWorkflow.getEffectiveVersion().getBuckets()) {
            if (workflowBucketDef.getName().equals("Pico/Plating Bucket")) {
                meetsCriteria = workflowBucketDef.meetsBucketCriteria(twoDBarcodedTube);
            }
        }
        Assert.assertTrue(meetsCriteria, "Meets criteria is not true");
    }

    @Test
    public void testBucketEntryFail() {

        Map<BSPSampleSearchColumn, String> dataMap = new HashMap<BSPSampleSearchColumn, String>(){{
            put(BSPSampleSearchColumn.PRIMARY_DISEASE, "Cancer");
            put(BSPSampleSearchColumn.LSID, "org.broad:SM-2345");
            put(BSPSampleSearchColumn.MATERIAL_TYPE, new String("DNA:DNA WGA Cleaned"));  //need to avoid interning string
            put(BSPSampleSearchColumn.COLLABORATOR_SAMPLE_ID, "5432");
            put(BSPSampleSearchColumn.SPECIES, "Homo Sapiens");
            put(BSPSampleSearchColumn.PARTICIPANT_ID, "PT-2345");
        }};

        TwoDBarcodedTube twoDBarcodedTube = new TwoDBarcodedTube("00002345");
        twoDBarcodedTube.addSample(new MercurySample("SM-2345", new BSPSampleDTO(dataMap)));

        WorkflowLoader workflowLoader = new WorkflowLoader();
        WorkflowConfig workflowConfig1 = workflowLoader.load();
        ProductWorkflowDef exomeExpressWorkflow = workflowConfig1.getWorkflowByName("Exome Express");
        boolean meetsCriteria = true;
        for (WorkflowBucketDef workflowBucketDef : exomeExpressWorkflow.getEffectiveVersion().getBuckets()) {
            if (workflowBucketDef.getName().equals("Pico/Plating Bucket")) {
                meetsCriteria = workflowBucketDef.meetsBucketCriteria(twoDBarcodedTube);
            }
        }
        Assert.assertFalse(meetsCriteria, "Bucket criteria should have failed.");
    }
}

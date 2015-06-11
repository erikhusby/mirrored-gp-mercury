package org.broadinstitute.gpinformatics.mercury.entity.workflow;

import com.google.common.collect.ImmutableSet;
import org.broadinstitute.gpinformatics.infrastructure.SampleDataTestFactory;
import org.broadinstitute.gpinformatics.infrastructure.bsp.BSPSampleSearchColumn;
import org.broadinstitute.gpinformatics.infrastructure.bsp.BSPSetVolumeConcentration;
import org.broadinstitute.gpinformatics.infrastructure.bsp.BSPSetVolumeConcentrationProducer;
import org.broadinstitute.gpinformatics.infrastructure.bsp.BSPUserList;
import org.broadinstitute.gpinformatics.infrastructure.bsp.BspSampleData;
import org.broadinstitute.gpinformatics.infrastructure.bsp.plating.BSPManagerFactoryProducer;
import org.broadinstitute.gpinformatics.infrastructure.test.TestGroups;
import org.broadinstitute.gpinformatics.mercury.control.labevent.LabEventFactory;
import org.broadinstitute.gpinformatics.mercury.control.vessel.LabVesselFactory;
import org.broadinstitute.gpinformatics.mercury.control.workflow.WorkflowLoader;
import org.broadinstitute.gpinformatics.mercury.entity.Metadata;
import org.broadinstitute.gpinformatics.mercury.entity.labevent.LabEventType;
import org.broadinstitute.gpinformatics.mercury.entity.sample.MercurySample;
import org.broadinstitute.gpinformatics.mercury.entity.vessel.BarcodedTube;
import org.broadinstitute.gpinformatics.mercury.entity.vessel.LabVessel;
import org.broadinstitute.gpinformatics.mercury.entity.vessel.LabVesselTest;
import org.testng.Assert;
import org.testng.annotations.BeforeTest;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBElement;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Marshaller;
import javax.xml.namespace.QName;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.hamcrest.CoreMatchers.hasItem;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;


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
    private LabEventFactory labEventFactory;
    private LabVesselFactory labVesselFactory;

    @BeforeTest
    public void setupWorkflow() {
        workflowConfig = new WorkflowLoader().load();
        BSPUserList testUserList = new BSPUserList(BSPManagerFactoryProducer.stubInstance());
        BSPSetVolumeConcentration bspSetVolumeConcentration = BSPSetVolumeConcentrationProducer.stubInstance();
        labEventFactory = new LabEventFactory(testUserList, bspSetVolumeConcentration);

        labVesselFactory = new LabVesselFactory();
        labVesselFactory.setBspUserList(testUserList);

    }

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

        Assert.assertTrue(exomeExpressProductVersion.isPreviousStepBucket(LabEventType.COVARIS_LOADED
                .getName()));
        Assert.assertFalse(exomeExpressProductVersion
                .isNextStepBucket(LabEventType.COVARIS_LOADED.getName()));

        Assert.assertFalse(exomeExpressProductVersion
                .isPreviousStepBucket(LabEventType.PICO_PLATING_POST_NORM_PICO.getName()));
        Assert.assertTrue(exomeExpressProductVersion
                .isNextStepBucket(LabEventType.PICO_PLATING_POST_NORM_PICO.getName()));

    }

    public void buildProcesses() {
        ArrayList<WorkflowStepDef> workflowStepDefs = new ArrayList<>();
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

        WorkflowProcessDef hybridSelectionProcess =
                new WorkflowProcessDef(Workflow.HYBRID_SELECTION.getWorkflowName());
        WorkflowProcessDefVersion hybridSelectionProcessVersion = new WorkflowProcessDefVersion("1.0", new Date());
        hybridSelectionProcess.addWorkflowProcessDefVersion(hybridSelectionProcessVersion);
        WorkflowStepDef capture = new WorkflowStepDef("Capture");
        capture.addLabEvent(LabEventType.AP_WASH);
        capture.addLabEvent(LabEventType.GS_WASH_1);
        hybridSelectionProcessVersion.addStep(capture);

        new WorkflowProcessDef("QTP");
        new WorkflowProcessDef("HiSeq");

        exomeExpressProductName = Workflow.AGILENT_EXOME_EXPRESS.getWorkflowName();
        exomeExpressProduct = new ProductWorkflowDef(exomeExpressProductName);
        exomeExpressProductVersion = new ProductWorkflowDefVersion("1.0", new Date());
        exomeExpressProduct.addProductWorkflowDefVersion(exomeExpressProductVersion);
        exomeExpressProductVersion.addWorkflowProcessDef(picoProcess);
        exomeExpressProductVersion.addWorkflowProcessDef(preLcProcess);
        exomeExpressProductVersion.addWorkflowProcessDef(libraryConstructionProcess);
        exomeExpressProductVersion.addWorkflowProcessDef(hybridSelectionProcess);

        workflowConfig = new WorkflowConfig(
                Arrays.asList(picoProcess, preLcProcess, libraryConstructionProcess, hybridSelectionProcess),
                Collections.singletonList(exomeExpressProduct));

        try {
            // Have to explicitly include WorkflowStepDef subclasses, otherwise JAXB doesn't find them
            JAXBContext jc = JAXBContext.newInstance(WorkflowConfig.class, WorkflowBucketDef.class);

            Marshaller marshaller = jc.createMarshaller();
            marshaller.setProperty(Marshaller.JAXB_FORMATTED_OUTPUT, true);
            JAXBElement<WorkflowConfig> jaxbElement = new JAXBElement<>(new QName("workflowConfig"),
                    WorkflowConfig.class,
                    workflowConfig);
            marshaller.marshal(jaxbElement, System.out);
        } catch (JAXBException e) {
            throw new RuntimeException(e);
        }
    }

    @Test
    public void testEntryExpression() {
        BarcodedTube barcodedTube = new BarcodedTube("00001234");
        MercurySample mercurySample = createNewMercurySample("SM-1234", "DNA:DNA Genomic",
                MercurySample.MetadataSource.MERCURY);
        barcodedTube.addSample(mercurySample);

        ProductWorkflowDef exomeExpressWorkflow = workflowConfig.getWorkflow(Workflow.AGILENT_EXOME_EXPRESS);
        for (WorkflowBucketDef workflowBucketDef : exomeExpressWorkflow.getEffectiveVersion().getBuckets()) {
            if (workflowBucketDef.getName().equals("Pico/Plating Bucket")) {
                assertThat(workflowBucketDef.meetsBucketCriteria(barcodedTube), is(true));
            }
        }
    }

    private MercurySample createNewMercurySample(final String sampleId, final String materialType,
                                                 MercurySample.MetadataSource metadataSource) {
        MercurySample mercurySample=null;
        switch (metadataSource) {
        case MERCURY:
            Set<Metadata> data = ImmutableSet.of(
                    new Metadata(Metadata.Key.SAMPLE_ID, sampleId),
                    new Metadata(Metadata.Key.PATIENT_ID, "1234"),
                    new Metadata(Metadata.Key.MATERIAL_TYPE, materialType)
            );
            mercurySample = new MercurySample(sampleId, data);
            break;
        case BSP:
            Map<BSPSampleSearchColumn, String> dataMap = new HashMap<BSPSampleSearchColumn, String>() {{
                put(BSPSampleSearchColumn.PRIMARY_DISEASE, "Cancer");
                put(BSPSampleSearchColumn.LSID, String.format("org.broad:%s", sampleId));
                put(BSPSampleSearchColumn.MATERIAL_TYPE, new String(materialType));  //need to avoid interning string
                put(BSPSampleSearchColumn.COLLABORATOR_SAMPLE_ID, "4321");
                put(BSPSampleSearchColumn.SPECIES, "Homo Sapiens");
                put(BSPSampleSearchColumn.PARTICIPANT_ID, "PT-1234");
            }};

            mercurySample = new MercurySample(sampleId, new BspSampleData(dataMap));
            break;
        }
        return mercurySample;
    }

    @Test
    public void testNoEntryExpressionsThenVesselMeetsCriteria() {
        BarcodedTube barcodedTube = new BarcodedTube("00001234");
        MercurySample mercurySample = createNewMercurySample("SM-1234", "DNA:DNA Genomic", MercurySample.MetadataSource.MERCURY);
        barcodedTube.addSample(mercurySample);

        WorkflowBucketDef workflowBucketDef = new WorkflowBucketDef();
        boolean meetsCriteria = workflowBucketDef.meetsBucketCriteria(barcodedTube);
        assertThat(meetsCriteria, is(true));
        Assert.assertTrue(meetsCriteria, "Meets criteria is not true");
    }

    @Test
    public void testBucketEntryFail() {
        BarcodedTube barcodedTube = new BarcodedTube("00002345");
        barcodedTube.addSample(
                new MercurySample("SM-2345", Collections.singleton(new Metadata(Metadata.Key.MATERIAL_TYPE, ""))));

        ProductWorkflowDef exomeExpressWorkflow = workflowConfig.getWorkflow(Workflow.AGILENT_EXOME_EXPRESS);

        for (WorkflowBucketDef workflowBucketDef : exomeExpressWorkflow.getEffectiveVersion().getBuckets()) {
            if (workflowBucketDef.getName().equals("Pico/Plating Bucket")) {
                assertThat(workflowBucketDef.getBucketEntryEvaluators(),
                        hasItem(DnaBucketEntryEvaluator.class.getName()));
                assertThat(workflowBucketDef.meetsBucketCriteria(barcodedTube), is(false));
            }
        }
    }

    @Test
    public void testBucketEntrySucceed() {
        BarcodedTube barcodedTube = new BarcodedTube("00002345");
        barcodedTube.addSample(
                new MercurySample("SM-2345", Collections.singleton(new Metadata(Metadata.Key.MATERIAL_TYPE, "DNA"))));

        ProductWorkflowDef exomeExpressWorkflow = workflowConfig.getWorkflow(Workflow.AGILENT_EXOME_EXPRESS);

        for (WorkflowBucketDef workflowBucketDef : exomeExpressWorkflow.getEffectiveVersion().getBuckets()) {
            if (workflowBucketDef.getName().equals("Pico/Plating Bucket")) {
                assertThat(workflowBucketDef.getBucketEntryEvaluators(),
                        hasItem(DnaBucketEntryEvaluator.class.getName()));
                assertThat(workflowBucketDef.meetsBucketCriteria(barcodedTube), is(true));
            }
        }
    }

    @Test
    public void testSupportedWorkflows() {
        Assert.assertTrue(Workflow.AGILENT_EXOME_EXPRESS.isWorkflowSupportedByMercury(),
                "Uh oh, mercury doesn't support exome express!");
        Assert.assertFalse(Workflow.WHOLE_GENOME.isWorkflowSupportedByMercury(),
                "Do we support WGS in mercury? Party time!");
    }

    @DataProvider(name = "bucketEntryEvaluators")
    public static Object[][] bucketEntryEvaluators() {
        String trueEvaluatorName = AlwaysTrueEvaluator.class.getCanonicalName();
        String falseEvaluatorName = AlwaysFalseEvaluator.class.getCanonicalName();

         return new Object[][]{
                 {true,  Arrays.asList(trueEvaluatorName, trueEvaluatorName)},
                 {false, Arrays.asList(trueEvaluatorName, falseEvaluatorName)},
                 {false, Arrays.asList(falseEvaluatorName, falseEvaluatorName)},
                 {false, Arrays.asList(falseEvaluatorName, trueEvaluatorName)},
         };

    }

     @Test(dataProvider = "bucketEntryEvaluators")
     public void testBucketEntryEvaluatorAndingWorks(boolean expectedResult, List<String> classNames) {
         BarcodedTube barcodedTube = new BarcodedTube("00002345");
         barcodedTube.addSample(
                 new MercurySample("SM-2345", Collections.singleton(new Metadata(Metadata.Key.MATERIAL_TYPE, ""))));

        WorkflowBucketDef workflowBucketDef = new WorkflowBucketDef();
        workflowBucketDef.setBucketEntryEvaluators(classNames);
        assertThat(workflowBucketDef.meetsBucketCriteria(barcodedTube), is(expectedResult));
    }

    @DataProvider(name = "bucketScenariosDataProvider")
    public Iterator<Object[]> bucketScenariosDataProvider() {
        Set<Object[]> result= new HashSet<>();

        EnumSet<LabEventType> dnaLabEventTypes = EnumSet.copyOf(
                LabEventType.getLabEventTypesForMaterialType(LabVessel.MaterialType.DNA));
        EnumSet<LabEventType> nonDnaLabEventTypes = EnumSet.complementOf(dnaLabEventTypes);

        // the value of metadataSource should not affect the test result
        for (MercurySample.MetadataSource metadataSource : MercurySample.MetadataSource.values()) {
            for (LabEventType labEventType : dnaLabEventTypes) {
                // All these lab events should allow the sample into the pico/plating bucket, regardless of MaterialType
                result.add(new Object[]{labEventType, LabVessel.MaterialType.DNA, metadataSource, true, true});
                // Blood should pass here because there has been an extraction.
                result.add(new Object[]{labEventType, LabVessel.MaterialType.FFPE, metadataSource, true, true});
            }

            for (LabEventType labEventType : nonDnaLabEventTypes) {
                // DNA should always pass because DNA is a valid materialType for the pico/plating bucket
                result.add(new Object[]{labEventType, LabVessel.MaterialType.DNA, metadataSource, false, true});
                // BLOOD will never pass because DNA is a valid materialType for the pico/plating bucket
                result.add(new Object[]{labEventType, LabVessel.MaterialType.FFPE, metadataSource, false, false});
            }
        }
        return result.iterator();
    }

    @Test(dataProvider = "bucketScenariosDataProvider")
    public void testBucketEntryForManyScenarios(LabEventType labEventType, LabVessel.MaterialType sampleMaterialType,
                                                MercurySample.MetadataSource metadataSource,
                                                boolean doTransfer,
                                                boolean meetsBucketCriteriaExpected) {
        LabVessel labVessel = createLabVesselWithSample(labEventType, sampleMaterialType, metadataSource, doTransfer);

        WorkflowBucketDef workflowBucketDef = new WorkflowBucketDef();
        workflowBucketDef.setBucketEntryEvaluators(Collections.singletonList(DnaBucketEntryEvaluator.class.getCanonicalName()));

        boolean actualBucketCriteria = workflowBucketDef.meetsBucketCriteria(labVessel);
        assertThat(actualBucketCriteria, is(meetsBucketCriteriaExpected));
    }

    private LabVessel createLabVesselWithSample(LabEventType labEventType,
                                                LabVessel.MaterialType sampleMaterialType,
                                                MercurySample.MetadataSource metadataSource,
                                                boolean doTransfer) {
        BarcodedTube sourceVessel = new BarcodedTube("A_SOURCE_VESSEL", BarcodedTube.BarcodedTubeType.MatrixTube075);
        MercurySample mercurySample = SampleDataTestFactory.getTestMercurySample(sampleMaterialType, metadataSource);
        mercurySample.addLabVessel(sourceVessel);

        if (doTransfer) {
            BarcodedTube destinationVessel =
                    new BarcodedTube("A_DESTINATION_VESSEL", BarcodedTube.BarcodedTubeType.MatrixTube075);
            LabVesselTest
                    .doVesselToVesselTransfer(sourceVessel, destinationVessel, sampleMaterialType, labEventType,
                            metadataSource, labEventFactory);
            return destinationVessel;
        } else {
            return sourceVessel;
        }
    }
}


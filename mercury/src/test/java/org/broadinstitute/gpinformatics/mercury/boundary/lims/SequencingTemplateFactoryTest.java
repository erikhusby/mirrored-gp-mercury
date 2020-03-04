package org.broadinstitute.gpinformatics.mercury.boundary.lims;

import com.google.common.collect.Lists;
import org.broadinstitute.gpinformatics.athena.entity.orders.ProductOrder;
import org.broadinstitute.gpinformatics.infrastructure.test.dbfree.LabEventTestFactory;
import org.broadinstitute.gpinformatics.infrastructure.test.dbfree.ProductOrderTestFactory;
import org.broadinstitute.gpinformatics.mercury.boundary.run.FlowcellDesignationEjb;
import org.broadinstitute.gpinformatics.mercury.control.workflow.WorkflowLoader;
import org.broadinstitute.gpinformatics.mercury.entity.labevent.LabEvent;
import org.broadinstitute.gpinformatics.mercury.entity.labevent.LabEventType;
import org.broadinstitute.gpinformatics.mercury.entity.labevent.VesselToSectionTransfer;
import org.broadinstitute.gpinformatics.mercury.entity.reagent.UMIReagent;
import org.broadinstitute.gpinformatics.mercury.entity.reagent.UniqueMolecularIdentifier;
import org.broadinstitute.gpinformatics.mercury.entity.run.FlowcellDesignation;
import org.broadinstitute.gpinformatics.mercury.entity.run.IlluminaFlowcell;
import org.broadinstitute.gpinformatics.mercury.entity.vessel.BarcodedTube;
import org.broadinstitute.gpinformatics.mercury.entity.vessel.LabVessel;
import org.broadinstitute.gpinformatics.mercury.entity.vessel.MiSeqReagentKit;
import org.broadinstitute.gpinformatics.mercury.entity.vessel.RackOfTubes;
import org.broadinstitute.gpinformatics.mercury.entity.vessel.SBSSection;
import org.broadinstitute.gpinformatics.mercury.entity.vessel.StaticPlate;
import org.broadinstitute.gpinformatics.mercury.entity.vessel.TubeFormation;
import org.broadinstitute.gpinformatics.mercury.entity.vessel.VesselAndPosition;
import org.broadinstitute.gpinformatics.mercury.entity.vessel.VesselPosition;
import org.broadinstitute.gpinformatics.mercury.entity.workflow.LabBatch;
import org.broadinstitute.gpinformatics.mercury.entity.workflow.LabBatchStartingVessel;
import org.broadinstitute.gpinformatics.mercury.entity.workflow.Workflow;
import org.broadinstitute.gpinformatics.mercury.limsquery.generated.SequencingTemplateLaneType;
import org.broadinstitute.gpinformatics.mercury.limsquery.generated.SequencingTemplateType;
import org.broadinstitute.gpinformatics.mercury.test.BaseEventTest;
import org.broadinstitute.gpinformatics.mercury.test.LabEventTest;
import org.broadinstitute.gpinformatics.mercury.test.builders.ExomeExpressShearingEntityBuilder;
import org.broadinstitute.gpinformatics.mercury.test.builders.HiSeq2500FlowcellEntityBuilder;
import org.broadinstitute.gpinformatics.mercury.test.builders.HiSeq4000FlowcellEntityBuilder;
import org.broadinstitute.gpinformatics.mercury.test.builders.HybridSelectionEntityBuilder;
import org.broadinstitute.gpinformatics.mercury.test.builders.LibraryConstructionEntityBuilder;
import org.broadinstitute.gpinformatics.mercury.test.builders.LibraryConstructionJaxbBuilder;
import org.broadinstitute.gpinformatics.mercury.test.builders.PicoPlatingEntityBuilder;
import org.broadinstitute.gpinformatics.mercury.test.builders.ProductionFlowcellPath;
import org.broadinstitute.gpinformatics.mercury.test.builders.QtpEntityBuilder;
import org.hamcrest.MatcherAssert;
import org.hamcrest.Matchers;
import org.hamcrest.core.AnyOf;
import org.testng.Assert;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.broadinstitute.gpinformatics.infrastructure.test.TestGroups.DATABASE_FREE;
import static org.broadinstitute.gpinformatics.mercury.entity.labevent.LabEventType.DENATURE_TO_REAGENT_KIT_TRANSFER;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.nullValue;
import static org.hamcrest.collection.IsEmptyCollection.empty;
import static org.hamcrest.core.IsNot.not;

/**
 * Database-free test for SequencingTemplateFactory.
 */
@Test(groups = DATABASE_FREE)
public class SequencingTemplateFactoryTest extends BaseEventTest {

    public static final String BARCODE_SUFFIX = "1";
    public static final String FLOWCELL_2500_TICKET = "FCT-3";
    public static final String FLOWCELL_2000_TICKET = "FCT-4";
    public static final String FLOWCELL_4000_TICKET = "FCT-5";
    private static final BigDecimal BIG_DECIMAL_12_33 = new BigDecimal("12.33");
    private static final BigDecimal BIG_DECIMAL_7_77 = new BigDecimal("7.77");
    private SequencingTemplateFactory factory = null;
    private StaticPlate shearingCleanupPlate = null;
    private BarcodedTube denatureTube2500 = null;
    private BarcodedTube denatureTube2000 = null;
    private BarcodedTube denatureTube4000 = null;
    private IlluminaFlowcell flowcellHiSeq2500 = null;
    private MiSeqReagentKit reagentKit = null;
    private static final String PRODUCTION_CIGAR = "76T8B8B76T";
    private static final String DESIGNATION_CIGAR = "99T8B8B99T";
    private static final String POOL_TEST_CIGAR = "8B8B";
    private SequencingTemplateType template;
    private Date runDate;
    private String flowcellHiSeq2500Barcode;
    private BarcodedTube dilutionTube2500;
    private BarcodedTube dilutionTube4000;
    private LabBatch fctBatch;
    private LabBatch miSeqBatch;
    private LabBatch fctBatchHiSeq2000;
    private LabBatch fctBatchHiSeq4000;
    private LabBatch.VesselToLanesInfo vesselToLanesInfo;
    private LabBatch.VesselToLanesInfo vesselToLanesInfo2;
    private IlluminaFlowcell flowcellHiSeq4000;
    private LabBatch workflowBatch;
    private final List<FlowcellDesignation> flowcellDesignations = new ArrayList<>();
    private ProductOrder productOrder;
    private ExomeExpressShearingEntityBuilder exomeExpressShearingEntityBuilder;
    private BarcodedTube singleIndexTube;

    @Override
    @BeforeMethod
    public void setUp() {
        expectedRouting = SystemOfRecord.System.MERCURY;

        super.setUp();
        factory = new SequencingTemplateFactory();
        factory.setWorkflowLoader(new WorkflowLoader());

        // Method calls on factory will always use our list of flowcell designations.
        factory.setFlowcellDesignationEjb(new FlowcellDesignationEjb(){
            @Override
            public List<FlowcellDesignation> getFlowcellDesignations(LabBatch fct) {
                return flowcellDesignations;
            }
        });
        flowcellDesignations.clear();

        productOrder = ProductOrderTestFactory.buildExExProductOrder(96);
        runDate = new Date();
        Map<String, BarcodedTube> mapBarcodeToTube = createInitialRack(productOrder, "R");
        workflowBatch = new LabBatch("Exome Express Batch",
                new HashSet<LabVessel>(mapBarcodeToTube.values()),
                LabBatch.LabBatchType.WORKFLOW);
        workflowBatch.setWorkflow(Workflow.AGILENT_EXOME_EXPRESS);
        workflowBatch.setCreatedOn(EX_EX_IN_MERCURY_CALENDAR.getTime());

        //Build Event History
        bucketBatchAndDrain(mapBarcodeToTube, productOrder, workflowBatch, BARCODE_SUFFIX);
        PicoPlatingEntityBuilder picoPlatingEntityBuilder = runPicoPlatingProcess(mapBarcodeToTube,
                String.valueOf(runDate.getTime()),
                BARCODE_SUFFIX, true);
        ExomeExpressShearingEntityBuilder exomeExpressShearingEntityBuilder =
                runExomeExpressShearingProcess(picoPlatingEntityBuilder.getNormBarcodeToTubeMap(),
                        picoPlatingEntityBuilder.getNormTubeFormation(),
                        picoPlatingEntityBuilder.getNormalizationBarcode(), BARCODE_SUFFIX);
        LibraryConstructionEntityBuilder libraryConstructionEntityBuilder =
                runLibraryConstructionProcess(exomeExpressShearingEntityBuilder.getShearingCleanupPlate(),
                        exomeExpressShearingEntityBuilder.getShearCleanPlateBarcode(),
                        exomeExpressShearingEntityBuilder.getShearingPlate(), BARCODE_SUFFIX);
        HybridSelectionEntityBuilder hybridSelectionEntityBuilder =
                runHybridSelectionProcess(libraryConstructionEntityBuilder.getPondRegRack(),
                        libraryConstructionEntityBuilder.getPondRegRackBarcode(),
                        libraryConstructionEntityBuilder.getPondRegTubeBarcodes(), BARCODE_SUFFIX);
        QtpEntityBuilder qtpEntityBuilder = runQtpProcess(hybridSelectionEntityBuilder.getNormCatchRack(),
                hybridSelectionEntityBuilder.getNormCatchBarcodes(),
                hybridSelectionEntityBuilder.getMapBarcodeToNormCatchTubes(),
                "1");
        denatureTube2500 = qtpEntityBuilder.getDenatureRack().getContainerRole().getVesselAtPosition(VesselPosition.A01);

        // Generate a Single Index Tube for Testing
        ExomeExpressShearingEntityBuilder exomeExpressShearingEntityBuilderSingleIndex =
                runExomeExpressShearingProcess(picoPlatingEntityBuilder.getNormBarcodeToTubeMap(),
                        picoPlatingEntityBuilder.getNormTubeFormation(),
                        picoPlatingEntityBuilder.getNormalizationBarcode(), "SINGLE");
        LibraryConstructionEntityBuilder libraryConstructionEntityBuilderSingleIndex = new LibraryConstructionEntityBuilder(
                getBettaLimsMessageTestFactory(), getLabEventFactory(), getLabEventHandler(),
                exomeExpressShearingEntityBuilderSingleIndex.getShearingCleanupPlate(),
                exomeExpressShearingEntityBuilderSingleIndex.getShearCleanPlateBarcode(),
                exomeExpressShearingEntityBuilderSingleIndex.getShearingPlate(), 96, "MixTest",
                LibraryConstructionEntityBuilder.Indexing.SINGLE,
                LibraryConstructionJaxbBuilder.PondType.REGULAR, LibraryConstructionEntityBuilder.Umi.NONE,
                "p7singleIndexTest", null).invoke();
        HybridSelectionEntityBuilder hybridSelectionEntityBuilderSingleIndex =
                runHybridSelectionProcess(libraryConstructionEntityBuilderSingleIndex.getPondRegRack(),
                        libraryConstructionEntityBuilderSingleIndex.getPondRegRackBarcode(),
                        libraryConstructionEntityBuilderSingleIndex.getPondRegTubeBarcodes(), BARCODE_SUFFIX);
        QtpEntityBuilder qtpEntityBuilderSingleIndex = runQtpProcess(hybridSelectionEntityBuilderSingleIndex.getNormCatchRack(),
                hybridSelectionEntityBuilderSingleIndex.getNormCatchBarcodes(),
                hybridSelectionEntityBuilderSingleIndex.getMapBarcodeToNormCatchTubes(),
                "1");
        singleIndexTube = qtpEntityBuilderSingleIndex.getDenatureRack().getContainerRole().
                getVesselAtPosition(VesselPosition.A01);

        reagentKit = new MiSeqReagentKit("reagent_kit_barcode");
        LabEvent denatureToReagentKitEvent = new LabEvent(DENATURE_TO_REAGENT_KIT_TRANSFER, new Date(),
                "ZLAB", 1L, 1L, "sequencingTemplateFactoryTest");
        final VesselToSectionTransfer sectionTransfer = new VesselToSectionTransfer(
                denatureTube2500, SBSSection.getBySectionName(MiSeqReagentKit.LOADING_WELL.name()),
                reagentKit.getContainerRole(), null, denatureToReagentKitEvent);
        denatureToReagentKitEvent.getVesselToSectionTransfers().add(sectionTransfer);


        //create a Miseq batch then one FCT (2500) batch
        miSeqBatch = new LabBatch("FCT-1", LabBatch.LabBatchType.MISEQ, IlluminaFlowcell.FlowcellType.MiSeqFlowcell,
                denatureTube2500, BigDecimal.valueOf(7f));

        fctBatch = new LabBatch("FCT-3", LabBatch.LabBatchType.FCT,
                IlluminaFlowcell.FlowcellType.HiSeq2500Flowcell, denatureTube2500, BIG_DECIMAL_12_33);

        HiSeq2500FlowcellEntityBuilder flowcellEntityBuilder =
                runHiSeq2500FlowcellProcess(qtpEntityBuilder.getDenatureRack(), BARCODE_SUFFIX + "ADXX",
                        FLOWCELL_2500_TICKET,
                        ProductionFlowcellPath.DILUTION_TO_FLOWCELL, null,
                        Workflow.AGILENT_EXOME_EXPRESS);
        dilutionTube2500 = flowcellEntityBuilder.getDilutionRack().getContainerRole().getVesselAtPosition(
                VesselPosition.A01);

        flowcellHiSeq2500 = flowcellEntityBuilder.getIlluminaFlowcell();
        flowcellHiSeq2500Barcode = flowcellHiSeq2500.getLabel();

        qtpEntityBuilder = runQtpProcess(hybridSelectionEntityBuilder.getNormCatchRack(),
                hybridSelectionEntityBuilder.getNormCatchBarcodes(),
                hybridSelectionEntityBuilder.getMapBarcodeToNormCatchTubes(),
                "1");

        denatureTube2000 = qtpEntityBuilder.getDenatureRack().getContainerRole().getVesselAtPosition(VesselPosition.A01);


        fctBatchHiSeq2000 = new LabBatch(FLOWCELL_2000_TICKET, LabBatch.LabBatchType.FCT,
                IlluminaFlowcell.FlowcellType.HiSeqFlowcell, denatureTube2000, BIG_DECIMAL_12_33);

        template = null;

        qtpEntityBuilder = runQtpProcess(hybridSelectionEntityBuilder.getNormCatchRack(),
                hybridSelectionEntityBuilder.getNormCatchBarcodes(),
                hybridSelectionEntityBuilder.getMapBarcodeToNormCatchTubes(),
                "4000");

        denatureTube4000 = qtpEntityBuilder.getDenatureRack().getContainerRole().getVesselAtPosition(VesselPosition.A01);

        VesselPosition[] hiseq4000VesselPositions =
                IlluminaFlowcell.FlowcellType.HiSeq4000Flowcell.getVesselGeometry().getVesselPositions();
        List<VesselPosition> vesselPositionList = Arrays.asList(hiseq4000VesselPositions);
        List<List<VesselPosition>> partition = Lists.partition(vesselPositionList, hiseq4000VesselPositions.length / 2);
        List<VesselPosition> vesselPositions1 = partition.get(0);
        List<VesselPosition> vesselPositions2 = partition.get(1);
        List<LabBatch.VesselToLanesInfo> vesselToLanesInfos = new ArrayList<>();

        vesselToLanesInfo = new LabBatch.VesselToLanesInfo(
                vesselPositions1, new BigDecimal("16.22"), denatureTube2000, workflowBatch.getBatchName(),
                productOrder.getProduct().getProductName(), Collections.<FlowcellDesignation>emptyList());

        vesselToLanesInfo2 = new LabBatch.VesselToLanesInfo(
                vesselPositions2, BIG_DECIMAL_12_33, denatureTube4000, workflowBatch.getBatchName(),
                productOrder.getProduct().getProductName(), Collections.<FlowcellDesignation>emptyList());

        vesselToLanesInfos.add(vesselToLanesInfo);
        vesselToLanesInfos.add(vesselToLanesInfo2);

        fctBatchHiSeq4000 = new LabBatch(FLOWCELL_4000_TICKET, vesselToLanesInfos,
                LabBatch.LabBatchType.FCT, IlluminaFlowcell.FlowcellType.HiSeq4000Flowcell);

        Map<VesselPosition, BarcodedTube> mapPositionToTube = new HashMap<>();
        mapPositionToTube.put(VesselPosition.A01, denatureTube2000);
        mapPositionToTube.put(VesselPosition.B01, denatureTube4000);
        TubeFormation rearrayedDenatureRack = new TubeFormation(mapPositionToTube, RackOfTubes.RackType.Matrix96);
        rearrayedDenatureRack.addRackOfTubes(new RackOfTubes("denatureRearray", RackOfTubes.RackType.Matrix96));
        HiSeq4000FlowcellEntityBuilder flowcell4000EntityBuilder =
                runHiSeq4000FlowcellProcess(rearrayedDenatureRack, null, BARCODE_SUFFIX + "4ADXX",
                        fctBatchHiSeq4000, null, HiSeq4000FlowcellEntityBuilder.FCTCreationPoint.DENATURE);
        dilutionTube4000 = flowcellEntityBuilder.getDilutionRack().getContainerRole().getVesselAtPosition(
                VesselPosition.A01);

        flowcellHiSeq4000 = flowcell4000EntityBuilder.getIlluminaFlowcell();
    }

    public void testGetSequencingTemplateFromReagentKitPoolTest() {
        template = factory.getSequencingTemplate(reagentKit, true);
        assertThat(template.getBarcode(), Matchers.nullValue());
        assertThat(template.getLanes().size(), is(1));
        assertThat(template.getOnRigChemistry(), is("Default"));
        assertThat(template.getOnRigWorkflow(), is("Resequencing"));
        assertThat(template.getReadStructure(), is(POOL_TEST_CIGAR));

        assertThat(template.getLanes().get(0).getLaneName(), is("LANE1"));
        assertThat(template.getLanes().get(0).getLoadingVesselLabel(), is(""));
        assertThat(template.getLanes().get(0).getDerivedVesselLabel(), is(denatureTube2500.getLabel()));
    }

    public void testGetSequencingTemplateFromReagentKitProduction() {
        template = factory.getSequencingTemplate(reagentKit, false);
        assertThat(template.getBarcode(), Matchers.nullValue());
        assertThat(template.getLanes().size(), is(1));
        assertThat(template.getOnRigChemistry(), is(nullValue()));
        assertThat(template.getOnRigWorkflow(), is(nullValue()));
        assertThat(template.getReadStructure(), is(PRODUCTION_CIGAR));

        assertThat(template.getLanes().get(0).getLaneName(), is("LANE1"));
        assertThat(template.getLanes().get(0).getLoadingVesselLabel(), is(""));
        assertThat(template.getLanes().get(0).getDerivedVesselLabel(), is(denatureTube2500.getLabel()));
    }

    public void testGetSequencingTemplatePoolTest() {
        Set<VesselAndPosition> vesselsAndPositions = flowcellHiSeq2500.getLoadingVessels();
        MatcherAssert.assertThat(vesselsAndPositions, not(Matchers.empty()));
        // Does a pool test with the HiSeq flowcell.
        template = factory.getSequencingTemplate(flowcellHiSeq2500, vesselsAndPositions, true);
        assertThat(template.getOnRigChemistry(), is("Default"));
        assertThat(template.getOnRigWorkflow(), is("Resequencing"));
        assertThat(template.getReadStructure(), is(POOL_TEST_CIGAR));
        assertThat(template.getBarcode(), equalTo(flowcellHiSeq2500Barcode));
        assertThat(template.getLanes().size(), is(2));
        Set<String> allLanes = new HashSet<>();

        for (SequencingTemplateLaneType lane : template.getLanes()) {
            allLanes.add(lane.getLaneName());
            assertThat(lane.getDerivedVesselLabel(), equalTo(denatureTube2500.getLabel()));
            assertThat(lane.getLoadingVesselLabel(), equalTo(dilutionTube2500.getLabel()));
        }
        assertThat(allLanes, hasItem("LANE1"));
        assertThat(allLanes, hasItem("LANE2"));
    }

    public void testGetSequencingTemplateProduction() {
        Set<VesselAndPosition> vesselsAndPositions = flowcellHiSeq2500.getLoadingVessels();
        MatcherAssert.assertThat(vesselsAndPositions, not(Matchers.empty()));
        template = factory.getSequencingTemplate(flowcellHiSeq2500, vesselsAndPositions, false);
        assertThat(template.getOnRigChemistry(), is(nullValue()));
        assertThat(template.getOnRigWorkflow(), is(nullValue()));
        assertThat(template.getRegulatoryDesignation(), Matchers.hasSize(1));
        assertThat(template.getRegulatoryDesignation(), Matchers.hasItem("RESEARCH_ONLY"));
        assertThat(template.getProducts(), not(empty()));

        assertThat(template.getReadStructure(), is(PRODUCTION_CIGAR));

        assertThat(template.getBarcode(), equalTo(flowcellHiSeq2500Barcode));
        assertThat(template.getLanes().size(), is(2));
        Set<String> allLanes = new HashSet<>();

        for (SequencingTemplateLaneType lane : template.getLanes()) {
            allLanes.add(lane.getLaneName());
            assertThat(lane.getDerivedVesselLabel(), equalTo(denatureTube2500.getLabel()));
            assertThat(lane.getLoadingVesselLabel(), equalTo(dilutionTube2500.getLabel()));
        }
        assertThat(allLanes, hasItem("LANE1"));
        assertThat(allLanes, hasItem("LANE2"));
    }

    public void testGetLoadingVesselsForFlowcell() {
        Set<VesselAndPosition> vesselsAndPositions = flowcellHiSeq2500.getLoadingVessels();
        MatcherAssert.assertThat(vesselsAndPositions, not(Matchers.empty()));
        final List<VesselPosition> vesselPositions = Arrays.asList(VesselPosition.LANE1, VesselPosition.LANE2);

        for (VesselAndPosition vesselsAndPosition : vesselsAndPositions) {
            assertThat(vesselsAndPosition.getPosition(),
                    AnyOf.anyOf(equalTo(VesselPosition.LANE1), equalTo(VesselPosition.LANE2)));
            assertThat(dilutionTube2500, equalTo(vesselsAndPosition.getVessel()));
        }

    }

    public void testGetSequencingTemplateFromDenatureTubePoolTest() {
        template = factory.getSequencingTemplate(denatureTube2500, true);
        assertThat(template.getBarcode(), Matchers.nullValue());
        assertThat(template.getLanes().size(), is(1));
        assertThat(template.getOnRigChemistry(), is("Default"));
        assertThat(template.getOnRigWorkflow(), is("Resequencing"));
        assertThat(template.getReadStructure(), is(POOL_TEST_CIGAR));

        assertThat(template.getLanes().get(0).getLaneName(), is("LANE1"));
        assertThat(template.getLanes().get(0).getLoadingVesselLabel(), is(""));
        assertThat(template.getLanes().get(0).getDerivedVesselLabel(), is(denatureTube2500.getLabel()));
        assertThat(template.getLanes().get(0).getLoadingConcentration(), is(BigDecimal.valueOf(7.0f)));
    }

    public void testGetSequencingTemplateFromDenatureTubeProduction() {
        template = factory.getSequencingTemplate(denatureTube2500, false);
        assertThat(template.getBarcode(), Matchers.nullValue());
        assertThat(template.getOnRigChemistry(), is(nullValue()));
        assertThat(template.getOnRigWorkflow(), is(nullValue()));
        assertThat(template.getReadStructure(), is(PRODUCTION_CIGAR));
        assertThat(template.getRegulatoryDesignation(), Matchers.hasSize(1));
        assertThat(template.getRegulatoryDesignation(), Matchers.hasItem("RESEARCH_ONLY"));
        assertThat(template.getProducts(), not(empty()));
        assertThat(template.getLanes().size(), is(2));
        Set<String> allLanes = new HashSet<>();

        for (SequencingTemplateLaneType lane : template.getLanes()) {
            allLanes.add(lane.getLaneName());
            assertThat(lane.getLoadingVesselLabel(), equalTo(""));
            assertThat(lane.getDerivedVesselLabel(), equalTo(denatureTube2500.getLabel()));
            assertThat(lane.getLoadingConcentration(), is(BIG_DECIMAL_12_33));
        }
        assertThat(allLanes, hasItem("LANE1"));
        assertThat(allLanes, hasItem("LANE2"));
    }

    public void testGetSequencingTemplateFromDilutionTubeProduction() {
        template = factory.getSequencingTemplate(dilutionTube2500, false);
        assertThat(template.getBarcode(), Matchers.nullValue());
        assertThat(template.getOnRigChemistry(), is(nullValue()));
        assertThat(template.getOnRigWorkflow(), is(nullValue()));
        assertThat(template.getReadStructure(), is(PRODUCTION_CIGAR));
        assertThat(template.getRegulatoryDesignation(), Matchers.hasSize(1));
        assertThat(template.getRegulatoryDesignation(), Matchers.hasItem("RESEARCH_ONLY"));
        assertThat(template.getProducts(), not(empty()));
        assertThat(template.getLanes().size(), is(2));
        Set<String> allLanes = new HashSet<>();

        for (SequencingTemplateLaneType lane : template.getLanes()) {
            allLanes.add(lane.getLaneName());
            assertThat(lane.getLoadingVesselLabel(), equalTo(""));
            assertThat(lane.getDerivedVesselLabel(), equalTo(denatureTube2500.getLabel()));
            assertThat(lane.getLoadingConcentration(), is(BIG_DECIMAL_12_33));
        }
        assertThat(allLanes, hasItem("LANE1"));
        assertThat(allLanes, hasItem("LANE2"));
    }

    public void testGetSequencingTemplateFromFctProduction() {
        template = factory.getSequencingTemplate(fctBatch, false);
        assertThat(template.getBarcode(), Matchers.nullValue());
        assertThat(template.getOnRigChemistry(), is(nullValue()));
        assertThat(template.getOnRigWorkflow(), is(nullValue()));
        assertThat(template.getReadStructure(), is(PRODUCTION_CIGAR));
        assertThat(template.getRegulatoryDesignation(), Matchers.hasSize(1));
        assertThat(template.getRegulatoryDesignation(), Matchers.hasItem("RESEARCH_ONLY"));
        assertThat(template.getProducts(), not(empty()));
        assertThat(template.getLanes().size(), is(2));
        Set<String> allLanes = new HashSet<>();

        for (SequencingTemplateLaneType lane : template.getLanes()) {
            allLanes.add(lane.getLaneName());
            assertThat(lane.getLoadingVesselLabel(), equalTo(""));
            assertThat(lane.getDerivedVesselLabel(), equalTo(denatureTube2500.getLabel()));
            assertThat(lane.getLoadingConcentration(), is(BIG_DECIMAL_12_33));
        }
        assertThat(allLanes, hasItem("LANE1"));
        assertThat(allLanes, hasItem("LANE2"));
    }

    public void testGetSequencingTemplateForHiSeq2000() {
        template = factory.getSequencingTemplate(fctBatchHiSeq2000, false);
        assertThat(template.getBarcode(), Matchers.nullValue());
        assertThat(template.getOnRigChemistry(), is(nullValue()));
        assertThat(template.getOnRigWorkflow(), is(nullValue()));
        assertThat(template.getReadStructure(), is(PRODUCTION_CIGAR));
        assertThat(template.getLanes().size(), is(8));
        Set<String> allLanes = new HashSet<>();

        for (SequencingTemplateLaneType lane : template.getLanes()) {
            allLanes.add(lane.getLaneName());
            assertThat(lane.getLoadingVesselLabel(), equalTo(""));
            assertThat(lane.getDerivedVesselLabel(), equalTo(denatureTube2000.getLabel()));
            assertThat(lane.getLoadingConcentration(), is(BIG_DECIMAL_12_33));
        }
        for(int i = 1; i <= 8; i++) {
            assertThat(allLanes, hasItem("LANE" + i));
        }
    }

    public void testGetSequencingTemplateForHiSeq4000() {
        template = factory.getSequencingTemplate(fctBatchHiSeq4000, false);
        assertThat(template.getBarcode(), Matchers.nullValue());
        assertThat(template.getOnRigChemistry(), is(nullValue()));
        assertThat(template.getOnRigWorkflow(), is(nullValue()));
        assertThat(template.getReadStructure(), is(PRODUCTION_CIGAR));
        assertThat(template.getLanes().size(), is(8));
        Set<String> allLanes = new HashSet<>();

        for (SequencingTemplateLaneType lane : template.getLanes()) {
            LabBatch.VesselToLanesInfo laneInfo = null;
            BigDecimal loadingConcentration = null;
            if(vesselToLanesInfo.getLabVessel().getLabel().equals(lane.getDerivedVesselLabel())) {
                laneInfo = vesselToLanesInfo;
                loadingConcentration = new BigDecimal("16.22");
            } else if(vesselToLanesInfo2.getLabVessel().getLabel().equals(lane.getDerivedVesselLabel())) {
                laneInfo = vesselToLanesInfo2;
                loadingConcentration = new BigDecimal("12.33");
            }
            assertThat(laneInfo, not(nullValue()));
            boolean foundVesselPosition = false;
            for (VesselPosition vesselPosition : laneInfo.getLanes()) {
                if(vesselPosition.name().equals(lane.getLaneName())) {
                    allLanes.add(lane.getLaneName());
                    foundVesselPosition = true;
                    break;
                }
            }
            assertThat(foundVesselPosition, is(true));
            assertThat(lane.getLoadingVesselLabel(), equalTo(""));
            assertThat(lane.getLoadingConcentration(), is(loadingConcentration));
        }
        for(int i = 1; i <= 8; i++) {
            assertThat(allLanes, hasItem("LANE" + i));
        }
    }

    public void testGetSeqTemplateForHiseq4000Flowcell() {
        template = factory.getSequencingTemplate(flowcellHiSeq4000, flowcellHiSeq4000.getLoadingVessels(), false);
        assertThat(template.getBarcode(), is(flowcellHiSeq4000.getLabel()));
        assertThat(template.getOnRigChemistry(), is(nullValue()));
        assertThat(template.getOnRigWorkflow(), is(nullValue()));
        assertThat(template.getReadStructure(), is(PRODUCTION_CIGAR));
        assertThat(template.getLanes().size(), is(8));
        Set<String> allLanes = new HashSet<>();

        for (SequencingTemplateLaneType lane : template.getLanes()) {
            LabBatch.VesselToLanesInfo laneInfo = null;
            if(vesselToLanesInfo.getLabVessel().getLabel().equals(lane.getDerivedVesselLabel())) {
                laneInfo = vesselToLanesInfo;
                assertThat(lane.getLoadingVesselLabel(), equalTo("DenatureTube10"));
            } else if(vesselToLanesInfo2.getLabVessel().getLabel().equals(lane.getDerivedVesselLabel())) {
                laneInfo = vesselToLanesInfo2;
                assertThat(lane.getLoadingVesselLabel(), equalTo("DenatureTube40000"));
            }
            assertThat(laneInfo, not(nullValue()));
            boolean foundVesselPosition = false;
            for (VesselPosition vesselPosition : laneInfo.getLanes()) {
                if(vesselPosition.name().equals(lane.getLaneName())) {
                    allLanes.add(lane.getLaneName());
                    foundVesselPosition = true;
                    break;
                }
            }
            assertThat(foundVesselPosition, is(true));
        }
        for(int i = 1; i <= 8; i++) {
            assertThat(allLanes, hasItem("LANE" + i));
        }
    }

    // Tests getSequencingTemplate from a denature tube with designation parameter override.
    public void testDesignationVessel() {

        for (boolean poolTest : new boolean[]{false, true}) {
            // getSequencingTemplate() appears to only support pool tests on a MiSeq.
            IlluminaFlowcell.FlowcellType flowcellType = poolTest ?
                    IlluminaFlowcell.FlowcellType.MiSeqFlowcell : IlluminaFlowcell.FlowcellType.HiSeq2500Flowcell;

            FlowcellDesignation designation = new FlowcellDesignation(denatureTube2500, workflowBatch,
                    FlowcellDesignation.IndexType.DUAL, poolTest,
                    flowcellType, 4, 99, BIG_DECIMAL_7_77, true,
                    FlowcellDesignation.Status.IN_FCT, FlowcellDesignation.Priority.NORMAL);
            flowcellDesignations.clear();
            flowcellDesignations.add(designation);

            template = factory.getSequencingTemplate(denatureTube2500, poolTest);
            assertThat(template.getBarcode(), Matchers.nullValue());
            if (poolTest) {
                assertThat(template.getOnRigChemistry(), is("Default"));
                assertThat(template.getOnRigWorkflow(), is("Resequencing"));
                assertThat(template.getReadStructure(), is(POOL_TEST_CIGAR));
            } else {
                assertThat(template.getOnRigChemistry(), is(nullValue()));
                assertThat(template.getOnRigWorkflow(), is(nullValue()));
                assertThat(template.getReadStructure(), is(DESIGNATION_CIGAR));
            }
            assertThat(template.getRegulatoryDesignation(), Matchers.hasSize(1));
            assertThat(template.getRegulatoryDesignation(), Matchers.hasItem("RESEARCH_ONLY"));
            assertThat(template.getProducts(), not(empty()));
            assertThat(template.getLanes().size(), is(flowcellType.getVesselGeometry().getCapacity()));
            assertThat(template.getConcentration(), is(BIG_DECIMAL_7_77));

            Set<String> allLanes = new HashSet<>();
            for (SequencingTemplateLaneType lane : template.getLanes()) {
                allLanes.add(lane.getLaneName());
                assertThat(lane.getLoadingVesselLabel(), equalTo(""));
                assertThat(lane.getDerivedVesselLabel(), equalTo(denatureTube2500.getLabel()));
                assertThat(lane.getLoadingConcentration(), is(BIG_DECIMAL_7_77));
            }
            assertThat(allLanes, hasItem("LANE1"));
            if (flowcellType != IlluminaFlowcell.FlowcellType.MiSeqFlowcell) {
                assertThat(allLanes, hasItem("LANE2"));
            }
        }
    }

    // Tests getSequencingTemplate from a flowcell that has a designation.
    public void testDesignationFlowcell() {

        for (boolean poolTest : new boolean[]{false, true}) {

            FlowcellDesignation designation = new FlowcellDesignation(denatureTube2000, workflowBatch,
                    FlowcellDesignation.IndexType.DUAL, poolTest,
                    IlluminaFlowcell.FlowcellType.HiSeq2500Flowcell, 4, 99, BIG_DECIMAL_7_77, true,
                    FlowcellDesignation.Status.IN_FCT, FlowcellDesignation.Priority.NORMAL);
            flowcellDesignations.clear();
            flowcellDesignations.add(designation);

            Set<VesselAndPosition> vesselsAndPositions = flowcellHiSeq2500.getLoadingVessels();
            MatcherAssert.assertThat(vesselsAndPositions, not(Matchers.empty()));
            template = factory.getSequencingTemplate(flowcellHiSeq2500, vesselsAndPositions, poolTest);
            if (poolTest) {
                assertThat(template.getOnRigChemistry(), is("Default"));
                assertThat(template.getOnRigWorkflow(), is("Resequencing"));
                assertThat(template.getReadStructure(), is(POOL_TEST_CIGAR));
            } else {
                assertThat(template.getOnRigChemistry(), is(nullValue()));
                assertThat(template.getOnRigWorkflow(), is(nullValue()));
                assertThat(template.getReadStructure(), is(DESIGNATION_CIGAR));
            }
            assertThat(template.getBarcode(), equalTo(flowcellHiSeq2500Barcode));
            assertThat(template.getLanes().size(), is(2));
            Set<String> allLanes = new HashSet<>();
            for (SequencingTemplateLaneType lane : template.getLanes()) {
                allLanes.add(lane.getLaneName());
                assertThat(lane.getDerivedVesselLabel(), equalTo(denatureTube2500.getLabel()));
                assertThat(lane.getLoadingVesselLabel(), equalTo(dilutionTube2500.getLabel()));
                assertThat(lane.getLoadingConcentration(), is(BIG_DECIMAL_7_77));
            }
            assertThat(allLanes, hasItem("LANE1"));
            assertThat(allLanes, hasItem("LANE2"));
        }
    }

    // Tests getSequencingTemplate from a reagent kit with designation parameter override.
    public void testDesignationReagentKit() {
        for (boolean poolTest : new boolean[]{false, true}) {

            LabVessel loadingTube = miSeqBatch.getLabBatchStartingVessels().iterator().next().getLabVessel();
            FlowcellDesignation designation = new FlowcellDesignation(loadingTube, workflowBatch,
                    FlowcellDesignation.IndexType.DUAL, poolTest,
                    IlluminaFlowcell.FlowcellType.MiSeqFlowcell, 4, 100, BIG_DECIMAL_7_77, true,
                    FlowcellDesignation.Status.IN_FCT, FlowcellDesignation.Priority.NORMAL);
            flowcellDesignations.clear();
            flowcellDesignations.add(designation);

            template = factory.getSequencingTemplate(reagentKit, poolTest);
            assertThat(template.getBarcode(), Matchers.nullValue());
            assertThat(template.getLanes().size(), is(1));
            assertThat(template.getBarcode(), Matchers.nullValue());
            if (poolTest) {
                assertThat(template.getOnRigChemistry(), is("Default"));
                assertThat(template.getOnRigWorkflow(), is("Resequencing"));
                assertThat(template.getReadStructure(), is(POOL_TEST_CIGAR));
            } else {
                assertThat(template.getOnRigChemistry(), is(nullValue()));
                assertThat(template.getOnRigWorkflow(), is(nullValue()));
                assertThat(template.getReadStructure(), is(PRODUCTION_CIGAR));
            }
            assertThat(template.getLanes().get(0).getLaneName(), is("LANE1"));
            assertThat(template.getLanes().get(0).getLoadingVesselLabel(), is(""));
            assertThat(template.getLanes().get(0).getDerivedVesselLabel(), is(denatureTube2500.getLabel()));
            assertThat(template.getLanes().get(0).getLoadingConcentration(), is(BIG_DECIMAL_7_77));
            assertThat(template.getConcentration(), Matchers.nullValue());
        }
    }

    public void testMultipleDesignations() {
        BigDecimal denature4000Conc = new BigDecimal("8.88");
        BigDecimal denature2500Conc = new BigDecimal("13.33");
        FlowcellDesignation designation = new FlowcellDesignation(denatureTube4000, fctBatchHiSeq4000,
                FlowcellDesignation.IndexType.DUAL, false,
                IlluminaFlowcell.FlowcellType.HiSeq2500Flowcell, 4, 99, denature4000Conc, true,
                FlowcellDesignation.Status.IN_FCT, FlowcellDesignation.Priority.NORMAL);

        FlowcellDesignation designation2 = new FlowcellDesignation(denatureTube2500, fctBatchHiSeq4000,
                FlowcellDesignation.IndexType.DUAL, false,
                IlluminaFlowcell.FlowcellType.HiSeq2500Flowcell, 4, 99, denature2500Conc, true,
                FlowcellDesignation.Status.IN_FCT, FlowcellDesignation.Priority.NORMAL);

        flowcellDesignations.clear();
        flowcellDesignations.add(designation);
        flowcellDesignations.add(designation2);

        // Verifies the transient fields are correct.
        for (LabBatchStartingVessel labBatchStartingVessel : fctBatchHiSeq4000.getLabBatchStartingVessels()) {
            Assert.assertEquals(labBatchStartingVessel.getLinkedLcset(), workflowBatch.getBatchName());
            Assert.assertEquals(labBatchStartingVessel.getProductNames(), productOrder.getProduct().getProductName());
        }

        template = factory.getSequencingTemplate(fctBatchHiSeq4000, false);
        for (SequencingTemplateLaneType laneType: template.getLanes()) {
            if (laneType.getDerivedVesselLabel().equals(denatureTube2500.getLabel())) {
                assertThat(laneType.getLoadingConcentration(), is(denature2500Conc));
            } else if (laneType.getDerivedVesselLabel().equals(denatureTube4000.getLabel())) {
                assertThat(laneType.getLoadingConcentration(), is(denature4000Conc));
            } else {
                String failMsg = String.format("Expected to only find tubes %s and %s but found %s",
                        denatureTube2500.getLabel(), denatureTube4000.getLabel(), laneType.getDerivedVesselLabel());
                Assert.fail(failMsg);
            }
        }
    }

    @Test
    public void testSingleUmiMultiDesignations() {
        UniqueMolecularIdentifier umiReagent = LabEventTest.createUmi(6, 3, UniqueMolecularIdentifier.UMILocation.BEFORE_SECOND_INDEX_READ);
        StaticPlate umiPlate = LabEventTest.buildUmiPlate("UMITestPlate0101", umiReagent);
        testUniqueMolecularIdentifierMultiDesignations(umiPlate, "99T8B6M3S8B99T", LibraryConstructionEntityBuilder.Umi.SINGLE);
    }

    @Test
    public void testDoubleUMIMultiDesignations() {
        UniqueMolecularIdentifier umiReagent = LabEventTest.createUmi(3, 2, UniqueMolecularIdentifier.UMILocation.BEFORE_FIRST_READ);
        UniqueMolecularIdentifier umiReagent2 = LabEventTest.createUmi(3, 2, UniqueMolecularIdentifier.UMILocation.BEFORE_SECOND_READ);
        StaticPlate umiPlate = LabEventTest.buildUmiPlate("UMITestPlate0101", umiReagent);
        LabEventTest.attachUMIToPlate(umiReagent2, umiPlate);
        testUniqueMolecularIdentifierMultiDesignations(umiPlate, "3M2S94T8B8B3M2S94T",
                LibraryConstructionEntityBuilder.Umi.DUAL);
    }

    @Test
    public void testDoubleUMIFromTubeMultiDesignations() {
        UMIReagent umiReagent = new UMIReagent(
                LabEventTest.createUmi(3, 2, UniqueMolecularIdentifier.UMILocation.BEFORE_FIRST_READ));
        UMIReagent umiReagent2 = new UMIReagent(
                LabEventTest.createUmi(3, 2, UniqueMolecularIdentifier.UMILocation.BEFORE_SECOND_READ));
        BarcodedTube barcodedTube = new BarcodedTube("UmiTestTube01232", BarcodedTube.BarcodedTubeType.MatrixTube075);
        barcodedTube.addReagent(umiReagent);
        barcodedTube.addReagent(umiReagent2);
        testUniqueMolecularIdentifierMultiDesignations(barcodedTube, "3M2S94T8B8B3M2S94T",
                LibraryConstructionEntityBuilder.Umi.DUAL);
    }

    @Test
    public void testMixIndexLengths() {
        VesselPosition[] hiseq4000VesselPositions =
                IlluminaFlowcell.FlowcellType.HiSeq4000Flowcell.getVesselGeometry().getVesselPositions();
        List<VesselPosition> vesselPositionList = Arrays.asList(hiseq4000VesselPositions);
        List<List<VesselPosition>> partition = Lists.partition(vesselPositionList, hiseq4000VesselPositions.length / 2);
        List<VesselPosition> vesselPositions1 = partition.get(0);
        List<VesselPosition> vesselPositions2 = partition.get(1);
        List<LabBatch.VesselToLanesInfo> vesselToLanesInfos = new ArrayList<>();

        vesselToLanesInfo = new LabBatch.VesselToLanesInfo(
                vesselPositions1, new BigDecimal("16.22"), denatureTube2000, workflowBatch.getBatchName(),
                productOrder.getProduct().getProductName(), Collections.<FlowcellDesignation>emptyList());

        vesselToLanesInfo2 = new LabBatch.VesselToLanesInfo(
                vesselPositions2, BIG_DECIMAL_12_33, singleIndexTube, workflowBatch.getBatchName(),
                productOrder.getProduct().getProductName(), Collections.<FlowcellDesignation>emptyList());

        vesselToLanesInfos.add(vesselToLanesInfo);
        vesselToLanesInfos.add(vesselToLanesInfo2);

        fctBatchHiSeq4000 = new LabBatch(FLOWCELL_4000_TICKET, vesselToLanesInfos,
                LabBatch.LabBatchType.FCT, IlluminaFlowcell.FlowcellType.HiSeq4000Flowcell);

        FlowcellDesignation designation = new FlowcellDesignation(denatureTube2000, fctBatchHiSeq4000,
                FlowcellDesignation.IndexType.DUAL, false,
                IlluminaFlowcell.FlowcellType.HiSeq2500Flowcell, 4, 99, new BigDecimal("2.2"), true,
                FlowcellDesignation.Status.IN_FCT, FlowcellDesignation.Priority.NORMAL);

        FlowcellDesignation designation2 = new FlowcellDesignation(singleIndexTube, fctBatchHiSeq4000,
                FlowcellDesignation.IndexType.SINGLE, false,
                IlluminaFlowcell.FlowcellType.HiSeq2500Flowcell, 4, 99, new BigDecimal("2.3"), true,
                FlowcellDesignation.Status.IN_FCT, FlowcellDesignation.Priority.NORMAL);

        flowcellDesignations.clear();
        flowcellDesignations.add(designation);
        flowcellDesignations.add(designation2);

        template = factory.getSequencingTemplate(fctBatchHiSeq4000, false);
        Assert.assertEquals(template.getReadStructure(), "99T8B8B99T");
    }

    public void testZeroReadLenthDesignation() {
        FlowcellDesignation designation = new FlowcellDesignation(denatureTube2000, workflowBatch,
                FlowcellDesignation.IndexType.DUAL, false,
                IlluminaFlowcell.FlowcellType.HiSeq2500Flowcell, 4, 0, BIG_DECIMAL_7_77, true,
                FlowcellDesignation.Status.IN_FCT, FlowcellDesignation.Priority.NORMAL);
        flowcellDesignations.clear();
        flowcellDesignations.add(designation);

        Set<VesselAndPosition> vesselsAndPositions = flowcellHiSeq2500.getLoadingVessels();
        MatcherAssert.assertThat(vesselsAndPositions, not(Matchers.empty()));
        template = factory.getSequencingTemplate(flowcellHiSeq2500, vesselsAndPositions, false);
        assertThat(template.getOnRigChemistry(), is(nullValue()));
        assertThat(template.getOnRigWorkflow(), is(nullValue()));
        assertThat(template.getReadStructure(), is("0T8B8B0T"));
    }

    private void testUniqueMolecularIdentifierMultiDesignations(LabVessel umiPlate, String UMIReadStructure,
                                                                LibraryConstructionEntityBuilder.Umi umiType) {
        final ProductOrder
                productOrder = ProductOrderTestFactory.buildExExProductOrder(96);
        runDate = new Date();
        Map<String, BarcodedTube> mapBarcodeToTube = createInitialRack(productOrder, "UMI");
        workflowBatch = new LabBatch("Exome Express Batch",
                new HashSet<LabVessel>(mapBarcodeToTube.values()),
                LabBatch.LabBatchType.WORKFLOW);
        workflowBatch.setWorkflow(Workflow.AGILENT_EXOME_EXPRESS);
        workflowBatch.setCreatedOn(EX_EX_IN_MERCURY_CALENDAR.getTime());

        //Generate new rack, this time with a UMI on the LC plate
        bucketBatchAndDrain(mapBarcodeToTube, productOrder, workflowBatch, BARCODE_SUFFIX);
        PicoPlatingEntityBuilder picoPlatingEntityBuilder = runPicoPlatingProcess(mapBarcodeToTube,
                String.valueOf(runDate.getTime()),
                BARCODE_SUFFIX, true);

        exomeExpressShearingEntityBuilder =
                runExomeExpressShearingProcess(picoPlatingEntityBuilder.getNormBarcodeToTubeMap(),
                        picoPlatingEntityBuilder.getNormTubeFormation(),
                        picoPlatingEntityBuilder.getNormalizationBarcode(), "UMIShearingSuffix");

        shearingCleanupPlate = exomeExpressShearingEntityBuilder.getShearingCleanupPlate();
        shearingCleanupPlate.clearCaches();
        if (umiPlate instanceof StaticPlate) {
            LabEventTestFactory.doSectionTransfer(LabEventType.UMI_ADDITION, umiPlate, shearingCleanupPlate);
        } else if (umiPlate instanceof BarcodedTube) {
            LabEvent labEvent = new LabEvent(LabEventType.UMI_ADDITION, new Date(), "Mercury", 1L, 1L, "SeqTestFactory");
            labEvent.getVesselToSectionTransfers().add(new VesselToSectionTransfer(umiPlate, SBSSection.ALL96,
                    shearingCleanupPlate.getContainerRole(), shearingCleanupPlate, labEvent));
        }

        QtpEntityBuilder qtpEntityBuilder = runUpToQTP(exomeExpressShearingEntityBuilder, "UMI1010",
                LibraryConstructionEntityBuilder.Indexing.DUAL, umiType);

        BarcodedTube denatureTubeUmi = qtpEntityBuilder.getDenatureRack().getContainerRole()
                .getVesselAtPosition(VesselPosition.A01);

        VesselPosition[] hiseq4000VesselPositions =
                IlluminaFlowcell.FlowcellType.HiSeq4000Flowcell.getVesselGeometry().getVesselPositions();
        List<VesselPosition> vesselPositionList = Arrays.asList(hiseq4000VesselPositions);
        List<List<VesselPosition>> partition = Lists.partition(vesselPositionList, hiseq4000VesselPositions.length / 2);
        List<VesselPosition> vesselPositions1 = partition.get(0);
        List<VesselPosition> vesselPositions2 = partition.get(1);

        vesselToLanesInfo = new LabBatch.VesselToLanesInfo(vesselPositions1, new BigDecimal("16.22"), denatureTube2000,
                workflowBatch.getBatchName(),
                productOrder.getProduct().getProductName(), Collections.<FlowcellDesignation>emptyList());

        vesselToLanesInfo2 = new LabBatch.VesselToLanesInfo(vesselPositions2, BIG_DECIMAL_12_33, denatureTubeUmi,
                workflowBatch.getBatchName(),
                productOrder.getProduct().getProductName(), Collections.<FlowcellDesignation>emptyList());
        List<LabBatch.VesselToLanesInfo> vesselToLanesInfos = new ArrayList<>();

        vesselToLanesInfos.add(vesselToLanesInfo);
        vesselToLanesInfos.add(vesselToLanesInfo2);

        fctBatchHiSeq4000 = new LabBatch(FLOWCELL_4000_TICKET, vesselToLanesInfos,
                LabBatch.LabBatchType.FCT, IlluminaFlowcell.FlowcellType.HiSeq4000Flowcell);

        BigDecimal denatureTubeUmiConc = new BigDecimal("8.88");
        BigDecimal denature2500Conc = new BigDecimal("13.33");

        FlowcellDesignation designation = new FlowcellDesignation(denatureTubeUmi, fctBatchHiSeq4000,
                FlowcellDesignation.IndexType.DUAL, false,
                IlluminaFlowcell.FlowcellType.HiSeq2500Flowcell, 4, 99, denatureTubeUmiConc, true,
                FlowcellDesignation.Status.IN_FCT, FlowcellDesignation.Priority.NORMAL);

        FlowcellDesignation designation2 = new FlowcellDesignation(denatureTube2500, fctBatchHiSeq4000,
                FlowcellDesignation.IndexType.DUAL, false,
                IlluminaFlowcell.FlowcellType.HiSeq2500Flowcell, 4, 99, denature2500Conc, true,
                FlowcellDesignation.Status.IN_FCT, FlowcellDesignation.Priority.NORMAL);

        flowcellDesignations.clear();
        flowcellDesignations.add(designation);
        flowcellDesignations.add(designation2);

        template = factory.getSequencingTemplate(fctBatchHiSeq4000, false);
        for (SequencingTemplateLaneType laneType: template.getLanes()) {
            if (laneType.getDerivedVesselLabel().equals(denatureTube2500.getLabel())) {
                assertThat(laneType.getLoadingConcentration(), is(denature2500Conc));
                assertThat(laneType.getReadStructure(), is("99T8B8B99T"));
            } else if (laneType.getDerivedVesselLabel().equals(denatureTubeUmi.getLabel())) {
                assertThat(laneType.getLoadingConcentration(), is(denatureTubeUmiConc));
                assertThat(laneType.getReadStructure(), is(UMIReadStructure));
            } else {
                String failMsg = String.format("Expected to only find tubes %s and %s but found %s",
                        denatureTube2000.getLabel(), denatureTubeUmi.getLabel(), laneType.getDerivedVesselLabel());
                Assert.fail(failMsg);
            }
        }
    }

    private QtpEntityBuilder runUpToQTP(ExomeExpressShearingEntityBuilder shearingEntityBuilder, String qtpSuffix,
                                        LibraryConstructionEntityBuilder.Indexing indexing,
                                        LibraryConstructionEntityBuilder.Umi umiType) {
        LibraryConstructionEntityBuilder libraryConstructionEntityBuilder =
                runWgsLibraryConstructionProcessWithUMI(shearingEntityBuilder.getShearingCleanupPlate(),
                        shearingEntityBuilder.getShearCleanPlateBarcode(),
                        shearingEntityBuilder.getShearingPlate(), BARCODE_SUFFIX,
                        LibraryConstructionJaxbBuilder.PondType.REGULAR, indexing, umiType);
        HybridSelectionEntityBuilder hybridSelectionEntityBuilder =
                runHybridSelectionProcess(libraryConstructionEntityBuilder.getPondRegRack(),
                        libraryConstructionEntityBuilder.getPondRegRackBarcode(),
                        libraryConstructionEntityBuilder.getPondRegTubeBarcodes(), BARCODE_SUFFIX);
        return runQtpProcess(hybridSelectionEntityBuilder.getNormCatchRack(),
                hybridSelectionEntityBuilder.getNormCatchBarcodes(),
                hybridSelectionEntityBuilder.getMapBarcodeToNormCatchTubes(),
                qtpSuffix);
    }

}

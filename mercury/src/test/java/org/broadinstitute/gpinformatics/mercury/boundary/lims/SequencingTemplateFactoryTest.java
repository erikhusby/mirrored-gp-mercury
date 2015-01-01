package org.broadinstitute.gpinformatics.mercury.boundary.lims;

import org.broadinstitute.gpinformatics.athena.entity.orders.ProductOrder;
import org.broadinstitute.gpinformatics.infrastructure.test.dbfree.ProductOrderTestFactory;
import org.broadinstitute.gpinformatics.mercury.entity.labevent.LabEvent;
import org.broadinstitute.gpinformatics.mercury.entity.labevent.VesselToSectionTransfer;
import org.broadinstitute.gpinformatics.mercury.entity.run.IlluminaFlowcell;
import org.broadinstitute.gpinformatics.mercury.entity.vessel.LabVessel;
import org.broadinstitute.gpinformatics.mercury.entity.vessel.MiSeqReagentKit;
import org.broadinstitute.gpinformatics.mercury.entity.vessel.SBSSection;
import org.broadinstitute.gpinformatics.mercury.entity.vessel.BarcodedTube;
import org.broadinstitute.gpinformatics.mercury.entity.vessel.VesselAndPosition;
import org.broadinstitute.gpinformatics.mercury.entity.vessel.VesselPosition;
import org.broadinstitute.gpinformatics.mercury.entity.workflow.LabBatch;
import org.broadinstitute.gpinformatics.mercury.entity.workflow.Workflow;
import org.broadinstitute.gpinformatics.mercury.limsquery.generated.SequencingTemplateLaneType;
import org.broadinstitute.gpinformatics.mercury.limsquery.generated.SequencingTemplateType;
import org.broadinstitute.gpinformatics.mercury.test.BaseEventTest;
import org.broadinstitute.gpinformatics.mercury.test.builders.ExomeExpressShearingEntityBuilder;
import org.broadinstitute.gpinformatics.mercury.test.builders.HiSeq2500FlowcellEntityBuilder;
import org.broadinstitute.gpinformatics.mercury.test.builders.HybridSelectionEntityBuilder;
import org.broadinstitute.gpinformatics.mercury.test.builders.LibraryConstructionEntityBuilder;
import org.broadinstitute.gpinformatics.mercury.test.builders.PicoPlatingEntityBuilder;
import org.broadinstitute.gpinformatics.mercury.test.builders.ProductionFlowcellPath;
import org.broadinstitute.gpinformatics.mercury.test.builders.QtpEntityBuilder;
import org.hamcrest.MatcherAssert;
import org.hamcrest.Matchers;
import org.hamcrest.core.AnyOf;
import org.testng.annotations.BeforeTest;
import org.testng.annotations.Test;

import java.math.BigDecimal;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.broadinstitute.gpinformatics.infrastructure.test.TestGroups.DATABASE_FREE;
import static org.broadinstitute.gpinformatics.mercury.entity.labevent.LabEventType.DENATURE_TO_REAGENT_KIT_TRANSFER;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.hasItem;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.IsNot.not;

/**
 * Database-free test for SequencingTemplateFactory.
 */
@Test(groups = DATABASE_FREE)
public class SequencingTemplateFactoryTest extends BaseEventTest {

    public static final String BARCODE_SUFFIX = "1";
    public static final String FLOWCELL_2500_TICKET = "FCT-3";
    private SequencingTemplateFactory factory = null;
    private BarcodedTube denatureTube = null;
    private BarcodedTube dilutionTube = null;
    private IlluminaFlowcell flowcell = null;
    private MiSeqReagentKit reagentKit = null;
    private static final String PRODUCTION_CIGAR = "76T8B8B76T";
    private static final String POOL_TEST_CIGAR = "8B8B";
    private SequencingTemplateType template;
    private Date runDate;
    String flowcellBarcode;
    private String denatureTubeBarcode;
    private String dilutionTubeBarcode;
    private LabBatch fctBatch;
    private LabBatch miseqBatch2;
    private LabBatch miseqBatch1;

    @Override
    @BeforeTest(alwaysRun = true)
    public void setUp() {
        expectedRouting = SystemRouter.System.MERCURY;

        super.setUp();
        factory = new SequencingTemplateFactory();

        final ProductOrder
                productOrder = ProductOrderTestFactory.buildExExProductOrder(96);
        runDate = new Date();
        Map<String, BarcodedTube> mapBarcodeToTube = createInitialRack(productOrder, "R");
        LabBatch workflowBatch = new LabBatch("Exome Express Batch",
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

        denatureTube = qtpEntityBuilder.getDenatureRack().getContainerRole().getVesselAtPosition(VesselPosition.A01);
        denatureTubeBarcode = denatureTube.getLabel();
        reagentKit = new MiSeqReagentKit("reagent_kit_barcode");
        LabEvent denatureToReagentKitEvent = new LabEvent(DENATURE_TO_REAGENT_KIT_TRANSFER, new Date(),
                                                          "ZLAB", 1L, 1L, "sequencingTemplateFactoryTest");
        final VesselToSectionTransfer sectionTransfer = new VesselToSectionTransfer(
                denatureTube, SBSSection.getBySectionName(MiSeqReagentKit.LOADING_WELL.name()),
                reagentKit.getContainerRole(), null, denatureToReagentKitEvent);
        denatureToReagentKitEvent.getVesselToSectionTransfers().add(sectionTransfer);


        Set<LabVessel> starterVessels = Collections.singleton((LabVessel) denatureTube);
        //create a couple Miseq batches then one FCT (2500) batch
        miseqBatch1 = new LabBatch("FCT-1", starterVessels, LabBatch.LabBatchType.MISEQ, BigDecimal.valueOf(7f));
//        miseqBatch2 = new LabBatch("FCT-2", starterVessels, LabBatch.LabBatchType.MISEQ, 7f);
        fctBatch = new LabBatch(FLOWCELL_2500_TICKET, starterVessels, LabBatch.LabBatchType.FCT, BigDecimal.valueOf(
                12.33f));

        HiSeq2500FlowcellEntityBuilder flowcellEntityBuilder =
                runHiSeq2500FlowcellProcess(qtpEntityBuilder.getDenatureRack(), BARCODE_SUFFIX + "ADXX",
                                            FLOWCELL_2500_TICKET,
                                            ProductionFlowcellPath.DILUTION_TO_FLOWCELL, null,
                                            Workflow.AGILENT_EXOME_EXPRESS);
        dilutionTube = flowcellEntityBuilder.getDilutionRack().getContainerRole().getVesselAtPosition(
                VesselPosition.A01);
        dilutionTubeBarcode = dilutionTube.getLabel();

        flowcell = flowcellEntityBuilder.getIlluminaFlowcell();
        flowcellBarcode = flowcell.getLabel();

        template = null;
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
        assertThat(template.getLanes().get(0).getDerivedVesselLabel(), is(denatureTubeBarcode));
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
        assertThat(template.getLanes().get(0).getDerivedVesselLabel(), is(denatureTubeBarcode));
    }

    public void testGetSequencingTemplatePoolTest() {
        // fixme this is a bit shady.  The flowcell here is a production flowcell, not a MiSeq flowcell
        Set<VesselAndPosition> vesselsAndPositions = flowcell.getLoadingVessels();
        MatcherAssert.assertThat(vesselsAndPositions, not(Matchers.empty()));
        template = factory.getSequencingTemplate(flowcell, vesselsAndPositions, true);
        assertThat(template.getOnRigChemistry(), is("Default"));
        assertThat(template.getOnRigWorkflow(), is("Resequencing"));
        assertThat(template.getReadStructure(), is(POOL_TEST_CIGAR));
        assertThat(template.getBarcode(), equalTo(flowcellBarcode));
        assertThat(template.getLanes().size(), is(2));
        Set<String> allLanes = new HashSet<>();

        for (SequencingTemplateLaneType lane : template.getLanes()) {
            allLanes.add(lane.getLaneName());
            assertThat(lane.getDerivedVesselLabel(), equalTo(denatureTubeBarcode));
            assertThat(lane.getLoadingVesselLabel(), equalTo(dilutionTubeBarcode));
        }
        assertThat(allLanes, hasItem("LANE1"));
        assertThat(allLanes, hasItem("LANE2"));
    }

    public void testGetSequencingTemplateProduction() {
        Set<VesselAndPosition> vesselsAndPositions = flowcell.getLoadingVessels();
        MatcherAssert.assertThat(vesselsAndPositions, not(Matchers.empty()));
        template = factory.getSequencingTemplate(flowcell, vesselsAndPositions, false);
        assertThat(template.getOnRigChemistry(), is(nullValue()));
        assertThat(template.getOnRigWorkflow(), is(nullValue()));

        assertThat(template.getReadStructure(), is(PRODUCTION_CIGAR));

        assertThat(template.getBarcode(), equalTo(flowcellBarcode));
        assertThat(template.getLanes().size(), is(2));
        Set<String> allLanes = new HashSet<>();

        for (SequencingTemplateLaneType lane : template.getLanes()) {
            allLanes.add(lane.getLaneName());
            assertThat(lane.getDerivedVesselLabel(), equalTo(denatureTubeBarcode));
            assertThat(lane.getLoadingVesselLabel(), equalTo(dilutionTubeBarcode));
        }
        assertThat(allLanes, hasItem("LANE1"));
        assertThat(allLanes, hasItem("LANE2"));
    }

    public void testGetLoadingVesselsForFlowcell() {
        Set<VesselAndPosition> vesselsAndPositions = flowcell.getLoadingVessels();
        MatcherAssert.assertThat(vesselsAndPositions, not(Matchers.empty()));
        final List<VesselPosition> vesselPositions = Arrays.asList(VesselPosition.LANE1, VesselPosition.LANE2);

        for (VesselAndPosition vesselsAndPosition : vesselsAndPositions) {
            assertThat(vesselsAndPosition.getPosition(),
                       AnyOf.anyOf(equalTo(VesselPosition.LANE1), equalTo(VesselPosition.LANE2)));
            assertThat(dilutionTube, equalTo(vesselsAndPosition.getVessel()));
        }

    }

    public void testGetSequencingTemplateFromDenatureTubePoolTest() {
        template = factory.getSequencingTemplate(denatureTube, true);
        assertThat(template.getBarcode(), Matchers.nullValue());
        assertThat(template.getLanes().size(), is(1));
        assertThat(template.getOnRigChemistry(), is("Default"));
        assertThat(template.getOnRigWorkflow(), is("Resequencing"));
        assertThat(template.getReadStructure(), is(POOL_TEST_CIGAR));

        assertThat(template.getLanes().get(0).getLaneName(), is("LANE1"));
        assertThat(template.getLanes().get(0).getLoadingVesselLabel(), is(""));
        assertThat(template.getLanes().get(0).getDerivedVesselLabel(), is(denatureTubeBarcode));
        assertThat(template.getLanes().get(0).getLoadingConcentration(), is(BigDecimal.valueOf(7.0f)));
    }

    public void testGetSequencingTemplateFromDenatureTubeProduction() {
        template = factory.getSequencingTemplate(denatureTube, false);
        assertThat(template.getBarcode(), Matchers.nullValue());
        assertThat(template.getOnRigChemistry(), is(nullValue()));
        assertThat(template.getOnRigWorkflow(), is(nullValue()));
        assertThat(template.getReadStructure(), is(PRODUCTION_CIGAR));
        assertThat(template.getLanes().size(), is(2));
        Set<String> allLanes = new HashSet<>();

        for (SequencingTemplateLaneType lane : template.getLanes()) {
            allLanes.add(lane.getLaneName());
            assertThat(lane.getLoadingVesselLabel(), equalTo(""));
            assertThat(lane.getDerivedVesselLabel(), equalTo(denatureTubeBarcode));
            assertThat(lane.getLoadingConcentration(), is(BigDecimal.valueOf(12.33f)));
        }
        assertThat(allLanes, hasItem("LANE1"));
        assertThat(allLanes, hasItem("LANE2"));
    }

    public void testGetSequencingTemplateFromDilutionTubeProduction() {
        template = factory.getSequencingTemplate(dilutionTube, false);
        assertThat(template.getBarcode(), Matchers.nullValue());
        assertThat(template.getOnRigChemistry(), is(nullValue()));
        assertThat(template.getOnRigWorkflow(), is(nullValue()));
        assertThat(template.getReadStructure(), is(PRODUCTION_CIGAR));
        assertThat(template.getLanes().size(), is(2));
        Set<String> allLanes = new HashSet<>();

        for (SequencingTemplateLaneType lane : template.getLanes()) {
            allLanes.add(lane.getLaneName());
            assertThat(lane.getLoadingVesselLabel(), equalTo(""));
            assertThat(lane.getDerivedVesselLabel(), equalTo(denatureTubeBarcode));
            assertThat(lane.getLoadingConcentration(), is(BigDecimal.valueOf(12.33f)));
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
        assertThat(template.getLanes().size(), is(2));
        Set<String> allLanes = new HashSet<>();

        for (SequencingTemplateLaneType lane : template.getLanes()) {
            allLanes.add(lane.getLaneName());
            assertThat(lane.getLoadingVesselLabel(), equalTo(""));
            assertThat(lane.getDerivedVesselLabel(), equalTo(denatureTubeBarcode));
            assertThat(lane.getLoadingConcentration(), is(BigDecimal.valueOf(12.33f)));
        }
        assertThat(allLanes, hasItem("LANE1"));
        assertThat(allLanes, hasItem("LANE2"));
    }
}

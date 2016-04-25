package org.broadinstitute.gpinformatics.mercury.test.builders;

import org.broadinstitute.gpinformatics.infrastructure.test.dbfree.BettaLimsMessageTestFactory;
import org.broadinstitute.gpinformatics.mercury.bettalims.generated.ReceptacleType;
import org.broadinstitute.gpinformatics.mercury.boundary.lims.LimsQueries;
import org.broadinstitute.gpinformatics.mercury.control.labevent.LabEventFactory;
import org.broadinstitute.gpinformatics.mercury.control.labevent.LabEventHandler;
import org.broadinstitute.gpinformatics.mercury.entity.labevent.LabEvent;
import org.broadinstitute.gpinformatics.mercury.entity.reagent.ReagentDesign;
import org.broadinstitute.gpinformatics.mercury.entity.sample.SampleInstanceV2;
import org.broadinstitute.gpinformatics.mercury.entity.vessel.LabVessel;
import org.broadinstitute.gpinformatics.mercury.entity.vessel.SBSSection;
import org.broadinstitute.gpinformatics.mercury.entity.vessel.StaticPlate;
import org.broadinstitute.gpinformatics.mercury.entity.vessel.TubeFormation;
import org.broadinstitute.gpinformatics.mercury.entity.vessel.BarcodedTube;
import org.broadinstitute.gpinformatics.mercury.entity.vessel.VesselPosition;
import org.broadinstitute.gpinformatics.mercury.limsquery.generated.PlateTransferType;
import org.broadinstitute.gpinformatics.mercury.test.LabEventTest;
import org.testng.Assert;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Builds entity graph for Hybrid Selection events
 */
public class HybridSelectionEntityBuilder {
    public static final String BAIT_DESIGN_NAME = "cancer_2000gene_shift170_undercovered";

    private final BettaLimsMessageTestFactory   bettaLimsMessageTestFactory;
    private final LabEventFactory               labEventFactory;
    private final LabEventHandler               labEventHandler;
    private final TubeFormation                 pondRegRack;
    private final String pondRegRackBarcode;
    private final List<String>                  pondRegTubeBarcodes;
    private String testPrefix;

    private String normCatchRackBarcode;
    private List<String> normCatchBarcodes;
    private Map<String, BarcodedTube> mapBarcodeToNormCatchTubes = new HashMap<>();
    private TubeFormation normCatchRack;

    public HybridSelectionEntityBuilder(BettaLimsMessageTestFactory bettaLimsMessageTestFactory,
                                        LabEventFactory labEventFactory, LabEventHandler labEventHandler,
                                        TubeFormation pondRegRack, String pondRegRackBarcode,
                                        List<String> pondRegTubeBarcodes, String testPrefix) {
        this.bettaLimsMessageTestFactory = bettaLimsMessageTestFactory;
        this.labEventFactory = labEventFactory;
        this.labEventHandler = labEventHandler;
        this.pondRegRack = pondRegRack;
        this.pondRegRackBarcode = pondRegRackBarcode;
        this.pondRegTubeBarcodes = pondRegTubeBarcodes;
        this.testPrefix = testPrefix;
    }

    public List<String> getNormCatchBarcodes() {
        return normCatchBarcodes;
    }

    public String getNormCatchRackBarcode() {
        return normCatchRackBarcode;
    }

    public Map<String, BarcodedTube> getMapBarcodeToNormCatchTubes() {
        return mapBarcodeToNormCatchTubes;
    }

    public TubeFormation getNormCatchRack() {
        return normCatchRack;
    }

    public HybridSelectionEntityBuilder invoke() {
        return invoke(true);
    }

    public HybridSelectionEntityBuilder invoke(boolean doAllGSWashes) {
        HybridSelectionJaxbBuilder
                hybridSelectionJaxbBuilder = new HybridSelectionJaxbBuilder(
                bettaLimsMessageTestFactory, testPrefix, pondRegRackBarcode, pondRegTubeBarcodes, "Bait").invoke();
        normCatchRackBarcode = hybridSelectionJaxbBuilder.getNormCatchRackBarcode();
        normCatchBarcodes = hybridSelectionJaxbBuilder.getNormCatchBarcodes();

        // PreSelectionPool - rearray left half of pond rack into left half of a new rack,
        // rearray right half of pond rack into left half of a new rack, then transfer these
        // two racks into a third rack, making a 2-plex pool.
        LabEventTest.validateWorkflow("PreSelectionPool", pondRegRack); //todo jmt should be mapBarcodeToPondRegTube.values());
        Map<String, BarcodedTube> mapBarcodeToPondTube = new HashMap<>();
        for (BarcodedTube barcodedTube : pondRegRack.getContainerRole().getContainedVessels()) {
            mapBarcodeToPondTube.put(barcodedTube.getLabel(), barcodedTube);
        }

        Map<String, BarcodedTube> mapBarcodeToPreSelSource1Tube = new HashMap<>();
        for (ReceptacleType receptacleType : hybridSelectionJaxbBuilder.getPreSelPoolJaxb().getSourcePositionMap()
                .getReceptacle()) {
            mapBarcodeToPreSelSource1Tube.put(receptacleType.getBarcode(), mapBarcodeToPondTube.get(
                    receptacleType.getBarcode()));
        }

        Map<String, BarcodedTube> mapBarcodeToPreSelSource2Tube = new HashMap<>();
        for (ReceptacleType receptacleType : hybridSelectionJaxbBuilder.getPreSelPoolJaxb2().getSourcePositionMap()
                .getReceptacle()) {
            mapBarcodeToPreSelSource2Tube.put(receptacleType.getBarcode(), mapBarcodeToPondTube.get(
                    receptacleType.getBarcode()));
        }
        Map<String, LabVessel> mapBarcodeToVessel = new HashMap<>();
        mapBarcodeToVessel.putAll(mapBarcodeToPreSelSource1Tube);
        LabEvent preSelPoolEntity = labEventFactory.buildFromBettaLims(hybridSelectionJaxbBuilder.getPreSelPoolJaxb(),
                mapBarcodeToVessel);
        labEventHandler.processEvent(preSelPoolEntity);
        TubeFormation preSelPoolRack = (TubeFormation) preSelPoolEntity.getTargetLabVessels().iterator().next();

        mapBarcodeToVessel.clear();
        mapBarcodeToVessel.putAll(mapBarcodeToPreSelSource2Tube);
        mapBarcodeToVessel.put(preSelPoolRack.getLabel(), preSelPoolRack);
        for (BarcodedTube barcodedTube : preSelPoolRack.getContainerRole().getContainedVessels()) {
            mapBarcodeToVessel.put(barcodedTube.getLabel(), barcodedTube);
        }
        LabEvent preSelPoolEntity2 = labEventFactory.buildFromBettaLims(hybridSelectionJaxbBuilder.getPreSelPoolJaxb2(),
                mapBarcodeToVessel);
        labEventHandler.processEvent(preSelPoolEntity2);
        preSelPoolRack = (TubeFormation) preSelPoolEntity2.getTargetLabVessels().iterator().next();
        //asserts
        Set<SampleInstanceV2> preSelPoolSampleInstances = preSelPoolRack.getSampleInstancesV2();
        Assert.assertEquals(preSelPoolSampleInstances.size(), pondRegRack.getSampleInstancesV2().size(),
                                   "Wrong number of sample instances");
        Set<String> sampleNames = new HashSet<>();
        for (SampleInstanceV2 preSelPoolSampleInstance : preSelPoolSampleInstances) {
            if (!sampleNames.add(preSelPoolSampleInstance.getRootOrEarliestMercurySampleName())) {
                Assert.fail("Duplicate sample " + preSelPoolSampleInstance.getRootOrEarliestMercurySampleName());
            }
        }
        Set<SampleInstanceV2> sampleInstancesInPreSelPoolWell =
                preSelPoolRack.getContainerRole().getSampleInstancesAtPositionV2(VesselPosition.A01);
        Assert.assertEquals(sampleInstancesInPreSelPoolWell.size(), 2, "Wrong number of sample instances in position");

        // Hybridization
        LabEventTest.validateWorkflow("Hybridization", preSelPoolRack);
        mapBarcodeToVessel.clear();
        mapBarcodeToVessel.put(preSelPoolRack.getLabel(), preSelPoolRack);
        for (BarcodedTube barcodedTube : preSelPoolRack.getContainerRole().getContainedVessels()) {
            mapBarcodeToVessel.put(barcodedTube.getLabel(), barcodedTube);
        }

        LabEvent hybridizationEntity = labEventFactory.buildFromBettaLims(
                hybridSelectionJaxbBuilder.getHybridizationJaxb(), mapBarcodeToVessel);
        labEventHandler.processEvent(hybridizationEntity);
        StaticPlate hybridizationPlate = (StaticPlate) hybridizationEntity.getTargetLabVessels().iterator().next();

        // BaitSetup
        ReagentDesign baitDesign =
                new ReagentDesign(BAIT_DESIGN_NAME, ReagentDesign.ReagentType.BAIT);

        BarcodedTube baitTube = LabEventTest
                                            .buildBaitTube(hybridSelectionJaxbBuilder.getBaitTubeBarcode(), baitDesign);
        LabEvent baitSetupEntity = labEventFactory.buildVesselToSectionDbFree(
                hybridSelectionJaxbBuilder.getBaitSetupJaxb(), baitTube, null, SBSSection.ALL96.getSectionName());
        labEventHandler.processEvent(baitSetupEntity);
        StaticPlate baitSetupPlate = (StaticPlate) baitSetupEntity.getTargetLabVessels().iterator().next();

        // PostHybridizationThermoCyclerLoaded
        LabEventTest.validateWorkflow("PostHybridizationThermoCyclerLoaded", hybridizationPlate);
        LabEvent postHybridizationThermoCyclerLoadedEntity = labEventFactory.buildFromBettaLimsPlateEventDbFree(
                hybridSelectionJaxbBuilder.getPostHybridizationThermoCyclerLoadedJaxb(), hybridizationPlate);
        labEventHandler.processEvent(postHybridizationThermoCyclerLoadedEntity);

        // BaitAddition
        LabEventTest.validateWorkflow("BaitAddition", hybridizationPlate);
        mapBarcodeToVessel.clear();
        mapBarcodeToVessel.put(baitSetupPlate.getLabel(), baitSetupPlate);
        mapBarcodeToVessel.put(hybridizationPlate.getLabel(), hybridizationPlate);
        LabEvent baitAdditionEntity = labEventFactory.buildFromBettaLims(
                hybridSelectionJaxbBuilder.getBaitAdditionJaxb(), mapBarcodeToVessel);
        labEventHandler.processEvent(baitAdditionEntity);
        hybridizationPlate.clearCaches();

        // BeadAddition
        LabEventTest.validateWorkflow("BeadAddition", hybridizationPlate);
        LabEvent beadAdditionEntity = labEventFactory.buildFromBettaLimsPlateEventDbFree(
                hybridSelectionJaxbBuilder.getBeadAdditionJaxb(), hybridizationPlate);
        labEventHandler.processEvent(beadAdditionEntity);

        // APWash
        LabEventTest.validateWorkflow("APWash", hybridizationPlate);
        LabEvent apWashEntity = labEventFactory.buildFromBettaLimsPlateEventDbFree(
                hybridSelectionJaxbBuilder.getApWashJaxb(), hybridizationPlate);
        labEventHandler.processEvent(apWashEntity);

        // GSWash1
        LabEventTest.validateWorkflow("GSWash1", hybridizationPlate);
        LabEvent gsWash1Entity = labEventFactory.buildFromBettaLimsPlateEventDbFree(
                hybridSelectionJaxbBuilder.getGsWash1Jaxb(), hybridizationPlate);
        labEventHandler.processEvent(gsWash1Entity);

        // GSWash2
        LabEventTest.validateWorkflow("GSWash2", hybridizationPlate);
        LabEvent gsWash2Entity = labEventFactory.buildFromBettaLimsPlateEventDbFree(
                hybridSelectionJaxbBuilder.getGsWash2Jaxb(), hybridizationPlate);
        labEventHandler.processEvent(gsWash2Entity);

        if (doAllGSWashes) {
            // GSWash3
            LabEventTest.validateWorkflow("GSWash3", hybridizationPlate);
            LabEvent gsWash3Entity = labEventFactory.buildFromBettaLimsPlateEventDbFree(
                    hybridSelectionJaxbBuilder.getGsWash3Jaxb(), hybridizationPlate);
            labEventHandler.processEvent(gsWash3Entity);

            // GSWash4
            LabEventTest.validateWorkflow("GSWash4", hybridizationPlate);
            LabEvent gsWash4Entity = labEventFactory.buildFromBettaLimsPlateEventDbFree(
                    hybridSelectionJaxbBuilder.getGsWash4Jaxb(), hybridizationPlate);
            labEventHandler.processEvent(gsWash4Entity);

            // GSWash5
            LabEventTest.validateWorkflow("GSWash5", hybridizationPlate);
            LabEvent gsWash5Entity = labEventFactory.buildFromBettaLimsPlateEventDbFree(
                    hybridSelectionJaxbBuilder.getGsWash5Jaxb(), hybridizationPlate);
            labEventHandler.processEvent(gsWash5Entity);

            // GSWash6
            LabEventTest.validateWorkflow("GSWash6", hybridizationPlate);
            LabEvent gsWash6Entity = labEventFactory.buildFromBettaLimsPlateEventDbFree(
                    hybridSelectionJaxbBuilder.getGsWash6Jaxb(), hybridizationPlate);
            labEventHandler.processEvent(gsWash6Entity);
        }

        // CatchEnrichmentSetup
        LabEventTest.validateWorkflow("CatchEnrichmentSetup", hybridizationPlate);
        LabEvent catchEnrichmentSetupEntity = labEventFactory.buildFromBettaLimsPlateEventDbFree(
                hybridSelectionJaxbBuilder.getCatchEnrichmentSetupJaxb(), hybridizationPlate);
        labEventHandler.processEvent(catchEnrichmentSetupEntity);

        // PostCatchEnrichmentThermoCycler
        LabEventTest.validateWorkflow("PostCatchEnrichmentSetupThermoCyclerLoaded", hybridizationPlate);
        LabEvent postCatchEnrichmentSetupThermoCyclerEntity = labEventFactory.buildFromBettaLimsPlateEventDbFree(
                hybridSelectionJaxbBuilder.getPostCatchEnrichmentSetupThermoCyclerLoadedJaxb(), hybridizationPlate);
        labEventHandler.processEvent(postCatchEnrichmentSetupThermoCyclerEntity);

        // CatchEnrichmentCleanup
        LabEventTest.validateWorkflow("CatchEnrichmentCleanup", hybridizationPlate);
        mapBarcodeToVessel.put(hybridizationPlate.getLabel(), hybridizationPlate);
        LabEvent catchEnrichmentCleanupEntity = labEventFactory.buildFromBettaLims(
                hybridSelectionJaxbBuilder.getCatchEnrichmentCleanupJaxb(), mapBarcodeToVessel);
        labEventHandler.processEvent(catchEnrichmentCleanupEntity);
        StaticPlate catchCleanPlate =
                (StaticPlate) catchEnrichmentCleanupEntity.getTargetLabVessels().iterator().next();

        LimsQueries limsQueries = new LimsQueries(null, null, null, null);
        List<PlateTransferType> plateTransferTypes =
                limsQueries.fetchTransfersForPlate(hybridizationPlate, 2);
        Assert.assertEquals(plateTransferTypes.size(), 4, "Wrong number of plate transfers");
        int matches = 0;
        for (PlateTransferType plateTransferType : plateTransferTypes) {
            if (plateTransferType.getSourceBarcode().equals(baitSetupPlate.getLabel()) &&
                    plateTransferType.getDestinationBarcode().equals(hybridizationPlate.getLabel())) {
                Assert.assertTrue(plateTransferType.getSourcePositionMap().isEmpty());
                Assert.assertTrue(plateTransferType.getDestinationPositionMap().isEmpty());
                matches++;
            }
            if (plateTransferType.getSourceBarcode().equals(
                    preSelPoolRack.getRacksOfTubes().iterator().next().getLabel()) &&
                    plateTransferType.getDestinationBarcode().equals(hybridizationPlate.getLabel())) {
                Assert.assertEquals(plateTransferType.getSourcePositionMap().size(),
                        hybridSelectionJaxbBuilder.getPreSelPoolJaxb().getSourcePositionMap().getReceptacle().size());
                Assert.assertTrue(plateTransferType.getDestinationPositionMap().isEmpty());
                matches++;
            }
            if (plateTransferType.getSourceBarcode().equals(
                    pondRegRack.getRacksOfTubes().iterator().next().getLabel()) &&
                    plateTransferType.getDestinationBarcode().equals(
                            preSelPoolRack.getRacksOfTubes().iterator().next().getLabel())) {
                Assert.assertEquals(plateTransferType.getSourcePositionMap().size(),
                        hybridSelectionJaxbBuilder.getPreSelPoolJaxb().getSourcePositionMap().getReceptacle().size());
                Assert.assertEquals(plateTransferType.getDestinationPositionMap().size(),
                        hybridSelectionJaxbBuilder.getPreSelPoolJaxb().getSourcePositionMap().getReceptacle().size());
                matches++;
            }
        }
        Assert.assertEquals(matches, 4, "Wrong number of plate transfer matches");

        Map<String, Boolean> mapPositionToOccupied = limsQueries.fetchParentRackContentsForPlate(hybridizationPlate);
        int occupiedCount = 0;
        for (Boolean occupied : mapPositionToOccupied.values()) {
            if (occupied) {
                occupiedCount++;
            }
        }
        Assert.assertEquals(occupiedCount, pondRegTubeBarcodes.size() / 2);

        // NormalizedCatchRegistration
        LabEventTest.validateWorkflow("NormalizedCatchRegistration", catchCleanPlate);
        mapBarcodeToNormCatchTubes = new HashMap<>();
        mapBarcodeToVessel.clear();
        mapBarcodeToVessel.put(catchCleanPlate.getLabel(), catchCleanPlate);
        LabEvent normCatchEntity = labEventFactory.buildFromBettaLims(hybridSelectionJaxbBuilder.getNormCatchJaxb(),
                mapBarcodeToVessel);
        labEventHandler.processEvent(normCatchEntity);
        normCatchRack = (TubeFormation) normCatchEntity.getTargetLabVessels().iterator().next();
        for (BarcodedTube barcodedTube : normCatchRack.getContainerRole().getContainedVessels()) {
            mapBarcodeToNormCatchTubes.put(barcodedTube.getLabel(), barcodedTube);
        }
        return this;
    }
}

package org.broadinstitute.gpinformatics.mercury.test.builders;

import org.broadinstitute.gpinformatics.infrastructure.test.dbfree.BettaLimsMessageTestFactory;
import org.broadinstitute.gpinformatics.mercury.bettalims.generated.ReceptacleType;
import org.broadinstitute.gpinformatics.mercury.control.labevent.LabEventFactory;
import org.broadinstitute.gpinformatics.mercury.control.labevent.LabEventHandler;
import org.broadinstitute.gpinformatics.mercury.entity.labevent.LabEvent;
import org.broadinstitute.gpinformatics.mercury.entity.reagent.ReagentDesign;
import org.broadinstitute.gpinformatics.mercury.entity.sample.SampleInstance;
import org.broadinstitute.gpinformatics.mercury.entity.vessel.SBSSection;
import org.broadinstitute.gpinformatics.mercury.entity.vessel.StaticPlate;
import org.broadinstitute.gpinformatics.mercury.entity.vessel.TubeFormation;
import org.broadinstitute.gpinformatics.mercury.entity.vessel.TwoDBarcodedTube;
import org.broadinstitute.gpinformatics.mercury.entity.vessel.VesselPosition;
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
    private String normCatchRackBarcode;
    private List<String> normCatchBarcodes;
    private       Map<String, TwoDBarcodedTube> mapBarcodeToNormCatchTubes;
    private TubeFormation normCatchRack;
    private String testPrefix;

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

    public Map<String, TwoDBarcodedTube> getMapBarcodeToNormCatchTubes() {
        return mapBarcodeToNormCatchTubes;
    }

    public TubeFormation getNormCatchRack() {
        return normCatchRack;
    }

    public HybridSelectionEntityBuilder invoke() {
        HybridSelectionJaxbBuilder
                hybridSelectionJaxbBuilder = new HybridSelectionJaxbBuilder(
                bettaLimsMessageTestFactory, testPrefix, pondRegRackBarcode, pondRegTubeBarcodes, "Bait").invoke();
        normCatchRackBarcode = hybridSelectionJaxbBuilder.getNormCatchRackBarcode();
        normCatchBarcodes = hybridSelectionJaxbBuilder.getNormCatchBarcodes();

        // PreSelectionPool - rearray left half of pond rack into left half of a new rack,
        // rearray right half of pond rack into left half of a new rack, then transfer these
        // two racks into a third rack, making a 2-plex pool.
        LabEventTest.validateWorkflow("PreSelectionPool", pondRegRack); //todo jmt should be mapBarcodeToPondRegTube.values());
        Map<String, TwoDBarcodedTube> mapBarcodeToPreSelPoolTube = new HashMap<String, TwoDBarcodedTube>();
        Map<String, TwoDBarcodedTube> mapBarcodeToPondTube = new HashMap<String, TwoDBarcodedTube>();
        for (TwoDBarcodedTube twoDBarcodedTube : pondRegRack.getContainerRole().getContainedVessels()) {
            mapBarcodeToPondTube.put(twoDBarcodedTube.getLabel(), twoDBarcodedTube);
        }
        Map<String, TwoDBarcodedTube> mapBarcodeToPreSelSource1Tube = new HashMap<String, TwoDBarcodedTube>();
        for (ReceptacleType receptacleType : hybridSelectionJaxbBuilder.getPreSelPoolJaxb().getSourcePositionMap()
                .getReceptacle()) {
            mapBarcodeToPreSelSource1Tube.put(receptacleType.getBarcode(), mapBarcodeToPondTube.get(
                    receptacleType.getBarcode()));
        }
        Map<String, TwoDBarcodedTube> mapBarcodeToPreSelSource2Tube = new HashMap<String, TwoDBarcodedTube>();
        for (ReceptacleType receptacleType : hybridSelectionJaxbBuilder.getPreSelPoolJaxb2().getSourcePositionMap()
                .getReceptacle()) {
            mapBarcodeToPreSelSource2Tube.put(receptacleType.getBarcode(), mapBarcodeToPondTube.get(
                    receptacleType.getBarcode()));
        }
        LabEvent preSelPoolEntity = labEventFactory.buildFromBettaLimsRackToRackDbFree(hybridSelectionJaxbBuilder
                .getPreSelPoolJaxb(),
                mapBarcodeToPreSelSource1Tube, null, mapBarcodeToPreSelPoolTube, null);
        labEventHandler.processEvent(preSelPoolEntity);
        TubeFormation preSelPoolRack = (TubeFormation) preSelPoolEntity.getTargetLabVessels().iterator().next();
        LabEvent preSelPoolEntity2 = labEventFactory.buildFromBettaLimsRackToRackDbFree(
                hybridSelectionJaxbBuilder.getPreSelPoolJaxb2(), mapBarcodeToPreSelSource2Tube, null, preSelPoolRack);
        labEventHandler.processEvent(preSelPoolEntity2);
        //asserts
        Set<SampleInstance> preSelPoolSampleInstances = preSelPoolRack.getSampleInstances();
        Assert.assertEquals(preSelPoolSampleInstances.size(), pondRegRack.getSampleInstances().size(),
                                   "Wrong number of sample instances");
        Set<String> sampleNames = new HashSet<String>();
        for (SampleInstance preSelPoolSampleInstance : preSelPoolSampleInstances) {
            if (!sampleNames.add(preSelPoolSampleInstance.getStartingSample().getSampleKey())) {
                Assert.fail("Duplicate sample " + preSelPoolSampleInstance.getStartingSample().getSampleKey());
            }
        }
        Set<SampleInstance> sampleInstancesInPreSelPoolWell =
                preSelPoolRack.getContainerRole().getSampleInstancesAtPosition(VesselPosition.A01);
        Assert.assertEquals(sampleInstancesInPreSelPoolWell.size(), 2,
                "Wrong number of sample instances in position");

        // Hybridization
        LabEventTest.validateWorkflow("Hybridization", preSelPoolRack);
        LabEvent hybridizationEntity = labEventFactory.buildFromBettaLimsRackToPlateDbFree(
                hybridSelectionJaxbBuilder.getHybridizationJaxb(), preSelPoolRack, null);
        labEventHandler.processEvent(hybridizationEntity);
        StaticPlate hybridizationPlate = (StaticPlate) hybridizationEntity.getTargetLabVessels().iterator().next();

        // BaitSetup
        ReagentDesign baitDesign =
                new ReagentDesign(BAIT_DESIGN_NAME, ReagentDesign.ReagentType.BAIT);

        TwoDBarcodedTube baitTube = LabEventTest
                                            .buildBaitTube(hybridSelectionJaxbBuilder.getBaitTubeBarcode(), baitDesign);
        LabEvent baitSetupEntity = labEventFactory.buildVesselToSectionDbFree(
                hybridSelectionJaxbBuilder.getBaitSetupJaxb(), baitTube, null, SBSSection.ALL96.getSectionName());
        labEventHandler.processEvent(baitSetupEntity);
        StaticPlate baitSetupPlate = (StaticPlate) baitSetupEntity.getTargetLabVessels().iterator().next();

        // BaitAddition
        LabEventTest.validateWorkflow("BaitAddition", hybridizationPlate);
        LabEvent baitAdditionEntity = labEventFactory.buildFromBettaLimsPlateToPlateDbFree(
                hybridSelectionJaxbBuilder.getBaitAdditionJaxb(), baitSetupPlate, hybridizationPlate);
        labEventHandler.processEvent(baitAdditionEntity);

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

        // CatchEnrichmentSetup
        LabEventTest.validateWorkflow("CatchEnrichmentSetup", hybridizationPlate);
        LabEvent catchEnrichmentSetupEntity = labEventFactory.buildFromBettaLimsPlateEventDbFree(
                hybridSelectionJaxbBuilder.getCatchEnrichmentSetupJaxb(), hybridizationPlate);
        labEventHandler.processEvent(catchEnrichmentSetupEntity);

        // CatchEnrichmentCleanup
        LabEventTest.validateWorkflow("CatchEnrichmentCleanup", hybridizationPlate);
        LabEvent catchEnrichmentCleanupEntity = labEventFactory.buildFromBettaLimsPlateToPlateDbFree(
                hybridSelectionJaxbBuilder.getCatchEnrichmentCleanupJaxb(), hybridizationPlate, null);
        labEventHandler.processEvent(catchEnrichmentCleanupEntity);
        StaticPlate catchCleanPlate =
                (StaticPlate) catchEnrichmentCleanupEntity.getTargetLabVessels().iterator().next();

        // NormalizedCatchRegistration
        LabEventTest.validateWorkflow("NormalizedCatchRegistration", catchCleanPlate);
        mapBarcodeToNormCatchTubes = new HashMap<String, TwoDBarcodedTube>();
        LabEvent normCatchEntity = labEventFactory.buildFromBettaLimsPlateToRackDbFree(hybridSelectionJaxbBuilder
                .getNormCatchJaxb(),
                catchCleanPlate, mapBarcodeToNormCatchTubes, null);
        labEventHandler.processEvent(normCatchEntity);
        normCatchRack = (TubeFormation) normCatchEntity.getTargetLabVessels().iterator().next();
        return this;
    }
}

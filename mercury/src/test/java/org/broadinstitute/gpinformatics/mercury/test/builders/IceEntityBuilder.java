package org.broadinstitute.gpinformatics.mercury.test.builders;

import org.apache.commons.lang3.tuple.Triple;
import org.broadinstitute.gpinformatics.infrastructure.test.dbfree.BettaLimsMessageTestFactory;
import org.broadinstitute.gpinformatics.mercury.control.labevent.LabEventFactory;
import org.broadinstitute.gpinformatics.mercury.control.labevent.LabEventHandler;
import org.broadinstitute.gpinformatics.mercury.entity.labevent.LabEvent;
import org.broadinstitute.gpinformatics.mercury.entity.reagent.ReagentDesign;
import org.broadinstitute.gpinformatics.mercury.entity.vessel.BarcodedTube;
import org.broadinstitute.gpinformatics.mercury.entity.vessel.LabVessel;
import org.broadinstitute.gpinformatics.mercury.entity.vessel.StaticPlate;
import org.broadinstitute.gpinformatics.mercury.entity.vessel.TubeFormation;
import org.broadinstitute.gpinformatics.mercury.entity.workflow.LabBatch;
import org.broadinstitute.gpinformatics.mercury.test.LabEventTest;
import org.testng.Assert;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Builds entity graphs for the Illumina Content Exome process.
 */
public class IceEntityBuilder {

    private final BettaLimsMessageTestFactory bettaLimsMessageTestFactory;
    private final LabEventFactory labEventFactory;
    private final LabEventHandler labEventHandler;
    private final List<TubeFormation> pondRegRacks;
    private final List<String> pondRegRackBarcodes = new ArrayList<>();
    private final List<List<String>> listPondRegTubeBarcodes = new ArrayList<>();
    private final String testPrefix;
    private final IceJaxbBuilder.PlexType plexType;
    private final IceJaxbBuilder.PrepType prepType;

    private TubeFormation catchEnrichRack;
    private List<String> catchEnrichBarcodes;
    private Map<String, BarcodedTube> mapBarcodeToCatchEnrichTubes;

    public IceEntityBuilder(BettaLimsMessageTestFactory bettaLimsMessageTestFactory,
            LabEventFactory labEventFactory, LabEventHandler labEventHandler,
            List<TubeFormation> pondRegRacks, String testPrefix, IceJaxbBuilder.PlexType plexType,
            IceJaxbBuilder.PrepType prepType) {
        this.bettaLimsMessageTestFactory = bettaLimsMessageTestFactory;
        this.labEventFactory = labEventFactory;
        this.labEventHandler = labEventHandler;
        this.pondRegRacks = pondRegRacks;
        for (TubeFormation pondRegRack : pondRegRacks) {
            pondRegRackBarcodes.add(pondRegRack.getRacksOfTubes().iterator().next().getLabel());
            ArrayList<String> pondRegTubeBarcodes = new ArrayList<>();
            listPondRegTubeBarcodes.add(pondRegTubeBarcodes);
            for (BarcodedTube barcodedTube : pondRegRack.getContainerRole().getContainedVessels()) {
                pondRegTubeBarcodes.add(barcodedTube.getLabel());
            }
        }
        this.testPrefix = testPrefix;
        this.plexType = plexType;
        this.prepType = prepType;
    }

    public IceEntityBuilder invoke() {
        IceJaxbBuilder iceJaxbBuilder = new IceJaxbBuilder(bettaLimsMessageTestFactory, testPrefix, pondRegRackBarcodes,
                listPondRegTubeBarcodes, testPrefix + "IceBait1", testPrefix + "IceBait2",
                plexType,
                Arrays.asList(Triple.of("CT3", "0009763452", 1), Triple.of("Rapid Capture Kit bait", "0009773452", 2),
                        Triple.of("Rapid Capture Kit Resuspension Buffer", "0009783452", 3)),
                Arrays.asList(Triple.of("Dual Index Primers Lot", "0009764452", 4),
                        Triple.of("Rapid Capture Enrichment Amp Lot Barcode", "0009765452", 5)),
                prepType
        ).invoke();
        catchEnrichBarcodes = iceJaxbBuilder.getCatchEnrichTubeBarcodes();

        Map<String, LabVessel> mapBarcodeToVessel = new HashMap<>();

        LabEventTest.validateWorkflow("IcePoolingTransfer", pondRegRacks.get(0));
        for (TubeFormation pondRegRack : pondRegRacks) {
            mapBarcodeToVessel.put(pondRegRack.getLabel(), pondRegRack);
            for (BarcodedTube barcodedTube : pondRegRack.getContainerRole().getContainedVessels()) {
                mapBarcodeToVessel.put(barcodedTube.getLabel(), barcodedTube);
            }
        }

        LabEvent icePoolingTransfer = labEventFactory.buildFromBettaLims(iceJaxbBuilder.getIcePoolingTransfer(),
                mapBarcodeToVessel);
        labEventHandler.processEvent(icePoolingTransfer);
        TubeFormation poolRack = (TubeFormation) icePoolingTransfer.getTargetLabVessels().iterator().next();

        mapBarcodeToVessel.clear();
        mapBarcodeToVessel.put(poolRack.getLabel(), poolRack);
        for (BarcodedTube barcodedTube : poolRack.getContainerRole().getContainedVessels()) {
            mapBarcodeToVessel.put(barcodedTube.getLabel(), barcodedTube);
        }
        LabEvent iceSpriConcentration = null;
        switch (plexType) {
            case PLEX12:
                LabEventTest.validateWorkflow("IceSPRIConcentration", poolRack);
                iceSpriConcentration = labEventFactory.buildFromBettaLims(iceJaxbBuilder.getIceSPRIConcentration(),
                        mapBarcodeToVessel);
                break;
            case PLEX96:
                LabEventTest.validateWorkflow("Ice96PlexSpriConcentration", poolRack);
                iceSpriConcentration = labEventFactory.buildFromBettaLims(iceJaxbBuilder.getIce96PlexSpriConcentration(),
                        mapBarcodeToVessel);
                break;
        }
        labEventHandler.processEvent(iceSpriConcentration);
        TubeFormation spriRack = (TubeFormation) iceSpriConcentration.getTargetLabVessels().iterator().next();

        LabEventTest.validateWorkflow("IcePoolTest", spriRack);
        mapBarcodeToVessel.clear();
        mapBarcodeToVessel.put(spriRack.getLabel(), spriRack);
        for (BarcodedTube barcodedTube : spriRack.getContainerRole().getContainedVessels()) {
            mapBarcodeToVessel.put(barcodedTube.getLabel(), barcodedTube);
        }
        LabEvent icePoolTest = labEventFactory.buildFromBettaLims(iceJaxbBuilder.getIcePoolTest(), mapBarcodeToVessel);
        labEventHandler.processEvent(icePoolTest);

        StaticPlate firstHybPlate;
        if (prepType == IceJaxbBuilder.PrepType.HYPER_PREP_ICE) {
            // IceHyperPrep1stBaitPick (bait tubes to empty plate)
            ReagentDesign baitDesign = new ReagentDesign("Ice Bait 1", ReagentDesign.ReagentType.BAIT);
            BarcodedTube baitTube = LabEventTest.buildBaitTube(iceJaxbBuilder.getBaitTube1Barcode(), baitDesign);
            mapBarcodeToVessel.clear();
            mapBarcodeToVessel.put(baitTube.getLabel(), baitTube);
            LabEvent baitPickEvent = labEventFactory.buildFromBettaLims(iceJaxbBuilder.getIce1stBaitPick(),
                    mapBarcodeToVessel);
            labEventHandler.processEvent(baitPickEvent);
            StaticPlate baitPlate = (StaticPlate) baitPickEvent.getTargetLabVessels().iterator().next();

            // IceHyperPrep1stBaitAddition (bait plate to empty hyb plate)
            mapBarcodeToVessel.clear();
            mapBarcodeToVessel.put(baitPlate.getLabel(), baitPlate);
            LabEvent baitAdditionEvent = labEventFactory.buildFromBettaLims(
                    iceJaxbBuilder.getIceHyperPrep1stBaitAddition(), mapBarcodeToVessel);
            labEventHandler.processEvent(baitAdditionEvent);
            firstHybPlate = (StaticPlate) baitAdditionEvent.getTargetLabVessels().iterator().next();

            // IceHyperPrep1stHybridization (SPRI tubes merge into bait plate)
            LabEventTest.validateWorkflow("IceHyperPrep1stHybridization", spriRack);
            mapBarcodeToVessel.clear();
            mapBarcodeToVessel.put(spriRack.getLabel(), spriRack);
            mapBarcodeToVessel.put(firstHybPlate.getLabel(), firstHybPlate);
            for (BarcodedTube barcodedTube : spriRack.getContainerRole().getContainedVessels()) {
                mapBarcodeToVessel.put(barcodedTube.getLabel(), barcodedTube);
            }
            LabEvent hybridizationEvent = labEventFactory.buildFromBettaLims(iceJaxbBuilder.getIce1stHybridization(),
                    mapBarcodeToVessel);
            labEventHandler.processEvent(hybridizationEvent);
            firstHybPlate.clearCaches();
            Set<LabBatch> computedLcSets = hybridizationEvent.getComputedLcSets();
            Assert.assertFalse(computedLcSets.isEmpty());

        } else {
            mapBarcodeToVessel.clear();
            mapBarcodeToVessel.put(spriRack.getLabel(), spriRack);
            for (BarcodedTube barcodedTube : spriRack.getContainerRole().getContainedVessels()) {
                mapBarcodeToVessel.put(barcodedTube.getLabel(), barcodedTube);
            }

            LabEventTest.validateWorkflow("Ice1stHybridization", spriRack);
            LabEvent ice1stHybridization = labEventFactory.buildFromBettaLims(iceJaxbBuilder.getIce1stHybridization(),
                    mapBarcodeToVessel);
            labEventHandler.processEvent(ice1stHybridization);
            firstHybPlate = (StaticPlate) ice1stHybridization.getTargetLabVessels().iterator().next();
            Set<LabBatch> computedLcSets = ice1stHybridization.getComputedLcSets();
            Assert.assertFalse(computedLcSets.isEmpty());

            LabEventTest.validateWorkflow("Ice1stBaitAddition", firstHybPlate);
            firstHybPlate.clearCaches();
            ReagentDesign baitDesign1 = new ReagentDesign("Ice Bait 1", ReagentDesign.ReagentType.BAIT);
            BarcodedTube baitTube1 = LabEventTest.buildBaitTube(iceJaxbBuilder.getBaitTube1Barcode(), baitDesign1);
            mapBarcodeToVessel.clear();
            mapBarcodeToVessel.put(baitTube1.getLabel(), baitTube1);
            mapBarcodeToVessel.put(firstHybPlate.getLabel(), firstHybPlate);
            LabEvent ice1stBaitAddition = labEventFactory.buildFromBettaLims(iceJaxbBuilder.getIce1stBaitPick(),
                    mapBarcodeToVessel);
            labEventHandler.processEvent(ice1stBaitAddition);

            LabEventTest.validateWorkflow("PostIce1stHybridizationThermoCyclerLoaded", firstHybPlate);
            LabEvent postIce1stHybridizationThermoCyclerLoaded = labEventFactory.buildFromBettaLimsPlateEventDbFree(
                    iceJaxbBuilder.getPostIce1stHybridizationThermoCyclerLoaded(), firstHybPlate);
            labEventHandler.processEvent(postIce1stHybridizationThermoCyclerLoaded);
        }

        LabEventTest.validateWorkflow("Ice1stCapture", firstHybPlate);
        mapBarcodeToVessel.clear();
        mapBarcodeToVessel.put(firstHybPlate.getLabel(), firstHybPlate);
        LabEvent ice1stCapture = labEventFactory.buildFromBettaLims(iceJaxbBuilder.getIce1stCapture(),
                mapBarcodeToVessel);
        labEventHandler.processEvent(ice1stCapture);
        StaticPlate firstCapturePlate = (StaticPlate) ice1stCapture.getTargetLabVessels().iterator().next();

        LabEventTest.validateWorkflow("PostIce1stCaptureThermoCyclerLoaded", firstCapturePlate);
        LabEvent postIce1stCaptureThermoCyclerLoaded = labEventFactory.buildFromBettaLimsPlateEventDbFree(
                iceJaxbBuilder.getPostIce1stCaptureThermoCyclerLoaded(), firstCapturePlate);
        labEventHandler.processEvent(postIce1stCaptureThermoCyclerLoaded);

        if (prepType == IceJaxbBuilder.PrepType.HYPER_PREP_ICE) {
            // IceHyperPrep2ndBaitPick (bait tubes to empty plate)
            ReagentDesign baitDesign = new ReagentDesign("Ice Bait 2", ReagentDesign.ReagentType.BAIT);
            BarcodedTube baitTube = LabEventTest.buildBaitTube(iceJaxbBuilder.getBaitTube2Barcode(), baitDesign);
            mapBarcodeToVessel.clear();
            mapBarcodeToVessel.put(baitTube.getLabel(), baitTube);
            LabEvent baitPickEvent = labEventFactory.buildFromBettaLims(iceJaxbBuilder.getIce2ndBaitPick(),
                    mapBarcodeToVessel);
            labEventHandler.processEvent(baitPickEvent);
            StaticPlate baitPlate = (StaticPlate) baitPickEvent.getTargetLabVessels().iterator().next();

            LabEventTest.validateWorkflow("Ice2ndHybridization", firstCapturePlate);
            LabEvent ice2ndHybridization = labEventFactory.buildFromBettaLimsPlateEventDbFree(
                    iceJaxbBuilder.getIce2ndHybridization(), firstCapturePlate);
            labEventHandler.processEvent(ice2ndHybridization);

            // IceHyperPrep2ndBaitAddition (bait plate merges into 2nd hyb plate)
            LabEventTest.validateWorkflow("IceHyperPrep2ndBaitAddition", firstCapturePlate);
            mapBarcodeToVessel.clear();
            mapBarcodeToVessel.put(baitPlate.getLabel(), baitPlate);
            mapBarcodeToVessel.put(firstCapturePlate.getLabel(), firstCapturePlate);
            LabEvent baitAdditionEvent = labEventFactory.buildFromBettaLims(
                    iceJaxbBuilder.getIceHyperPrep2ndBaitAddition(), mapBarcodeToVessel);
            labEventHandler.processEvent(baitAdditionEvent);
            firstCapturePlate.clearCaches();

        } else {
            LabEventTest.validateWorkflow("Ice2ndHybridization", firstCapturePlate);
            LabEvent ice2ndHybridization = labEventFactory.buildFromBettaLimsPlateEventDbFree(
                    iceJaxbBuilder.getIce2ndHybridization(), firstCapturePlate);
            labEventHandler.processEvent(ice2ndHybridization);

            LabEventTest.validateWorkflow("Ice2ndBaitAddition", firstCapturePlate);
            firstCapturePlate.clearCaches();
            ReagentDesign baitDesign2 = new ReagentDesign("Ice Bait 2", ReagentDesign.ReagentType.BAIT);
            BarcodedTube baitTube2 = LabEventTest.buildBaitTube(iceJaxbBuilder.getBaitTube2Barcode(), baitDesign2);
            mapBarcodeToVessel.clear();
            mapBarcodeToVessel.put(baitTube2.getLabel(), baitTube2);
            mapBarcodeToVessel.put(firstCapturePlate.getLabel(), firstCapturePlate);
            LabEvent ice2ndBaitAddition = labEventFactory.buildFromBettaLims(iceJaxbBuilder.getIce2ndBaitPick(),
                    mapBarcodeToVessel);
            labEventHandler.processEvent(ice2ndBaitAddition);

            LabEventTest.validateWorkflow("PostIce2ndHybridizationThermoCyclerLoaded", firstCapturePlate);
            LabEvent postIce2ndHybridizationThermoCyclerLoaded = labEventFactory.buildFromBettaLimsPlateEventDbFree(
                    iceJaxbBuilder.getPostIce2ndHybridizationThermoCyclerLoaded(), firstCapturePlate);
            labEventHandler.processEvent(postIce2ndHybridizationThermoCyclerLoaded);
        }

        LabEventTest.validateWorkflow("Ice2ndCapture", firstCapturePlate);
        mapBarcodeToVessel.clear();
        mapBarcodeToVessel.put(firstCapturePlate.getLabel(), firstCapturePlate);
        LabEvent ice2ndCapture = labEventFactory.buildFromBettaLims(iceJaxbBuilder.getIce2ndCapture(),
                mapBarcodeToVessel);
        labEventHandler.processEvent(ice2ndCapture);
        StaticPlate secondCapturePlate = (StaticPlate) ice2ndCapture.getTargetLabVessels().iterator().next();

        LabEventTest.validateWorkflow("PostIce2ndCaptureThermoCyclerLoaded", secondCapturePlate);
        LabEvent postIce2ndCaptureThermoCyclerLoaded = labEventFactory.buildFromBettaLimsPlateEventDbFree(
                iceJaxbBuilder.getPostIce2ndCaptureThermoCyclerLoaded(), secondCapturePlate);
        labEventHandler.processEvent(postIce2ndCaptureThermoCyclerLoaded);

        LabEventTest.validateWorkflow("IceCatchCleanup", secondCapturePlate);
        mapBarcodeToVessel.clear();
        mapBarcodeToVessel.put(secondCapturePlate.getLabel(), secondCapturePlate);
        LabEvent iceCatchCleanup = labEventFactory.buildFromBettaLims(iceJaxbBuilder.getIceCatchCleanup(),
                mapBarcodeToVessel);
        labEventHandler.processEvent(iceCatchCleanup);
        StaticPlate catchCleanupPlate = (StaticPlate) iceCatchCleanup.getTargetLabVessels().iterator().next();

        LabEventTest.validateWorkflow("IceCatchEnrichmentSetup", catchCleanupPlate);
        LabEvent iceCatchEnrichmentSetup = labEventFactory.buildFromBettaLimsPlateEventDbFree(
                iceJaxbBuilder.getIceCatchEnrichmentSetup(), catchCleanupPlate);
        labEventHandler.processEvent(iceCatchEnrichmentSetup);

        LabEventTest.validateWorkflow("PostIceCatchEnrichmentSetupThermoCyclerLoaded", catchCleanupPlate);
        LabEvent postIceCatchEnrichmentSetupThermoCyclerLoaded = labEventFactory.buildFromBettaLimsPlateEventDbFree(
                iceJaxbBuilder.getPostIceCatchEnrichmentSetupThermoCyclerLoaded(), catchCleanupPlate);
        labEventHandler.processEvent(postIceCatchEnrichmentSetupThermoCyclerLoaded);

        LabEventTest.validateWorkflow("IceCatchEnrichmentCleanup", catchCleanupPlate);
        mapBarcodeToVessel.clear();
        mapBarcodeToVessel.put(catchCleanupPlate.getLabel(), catchCleanupPlate);
        LabEvent iceCatchEnrichmentCleanup = labEventFactory.buildFromBettaLims(
                iceJaxbBuilder.getIceCatchEnrichmentCleanup(), mapBarcodeToVessel);
        labEventHandler.processEvent(iceCatchEnrichmentCleanup);
        catchEnrichRack = (TubeFormation) iceCatchEnrichmentCleanup.getTargetLabVessels().iterator().next();

        return this;
    }

    public TubeFormation getCatchEnrichRack() {
        return catchEnrichRack;
    }

    public List<String> getCatchEnrichBarcodes() {
        return catchEnrichBarcodes;
    }

    public Map<String, BarcodedTube> getMapBarcodeToCatchEnrichTubes() {
        if (mapBarcodeToCatchEnrichTubes == null) {
            mapBarcodeToCatchEnrichTubes = new HashMap<>();
            for (BarcodedTube barcodedTube : catchEnrichRack.getContainerRole().getContainedVessels()) {
                mapBarcodeToCatchEnrichTubes.put(barcodedTube.getLabel(), barcodedTube);
            }
        }
        return mapBarcodeToCatchEnrichTubes;
    }
}

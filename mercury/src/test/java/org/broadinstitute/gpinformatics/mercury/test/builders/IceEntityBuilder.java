package org.broadinstitute.gpinformatics.mercury.test.builders;

import org.broadinstitute.gpinformatics.infrastructure.test.dbfree.BettaLimsMessageTestFactory;
import org.broadinstitute.gpinformatics.mercury.control.labevent.LabEventFactory;
import org.broadinstitute.gpinformatics.mercury.control.labevent.LabEventHandler;
import org.broadinstitute.gpinformatics.mercury.entity.labevent.LabEvent;
import org.broadinstitute.gpinformatics.mercury.entity.reagent.ReagentDesign;
import org.broadinstitute.gpinformatics.mercury.entity.vessel.LabVessel;
import org.broadinstitute.gpinformatics.mercury.entity.vessel.StaticPlate;
import org.broadinstitute.gpinformatics.mercury.entity.vessel.TubeFormation;
import org.broadinstitute.gpinformatics.mercury.entity.vessel.BarcodedTube;
import org.broadinstitute.gpinformatics.mercury.entity.workflow.LabBatch;
import org.broadinstitute.gpinformatics.mercury.test.LabEventTest;
import org.testng.Assert;

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
    private final TubeFormation pondRegRack;
    private final String pondRegRackBarcode;
    private final List<String> pondRegTubeBarcodes;
    private final String testPrefix;
    private final IceJaxbBuilder.PlexType plexType;

    private TubeFormation catchEnrichRack;
    private List<String> catchEnrichBarcodes;
    private Map<String, BarcodedTube> mapBarcodeToCatchEnrichTubes;

    public IceEntityBuilder(BettaLimsMessageTestFactory bettaLimsMessageTestFactory,
            LabEventFactory labEventFactory, LabEventHandler labEventHandler,
            TubeFormation pondRegRack, String pondRegRackBarcode,
            List<String> pondRegTubeBarcodes, String testPrefix, IceJaxbBuilder.PlexType plexType) {
        this.bettaLimsMessageTestFactory = bettaLimsMessageTestFactory;
        this.labEventFactory = labEventFactory;
        this.labEventHandler = labEventHandler;
        this.pondRegRack = pondRegRack;
        this.pondRegRackBarcode = pondRegRackBarcode;
        this.pondRegTubeBarcodes = pondRegTubeBarcodes;
        this.testPrefix = testPrefix;
        this.plexType = plexType;
    }

    public IceEntityBuilder invoke() {
        IceJaxbBuilder iceJaxbBuilder = new IceJaxbBuilder(bettaLimsMessageTestFactory, testPrefix, pondRegRackBarcode,
                pondRegTubeBarcodes, testPrefix + "IceBait1", testPrefix + "IceBait2",
                LibraryConstructionJaxbBuilder.TargetSystem.SQUID_VIA_MERCURY, plexType).invoke();
        catchEnrichBarcodes = iceJaxbBuilder.getCatchEnrichTubeBarcodes();

        Map<String, LabVessel> mapBarcodeToVessel = new HashMap<>();

        LabEventTest.validateWorkflow("IcePoolingTransfer", pondRegRack);
        mapBarcodeToVessel.put(pondRegRack.getLabel(), pondRegRack);
        for (BarcodedTube barcodedTube : pondRegRack.getContainerRole().getContainedVessels()) {
            mapBarcodeToVessel.put(barcodedTube.getLabel(), barcodedTube);
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

        LabEventTest.validateWorkflow("Ice1stHybridization", spriRack);
        mapBarcodeToVessel.clear();
        mapBarcodeToVessel.put(spriRack.getLabel(), spriRack);
        for (BarcodedTube barcodedTube : spriRack.getContainerRole().getContainedVessels()) {
            mapBarcodeToVessel.put(barcodedTube.getLabel(), barcodedTube);
        }
        LabEvent ice1stHybridization = labEventFactory.buildFromBettaLims(iceJaxbBuilder.getIce1stHybridization(),
                mapBarcodeToVessel);
        labEventHandler.processEvent(ice1stHybridization);
        StaticPlate firstHybPlate = (StaticPlate) ice1stHybridization.getTargetLabVessels().iterator().next();
        Set<LabBatch> computedLcSets = ice1stHybridization.getComputedLcSets();
        Assert.assertFalse(computedLcSets.isEmpty());

        LabEventTest.validateWorkflow("Ice1stBaitAddition", firstHybPlate);
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

        LabEventTest.validateWorkflow("Ice2ndHybridization", firstCapturePlate);
        LabEvent ice2ndHybridization = labEventFactory.buildFromBettaLimsPlateEventDbFree(
                iceJaxbBuilder.getIce2ndHybridization(), firstCapturePlate);
        labEventHandler.processEvent(ice2ndHybridization);

        LabEventTest.validateWorkflow("Ice2ndBaitAddition", firstCapturePlate);
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

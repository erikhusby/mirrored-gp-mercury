package org.broadinstitute.gpinformatics.mercury.test.builders;

import org.broadinstitute.gpinformatics.infrastructure.test.dbfree.BettaLimsMessageTestFactory;
import org.broadinstitute.gpinformatics.mercury.control.labevent.LabEventFactory;
import org.broadinstitute.gpinformatics.mercury.control.labevent.LabEventHandler;
import org.broadinstitute.gpinformatics.mercury.entity.labevent.LabEvent;
import org.broadinstitute.gpinformatics.mercury.entity.reagent.ReagentDesign;
import org.broadinstitute.gpinformatics.mercury.entity.vessel.LabVessel;
import org.broadinstitute.gpinformatics.mercury.entity.vessel.SBSSection;
import org.broadinstitute.gpinformatics.mercury.entity.vessel.StaticPlate;
import org.broadinstitute.gpinformatics.mercury.entity.vessel.TubeFormation;
import org.broadinstitute.gpinformatics.mercury.entity.vessel.BarcodedTube;
import org.broadinstitute.gpinformatics.mercury.test.LabEventTest;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

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
    private String testPrefix;
    private TubeFormation catchEnrichRack;
    private List<String> catchEnrichBarcodes;
    private Map<String, BarcodedTube> mapBarcodeToCatchEnrichTubes;

    public IceEntityBuilder(BettaLimsMessageTestFactory bettaLimsMessageTestFactory,
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

    public IceEntityBuilder invoke() {
        IceJaxbBuilder iceJaxbBuilder = new IceJaxbBuilder(bettaLimsMessageTestFactory, testPrefix, pondRegRackBarcode,
                pondRegTubeBarcodes, testPrefix + "IceBait1", testPrefix + "IceBait2",
                LibraryConstructionJaxbBuilder.TargetSystem.SQUID_VIA_MERCURY).invoke();
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

        LabEventTest.validateWorkflow("IceSPRIConcentration", poolRack);
        mapBarcodeToVessel.clear();
        mapBarcodeToVessel.put(poolRack.getLabel(), poolRack);
        for (BarcodedTube barcodedTube : poolRack.getContainerRole().getContainedVessels()) {
            mapBarcodeToVessel.put(barcodedTube.getLabel(), barcodedTube);
        }
        LabEvent iceSpriConcentration = labEventFactory.buildFromBettaLims(iceJaxbBuilder.getIceSPRIConcentration(),
                mapBarcodeToVessel);
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

        LabEventTest.validateWorkflow("Ice1stBaitAddition", firstHybPlate);
        ReagentDesign baitDesign1 = new ReagentDesign("Ice Bait 1", ReagentDesign.ReagentType.BAIT);
        BarcodedTube baitTube1 = LabEventTest.buildBaitTube(iceJaxbBuilder.getBaitTube1Barcode(), baitDesign1);
        LabEvent ice1stBaitAddition = labEventFactory.buildVesselToSectionDbFree(
                iceJaxbBuilder.getIce1stBaitAddition(), baitTube1, firstHybPlate, SBSSection.ALL96.getSectionName());
        labEventHandler.processEvent(ice1stBaitAddition);

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
        LabEvent ice2ndBaitAddition = labEventFactory.buildVesselToSectionDbFree(
                iceJaxbBuilder.getIce2ndBaitAddition(), baitTube2, firstCapturePlate, SBSSection.ALL96.getSectionName());
        labEventHandler.processEvent(ice2ndBaitAddition);

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

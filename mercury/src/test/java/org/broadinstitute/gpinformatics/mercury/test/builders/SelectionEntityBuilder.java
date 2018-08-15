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
import org.broadinstitute.gpinformatics.mercury.test.LabEventTest;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Builds entity graphs for the Custom Selection process
 */
public class SelectionEntityBuilder {

    private final BettaLimsMessageTestFactory bettaLimsMessageTestFactory;
    private final LabEventFactory labEventFactory;
    private final LabEventHandler labEventHandler;
    private final List<TubeFormation> pondRegRacks;
    private final List<String> pondRegRackBarcodes = new ArrayList<>();
    private final List<List<String>> listPondRegTubeBarcodes = new ArrayList<>();
    private final String testPrefix;
    private LabEvent selectionConcentration;
    private StaticPlate concentrationPlate;
    private StaticPlate hybPlate;
    private StaticPlate masterMixPlate;
    private TubeFormation catchRack;
    private List<String> catchTubeBarcodes;
    private Map<String, BarcodedTube> mapBarcodeToCatchTubes;

    public SelectionEntityBuilder(BettaLimsMessageTestFactory bettaLimsMessageTestFactory,
                                  LabEventFactory labEventFactory, LabEventHandler labEventHandler,
                                  List<TubeFormation> pondRegRacks, String testPrefix) {
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
    }

    public SelectionEntityBuilder invoke() {

        SelectionJaxbBuilder jaxbBuilder = new SelectionJaxbBuilder(bettaLimsMessageTestFactory, testPrefix, pondRegRackBarcodes,
                listPondRegTubeBarcodes, testPrefix + "SelectionBait1"
        ).invoke();
        catchTubeBarcodes = jaxbBuilder.getCatchTubeBarcodes();

        Map<String, LabVessel> mapBarcodeToVessel = new HashMap<>();

        LabEventTest.validateWorkflow("SelectionPoolingTransfer", pondRegRacks.get(0));
        for (TubeFormation pondRegRack : pondRegRacks) {
            mapBarcodeToVessel.put(pondRegRack.getLabel(), pondRegRack);
            for (BarcodedTube barcodedTube : pondRegRack.getContainerRole().getContainedVessels()) {
                mapBarcodeToVessel.put(barcodedTube.getLabel(), barcodedTube);
            }
        }

        LabEvent selectionPoolingTransfer = labEventFactory.buildFromBettaLims(jaxbBuilder.getSelectionPoolingTransfer(),
                mapBarcodeToVessel);
        labEventHandler.processEvent(selectionPoolingTransfer);
        StaticPlate poolPlate = (StaticPlate) selectionPoolingTransfer.getTargetLabVessels().iterator().next();

        mapBarcodeToVessel.clear();
        mapBarcodeToVessel.put(poolPlate.getLabel(), poolPlate);

        LabEventTest.validateWorkflow("SelectionConcentrationTransfer", poolPlate);
        selectionConcentration = labEventFactory.buildFromBettaLims(jaxbBuilder.getConcentrationJaxb(),
                mapBarcodeToVessel);
        labEventHandler.processEvent(selectionConcentration);
        concentrationPlate = (StaticPlate) selectionConcentration.getTargetLabVessels().iterator().next();

        LabEventTest.validateWorkflow("SelectionHybSetup", concentrationPlate);
        mapBarcodeToVessel.clear();
        mapBarcodeToVessel.put(concentrationPlate.getLabel(), concentrationPlate);
        LabEvent hybridizationEvent = labEventFactory.buildFromBettaLims(jaxbBuilder.getSelectionHybSetup(),
                mapBarcodeToVessel);
        labEventHandler.processEvent(hybridizationEvent);
        hybPlate = (StaticPlate) hybridizationEvent.getTargetLabVessels().iterator().next();

        // Bait Cherry Pick to new Master Mix Plate
        ReagentDesign baitDesign = new ReagentDesign("Twist Bait 1", ReagentDesign.ReagentType.BAIT);
        BarcodedTube baitTube = LabEventTest.buildBaitTube(jaxbBuilder.getBaitTubeBarcode(), baitDesign);
        mapBarcodeToVessel.clear();
        mapBarcodeToVessel.put(baitTube.getLabel(), baitTube);

        LabEvent baitPick = labEventFactory.buildFromBettaLims(jaxbBuilder.getBaitPickJaxb(),
                mapBarcodeToVessel);
        labEventHandler.processEvent(baitPick);
        masterMixPlate = (StaticPlate) baitPick.getTargetLabVessels().iterator().next();

        LabEventTest.validateWorkflow("SelectionBaitAddition", hybPlate);

        mapBarcodeToVessel.clear();
        mapBarcodeToVessel.put(masterMixPlate.getLabel(), masterMixPlate);
        mapBarcodeToVessel.put(hybPlate.getLabel(), hybPlate);

        LabEvent baitAddition = labEventFactory.buildFromBettaLims(jaxbBuilder.getBaitAdditionJaxb(),
                mapBarcodeToVessel);
        labEventHandler.processEvent(baitAddition);

        LabEventTest.validateWorkflow("SelectionBeadBinding", hybPlate);
        LabEvent beadBinding = labEventFactory.buildFromBettaLimsPlateEventDbFree(
                jaxbBuilder.getBeadBindingJaxb(), hybPlate);
        labEventHandler.processEvent(beadBinding);

        LabEventTest.validateWorkflow("SelectionCapture", hybPlate);
        LabEvent captureEvent = labEventFactory.buildFromBettaLimsPlateEventDbFree(
                jaxbBuilder.getCaptureJaxb(), hybPlate);
        labEventHandler.processEvent(captureEvent);

        LabEventTest.validateWorkflow("SelectionCatchPCR", hybPlate);
        LabEvent catchPcrEvent = labEventFactory.buildFromBettaLimsPlateEventDbFree(
                jaxbBuilder.getCatchPcrJaxb(), hybPlate);
        labEventHandler.processEvent(catchPcrEvent);

        LabEventTest.validateWorkflow("SelectionCatchRegistration", hybPlate);
        LabEvent catchCleanupEvent = labEventFactory.buildFromBettaLims(
                jaxbBuilder.getCatchCleanup(), mapBarcodeToVessel);
        labEventHandler.processEvent(catchCleanupEvent);
        catchRack = (TubeFormation) catchCleanupEvent.getTargetLabVessels().iterator().next();

        return this;
    }

    public TubeFormation getCatchRack() {
        return catchRack;
    }

    public List<String> getCatchTubeBarcodes() {
        return catchTubeBarcodes;
    }

    public Map<String, BarcodedTube> getMapBarcodeToCatchTubes() {
        if (mapBarcodeToCatchTubes == null) {
            mapBarcodeToCatchTubes = new HashMap<>();
            for (BarcodedTube barcodedTube : catchRack.getContainerRole().getContainedVessels()) {
                mapBarcodeToCatchTubes.put(barcodedTube.getLabel(), barcodedTube);
            }
        }
        return mapBarcodeToCatchTubes;
    }
}

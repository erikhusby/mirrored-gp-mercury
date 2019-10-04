package org.broadinstitute.gpinformatics.mercury.test.builders;

import org.broadinstitute.gpinformatics.infrastructure.test.dbfree.BettaLimsMessageTestFactory;
import org.broadinstitute.gpinformatics.mercury.control.labevent.LabEventFactory;
import org.broadinstitute.gpinformatics.mercury.control.labevent.LabEventHandler;
import org.broadinstitute.gpinformatics.mercury.entity.labevent.LabEvent;
import org.broadinstitute.gpinformatics.mercury.entity.reagent.MolecularIndexingScheme;
import org.broadinstitute.gpinformatics.mercury.entity.vessel.BarcodedTube;
import org.broadinstitute.gpinformatics.mercury.entity.vessel.LabVessel;
import org.broadinstitute.gpinformatics.mercury.entity.vessel.StaticPlate;
import org.broadinstitute.gpinformatics.mercury.entity.vessel.TubeFormation;
import org.broadinstitute.gpinformatics.mercury.test.LabEventTest;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class SingleCellHashingEntityBuilder {
    private final Map<String, BarcodedTube> mapBarcodeToTube;
    private final TubeFormation rack;
    private final BettaLimsMessageTestFactory bettaLimsMessageTestFactory;
    private final LabEventFactory labEventFactory;
    private final LabEventHandler labEventHandler;
    private final String testPrefix;
    private final String rackBarcode;
    private StaticPlate spriPlate;
    private StaticPlate pcrPlate;
    private TubeFormation cleanupRack;
    private TubeFormation spriRack2TubeFormation;

    public SingleCellHashingEntityBuilder(Map<String, BarcodedTube> mapBarcodeToTube, TubeFormation rack,
                                          BettaLimsMessageTestFactory bettaLimsMessageTestFactory,
                                          LabEventFactory labEventFactory, LabEventHandler labEventHandler,
                                          String rackBarcode, String testPrefix) {
        this.mapBarcodeToTube = mapBarcodeToTube;
        this.rack = rack;
        this.bettaLimsMessageTestFactory = bettaLimsMessageTestFactory;
        this.labEventFactory = labEventFactory;
        this.labEventHandler = labEventHandler;
        this.rackBarcode = rackBarcode;
        this.testPrefix = testPrefix;
    }

    public SingleCellHashingEntityBuilder invoke() {
        final String indexPlateBarcode = "SC10XHashIndexPlate" + testPrefix;
        List<StaticPlate> indexPlatesList = LabEventTest.buildIndexPlate(null, null,
                new ArrayList<MolecularIndexingScheme.IndexPosition>() {{
                    add(MolecularIndexingScheme.IndexPosition.ILLUMINA_P7);
                }},
                new ArrayList<String>() {{
                    add(indexPlateBarcode);
                }}
        );

        SingleCellHashingJaxbBuilder jaxbBuilder = new SingleCellHashingJaxbBuilder(bettaLimsMessageTestFactory, testPrefix,
                rackBarcode, new ArrayList<>(mapBarcodeToTube.keySet()), indexPlateBarcode).invoke();

        Map<String, LabVessel> mapBarcodeToVessel = new HashMap<>();
        mapBarcodeToVessel.put(rack.getLabel(), rack);
        for (BarcodedTube barcodedTube : rack.getContainerRole().getContainedVessels()) {
            mapBarcodeToVessel.put(barcodedTube.getLabel(), barcodedTube);
        }
        LabEventTest.validateWorkflow("SingleCellHashingSPRI1", mapBarcodeToTube.values());
        LabEvent spirAdditionEvent = labEventFactory.buildFromBettaLims(
                jaxbBuilder.getSpri1Jaxb(), mapBarcodeToVessel);
        labEventHandler.processEvent(spirAdditionEvent);

        spriPlate = (StaticPlate) spirAdditionEvent.getTargetLabVessels().iterator().next();

        LabEventTest.validateWorkflow("SingleCellHashingSPRI2", spriPlate);
        mapBarcodeToVessel.clear();
        mapBarcodeToVessel.put(spriPlate.getLabel(), spriPlate);

        LabEvent spri2Entity = labEventFactory.buildFromBettaLims(
                jaxbBuilder.getSpri2Jaxb(), mapBarcodeToVessel);
        labEventHandler.processEvent(spri2Entity);

        spriRack2TubeFormation = (TubeFormation) spri2Entity.getTargetLabVessels().iterator().next();

        mapBarcodeToVessel.clear();
        mapBarcodeToVessel.put(indexPlatesList.get(0).getLabel(), indexPlatesList.get(0));
        mapBarcodeToVessel.put(spriRack2TubeFormation.getLabel(), spriRack2TubeFormation);
        for (BarcodedTube barcodedTube : spriRack2TubeFormation.getContainerRole().getContainedVessels()) {
            mapBarcodeToVessel.put(barcodedTube.getLabel(), barcodedTube);
        }
        LabEventTest.validateWorkflow("SingleCellHashingIndexAddition", spriRack2TubeFormation);
        LabEvent indexAdapterPCREntity = labEventFactory.buildFromBettaLims(
                jaxbBuilder.getIndexAdapterPCRJaxb(), mapBarcodeToVessel);
        labEventHandler.processEvent(indexAdapterPCREntity);
        spriRack2TubeFormation.clearCaches();

        LabEventTest.validateWorkflow("SingleCellHashingPCR", spriRack2TubeFormation);
        LabEvent pcrEntity = labEventFactory.buildFromBettaLims(
                jaxbBuilder.getPcrJaxb(), mapBarcodeToVessel);
        labEventHandler.processEvent(pcrEntity);
        pcrPlate = (StaticPlate) pcrEntity.getTargetLabVessels().iterator().next();

        LabEventTest.validateWorkflow("SingleCellHashingPCRCleanup", pcrPlate);
        mapBarcodeToVessel.clear();
        mapBarcodeToVessel.put(pcrPlate.getLabel(), pcrPlate);
        LabEvent doubleSidedSpriEntity = labEventFactory.buildFromBettaLims(
                jaxbBuilder.getCleanupJaxb(), mapBarcodeToVessel);
        labEventHandler.processEvent(doubleSidedSpriEntity);
        cleanupRack = (TubeFormation) doubleSidedSpriEntity.getTargetLabVessels().iterator().next();

        return this;
    }

    public TubeFormation getCleanupRack() {
        return cleanupRack;
    }
}

package org.broadinstitute.gpinformatics.mercury.test.builders;

import org.broadinstitute.gpinformatics.infrastructure.test.dbfree.BettaLimsMessageTestFactory;
import org.broadinstitute.gpinformatics.mercury.control.labevent.LabEventFactory;
import org.broadinstitute.gpinformatics.mercury.control.labevent.LabEventHandler;
import org.broadinstitute.gpinformatics.mercury.entity.labevent.LabEvent;
import org.broadinstitute.gpinformatics.mercury.entity.reagent.MolecularIndexingScheme;
import org.broadinstitute.gpinformatics.mercury.entity.vessel.LabVessel;
import org.broadinstitute.gpinformatics.mercury.entity.vessel.StaticPlate;
import org.broadinstitute.gpinformatics.mercury.entity.vessel.TubeFormation;
import org.broadinstitute.gpinformatics.mercury.test.LabEventTest;
import org.testng.Assert;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Builds entity graphs for the Single Cell Smart Seq process.
 */
public class SingleCellSmartSeqEntityBuilder {

    private final BettaLimsMessageTestFactory bettaLimsMessageTestFactory;
    private final LabEventFactory labEventFactory;
    private final LabEventHandler labEventHandler;
    private final List<StaticPlate> sourceplates;
    private final String testPrefix;
    private FPJaxbBuilder fpJaxbBuilder;
    private StaticPlate pcr1Plate;
    private StaticPlate indexPlate;
    private StaticPlate pcr2Plate;
    private StaticPlate preSPRIPoolingPlate;
    private TubeFormation finalPoolingRack;
    private int numSamples;
    private List<StaticPlate> elutionPlates = new ArrayList<>();
    private StaticPlate tagmentationPlate;
    private TubeFormation poolingRack;
    private TubeFormation bulkSpriRack;

    public SingleCellSmartSeqEntityBuilder(
            BettaLimsMessageTestFactory bettaLimsMessageTestFactory, LabEventFactory labEventFactory,
            LabEventHandler labEventHandler, List<StaticPlate> sourceplates, int numSamples, String testPrefix) {
        this.bettaLimsMessageTestFactory = bettaLimsMessageTestFactory;
        this.labEventFactory = labEventFactory;
        this.labEventHandler = labEventHandler;
        this.sourceplates = sourceplates;
        this.numSamples = numSamples;
        this.testPrefix = testPrefix;
    }

    public SingleCellSmartSeqEntityBuilder invoke() {

        List<String> indexPlateBarcodes = new ArrayList<>();
        List<StaticPlate> indexPlates = new ArrayList<>();
        for (int sourcePlateNum = 1; sourcePlateNum <= sourceplates.size(); sourcePlateNum++) {
            final String indexPlateBarcode = "SCIndexPlate_" + sourcePlateNum + "_" + testPrefix;
            List<StaticPlate> indexPlatesList = LabEventTest.buildIndexPlate(null, null,
                    new ArrayList<MolecularIndexingScheme.IndexPosition>() {{
                        add(MolecularIndexingScheme.IndexPosition.ILLUMINA_P7);
                        add(MolecularIndexingScheme.IndexPosition.ILLUMINA_P5);
                    }},
                    new ArrayList<String>() {{
                        add(indexPlateBarcode);
                        add(indexPlateBarcode);
                    }}
            );
            indexPlates.add(indexPlatesList.get(0));
            indexPlateBarcodes.add(indexPlateBarcode);
        }

        Map<String, LabVessel> mapBarcodeToVessel = new HashMap<>();
        List<String> sourcePlateBarcodes = new ArrayList<>();
        for (StaticPlate sourceplate: sourceplates) {
            sourceplate.clearCaches();
            mapBarcodeToVessel.put(sourceplate.getLabel(), sourceplate);
            sourcePlateBarcodes.add(sourceplate.getLabel());
        }

        SingleCellSmartSeqJaxbBuilder
                jaxbBuilder = new SingleCellSmartSeqJaxbBuilder(bettaLimsMessageTestFactory, testPrefix,
                sourcePlateBarcodes, indexPlateBarcodes).invoke();

        int picoCounter = 0;
        for (int i = 0; i < sourcePlateBarcodes.size(); i++) {
            StaticPlate sourcePlate = sourceplates.get(i);

            LabEventTest.validateWorkflow("SingleCellSPRIAddition", sourcePlate);
            LabEvent spriAddition = labEventFactory.buildFromBettaLimsPlateEventDbFree(
                    jaxbBuilder.getSingleCellSpriAdditions().get(i), sourcePlate);
            labEventHandler.processEvent(spriAddition);

            LabEventTest.validateWorkflow("SingleCellPolyA", sourcePlate);
            LabEvent polyASelection = labEventFactory.buildFromBettaLimsPlateEventDbFree(
                    jaxbBuilder.getSingleCellPolyAs().get(i), sourcePlate);
            labEventHandler.processEvent(polyASelection);

            LabEventTest.validateWorkflow("SingleCellRT", sourcePlate);
            LabEvent reverseTranscription = labEventFactory.buildFromBettaLimsPlateEventDbFree(
                    jaxbBuilder.getSingleCellRTs().get(i), sourcePlate);
            labEventHandler.processEvent(reverseTranscription);

            LabEventTest.validateWorkflow("SingleCellWTA", sourcePlate);
            LabEvent wta = labEventFactory.buildFromBettaLimsPlateEventDbFree(
                    jaxbBuilder.getSingleCellWTAs().get(i), sourcePlate);
            labEventHandler.processEvent(wta);

            LabEventTest.validateWorkflow("SingleCellWTASpri", sourcePlate);
            LabEvent wtaSpri = labEventFactory.buildFromBettaLimsPlateEventDbFree(
                    jaxbBuilder.getSingleCellWTASpris().get(i), sourcePlate);
            labEventHandler.processEvent(wtaSpri);

            // Elution
            LabEventTest.validateWorkflow("SingleCellElutionTransfer", sourcePlate);
            mapBarcodeToVessel.clear();
            mapBarcodeToVessel.put(sourcePlate.getLabel(), sourcePlate);
            LabEvent elutionTransferEntity = labEventFactory.buildFromBettaLims(
                    jaxbBuilder.getSingleCellElutions().get(i), mapBarcodeToVessel);
            labEventHandler.processEvent(elutionTransferEntity);
            StaticPlate elutionPlate =
                    (StaticPlate) elutionTransferEntity.getTargetLabVessels().iterator().next();
            elutionPlates.add(elutionPlate);

            // Pond Pico
            LabEventTest.validateWorkflow("PondPico", elutionPlate);
            mapBarcodeToVessel.clear();
            mapBarcodeToVessel.put(elutionPlate.getLabel(), elutionPlate);
            LabEvent pondPico1 = labEventFactory.buildFromBettaLims(jaxbBuilder.getSingleCellPondPicos().get(picoCounter),
                    mapBarcodeToVessel);
            picoCounter++;
            StaticPlate picoPlate1 = (StaticPlate) pondPico1.getTargetLabVessels().iterator().next();
            LabEvent pondPico2 = labEventFactory.buildFromBettaLims(jaxbBuilder.getSingleCellPondPicos().get(picoCounter),
                    mapBarcodeToVessel);
            StaticPlate picoPlate2 = (StaticPlate) pondPico2.getTargetLabVessels().iterator().next();
            picoCounter++;

            // Norm
            LabEventTest.validateWorkflow("SingleCellNormalization", elutionPlate);
            LabEvent normalization = labEventFactory.buildFromBettaLimsPlateEventDbFree(
                    jaxbBuilder.getSingleCellNormTransfers().get(i), elutionPlate);
            labEventHandler.processEvent(normalization);

            // Tagmentation
            LabEventTest.validateWorkflow("SingleCellTagmentation", elutionPlate);
            if (tagmentationPlate != null) {
                mapBarcodeToVessel.put(tagmentationPlate.getLabel(), tagmentationPlate);
            }

            LabEvent tagmentationEntity = labEventFactory.buildFromBettaLims(
                    jaxbBuilder.getSingleCellTagmentations().get(i), mapBarcodeToVessel);
            labEventHandler.processEvent(tagmentationEntity);
            if (tagmentationPlate == null) {
                tagmentationPlate =
                        (StaticPlate) tagmentationEntity.getTargetLabVessels().iterator().next();
            }
        }

        // Stop Tag
        LabEventTest.validateWorkflow("SingleCellStopTagmentation", tagmentationPlate);
        LabEvent stopTagmentation = labEventFactory.buildFromBettaLimsPlateEventDbFree(
                jaxbBuilder.getStopTagmentation(), tagmentationPlate);
        labEventHandler.processEvent(stopTagmentation);

        // Index Adapter Ligation
        LabEventTest.validateWorkflow("SingleCellIndexAdapterLigation", tagmentationPlate);
        for (int i = 0; i < sourceplates.size(); i++) {
            mapBarcodeToVessel.clear();
            mapBarcodeToVessel.put(indexPlates.get(i).getLabel(), indexPlates.get(i));
            mapBarcodeToVessel.put(tagmentationPlate.getLabel(), tagmentationPlate);
            LabEvent indexEntity = labEventFactory.buildFromBettaLims(
                    jaxbBuilder.getSingleCellIndexAdapterLigations().get(i), mapBarcodeToVessel);
            labEventHandler.processEvent(indexEntity);
        }

        tagmentationPlate.clearCaches();

        // Pooling
        mapBarcodeToVessel.clear();
        mapBarcodeToVessel.put(tagmentationPlate.getLabel(), tagmentationPlate);
        LabEvent poolingTransferEvent = labEventFactory.buildFromBettaLims(
                jaxbBuilder.getPoolingTransferJaxb(), mapBarcodeToVessel);
        labEventHandler.processEvent(poolingTransferEvent);
        poolingRack = (TubeFormation) poolingTransferEvent.getTargetLabVessels().iterator().next();

        Assert.assertEquals(poolingRack.getSampleInstancesV2().size(),
                numSamples, "Wrong number of sample instances");

        // Pooling
        mapBarcodeToVessel.clear();
        mapBarcodeToVessel.put(poolingRack.getLabel(), poolingRack);
        LabEvent bulkSpriEntity = labEventFactory.buildFromBettaLims(
                jaxbBuilder.getBulkSpriTransferJaxb(), mapBarcodeToVessel);
        labEventHandler.processEvent(bulkSpriEntity);
        bulkSpriRack = (TubeFormation) bulkSpriEntity.getTargetLabVessels().iterator().next();

        Assert.assertEquals(bulkSpriRack.getSampleInstancesV2().size(),
                numSamples, "Wrong number of sample instances");

        return this;
    }

    public TubeFormation getBulkSpriRack() {
        return bulkSpriRack;
    }
}

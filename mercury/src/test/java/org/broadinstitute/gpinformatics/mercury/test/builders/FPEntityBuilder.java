package org.broadinstitute.gpinformatics.mercury.test.builders;

import org.broadinstitute.gpinformatics.infrastructure.test.dbfree.BettaLimsMessageTestFactory;
import org.broadinstitute.gpinformatics.mercury.bettalims.generated.PlateTransferEventType;
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
 * Builds entity graphs for the FP process.
 */
public class FPEntityBuilder {
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

    public FPEntityBuilder(
            BettaLimsMessageTestFactory bettaLimsMessageTestFactory,  LabEventFactory labEventFactory,
            LabEventHandler labEventHandler, List<StaticPlate> sourceplates, int numSamples,  String testPrefix) {
        this.bettaLimsMessageTestFactory = bettaLimsMessageTestFactory;
        this.labEventFactory = labEventFactory;
        this.labEventHandler = labEventHandler;
        this.sourceplates = sourceplates;
        this.numSamples = numSamples;
        this.testPrefix = testPrefix;
    }

    public FPEntityBuilder invoke() {
        Map<String, LabVessel> mapBarcodeToVessel = new HashMap<>();
        for (StaticPlate sourceplate: sourceplates) {
            mapBarcodeToVessel.put(sourceplate.getLabel(), sourceplate);
        }
        List<String> sourcePlateBarcodes = new ArrayList<>(mapBarcodeToVessel.keySet());
        fpJaxbBuilder = new FPJaxbBuilder(bettaLimsMessageTestFactory, testPrefix, sourcePlateBarcodes, "IndexPlateP7",
                sourceplates.get(0).getSampleInstanceCount());
        fpJaxbBuilder.invoke();

        for (PlateTransferEventType plateTransferEventType : fpJaxbBuilder.getPcr1TransferJaxbs()) {
            LabEvent pcr1Event = labEventFactory.buildFromBettaLims(plateTransferEventType, mapBarcodeToVessel);
            labEventHandler.processEvent(pcr1Event);
            pcr1Plate = (StaticPlate) pcr1Event.getTargetLabVessels().iterator().next();
        }
        int numSampleInstances = pcr1Plate.getSampleInstancesV2().size();
        Assert.assertEquals(numSampleInstances,
                numSamples, "Wrong number of sample instances");

        //Index Plate to PCR2 plate
        mapBarcodeToVessel.clear();

        List<StaticPlate> indexPlates = LabEventTest.buildIndexPlate(null, null,
                new ArrayList<MolecularIndexingScheme.IndexPosition>() {{
                    add(MolecularIndexingScheme.IndexPosition.ILLUMINA_P7);
                    add(MolecularIndexingScheme.IndexPosition.ILLUMINA_P5);
                }},
                new ArrayList<String>() {{
                    add(fpJaxbBuilder.getDualIndexPrimerPlateBarcode());
                    add(fpJaxbBuilder.getDualIndexPrimerPlateBarcode());
                }}
        );
        indexPlate = indexPlates.get(0);
        mapBarcodeToVessel.put(indexPlate.getLabel(), indexPlate);

        LabEvent indexPrimerTransferEvent = labEventFactory.buildFromBettaLims(
                fpJaxbBuilder.getIndexPrimerTransferJaxb(), mapBarcodeToVessel);
        labEventHandler.processEvent(indexPrimerTransferEvent);

        pcr2Plate = (StaticPlate) indexPrimerTransferEvent.getTargetLabVessels().iterator().next();
        pcr2Plate.clearCaches();

        //PCR1 to PCR2 Indexed
        mapBarcodeToVessel.clear();
        mapBarcodeToVessel.put(pcr1Plate.getLabel(), pcr1Plate);
        mapBarcodeToVessel.put(pcr2Plate.getLabel(), pcr2Plate);
        LabEvent pcrTail2Event = labEventFactory.buildFromBettaLims(
                fpJaxbBuilder.getPcr1ToPcr2TransferJaxb(), mapBarcodeToVessel);
        labEventHandler.processEvent(pcrTail2Event);

        //Pre SPRI Pooling
        mapBarcodeToVessel.clear();
        mapBarcodeToVessel.put(pcr2Plate.getLabel(), pcr2Plate);
        for (PlateTransferEventType plateTransferEventType: fpJaxbBuilder.getPreSpriPoolingJaxbs()) {
            LabEvent preSPRIPoolingEvent = labEventFactory.buildFromBettaLims(plateTransferEventType, mapBarcodeToVessel);
            labEventHandler.processEvent(preSPRIPoolingEvent);
            preSPRIPoolingPlate = (StaticPlate) preSPRIPoolingEvent.getTargetLabVessels().iterator().next();
        }

        //Final Pooling Transfer
        mapBarcodeToVessel.clear();
        mapBarcodeToVessel.put(preSPRIPoolingPlate.getLabel(), preSPRIPoolingPlate);
        LabEvent finalPoolingTransferEvent = labEventFactory.buildFromBettaLims(
                fpJaxbBuilder.getPoolingTransferJaxb(), mapBarcodeToVessel);
        labEventHandler.processEvent(finalPoolingTransferEvent);
        finalPoolingRack = (TubeFormation) finalPoolingTransferEvent.getTargetLabVessels().iterator().next();

        Assert.assertEquals(finalPoolingRack.getSampleInstancesV2().size(),
                numSamples, "Wrong number of sample instances");

        return this;
    }

    public List<StaticPlate> getSourceplates() {
        return sourceplates;
    }

    public FPJaxbBuilder getFpJaxbBuilder() {
        return fpJaxbBuilder;
    }

    public StaticPlate getPcr1Plate() {
        return pcr1Plate;
    }

    public StaticPlate getIndexPlate() {
        return indexPlate;
    }

    public StaticPlate getPcr2Plate() {
        return pcr2Plate;
    }

    public StaticPlate getPreSPRIPoolingPlate() {
        return preSPRIPoolingPlate;
    }

    public TubeFormation getFinalPoolingRack() {
        return finalPoolingRack;
    }

    public int getNumSamples() {
        return numSamples;
    }
}

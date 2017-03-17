package org.broadinstitute.gpinformatics.mercury.test.builders;

import org.broadinstitute.gpinformatics.infrastructure.test.dbfree.BettaLimsMessageTestFactory;
import org.broadinstitute.gpinformatics.mercury.control.labevent.LabEventFactory;
import org.broadinstitute.gpinformatics.mercury.control.labevent.LabEventHandler;
import org.broadinstitute.gpinformatics.mercury.entity.labevent.LabEvent;
import org.broadinstitute.gpinformatics.mercury.entity.reagent.MolecularIndexReagent;
import org.broadinstitute.gpinformatics.mercury.entity.reagent.MolecularIndexingScheme;
import org.broadinstitute.gpinformatics.mercury.entity.reagent.Reagent;
import org.broadinstitute.gpinformatics.mercury.entity.sample.SampleInstanceV2;
import org.broadinstitute.gpinformatics.mercury.entity.vessel.BarcodedTube;
import org.broadinstitute.gpinformatics.mercury.entity.vessel.LabVessel;
import org.broadinstitute.gpinformatics.mercury.entity.vessel.StaticPlate;
import org.broadinstitute.gpinformatics.mercury.entity.vessel.TubeFormation;
import org.broadinstitute.gpinformatics.mercury.entity.vessel.VesselPosition;
import org.broadinstitute.gpinformatics.mercury.test.LabEventTest;
import org.testng.Assert;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Builds entity graph for TruSeq Strand Specific events
 */
public class TruSeqStrandSpecificEntityBuilder {
    private final Map<String, BarcodedTube> mapBarcodeToTube;
    private final TubeFormation inputRack;
    private final BettaLimsMessageTestFactory bettaLimsMessageTestFactory;
    private final LabEventFactory labEventFactory;
    private final LabEventHandler labEventHandler;
    private final String rackBarcode;
    private String testPrefix;
    private String polyAPlateBarcode;
    private StaticPlate polyAPlate;
    private StaticPlate polyASelectionPlate;
    private StaticPlate secondStrandCleanupPlate;
    private StaticPlate endRepairTSCleanupPlate;
    private TubeFormation enrichmentCleanupRack;
    private String enrichmentCleanupRackBarcode;
    private List<String> enrichmentCleanupBarcodes;
    private Map<String, BarcodedTube> mapBarcodeToEnrichmentCleanupTubes;

    public TruSeqStrandSpecificEntityBuilder(Map<String, BarcodedTube> mapBarcodeToTube, TubeFormation inputRack,
                                 BettaLimsMessageTestFactory bettaLimsMessageTestFactory,
                                 LabEventFactory labEventFactory, LabEventHandler labEventHandler,
                                 String rackBarcode, String testPrefix) {
        this.mapBarcodeToTube = mapBarcodeToTube;
        this.inputRack = inputRack;
        this.bettaLimsMessageTestFactory = bettaLimsMessageTestFactory;
        this.labEventFactory = labEventFactory;
        this.labEventHandler = labEventHandler;
        this.rackBarcode = rackBarcode;
        this.testPrefix = testPrefix;
    }

    public TubeFormation getEnrichmentCleanupRack() {
        return enrichmentCleanupRack;
    }

    public String getPolyAPlateBarcode() {
        return polyAPlateBarcode;
    }

    public StaticPlate getPolyAPlate() {
        return polyAPlate;
    }

    public StaticPlate getPolyASelectionPlate() {
        return polyASelectionPlate;
    }

    public StaticPlate getSecondStrandCleanupPlate() {
        return secondStrandCleanupPlate;
    }

    public StaticPlate getEndRepairTSCleanupPlate() {
        return endRepairTSCleanupPlate;
    }

    public String getEnrichmentCleanupRackBarcode() {
        return enrichmentCleanupRackBarcode;
    }

    public List<String> getEnrichmentCleanupBarcodes() {
        return enrichmentCleanupBarcodes;
    }

    public Map<String, BarcodedTube> getMapBarcodeToEnrichmentCleanupTubes() {
        return mapBarcodeToEnrichmentCleanupTubes;
    }

    public TruSeqStrandSpecificEntityBuilder invoke() {
        TruSeqStrandSpecificJaxbBuilder truSeqStrandSpecificJaxbBuilder = new TruSeqStrandSpecificJaxbBuilder(
                bettaLimsMessageTestFactory, new ArrayList<>(mapBarcodeToTube.keySet()), testPrefix,
                "IndexPlateP7", rackBarcode, mapBarcodeToTube.size()).invoke();

        enrichmentCleanupRackBarcode = truSeqStrandSpecificJaxbBuilder.getEnrichmentCleanupRackBarcode();
        enrichmentCleanupBarcodes = truSeqStrandSpecificJaxbBuilder.getEnrichmentcleanupTubeBarcodes();

        polyAPlateBarcode = truSeqStrandSpecificJaxbBuilder.getPolyAPlateBarcode();

        // PolyATransfer
        LabEventTest.validateWorkflow("PolyATransfer", mapBarcodeToTube.values());
        Map<String, LabVessel> mapBarcodeToVessel = new HashMap<>();
        mapBarcodeToVessel.put(inputRack.getLabel(), inputRack);
        for (BarcodedTube barcodedTube : inputRack.getContainerRole().getContainedVessels()) {
            mapBarcodeToVessel.put(barcodedTube.getLabel(), barcodedTube);
        }

        LabEvent polyATransferEventEntity = labEventFactory.buildFromBettaLims(
                truSeqStrandSpecificJaxbBuilder.getPolyATransferEventJaxb(), mapBarcodeToVessel);
        labEventHandler.processEvent(polyATransferEventEntity);

        // asserts
        polyAPlate = (StaticPlate) polyATransferEventEntity.getTargetLabVessels().iterator().next();
        Assert.assertEquals(polyAPlate.getSampleInstancesV2().size(), mapBarcodeToTube.size(),
                "Wrong number of sample instances");

        LabEventTest.validateWorkflow("ERCCSpikeIn", polyAPlate);
        LabEvent erccSpikeInEvent = labEventFactory.buildFromBettaLimsPlateEventDbFree(
                truSeqStrandSpecificJaxbBuilder.getErccSpikeInEventJaxb(), polyAPlate);
        labEventHandler.processEvent(erccSpikeInEvent);

        // PolyASelectionTS
        LabEventTest.validateWorkflow("PolyASelectionTS", polyAPlate);
        mapBarcodeToVessel.clear();
        mapBarcodeToVessel.put(polyAPlate.getLabel(), polyAPlate);
        LabEvent polyASelectionEntity = labEventFactory.buildFromBettaLims(
                truSeqStrandSpecificJaxbBuilder.getPostASelectionEventJaxb(), mapBarcodeToVessel);
        labEventHandler.processEvent(polyASelectionEntity);
        polyASelectionPlate = (StaticPlate) polyASelectionEntity.getTargetLabVessels().iterator().next();
        Assert.assertEquals(polyASelectionPlate.getSampleInstancesV2().size(), mapBarcodeToTube.size(),
                "Wrong number of sample instances");

        //FirstStrandTS
        LabEventTest.validateWorkflow("FirstStrandTS", polyASelectionPlate);
        LabEvent firstStrandTSEvent = labEventFactory.buildFromBettaLimsPlateEventDbFree(
                truSeqStrandSpecificJaxbBuilder.getFirstStrandEventJaxb(), polyASelectionPlate);
        labEventHandler.processEvent(firstStrandTSEvent);

        //SecondStrandTS
        LabEventTest.validateWorkflow("SecondStrandTS", polyASelectionPlate);
        LabEvent secondStrandTSEvent = labEventFactory.buildFromBettaLimsPlateEventDbFree(
                truSeqStrandSpecificJaxbBuilder.getSecondStrandEventJaxb(), polyASelectionPlate);
        labEventHandler.processEvent(secondStrandTSEvent);

        //SecondStrandCleanupTS
        LabEventTest.validateWorkflow("SecondStrandCleanupTS", polyASelectionPlate);
        mapBarcodeToVessel.clear();
        mapBarcodeToVessel.put(polyASelectionPlate.getLabel(), polyASelectionPlate);
        LabEvent secondStrandCleanupEntity = labEventFactory.buildFromBettaLims(
                truSeqStrandSpecificJaxbBuilder.getSecondStrandCleanupEventJaxb(), mapBarcodeToVessel);
        labEventHandler.processEvent(secondStrandCleanupEntity);
        secondStrandCleanupPlate = (StaticPlate) secondStrandCleanupEntity.getTargetLabVessels().iterator().next();

        //ABaseTS
        LabEventTest.validateWorkflow("ABaseTS", secondStrandCleanupPlate);
        LabEvent abaseTSEvent = labEventFactory.buildFromBettaLimsPlateEventDbFree(
                truSeqStrandSpecificJaxbBuilder.getAbaseTSEventJaxb(), secondStrandCleanupPlate);
        labEventHandler.processEvent(abaseTSEvent);

        //AdapterLigationTS
        LabEventTest.validateWorkflow("IndexedAdapterLigationTS", secondStrandCleanupPlate);
        StaticPlate indexPlateP7 = LabEventTest.buildIndexPlate(null, null,
                Collections.singletonList(MolecularIndexingScheme.IndexPosition.ILLUMINA_P7),
                Collections.singletonList(truSeqStrandSpecificJaxbBuilder.getIndexPlateBarcode())).get(0);
        mapBarcodeToVessel.clear();
        mapBarcodeToVessel.put(indexPlateP7.getLabel(), indexPlateP7);
        mapBarcodeToVessel.put(secondStrandCleanupPlate.getLabel(), secondStrandCleanupPlate);
        LabEvent indexedAdapterLigationEntity = labEventFactory.buildFromBettaLims(
                truSeqStrandSpecificJaxbBuilder.getIndexedAdapterLigationTSJaxb(), mapBarcodeToVessel);
        labEventHandler.processEvent(indexedAdapterLigationEntity);

        //Asserts
        Set<SampleInstanceV2> postIndexingSampleInstances =
                secondStrandCleanupPlate.getContainerRole().getSampleInstancesAtPositionV2(VesselPosition.A01);
        SampleInstanceV2 sampleInstance = postIndexingSampleInstances.iterator().next();
        List<Reagent> reagents = sampleInstance.getReagents();
        Assert.assertEquals(reagents.size(), 1, "Wrong number of reagents");
        MolecularIndexReagent molecularIndexReagent = (MolecularIndexReagent) reagents.iterator().next();
        Assert.assertEquals(molecularIndexReagent.getMolecularIndexingScheme().getName(), "Illumina_P7-Habab",
                "Wrong index");

        //AdapterLigationCleanupTS
        LabEventTest.validateWorkflow("AdapterLigationCleanupTS", secondStrandCleanupPlate);
        mapBarcodeToVessel.clear();
        mapBarcodeToVessel.put(secondStrandCleanupPlate.getLabel(), secondStrandCleanupPlate);
        LabEvent ligationCleanupEntity = labEventFactory.buildFromBettaLims(
                truSeqStrandSpecificJaxbBuilder.getAdapterLigationCleanupTSJaxb(), mapBarcodeToVessel);
        labEventHandler.processEvent(ligationCleanupEntity);
        StaticPlate ligationCleanupPlate =
                (StaticPlate) ligationCleanupEntity.getTargetLabVessels().iterator().next();

        //EnrichmentTS
        LabEventTest.validateWorkflow("EnrichmentTS", ligationCleanupPlate);
        LabEvent enrichmentTSEvent = labEventFactory.buildFromBettaLimsPlateEventDbFree(
                truSeqStrandSpecificJaxbBuilder.getEnrichmentTSEventJaxb(), ligationCleanupPlate);
        labEventHandler.processEvent(enrichmentTSEvent);

        //EnrichmentCleanupTS
        LabEventTest.validateWorkflow("EnrichmentCleanupTS", ligationCleanupPlate);
        mapBarcodeToEnrichmentCleanupTubes = new HashMap<>();
        mapBarcodeToVessel.clear();
        mapBarcodeToVessel.put(ligationCleanupPlate.getLabel(), ligationCleanupPlate);
        LabEvent enrichmentCleanupTSEntity = labEventFactory.buildFromBettaLims(
                truSeqStrandSpecificJaxbBuilder.getEnrichmentCleanupTSJaxb(), mapBarcodeToVessel);
        labEventHandler.processEvent(enrichmentCleanupTSEntity);
        enrichmentCleanupRack = (TubeFormation) enrichmentCleanupTSEntity.getTargetLabVessels().iterator().next();
        for (BarcodedTube barcodedTube : enrichmentCleanupRack.getContainerRole().getContainedVessels()) {
            mapBarcodeToEnrichmentCleanupTubes.put(barcodedTube.getLabel(), barcodedTube);
        }

        // asserts
        Assert.assertEquals(enrichmentCleanupRack.getSampleInstancesV2().size(),
                polyAPlate.getSampleInstancesV2().size(), "Wrong number of sample instances");
        Set<SampleInstanceV2> sampleInstancesInPondRegWell =
                enrichmentCleanupRack.getContainerRole().getSampleInstancesAtPositionV2(VesselPosition.A01);
        Assert.assertEquals(sampleInstancesInPondRegWell.size(), 1, "Wrong number of sample instances in position");
        SampleInstanceV2 pondRegSampleInstance = sampleInstancesInPondRegWell.iterator().next();
        Assert.assertEquals(pondRegSampleInstance.getRootOrEarliestMercurySampleName(),
                polyAPlate.getContainerRole().getSampleInstancesAtPositionV2(VesselPosition.A01).iterator().next()
                        .getRootOrEarliestMercurySampleName(),
                "Wrong sample");
        reagents = pondRegSampleInstance.getReagents();
        Assert.assertEquals(reagents.size(), 1, "Wrong number of reagents");
        molecularIndexReagent = (MolecularIndexReagent) reagents.iterator().next();
        Assert.assertEquals(molecularIndexReagent.getMolecularIndexingScheme().getName(), "Illumina_P7-Habab",
                "Wrong index");

        return this;
    }
}

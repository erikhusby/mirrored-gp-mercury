package org.broadinstitute.gpinformatics.mercury.test.builders;

import org.broadinstitute.gpinformatics.infrastructure.test.dbfree.BettaLimsMessageTestFactory;
import org.broadinstitute.gpinformatics.mercury.control.labevent.LabEventFactory;
import org.broadinstitute.gpinformatics.mercury.control.labevent.LabEventHandler;
import org.broadinstitute.gpinformatics.mercury.entity.labevent.LabEvent;
import org.broadinstitute.gpinformatics.mercury.entity.reagent.MolecularIndexingScheme;
import org.broadinstitute.gpinformatics.mercury.entity.reagent.UniqueMolecularIdentifier;
import org.broadinstitute.gpinformatics.mercury.entity.vessel.BarcodedTube;
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
 * Builds entity graph for Library Construction Cell Free UMI events
 */
public class LibraryConstructionCellFreeUMIEntityBuilder {

    private final Map<String, BarcodedTube> mapBarcodeToTube;
    private final TubeFormation upfrontRack;
    private final BettaLimsMessageTestFactory bettaLimsMessageTestFactory;
    private final LabEventFactory labEventFactory;
    private final LabEventHandler labEventHandler;
    private TubeFormation pondRegRack;
    private int numSamples;
    private String testPrefix;
    private final LibraryConstructionEntityBuilder.Umi umi;

    private StaticPlate endRepairPlate;
    private List<String> pondRegTubeBarcodes;
    private final Map<String, BarcodedTube> mapBarcodeToPondRegTubes = new HashMap<>();
    private String pondRegRackBarcode;

    public LibraryConstructionCellFreeUMIEntityBuilder(Map<String, BarcodedTube> mapBarcodeToTube,
                                                       TubeFormation upfrontRack,
                                                       BettaLimsMessageTestFactory bettaLimsMessageTestFactory,
                                                       LabEventFactory labEventFactory,
                                                       LabEventHandler labEventHandler, int numSamples,
                                                       String testPrefix,
                                                       LibraryConstructionEntityBuilder.Umi umi) {
        this.mapBarcodeToTube = mapBarcodeToTube;
        this.upfrontRack = upfrontRack;
        this.bettaLimsMessageTestFactory = bettaLimsMessageTestFactory;
        this.labEventFactory = labEventFactory;
        this.labEventHandler = labEventHandler;
        this.numSamples = numSamples;
        this.testPrefix = testPrefix;
        this.umi = umi;
    }

    public LibraryConstructionCellFreeUMIEntityBuilder invoke() {
        final String p7IndexPlateBarcode = testPrefix + "UmiP7IndexPlate";
        final String p5IndexPlateBarcode = testPrefix + "UmiP5IndexPlate";
        final LibraryConstructionCellFreeUMIJaxbBuilder libraryConstructionJaxbBuilder =
                new LibraryConstructionCellFreeUMIJaxbBuilder(bettaLimsMessageTestFactory, testPrefix,
                        "UmiTubeBarcode", p7IndexPlateBarcode, p5IndexPlateBarcode,
                        new ArrayList<>(mapBarcodeToTube.keySet()), "UmiStartingRackBarcode" + testPrefix
                ).invoke();

        LabEventTest.validateWorkflow("PreEndRepairTransfer", mapBarcodeToTube.values());
        Map<String, LabVessel> mapBarcodeToVessel = new HashMap<>();
        mapBarcodeToVessel.put(upfrontRack.getLabel(), upfrontRack);
        for (BarcodedTube barcodedTube : upfrontRack.getContainerRole().getContainedVessels()) {
            mapBarcodeToVessel.put(barcodedTube.getLabel(), barcodedTube);
        }

        LabEvent preEndRepairEventEntity = labEventFactory.buildFromBettaLims(
                libraryConstructionJaxbBuilder.getPreEndRepairTransferEventJaxb(), mapBarcodeToVessel);
        labEventHandler.processEvent(preEndRepairEventEntity);
        // asserts
        endRepairPlate = (StaticPlate) preEndRepairEventEntity.getTargetLabVessels().iterator().next();
        Assert.assertEquals(endRepairPlate.getSampleInstancesV2().size(), mapBarcodeToTube.size(),
                "Wrong number of sample instances");

        LabEventTest.validateWorkflow("EndRepair_ABase", endRepairPlate);
        LabEvent endRepairAbaseEntity = labEventFactory.buildFromBettaLimsPlateEventDbFree(
                libraryConstructionJaxbBuilder.getEndRepairAbaseJaxb(), endRepairPlate);
        labEventHandler.processEvent(endRepairAbaseEntity);

        LabEventTest.validateWorkflow("PostEndRepairThermoCyclerLoaded", endRepairPlate);
        LabEvent postEndRepairThermoCyclerEntity = labEventFactory.buildFromBettaLimsPlateEventDbFree(
                libraryConstructionJaxbBuilder.getPostEndRepairThermoCyclerLoadedJaxb(), endRepairPlate);
        labEventHandler.processEvent(postEndRepairThermoCyclerEntity);

        BarcodedTube umiTube = null;
        UniqueMolecularIdentifier umiReagent = new UniqueMolecularIdentifier(
                UniqueMolecularIdentifier.UMILocation.BEFORE_FIRST_READ, 6L, 3L);
        if (umi == LibraryConstructionEntityBuilder.Umi.DUAL) {
            UniqueMolecularIdentifier umiReagent2 = new UniqueMolecularIdentifier(
                    UniqueMolecularIdentifier.UMILocation.BEFORE_SECOND_READ, 6L, 3L);
            umiTube = LabEventTest.buildUmiTube(testPrefix + "UmiTestTube", umiReagent, umiReagent2);
        } else if (umi == LibraryConstructionEntityBuilder.Umi.SINGLE) {
            umiTube = LabEventTest.buildUmiTube(testPrefix + "UmiTestTube", umiReagent);
        }

        LabEventTest.validateWorkflow("UMIAddition", endRepairPlate);
        libraryConstructionJaxbBuilder.getUmiAdditionJaxb().getSourceReceptacle().setBarcode(umiTube.getLabel());
        LabEvent umiAdditionEntity = labEventFactory.buildVesselToSectionDbFree(
                libraryConstructionJaxbBuilder.getUmiAdditionJaxb(), umiTube, endRepairPlate, "ALL96");
        labEventHandler.processEvent(umiAdditionEntity);
        endRepairPlate.clearCaches();

        LabEventTest.validateWorkflow("PostUMIAdditionThermoCyclerLoaded", endRepairPlate);
        LabEvent postUmiThermoCyclerEntity = labEventFactory.buildFromBettaLimsPlateEventDbFree(
                libraryConstructionJaxbBuilder.getPostUmiAdditionThermoCyclerLoadedJaxb(), endRepairPlate);
        labEventHandler.processEvent(postUmiThermoCyclerEntity);

        LabEventTest.validateWorkflow("UMICleanup", endRepairPlate);
        mapBarcodeToVessel.clear();
        mapBarcodeToVessel.put(endRepairPlate.getLabel(), endRepairPlate);
        LabEvent umiCleanupEntity = labEventFactory.buildFromBettaLims(
                libraryConstructionJaxbBuilder.getUmiCleanupJaxb(), mapBarcodeToVessel);
        labEventHandler.processEvent(umiCleanupEntity);
        StaticPlate umiCleanupPlate =
                (StaticPlate) umiCleanupEntity.getTargetLabVessels().iterator().next();

        List<StaticPlate> indexPlates = LabEventTest.buildIndexPlate(null, null,
                new ArrayList<MolecularIndexingScheme.IndexPosition>() {{
                    add(MolecularIndexingScheme.IndexPosition.ILLUMINA_P7);
                    add(MolecularIndexingScheme.IndexPosition.ILLUMINA_P5);
                }},
                new ArrayList<String>() {{
                    add(p7IndexPlateBarcode);
                    add(p5IndexPlateBarcode);
                }}
        );
        StaticPlate p7IndexPlate = indexPlates.get(0);
        StaticPlate p5IndexPlate = indexPlates.get(1);

        LabEventTest.validateWorkflow("IndexedAdapterLigation", umiCleanupPlate);
        mapBarcodeToVessel.clear();
        mapBarcodeToVessel.put(p7IndexPlate.getLabel(), p7IndexPlate);
        mapBarcodeToVessel.put(umiCleanupPlate.getLabel(), umiCleanupPlate);
        LabEvent p7IndexAdapterLigation = labEventFactory.buildFromBettaLims(
                libraryConstructionJaxbBuilder.getP7IndexJaxb(), mapBarcodeToVessel);
        labEventHandler.processEvent(p7IndexAdapterLigation);
        p7IndexPlate.clearCaches();
        umiCleanupPlate.clearCaches();

        LabEventTest.validateWorkflow("IndexP5PondEnrichment", umiCleanupPlate);
        mapBarcodeToVessel.clear();
        mapBarcodeToVessel.put(p5IndexPlate.getLabel(), p5IndexPlate);
        mapBarcodeToVessel.put(umiCleanupPlate.getLabel(), umiCleanupPlate);
        LabEvent p5IndexPondEnrichment = labEventFactory.buildFromBettaLims(
                libraryConstructionJaxbBuilder.getP5IndexJaxb(), mapBarcodeToVessel);
        labEventHandler.processEvent(p5IndexPondEnrichment);
        p5IndexPlate.clearCaches();
        umiCleanupPlate.clearCaches();

        LabEventTest.validateWorkflow("PostIndexedAdapterLigationThermoCyclerLoaded", umiCleanupPlate);
        LabEvent postIndexAdapterLigationThermoCyclerLoaded = labEventFactory.buildFromBettaLimsPlateEventDbFree(
                libraryConstructionJaxbBuilder.getPostUmiAdditionThermoCyclerLoadedJaxb(), umiCleanupPlate);
        labEventHandler.processEvent(postIndexAdapterLigationThermoCyclerLoaded);

        LabEventTest.validateWorkflow("CFDnaPondRegistration", umiCleanupPlate);
        LabEvent pondRegistrationEntity = labEventFactory.buildFromBettaLims(
                libraryConstructionJaxbBuilder.getPondRegistrationJaxb(), mapBarcodeToVessel);
        labEventHandler.processEvent(pondRegistrationEntity);
        pondRegRack = (TubeFormation) pondRegistrationEntity.getTargetLabVessels().iterator().next();
        pondRegRackBarcode = libraryConstructionJaxbBuilder.getPondRegRackBarcode();
        pondRegTubeBarcodes = libraryConstructionJaxbBuilder.getPondRegTubeBarcodes();
        for (BarcodedTube barcodedTube : pondRegRack.getContainerRole().getContainedVessels()) {
            mapBarcodeToPondRegTubes.put(barcodedTube.getLabel(), barcodedTube);
        }

        Assert.assertEquals(pondRegRack.getSampleInstancesV2().size(),
                endRepairPlate.getSampleInstancesV2().size(), "Wrong number of sample instances");

        return this;
    }

    public TubeFormation getPondRegRack() {
        return pondRegRack;
    }

    public List<String> getPondRegTubeBarcodes() {
        return pondRegTubeBarcodes;
    }

    public Map<String, BarcodedTube> getMapBarcodeToPondRegTubes() {
        return mapBarcodeToPondRegTubes;
    }

    public String getPondRegRackBarcode() {
        return pondRegRackBarcode;
    }
}

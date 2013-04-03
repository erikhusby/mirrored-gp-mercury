package org.broadinstitute.gpinformatics.mercury.test.builders;

import org.broadinstitute.gpinformatics.infrastructure.test.dbfree.BettaLimsMessageTestFactory;
import org.broadinstitute.gpinformatics.mercury.control.labevent.LabEventFactory;
import org.broadinstitute.gpinformatics.mercury.control.labevent.LabEventHandler;
import org.broadinstitute.gpinformatics.mercury.entity.labevent.LabEvent;
import org.broadinstitute.gpinformatics.mercury.entity.reagent.MolecularIndexReagent;
import org.broadinstitute.gpinformatics.mercury.entity.sample.SampleInstance;
import org.broadinstitute.gpinformatics.mercury.entity.vessel.StaticPlate;
import org.broadinstitute.gpinformatics.mercury.entity.vessel.TubeFormation;
import org.broadinstitute.gpinformatics.mercury.entity.vessel.TwoDBarcodedTube;
import org.broadinstitute.gpinformatics.mercury.entity.vessel.VesselPosition;
import org.broadinstitute.gpinformatics.mercury.test.LabEventTest;
import org.testng.Assert;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Builds entity graph for Library Construction events
 */
public class LibraryConstructionEntityBuilder {
    private final BettaLimsMessageTestFactory bettaLimsMessageTestFactory;
    private final LabEventFactory             labEventFactory;
    private final LabEventHandler             labEventHandler;
    private final StaticPlate                 shearingPlate;
    private final String shearCleanPlateBarcode;
    private final StaticPlate shearingCleanupPlate;
    private String pondRegRackBarcode;
    private       List<String>                pondRegTubeBarcodes;
    private       TubeFormation               pondRegRack;
    private int numSamples;

    public LibraryConstructionEntityBuilder(BettaLimsMessageTestFactory bettaLimsMessageTestFactory,
                                            LabEventFactory labEventFactory, LabEventHandler labEventHandler,
                                            StaticPlate shearingCleanupPlate, String shearCleanPlateBarcode,
                                            StaticPlate shearingPlate, int numSamples) {
        this.bettaLimsMessageTestFactory = bettaLimsMessageTestFactory;
        this.labEventFactory = labEventFactory;
        this.labEventHandler = labEventHandler;
        this.shearingCleanupPlate = shearingCleanupPlate;
        this.shearCleanPlateBarcode = shearCleanPlateBarcode;
        this.shearingPlate = shearingPlate;
        this.numSamples = numSamples;
    }

    public List<String> getPondRegTubeBarcodes() {
        return pondRegTubeBarcodes;
    }

    public String getPondRegRackBarcode() {
        return pondRegRackBarcode;
    }

    public TubeFormation getPondRegRack() {
        return pondRegRack;
    }

    public LibraryConstructionEntityBuilder invoke() {
        LibraryConstructionJaxbBuilder
                libraryConstructionJaxbBuilder = new LibraryConstructionJaxbBuilder(
                bettaLimsMessageTestFactory, "", shearCleanPlateBarcode, "IndexPlate", numSamples).invoke();
        pondRegRackBarcode = libraryConstructionJaxbBuilder.getPondRegRackBarcode();
        pondRegTubeBarcodes = libraryConstructionJaxbBuilder.getPondRegTubeBarcodes();

        // EndRepair
        LabEventTest.validateWorkflow("EndRepair", shearingCleanupPlate);
        LabEvent endRepairEntity = labEventFactory.buildFromBettaLimsPlateEventDbFree(
                libraryConstructionJaxbBuilder.getEndRepairJaxb(), shearingCleanupPlate);
        labEventHandler.processEvent(endRepairEntity);

        // EndRepairCleanup
        LabEventTest.validateWorkflow("EndRepairCleanup", shearingCleanupPlate);
        LabEvent endRepairCleanupEntity = labEventFactory.buildFromBettaLimsPlateEventDbFree(
                libraryConstructionJaxbBuilder.getEndRepairCleanupJaxb(), shearingCleanupPlate);
        labEventHandler.processEvent(endRepairCleanupEntity);

        // ABase
        LabEventTest.validateWorkflow("ABase", shearingCleanupPlate);
        LabEvent aBaseEntity = labEventFactory.buildFromBettaLimsPlateEventDbFree(
                libraryConstructionJaxbBuilder.getaBaseJaxb(), shearingCleanupPlate);
        labEventHandler.processEvent(aBaseEntity);

        // ABaseCleanup
        LabEventTest.validateWorkflow("ABaseCleanup", shearingCleanupPlate);
        LabEvent aBaseCleanupEntity = labEventFactory.buildFromBettaLimsPlateEventDbFree(
                libraryConstructionJaxbBuilder.getaBaseCleanupJaxb(), shearingCleanupPlate);
        labEventHandler.processEvent(aBaseCleanupEntity);

        // IndexedAdapterLigation
        LabEventTest.validateWorkflow("IndexedAdapterLigation", shearingCleanupPlate);
        LabEventTest.BuildIndexPlate buildIndexPlate = new LabEventTest.BuildIndexPlate(libraryConstructionJaxbBuilder.getIndexPlateBarcode())
                .invoke(null);
        StaticPlate indexPlate = buildIndexPlate.getIndexPlate();
        LabEvent indexedAdapterLigationEntity = labEventFactory.buildFromBettaLimsPlateToPlateDbFree(
                libraryConstructionJaxbBuilder.getIndexedAdapterLigationJaxb(), indexPlate, shearingCleanupPlate);
        labEventHandler.processEvent(indexedAdapterLigationEntity);
        // asserts
        Set<SampleInstance> postIndexingSampleInstances =
                shearingCleanupPlate.getContainerRole().getSampleInstancesAtPosition(VesselPosition.A01);
        SampleInstance sampleInstance = postIndexingSampleInstances.iterator().next();
        MolecularIndexReagent molecularIndexReagent =
                (MolecularIndexReagent) sampleInstance.getReagents().iterator().next();
        Assert.assertEquals(molecularIndexReagent.getMolecularIndexingScheme().getName(), "Illumina_P7-M",
                                   "Wrong index");

        // AdapterLigationCleanup
        LabEventTest.validateWorkflow("AdapterLigationCleanup", shearingCleanupPlate);
        LabEvent ligationCleanupEntity = labEventFactory.buildFromBettaLimsPlateToPlateDbFree(
                libraryConstructionJaxbBuilder.getLigationCleanupJaxb(), shearingCleanupPlate, null);
        labEventHandler.processEvent(ligationCleanupEntity);
        StaticPlate ligationCleanupPlate =
                (StaticPlate) ligationCleanupEntity.getTargetLabVessels().iterator().next();

        // PondEnrichment
        LabEventTest.validateWorkflow("PondEnrichment", ligationCleanupPlate);
        LabEvent pondEnrichmentEntity = labEventFactory.buildFromBettaLimsPlateEventDbFree(
                libraryConstructionJaxbBuilder.getPondEnrichmentJaxb(), ligationCleanupPlate);
        labEventHandler.processEvent(pondEnrichmentEntity);

        // HybSelPondEnrichmentCleanup
        LabEventTest.validateWorkflow("HybSelPondEnrichmentCleanup", ligationCleanupPlate);
        LabEvent pondCleanupEntity = labEventFactory.buildFromBettaLimsPlateToPlateDbFree(
                libraryConstructionJaxbBuilder.getPondCleanupJaxb(), ligationCleanupPlate, null);
        labEventHandler.processEvent(pondCleanupEntity);
        StaticPlate pondCleanupPlate = (StaticPlate) pondCleanupEntity.getTargetLabVessels().iterator().next();

        // PondRegistration
        LabEventTest.validateWorkflow("PondRegistration", pondCleanupPlate);
        Map<String, TwoDBarcodedTube> mapBarcodeToPondRegTube = new HashMap<String, TwoDBarcodedTube>();
        LabEvent pondRegistrationEntity = labEventFactory.buildFromBettaLimsPlateToRackDbFree(
                libraryConstructionJaxbBuilder.getPondRegistrationJaxb(), pondCleanupPlate, mapBarcodeToPondRegTube, null);
        labEventHandler.processEvent(pondRegistrationEntity);
        // asserts
        pondRegRack = (TubeFormation) pondRegistrationEntity.getTargetLabVessels().iterator().next();
        Assert.assertEquals(pondRegRack.getSampleInstances().size(),
                shearingPlate.getSampleInstances().size(), "Wrong number of sample instances");
        Set<SampleInstance> sampleInstancesInPondRegWell = pondRegRack.getContainerRole().getSampleInstancesAtPosition(VesselPosition.A01);
        Assert.assertEquals(sampleInstancesInPondRegWell.size(), 1, "Wrong number of sample instances in position");
        Assert.assertEquals(sampleInstancesInPondRegWell.iterator().next().getStartingSample().getSampleKey(),
                shearingPlate.getContainerRole().getSampleInstancesAtPosition(VesselPosition.A01).iterator().next().getStartingSample().getSampleKey(),
                "Wrong sample");
        return this;
    }
}

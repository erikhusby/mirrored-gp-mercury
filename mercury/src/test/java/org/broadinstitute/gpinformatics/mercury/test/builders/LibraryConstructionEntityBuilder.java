package org.broadinstitute.gpinformatics.mercury.test.builders;

import org.apache.commons.lang3.tuple.Triple;
import org.broadinstitute.gpinformatics.infrastructure.test.dbfree.BettaLimsMessageTestFactory;
import org.broadinstitute.gpinformatics.mercury.boundary.lims.LimsQueries;
import org.broadinstitute.gpinformatics.mercury.control.labevent.LabEventFactory;
import org.broadinstitute.gpinformatics.mercury.control.labevent.LabEventHandler;
import org.broadinstitute.gpinformatics.mercury.entity.labevent.LabEvent;
import org.broadinstitute.gpinformatics.mercury.entity.reagent.MolecularIndexReagent;
import org.broadinstitute.gpinformatics.mercury.entity.reagent.MolecularIndexingScheme;
import org.broadinstitute.gpinformatics.mercury.entity.reagent.Reagent;
import org.broadinstitute.gpinformatics.mercury.entity.sample.SampleInstanceV2;
import org.broadinstitute.gpinformatics.mercury.entity.vessel.LabVessel;
import org.broadinstitute.gpinformatics.mercury.entity.vessel.StaticPlate;
import org.broadinstitute.gpinformatics.mercury.entity.vessel.TubeFormation;
import org.broadinstitute.gpinformatics.mercury.entity.vessel.VesselPosition;
import org.broadinstitute.gpinformatics.mercury.test.LabEventTest;
import org.testng.Assert;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Builds entity graph for Library Construction events
 */
public class LibraryConstructionEntityBuilder {
    public enum Indexing {
        SINGLE,
        DUAL
    }

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
    private String testPrefix;
    private Indexing indexing;

    public LibraryConstructionEntityBuilder(BettaLimsMessageTestFactory bettaLimsMessageTestFactory,
            LabEventFactory labEventFactory, LabEventHandler labEventHandler, StaticPlate shearingCleanupPlate,
            String shearCleanPlateBarcode, StaticPlate shearingPlate, int numSamples, String testPrefix,
            Indexing indexing) {
        this.bettaLimsMessageTestFactory = bettaLimsMessageTestFactory;
        this.labEventFactory = labEventFactory;
        this.labEventHandler = labEventHandler;
        this.shearingCleanupPlate = shearingCleanupPlate;
        this.shearCleanPlateBarcode = shearCleanPlateBarcode;
        this.shearingPlate = shearingPlate;
        this.numSamples = numSamples;
        this.testPrefix = testPrefix;
        this.indexing = indexing;
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
        final LibraryConstructionJaxbBuilder libraryConstructionJaxbBuilder = new LibraryConstructionJaxbBuilder(
                bettaLimsMessageTestFactory, testPrefix, shearCleanPlateBarcode, "IndexPlateP7", "IndexPlateP5",
                numSamples, LibraryConstructionJaxbBuilder.TargetSystem.SQUID_VIA_MERCURY,
                Arrays.asList(Triple.of("KAPA Reagent Box", "0009753252", 1)),
                Arrays.asList(Triple.of("PEG", "0009753352", 2), Triple.of("70% Ethanol", "LCEtohTest", 3),
                        Triple.of("EB", "0009753452", 4), Triple.of("SPRI", "LCSpriTest", 5)),
                Arrays.asList(Triple.of("KAPA Amp Kit", "0009753250", 6))
        ).invoke();
        pondRegRackBarcode = libraryConstructionJaxbBuilder.getPondRegRackBarcode();
        pondRegTubeBarcodes = libraryConstructionJaxbBuilder.getPondRegTubeBarcodes();

        // EndRepair
        LabEventTest.validateWorkflow("EndRepair", shearingCleanupPlate);
        LabEvent endRepairEntity = labEventFactory.buildFromBettaLimsPlateEventDbFree(
                libraryConstructionJaxbBuilder.getEndRepairJaxb(), shearingCleanupPlate);
        labEventHandler.processEvent(endRepairEntity);

        // PostEndRepairThermoCyclerLoaded
        LabEventTest.validateWorkflow("PostEndRepairThermoCyclerLoaded", shearingCleanupPlate);
        LabEvent postEndRepairThermoCyclerLoadedEntity = labEventFactory.buildFromBettaLimsPlateEventDbFree(
                libraryConstructionJaxbBuilder.getEndRepairJaxb(), shearingCleanupPlate);
        labEventHandler.processEvent(postEndRepairThermoCyclerLoadedEntity);

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

        // PostABaseThermoCyclerLoaded
        LabEventTest.validateWorkflow("PostAbaseThermoCyclerLoaded", shearingCleanupPlate);
        LabEvent postABaseThermoCyclerLoadedEntity = labEventFactory.buildFromBettaLimsPlateEventDbFree(
                libraryConstructionJaxbBuilder.getPostABaseThermoCyclerLoadedJaxb(), shearingCleanupPlate);
        labEventHandler.processEvent(postABaseThermoCyclerLoadedEntity);

        // ABaseCleanup
        LabEventTest.validateWorkflow("ABaseCleanup", shearingCleanupPlate);
        LabEvent aBaseCleanupEntity = labEventFactory.buildFromBettaLimsPlateEventDbFree(
                libraryConstructionJaxbBuilder.getaBaseCleanupJaxb(), shearingCleanupPlate);
        labEventHandler.processEvent(aBaseCleanupEntity);

        // IndexedAdapterLigation
        LabEventTest.validateWorkflow("IndexedAdapterLigation", shearingCleanupPlate);
        StaticPlate indexPlateP7;
        StaticPlate indexPlateP5 = null;
        if (indexing == Indexing.DUAL) {
            List<StaticPlate> indexPlates = LabEventTest.buildIndexPlate(null, null,
                    new ArrayList<MolecularIndexingScheme.IndexPosition>() {{
                        add(MolecularIndexingScheme.IndexPosition.ILLUMINA_P7);
                        add(MolecularIndexingScheme.IndexPosition.ILLUMINA_P5);
                    }},
                    new ArrayList<String>() {{
                        add(libraryConstructionJaxbBuilder.getP7IndexPlateBarcode());
                        add(libraryConstructionJaxbBuilder.getP5IndexPlateBarcode());
                    }}
            );
            indexPlateP7 = indexPlates.get(0);
            indexPlateP5 = indexPlates.get(1);
        } else {
            indexPlateP7 = LabEventTest.buildIndexPlate(null, null,
                    Collections.singletonList(MolecularIndexingScheme.IndexPosition.ILLUMINA_P7),
                    Collections.singletonList(libraryConstructionJaxbBuilder.getP7IndexPlateBarcode())).get(0);
        }
        Map<String, LabVessel> mapBarcodeToVessel = new HashMap<>();
        mapBarcodeToVessel.put(indexPlateP7.getLabel(), indexPlateP7);
        mapBarcodeToVessel.put(shearingCleanupPlate.getLabel(), shearingCleanupPlate);
        LabEvent indexedAdapterLigationEntity = labEventFactory.buildFromBettaLims(
                libraryConstructionJaxbBuilder.getIndexedAdapterLigationJaxb(), mapBarcodeToVessel);
        labEventHandler.processEvent(indexedAdapterLigationEntity);
        shearingCleanupPlate.clearCaches();

        // asserts
        Set<SampleInstanceV2> postIndexingSampleInstances =
                shearingCleanupPlate.getContainerRole().getSampleInstancesAtPositionV2(VesselPosition.A01);
        SampleInstanceV2 sampleInstance = postIndexingSampleInstances.iterator().next();
        List<Reagent> reagents = sampleInstance.getReagents();
        Assert.assertEquals(reagents.size(), 1, "Wrong number of reagents");
        MolecularIndexReagent molecularIndexReagent = (MolecularIndexReagent) reagents.iterator().next();
        Assert.assertEquals(molecularIndexReagent.getMolecularIndexingScheme().getName(), "Illumina_P7-M",
                                   "Wrong index");

        // PostIndexedAdapterLigationThermoCyclerLoaded
        LabEventTest.validateWorkflow("PostIndexedAdapterLigationThermoCyclerLoaded", shearingCleanupPlate);
        LabEvent postIdxAdapterLigationThermoCyclerLoadedEntity = labEventFactory.buildFromBettaLimsPlateEventDbFree(
                libraryConstructionJaxbBuilder.getPostIdxAdapterLigationThermoCyclerLoadedJaxb(), shearingCleanupPlate);
        labEventHandler.processEvent(postIdxAdapterLigationThermoCyclerLoadedEntity);

        // AdapterLigationCleanup
        LabEventTest.validateWorkflow("AdapterLigationCleanup", shearingCleanupPlate);
        mapBarcodeToVessel.clear();
        mapBarcodeToVessel.put(shearingCleanupPlate.getLabel(), shearingCleanupPlate);
        LabEvent ligationCleanupEntity = labEventFactory.buildFromBettaLims(
                libraryConstructionJaxbBuilder.getLigationCleanupJaxb(), mapBarcodeToVessel);
        labEventHandler.processEvent(ligationCleanupEntity);
        StaticPlate ligationCleanupPlate =
                (StaticPlate) ligationCleanupEntity.getTargetLabVessels().iterator().next();

        LimsQueries limsQueries = new LimsQueries(null, null, null, null);
        List<String> plateParents = limsQueries.findImmediatePlateParents(ligationCleanupPlate);
        Assert.assertEquals(plateParents.size(), 1, "Wrong number of plate parents");
        Assert.assertEquals(plateParents.get(0), shearingCleanupPlate.getLabel(), "Wrong parent barcode");

        if (libraryConstructionJaxbBuilder.getPondEnrichmentJaxb() != null) {
            // PondEnrichment
            LabEventTest.validateWorkflow("PondEnrichment", ligationCleanupPlate);
            LabEvent pondEnrichmentEntity = labEventFactory.buildFromBettaLimsPlateEventDbFree(
                    libraryConstructionJaxbBuilder.getPondEnrichmentJaxb(), ligationCleanupPlate);
            labEventHandler.processEvent(pondEnrichmentEntity);
        }

        if (indexing == Indexing.DUAL) {
            // IndexP5PondEnrichment
            LabEventTest.validateWorkflow("IndexP5PondEnrichment", ligationCleanupPlate);
            mapBarcodeToVessel.clear();
            mapBarcodeToVessel.put(indexPlateP5.getLabel(), indexPlateP5);
            mapBarcodeToVessel.put(ligationCleanupPlate.getLabel(), ligationCleanupPlate);
            LabEvent indexP5PondEnrichmentEntity = labEventFactory.buildFromBettaLims(
                    libraryConstructionJaxbBuilder.getIndexP5PondEnrichmentJaxb(), mapBarcodeToVessel);
            labEventHandler.processEvent(indexP5PondEnrichmentEntity);
            ligationCleanupPlate.clearCaches();
        }

        // PostPondEnrichmentThermoCyclerLoaded
        LabEventTest.validateWorkflow("PostPondEnrichmentThermoCyclerLoaded", ligationCleanupPlate);
        LabEvent postPondEnrichmentThermoCyclerLoadedEntity = labEventFactory.buildFromBettaLimsPlateEventDbFree(
                libraryConstructionJaxbBuilder.getPostPondEnrichmentThermoCyclerLoadedJaxb(), ligationCleanupPlate);
        labEventHandler.processEvent(postPondEnrichmentThermoCyclerLoadedEntity);

        // HybSelPondEnrichmentCleanup
        LabEventTest.validateWorkflow("HybSelPondEnrichmentCleanup", ligationCleanupPlate);
        mapBarcodeToVessel.clear();
        mapBarcodeToVessel.put(ligationCleanupPlate.getLabel(), ligationCleanupPlate);
        LabEvent pondCleanupEntity = labEventFactory.buildFromBettaLims(
                libraryConstructionJaxbBuilder.getPondCleanupJaxb(), mapBarcodeToVessel);
        labEventHandler.processEvent(pondCleanupEntity);
        StaticPlate pondCleanupPlate = (StaticPlate) pondCleanupEntity.getTargetLabVessels().iterator().next();

        // PondRegistration
        LabEventTest.validateWorkflow("PondRegistration", pondCleanupPlate);
        mapBarcodeToVessel.clear();
        mapBarcodeToVessel.put(pondCleanupPlate.getLabel(), pondCleanupPlate);
        LabEvent pondRegistrationEntity = labEventFactory.buildFromBettaLims(
                libraryConstructionJaxbBuilder.getPondRegistrationJaxb(), mapBarcodeToVessel);
        labEventHandler.processEvent(pondRegistrationEntity);
        // asserts
        pondRegRack = (TubeFormation) pondRegistrationEntity.getTargetLabVessels().iterator().next();
        Assert.assertEquals(pondRegRack.getSampleInstancesV2().size(),
                shearingPlate.getSampleInstancesV2().size(), "Wrong number of sample instances");
        Set<SampleInstanceV2> sampleInstancesInPondRegWell =
                pondRegRack.getContainerRole().getSampleInstancesAtPositionV2(VesselPosition.A01);
        Assert.assertEquals(sampleInstancesInPondRegWell.size(), 1, "Wrong number of sample instances in position");
        SampleInstanceV2 pondRegSampleInstance = sampleInstancesInPondRegWell.iterator().next();
        Assert.assertEquals(pondRegSampleInstance.getRootOrEarliestMercurySampleName(),
                shearingPlate.getContainerRole().getSampleInstancesAtPositionV2(VesselPosition.A01).iterator().next()
                        .getRootOrEarliestMercurySampleName(),
                "Wrong sample");
        reagents = pondRegSampleInstance.getReagents();
        Assert.assertEquals(reagents.size(), 1, "Wrong number of reagents");
        molecularIndexReagent = (MolecularIndexReagent) reagents.iterator().next();
        Assert.assertEquals(molecularIndexReagent.getMolecularIndexingScheme().getName(), "Illumina_P5-M_P7-M",
                "Wrong index");
        return this;
    }
}

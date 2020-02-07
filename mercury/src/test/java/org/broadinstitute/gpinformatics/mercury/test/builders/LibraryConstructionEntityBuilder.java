package org.broadinstitute.gpinformatics.mercury.test.builders;

import org.apache.commons.lang3.tuple.Triple;
import org.broadinstitute.gpinformatics.infrastructure.test.dbfree.BettaLimsMessageTestFactory;
import org.broadinstitute.gpinformatics.mercury.boundary.lims.LimsQueries;
import org.broadinstitute.gpinformatics.mercury.control.labevent.LabEventFactory;
import org.broadinstitute.gpinformatics.mercury.control.labevent.LabEventHandler;
import org.broadinstitute.gpinformatics.mercury.entity.OrmUtil;
import org.broadinstitute.gpinformatics.mercury.entity.labevent.LabEvent;
import org.broadinstitute.gpinformatics.mercury.entity.reagent.MolecularIndexReagent;
import org.broadinstitute.gpinformatics.mercury.entity.reagent.MolecularIndexingScheme;
import org.broadinstitute.gpinformatics.mercury.entity.reagent.Reagent;
import org.broadinstitute.gpinformatics.mercury.entity.reagent.UMIReagent;
import org.broadinstitute.gpinformatics.mercury.entity.reagent.UniqueMolecularIdentifier;
import org.broadinstitute.gpinformatics.mercury.entity.sample.SampleInstanceV2;
import org.broadinstitute.gpinformatics.mercury.entity.vessel.BarcodedTube;
import org.broadinstitute.gpinformatics.mercury.entity.vessel.LabVessel;
import org.broadinstitute.gpinformatics.mercury.entity.vessel.PlateWell;
import org.broadinstitute.gpinformatics.mercury.entity.vessel.StaticPlate;
import org.broadinstitute.gpinformatics.mercury.entity.vessel.TubeFormation;
import org.broadinstitute.gpinformatics.mercury.entity.vessel.VesselPosition;
import org.broadinstitute.gpinformatics.mercury.test.LabEventTest;
import org.testng.Assert;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Builds entity graph for Library Construction events
 */
public class LibraryConstructionEntityBuilder {
    public enum Indexing {
        SINGLE,
        DUAL,
        DUAL_UMI
    }

    public enum Umi {
        NONE,
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
    private String pondNormRackBarcode;
    private List<String> pondNormTubeBarcodes;
    private TubeFormation pondNormRack;
    private int numSamples;
    private String testPrefix;
    private Indexing indexing;
    private LibraryConstructionJaxbBuilder.PondType pondType;
    private final Umi umi;
    private final String p7IndexPlateBarcode;
    private final String p5IndexPlateBarcode;

    private final Map<String, BarcodedTube> mapBarcodeToPondRegTubes = new HashMap<>();
    private final Map<String, BarcodedTube> mapBarcodeToPondNormTubes = new HashMap<>();
    private boolean includeUmi = false;

    public LibraryConstructionEntityBuilder(BettaLimsMessageTestFactory bettaLimsMessageTestFactory,
                                            LabEventFactory labEventFactory, LabEventHandler labEventHandler, StaticPlate shearingCleanupPlate,
                                            String shearCleanPlateBarcode, StaticPlate shearingPlate, int numSamples, String testPrefix,
                                            Indexing indexing, LibraryConstructionJaxbBuilder.PondType pondType) {
        this(bettaLimsMessageTestFactory, labEventFactory, labEventHandler, shearingCleanupPlate, shearCleanPlateBarcode,
                shearingPlate, numSamples, testPrefix, indexing, pondType, Umi.SINGLE);
    }

    public LibraryConstructionEntityBuilder(BettaLimsMessageTestFactory bettaLimsMessageTestFactory,
            LabEventFactory labEventFactory, LabEventHandler labEventHandler, StaticPlate shearingCleanupPlate,
            String shearCleanPlateBarcode, StaticPlate shearingPlate, int numSamples, String testPrefix,
            Indexing indexing, LibraryConstructionJaxbBuilder.PondType pondType, Umi umi) {
        this(bettaLimsMessageTestFactory, labEventFactory, labEventHandler, shearingCleanupPlate, shearCleanPlateBarcode,
                shearingPlate, numSamples, testPrefix, indexing, pondType, umi, "IndexPlateP7", "IndexPlateP5");
    }

    public LibraryConstructionEntityBuilder(BettaLimsMessageTestFactory bettaLimsMessageTestFactory,
                                            LabEventFactory labEventFactory, LabEventHandler labEventHandler, StaticPlate shearingCleanupPlate,
                                            String shearCleanPlateBarcode, StaticPlate shearingPlate, int numSamples, String testPrefix,
                                            Indexing indexing, LibraryConstructionJaxbBuilder.PondType pondType, Umi umi,
                                            String p7IndexPlateBarcode, String p5IndexPlateBarcode) {
        this.bettaLimsMessageTestFactory = bettaLimsMessageTestFactory;
        this.labEventFactory = labEventFactory;
        this.labEventHandler = labEventHandler;
        this.shearingCleanupPlate = shearingCleanupPlate;
        this.shearCleanPlateBarcode = shearCleanPlateBarcode;
        this.shearingPlate = shearingPlate;
        this.numSamples = numSamples;
        this.testPrefix = testPrefix;
        this.indexing = indexing;
        this.pondType = pondType;
        this.umi = umi;
        this.p7IndexPlateBarcode = p7IndexPlateBarcode;
        this.p5IndexPlateBarcode = p5IndexPlateBarcode;
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

    public Map<String, BarcodedTube> getMapBarcodeToPondRegTubes() {
        return mapBarcodeToPondRegTubes;
    }

    public TubeFormation getPondNormRack() {
        return pondNormRack;
    }

    public String getPondNormRackBarcode() {
        return pondNormRackBarcode;
    }

    public List<String> getPondNormTubeBarcodes() {
        return pondNormTubeBarcodes;
    }

    public Map<String, BarcodedTube> getMapBarcodeToPondNormTubes() {
        return mapBarcodeToPondNormTubes;
    }

    public LibraryConstructionEntityBuilder invoke() {
        final LibraryConstructionJaxbBuilder libraryConstructionJaxbBuilder = new LibraryConstructionJaxbBuilder(
                bettaLimsMessageTestFactory, testPrefix, shearCleanPlateBarcode, p7IndexPlateBarcode, p5IndexPlateBarcode,
                numSamples,
                Arrays.asList(Triple.of("KAPA Reagent Box", "0009753252", 1)),
                Arrays.asList(Triple.of("PEG", "0009753352", 2), Triple.of("70% Ethanol", "LCEtohTest", 3),
                        Triple.of("EB", "0009753452", 4), Triple.of("SPRI", "LCSpriTest", 5)),
                Arrays.asList(Triple.of("KAPA Amp Kit", "0009753250", 6)),
                pondType
        ).invoke();

        Set<String> shearingPlateA01SampleNames = new HashSet<>();
        for (SampleInstanceV2 sampleInstance : shearingPlate.getContainerRole().getSampleInstancesAtPositionV2(VesselPosition.A01)) {
            shearingPlateA01SampleNames.add(sampleInstance.getRootOrEarliestMercurySampleName());
        }

        if (pondType == LibraryConstructionJaxbBuilder.PondType.PCR_PLUS_HYPER_PREP ||
            pondType == LibraryConstructionJaxbBuilder.PondType.PCR_FREE_HYPER_PREP ||
            pondType == LibraryConstructionJaxbBuilder.PondType.CELL_FREE) {
            LabEventTest.validateWorkflow("EndRepair_ABase", shearingCleanupPlate);
            LabEvent endRepairAbaseEntity = labEventFactory.buildFromBettaLimsPlateEventDbFree(
                    libraryConstructionJaxbBuilder.getEndRepairAbaseJaxb(), shearingCleanupPlate);
            labEventHandler.processEvent(endRepairAbaseEntity);
        } else {
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
        }

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
        } else if (indexing == Indexing.SINGLE){
            indexPlateP7 = LabEventTest.buildIndexPlate(null, null,
                    Collections.singletonList(MolecularIndexingScheme.IndexPosition.ILLUMINA_P7),
                    Collections.singletonList(libraryConstructionJaxbBuilder.getP7IndexPlateBarcode())).get(0);
        } else { //Must be Dual Index UMI
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
            attachUmiToIndexPlate(indexPlateP7, UniqueMolecularIdentifier.UMILocation.BEFORE_FIRST_INDEX_READ);
            attachUmiToIndexPlate(indexPlateP5, UniqueMolecularIdentifier.UMILocation.BEFORE_SECOND_INDEX_READ);
        }
        Map<String, LabVessel> mapBarcodeToVessel = new HashMap<>();
        mapBarcodeToVessel.put(indexPlateP7.getLabel(), indexPlateP7);
        mapBarcodeToVessel.put(shearingCleanupPlate.getLabel(), shearingCleanupPlate);
        LabEvent indexedAdapterLigationEntity = labEventFactory.buildFromBettaLims(
                libraryConstructionJaxbBuilder.getIndexedAdapterLigationJaxb(), mapBarcodeToVessel);
        labEventHandler.processEvent(indexedAdapterLigationEntity);
        shearingCleanupPlate.clearCaches();

        // asserts
        for (SampleInstanceV2 sampleInstance :
                shearingCleanupPlate.getContainerRole().getSampleInstancesAtPositionV2(VesselPosition.A01)) {
            List<Reagent> reagents = sampleInstance.getReagents();
            if (includeUmi) {
                int reagentCount = umi == Umi.SINGLE ? 2 : 3;
                Assert.assertEquals(reagents.size(), reagentCount, "Wrong number of reagents");
                MolecularIndexReagent molecularIndexReagent = findIndexReagent(reagents);
                Assert.assertEquals(molecularIndexReagent.getMolecularIndexingScheme().getName(), "Illumina_P7-Habab",
                        "Wrong index");
            } else {
                Assert.assertEquals(reagents.size(), 1, "Wrong number of reagents");
                MolecularIndexReagent molecularIndexReagent = (MolecularIndexReagent) reagents.iterator().next();
                Assert.assertEquals(molecularIndexReagent.getMolecularIndexingScheme().getName(), "Illumina_P7-Habab",
                        "Wrong index");
            }
        }
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

        if (indexing == Indexing.DUAL || indexing == Indexing.DUAL_UMI) {
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

        // HyperPrep skips the plate to plate cleanups
        StaticPlate pondCleanupPlate = ligationCleanupPlate;
        if (pondType != LibraryConstructionJaxbBuilder.PondType.PCR_FREE_HYPER_PREP) {
            if (pondType == LibraryConstructionJaxbBuilder.PondType.PCR_PLUS_HYPER_PREP) {
                LabEventTest.validateWorkflow("WGSPCRCleanup", ligationCleanupPlate);
                LabEvent wgsPcrCleanupEvent = labEventFactory.buildFromBettaLimsPlateEventDbFree(
                        libraryConstructionJaxbBuilder.getWgsPCRCleanupJaxb(), ligationCleanupPlate);
                labEventHandler.processEvent(wgsPcrCleanupEvent);
            } else if (pondType == LibraryConstructionJaxbBuilder.PondType.CELL_FREE) {
                LabEventTest.validateWorkflow("CFDnaPCRSetup", ligationCleanupPlate);
                LabEvent wgsPcrCleanupEvent = labEventFactory.buildFromBettaLimsPlateEventDbFree(
                        libraryConstructionJaxbBuilder.getCellFreePCRCleanupJaxb(), ligationCleanupPlate);
                labEventHandler.processEvent(wgsPcrCleanupEvent);
            } else {
                LabEventTest.validateWorkflow("HybSelPondEnrichmentCleanup", ligationCleanupPlate);
                mapBarcodeToVessel.clear();
                mapBarcodeToVessel.put(ligationCleanupPlate.getLabel(), ligationCleanupPlate);
                LabEvent pondCleanupEntity = labEventFactory.buildFromBettaLims(
                        libraryConstructionJaxbBuilder.getPondCleanupJaxb(), mapBarcodeToVessel);
                labEventHandler.processEvent(pondCleanupEntity);
                pondCleanupPlate = (StaticPlate) pondCleanupEntity.getTargetLabVessels().iterator().next();
            }
        }

        // PondRegistration
        LabEventTest.validateWorkflow(pondType.getEventType(), pondCleanupPlate);
        mapBarcodeToVessel.clear();
        mapBarcodeToVessel.put(pondCleanupPlate.getLabel(), pondCleanupPlate);
        LabEvent pondRegistrationEntity = labEventFactory.buildFromBettaLims(
                libraryConstructionJaxbBuilder.getPondRegistrationJaxb(), mapBarcodeToVessel);
        labEventHandler.processEvent(pondRegistrationEntity);
        pondRegRack = (TubeFormation) pondRegistrationEntity.getTargetLabVessels().iterator().next();

        pondRegRackBarcode = libraryConstructionJaxbBuilder.getPondRegRackBarcode();
        pondRegTubeBarcodes = libraryConstructionJaxbBuilder.getPondRegTubeBarcodes();

        for (BarcodedTube barcodedTube : pondRegRack.getContainerRole().getContainedVessels()) {
            mapBarcodeToPondRegTubes.put(barcodedTube.getLabel(), barcodedTube);
        }

        // PCRPlusPondNormalization
        if (pondType == LibraryConstructionJaxbBuilder.PondType.PCR_PLUS) {
            LabEventTest.validateWorkflow("PCRPlusPondNormalization", pondRegRack);
            mapBarcodeToVessel.clear();
            for (BarcodedTube barcodedTube : pondRegRack.getContainerRole().getContainedVessels()) {
                mapBarcodeToVessel.put(barcodedTube.getLabel(), barcodedTube);
            }
            mapBarcodeToVessel.put(pondRegRack.getLabel(), pondRegRack);
            LabEvent pondNormEntity = labEventFactory.buildFromBettaLims(
                    libraryConstructionJaxbBuilder.getPondNormJaxb(), mapBarcodeToVessel);
            labEventHandler.processEvent(pondNormEntity);
            pondNormRack = (TubeFormation) pondNormEntity.getTargetLabVessels().iterator().next();

            pondNormRackBarcode = libraryConstructionJaxbBuilder.getPondNormRackBarcode();
            pondNormTubeBarcodes = libraryConstructionJaxbBuilder.getPondNormTubeBarcodes();

            for (BarcodedTube barcodedTube : pondNormRack.getContainerRole().getContainedVessels()) {
                mapBarcodeToPondNormTubes.put(barcodedTube.getLabel(), barcodedTube);
            }
        }

        // Check the sample instances.
        // The pond cleanup plate will always have sample instances in all 96 wells due to the
        // reagent addition.
        Set<SampleInstanceV2> nonReagentInstances = new HashSet<>();
        for (SampleInstanceV2 sampleInstance : pondRegRack.getSampleInstancesV2()) {
            if (sampleInstance.getNearestMercurySample() != null) {
                nonReagentInstances.add(sampleInstance);
            }
        }
        Assert.assertEquals(nonReagentInstances.size(), shearingPlate.getSampleInstancesV2().size(),
                "Wrong number of sample instances");

        // A pooled sample will have multiple sample instances in A01.
        Set<SampleInstanceV2> sampleInstancesInPondRegWell =
                pondRegRack.getContainerRole().getSampleInstancesAtPositionV2(VesselPosition.A01);
        for (SampleInstanceV2 pondRegSampleInstance : sampleInstancesInPondRegWell) {
            Assert.assertTrue(shearingPlateA01SampleNames.contains(
                    pondRegSampleInstance.getRootOrEarliestMercurySampleName()),
                    "Wrong sample " + pondRegSampleInstance.getRootOrEarliestMercurySampleName());
            List<Reagent> reagents = pondRegSampleInstance.getReagents();
            if (includeUmi) {
                if (umi == Umi.DUAL) {
                    Assert.assertEquals(reagents.size(), 3, "Wrong number of reagents");
                } else {
                    Assert.assertEquals(reagents.size(), 2, "Wrong number of reagents");
                }
                MolecularIndexReagent molecularIndexReagent = findIndexReagent(reagents);
                Assert.assertEquals(molecularIndexReagent.getMolecularIndexingScheme().getName(),
                        "Illumina_P5-Habab_P7-Habab",
                        "Wrong index");
            } else if (indexing == Indexing.DUAL) {
                Assert.assertEquals(reagents.size(), 1, "Wrong number of reagents");
                MolecularIndexReagent molecularIndexReagent = (MolecularIndexReagent) reagents.iterator().next();
                Assert.assertEquals(molecularIndexReagent.getMolecularIndexingScheme().getName(),
                        "Illumina_P5-Habab_P7-Habab",
                        "Wrong index");
            } else if (indexing == Indexing.SINGLE) {
                Assert.assertEquals(reagents.size(), 1, "Wrong number of reagents");
                MolecularIndexReagent molecularIndexReagent = (MolecularIndexReagent) reagents.iterator().next();
                Assert.assertEquals(molecularIndexReagent.getMolecularIndexingScheme().getName(),
                        "Illumina_P7-Habab",
                        "Wrong index");
            }
        }
        return this;
    }

    private MolecularIndexReagent findIndexReagent(List<Reagent> reagents) {
        for (Reagent reagent : reagents) {
            if (OrmUtil.proxySafeIsInstance(reagent, MolecularIndexReagent.class))
                return OrmUtil.proxySafeCast(reagent, MolecularIndexReagent.class);
        }
        return null;
    }

    private void attachUmiToIndexPlate(StaticPlate indexPlate, UniqueMolecularIdentifier.UMILocation umiLocation) {
        UniqueMolecularIdentifier umi = new UniqueMolecularIdentifier(umiLocation, 3L, 2L);
        UMIReagent umiReagent = new UMIReagent(umi);
        Map<VesselPosition, PlateWell> mapPositionToVessel = indexPlate.getContainerRole().getMapPositionToVessel();
        for (VesselPosition vesselPosition: indexPlate.getVesselGeometry().getVesselPositions()) {
            PlateWell plateWell;
            if (mapPositionToVessel != null && mapPositionToVessel.containsKey(vesselPosition)) {
                plateWell = mapPositionToVessel.get(vesselPosition);
                plateWell.addReagent(umiReagent);
            } else {
                plateWell = new PlateWell(indexPlate, vesselPosition);
                plateWell.addReagent(umiReagent);
                indexPlate.getContainerRole().addContainedVessel(plateWell, vesselPosition);
            }

        }
    }

    public void setIncludeUmi(boolean includeUmi) {
        this.includeUmi = includeUmi;
    }
}

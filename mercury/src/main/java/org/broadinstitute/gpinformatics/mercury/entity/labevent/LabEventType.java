package org.broadinstitute.gpinformatics.mercury.entity.labevent;

import java.util.HashMap;
import java.util.Map;

/**
 * Properties common to all events of a particular message type
 */
public enum LabEventType {
    // Preflight

    PREFLIGHT_CLEANUP ("PreFlightCleanup", true, true, SystemOfRecord.SQUID),
    PREFLIGHT_PICO_SETUP ("PreflightPicoSetup", true, true, SystemOfRecord.SQUID),
    PREFLIGHT_NORMALIZATION("PreflightNormalization", true, true, SystemOfRecord.SQUID),
    PREFLIGHT_POST_NORM_PICO_SETUP ("PreflightPostNormPicoSetup", true, true, SystemOfRecord.SQUID),

    // Shearing
    SHEARING_TRANSFER("ShearingTransfer", false, true, SystemOfRecord.SQUID),
    PLATING_TO_SHEARING_TUBES("PlatingToShearingTubes", false, true, SystemOfRecord.SQUID),
    COVARIS_LOADED("CovarisLoaded", false, true, SystemOfRecord.SQUID),
    POST_SHEARING_TRANSFER_CLEANUP("PostShearingTransferCleanup", false, true, SystemOfRecord.SQUID),
    SHEARING_QC ("ShearingQC", false, true, SystemOfRecord.SQUID),

    // Library construction
    END_REPAIR ("EndRepair", true, true, SystemOfRecord.SQUID),
    POST_END_REPAIR_THERMO_CYCLER_LOADED("PostEndRepairThermoCyclerLoaded", false, true, SystemOfRecord.SQUID),
    END_REPAIR_CLEANUP ("EndRepairCleanup", true, true, SystemOfRecord.SQUID),
    A_BASE ("ABase", true, true, SystemOfRecord.SQUID),
    POST_ABASE_THERMO_CYCLER_LOADED("PostAbaseThermoCyclerLoaded", false, true, SystemOfRecord.SQUID),
    A_BASE_CLEANUP ("ABaseCleanup", true, true, SystemOfRecord.SQUID),
    INDEXED_ADAPTER_LIGATION ("IndexedAdapterLigation", true, false, SystemOfRecord.SQUID),
    POST_INDEXED_ADAPTER_LIGATION_THERMO_CYCLER_LOADED("PostIndexedAdapterLigationThermoCyclerLoaded", true, true, SystemOfRecord.SQUID),
    ADAPTER_LIGATION_CLEANUP ("AdapterLigationCleanup", false, true, SystemOfRecord.SQUID),
    // no longer used, needed to import past messages
    WGS_ADAPTER_LIGATION_CLEANUP ("WGSAdapterLigationCleanup", false, true, SystemOfRecord.SQUID),
    POND_ENRICHMENT ("PondEnrichment", true, true, SystemOfRecord.SQUID),
    POST_POND_ENRICHMENT_THERMO_CYCLER_LOADED("PostPondEnrichmentThermoCyclerLoaded", true, true, SystemOfRecord.SQUID),
    POND_ENRICHMENT_CLEANUP ("HybSelPondEnrichmentCleanup", false, true, SystemOfRecord.SQUID),
    POND_REGISTRATION ("PondRegistration", false, true, SystemOfRecord.SQUID),
    POND_PICO("PondPico", false, true, SystemOfRecord.SQUID),

    // Hybrid Selection
    PRE_SELECTION_POOL ("PreSelectionPool", false, true, SystemOfRecord.SQUID),
    HYBRIDIZATION ("Hybridization", false, true, SystemOfRecord.SQUID),
    POST_HYBRIDIZATION_THERMO_CYCLER_LOADED("PostHybridizationThermoCyclerLoaded", false, true, SystemOfRecord.SQUID),
    BAIT_SETUP ("BaitSetup", true, true, SystemOfRecord.SQUID), // todo jmt change to BOTH
    BAIT_ADDITION ("BaitAddition", true, false, SystemOfRecord.SQUID),
    BEAD_ADDITION ("BeadAddition", true, true, SystemOfRecord.SQUID),
    AP_WASH ("APWash", true, true, SystemOfRecord.SQUID),
    GS_WASH_1 ("GSWash1", true, true, SystemOfRecord.SQUID),
    GS_WASH_2 ("GSWash2", true, true, SystemOfRecord.SQUID),
    GS_WASH_3 ("GSWash3", true, true, SystemOfRecord.SQUID),
    GS_WASH_4 ("GSWash4", true, true, SystemOfRecord.SQUID),
    GS_WASH_5 ("GSWash5", true, true, SystemOfRecord.SQUID),
    GS_WASH_6 ("GSWash6", true, true, SystemOfRecord.SQUID),
    CATCH_ENRICHMENT_SETUP ("CatchEnrichmentSetup", true, true, SystemOfRecord.SQUID),
    POST_CATCH_ENRICHMENT_SETUP_THERMO_CYCLER_LOADED ("PostCatchEnrichmentSetupThermoCyclerLoaded", true, true, SystemOfRecord.SQUID),
    CATCH_ENRICHMENT_CLEANUP ("CatchEnrichmentCleanup", false, true, SystemOfRecord.SQUID),
    NORMALIZED_CATCH_REGISTRATION ("NormalizedCatchRegistration", false, true, SystemOfRecord.SQUID),
    CATCH_PICO ("CatchPico", false, true, SystemOfRecord.SQUID),

    // QTP
    POOLING_TRANSFER ("PoolingTransfer", false, true, SystemOfRecord.SQUID),
    ECO_TRANSFER ("EcoTransfer", false, true, SystemOfRecord.SQUID),
    NORMALIZATION_TRANSFER ("NormalizationTransfer", false, true, SystemOfRecord.SQUID),
    DENATURE_TRANSFER ("DenatureTransfer", false, true, SystemOfRecord.SQUID),
    STRIP_TUBE_B_TRANSFER ("StripTubeBTransfer", true, false, SystemOfRecord.SQUID),
    FLOWCELL_TRANSFER ("FlowcellTransfer", true, false, SystemOfRecord.SQUID),
    FLOWCELL_LOADED ("FlowcellLoaded", true, false, SystemOfRecord.SQUID),

    // Sage
    SAGE_LOADING ("SageLoading", true, false, SystemOfRecord.SQUID),
    SAGE_LOADED ("SageLoaded", true, false, SystemOfRecord.SQUID),
    SAGE_UNLOADING ("SageUnloading", true, false, SystemOfRecord.SQUID),
    SAGE_CLEANUP ("SageCleanup", true, false, SystemOfRecord.SQUID),

    // Fluidigm
    FLUIDIGM_SAMPLE_INPUT ("FluidigmSampleInput", true, false, SystemOfRecord.SQUID),
    FLUIDIGM_INDEXED_ADAPTER_INPUT ("FluidigmIndexedAdapterInput", true, false, SystemOfRecord.SQUID),
    FLUIDIGM_INDEXED_ADAPTER_PCR ("FluidigmIndexedAdapterPCR", true, false, SystemOfRecord.SQUID),
    FLUIDIGM_PRODUCT_DILUTION ("FluidigmProductDilution", true, false, SystemOfRecord.SQUID),
    FLUIDIGM_HARVESTING_TO_RACK ("FluidigmHarvestingToRack", true, false, SystemOfRecord.SQUID),
    FLUIDIGM_HARVESTING ("FluidigmHarvesting", true, false, SystemOfRecord.SQUID),
    FLUIDIGM_FINAL_TRANSFER("FluidigmFinalTransfer", true, false, SystemOfRecord.SQUID),

    // Jump
    JUMP_INITIAL_SHEARING_TRANSFER("JumpInitialShearingTransfer", false, true, SystemOfRecord.SQUID),
    JUMP_INITIAL_SIZE_SELECTION("JumpInitialSizeSelection", false, true, SystemOfRecord.SQUID),
    JUMP_END_REPAIR_1("JumpEndRepair1", true, false, SystemOfRecord.SQUID),
    JUMP_INITIAL_SIZE_SELECTION_QC("JumpInitialSizeSelectionQC", false, true, SystemOfRecord.SQUID),
    JUMP_END_REPAIR_1_CLEANUP("JumpEndRepair1Cleanup", false, true, SystemOfRecord.SQUID),
    JUMP_END_REPAIR_1_QC("JumpEndRepair1QC", false, true, SystemOfRecord.SQUID),
    JUMP_CIRCULARIZATION_SETUP("JumpCircularizationSetup", false, true, SystemOfRecord.SQUID),
    JUMP_EXONUCLEASE_TREATMENT("JumpExonucleaseTreatment", true, false, SystemOfRecord.SQUID),
    JUMP_EXO_INACTIVATION("JumpExoInactivation", false, true, SystemOfRecord.SQUID),
    JUMP_POST_CIRC_SIZE_SELECTION("JumpPostCircSizeSelection", false, true, SystemOfRecord.SQUID),
    JUMP_IMMOBILIZATION("JumpImmobilization", true, false, SystemOfRecord.SQUID),
    JUMP_IMMOBILIZATION_WASH("JumpImmobilizationWash", false, true, SystemOfRecord.SQUID),
    JUMP_END_REPAIR_2("JumpEndRepair2", false, true, SystemOfRecord.SQUID),
    JUMP_END_REPAIR_2_WASH("JumpEndRepair2Wash", false, true, SystemOfRecord.SQUID),
    JUMP_A_TAILING("JumpATailing", false, true, SystemOfRecord.SQUID),
    JUMP_A_TAILING_WASH("JumpATailingWash", false, true, SystemOfRecord.SQUID),
    JUMP_ADD_INDEX("JumpAddIndex", false, true, SystemOfRecord.SQUID),
    JUMP_ADAPTER_LIGATION("JumpAdapterLigation", false, true, SystemOfRecord.SQUID),
    JUMP_ADAPTER_LIGATION_WASH("JumpAdapterLigationWash", false, true, SystemOfRecord.SQUID),
    JUMP_AMPLIFICATION("JumpAmplification", false, true, SystemOfRecord.SQUID),
    JUMP_FINAL_LIBRARY_ELUTION("JumpFinalLibraryElution", false, true, SystemOfRecord.SQUID),
    JUMP_FINAL_LIBRARY_SIZE_SELECTION("JumpFinalLibrarySizeSelection", false, true, SystemOfRecord.SQUID),
    JUMP_FINAL_LIBRARY_QC("JumpFinalLibraryQC", false, true, SystemOfRecord.SQUID),
    JUMP_ENRICHED_LIBRARY_REGISTRATION("JumpEnrichedLibraryRegistration", false, true, SystemOfRecord.SQUID),

    // BSP Pico

    PICO_PLATING_BUCKET("PicoPlatingBucket", true, true, SystemOfRecord.MERCURY),
    PICO_PLATING_QC ("PicoPlatingQC", true,true, SystemOfRecord.MERCURY),
    PICO_DILUTION_TRANSFER ("PicoDilutionTransfer", true, true, SystemOfRecord.MERCURY),
    PICO_BUFFER_ADDITION("PicoBufferAddition", true, true, SystemOfRecord.MERCURY),
    PICO_MICROFLUOR_TRANSFER("PicoMicrofluorTransfer", true, true, SystemOfRecord.MERCURY),
    PICO_STANDARDS_TRANSFER ("PicoStandardsTransfer", true, true, SystemOfRecord.MERCURY),
    PICO_PLATING_POST_NORM_PICO("PicoPlatingPostNorm", true, true, SystemOfRecord.MERCURY),

    // Dried Blood Spot
    DBS_SAMPLE_PUNCH ("DBSSamplePunch", false, true, SystemOfRecord.MERCURY),
    DBS_INCUBATION_MIX ("DBSIncubationMix", true, false, SystemOfRecord.MERCURY),
    DBS_LYSIS_BUFFER ("DBSLysisBuffer", true, false, SystemOfRecord.MERCURY),
    DBS_MAGNETIC_RESIN ("DBSMagneticResin", true, false, SystemOfRecord.MERCURY),
    DBS_1ST_PURIFICATION ("DBS1stPurification", false, true, SystemOfRecord.MERCURY),
    DBS_WASH_BUFFER ("DBSWashBuffer", true, false, SystemOfRecord.MERCURY),
    DBS_2ND_PURIFICATION ("DBS2ndPurification", false, true, SystemOfRecord.MERCURY),
    DBS_ELUTION_BUFFER ("DBSElutionBuffer", false, true, SystemOfRecord.MERCURY),
    DBS_FINAL_TRANSFER ("DBSFinalTransfer", false, true, SystemOfRecord.MERCURY),

    // TruSeq Custom Amplicon
    TSCA_POST_NORM_TRANSFER("TSCAPostNormTransfer", false, true, SystemOfRecord.SQUID),
    TSCA_HYB_SET_UP("TSCAHybSetUp", false, true, SystemOfRecord.SQUID),
    TSCA_CAT_ADDITION("TSCACATAddition", false, true, SystemOfRecord.SQUID),
    TSCA_HYBRIDIZATION_CLEANUP("TSCAHybridizationCleanUp", false, true, SystemOfRecord.SQUID),
    TSCA_FLP_PREP("TSCAFLPPrep", false, true, SystemOfRecord.SQUID),
    TSCA_EXTENSION_LIGATION_SETUP("TSCAExtensionLigationSetup", false, true, SystemOfRecord.SQUID),
    TSCA_INDEXED_AMPLICON_PCR("TSCAIndexedAmpliconPCR", false, true, SystemOfRecord.SQUID),
    TSCA_INDEX_ADDITION("TSCAIndexAddition", false, true, SystemOfRecord.SQUID),
    TSCA_AMPLICON_REGISTRATION("TSCAAmpliconRegistration", false, true, SystemOfRecord.SQUID),
    TSCA_AMPLICON_PCR_CLEANUP("TSCAAmpliconPCRCleanup", false, true, SystemOfRecord.SQUID),
    TSCA_POOL_CREATION("TSCAPoolCreation", false, true, SystemOfRecord.SQUID),

    // 16S
    ILLUMINA_16S_TO_PRIMER_TRANSFER("Illumina16StoPrimerTransfer", false, true, SystemOfRecord.SQUID),
    ILLUMINA_16S_PCR_PRODUCT_TRANSFER("Illumina16SPcrProductTransfer", false, true, SystemOfRecord.SQUID),

    // cDNA dUTP
    POLY_A_TRANSFER("PolyATransfer", false, true, SystemOfRecord.SQUID),
    POLY_A_SELECTION("PolyASelection", false, true, SystemOfRecord.SQUID),
    DNASE("Dnase", false, true, SystemOfRecord.SQUID),
    DNASE_CLEANUP("DnaseCleanup", false, true, SystemOfRecord.SQUID),
    FRAGMENTATION("Fragmentation", false, true, SystemOfRecord.SQUID),
    FRAGMENTATION_CLEANUP("FragmentationCleanup", false, true, SystemOfRecord.SQUID),
    FIRST_STRAND("FirstStrand", false, true, SystemOfRecord.SQUID),
    FIRST_STRAND_CLEANUP("FirstStrandCleanup", false, true, SystemOfRecord.SQUID),
    SECOND_STRAND("SecondStrand", false, true, SystemOfRecord.SQUID),
    SECOND_STRAND_CLEANUP("SecondStrandCleanup", false, true, SystemOfRecord.SQUID),
    USER("USER", false, true, SystemOfRecord.SQUID),
    CDNA_PONDENRICHMENT_CLEANUP("cDNAPondEnrichmentCleanup", false, true, SystemOfRecord.SQUID),

    // cDNA TruSeq
    POLY_A_SELECTION_TS("PolyASelectionTS", false, true, SystemOfRecord.SQUID),
    FIRST_STRAND_TS("FirstStrandTS", false, true, SystemOfRecord.SQUID),
    SECOND_STRAND_TS("SecondStrandTS", false, true, SystemOfRecord.SQUID),
    SECOND_STRAND_CLEANUP_TS("SecondStrandCleanupTS", false, true, SystemOfRecord.SQUID),
    END_REPAIR_TS("EndRepairTS", false, true, SystemOfRecord.SQUID),
    END_REPAIR_CLEANUP_TS("EndRepairCleanupTS", false, true, SystemOfRecord.SQUID),
    A_BASE_TS("ABaseTS", false, true, SystemOfRecord.SQUID),
    INDEXED_ADAPTER_LIGATION_TS("IndexedAdapterLigationTS", false, true, SystemOfRecord.SQUID),
    ADAPTER_LIGATION_CLEANUP_TS("AdapterLigationCleanupTS", false, true, SystemOfRecord.SQUID),
    ENRICHMENT_TS("EnrichmentTS", false, true, SystemOfRecord.SQUID),
    ENRICHMENT_CLEANUP_TS("EnrichmentCleanupTS", false, true, SystemOfRecord.SQUID),

    // Illumina Genome Network
    IGN_PRE_NORM_TRANSFER("IGNPreNormTransfer", false, true, SystemOfRecord.SQUID),
    IGN_NORM_TRANSFER("IGNNormTransfer", false, true, SystemOfRecord.SQUID),
    IGN_INCOMING_PICO("IGNIncomingPico", false, true, SystemOfRecord.SQUID),
    IGN_GAP_ALIQUOT("IGNGapAliquot", false, true, SystemOfRecord.SQUID),

    VIRAL_PCR_PRODUCT_POOL_TRANSFER("ViralPcrProductPoolTransfer", false, true, SystemOfRecord.SQUID),
    ONE_STEP_RT_PCR_PRODUCT_POOL_TRANSFER("OneStepRtPcrProductPoolTransfer", false, true, SystemOfRecord.SQUID),

    // From BSP UI
    SAMPLES_EXTRACTION_START("SamplesExtractionStart", false, true, SystemOfRecord.MERCURY),
    SAMPLES_EXTRACTION_END_TRANSFER("SamplesExtractionEndTransfer", false, true, SystemOfRecord.MERCURY),
    SAMPLES_NORMALIZATION_TRANSFER("SamplesNormalizationTransfer", false, true, SystemOfRecord.MERCURY),
    SAMPLES_PLATING_TO_COVARIS("SamplesPlatingToCovaris", false, true, SystemOfRecord.MERCURY),

    MRRBS_GENOMIC_TRANSFER("mRRBSGenomicTransfer", false, true, SystemOfRecord.SQUID),
    MRRBS_INDEXING("mRRBSIndexing", false, true, SystemOfRecord.SQUID),
    MRRBS_NORM_LIBS("mRRBSNormLibs", false, true, SystemOfRecord.SQUID),

    /**
     * TODO SGM  the following names are place holders.  They will be re-evaluated as development of
     */
    SHEARING_BUCKET("ShearingBucket", true, true, SystemOfRecord.MERCURY),
//    SHEARING_BUCKET_ENTRY ("ShearingBucketEntry", true, true, SystemOfRecord.MERCURY),
//    SHEARING_BUCKET_EXIT ("ShearingBucketExit", true, true, SystemOfRecord.MERCURY),
    ;

    private final String name;

    private final boolean expectedEmptySources;

    private final boolean expectedEmptyTargets;

    /**
     * For each event, which system is primarily responsible for that lab process
     */
    public enum SystemOfRecord {
        /** Squid / BettaLIMS, being phased out */
        SQUID,
        /** Mercury, being phased in */
        MERCURY,
        /** For processes that are shared by multiple products, a message could belong to either system.  The
         * message router must examine the plastic barcodes to determine system or record
         */
        PRODUCT_DEPENDENT,
        /** Some messages, e.g. BaitSetup, don't include enough information to determine system of record, so
         * they must be processed in both.
         */
        BOTH
    }

    private final SystemOfRecord systemOfRecord;

    private static final Map<String, LabEventType> mapNameToType = new HashMap<String, LabEventType>();

    /**
     * One attempt at trying to make a very generic
     * {@link LabEvent} to handle lots of different
     * {@link LabEventName event names}
     * @param name
     * @param expectSourcesEmpty
     * @param expectTargetsEmpty
     * @param systemOfRecord
     */
    LabEventType(String name,
            boolean expectSourcesEmpty,
            boolean expectTargetsEmpty,
            SystemOfRecord systemOfRecord) {
        this.name = name;
        this.expectedEmptySources = expectSourcesEmpty;
        this.expectedEmptyTargets = expectTargetsEmpty;
        this.systemOfRecord = systemOfRecord;
    }

    public String getName() {
        return name;
    }

    public boolean isExpectedEmptySources() {
        return expectedEmptySources;
    }

    public boolean isExpectedEmptyTargets() {
        return expectedEmptyTargets;
    }

    public SystemOfRecord getSystemOfRecord() {
        return systemOfRecord;
    }

    public static LabEventType getByName(String name) {
        if(mapNameToType.isEmpty()) {
            for (LabEventType labEventType : LabEventType.values()) {
                mapNameToType.put(labEventType.getName(), labEventType);
            }
        }
        return mapNameToType.get(name);
    }
}

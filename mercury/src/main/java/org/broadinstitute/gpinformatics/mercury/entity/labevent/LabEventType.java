package org.broadinstitute.gpinformatics.mercury.entity.labevent;

import java.util.HashMap;
import java.util.Map;

/**
 * Properties common to all events of a particular message type
 */
public enum LabEventType {

    // Preflight
    PREFLIGHT_CLEANUP ("PreFlightCleanup", true, true, SystemOfRecord.SQUID, false),
    PREFLIGHT_PICO_SETUP ("PreflightPicoSetup", true, true, SystemOfRecord.SQUID, false),
    PREFLIGHT_NORMALIZATION("PreflightNormalization", true, true, SystemOfRecord.SQUID, false),
    PREFLIGHT_POST_NORM_PICO_SETUP ("PreflightPostNormPicoSetup", true, true, SystemOfRecord.SQUID, false),

    // Shearing
    SHEARING_TRANSFER("ShearingTransfer", false, true, SystemOfRecord.SQUID, false),
    PLATING_TO_SHEARING_TUBES("PlatingToShearingTubes", false, true, SystemOfRecord.SQUID, false),
    COVARIS_LOADED("CovarisLoaded", false, true, SystemOfRecord.SQUID, false),
    POST_SHEARING_TRANSFER_CLEANUP("PostShearingTransferCleanup", false, true, SystemOfRecord.SQUID, false),
    SHEARING_QC ("ShearingQC", false, true, SystemOfRecord.SQUID, false),

    // Library construction
    END_REPAIR ("EndRepair", true, true, SystemOfRecord.SQUID, false),
    POST_END_REPAIR_THERMO_CYCLER_LOADED("PostEndRepairThermoCyclerLoaded", false, true, SystemOfRecord.SQUID, false),
    END_REPAIR_CLEANUP ("EndRepairCleanup", true, true, SystemOfRecord.SQUID, false),
    A_BASE ("ABase", true, true, SystemOfRecord.SQUID, false),
    POST_ABASE_THERMO_CYCLER_LOADED("PostAbaseThermoCyclerLoaded", false, true, SystemOfRecord.SQUID, false),
    A_BASE_CLEANUP ("ABaseCleanup", true, true, SystemOfRecord.SQUID, false),
    INDEXED_ADAPTER_LIGATION ("IndexedAdapterLigation", true, false, SystemOfRecord.SQUID, false),
    POST_INDEXED_ADAPTER_LIGATION_THERMO_CYCLER_LOADED("PostIndexedAdapterLigationThermoCyclerLoaded", true, true, SystemOfRecord.SQUID, false),
    ADAPTER_LIGATION_CLEANUP ("AdapterLigationCleanup", false, true, SystemOfRecord.SQUID, false),
    // no longer used, needed to import past messages
    WGS_ADAPTER_LIGATION_CLEANUP ("WGSAdapterLigationCleanup", false, true, SystemOfRecord.SQUID, false),
    POND_ENRICHMENT ("PondEnrichment", true, true, SystemOfRecord.SQUID, false),
    INDEX_P5_POND_ENRICHMENT ("IndexP5PondEnrichment", true, true, SystemOfRecord.SQUID, false),
    POST_POND_ENRICHMENT_THERMO_CYCLER_LOADED("PostPondEnrichmentThermoCyclerLoaded", true, true, SystemOfRecord.SQUID, false),
    POND_ENRICHMENT_CLEANUP ("HybSelPondEnrichmentCleanup", false, true, SystemOfRecord.SQUID, false),
    POND_REGISTRATION ("PondRegistration", false, true, SystemOfRecord.SQUID, false),
    POND_PICO("PondPico", false, true, SystemOfRecord.SQUID, false),

    // Hybrid Selection
    PRE_SELECTION_POOL ("PreSelectionPool", false, true, SystemOfRecord.SQUID, false),
    HYBRIDIZATION ("Hybridization", false, true, SystemOfRecord.SQUID, false),
    POST_HYBRIDIZATION_THERMO_CYCLER_LOADED("PostHybridizationThermoCyclerLoaded", false, true, SystemOfRecord.SQUID, false),
    BAIT_SETUP ("BaitSetup", true, true, SystemOfRecord.SQUID, false), // todo jmt change to BOTH
    BAIT_ADDITION ("BaitAddition", true, false, SystemOfRecord.SQUID, false),
    BEAD_ADDITION ("BeadAddition", true, true, SystemOfRecord.SQUID, false),
    AP_WASH ("APWash", true, true, SystemOfRecord.SQUID, false),
    GS_WASH_1 ("GSWash1", true, true, SystemOfRecord.SQUID, false),
    GS_WASH_2 ("GSWash2", true, true, SystemOfRecord.SQUID, false),
    GS_WASH_3 ("GSWash3", true, true, SystemOfRecord.SQUID, false),
    GS_WASH_4 ("GSWash4", true, true, SystemOfRecord.SQUID, false),
    GS_WASH_5 ("GSWash5", true, true, SystemOfRecord.SQUID, false),
    GS_WASH_6 ("GSWash6", true, true, SystemOfRecord.SQUID, false),
    CATCH_ENRICHMENT_SETUP ("CatchEnrichmentSetup", true, true, SystemOfRecord.SQUID, false),
    POST_CATCH_ENRICHMENT_SETUP_THERMO_CYCLER_LOADED ("PostCatchEnrichmentSetupThermoCyclerLoaded", true, true, SystemOfRecord.SQUID, false),
    CATCH_ENRICHMENT_CLEANUP ("CatchEnrichmentCleanup", false, true, SystemOfRecord.SQUID, false),
    NORMALIZED_CATCH_REGISTRATION ("NormalizedCatchRegistration", false, true, SystemOfRecord.SQUID, false),
    CATCH_PICO ("CatchPico", false, true, SystemOfRecord.SQUID, false),

    // QTP
    POOLING_TRANSFER ("PoolingTransfer", false, true, SystemOfRecord.SQUID, false),
    ECO_TRANSFER ("EcoTransfer", false, true, SystemOfRecord.SQUID, false),
    NORMALIZATION_TRANSFER ("NormalizationTransfer", false, true, SystemOfRecord.SQUID, false),
    DENATURE_TRANSFER ("DenatureTransfer", false, true, SystemOfRecord.SQUID, false),
    STRIP_TUBE_B_TRANSFER ("StripTubeBTransfer", true, false, SystemOfRecord.SQUID, false),
    FLOWCELL_TRANSFER ("FlowcellTransfer", true, false, SystemOfRecord.SQUID, false),
    FLOWCELL_LOADED ("FlowcellLoaded", true, false, SystemOfRecord.SQUID, false),

    // Sage
    SAGE_LOADING ("SageLoading", true, false, SystemOfRecord.SQUID, false),
    SAGE_LOADED ("SageLoaded", true, false, SystemOfRecord.SQUID, false),
    SAGE_UNLOADING ("SageUnloading", true, false, SystemOfRecord.SQUID, false),
    SAGE_CLEANUP ("SageCleanup", true, false, SystemOfRecord.SQUID, false),

    // Fluidigm
    FLUIDIGM_SAMPLE_INPUT ("FluidigmSampleInput", true, false, SystemOfRecord.SQUID, false),
    FLUIDIGM_INDEXED_ADAPTER_INPUT ("FluidigmIndexedAdapterInput", true, false, SystemOfRecord.SQUID, false),
    FLUIDIGM_INDEXED_ADAPTER_PCR ("FluidigmIndexedAdapterPCR", true, false, SystemOfRecord.SQUID, false),
    FLUIDIGM_PRODUCT_DILUTION ("FluidigmProductDilution", true, false, SystemOfRecord.SQUID, false),
    FLUIDIGM_HARVESTING_TO_RACK ("FluidigmHarvestingToRack", true, false, SystemOfRecord.SQUID, false),
    FLUIDIGM_HARVESTING ("FluidigmHarvesting", true, false, SystemOfRecord.SQUID, false),
    FLUIDIGM_FINAL_TRANSFER("FluidigmFinalTransfer", true, false, SystemOfRecord.SQUID, false),

    // Jump
    JUMP_INITIAL_SHEARING_TRANSFER("JumpInitialShearingTransfer", false, true, SystemOfRecord.SQUID, false),
    JUMP_INITIAL_SIZE_SELECTION("JumpInitialSizeSelection", false, true, SystemOfRecord.SQUID, false),
    JUMP_END_REPAIR_1("JumpEndRepair1", true, false, SystemOfRecord.SQUID, false),
    JUMP_INITIAL_SIZE_SELECTION_QC("JumpInitialSizeSelectionQC", false, true, SystemOfRecord.SQUID, false),
    JUMP_END_REPAIR_1_CLEANUP("JumpEndRepair1Cleanup", false, true, SystemOfRecord.SQUID, false),
    JUMP_END_REPAIR_1_QC("JumpEndRepair1QC", false, true, SystemOfRecord.SQUID, false),
    JUMP_CIRCULARIZATION_SETUP("JumpCircularizationSetup", false, true, SystemOfRecord.SQUID, false),
    JUMP_EXONUCLEASE_TREATMENT("JumpExonucleaseTreatment", true, false, SystemOfRecord.SQUID, false),
    JUMP_EXO_INACTIVATION("JumpExoInactivation", false, true, SystemOfRecord.SQUID, false),
    JUMP_POST_CIRC_SIZE_SELECTION("JumpPostCircSizeSelection", false, true, SystemOfRecord.SQUID, false),
    JUMP_IMMOBILIZATION("JumpImmobilization", true, false, SystemOfRecord.SQUID, false),
    JUMP_IMMOBILIZATION_WASH("JumpImmobilizationWash", false, true, SystemOfRecord.SQUID, false),
    JUMP_END_REPAIR_2("JumpEndRepair2", false, true, SystemOfRecord.SQUID, false),
    JUMP_END_REPAIR_2_WASH("JumpEndRepair2Wash", false, true, SystemOfRecord.SQUID, false),
    JUMP_A_TAILING("JumpATailing", false, true, SystemOfRecord.SQUID, false),
    JUMP_A_TAILING_WASH("JumpATailingWash", false, true, SystemOfRecord.SQUID, false),
    JUMP_ADD_INDEX("JumpAddIndex", false, true, SystemOfRecord.SQUID, false),
    JUMP_ADAPTER_LIGATION("JumpAdapterLigation", false, true, SystemOfRecord.SQUID, false),
    JUMP_ADAPTER_LIGATION_WASH("JumpAdapterLigationWash", false, true, SystemOfRecord.SQUID, false),
    JUMP_AMPLIFICATION("JumpAmplification", false, true, SystemOfRecord.SQUID, false),
    JUMP_FINAL_LIBRARY_ELUTION("JumpFinalLibraryElution", false, true, SystemOfRecord.SQUID, false),
    JUMP_FINAL_LIBRARY_SIZE_SELECTION("JumpFinalLibrarySizeSelection", false, true, SystemOfRecord.SQUID, false),
    JUMP_FINAL_LIBRARY_QC("JumpFinalLibraryQC", false, true, SystemOfRecord.SQUID, false),
    JUMP_ENRICHED_LIBRARY_REGISTRATION("JumpEnrichedLibraryRegistration", false, true, SystemOfRecord.SQUID, false),

    // BSP Pico
    PICO_PLATING_BUCKET("PicoPlatingBucket", true, true, SystemOfRecord.MERCURY, false),
    PICO_PLATING_QC ("PicoPlatingQC", true,true, SystemOfRecord.MERCURY, false),
    PICO_DILUTION_TRANSFER ("PicoDilutionTransfer", true, true, SystemOfRecord.MERCURY, false),
    PICO_BUFFER_ADDITION("PicoBufferAddition", true, true, SystemOfRecord.MERCURY, false),
    PICO_MICROFLUOR_TRANSFER("PicoMicrofluorTransfer", true, true, SystemOfRecord.MERCURY, false),
    PICO_STANDARDS_TRANSFER ("PicoStandardsTransfer", true, true, SystemOfRecord.MERCURY, true),
    PICO_PLATING_POST_NORM_PICO("PicoPlatingPostNorm", true, true, SystemOfRecord.MERCURY, false),

    // Dried Blood Spot
    DBS_SAMPLE_PUNCH ("DBSSamplePunch", false, true, SystemOfRecord.MERCURY, false),
    DBS_INCUBATION_MIX ("DBSIncubationMix", true, false, SystemOfRecord.MERCURY, false),
    DBS_LYSIS_BUFFER ("DBSLysisBuffer", true, false, SystemOfRecord.MERCURY, false),
    DBS_MAGNETIC_RESIN ("DBSMagneticResin", true, false, SystemOfRecord.MERCURY, false),
    DBS_1ST_PURIFICATION ("DBS1stPurification", false, true, SystemOfRecord.MERCURY, false),
    DBS_WASH_BUFFER ("DBSWashBuffer", true, false, SystemOfRecord.MERCURY, false),
    DBS_2ND_PURIFICATION ("DBS2ndPurification", false, true, SystemOfRecord.MERCURY, false),
    DBS_ELUTION_BUFFER ("DBSElutionBuffer", false, true, SystemOfRecord.MERCURY, false),
    DBS_FINAL_TRANSFER ("DBSFinalTransfer", false, true, SystemOfRecord.MERCURY, false),

    // TruSeq Custom Amplicon
    TSCA_POST_NORM_TRANSFER("TSCAPostNormTransfer", false, true, SystemOfRecord.SQUID, false),
    TSCA_HYB_SET_UP("TSCAHybSetUp", false, true, SystemOfRecord.SQUID, false),
    TSCA_CAT_ADDITION("TSCACATAddition", false, true, SystemOfRecord.SQUID, false),
    TSCA_HYBRIDIZATION_CLEANUP("TSCAHybridizationCleanUp", false, true, SystemOfRecord.SQUID, false),
    TSCA_FLP_PREP("TSCAFLPPrep", false, true, SystemOfRecord.SQUID, false),
    TSCA_SW1_ADDITION1("TSCASW1Addition1", false, true, SystemOfRecord.SQUID, false),
    TSCA_SW1_ADDITION2("TSCASW1Addition2", false, true, SystemOfRecord.SQUID, false),
    TSCA_UB1_ADDITION("TSCAUB1Addition", false, true, SystemOfRecord.SQUID, false),
    TSCA_EXTENSION_LIGATION_SETUP("TSCAExtensionLigationSetup", false, true, SystemOfRecord.SQUID, false),
    TSCA_INDEXED_AMPLICON_PCR("TSCAIndexedAmpliconPCR", false, true, SystemOfRecord.SQUID, false),
    TSCA_INDEX_ADDITION("TSCAIndexAddition", false, true, SystemOfRecord.SQUID, false),
    TSCA_AMPLICON_REGISTRATION("TSCAAmpliconRegistration", false, true, SystemOfRecord.SQUID, false),
    TSCA_AMPLICON_PCR_CLEANUP("TSCAAmpliconPCRCleanup", false, true, SystemOfRecord.SQUID, false),
    TSCA_POOL_CREATION("TSCAPoolCreation", false, true, SystemOfRecord.SQUID, false),

    // 16S
    ILLUMINA_16S_TO_PRIMER_TRANSFER("Illumina16StoPrimerTransfer", false, true, SystemOfRecord.SQUID, false),
    ILLUMINA_16S_PCR_PRODUCT_TRANSFER("Illumina16SPcrProductTransfer", false, true, SystemOfRecord.SQUID, false),

    // cDNA dUTP
    POLY_A_TRANSFER("PolyATransfer", false, true, SystemOfRecord.SQUID, false),
    POLY_A_SELECTION("PolyASelection", false, true, SystemOfRecord.SQUID, false),
    DNASE("Dnase", false, true, SystemOfRecord.SQUID, false),
    DNASE_CLEANUP("DnaseCleanup", false, true, SystemOfRecord.SQUID, false),
    FRAGMENTATION("Fragmentation", false, true, SystemOfRecord.SQUID, false),
    FRAGMENTATION_CLEANUP("FragmentationCleanup", false, true, SystemOfRecord.SQUID, false),
    FIRST_STRAND("FirstStrand", false, true, SystemOfRecord.SQUID, false),
    FIRST_STRAND_CLEANUP("FirstStrandCleanup", false, true, SystemOfRecord.SQUID, false),
    SECOND_STRAND("SecondStrand", false, true, SystemOfRecord.SQUID, false),
    SECOND_STRAND_CLEANUP("SecondStrandCleanup", false, true, SystemOfRecord.SQUID, false),
    USER("USER", false, true, SystemOfRecord.SQUID, false),
    CDNA_PONDENRICHMENT_CLEANUP("cDNAPondEnrichmentCleanup", false, true, SystemOfRecord.SQUID, false),

    // cDNA TruSeq
    POLY_A_SELECTION_TS("PolyASelectionTS", false, true, SystemOfRecord.SQUID, false),
    FIRST_STRAND_TS("FirstStrandTS", false, true, SystemOfRecord.SQUID, false),
    SECOND_STRAND_TS("SecondStrandTS", false, true, SystemOfRecord.SQUID, false),
    SECOND_STRAND_CLEANUP_TS("SecondStrandCleanupTS", false, true, SystemOfRecord.SQUID, false),
    END_REPAIR_TS("EndRepairTS", false, true, SystemOfRecord.SQUID, false),
    END_REPAIR_CLEANUP_TS("EndRepairCleanupTS", false, true, SystemOfRecord.SQUID, false),
    A_BASE_TS("ABaseTS", false, true, SystemOfRecord.SQUID, false),
    INDEXED_ADAPTER_LIGATION_TS("IndexedAdapterLigationTS", false, true, SystemOfRecord.SQUID, false),
    ADAPTER_LIGATION_CLEANUP_TS("AdapterLigationCleanupTS", false, true, SystemOfRecord.SQUID, false),
    ENRICHMENT_TS("EnrichmentTS", false, true, SystemOfRecord.SQUID, false),
    ENRICHMENT_CLEANUP_TS("EnrichmentCleanupTS", false, true, SystemOfRecord.SQUID, false),

    // Illumina Genome Network
    IGN_PRE_NORM_TRANSFER("IGNPreNormTransfer", false, true, SystemOfRecord.SQUID, false),
    IGN_NORM_TRANSFER("IGNNormTransfer", false, true, SystemOfRecord.SQUID, false),
    IGN_INCOMING_PICO("IGNIncomingPico", false, true, SystemOfRecord.SQUID, false),
    IGN_GAP_ALIQUOT("IGNGapAliquot", false, true, SystemOfRecord.SQUID, false),

    VIRAL_PCR_PRODUCT_POOL_TRANSFER("ViralPcrProductPoolTransfer", false, true, SystemOfRecord.SQUID, false),
    ONE_STEP_RT_PCR_PRODUCT_POOL_TRANSFER("OneStepRtPcrProductPoolTransfer", false, true, SystemOfRecord.SQUID, false),

    // From BSP UI
    SAMPLES_EXTRACTION_START("SamplesExtractionStart", false, true, SystemOfRecord.MERCURY, false),
    SAMPLES_EXTRACTION_END_TRANSFER("SamplesExtractionEndTransfer", false, true, SystemOfRecord.MERCURY, false),
    SAMPLES_NORMALIZATION_TRANSFER("SamplesNormalizationTransfer", false, true, SystemOfRecord.MERCURY, false),
    SAMPLES_PLATING_TO_COVARIS("SamplesPlatingToCovaris", false, true, SystemOfRecord.MERCURY, false),

    // mRRBS
    MRRBS_GENOMIC_TRANSFER("mRRBSGenomicTransfer", false, true, SystemOfRecord.SQUID, false),
    MRRBS_INDEXING("mRRBSIndexing", false, true, SystemOfRecord.SQUID, false),
    MRRBS_NORM_LIBS("mRRBSNormLibs", false, true, SystemOfRecord.SQUID, false),
    MRRBS_FINAL_PRODUCT_POOL("mRRBSFinalProductPool", false, true, SystemOfRecord.SQUID, false),

    /**
     * TODO SGM  the following names are place holders.  They will be re-evaluated as development of
     */
    SHEARING_BUCKET("ShearingBucket", true, true, SystemOfRecord.MERCURY, false),
//    SHEARING_BUCKET_ENTRY ("ShearingBucketEntry", true, true, SystemOfRecord.MERCURY),
//    SHEARING_BUCKET_EXIT ("ShearingBucketExit", true, true, SystemOfRecord.MERCURY),
    ;

    private final String name;

    private final boolean expectedEmptySources;

    private final boolean expectedEmptyTargets;
    
    private boolean createSources;

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
     * @param name the event name in the message
     * @param expectSourcesEmpty whether it's an error for the source vessels to have content
     * @param expectTargetsEmpty whether it's an error for the target vessels to have content
     * @param systemOfRecord which system is responsible for handling the message
     * @param createSources whether sources should be create, if they don't exist
     */
    LabEventType(String name,
            boolean expectSourcesEmpty,
            boolean expectTargetsEmpty,
            SystemOfRecord systemOfRecord,
            boolean createSources) {
        this.name = name;
        this.expectedEmptySources = expectSourcesEmpty;
        this.expectedEmptyTargets = expectTargetsEmpty;
        this.systemOfRecord = systemOfRecord;
        this.createSources = createSources;
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

    public boolean isCreateSources() {
        return createSources;
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

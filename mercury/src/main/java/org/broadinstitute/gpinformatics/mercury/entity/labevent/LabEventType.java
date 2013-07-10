package org.broadinstitute.gpinformatics.mercury.entity.labevent;

import java.util.HashMap;
import java.util.Map;

/**
 * Properties common to all events of a particular message type
 */
public enum LabEventType {

    // Preflight
    PREFLIGHT_CLEANUP("PreFlightCleanup", true, true, SystemOfRecord.SQUID, false, PlasticToValidate.SOURCE, PipelineTransformation.NONE),
    PREFLIGHT_PICO_SETUP("PreflightPicoSetup", true, true, SystemOfRecord.SQUID, false, PlasticToValidate.SOURCE, PipelineTransformation.NONE),
    PREFLIGHT_NORMALIZATION("PreflightNormalization", true, true, SystemOfRecord.SQUID, false, PlasticToValidate.SOURCE, PipelineTransformation.NONE),
    PREFLIGHT_POST_NORM_PICO_SETUP("PreflightPostNormPicoSetup", true, true, SystemOfRecord.SQUID, false,
            PlasticToValidate.SOURCE, PipelineTransformation.NONE),

    // Shearing
    SHEARING_TRANSFER("ShearingTransfer", false, true, SystemOfRecord.WORKFLOW_DEPENDENT, false,
            PlasticToValidate.SOURCE, PipelineTransformation.NONE),
    COVARIS_LOADED("CovarisLoaded", false, true, SystemOfRecord.WORKFLOW_DEPENDENT, false,
            PlasticToValidate.SOURCE, PipelineTransformation.NONE),
    POST_SHEARING_TRANSFER_CLEANUP("PostShearingTransferCleanup", false, true, SystemOfRecord.WORKFLOW_DEPENDENT, false,
            PlasticToValidate.SOURCE, PipelineTransformation.NONE),
    SHEARING_QC("ShearingQC", false, true, SystemOfRecord.WORKFLOW_DEPENDENT, false,
            PlasticToValidate.SOURCE, PipelineTransformation.NONE),

    // Library construction
    END_REPAIR("EndRepair", true, true, SystemOfRecord.WORKFLOW_DEPENDENT, false,
            PlasticToValidate.SOURCE, PipelineTransformation.NONE),
    POST_END_REPAIR_THERMO_CYCLER_LOADED("PostEndRepairThermoCyclerLoaded", false, true, SystemOfRecord.WORKFLOW_DEPENDENT, false,
            PlasticToValidate.SOURCE, PipelineTransformation.NONE),
    END_REPAIR_CLEANUP("EndRepairCleanup", true, true, SystemOfRecord.WORKFLOW_DEPENDENT, false,
            PlasticToValidate.SOURCE, PipelineTransformation.NONE),
    A_BASE("ABase", true, true, SystemOfRecord.WORKFLOW_DEPENDENT, false,
            PlasticToValidate.SOURCE, PipelineTransformation.NONE),
    POST_ABASE_THERMO_CYCLER_LOADED("PostAbaseThermoCyclerLoaded", false, true, SystemOfRecord.WORKFLOW_DEPENDENT, false,
            PlasticToValidate.SOURCE, PipelineTransformation.NONE),
    A_BASE_CLEANUP("ABaseCleanup", true, true, SystemOfRecord.WORKFLOW_DEPENDENT, false,
            PlasticToValidate.SOURCE, PipelineTransformation.NONE),
    INDEXED_ADAPTER_LIGATION("IndexedAdapterLigation", true, false, SystemOfRecord.WORKFLOW_DEPENDENT, false,
            PlasticToValidate.TARGET, PipelineTransformation.LIGATION),
    POST_INDEXED_ADAPTER_LIGATION_THERMO_CYCLER_LOADED("PostIndexedAdapterLigationThermoCyclerLoaded", true, true,
            SystemOfRecord.WORKFLOW_DEPENDENT, false, PlasticToValidate.SOURCE, PipelineTransformation.NONE),
    ADAPTER_LIGATION_CLEANUP("AdapterLigationCleanup", false, true, SystemOfRecord.WORKFLOW_DEPENDENT, false,
            PlasticToValidate.SOURCE, PipelineTransformation.NONE),
    // no longer used, needed to import past messages
    WGS_ADAPTER_LIGATION_CLEANUP("WGSAdapterLigationCleanup", false, true, SystemOfRecord.WORKFLOW_DEPENDENT, false,
            PlasticToValidate.SOURCE, PipelineTransformation.NONE),
    POND_ENRICHMENT("PondEnrichment", true, true, SystemOfRecord.WORKFLOW_DEPENDENT, false,
            PlasticToValidate.SOURCE, PipelineTransformation.PCR),
    INDEX_P5_POND_ENRICHMENT("IndexP5PondEnrichment", true, true, SystemOfRecord.WORKFLOW_DEPENDENT, false,
            PlasticToValidate.TARGET, PipelineTransformation.PCR),
    POST_POND_ENRICHMENT_THERMO_CYCLER_LOADED("PostPondEnrichmentThermoCyclerLoaded", true, true, SystemOfRecord.WORKFLOW_DEPENDENT,
            false, PlasticToValidate.SOURCE, PipelineTransformation.NONE),
    POND_ENRICHMENT_CLEANUP("HybSelPondEnrichmentCleanup", false, true, SystemOfRecord.WORKFLOW_DEPENDENT, false,
            PlasticToValidate.SOURCE, PipelineTransformation.NONE),
    POND_REGISTRATION("PondRegistration", false, true, SystemOfRecord.WORKFLOW_DEPENDENT, false,
            PlasticToValidate.SOURCE, PipelineTransformation.NONE),
    POND_PICO("PondPico", false, true, SystemOfRecord.WORKFLOW_DEPENDENT, false,
            PlasticToValidate.SOURCE, PipelineTransformation.NONE),

    // Hybrid Selection
    PRE_SELECTION_POOL("PreSelectionPool", false, true, SystemOfRecord.WORKFLOW_DEPENDENT, false,
            PlasticToValidate.SOURCE, PipelineTransformation.NONE),
    HYBRIDIZATION("Hybridization", false, true, SystemOfRecord.WORKFLOW_DEPENDENT, false,
            PlasticToValidate.SOURCE, PipelineTransformation.NONE),
    POST_HYBRIDIZATION_THERMO_CYCLER_LOADED("PostHybridizationThermoCyclerLoaded", false, true, SystemOfRecord.WORKFLOW_DEPENDENT, false,
            PlasticToValidate.SOURCE, PipelineTransformation.NONE),
    BAIT_SETUP("BaitSetup", true, true, SystemOfRecord.BOTH, false,
            PlasticToValidate.SOURCE, PipelineTransformation.NONE),
    BAIT_ADDITION("BaitAddition", true, false, SystemOfRecord.WORKFLOW_DEPENDENT, false,
            PlasticToValidate.TARGET, PipelineTransformation.NONE),
    BEAD_ADDITION("BeadAddition", true, true, SystemOfRecord.WORKFLOW_DEPENDENT, false,
            PlasticToValidate.SOURCE, PipelineTransformation.NONE),
    AP_WASH("APWash", true, true, SystemOfRecord.WORKFLOW_DEPENDENT, false,
            PlasticToValidate.SOURCE, PipelineTransformation.NONE),
    GS_WASH_1("GSWash1", true, true, SystemOfRecord.WORKFLOW_DEPENDENT, false,
            PlasticToValidate.SOURCE, PipelineTransformation.NONE),
    GS_WASH_2("GSWash2", true, true, SystemOfRecord.WORKFLOW_DEPENDENT, false,
            PlasticToValidate.SOURCE, PipelineTransformation.NONE),
    GS_WASH_3("GSWash3", true, true, SystemOfRecord.WORKFLOW_DEPENDENT, false,
            PlasticToValidate.SOURCE, PipelineTransformation.NONE),
    GS_WASH_4("GSWash4", true, true, SystemOfRecord.WORKFLOW_DEPENDENT, false,
            PlasticToValidate.SOURCE, PipelineTransformation.NONE),
    GS_WASH_5("GSWash5", true, true, SystemOfRecord.WORKFLOW_DEPENDENT, false,
            PlasticToValidate.SOURCE, PipelineTransformation.NONE),
    GS_WASH_6("GSWash6", true, true, SystemOfRecord.WORKFLOW_DEPENDENT, false,
            PlasticToValidate.SOURCE, PipelineTransformation.NONE),
    CATCH_ENRICHMENT_SETUP("CatchEnrichmentSetup", true, true, SystemOfRecord.WORKFLOW_DEPENDENT, false,
            PlasticToValidate.SOURCE, PipelineTransformation.NONE),
    POST_CATCH_ENRICHMENT_SETUP_THERMO_CYCLER_LOADED("PostCatchEnrichmentSetupThermoCyclerLoaded", true, true, SystemOfRecord.WORKFLOW_DEPENDENT, false,
            PlasticToValidate.SOURCE, PipelineTransformation.NONE),
    CATCH_ENRICHMENT_CLEANUP("CatchEnrichmentCleanup", false, true, SystemOfRecord.WORKFLOW_DEPENDENT, false,
            PlasticToValidate.SOURCE, PipelineTransformation.NONE),
    NORMALIZED_CATCH_REGISTRATION("NormalizedCatchRegistration", false, true, SystemOfRecord.WORKFLOW_DEPENDENT, false,
            PlasticToValidate.SOURCE, PipelineTransformation.PCR), // todo jmt should this pipeline transformation be on one of the previous plates?
    CATCH_PICO("CatchPico", false, true, SystemOfRecord.WORKFLOW_DEPENDENT, false,
            PlasticToValidate.SOURCE, PipelineTransformation.NONE),

    // QTP
    POOLING_TRANSFER("PoolingTransfer", false, true, SystemOfRecord.WORKFLOW_DEPENDENT, false,
            PlasticToValidate.SOURCE, PipelineTransformation.NONE),
    ECO_TRANSFER("EcoTransfer", false, true, SystemOfRecord.WORKFLOW_DEPENDENT, false,
            PlasticToValidate.SOURCE, PipelineTransformation.NONE),
    NORMALIZATION_TRANSFER("NormalizationTransfer", false, true, SystemOfRecord.WORKFLOW_DEPENDENT, false,
            PlasticToValidate.SOURCE, PipelineTransformation.NONE),
    DENATURE_TRANSFER("DenatureTransfer", false, true, SystemOfRecord.WORKFLOW_DEPENDENT, false,
            PlasticToValidate.SOURCE, PipelineTransformation.NONE),
    STRIP_TUBE_B_TRANSFER("StripTubeBTransfer", true, false, SystemOfRecord.WORKFLOW_DEPENDENT, false,
            PlasticToValidate.SOURCE, PipelineTransformation.NONE),

    // HiSeq 2000
    FLOWCELL_TRANSFER("FlowcellTransfer", true, false, SystemOfRecord.WORKFLOW_DEPENDENT, false,
            PlasticToValidate.SOURCE, PipelineTransformation.NONE),
    FLOWCELL_LOADED("FlowcellLoaded", true, false, SystemOfRecord.WORKFLOW_DEPENDENT, false,
            PlasticToValidate.SOURCE, PipelineTransformation.NONE),

    // For HiSeq 2500
    DENATURE_TO_FLOWCELL_TRANSFER("DenatureToFlowcellTransfer", false, true, SystemOfRecord.WORKFLOW_DEPENDENT, false,
            PlasticToValidate.SOURCE, PipelineTransformation.NONE),
    DENATURE_TO_DILUTION_TRANSFER("DenatureToDilutionTransfer", false, true, SystemOfRecord.WORKFLOW_DEPENDENT, false,
            PlasticToValidate.SOURCE, PipelineTransformation.NONE),
    DILUTION_TO_FLOWCELL_TRANSFER("DilutionToFlowcellTransfer", false, true, SystemOfRecord.WORKFLOW_DEPENDENT, false,
            PlasticToValidate.SOURCE, PipelineTransformation.NONE),

    // for MiSeq
    DENATURE_TO_REAGENT_KIT_TRANSFER("MiseqReagentKitLoading", false, true, SystemOfRecord.WORKFLOW_DEPENDENT, false,
            PlasticToValidate.SOURCE, PipelineTransformation.NONE),
    REAGENT_KIT_TO_FLOWCELL_TRANSFER("ReagentKitToFlowcellTransfer", false, true, SystemOfRecord.WORKFLOW_DEPENDENT, false,
            PlasticToValidate.SOURCE, PipelineTransformation.NONE),

    // Sage
    SAGE_LOADING("SageLoading", true, false, SystemOfRecord.SQUID, false, PlasticToValidate.SOURCE, PipelineTransformation.NONE),
    SAGE_LOADED("SageLoaded", true, false, SystemOfRecord.SQUID, false, PlasticToValidate.SOURCE, PipelineTransformation.NONE),
    SAGE_UNLOADING("SageUnloading", true, false, SystemOfRecord.SQUID, false, PlasticToValidate.SOURCE, PipelineTransformation.NONE),
    SAGE_CLEANUP("SageCleanup", true, false, SystemOfRecord.SQUID, false, PlasticToValidate.SOURCE, PipelineTransformation.SIZE_SELECTION),

    // Fluidigm
    FLUIDIGM_SAMPLE_INPUT("FluidigmSampleInput", true, false, SystemOfRecord.SQUID, false, PlasticToValidate.SOURCE, PipelineTransformation.NONE),
    FLUIDIGM_INDEXED_ADAPTER_INPUT("FluidigmIndexedAdapterInput", true, false, SystemOfRecord.SQUID, false,
            PlasticToValidate.SOURCE, PipelineTransformation.LIGATION),
    FLUIDIGM_INDEXED_ADAPTER_PCR("FluidigmIndexedAdapterPCR", true, false, SystemOfRecord.SQUID, false,
            PlasticToValidate.SOURCE, PipelineTransformation.PCR),
    FLUIDIGM_PRODUCT_DILUTION("FluidigmProductDilution", true, false, SystemOfRecord.SQUID, false,
            PlasticToValidate.SOURCE, PipelineTransformation.NONE),
    FLUIDIGM_HARVESTING_TO_RACK("FluidigmHarvestingToRack", true, false, SystemOfRecord.SQUID, false,
            PlasticToValidate.SOURCE, PipelineTransformation.NONE),
    FLUIDIGM_HARVESTING("FluidigmHarvesting", true, false, SystemOfRecord.SQUID, false, PlasticToValidate.SOURCE, PipelineTransformation.NONE),
    FLUIDIGM_FINAL_TRANSFER("FluidigmFinalTransfer", true, false, SystemOfRecord.SQUID, false, PlasticToValidate.SOURCE, PipelineTransformation.NONE),

    // Jump
    JUMP_INITIAL_SHEARING_TRANSFER("JumpInitialShearingTransfer", false, true, SystemOfRecord.SQUID, false,
            PlasticToValidate.SOURCE, PipelineTransformation.NONE),
    JUMP_INITIAL_SIZE_SELECTION("JumpInitialSizeSelection", false, true, SystemOfRecord.SQUID, false,
            PlasticToValidate.SOURCE, PipelineTransformation.SIZE_SELECTION),
    JUMP_END_REPAIR_1("JumpEndRepair1", true, false, SystemOfRecord.SQUID, false, PlasticToValidate.SOURCE, PipelineTransformation.NONE),
    JUMP_INITIAL_SIZE_SELECTION_QC("JumpInitialSizeSelectionQC", false, true, SystemOfRecord.SQUID, false,
            PlasticToValidate.SOURCE, PipelineTransformation.NONE),
    JUMP_END_REPAIR_1_CLEANUP("JumpEndRepair1Cleanup", false, true, SystemOfRecord.SQUID, false,
            PlasticToValidate.SOURCE, PipelineTransformation.NONE),
    JUMP_END_REPAIR_1_QC("JumpEndRepair1QC", false, true, SystemOfRecord.SQUID, false, PlasticToValidate.SOURCE, PipelineTransformation.NONE),
    JUMP_CIRCULARIZATION_SETUP("JumpCircularizationSetup", false, true, SystemOfRecord.SQUID, false,
            PlasticToValidate.SOURCE, PipelineTransformation.NONE),
    JUMP_EXONUCLEASE_TREATMENT("JumpExonucleaseTreatment", true, false, SystemOfRecord.SQUID, false,
            PlasticToValidate.SOURCE, PipelineTransformation.NONE),
    JUMP_EXO_INACTIVATION("JumpExoInactivation", false, true, SystemOfRecord.SQUID, false, PlasticToValidate.SOURCE, PipelineTransformation.NONE),
    JUMP_POST_CIRC_SIZE_SELECTION("JumpPostCircSizeSelection", false, true, SystemOfRecord.SQUID, false,
            PlasticToValidate.SOURCE, PipelineTransformation.SIZE_SELECTION),
    JUMP_IMMOBILIZATION("JumpImmobilization", true, false, SystemOfRecord.SQUID, false, PlasticToValidate.SOURCE, PipelineTransformation.NONE),
    JUMP_IMMOBILIZATION_WASH("JumpImmobilizationWash", false, true, SystemOfRecord.SQUID, false,
            PlasticToValidate.SOURCE, PipelineTransformation.NONE),
    JUMP_END_REPAIR_2("JumpEndRepair2", false, true, SystemOfRecord.SQUID, false, PlasticToValidate.SOURCE, PipelineTransformation.NONE),
    JUMP_END_REPAIR_2_WASH("JumpEndRepair2Wash", false, true, SystemOfRecord.SQUID, false, PlasticToValidate.SOURCE, PipelineTransformation.NONE),
    JUMP_A_TAILING("JumpATailing", false, true, SystemOfRecord.SQUID, false, PlasticToValidate.SOURCE, PipelineTransformation.NONE),
    JUMP_A_TAILING_WASH("JumpATailingWash", false, true, SystemOfRecord.SQUID, false, PlasticToValidate.SOURCE, PipelineTransformation.NONE),
    JUMP_ADD_INDEX("JumpAddIndex", false, true, SystemOfRecord.SQUID, false, PlasticToValidate.SOURCE, PipelineTransformation.NONE),
    JUMP_ADAPTER_LIGATION("JumpAdapterLigation", false, true, SystemOfRecord.SQUID, false, PlasticToValidate.SOURCE, PipelineTransformation.LIGATION),
    JUMP_ADAPTER_LIGATION_WASH("JumpAdapterLigationWash", false, true, SystemOfRecord.SQUID, false,
            PlasticToValidate.SOURCE, PipelineTransformation.NONE),
    JUMP_AMPLIFICATION("JumpAmplification", false, true, SystemOfRecord.SQUID, false, PlasticToValidate.SOURCE, PipelineTransformation.NONE),
    JUMP_FINAL_LIBRARY_ELUTION("JumpFinalLibraryElution", false, true, SystemOfRecord.SQUID, false,
            PlasticToValidate.SOURCE, PipelineTransformation.NONE),
    JUMP_FINAL_LIBRARY_SIZE_SELECTION("JumpFinalLibrarySizeSelection", false, true, SystemOfRecord.SQUID, false,
            PlasticToValidate.SOURCE, PipelineTransformation.SIZE_SELECTION),
    JUMP_FINAL_LIBRARY_QC("JumpFinalLibraryQC", false, true, SystemOfRecord.SQUID, false, PlasticToValidate.SOURCE, PipelineTransformation.NONE),
    JUMP_ENRICHED_LIBRARY_REGISTRATION("JumpEnrichedLibraryRegistration", false, true, SystemOfRecord.SQUID, false,
            PlasticToValidate.SOURCE, PipelineTransformation.NONE),

    // BSP Pico
    PICO_PLATING_BUCKET("PicoPlatingBucket", true, true, SystemOfRecord.MERCURY, false, PlasticToValidate.SOURCE, PipelineTransformation.NONE),
    PICO_PLATING_QC("PicoPlatingQC", true, true, SystemOfRecord.MERCURY, false, PlasticToValidate.SOURCE, PipelineTransformation.NONE),
    PICO_DILUTION_TRANSFER("PicoDilutionTransfer", true, true, SystemOfRecord.MERCURY, false, PlasticToValidate.SOURCE, PipelineTransformation.NONE),
    PICO_BUFFER_ADDITION("PicoBufferAddition", true, true, SystemOfRecord.MERCURY, false, PlasticToValidate.SOURCE, PipelineTransformation.NONE),
    PICO_MICROFLUOR_TRANSFER("PicoMicrofluorTransfer", true, true, SystemOfRecord.MERCURY, false,
            PlasticToValidate.SOURCE, PipelineTransformation.NONE),
    PICO_STANDARDS_TRANSFER("PicoStandardsTransfer", true, true, SystemOfRecord.MERCURY, true,
            PlasticToValidate.SOURCE, PipelineTransformation.NONE),
    PICO_PLATING_POST_NORM_PICO("PicoPlatingPostNorm", true, true, SystemOfRecord.MERCURY, false,
            PlasticToValidate.SOURCE, PipelineTransformation.NONE),

    // Dried Blood Spot
    DBS_SAMPLE_PUNCH("DBSSamplePunch", false, true, SystemOfRecord.MERCURY, false, PlasticToValidate.SOURCE, PipelineTransformation.NONE),
    DBS_INCUBATION_MIX("DBSIncubationMix", true, false, SystemOfRecord.MERCURY, false, PlasticToValidate.SOURCE, PipelineTransformation.NONE),
    DBS_LYSIS_BUFFER("DBSLysisBuffer", true, false, SystemOfRecord.MERCURY, false, PlasticToValidate.SOURCE, PipelineTransformation.NONE),
    DBS_MAGNETIC_RESIN("DBSMagneticResin", true, false, SystemOfRecord.MERCURY, false, PlasticToValidate.SOURCE, PipelineTransformation.NONE),
    DBS_1ST_PURIFICATION("DBS1stPurification", false, true, SystemOfRecord.MERCURY, false, PlasticToValidate.SOURCE, PipelineTransformation.NONE),
    DBS_WASH_BUFFER("DBSWashBuffer", true, false, SystemOfRecord.MERCURY, false, PlasticToValidate.SOURCE, PipelineTransformation.NONE),
    DBS_2ND_PURIFICATION("DBS2ndPurification", false, true, SystemOfRecord.MERCURY, false, PlasticToValidate.SOURCE, PipelineTransformation.NONE),
    DBS_ELUTION_BUFFER("DBSElutionBuffer", false, true, SystemOfRecord.MERCURY, false, PlasticToValidate.SOURCE, PipelineTransformation.NONE),
    DBS_FINAL_TRANSFER("DBSFinalTransfer", false, true, SystemOfRecord.MERCURY, false, PlasticToValidate.SOURCE, PipelineTransformation.NONE),

    // TruSeq Custom Amplicon
    TSCA_POST_NORM_TRANSFER("TSCAPostNormTransfer", false, true, SystemOfRecord.SQUID, false, PlasticToValidate.SOURCE, PipelineTransformation.NONE),
    TSCA_HYB_SET_UP("TSCAHybSetUp", false, true, SystemOfRecord.SQUID, false, PlasticToValidate.SOURCE, PipelineTransformation.NONE),
    TSCA_CAT_ADDITION("TSCACATAddition", false, true, SystemOfRecord.SQUID, false, PlasticToValidate.SOURCE, PipelineTransformation.NONE),
    TSCA_HYBRIDIZATION_CLEANUP("TSCAHybridizationCleanUp", false, true, SystemOfRecord.SQUID, false,
            PlasticToValidate.SOURCE, PipelineTransformation.NONE),
    TSCA_FLP_PREP("TSCAFLPPrep", false, true, SystemOfRecord.SQUID, false, PlasticToValidate.SOURCE, PipelineTransformation.NONE),
    TSCA_SW1_ADDITION1("TSCASW1Addition1", false, true, SystemOfRecord.SQUID, false, PlasticToValidate.SOURCE, PipelineTransformation.NONE),
    TSCA_SW1_ADDITION2("TSCASW1Addition2", false, true, SystemOfRecord.SQUID, false, PlasticToValidate.SOURCE, PipelineTransformation.NONE),
    TSCA_UB1_ADDITION("TSCAUB1Addition", false, true, SystemOfRecord.SQUID, false, PlasticToValidate.SOURCE, PipelineTransformation.NONE),
    TSCA_EXTENSION_LIGATION_SETUP("TSCAExtensionLigationSetup", false, true, SystemOfRecord.SQUID, false,
            PlasticToValidate.SOURCE, PipelineTransformation.NONE),
    TSCA_INDEXED_AMPLICON_PCR("TSCAIndexedAmpliconPCR", false, true, SystemOfRecord.SQUID, false,
            PlasticToValidate.SOURCE, PipelineTransformation.NONE),
    TSCA_INDEX_ADDITION("TSCAIndexAddition", false, true, SystemOfRecord.SQUID, false, PlasticToValidate.SOURCE, PipelineTransformation.LIGATION),
    TSCA_AMPLICON_REGISTRATION("TSCAAmpliconRegistration", false, true, SystemOfRecord.SQUID, false,
            PlasticToValidate.SOURCE, PipelineTransformation.NONE),
    TSCA_AMPLICON_PCR_CLEANUP("TSCAAmpliconPCRCleanup", false, true, SystemOfRecord.SQUID, false,
            PlasticToValidate.SOURCE, PipelineTransformation.NONE),
    TSCA_POOL_CREATION("TSCAPoolCreation", false, true, SystemOfRecord.SQUID, false, PlasticToValidate.SOURCE, PipelineTransformation.NONE),

    // 16S
    ILLUMINA_16S_TO_PRIMER_TRANSFER("Illumina16StoPrimerTransfer", false, true, SystemOfRecord.SQUID, false,
            PlasticToValidate.SOURCE, PipelineTransformation.LIGATION),
    ILLUMINA_16S_PCR_PRODUCT_TRANSFER("Illumina16SPcrProductTransfer", false, true, SystemOfRecord.SQUID, false,
            PlasticToValidate.SOURCE, PipelineTransformation.NONE),

    // cDNA dUTP
    POLY_A_TRANSFER("PolyATransfer", false, true, SystemOfRecord.SQUID, false, PlasticToValidate.SOURCE, PipelineTransformation.NONE),
    POLY_A_SELECTION("PolyASelection", false, true, SystemOfRecord.SQUID, false, PlasticToValidate.SOURCE, PipelineTransformation.NONE),
    DNASE("Dnase", false, true, SystemOfRecord.SQUID, false, PlasticToValidate.SOURCE, PipelineTransformation.NONE),
    DNASE_CLEANUP("DnaseCleanup", false, true, SystemOfRecord.SQUID, false, PlasticToValidate.SOURCE, PipelineTransformation.NONE),
    FRAGMENTATION("Fragmentation", false, true, SystemOfRecord.SQUID, false, PlasticToValidate.SOURCE, PipelineTransformation.NONE),
    FRAGMENTATION_CLEANUP("FragmentationCleanup", false, true, SystemOfRecord.SQUID, false, PlasticToValidate.SOURCE, PipelineTransformation.NONE),
    FIRST_STRAND("FirstStrand", false, true, SystemOfRecord.SQUID, false, PlasticToValidate.SOURCE, PipelineTransformation.NONE),
    FIRST_STRAND_CLEANUP("FirstStrandCleanup", false, true, SystemOfRecord.SQUID, false, PlasticToValidate.SOURCE, PipelineTransformation.NONE),
    SECOND_STRAND("SecondStrand", false, true, SystemOfRecord.SQUID, false, PlasticToValidate.SOURCE, PipelineTransformation.NONE),
    SECOND_STRAND_CLEANUP("SecondStrandCleanup", false, true, SystemOfRecord.SQUID, false, PlasticToValidate.SOURCE, PipelineTransformation.NONE),
    USER("USER", false, true, SystemOfRecord.SQUID, false, PlasticToValidate.SOURCE, PipelineTransformation.NONE),
    CDNA_PONDENRICHMENT_CLEANUP("cDNAPondEnrichmentCleanup", false, true, SystemOfRecord.SQUID, false,
            PlasticToValidate.SOURCE, PipelineTransformation.PCR),

    // cDNA TruSeq
    POLY_A_SELECTION_TS("PolyASelectionTS", false, true, SystemOfRecord.SQUID, false, PlasticToValidate.SOURCE, PipelineTransformation.NONE),
    FIRST_STRAND_TS("FirstStrandTS", false, true, SystemOfRecord.SQUID, false, PlasticToValidate.SOURCE, PipelineTransformation.NONE),
    SECOND_STRAND_TS("SecondStrandTS", false, true, SystemOfRecord.SQUID, false, PlasticToValidate.SOURCE, PipelineTransformation.NONE),
    SECOND_STRAND_CLEANUP_TS("SecondStrandCleanupTS", false, true, SystemOfRecord.SQUID, false,
            PlasticToValidate.SOURCE, PipelineTransformation.NONE),
    END_REPAIR_TS("EndRepairTS", false, true, SystemOfRecord.SQUID, false, PlasticToValidate.SOURCE, PipelineTransformation.NONE),
    END_REPAIR_CLEANUP_TS("EndRepairCleanupTS", false, true, SystemOfRecord.SQUID, false, PlasticToValidate.SOURCE, PipelineTransformation.NONE),
    A_BASE_TS("ABaseTS", false, true, SystemOfRecord.SQUID, false, PlasticToValidate.SOURCE, PipelineTransformation.NONE),
    INDEXED_ADAPTER_LIGATION_TS("IndexedAdapterLigationTS", false, true, SystemOfRecord.SQUID, false,
            PlasticToValidate.SOURCE, PipelineTransformation.LIGATION),
    ADAPTER_LIGATION_CLEANUP_TS("AdapterLigationCleanupTS", false, true, SystemOfRecord.SQUID, false,
            PlasticToValidate.SOURCE, PipelineTransformation.NONE),
    ENRICHMENT_TS("EnrichmentTS", false, true, SystemOfRecord.SQUID, false, PlasticToValidate.SOURCE, PipelineTransformation.PCR),
    ENRICHMENT_CLEANUP_TS("EnrichmentCleanupTS", false, true, SystemOfRecord.SQUID, false, PlasticToValidate.SOURCE, PipelineTransformation.NONE),

    // Illumina Genome Network
    IGN_PRE_NORM_TRANSFER("IGNPreNormTransfer", false, true, SystemOfRecord.SQUID, false, PlasticToValidate.SOURCE, PipelineTransformation.NONE),
    IGN_NORM_TRANSFER("IGNNormTransfer", false, true, SystemOfRecord.SQUID, false, PlasticToValidate.SOURCE, PipelineTransformation.NONE),
    IGN_INCOMING_PICO("IGNIncomingPico", false, true, SystemOfRecord.SQUID, false, PlasticToValidate.SOURCE, PipelineTransformation.NONE),
    IGN_GAP_ALIQUOT("IGNGapAliquot", false, true, SystemOfRecord.SQUID, false, PlasticToValidate.SOURCE, PipelineTransformation.NONE),

    VIRAL_PCR_PRODUCT_POOL_TRANSFER("ViralPcrProductPoolTransfer", false, true, SystemOfRecord.SQUID, false,
            PlasticToValidate.SOURCE, PipelineTransformation.NONE),
    ONE_STEP_RT_PCR_PRODUCT_POOL_TRANSFER("OneStepRtPcrProductPoolTransfer", false, true, SystemOfRecord.SQUID, false,
            PlasticToValidate.SOURCE, PipelineTransformation.NONE),

    // From BSP UI
    SAMPLE_RECEIPT("SampleReceipt", false, true, SystemOfRecord.MERCURY, false,
            PlasticToValidate.SOURCE, PipelineTransformation.NONE),
    SAMPLES_EXTRACTION_START("SamplesExtractionStart", false, true, SystemOfRecord.MERCURY, false,
            PlasticToValidate.SOURCE, PipelineTransformation.NONE),
    SAMPLES_EXTRACTION_END_TRANSFER("SamplesExtractionEndTransfer", false, true, SystemOfRecord.MERCURY, false,
            PlasticToValidate.SOURCE, PipelineTransformation.NONE),
    SAMPLES_NORMALIZATION_TRANSFER("SamplesNormalizationTransfer", false, true, SystemOfRecord.MERCURY, false,
            PlasticToValidate.SOURCE, PipelineTransformation.NONE),
    SAMPLES_PLATING_TO_COVARIS("SamplesPlatingToCovaris", false, true, SystemOfRecord.MERCURY, false,
            PlasticToValidate.SOURCE, PipelineTransformation.NONE),
    SAMPLES_DAUGHTER_PLATE_CREATION("SamplesDaughterPlateCreation", false, false, SystemOfRecord.MERCURY, true, PlasticToValidate.BOTH, PipelineTransformation.NONE),
    SAMPLE_IMPORT("SampleImport", false, true, SystemOfRecord.MERCURY, false,
            PlasticToValidate.SOURCE, PipelineTransformation.NONE),

    // mRRBS
    MRRBS_GENOMIC_TRANSFER("mRRBSGenomicTransfer", false, true, SystemOfRecord.SQUID, false, PlasticToValidate.SOURCE, PipelineTransformation.NONE),
    MRRBS_INDEXING("mRRBSIndexing", false, true, SystemOfRecord.SQUID, false, PlasticToValidate.SOURCE, PipelineTransformation.LIGATION),
    MRRBS_NORM_LIBS("mRRBSNormLibs", false, true, SystemOfRecord.SQUID, false, PlasticToValidate.SOURCE, PipelineTransformation.NONE),
    MRRBS_FINAL_PRODUCT_POOL("mRRBSFinalProductPool", false, true, SystemOfRecord.SQUID, false,
            PlasticToValidate.SOURCE, PipelineTransformation.NONE),

    // Malaria Illumina
    MALARIA_PCR1("Malaria_PCR1", false, true, SystemOfRecord.SQUID, false, PlasticToValidate.SOURCE, PipelineTransformation.LIGATION),
    MALARIA_PCRTAIL2("Malaria_PCRTail2", false, true, SystemOfRecord.SQUID, false, PlasticToValidate.SOURCE, PipelineTransformation.LIGATION),
    MALARIA_PCR_NORM("Malaria_PCR_Norm", false, true, SystemOfRecord.SQUID, false, PlasticToValidate.SOURCE, PipelineTransformation.NONE),
    MALARIA_MISEQ_PCR_POOL_SPRI("MalariaMiseqPCRPoolSPRI", false, true, SystemOfRecord.SQUID, false, PlasticToValidate.SOURCE, PipelineTransformation.NONE),

    // Malaria PacBio
    MALARIA_BEP_PCR("Malaria_BEP_PCR", false, true, SystemOfRecord.SQUID, false, PlasticToValidate.SOURCE, PipelineTransformation.NONE),
    MALARIA_BEP_PRIMER_TRANSFER("MalariaBEPPrimerTransfer", false, true, SystemOfRecord.SQUID, false, PlasticToValidate.TARGET, PipelineTransformation.LIGATION),
    MALARIA_BEP_PCR_NORM("Malaria_BEP_PCR_Norm", false, true, SystemOfRecord.SQUID, false, PlasticToValidate.SOURCE, PipelineTransformation.NONE),
    MALARIA_BEP_POOL("Malaria_BEP_Pool", false, true, SystemOfRecord.SQUID, false, PlasticToValidate.SOURCE, PipelineTransformation.NONE),

    // Illumina Content Exome (ICE)
    ICE_POOLING_TRANSFER("IcePoolingTransfer", false, true, SystemOfRecord.SQUID, false, PlasticToValidate.SOURCE, PipelineTransformation.NONE),
    ICE_SPRI_CONCENTRATION("IceSPRIConcentration", false, true, SystemOfRecord.SQUID, false, PlasticToValidate.SOURCE, PipelineTransformation.NONE),
    ICE_1ST_HYBRIDIZATION("Ice1stHybridization", false, true, SystemOfRecord.SQUID, false, PlasticToValidate.SOURCE, PipelineTransformation.NONE),
    ICE_1S_TBAIT_ADDITION("Ice1stBaitAddition", false, true, SystemOfRecord.SQUID, false, PlasticToValidate.TARGET, PipelineTransformation.NONE),
    ICE_1ST_CAPTURE("Ice1stCapture", false, true, SystemOfRecord.SQUID, false, PlasticToValidate.SOURCE, PipelineTransformation.NONE),
    ICE_2ND_HYBRIDIZATION("Ice2ndHybridization", false, true, SystemOfRecord.SQUID, false, PlasticToValidate.SOURCE, PipelineTransformation.NONE),
    ICE_2ND_BAIT_ADDITION("Ice2ndBaitAddition", false, true, SystemOfRecord.SQUID, false, PlasticToValidate.TARGET, PipelineTransformation.NONE),
    ICE_2ND_CAPTURE("Ice2ndCapture", false, true, SystemOfRecord.SQUID, false, PlasticToValidate.SOURCE, PipelineTransformation.NONE),
    ICE_CATCH_CLEANUP("IceCatchCleanup", false, true, SystemOfRecord.SQUID, false, PlasticToValidate.SOURCE, PipelineTransformation.NONE),
    ICE_CATCH_ENRICHMENT_SETUP("IceCatchEnrichmentSetup", false, true, SystemOfRecord.SQUID, false, PlasticToValidate.SOURCE, PipelineTransformation.NONE),
    ICE_CATCH_ENRICHMENT_CLEANUP("IceCatchEnrichmentCleanup", false, true, SystemOfRecord.SQUID, false, PlasticToValidate.SOURCE, PipelineTransformation.NONE),
    ICE_CATCH_PICO("IceCatchPico", false, true, SystemOfRecord.SQUID, false, PlasticToValidate.SOURCE, PipelineTransformation.NONE),

    /**
     * TODO SGM  the following names are place holders.  They will be re-evaluated as development of
     */
    SHEARING_BUCKET("ShearingBucket", true, true, SystemOfRecord.MERCURY, false, PlasticToValidate.SOURCE, PipelineTransformation.NONE),
//    SHEARING_BUCKET_ENTRY ("ShearingBucketEntry", true, true, SystemOfRecord.MERCURY),
//    SHEARING_BUCKET_EXIT ("ShearingBucketExit", true, true, SystemOfRecord.MERCURY),
    ;

    private final String name;

    private final boolean expectedEmptySources;

    private final boolean expectedEmptyTargets;

    private boolean createSources;

    public PipelineTransformation getPipelineTransformation() {
        return pipelineTransformation;
    }

    /**
     * For each event, which system is primarily responsible for that lab process
     */
    public enum SystemOfRecord {
        /**
         * Squid / BettaLIMS, being phased out
         */
        SQUID,
        /**
         * Mercury, being phased in
         */
        MERCURY,
        /**
         * For processes that are shared by multiple products, a message could belong to either system.  The
         * message router must examine the plastic barcodes to determine system of record
         */
        WORKFLOW_DEPENDENT,
        /**
         * Some messages, e.g. BaitSetup, don't include enough information to determine system of record, so
         * they must be processed in both.
         */
        BOTH
    }

    private final SystemOfRecord systemOfRecord;

    private static final Map<String, LabEventType> mapNameToType = new HashMap<String, LabEventType>();

    public enum PlasticToValidate {
        /*
            Lab Event Types associated with this will expect that the source of the event will be the only plastic ware
             that should already have been registered in the system AND be associated with a PDO that can be validated
         */
        SOURCE,

        /*
            Lab Event Types associated with this will expect that the Target of the event will be the only plasticware
            that should already have been registered in the system AND be associated with a PDO that can be validated
         */
        TARGET,

        /*
            No existing plan to Use this!!  Lab event Types associated with this will expect that both the Source and
            the Target of the event will have plasticware that should already have been registered in the system AND
            be associated with a PDO that can be validated
         */
        BOTH
    }

    private final PlasticToValidate plasticToValidate;

    /** The type of transformation, from the analysis pipeline's perspective. */
    public enum PipelineTransformation {
        NONE,
        LIGATION,
        PCR,
        CAPTURE,
        SIZE_SELECTION
    }

    private final PipelineTransformation pipelineTransformation;

    /**
     * One attempt at trying to make a very generic
     * {@link LabEvent} to handle lots of different
     * {@link LabEventName event names}
     *
     * @param name                 the event name in the message
     * @param expectSourcesEmpty   whether it's an error for the source vessels to have content
     * @param expectTargetsEmpty   whether it's an error for the target vessels to have content
     * @param systemOfRecord       which system is responsible for handling the message
     * @param createSources        whether sources should be created, if they don't exist
     * @param plasticToValidate    determines whether sources and / or targets are validated
     * @param pipelineTransformation type of transformation, from the analysis pipeline's perspective
     */
    LabEventType(String name, boolean expectSourcesEmpty, boolean expectTargetsEmpty, SystemOfRecord systemOfRecord,
            boolean createSources, PlasticToValidate plasticToValidate, PipelineTransformation pipelineTransformation) {
        this.name = name;
        this.expectedEmptySources = expectSourcesEmpty;
        this.expectedEmptyTargets = expectTargetsEmpty;
        this.systemOfRecord = systemOfRecord;
        this.createSources = createSources;
        this.plasticToValidate = plasticToValidate;
        this.pipelineTransformation = pipelineTransformation;
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

    public PlasticToValidate getPlasticToValidate() {
        return plasticToValidate;
    }

    public static LabEventType getByName(String name) {
        if (mapNameToType.isEmpty()) {
            for (LabEventType labEventType : LabEventType.values()) {
                mapNameToType.put(labEventType.getName(), labEventType);
            }
        }
        return mapNameToType.get(name);
    }
}

package org.broadinstitute.gpinformatics.mercury.entity.labevent;

import java.util.HashMap;
import java.util.Map;

/**
 * Properties common to all events of a particular message type
 */
public enum LabEventType {

    // Preflight
    PREFLIGHT_CLEANUP("PreFlightCleanup", ExpectSourcesEmpty.TRUE, ExpectTargetsEmpty.TRUE, SystemOfRecord.SQUID,
            CreateSources.FALSE, PlasticToValidate.SOURCE, PipelineTransformation.NONE, SendToBsp.FALSE),
    PREFLIGHT_PICO_SETUP("PreflightPicoSetup", ExpectSourcesEmpty.TRUE, ExpectTargetsEmpty.TRUE, SystemOfRecord.SQUID,
            CreateSources.FALSE, PlasticToValidate.SOURCE, PipelineTransformation.NONE, SendToBsp.FALSE),
    PREFLIGHT_NORMALIZATION("PreflightNormalization", ExpectSourcesEmpty.TRUE, ExpectTargetsEmpty.TRUE,
            SystemOfRecord.SQUID, CreateSources.FALSE, PlasticToValidate.SOURCE, PipelineTransformation.NONE,
            SendToBsp.FALSE),
    PREFLIGHT_POST_NORM_PICO_SETUP("PreflightPostNormPicoSetup", ExpectSourcesEmpty.TRUE, ExpectTargetsEmpty.TRUE,
            SystemOfRecord.SQUID, CreateSources.FALSE,
            PlasticToValidate.SOURCE, PipelineTransformation.NONE, SendToBsp.FALSE),

    // Shearing
    SHEARING_TRANSFER("ShearingTransfer", ExpectSourcesEmpty.FALSE, ExpectTargetsEmpty.TRUE,
            SystemOfRecord.WORKFLOW_DEPENDENT, CreateSources.FALSE,
            PlasticToValidate.SOURCE, PipelineTransformation.NONE, SendToBsp.FALSE),
    COVARIS_LOADED("CovarisLoaded", ExpectSourcesEmpty.FALSE, ExpectTargetsEmpty.TRUE,
            SystemOfRecord.WORKFLOW_DEPENDENT, CreateSources.FALSE,
            PlasticToValidate.SOURCE, PipelineTransformation.NONE, SendToBsp.FALSE),
    POST_SHEARING_TRANSFER_CLEANUP("PostShearingTransferCleanup", ExpectSourcesEmpty.FALSE, ExpectTargetsEmpty.TRUE,
            SystemOfRecord.WORKFLOW_DEPENDENT, CreateSources.FALSE,
            PlasticToValidate.SOURCE, PipelineTransformation.NONE, SendToBsp.FALSE),
    SHEARING_QC("ShearingQC", ExpectSourcesEmpty.FALSE, ExpectTargetsEmpty.TRUE, SystemOfRecord.WORKFLOW_DEPENDENT,
            CreateSources.FALSE,
            PlasticToValidate.SOURCE, PipelineTransformation.NONE, SendToBsp.FALSE),

    // Library construction
    END_REPAIR("EndRepair", ExpectSourcesEmpty.TRUE, ExpectTargetsEmpty.TRUE, SystemOfRecord.WORKFLOW_DEPENDENT,
            CreateSources.FALSE,
            PlasticToValidate.SOURCE, PipelineTransformation.NONE, SendToBsp.FALSE),
    POST_END_REPAIR_THERMO_CYCLER_LOADED("PostEndRepairThermoCyclerLoaded", ExpectSourcesEmpty.FALSE,
            ExpectTargetsEmpty.TRUE, SystemOfRecord.WORKFLOW_DEPENDENT, CreateSources.FALSE,
            PlasticToValidate.SOURCE, PipelineTransformation.NONE, SendToBsp.FALSE),
    END_REPAIR_CLEANUP("EndRepairCleanup", ExpectSourcesEmpty.TRUE, ExpectTargetsEmpty.TRUE,
            SystemOfRecord.WORKFLOW_DEPENDENT, CreateSources.FALSE,
            PlasticToValidate.SOURCE, PipelineTransformation.NONE, SendToBsp.FALSE),
    A_BASE("ABase", ExpectSourcesEmpty.TRUE, ExpectTargetsEmpty.TRUE, SystemOfRecord.WORKFLOW_DEPENDENT,
            CreateSources.FALSE, PlasticToValidate.SOURCE, PipelineTransformation.NONE, SendToBsp.FALSE),
    POST_ABASE_THERMO_CYCLER_LOADED("PostAbaseThermoCyclerLoaded", ExpectSourcesEmpty.FALSE, ExpectTargetsEmpty.TRUE,
            SystemOfRecord.WORKFLOW_DEPENDENT, CreateSources.FALSE,
            PlasticToValidate.SOURCE, PipelineTransformation.NONE, SendToBsp.FALSE),
    A_BASE_CLEANUP("ABaseCleanup", ExpectSourcesEmpty.TRUE, ExpectTargetsEmpty.TRUE, SystemOfRecord.WORKFLOW_DEPENDENT,
            CreateSources.FALSE,
            PlasticToValidate.SOURCE, PipelineTransformation.NONE, SendToBsp.FALSE),
    INDEXED_ADAPTER_LIGATION("IndexedAdapterLigation", ExpectSourcesEmpty.TRUE, ExpectTargetsEmpty.FALSE,
            SystemOfRecord.WORKFLOW_DEPENDENT, CreateSources.FALSE,
            PlasticToValidate.TARGET, PipelineTransformation.LIGATION, SendToBsp.FALSE),
    POST_INDEXED_ADAPTER_LIGATION_THERMO_CYCLER_LOADED("PostIndexedAdapterLigationThermoCyclerLoaded",
            ExpectSourcesEmpty.TRUE, ExpectTargetsEmpty.TRUE,
            SystemOfRecord.WORKFLOW_DEPENDENT, CreateSources.FALSE, PlasticToValidate.SOURCE,
            PipelineTransformation.NONE, SendToBsp.FALSE),
    ADAPTER_LIGATION_CLEANUP("AdapterLigationCleanup", ExpectSourcesEmpty.FALSE, ExpectTargetsEmpty.TRUE,
            SystemOfRecord.WORKFLOW_DEPENDENT, CreateSources.FALSE,
            PlasticToValidate.SOURCE, PipelineTransformation.NONE, SendToBsp.FALSE),
    // no longer used, needed to import past messages
    WGS_ADAPTER_LIGATION_CLEANUP("WGSAdapterLigationCleanup", ExpectSourcesEmpty.FALSE, ExpectTargetsEmpty.TRUE,
            SystemOfRecord.WORKFLOW_DEPENDENT, CreateSources.FALSE,
            PlasticToValidate.SOURCE, PipelineTransformation.NONE, SendToBsp.FALSE),
    POND_ENRICHMENT("PondEnrichment", ExpectSourcesEmpty.TRUE, ExpectTargetsEmpty.TRUE,
            SystemOfRecord.WORKFLOW_DEPENDENT, CreateSources.FALSE,
            PlasticToValidate.SOURCE, PipelineTransformation.PCR, SendToBsp.FALSE),
    INDEX_P5_POND_ENRICHMENT("IndexP5PondEnrichment", ExpectSourcesEmpty.TRUE, ExpectTargetsEmpty.TRUE,
            SystemOfRecord.WORKFLOW_DEPENDENT, CreateSources.FALSE,
            PlasticToValidate.TARGET, PipelineTransformation.PCR, SendToBsp.FALSE),
    POST_POND_ENRICHMENT_THERMO_CYCLER_LOADED("PostPondEnrichmentThermoCyclerLoaded", ExpectSourcesEmpty.TRUE,
            ExpectTargetsEmpty.TRUE, SystemOfRecord.WORKFLOW_DEPENDENT,
            CreateSources.FALSE.FALSE, PlasticToValidate.SOURCE, PipelineTransformation.NONE, SendToBsp.FALSE),
    POND_ENRICHMENT_CLEANUP("HybSelPondEnrichmentCleanup", ExpectSourcesEmpty.FALSE, ExpectTargetsEmpty.TRUE,
            SystemOfRecord.WORKFLOW_DEPENDENT, CreateSources.FALSE,
            PlasticToValidate.SOURCE, PipelineTransformation.NONE, SendToBsp.FALSE),
    POND_REGISTRATION("PondRegistration", ExpectSourcesEmpty.FALSE, ExpectTargetsEmpty.TRUE,
            SystemOfRecord.WORKFLOW_DEPENDENT, CreateSources.FALSE,
            PlasticToValidate.SOURCE, PipelineTransformation.NONE, SendToBsp.FALSE),
    POND_PICO("PondPico", ExpectSourcesEmpty.FALSE, ExpectTargetsEmpty.TRUE, SystemOfRecord.WORKFLOW_DEPENDENT,
            CreateSources.FALSE,
            PlasticToValidate.SOURCE, PipelineTransformation.NONE, SendToBsp.FALSE),

    // Hybrid Selection
    PRE_SELECTION_POOL("PreSelectionPool", ExpectSourcesEmpty.FALSE, ExpectTargetsEmpty.TRUE,
            SystemOfRecord.WORKFLOW_DEPENDENT, CreateSources.FALSE,
            PlasticToValidate.SOURCE, PipelineTransformation.NONE, SendToBsp.FALSE),
    HYBRIDIZATION_BUCKET("HybridizationBucket", ExpectSourcesEmpty.TRUE, ExpectTargetsEmpty.TRUE,
            SystemOfRecord.MERCURY, CreateSources.FALSE, PlasticToValidate.SOURCE, PipelineTransformation.NONE,
            SendToBsp.FALSE),
    HYBRIDIZATION("Hybridization", ExpectSourcesEmpty.FALSE, ExpectTargetsEmpty.TRUE, SystemOfRecord.WORKFLOW_DEPENDENT,
            CreateSources.FALSE,
            PlasticToValidate.SOURCE, PipelineTransformation.NONE, SendToBsp.FALSE),
    POST_HYBRIDIZATION_THERMO_CYCLER_LOADED("PostHybridizationThermoCyclerLoaded", ExpectSourcesEmpty.FALSE,
            ExpectTargetsEmpty.TRUE, SystemOfRecord.WORKFLOW_DEPENDENT, CreateSources.FALSE,
            PlasticToValidate.SOURCE, PipelineTransformation.NONE, SendToBsp.FALSE),
    BAIT_SETUP("BaitSetup", ExpectSourcesEmpty.TRUE, ExpectTargetsEmpty.TRUE, SystemOfRecord.BOTH, CreateSources.FALSE,
            PlasticToValidate.SOURCE, PipelineTransformation.NONE, SendToBsp.FALSE),
    BAIT_ADDITION("BaitAddition", ExpectSourcesEmpty.TRUE, ExpectTargetsEmpty.FALSE, SystemOfRecord.WORKFLOW_DEPENDENT,
            CreateSources.FALSE,
            PlasticToValidate.TARGET, PipelineTransformation.NONE, SendToBsp.FALSE),
    BEAD_ADDITION("BeadAddition", ExpectSourcesEmpty.TRUE, ExpectTargetsEmpty.TRUE, SystemOfRecord.WORKFLOW_DEPENDENT,
            CreateSources.FALSE,
            PlasticToValidate.SOURCE, PipelineTransformation.NONE, SendToBsp.FALSE),
    AP_WASH("APWash", ExpectSourcesEmpty.TRUE, ExpectTargetsEmpty.TRUE, SystemOfRecord.WORKFLOW_DEPENDENT,
            CreateSources.FALSE,
            PlasticToValidate.SOURCE, PipelineTransformation.NONE, SendToBsp.FALSE),
    GS_WASH_1("GSWash1", ExpectSourcesEmpty.TRUE, ExpectTargetsEmpty.TRUE, SystemOfRecord.WORKFLOW_DEPENDENT,
            CreateSources.FALSE,
            PlasticToValidate.SOURCE, PipelineTransformation.NONE, SendToBsp.FALSE),
    GS_WASH_2("GSWash2", ExpectSourcesEmpty.TRUE, ExpectTargetsEmpty.TRUE, SystemOfRecord.WORKFLOW_DEPENDENT,
            CreateSources.FALSE,
            PlasticToValidate.SOURCE, PipelineTransformation.NONE, SendToBsp.FALSE),
    GS_WASH_3("GSWash3", ExpectSourcesEmpty.TRUE, ExpectTargetsEmpty.TRUE, SystemOfRecord.WORKFLOW_DEPENDENT,
            CreateSources.FALSE,
            PlasticToValidate.SOURCE, PipelineTransformation.NONE, SendToBsp.FALSE),
    GS_WASH_4("GSWash4", ExpectSourcesEmpty.TRUE, ExpectTargetsEmpty.TRUE, SystemOfRecord.WORKFLOW_DEPENDENT,
            CreateSources.FALSE,
            PlasticToValidate.SOURCE, PipelineTransformation.NONE, SendToBsp.FALSE),
    GS_WASH_5("GSWash5", ExpectSourcesEmpty.TRUE, ExpectTargetsEmpty.TRUE, SystemOfRecord.WORKFLOW_DEPENDENT,
            CreateSources.FALSE,
            PlasticToValidate.SOURCE, PipelineTransformation.NONE, SendToBsp.FALSE),
    GS_WASH_6("GSWash6", ExpectSourcesEmpty.TRUE, ExpectTargetsEmpty.TRUE, SystemOfRecord.WORKFLOW_DEPENDENT,
            CreateSources.FALSE,
            PlasticToValidate.SOURCE, PipelineTransformation.NONE, SendToBsp.FALSE),
    CATCH_ENRICHMENT_SETUP("CatchEnrichmentSetup", ExpectSourcesEmpty.TRUE, ExpectTargetsEmpty.TRUE,
            SystemOfRecord.WORKFLOW_DEPENDENT, CreateSources.FALSE,
            PlasticToValidate.SOURCE, PipelineTransformation.NONE, SendToBsp.FALSE),
    POST_CATCH_ENRICHMENT_SETUP_THERMO_CYCLER_LOADED("PostCatchEnrichmentSetupThermoCyclerLoaded",
            ExpectSourcesEmpty.TRUE, ExpectTargetsEmpty.TRUE, SystemOfRecord.WORKFLOW_DEPENDENT, CreateSources.FALSE,
            PlasticToValidate.SOURCE, PipelineTransformation.NONE, SendToBsp.FALSE),
    CATCH_ENRICHMENT_CLEANUP("CatchEnrichmentCleanup", ExpectSourcesEmpty.FALSE, ExpectTargetsEmpty.TRUE,
            SystemOfRecord.WORKFLOW_DEPENDENT, CreateSources.FALSE,
            PlasticToValidate.SOURCE, PipelineTransformation.NONE, SendToBsp.FALSE),
    NORMALIZED_CATCH_REGISTRATION("NormalizedCatchRegistration", ExpectSourcesEmpty.FALSE, ExpectTargetsEmpty.TRUE,
            SystemOfRecord.WORKFLOW_DEPENDENT, CreateSources.FALSE,
            PlasticToValidate.SOURCE, PipelineTransformation.PCR, SendToBsp.FALSE),
    CATCH_PICO("CatchPico", ExpectSourcesEmpty.FALSE, ExpectTargetsEmpty.TRUE, SystemOfRecord.WORKFLOW_DEPENDENT,
            CreateSources.FALSE,
            PlasticToValidate.SOURCE, PipelineTransformation.NONE, SendToBsp.FALSE),

    // QTP
    POOLING_BUCKET("PoolingBucket", ExpectSourcesEmpty.TRUE, ExpectTargetsEmpty.TRUE, SystemOfRecord.MERCURY,
            CreateSources.FALSE, PlasticToValidate.SOURCE, PipelineTransformation.NONE, SendToBsp.FALSE),
    POOLING_TRANSFER("PoolingTransfer", ExpectSourcesEmpty.FALSE, ExpectTargetsEmpty.TRUE,
            SystemOfRecord.WORKFLOW_DEPENDENT, CreateSources.FALSE,
            PlasticToValidate.SOURCE, PipelineTransformation.NONE, SendToBsp.FALSE),
    ICE_POST_HYB_POOLING_TRANSFER("IcePostHybPoolingTransfer", ExpectSourcesEmpty.FALSE, ExpectTargetsEmpty.TRUE,
            SystemOfRecord.WORKFLOW_DEPENDENT, CreateSources.FALSE,
            PlasticToValidate.SOURCE, PipelineTransformation.NONE, SendToBsp.FALSE),
    ECO_TRANSFER("EcoTransfer", ExpectSourcesEmpty.FALSE, ExpectTargetsEmpty.TRUE, SystemOfRecord.WORKFLOW_DEPENDENT,
            CreateSources.FALSE,
            PlasticToValidate.SOURCE, PipelineTransformation.NONE, SendToBsp.FALSE),
    VIIA7_TRANSFER("Viia7Transfer", ExpectSourcesEmpty.FALSE, ExpectTargetsEmpty.TRUE,
            SystemOfRecord.WORKFLOW_DEPENDENT, CreateSources.FALSE,
            PlasticToValidate.SOURCE, PipelineTransformation.NONE, SendToBsp.FALSE),
    NORMALIZATION_TRANSFER("NormalizationTransfer", ExpectSourcesEmpty.FALSE, ExpectTargetsEmpty.TRUE,
            SystemOfRecord.WORKFLOW_DEPENDENT, CreateSources.FALSE,
            PlasticToValidate.SOURCE, PipelineTransformation.NONE, SendToBsp.FALSE),
    DENATURE_TRANSFER("DenatureTransfer", ExpectSourcesEmpty.FALSE, ExpectTargetsEmpty.TRUE,
            SystemOfRecord.WORKFLOW_DEPENDENT, CreateSources.FALSE,
            PlasticToValidate.SOURCE, PipelineTransformation.NONE, SendToBsp.FALSE),
    STRIP_TUBE_B_TRANSFER("StripTubeBTransfer", ExpectSourcesEmpty.TRUE, ExpectTargetsEmpty.FALSE,
            SystemOfRecord.WORKFLOW_DEPENDENT, CreateSources.FALSE,
            PlasticToValidate.SOURCE, PipelineTransformation.NONE, SendToBsp.FALSE),

    // HiSeq 2000
    FLOWCELL_TRANSFER("FlowcellTransfer", ExpectSourcesEmpty.TRUE, ExpectTargetsEmpty.FALSE,
            SystemOfRecord.WORKFLOW_DEPENDENT, CreateSources.FALSE,
            PlasticToValidate.SOURCE, PipelineTransformation.NONE, SendToBsp.FALSE),
    FLOWCELL_LOADED("FlowcellLoaded", ExpectSourcesEmpty.TRUE, ExpectTargetsEmpty.FALSE,
            SystemOfRecord.WORKFLOW_DEPENDENT, CreateSources.FALSE,
            PlasticToValidate.SOURCE, PipelineTransformation.NONE, SendToBsp.FALSE),

    // For HiSeq 2500
    DENATURE_TO_FLOWCELL_TRANSFER("DenatureToFlowcellTransfer", ExpectSourcesEmpty.FALSE, ExpectTargetsEmpty.TRUE,
            SystemOfRecord.WORKFLOW_DEPENDENT, CreateSources.FALSE,
            PlasticToValidate.SOURCE, PipelineTransformation.NONE, SendToBsp.FALSE),
    DENATURE_TO_DILUTION_TRANSFER("DenatureToDilutionTransfer", ExpectSourcesEmpty.FALSE, ExpectTargetsEmpty.TRUE,
            SystemOfRecord.WORKFLOW_DEPENDENT, CreateSources.FALSE,
            PlasticToValidate.SOURCE, PipelineTransformation.NONE, SendToBsp.FALSE),
    DILUTION_TO_FLOWCELL_TRANSFER("DilutionToFlowcellTransfer", ExpectSourcesEmpty.FALSE, ExpectTargetsEmpty.TRUE,
            SystemOfRecord.WORKFLOW_DEPENDENT, CreateSources.FALSE,
            PlasticToValidate.SOURCE, PipelineTransformation.NONE, SendToBsp.FALSE),

    // for MiSeq
    DENATURE_TO_REAGENT_KIT_TRANSFER("MiseqReagentKitLoading", ExpectSourcesEmpty.FALSE, ExpectTargetsEmpty.TRUE,
            SystemOfRecord.WORKFLOW_DEPENDENT, CreateSources.FALSE,
            PlasticToValidate.SOURCE, PipelineTransformation.NONE, SendToBsp.FALSE),
    REAGENT_KIT_TO_FLOWCELL_TRANSFER("ReagentKitToFlowcellTransfer", ExpectSourcesEmpty.FALSE, ExpectTargetsEmpty.TRUE,
            SystemOfRecord.WORKFLOW_DEPENDENT, CreateSources.FALSE,
            PlasticToValidate.SOURCE, PipelineTransformation.NONE, SendToBsp.FALSE),

    // Sage
    SAGE_LOADING("SageLoading", ExpectSourcesEmpty.TRUE, ExpectTargetsEmpty.FALSE, SystemOfRecord.SQUID,
            CreateSources.FALSE, PlasticToValidate.SOURCE, PipelineTransformation.NONE, SendToBsp.FALSE),
    SAGE_LOADED("SageLoaded", ExpectSourcesEmpty.TRUE, ExpectTargetsEmpty.FALSE, SystemOfRecord.SQUID,
            CreateSources.FALSE, PlasticToValidate.SOURCE, PipelineTransformation.NONE, SendToBsp.FALSE),
    SAGE_UNLOADING("SageUnloading", ExpectSourcesEmpty.TRUE, ExpectTargetsEmpty.FALSE, SystemOfRecord.SQUID,
            CreateSources.FALSE, PlasticToValidate.SOURCE, PipelineTransformation.NONE, SendToBsp.FALSE),
    SAGE_CLEANUP("SageCleanup", ExpectSourcesEmpty.TRUE, ExpectTargetsEmpty.FALSE, SystemOfRecord.SQUID,
            CreateSources.FALSE, PlasticToValidate.SOURCE, PipelineTransformation.SIZE_SELECTION, SendToBsp.FALSE),

    // Fluidigm
    FLUIDIGM_SAMPLE_INPUT("FluidigmSampleInput", ExpectSourcesEmpty.TRUE, ExpectTargetsEmpty.FALSE,
            SystemOfRecord.SQUID, CreateSources.FALSE, PlasticToValidate.SOURCE, PipelineTransformation.NONE,
            SendToBsp.FALSE),
    FLUIDIGM_INDEXED_ADAPTER_INPUT("FluidigmIndexedAdapterInput", ExpectSourcesEmpty.TRUE, ExpectTargetsEmpty.FALSE,
            SystemOfRecord.SQUID, CreateSources.FALSE,
            PlasticToValidate.SOURCE, PipelineTransformation.LIGATION, SendToBsp.FALSE),
    FLUIDIGM_INDEXED_ADAPTER_PCR("FluidigmIndexedAdapterPCR", ExpectSourcesEmpty.TRUE, ExpectTargetsEmpty.FALSE,
            SystemOfRecord.SQUID, CreateSources.FALSE,
            PlasticToValidate.SOURCE, PipelineTransformation.PCR, SendToBsp.FALSE),
    FLUIDIGM_PRODUCT_DILUTION("FluidigmProductDilution", ExpectSourcesEmpty.TRUE, ExpectTargetsEmpty.FALSE,
            SystemOfRecord.SQUID, CreateSources.FALSE,
            PlasticToValidate.SOURCE, PipelineTransformation.NONE, SendToBsp.FALSE),
    FLUIDIGM_HARVESTING_TO_RACK("FluidigmHarvestingToRack", ExpectSourcesEmpty.TRUE, ExpectTargetsEmpty.FALSE,
            SystemOfRecord.SQUID, CreateSources.FALSE,
            PlasticToValidate.SOURCE, PipelineTransformation.NONE, SendToBsp.FALSE),
    FLUIDIGM_HARVESTING("FluidigmHarvesting", ExpectSourcesEmpty.TRUE, ExpectTargetsEmpty.FALSE, SystemOfRecord.SQUID,
            CreateSources.FALSE, PlasticToValidate.SOURCE, PipelineTransformation.NONE, SendToBsp.FALSE),
    FLUIDIGM_FINAL_TRANSFER("FluidigmFinalTransfer", ExpectSourcesEmpty.TRUE, ExpectTargetsEmpty.FALSE,
            SystemOfRecord.SQUID, CreateSources.FALSE, PlasticToValidate.SOURCE, PipelineTransformation.NONE,
            SendToBsp.FALSE),

    // Jump
    JUMP_INITIAL_SHEARING_TRANSFER("JumpInitialShearingTransfer", ExpectSourcesEmpty.FALSE, ExpectTargetsEmpty.TRUE,
            SystemOfRecord.SQUID, CreateSources.FALSE,
            PlasticToValidate.SOURCE, PipelineTransformation.NONE, SendToBsp.FALSE),
    JUMP_INITIAL_SIZE_SELECTION("JumpInitialSizeSelection", ExpectSourcesEmpty.FALSE, ExpectTargetsEmpty.TRUE,
            SystemOfRecord.SQUID, CreateSources.FALSE,
            PlasticToValidate.SOURCE, PipelineTransformation.SIZE_SELECTION, SendToBsp.FALSE),
    JUMP_END_REPAIR_1("JumpEndRepair1", ExpectSourcesEmpty.TRUE, ExpectTargetsEmpty.FALSE, SystemOfRecord.SQUID,
            CreateSources.FALSE, PlasticToValidate.SOURCE, PipelineTransformation.NONE, SendToBsp.FALSE),
    JUMP_INITIAL_SIZE_SELECTION_QC("JumpInitialSizeSelectionQC", ExpectSourcesEmpty.FALSE, ExpectTargetsEmpty.TRUE,
            SystemOfRecord.SQUID, CreateSources.FALSE,
            PlasticToValidate.SOURCE, PipelineTransformation.NONE, SendToBsp.FALSE),
    JUMP_END_REPAIR_1_CLEANUP("JumpEndRepair1Cleanup", ExpectSourcesEmpty.FALSE, ExpectTargetsEmpty.TRUE,
            SystemOfRecord.SQUID, CreateSources.FALSE,
            PlasticToValidate.SOURCE, PipelineTransformation.NONE, SendToBsp.FALSE),
    JUMP_END_REPAIR_1_QC("JumpEndRepair1QC", ExpectSourcesEmpty.FALSE, ExpectTargetsEmpty.TRUE, SystemOfRecord.SQUID,
            CreateSources.FALSE, PlasticToValidate.SOURCE, PipelineTransformation.NONE, SendToBsp.FALSE),
    JUMP_CIRCULARIZATION_SETUP("JumpCircularizationSetup", ExpectSourcesEmpty.FALSE, ExpectTargetsEmpty.TRUE,
            SystemOfRecord.SQUID, CreateSources.FALSE,
            PlasticToValidate.SOURCE, PipelineTransformation.NONE, SendToBsp.FALSE),
    JUMP_EXONUCLEASE_TREATMENT("JumpExonucleaseTreatment", ExpectSourcesEmpty.TRUE, ExpectTargetsEmpty.FALSE,
            SystemOfRecord.SQUID, CreateSources.FALSE,
            PlasticToValidate.SOURCE, PipelineTransformation.NONE, SendToBsp.FALSE),
    JUMP_EXO_INACTIVATION("JumpExoInactivation", ExpectSourcesEmpty.FALSE, ExpectTargetsEmpty.TRUE,
            SystemOfRecord.SQUID, CreateSources.FALSE, PlasticToValidate.SOURCE, PipelineTransformation.NONE,
            SendToBsp.FALSE),
    JUMP_POST_CIRC_SIZE_SELECTION("JumpPostCircSizeSelection", ExpectSourcesEmpty.FALSE, ExpectTargetsEmpty.TRUE,
            SystemOfRecord.SQUID, CreateSources.FALSE,
            PlasticToValidate.SOURCE, PipelineTransformation.SIZE_SELECTION, SendToBsp.FALSE),
    JUMP_IMMOBILIZATION("JumpImmobilization", ExpectSourcesEmpty.TRUE, ExpectTargetsEmpty.FALSE, SystemOfRecord.SQUID,
            CreateSources.FALSE, PlasticToValidate.SOURCE, PipelineTransformation.NONE, SendToBsp.FALSE),
    JUMP_IMMOBILIZATION_WASH("JumpImmobilizationWash", ExpectSourcesEmpty.FALSE, ExpectTargetsEmpty.TRUE,
            SystemOfRecord.SQUID, CreateSources.FALSE,
            PlasticToValidate.SOURCE, PipelineTransformation.NONE, SendToBsp.FALSE),
    JUMP_END_REPAIR_2("JumpEndRepair2", ExpectSourcesEmpty.FALSE, ExpectTargetsEmpty.TRUE, SystemOfRecord.SQUID,
            CreateSources.FALSE, PlasticToValidate.SOURCE, PipelineTransformation.NONE, SendToBsp.FALSE),
    JUMP_END_REPAIR_2_WASH("JumpEndRepair2Wash", ExpectSourcesEmpty.FALSE, ExpectTargetsEmpty.TRUE,
            SystemOfRecord.SQUID, CreateSources.FALSE, PlasticToValidate.SOURCE, PipelineTransformation.NONE,
            SendToBsp.FALSE),
    JUMP_A_TAILING("JumpATailing", ExpectSourcesEmpty.FALSE, ExpectTargetsEmpty.TRUE, SystemOfRecord.SQUID,
            CreateSources.FALSE, PlasticToValidate.SOURCE, PipelineTransformation.NONE, SendToBsp.FALSE),
    JUMP_A_TAILING_WASH("JumpATailingWash", ExpectSourcesEmpty.FALSE, ExpectTargetsEmpty.TRUE, SystemOfRecord.SQUID,
            CreateSources.FALSE, PlasticToValidate.SOURCE, PipelineTransformation.NONE, SendToBsp.FALSE),
    JUMP_ADD_INDEX("JumpAddIndex", ExpectSourcesEmpty.FALSE, ExpectTargetsEmpty.TRUE, SystemOfRecord.SQUID,
            CreateSources.FALSE, PlasticToValidate.SOURCE, PipelineTransformation.NONE, SendToBsp.FALSE),
    JUMP_ADAPTER_LIGATION("JumpAdapterLigation", ExpectSourcesEmpty.FALSE, ExpectTargetsEmpty.TRUE,
            SystemOfRecord.SQUID, CreateSources.FALSE, PlasticToValidate.SOURCE, PipelineTransformation.LIGATION,
            SendToBsp.FALSE),
    JUMP_ADAPTER_LIGATION_WASH("JumpAdapterLigationWash", ExpectSourcesEmpty.FALSE, ExpectTargetsEmpty.TRUE,
            SystemOfRecord.SQUID, CreateSources.FALSE,
            PlasticToValidate.SOURCE, PipelineTransformation.NONE, SendToBsp.FALSE),
    JUMP_AMPLIFICATION("JumpAmplification", ExpectSourcesEmpty.FALSE, ExpectTargetsEmpty.TRUE, SystemOfRecord.SQUID,
            CreateSources.FALSE, PlasticToValidate.SOURCE, PipelineTransformation.NONE, SendToBsp.FALSE),
    JUMP_FINAL_LIBRARY_ELUTION("JumpFinalLibraryElution", ExpectSourcesEmpty.FALSE, ExpectTargetsEmpty.TRUE,
            SystemOfRecord.SQUID, CreateSources.FALSE,
            PlasticToValidate.SOURCE, PipelineTransformation.NONE, SendToBsp.FALSE),
    JUMP_FINAL_LIBRARY_SIZE_SELECTION("JumpFinalLibrarySizeSelection", ExpectSourcesEmpty.FALSE,
            ExpectTargetsEmpty.TRUE, SystemOfRecord.SQUID, CreateSources.FALSE,
            PlasticToValidate.SOURCE, PipelineTransformation.SIZE_SELECTION, SendToBsp.FALSE),
    JUMP_FINAL_LIBRARY_QC("JumpFinalLibraryQC", ExpectSourcesEmpty.FALSE, ExpectTargetsEmpty.TRUE, SystemOfRecord.SQUID,
            CreateSources.FALSE, PlasticToValidate.SOURCE, PipelineTransformation.NONE, SendToBsp.FALSE),
    JUMP_ENRICHED_LIBRARY_REGISTRATION("JumpEnrichedLibraryRegistration", ExpectSourcesEmpty.FALSE,
            ExpectTargetsEmpty.TRUE, SystemOfRecord.SQUID, CreateSources.FALSE,
            PlasticToValidate.SOURCE, PipelineTransformation.NONE, SendToBsp.FALSE),

    // BSP Pico
    PICO_PLATING_BUCKET("PicoPlatingBucket", ExpectSourcesEmpty.TRUE, ExpectTargetsEmpty.TRUE, SystemOfRecord.MERCURY,
            CreateSources.FALSE, PlasticToValidate.SOURCE, PipelineTransformation.NONE, SendToBsp.FALSE),
    PICO_PLATING_QC("PicoPlatingQC", ExpectSourcesEmpty.TRUE, ExpectTargetsEmpty.TRUE, SystemOfRecord.MERCURY,
            CreateSources.FALSE, PlasticToValidate.SOURCE, PipelineTransformation.NONE, SendToBsp.FALSE),
    PICO_DILUTION_TRANSFER("PicoDilutionTransfer", ExpectSourcesEmpty.TRUE, ExpectTargetsEmpty.TRUE,
            SystemOfRecord.MERCURY, CreateSources.FALSE, PlasticToValidate.SOURCE, PipelineTransformation.NONE,
            SendToBsp.FALSE),
    PICO_BUFFER_ADDITION("PicoBufferAddition", ExpectSourcesEmpty.TRUE, ExpectTargetsEmpty.TRUE, SystemOfRecord.MERCURY,
            CreateSources.FALSE, PlasticToValidate.SOURCE, PipelineTransformation.NONE, SendToBsp.FALSE),
    PICO_MICROFLUOR_TRANSFER("PicoMicrofluorTransfer", ExpectSourcesEmpty.TRUE, ExpectTargetsEmpty.TRUE,
            SystemOfRecord.MERCURY, CreateSources.FALSE,
            PlasticToValidate.SOURCE, PipelineTransformation.NONE, SendToBsp.FALSE),
    PICO_STANDARDS_TRANSFER("PicoStandardsTransfer", ExpectSourcesEmpty.TRUE, ExpectTargetsEmpty.TRUE,
            SystemOfRecord.MERCURY, CreateSources.TRUE,
            PlasticToValidate.SOURCE, PipelineTransformation.NONE, SendToBsp.FALSE),
    PICO_PLATING_POST_NORM_PICO("PicoPlatingPostNorm", ExpectSourcesEmpty.TRUE, ExpectTargetsEmpty.TRUE,
            SystemOfRecord.MERCURY, CreateSources.FALSE,
            PlasticToValidate.SOURCE, PipelineTransformation.NONE, SendToBsp.FALSE),
    PICO_REWORK("PicoRework", ExpectSourcesEmpty.TRUE, ExpectTargetsEmpty.TRUE, SystemOfRecord.MERCURY,
            CreateSources.FALSE,
            PlasticToValidate.SOURCE, PipelineTransformation.NONE, SendToBsp.FALSE),
    PICO_TRANSFER("PicoTransfer", ExpectSourcesEmpty.FALSE, ExpectTargetsEmpty.TRUE, SystemOfRecord.MERCURY,
            CreateSources.FALSE, PlasticToValidate.SOURCE, PipelineTransformation.NONE, SendToBsp.FALSE),

    // Dried Blood Spot
    DBS_SAMPLE_PUNCH("DBSSamplePunch", ExpectSourcesEmpty.FALSE, ExpectTargetsEmpty.TRUE, SystemOfRecord.MERCURY,
            CreateSources.FALSE, PlasticToValidate.SOURCE, PipelineTransformation.NONE, SendToBsp.FALSE),
    DBS_INCUBATION_MIX("DBSIncubationMix", ExpectSourcesEmpty.TRUE, ExpectTargetsEmpty.FALSE, SystemOfRecord.MERCURY,
            CreateSources.FALSE, PlasticToValidate.SOURCE, PipelineTransformation.NONE, SendToBsp.FALSE),
    DBS_LYSIS_BUFFER("DBSLysisBuffer", ExpectSourcesEmpty.TRUE, ExpectTargetsEmpty.FALSE, SystemOfRecord.MERCURY,
            CreateSources.FALSE, PlasticToValidate.SOURCE, PipelineTransformation.NONE, SendToBsp.FALSE),
    DBS_MAGNETIC_RESIN("DBSMagneticResin", ExpectSourcesEmpty.TRUE, ExpectTargetsEmpty.FALSE, SystemOfRecord.MERCURY,
            CreateSources.FALSE, PlasticToValidate.SOURCE, PipelineTransformation.NONE, SendToBsp.FALSE),
    DBS_1ST_PURIFICATION("DBS1stPurification", ExpectSourcesEmpty.FALSE, ExpectTargetsEmpty.TRUE,
            SystemOfRecord.MERCURY, CreateSources.FALSE, PlasticToValidate.SOURCE, PipelineTransformation.NONE,
            SendToBsp.FALSE),
    DBS_WASH_BUFFER("DBSWashBuffer", ExpectSourcesEmpty.TRUE, ExpectTargetsEmpty.FALSE, SystemOfRecord.MERCURY,
            CreateSources.FALSE, PlasticToValidate.SOURCE, PipelineTransformation.NONE, SendToBsp.FALSE),
    DBS_2ND_PURIFICATION("DBS2ndPurification", ExpectSourcesEmpty.FALSE, ExpectTargetsEmpty.TRUE,
            SystemOfRecord.MERCURY, CreateSources.FALSE, PlasticToValidate.SOURCE, PipelineTransformation.NONE,
            SendToBsp.FALSE),
    DBS_ELUTION_BUFFER("DBSElutionBuffer", ExpectSourcesEmpty.FALSE, ExpectTargetsEmpty.TRUE, SystemOfRecord.MERCURY,
            CreateSources.FALSE, PlasticToValidate.SOURCE, PipelineTransformation.NONE, SendToBsp.FALSE),
    DBS_FINAL_TRANSFER("DBSFinalTransfer", ExpectSourcesEmpty.FALSE, ExpectTargetsEmpty.TRUE, SystemOfRecord.MERCURY,
            CreateSources.FALSE, PlasticToValidate.SOURCE, PipelineTransformation.NONE, SendToBsp.FALSE),

    //Cryovial Blood and Saliva Extraction
    BLOOD_CRYOVIAL_EXTRACTION("BloodCryovialExtraction", ExpectSourcesEmpty.FALSE, ExpectTargetsEmpty.TRUE,
            SystemOfRecord.MERCURY,
            CreateSources.TRUE, PlasticToValidate.SOURCE, PipelineTransformation.NONE, SendToBsp.TRUE),
    BLOOD_DEEPWELL_CHEMAGEN_TRANSFER("ExtractionsBloodDeepwellToChemagen", ExpectSourcesEmpty.TRUE,
            ExpectTargetsEmpty.TRUE, SystemOfRecord.MERCURY,
            CreateSources.FALSE, PlasticToValidate.SOURCE, PipelineTransformation.NONE, SendToBsp.TRUE),
    BLOOD_CHEMAGEN_TO_FINAL_RACK("ExtractionsBloodChemagenToFinalRack", ExpectSourcesEmpty.TRUE,
            ExpectTargetsEmpty.TRUE, SystemOfRecord.MERCURY,
            CreateSources.FALSE, PlasticToValidate.SOURCE, PipelineTransformation.NONE, SendToBsp.TRUE),
    SALIVA_CRYOVIAL_EXTRACTION("SalivaCryovialExtraction", ExpectSourcesEmpty.FALSE, ExpectTargetsEmpty.TRUE,
            SystemOfRecord.MERCURY,
            CreateSources.TRUE, PlasticToValidate.SOURCE, PipelineTransformation.NONE, SendToBsp.TRUE),
    SALIVA_DEEPWELL_CHEMAGEN_TRANSFER("ExtractionsSalivaDeepwellToChemagen", ExpectSourcesEmpty.TRUE,
            ExpectTargetsEmpty.TRUE, SystemOfRecord.MERCURY,
            CreateSources.FALSE, PlasticToValidate.SOURCE, PipelineTransformation.NONE, SendToBsp.TRUE),
    SALIVA_CHEMAGEN_TO_FINAL_RACK("ExtractionsSalivaChemagenToFinalRack", ExpectSourcesEmpty.TRUE,
            ExpectTargetsEmpty.TRUE, SystemOfRecord.MERCURY,
            CreateSources.FALSE, PlasticToValidate.SOURCE, PipelineTransformation.NONE, SendToBsp.TRUE),


    // TruSeq Custom Amplicon
    TSCA_POST_NORM_TRANSFER("TSCAPostNormTransfer", ExpectSourcesEmpty.FALSE, ExpectTargetsEmpty.TRUE,
            SystemOfRecord.SQUID, CreateSources.FALSE, PlasticToValidate.SOURCE, PipelineTransformation.NONE,
            SendToBsp.FALSE),
    TSCA_HYB_SET_UP("TSCAHybSetUp", ExpectSourcesEmpty.FALSE, ExpectTargetsEmpty.TRUE, SystemOfRecord.SQUID,
            CreateSources.FALSE, PlasticToValidate.SOURCE, PipelineTransformation.NONE, SendToBsp.FALSE),
    TSCA_CAT_ADDITION("TSCACATAddition", ExpectSourcesEmpty.FALSE, ExpectTargetsEmpty.TRUE, SystemOfRecord.SQUID,
            CreateSources.FALSE, PlasticToValidate.SOURCE, PipelineTransformation.NONE, SendToBsp.FALSE),
    TSCA_HYBRIDIZATION_CLEANUP("TSCAHybridizationCleanUp", ExpectSourcesEmpty.FALSE, ExpectTargetsEmpty.TRUE,
            SystemOfRecord.SQUID, CreateSources.FALSE,
            PlasticToValidate.SOURCE, PipelineTransformation.NONE, SendToBsp.FALSE),
    TSCA_FLP_PREP("TSCAFLPPrep", ExpectSourcesEmpty.FALSE, ExpectTargetsEmpty.TRUE, SystemOfRecord.SQUID,
            CreateSources.FALSE, PlasticToValidate.SOURCE, PipelineTransformation.NONE, SendToBsp.FALSE),
    TSCA_SW1_ADDITION1("TSCASW1Addition1", ExpectSourcesEmpty.FALSE, ExpectTargetsEmpty.TRUE, SystemOfRecord.SQUID,
            CreateSources.FALSE, PlasticToValidate.SOURCE, PipelineTransformation.NONE, SendToBsp.FALSE),
    TSCA_SW1_ADDITION2("TSCASW1Addition2", ExpectSourcesEmpty.FALSE, ExpectTargetsEmpty.TRUE, SystemOfRecord.SQUID,
            CreateSources.FALSE, PlasticToValidate.SOURCE, PipelineTransformation.NONE, SendToBsp.FALSE),
    TSCA_UB1_ADDITION("TSCAUB1Addition", ExpectSourcesEmpty.FALSE, ExpectTargetsEmpty.TRUE, SystemOfRecord.SQUID,
            CreateSources.FALSE, PlasticToValidate.SOURCE, PipelineTransformation.NONE, SendToBsp.FALSE),
    TSCA_EXTENSION_LIGATION_SETUP("TSCAExtensionLigationSetup", ExpectSourcesEmpty.FALSE, ExpectTargetsEmpty.TRUE,
            SystemOfRecord.SQUID, CreateSources.FALSE,
            PlasticToValidate.SOURCE, PipelineTransformation.NONE, SendToBsp.FALSE),
    TSCA_INDEXED_AMPLICON_PCR("TSCAIndexedAmpliconPCR", ExpectSourcesEmpty.FALSE, ExpectTargetsEmpty.TRUE,
            SystemOfRecord.SQUID, CreateSources.FALSE,
            PlasticToValidate.SOURCE, PipelineTransformation.NONE, SendToBsp.FALSE),
    TSCA_INDEX_ADDITION("TSCAIndexAddition", ExpectSourcesEmpty.FALSE, ExpectTargetsEmpty.TRUE, SystemOfRecord.SQUID,
            CreateSources.FALSE, PlasticToValidate.SOURCE, PipelineTransformation.LIGATION, SendToBsp.FALSE),
    TSCA_AMPLICON_REGISTRATION("TSCAAmpliconRegistration", ExpectSourcesEmpty.FALSE, ExpectTargetsEmpty.TRUE,
            SystemOfRecord.SQUID, CreateSources.FALSE,
            PlasticToValidate.SOURCE, PipelineTransformation.NONE, SendToBsp.FALSE),
    TSCA_AMPLICON_PCR_CLEANUP("TSCAAmpliconPCRCleanup", ExpectSourcesEmpty.FALSE, ExpectTargetsEmpty.TRUE,
            SystemOfRecord.SQUID, CreateSources.FALSE,
            PlasticToValidate.SOURCE, PipelineTransformation.NONE, SendToBsp.FALSE),
    TSCA_POOL_CREATION("TSCAPoolCreation", ExpectSourcesEmpty.FALSE, ExpectTargetsEmpty.TRUE, SystemOfRecord.SQUID,
            CreateSources.FALSE, PlasticToValidate.SOURCE, PipelineTransformation.NONE, SendToBsp.FALSE),

    // 16S
    ILLUMINA_16S_TO_PRIMER_TRANSFER("Illumina16StoPrimerTransfer", ExpectSourcesEmpty.FALSE, ExpectTargetsEmpty.TRUE,
            SystemOfRecord.SQUID, CreateSources.FALSE,
            PlasticToValidate.SOURCE, PipelineTransformation.LIGATION, SendToBsp.FALSE),
    ILLUMINA_16S_PCR_PRODUCT_TRANSFER("Illumina16SPcrProductTransfer", ExpectSourcesEmpty.FALSE,
            ExpectTargetsEmpty.TRUE, SystemOfRecord.SQUID, CreateSources.FALSE,
            PlasticToValidate.SOURCE, PipelineTransformation.NONE, SendToBsp.FALSE),

    // cDNA dUTP
    POLY_A_TRANSFER("PolyATransfer", ExpectSourcesEmpty.FALSE, ExpectTargetsEmpty.TRUE, SystemOfRecord.SQUID,
            CreateSources.FALSE, PlasticToValidate.SOURCE, PipelineTransformation.NONE, SendToBsp.FALSE),
    POLY_A_SELECTION("PolyASelection", ExpectSourcesEmpty.FALSE, ExpectTargetsEmpty.TRUE, SystemOfRecord.SQUID,
            CreateSources.FALSE, PlasticToValidate.SOURCE, PipelineTransformation.NONE, SendToBsp.FALSE),
    DNASE("Dnase", ExpectSourcesEmpty.FALSE, ExpectTargetsEmpty.TRUE, SystemOfRecord.SQUID, CreateSources.FALSE,
            PlasticToValidate.SOURCE, PipelineTransformation.NONE, SendToBsp.FALSE),
    DNASE_CLEANUP("DnaseCleanup", ExpectSourcesEmpty.FALSE, ExpectTargetsEmpty.TRUE, SystemOfRecord.SQUID,
            CreateSources.FALSE, PlasticToValidate.SOURCE, PipelineTransformation.NONE, SendToBsp.FALSE),
    FRAGMENTATION("Fragmentation", ExpectSourcesEmpty.FALSE, ExpectTargetsEmpty.TRUE, SystemOfRecord.SQUID,
            CreateSources.FALSE, PlasticToValidate.SOURCE, PipelineTransformation.NONE, SendToBsp.FALSE),
    FRAGMENTATION_CLEANUP("FragmentationCleanup", ExpectSourcesEmpty.FALSE, ExpectTargetsEmpty.TRUE,
            SystemOfRecord.SQUID, CreateSources.FALSE, PlasticToValidate.SOURCE, PipelineTransformation.NONE,
            SendToBsp.FALSE),
    FIRST_STRAND("FirstStrand", ExpectSourcesEmpty.FALSE, ExpectTargetsEmpty.TRUE, SystemOfRecord.SQUID,
            CreateSources.FALSE, PlasticToValidate.SOURCE, PipelineTransformation.NONE, SendToBsp.FALSE),
    FIRST_STRAND_CLEANUP("FirstStrandCleanup", ExpectSourcesEmpty.FALSE, ExpectTargetsEmpty.TRUE, SystemOfRecord.SQUID,
            CreateSources.FALSE, PlasticToValidate.SOURCE, PipelineTransformation.NONE, SendToBsp.FALSE),
    SECOND_STRAND("SecondStrand", ExpectSourcesEmpty.FALSE, ExpectTargetsEmpty.TRUE, SystemOfRecord.SQUID,
            CreateSources.FALSE, PlasticToValidate.SOURCE, PipelineTransformation.NONE, SendToBsp.FALSE),
    SECOND_STRAND_CLEANUP("SecondStrandCleanup", ExpectSourcesEmpty.FALSE, ExpectTargetsEmpty.TRUE,
            SystemOfRecord.SQUID, CreateSources.FALSE, PlasticToValidate.SOURCE, PipelineTransformation.NONE,
            SendToBsp.FALSE),
    USER("USER", ExpectSourcesEmpty.FALSE, ExpectTargetsEmpty.TRUE, SystemOfRecord.SQUID, CreateSources.FALSE,
            PlasticToValidate.SOURCE, PipelineTransformation.NONE, SendToBsp.FALSE),
    CDNA_PONDENRICHMENT_CLEANUP("cDNAPondEnrichmentCleanup", ExpectSourcesEmpty.FALSE, ExpectTargetsEmpty.TRUE,
            SystemOfRecord.SQUID, CreateSources.FALSE,
            PlasticToValidate.SOURCE, PipelineTransformation.PCR, SendToBsp.FALSE),

    // cDNA TruSeq
    POLY_A_SELECTION_TS("PolyASelectionTS", ExpectSourcesEmpty.FALSE, ExpectTargetsEmpty.TRUE, SystemOfRecord.SQUID,
            CreateSources.FALSE, PlasticToValidate.SOURCE, PipelineTransformation.NONE, SendToBsp.FALSE),
    FIRST_STRAND_TS("FirstStrandTS", ExpectSourcesEmpty.FALSE, ExpectTargetsEmpty.TRUE, SystemOfRecord.SQUID,
            CreateSources.FALSE, PlasticToValidate.SOURCE, PipelineTransformation.NONE, SendToBsp.FALSE),
    SECOND_STRAND_TS("SecondStrandTS", ExpectSourcesEmpty.FALSE, ExpectTargetsEmpty.TRUE, SystemOfRecord.SQUID,
            CreateSources.FALSE, PlasticToValidate.SOURCE, PipelineTransformation.NONE, SendToBsp.FALSE),
    SECOND_STRAND_CLEANUP_TS("SecondStrandCleanupTS", ExpectSourcesEmpty.FALSE, ExpectTargetsEmpty.TRUE,
            SystemOfRecord.SQUID, CreateSources.FALSE,
            PlasticToValidate.SOURCE, PipelineTransformation.NONE, SendToBsp.FALSE),
    END_REPAIR_TS("EndRepairTS", ExpectSourcesEmpty.FALSE, ExpectTargetsEmpty.TRUE, SystemOfRecord.SQUID,
            CreateSources.FALSE, PlasticToValidate.SOURCE, PipelineTransformation.NONE, SendToBsp.FALSE),
    END_REPAIR_CLEANUP_TS("EndRepairCleanupTS", ExpectSourcesEmpty.FALSE, ExpectTargetsEmpty.TRUE, SystemOfRecord.SQUID,
            CreateSources.FALSE, PlasticToValidate.SOURCE, PipelineTransformation.NONE, SendToBsp.FALSE),
    A_BASE_TS("ABaseTS", ExpectSourcesEmpty.FALSE, ExpectTargetsEmpty.TRUE, SystemOfRecord.SQUID, CreateSources.FALSE,
            PlasticToValidate.SOURCE, PipelineTransformation.NONE, SendToBsp.FALSE),
    INDEXED_ADAPTER_LIGATION_TS("IndexedAdapterLigationTS", ExpectSourcesEmpty.FALSE, ExpectTargetsEmpty.TRUE,
            SystemOfRecord.SQUID, CreateSources.FALSE,
            PlasticToValidate.SOURCE, PipelineTransformation.LIGATION, SendToBsp.FALSE),
    ADAPTER_LIGATION_CLEANUP_TS("AdapterLigationCleanupTS", ExpectSourcesEmpty.FALSE, ExpectTargetsEmpty.TRUE,
            SystemOfRecord.SQUID, CreateSources.FALSE,
            PlasticToValidate.SOURCE, PipelineTransformation.NONE, SendToBsp.FALSE),
    ENRICHMENT_TS("EnrichmentTS", ExpectSourcesEmpty.FALSE, ExpectTargetsEmpty.TRUE, SystemOfRecord.SQUID,
            CreateSources.FALSE, PlasticToValidate.SOURCE, PipelineTransformation.PCR, SendToBsp.FALSE),
    ENRICHMENT_CLEANUP_TS("EnrichmentCleanupTS", ExpectSourcesEmpty.FALSE, ExpectTargetsEmpty.TRUE,
            SystemOfRecord.SQUID, CreateSources.FALSE, PlasticToValidate.SOURCE, PipelineTransformation.NONE,
            SendToBsp.FALSE),

    // Illumina Genome Network
    IGN_PRE_NORM_TRANSFER("IGNPreNormTransfer", ExpectSourcesEmpty.FALSE, ExpectTargetsEmpty.TRUE, SystemOfRecord.SQUID,
            CreateSources.FALSE, PlasticToValidate.SOURCE, PipelineTransformation.NONE, SendToBsp.FALSE),
    IGN_NORM_TRANSFER("IGNNormTransfer", ExpectSourcesEmpty.FALSE, ExpectTargetsEmpty.TRUE, SystemOfRecord.SQUID,
            CreateSources.FALSE, PlasticToValidate.SOURCE, PipelineTransformation.NONE, SendToBsp.FALSE),
    IGN_INCOMING_PICO("IGNIncomingPico", ExpectSourcesEmpty.FALSE, ExpectTargetsEmpty.TRUE, SystemOfRecord.SQUID,
            CreateSources.FALSE, PlasticToValidate.SOURCE, PipelineTransformation.NONE, SendToBsp.FALSE),
    IGN_GAP_ALIQUOT("IGNGapAliquot", ExpectSourcesEmpty.FALSE, ExpectTargetsEmpty.TRUE, SystemOfRecord.SQUID,
            CreateSources.FALSE, PlasticToValidate.SOURCE, PipelineTransformation.NONE, SendToBsp.FALSE),

    VIRAL_PCR_PRODUCT_POOL_TRANSFER("ViralPcrProductPoolTransfer", ExpectSourcesEmpty.FALSE, ExpectTargetsEmpty.TRUE,
            SystemOfRecord.SQUID, CreateSources.FALSE,
            PlasticToValidate.SOURCE, PipelineTransformation.NONE, SendToBsp.FALSE),
    ONE_STEP_RT_PCR_PRODUCT_POOL_TRANSFER("OneStepRtPcrProductPoolTransfer", ExpectSourcesEmpty.FALSE,
            ExpectTargetsEmpty.TRUE, SystemOfRecord.SQUID, CreateSources.FALSE,
            PlasticToValidate.SOURCE, PipelineTransformation.NONE, SendToBsp.FALSE),

    // From BSP UI
    /**
     * "Packaging" indicates the samples are being placed in a package for shipping.
     */
    SAMPLE_PACKAGE("SamplePackage", ExpectSourcesEmpty.FALSE, ExpectTargetsEmpty.TRUE, SystemOfRecord.MERCURY,
            CreateSources.FALSE, PlasticToValidate.SOURCE, PipelineTransformation.NONE, SendToBsp.FALSE),
    INITIAL_TARE("InitialTare", ExpectSourcesEmpty.FALSE, ExpectTargetsEmpty.TRUE, SystemOfRecord.MERCURY,
            CreateSources.FALSE, PlasticToValidate.SOURCE, PipelineTransformation.NONE, SendToBsp.FALSE),
    SAMPLE_RECEIPT("SampleReceipt", ExpectSourcesEmpty.FALSE, ExpectTargetsEmpty.TRUE, SystemOfRecord.MERCURY,
            CreateSources.FALSE, PlasticToValidate.SOURCE, PipelineTransformation.NONE, SendToBsp.FALSE),
    SAMPLES_EXTRACTION_START("SamplesExtractionStart", ExpectSourcesEmpty.FALSE, ExpectTargetsEmpty.TRUE,
            SystemOfRecord.MERCURY, CreateSources.FALSE,
            PlasticToValidate.SOURCE, PipelineTransformation.NONE, SendToBsp.FALSE),
    SAMPLES_EXTRACTION_END_TRANSFER("SamplesExtractionEndTransfer", ExpectSourcesEmpty.FALSE, ExpectTargetsEmpty.TRUE,
            SystemOfRecord.MERCURY, CreateSources.FALSE,
            PlasticToValidate.SOURCE, PipelineTransformation.NONE, SendToBsp.FALSE),
    SAMPLES_NORMALIZATION_TRANSFER("SamplesNormalizationTransfer", ExpectSourcesEmpty.FALSE, ExpectTargetsEmpty.TRUE,
            SystemOfRecord.MERCURY, CreateSources.FALSE,
            PlasticToValidate.SOURCE, PipelineTransformation.NONE, SendToBsp.FALSE),
    SAMPLES_PLATING_TO_COVARIS("SamplesPlatingToCovaris", ExpectSourcesEmpty.FALSE, ExpectTargetsEmpty.TRUE,
            SystemOfRecord.MERCURY, CreateSources.FALSE,
            PlasticToValidate.SOURCE, PipelineTransformation.NONE, SendToBsp.FALSE),
    SAMPLES_DAUGHTER_PLATE_CREATION("SamplesDaughterPlateCreation", ExpectSourcesEmpty.FALSE, ExpectTargetsEmpty.FALSE,
            SystemOfRecord.MERCURY, CreateSources.TRUE, PlasticToValidate.BOTH, PipelineTransformation.NONE,
            SendToBsp.FALSE),
    AUTO_DAUGHTER_PLATE_CREATION("AutomatedDaughterPlateCreation", ExpectSourcesEmpty.FALSE, ExpectTargetsEmpty.FALSE,
            SystemOfRecord.MERCURY, CreateSources.TRUE, PlasticToValidate.BOTH, PipelineTransformation.NONE,
            SendToBsp.TRUE),
    SAMPLE_IMPORT("SampleImport", ExpectSourcesEmpty.FALSE, ExpectTargetsEmpty.TRUE, SystemOfRecord.MERCURY,
            CreateSources.FALSE,
            PlasticToValidate.SOURCE, PipelineTransformation.NONE, SendToBsp.FALSE),
    SEQ_PLATING_NORMALIZATION("SeqPlatingNormalization", ExpectSourcesEmpty.FALSE, ExpectTargetsEmpty.TRUE,
            SystemOfRecord.MERCURY, CreateSources.TRUE, PlasticToValidate.SOURCE, PipelineTransformation.NONE,
            SendToBsp.FALSE),
    ARRAY_PLATING_DILUTION("ArrayPlatingDilution", ExpectSourcesEmpty.FALSE, ExpectTargetsEmpty.TRUE,
            SystemOfRecord.MERCURY, CreateSources.TRUE, PlasticToValidate.SOURCE, PipelineTransformation.NONE,
            SendToBsp.TRUE),

    // mRRBS
    MRRBS_GENOMIC_TRANSFER("mRRBSGenomicTransfer", ExpectSourcesEmpty.FALSE, ExpectTargetsEmpty.TRUE,
            SystemOfRecord.SQUID, CreateSources.FALSE, PlasticToValidate.SOURCE, PipelineTransformation.NONE,
            SendToBsp.FALSE),
    MRRBS_INDEXING("mRRBSIndexing", ExpectSourcesEmpty.FALSE, ExpectTargetsEmpty.TRUE, SystemOfRecord.SQUID,
            CreateSources.FALSE, PlasticToValidate.SOURCE, PipelineTransformation.LIGATION, SendToBsp.FALSE),
    MRRBS_NORM_LIBS("mRRBSNormLibs", ExpectSourcesEmpty.FALSE, ExpectTargetsEmpty.TRUE, SystemOfRecord.SQUID,
            CreateSources.FALSE, PlasticToValidate.SOURCE, PipelineTransformation.NONE, SendToBsp.FALSE),
    MRRBS_FINAL_PRODUCT_POOL("mRRBSFinalProductPool", ExpectSourcesEmpty.FALSE, ExpectTargetsEmpty.TRUE,
            SystemOfRecord.SQUID, CreateSources.FALSE,
            PlasticToValidate.SOURCE, PipelineTransformation.NONE, SendToBsp.FALSE),

    // Malaria Illumina
    MALARIA_PCR1("Malaria_PCR1", ExpectSourcesEmpty.FALSE, ExpectTargetsEmpty.TRUE, SystemOfRecord.SQUID,
            CreateSources.FALSE, PlasticToValidate.SOURCE, PipelineTransformation.LIGATION, SendToBsp.FALSE),
    MALARIA_PCRTAIL2("Malaria_PCRTail2", ExpectSourcesEmpty.FALSE, ExpectTargetsEmpty.TRUE, SystemOfRecord.SQUID,
            CreateSources.FALSE, PlasticToValidate.SOURCE, PipelineTransformation.LIGATION, SendToBsp.FALSE),
    MALARIA_PCR_NORM("Malaria_PCR_Norm", ExpectSourcesEmpty.FALSE, ExpectTargetsEmpty.TRUE, SystemOfRecord.SQUID,
            CreateSources.FALSE, PlasticToValidate.SOURCE, PipelineTransformation.NONE, SendToBsp.FALSE),
    MALARIA_MISEQ_PCR_POOL_SPRI("MalariaMiseqPCRPoolSPRI", ExpectSourcesEmpty.FALSE, ExpectTargetsEmpty.TRUE,
            SystemOfRecord.SQUID, CreateSources.FALSE, PlasticToValidate.SOURCE, PipelineTransformation.NONE,
            SendToBsp.FALSE),

    // Malaria PacBio
    MALARIA_BEP_PCR("Malaria_BEP_PCR", ExpectSourcesEmpty.FALSE, ExpectTargetsEmpty.TRUE, SystemOfRecord.SQUID,
            CreateSources.FALSE, PlasticToValidate.SOURCE, PipelineTransformation.NONE, SendToBsp.FALSE),
    MALARIA_BEP_PRIMER_TRANSFER("MalariaBEPPrimerTransfer", ExpectSourcesEmpty.FALSE, ExpectTargetsEmpty.TRUE,
            SystemOfRecord.SQUID, CreateSources.FALSE, PlasticToValidate.TARGET, PipelineTransformation.LIGATION,
            SendToBsp.FALSE),
    MALARIA_BEP_PCR_NORM("Malaria_BEP_PCR_Norm", ExpectSourcesEmpty.FALSE, ExpectTargetsEmpty.TRUE,
            SystemOfRecord.SQUID, CreateSources.FALSE, PlasticToValidate.SOURCE, PipelineTransformation.NONE,
            SendToBsp.FALSE),
    MALARIA_BEP_POOL("Malaria_BEP_Pool", ExpectSourcesEmpty.FALSE, ExpectTargetsEmpty.TRUE, SystemOfRecord.SQUID,
            CreateSources.FALSE, PlasticToValidate.SOURCE, PipelineTransformation.NONE, SendToBsp.FALSE),

    // Illumina Content Exome (ICE)
    ICE_BUCKET("IceBucket", ExpectSourcesEmpty.TRUE, ExpectTargetsEmpty.TRUE, SystemOfRecord.WORKFLOW_DEPENDENT,
            CreateSources.FALSE, PlasticToValidate.SOURCE, PipelineTransformation.NONE, SendToBsp.FALSE),
    ICE_POOLING_TRANSFER("IcePoolingTransfer", ExpectSourcesEmpty.FALSE, ExpectTargetsEmpty.TRUE,
            SystemOfRecord.WORKFLOW_DEPENDENT, CreateSources.FALSE, PlasticToValidate.SOURCE,
            PipelineTransformation.NONE, SendToBsp.FALSE),
    ICE_POOL_TEST("IcePoolTest", ExpectSourcesEmpty.FALSE, ExpectTargetsEmpty.TRUE, SystemOfRecord.WORKFLOW_DEPENDENT,
            CreateSources.FALSE, PlasticToValidate.SOURCE, PipelineTransformation.NONE, SendToBsp.FALSE),
    ICE_SPRI_CONCENTRATION("IceSPRIConcentration", ExpectSourcesEmpty.FALSE, ExpectTargetsEmpty.TRUE,
            SystemOfRecord.WORKFLOW_DEPENDENT, CreateSources.FALSE, PlasticToValidate.SOURCE,
            PipelineTransformation.NONE, SendToBsp.FALSE),
    ICE_96_PLEX_SPRI_CONCENTRATION("Ice96PlexSpriConcentration", ExpectSourcesEmpty.FALSE, ExpectTargetsEmpty.TRUE,
            SystemOfRecord.WORKFLOW_DEPENDENT, CreateSources.FALSE, PlasticToValidate.SOURCE,
            PipelineTransformation.NONE, SendToBsp.FALSE),
    ICE_1ST_HYBRIDIZATION("Ice1stHybridization", ExpectSourcesEmpty.FALSE, ExpectTargetsEmpty.TRUE,
            SystemOfRecord.WORKFLOW_DEPENDENT, CreateSources.FALSE, PlasticToValidate.SOURCE,
            PipelineTransformation.NONE, SendToBsp.FALSE),
    ICE_1S_TBAIT_ADDITION("Ice1stBaitAddition", ExpectSourcesEmpty.FALSE, ExpectTargetsEmpty.TRUE,
            SystemOfRecord.WORKFLOW_DEPENDENT, CreateSources.FALSE, PlasticToValidate.TARGET,
            PipelineTransformation.NONE, SendToBsp.FALSE),
    ICE_1ST_CAPTURE("Ice1stCapture", ExpectSourcesEmpty.FALSE, ExpectTargetsEmpty.TRUE,
            SystemOfRecord.WORKFLOW_DEPENDENT,
            CreateSources.FALSE, PlasticToValidate.SOURCE, PipelineTransformation.NONE, SendToBsp.FALSE),
    ICE_2ND_HYBRIDIZATION("Ice2ndHybridization", ExpectSourcesEmpty.FALSE, ExpectTargetsEmpty.TRUE,
            SystemOfRecord.WORKFLOW_DEPENDENT, CreateSources.FALSE, PlasticToValidate.SOURCE,
            PipelineTransformation.NONE, SendToBsp.FALSE),
    ICE_2ND_BAIT_ADDITION("Ice2ndBaitAddition", ExpectSourcesEmpty.FALSE, ExpectTargetsEmpty.TRUE,
            SystemOfRecord.WORKFLOW_DEPENDENT, CreateSources.FALSE, PlasticToValidate.TARGET,
            PipelineTransformation.NONE, SendToBsp.FALSE),
    ICE_2ND_CAPTURE("Ice2ndCapture", ExpectSourcesEmpty.FALSE, ExpectTargetsEmpty.TRUE,
            SystemOfRecord.WORKFLOW_DEPENDENT,
            CreateSources.FALSE, PlasticToValidate.SOURCE, PipelineTransformation.NONE, SendToBsp.FALSE),
    ICE_CATCH_CLEANUP("IceCatchCleanup", ExpectSourcesEmpty.FALSE, ExpectTargetsEmpty.TRUE,
            SystemOfRecord.WORKFLOW_DEPENDENT, CreateSources.FALSE, PlasticToValidate.SOURCE,
            PipelineTransformation.NONE, SendToBsp.FALSE),
    ICE_CATCH_ENRICHMENT_SETUP("IceCatchEnrichmentSetup", ExpectSourcesEmpty.FALSE, ExpectTargetsEmpty.TRUE,
            SystemOfRecord.WORKFLOW_DEPENDENT, CreateSources.FALSE, PlasticToValidate.SOURCE,
            PipelineTransformation.NONE, SendToBsp.FALSE),
    ICE_CATCH_ENRICHMENT_CLEANUP("IceCatchEnrichmentCleanup", ExpectSourcesEmpty.FALSE, ExpectTargetsEmpty.TRUE,
            SystemOfRecord.WORKFLOW_DEPENDENT, CreateSources.FALSE, PlasticToValidate.SOURCE,
            PipelineTransformation.PCR, SendToBsp.FALSE),
    ICE_CATCH_PICO("IceCatchPico", ExpectSourcesEmpty.FALSE, ExpectTargetsEmpty.TRUE, SystemOfRecord.WORKFLOW_DEPENDENT,
            CreateSources.FALSE, PlasticToValidate.SOURCE, PipelineTransformation.NONE, SendToBsp.FALSE),

    // Nexome
    PRE_TAGMENTATION_TRANSFER("PreTagmentationTransfer", ExpectSourcesEmpty.FALSE, ExpectTargetsEmpty.TRUE,
            SystemOfRecord.SQUID,
            CreateSources.FALSE, PlasticToValidate.SOURCE, PipelineTransformation.NONE, SendToBsp.FALSE),
    TAGMENTATION("Tagmentation", ExpectSourcesEmpty.FALSE, ExpectTargetsEmpty.TRUE, SystemOfRecord.SQUID,
            CreateSources.FALSE, PlasticToValidate.SOURCE, PipelineTransformation.NONE, SendToBsp.FALSE),
    STOP_TAGMENTATION("StopTagmentation", ExpectSourcesEmpty.FALSE, ExpectTargetsEmpty.TRUE, SystemOfRecord.SQUID,
            CreateSources.FALSE, PlasticToValidate.SOURCE, PipelineTransformation.NONE, SendToBsp.FALSE),
    TAGMENTATION_CLEANUP("TagmentationCleanup", ExpectSourcesEmpty.FALSE, ExpectTargetsEmpty.TRUE, SystemOfRecord.SQUID,
            CreateSources.FALSE, PlasticToValidate.SOURCE, PipelineTransformation.NONE, SendToBsp.FALSE),
    NEXTERA_DUAL_INDEX_PCR("NexteraDualIndexPCR", ExpectSourcesEmpty.FALSE, ExpectTargetsEmpty.TRUE,
            SystemOfRecord.SQUID,
            CreateSources.FALSE, PlasticToValidate.TARGET, PipelineTransformation.PCR, SendToBsp.FALSE),
    NEXTERA_PCR_CLEANUP("NexteraPCRCleanup", ExpectSourcesEmpty.FALSE, ExpectTargetsEmpty.TRUE, SystemOfRecord.SQUID,
            CreateSources.FALSE, PlasticToValidate.SOURCE, PipelineTransformation.PCR, SendToBsp.FALSE),
    NEXOME_POND_PICO("NexomePondPico", ExpectSourcesEmpty.FALSE, ExpectTargetsEmpty.TRUE, SystemOfRecord.SQUID,
            CreateSources.FALSE, PlasticToValidate.SOURCE, PipelineTransformation.PCR, SendToBsp.FALSE),
    NEXOME_POOLING_TRANSFER("NexomePoolingTransfer", ExpectSourcesEmpty.FALSE, ExpectTargetsEmpty.TRUE,
            SystemOfRecord.SQUID,
            CreateSources.FALSE, PlasticToValidate.SOURCE, PipelineTransformation.PCR, SendToBsp.FALSE),
    NEXOME_POOL_TEST("NexomePoolTest", ExpectSourcesEmpty.FALSE, ExpectTargetsEmpty.TRUE, SystemOfRecord.SQUID,
            CreateSources.FALSE, PlasticToValidate.SOURCE, PipelineTransformation.PCR, SendToBsp.FALSE),
    NEXOME_SPRI_CONCENTRATION("NexomeSPRIConcentration", ExpectSourcesEmpty.FALSE, ExpectTargetsEmpty.TRUE,
            SystemOfRecord.SQUID,
            CreateSources.FALSE, PlasticToValidate.SOURCE, PipelineTransformation.PCR, SendToBsp.FALSE),
    NEXOME_1ST_HYBRIDIZATION("Nexome1stHybridization", ExpectSourcesEmpty.FALSE, ExpectTargetsEmpty.TRUE,
            SystemOfRecord.SQUID,
            CreateSources.FALSE, PlasticToValidate.SOURCE, PipelineTransformation.PCR, SendToBsp.FALSE),
    NEXOME_1ST_BAIT_ADDITION("Nexome1stBaitAddition", ExpectSourcesEmpty.FALSE, ExpectTargetsEmpty.TRUE,
            SystemOfRecord.SQUID,
            CreateSources.FALSE, PlasticToValidate.TARGET, PipelineTransformation.PCR, SendToBsp.FALSE),
    NEXOME_1ST_CAPTURE("Nexome1stCapture", ExpectSourcesEmpty.FALSE, ExpectTargetsEmpty.TRUE, SystemOfRecord.SQUID,
            CreateSources.FALSE, PlasticToValidate.SOURCE, PipelineTransformation.PCR, SendToBsp.FALSE),
    NEXOME_2ND_BAIT_ADDITION("Nexome2ndBaitAddition", ExpectSourcesEmpty.FALSE, ExpectTargetsEmpty.TRUE,
            SystemOfRecord.SQUID,
            CreateSources.FALSE, PlasticToValidate.SOURCE, PipelineTransformation.PCR, SendToBsp.FALSE),
    NEXOME_2ND_HYBRIDIZATION("Nexome2ndHybridization", ExpectSourcesEmpty.FALSE, ExpectTargetsEmpty.TRUE,
            SystemOfRecord.SQUID,
            CreateSources.FALSE, PlasticToValidate.SOURCE, PipelineTransformation.PCR, SendToBsp.FALSE),
    NEXOME_2ND_CAPTURE("Nexome2ndCapture", ExpectSourcesEmpty.FALSE, ExpectTargetsEmpty.TRUE, SystemOfRecord.SQUID,
            CreateSources.FALSE, PlasticToValidate.SOURCE, PipelineTransformation.PCR, SendToBsp.FALSE),
    NEXOME_CATCH_CLEANUP("NexomeCatchCleanup", ExpectSourcesEmpty.FALSE, ExpectTargetsEmpty.TRUE, SystemOfRecord.SQUID,
            CreateSources.FALSE, PlasticToValidate.SOURCE, PipelineTransformation.PCR, SendToBsp.FALSE),
    NEXOME_CATCH_ENRICHMENT("NexomeCatchEnrichment", ExpectSourcesEmpty.FALSE, ExpectTargetsEmpty.TRUE,
            SystemOfRecord.SQUID,
            CreateSources.FALSE, PlasticToValidate.SOURCE, PipelineTransformation.PCR, SendToBsp.FALSE),
    NEXOME_CATCH_ENRICHMENT_CLEANUP("NexomeCatchEnrichmentCleanup", ExpectSourcesEmpty.FALSE, ExpectTargetsEmpty.TRUE,
            SystemOfRecord.SQUID,
            CreateSources.FALSE, PlasticToValidate.SOURCE, PipelineTransformation.PCR, SendToBsp.FALSE),
    NEXOME_CATCH_PICO("NexomeCatchPico", ExpectSourcesEmpty.FALSE, ExpectTargetsEmpty.TRUE, SystemOfRecord.SQUID,
            CreateSources.FALSE, PlasticToValidate.SOURCE, PipelineTransformation.PCR, SendToBsp.FALSE),

    // PCRFree
    PCR_FREE_POND_REGISTRATION("PCRFreePondRegistration", ExpectSourcesEmpty.FALSE, ExpectTargetsEmpty.TRUE,
            SystemOfRecord.SQUID, CreateSources.FALSE,
            PlasticToValidate.SOURCE, PipelineTransformation.NONE, SendToBsp.FALSE),

    /**
     * TODO SGM  the following names are place holders.  They will be re-evaluated as development of
     */
    SHEARING_BUCKET("ShearingBucket", ExpectSourcesEmpty.TRUE, ExpectTargetsEmpty.TRUE, SystemOfRecord.MERCURY,
            CreateSources.FALSE, PlasticToValidate.SOURCE, PipelineTransformation.NONE, SendToBsp.FALSE),
//    SHEARING_BUCKET_ENTRY ("ShearingBucketEntry", ExpectSourcesEmpty.TRUE, ExpectTargetsEmpty.TRUE, SystemOfRecord.MERCURY),
//    SHEARING_BUCKET_EXIT ("ShearingBucketExit", ExpectSourcesEmpty.TRUE, ExpectTargetsEmpty.TRUE, SystemOfRecord.MERCURY),
    ;

    private final String name;

    private final ExpectSourcesEmpty expectedEmptySources;

    private final ExpectTargetsEmpty expectedEmptyTargets;

    private final CreateSources createSources;

    public PipelineTransformation getPipelineTransformation() {
        return pipelineTransformation;
    }

    /**
     * For each event, which system is primarily responsible for that lab process
     * <p/>
     * <ul>
     * <li>SQUID: Squid / BettaLIMS, being phased out.</li>
     * <li>MERCURY: Mercury, being phased in.</li>
     * <li>WORKFLOW_DEPENDENT: For processes that are shared by multiple products, a message could belong to either
     * system.  The message router must examine the plastic barcodes to determine system of record.</li>
     * <li>BOTH: Some messages, e.g. BaitSetup, don't include enough information to determine system of record,
     * so they must be processed in both.</li>
     * </ul>
     */
    public enum SystemOfRecord {
        SQUID,
        MERCURY,
        WORKFLOW_DEPENDENT,
        BOTH
    }

    private final SystemOfRecord systemOfRecord;

    private static final Map<String, LabEventType> mapNameToType = new HashMap<>();

    /**
     * <ul>
     * <li>SOURCE: Lab Event Types associated with this will expect that the <b>source</b> of the event will be the only
     * plasticware that should already have been registered in the system AND be associated with a PDO that can be validated.</li>
     * <li>TARGET: Lab Event Types associated with this will expect that the <b>target</b> of the event will be the only
     * plasticware that should already have been registered in the system AND be associated with a PDO that can be validated.</li>
     * <li>BOTH: No existing plan to Use this!!  Lab event Types associated with this will expect that both the Source and
     * the Target of the event will have plasticware that should already have been registered in the system AND
     * be associated with a PDO that can be validated.</li>
     * </ul>
     */
    public enum PlasticToValidate {
        SOURCE, TARGET, BOTH
    }

    private final PlasticToValidate plasticToValidate;

    /**
     * The type of transformation, from the analysis pipeline's perspective.
     */
    public enum PipelineTransformation {
        NONE,
        LIGATION,
        PCR,
        CAPTURE,
        SIZE_SELECTION
    }

    /**
     * Whether it's an error for the source vessels to have content.
     */
    public enum ExpectSourcesEmpty {
        TRUE(true),
        FALSE(false);
        private final boolean value;

        ExpectSourcesEmpty(boolean value) {
            this.value = value;
        }

        public boolean booleanValue() {
            return value;
        }
    }

    /**
     * Create the source vessel if it doesn't exist.
     */
    public enum CreateSources {
        TRUE(true),
        FALSE(false);
        private final boolean value;

        CreateSources(boolean value) {
            this.value = value;
        }

        public boolean booleanValue() {
            return value;
        }
    }

    /**
     * Whether it's an error for the target vessels to have content.
     */
    private enum ExpectTargetsEmpty {
        TRUE(true),
        FALSE(false);
        private boolean value;

        private ExpectTargetsEmpty(boolean value) {
            this.value = value;
        }
    }

    /** Whether to send this event message to BSP. */
    private enum SendToBsp {
        TRUE(true),
        FALSE(false);
        private boolean value;

        private SendToBsp(boolean value) {
            this.value = value;
        }
    }

    private final PipelineTransformation pipelineTransformation;
    private final SendToBsp sendToBsp;

    /**
     * One attempt at trying to make a very generic
     * {@link LabEvent} to handle lots of different
     * {@link LabEventName event names}
     *
     * @param name                   the event name in the message
     * @param expectSourcesEmpty     whether it's an error for the source vessels to have content
     * @param expectTargetsEmpty     whether it's an error for the target vessels to have content
     * @param systemOfRecord         which system is responsible for handling the message
     * @param createSources          whether sources should be created, if they don't exist
     * @param plasticToValidate      determines whether sources and / or targets are validated
     * @param pipelineTransformation type of transformation, from the analysis pipeline's perspective
     * @param sendToBsp              whether to send event message to BSP
     */
    LabEventType(String name, ExpectSourcesEmpty expectSourcesEmpty, ExpectTargetsEmpty expectTargetsEmpty,
                 SystemOfRecord systemOfRecord, CreateSources createSources, PlasticToValidate plasticToValidate,
                 PipelineTransformation pipelineTransformation, SendToBsp sendToBsp) {
        this.name = name;
        this.expectedEmptySources = expectSourcesEmpty;
        this.expectedEmptyTargets = expectTargetsEmpty;
        this.systemOfRecord = systemOfRecord;
        this.createSources = createSources;
        this.plasticToValidate = plasticToValidate;
        this.pipelineTransformation = pipelineTransformation;
        this.sendToBsp = sendToBsp;
    }

    public String getName() {
        return name;
    }

    public SystemOfRecord getSystemOfRecord() {
        return systemOfRecord;
    }

    public boolean isCreateSources() {
        return createSources == CreateSources.TRUE;
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

    public boolean isSendToBsp() {
        return sendToBsp == SendToBsp.TRUE;
    }
}

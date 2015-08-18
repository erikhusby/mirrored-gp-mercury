package org.broadinstitute.gpinformatics.mercury.entity.labevent;

import org.broadinstitute.gpinformatics.mercury.entity.vessel.BarcodedTube;
import org.broadinstitute.gpinformatics.mercury.entity.vessel.LabVessel;
import org.broadinstitute.gpinformatics.mercury.entity.vessel.StaticPlate;
import org.broadinstitute.gpinformatics.mercury.entity.vessel.VesselTypeGeometry;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * Properties common to all events of a particular message type
 */
public enum LabEventType {

    // Preflight
    PREFLIGHT_CLEANUP("PreFlightCleanup",
            ExpectSourcesEmpty.TRUE, ExpectTargetsEmpty.TRUE, SystemOfRecord.SQUID, CreateSources.FALSE,
            PlasticToValidate.SOURCE, PipelineTransformation.NONE, ForwardMessage.NONE, VolumeConcUpdate.MERCURY_ONLY),
    PREFLIGHT_PICO_SETUP("PreflightPicoSetup",
            ExpectSourcesEmpty.TRUE, ExpectTargetsEmpty.TRUE, SystemOfRecord.SQUID, CreateSources.FALSE,
            PlasticToValidate.SOURCE, PipelineTransformation.NONE, ForwardMessage.NONE, VolumeConcUpdate.MERCURY_ONLY),
    PREFLIGHT_NORMALIZATION("PreflightNormalization",
            ExpectSourcesEmpty.TRUE, ExpectTargetsEmpty.TRUE, SystemOfRecord.SQUID, CreateSources.FALSE,
            PlasticToValidate.SOURCE, PipelineTransformation.NONE, ForwardMessage.NONE, VolumeConcUpdate.MERCURY_ONLY),
    PREFLIGHT_POST_NORM_PICO_SETUP("PreflightPostNormPicoSetup",
            ExpectSourcesEmpty.TRUE, ExpectTargetsEmpty.TRUE, SystemOfRecord.SQUID, CreateSources.FALSE,
            PlasticToValidate.SOURCE, PipelineTransformation.NONE, ForwardMessage.NONE, VolumeConcUpdate.MERCURY_ONLY),

    // Shearing
    SHEARING_TRANSFER("ShearingTransfer",
            ExpectSourcesEmpty.FALSE, ExpectTargetsEmpty.TRUE, SystemOfRecord.WORKFLOW_DEPENDENT, CreateSources.FALSE,
            PlasticToValidate.SOURCE, PipelineTransformation.NONE, ForwardMessage.NONE, VolumeConcUpdate.MERCURY_ONLY),
    COVARIS_LOADED("CovarisLoaded",
            ExpectSourcesEmpty.FALSE, ExpectTargetsEmpty.TRUE, SystemOfRecord.WORKFLOW_DEPENDENT, CreateSources.FALSE,
            PlasticToValidate.SOURCE, PipelineTransformation.NONE, ForwardMessage.NONE, VolumeConcUpdate.MERCURY_ONLY),
    POST_SHEARING_TRANSFER_CLEANUP("PostShearingTransferCleanup",
            ExpectSourcesEmpty.FALSE, ExpectTargetsEmpty.TRUE, SystemOfRecord.WORKFLOW_DEPENDENT, CreateSources.FALSE,
            PlasticToValidate.SOURCE, PipelineTransformation.NONE, ForwardMessage.NONE, VolumeConcUpdate.MERCURY_ONLY,
            MessageType.PLATE_TRANSFER_EVENT, StaticPlate.PlateType.Eppendorf96, StaticPlate.PlateType.Eppendorf96,
            new String[]{"SPRI", "70% Ethanol", "EB"}),
    SHEARING_QC("ShearingQC",
            ExpectSourcesEmpty.FALSE, ExpectTargetsEmpty.TRUE, SystemOfRecord.WORKFLOW_DEPENDENT, CreateSources.FALSE,
            PlasticToValidate.SOURCE, PipelineTransformation.NONE, ForwardMessage.NONE, VolumeConcUpdate.MERCURY_ONLY),

    // Library construction
    END_REPAIR("EndRepair",
            ExpectSourcesEmpty.TRUE, ExpectTargetsEmpty.TRUE, SystemOfRecord.WORKFLOW_DEPENDENT, CreateSources.FALSE,
            PlasticToValidate.SOURCE, PipelineTransformation.NONE, ForwardMessage.NONE, VolumeConcUpdate.MERCURY_ONLY),
    POST_END_REPAIR_THERMO_CYCLER_LOADED("PostEndRepairThermoCyclerLoaded",
            ExpectSourcesEmpty.FALSE, ExpectTargetsEmpty.TRUE, SystemOfRecord.WORKFLOW_DEPENDENT, CreateSources.FALSE,
            PlasticToValidate.SOURCE, PipelineTransformation.NONE, ForwardMessage.NONE, VolumeConcUpdate.MERCURY_ONLY),
    END_REPAIR_CLEANUP("EndRepairCleanup",
            ExpectSourcesEmpty.TRUE, ExpectTargetsEmpty.TRUE, SystemOfRecord.WORKFLOW_DEPENDENT, CreateSources.FALSE,
            PlasticToValidate.SOURCE, PipelineTransformation.NONE, ForwardMessage.NONE, VolumeConcUpdate.MERCURY_ONLY),
    A_BASE("ABase",
            ExpectSourcesEmpty.TRUE, ExpectTargetsEmpty.TRUE, SystemOfRecord.WORKFLOW_DEPENDENT, CreateSources.FALSE,
            PlasticToValidate.SOURCE, PipelineTransformation.NONE, ForwardMessage.NONE, VolumeConcUpdate.MERCURY_ONLY),
    POST_ABASE_THERMO_CYCLER_LOADED("PostAbaseThermoCyclerLoaded",
            ExpectSourcesEmpty.FALSE, ExpectTargetsEmpty.TRUE, SystemOfRecord.WORKFLOW_DEPENDENT, CreateSources.FALSE,
            PlasticToValidate.SOURCE, PipelineTransformation.NONE, ForwardMessage.NONE, VolumeConcUpdate.MERCURY_ONLY),
    A_BASE_CLEANUP("ABaseCleanup",
            ExpectSourcesEmpty.TRUE, ExpectTargetsEmpty.TRUE, SystemOfRecord.WORKFLOW_DEPENDENT, CreateSources.FALSE,
            PlasticToValidate.SOURCE, PipelineTransformation.NONE, ForwardMessage.NONE, VolumeConcUpdate.MERCURY_ONLY),
    INDEXED_ADAPTER_LIGATION("IndexedAdapterLigation",
            ExpectSourcesEmpty.TRUE, ExpectTargetsEmpty.FALSE, SystemOfRecord.WORKFLOW_DEPENDENT, CreateSources.FALSE,
            PlasticToValidate.TARGET, PipelineTransformation.LIGATION, ForwardMessage.NONE, VolumeConcUpdate.MERCURY_ONLY),
    POST_INDEXED_ADAPTER_LIGATION_THERMO_CYCLER_LOADED("PostIndexedAdapterLigationThermoCyclerLoaded",
            ExpectSourcesEmpty.TRUE, ExpectTargetsEmpty.TRUE, SystemOfRecord.WORKFLOW_DEPENDENT, CreateSources.FALSE,
            PlasticToValidate.SOURCE, PipelineTransformation.NONE, ForwardMessage.NONE, VolumeConcUpdate.MERCURY_ONLY),
    ADAPTER_LIGATION_CLEANUP("AdapterLigationCleanup",
            ExpectSourcesEmpty.FALSE, ExpectTargetsEmpty.TRUE, SystemOfRecord.WORKFLOW_DEPENDENT, CreateSources.FALSE,
            PlasticToValidate.SOURCE, PipelineTransformation.NONE, ForwardMessage.NONE, VolumeConcUpdate.MERCURY_ONLY),
    // no longer used, needed to import past messages
    WGS_ADAPTER_LIGATION_CLEANUP("WGSAdapterLigationCleanup",
            ExpectSourcesEmpty.FALSE, ExpectTargetsEmpty.TRUE, SystemOfRecord.WORKFLOW_DEPENDENT, CreateSources.FALSE,
            PlasticToValidate.SOURCE, PipelineTransformation.NONE, ForwardMessage.NONE, VolumeConcUpdate.MERCURY_ONLY),
    // The PCR is associated with PondRegistration, because we want a tube barcode for the pipeline
    POND_ENRICHMENT("PondEnrichment",
            ExpectSourcesEmpty.TRUE, ExpectTargetsEmpty.TRUE, SystemOfRecord.WORKFLOW_DEPENDENT, CreateSources.FALSE,
            PlasticToValidate.SOURCE, PipelineTransformation.NONE, ForwardMessage.NONE, VolumeConcUpdate.MERCURY_ONLY),
    // The PCR is associated with PondRegistration, because we want a tube barcode for the pipeline
    INDEX_P5_POND_ENRICHMENT("IndexP5PondEnrichment",
            ExpectSourcesEmpty.TRUE, ExpectTargetsEmpty.TRUE, SystemOfRecord.WORKFLOW_DEPENDENT, CreateSources.FALSE,
            PlasticToValidate.TARGET, PipelineTransformation.NONE, ForwardMessage.NONE, VolumeConcUpdate.MERCURY_ONLY),
    POST_POND_ENRICHMENT_THERMO_CYCLER_LOADED("PostPondEnrichmentThermoCyclerLoaded",
            ExpectSourcesEmpty.TRUE, ExpectTargetsEmpty.TRUE, SystemOfRecord.WORKFLOW_DEPENDENT, CreateSources.FALSE,
            PlasticToValidate.SOURCE, PipelineTransformation.NONE, ForwardMessage.NONE, VolumeConcUpdate.MERCURY_ONLY),
    POND_ENRICHMENT_CLEANUP("HybSelPondEnrichmentCleanup",
            ExpectSourcesEmpty.FALSE, ExpectTargetsEmpty.TRUE, SystemOfRecord.WORKFLOW_DEPENDENT, CreateSources.FALSE,
            PlasticToValidate.SOURCE, PipelineTransformation.NONE, ForwardMessage.NONE, VolumeConcUpdate.MERCURY_ONLY),
    POND_REGISTRATION("PondRegistration",
            ExpectSourcesEmpty.FALSE, ExpectTargetsEmpty.TRUE, SystemOfRecord.WORKFLOW_DEPENDENT, CreateSources.FALSE,
            PlasticToValidate.SOURCE, PipelineTransformation.PCR, ForwardMessage.NONE, VolumeConcUpdate.MERCURY_ONLY),
    POND_PICO("PondPico",
            ExpectSourcesEmpty.FALSE, ExpectTargetsEmpty.TRUE, SystemOfRecord.WORKFLOW_DEPENDENT, CreateSources.FALSE,
            PlasticToValidate.SOURCE, PipelineTransformation.NONE, ForwardMessage.NONE, VolumeConcUpdate.MERCURY_ONLY),
    POND_NORMALIZATION("PondNormalization",
            ExpectSourcesEmpty.FALSE, ExpectTargetsEmpty.TRUE, SystemOfRecord.MERCURY, CreateSources.FALSE,
            PlasticToValidate.SOURCE, PipelineTransformation.NONE, ForwardMessage.NONE, VolumeConcUpdate.MERCURY_ONLY),


    // Hybrid Selection
    PRE_SELECTION_POOL("PreSelectionPool",
            ExpectSourcesEmpty.FALSE, ExpectTargetsEmpty.TRUE, SystemOfRecord.WORKFLOW_DEPENDENT, CreateSources.FALSE,
            PlasticToValidate.SOURCE, PipelineTransformation.NONE, ForwardMessage.NONE, VolumeConcUpdate.MERCURY_ONLY),
    HYBRIDIZATION_BUCKET("HybridizationBucket",
            ExpectSourcesEmpty.TRUE, ExpectTargetsEmpty.TRUE, SystemOfRecord.MERCURY, CreateSources.FALSE,
            PlasticToValidate.SOURCE, PipelineTransformation.NONE, ForwardMessage.NONE, VolumeConcUpdate.MERCURY_ONLY),
    HYBRIDIZATION("Hybridization",
            ExpectSourcesEmpty.FALSE, ExpectTargetsEmpty.TRUE, SystemOfRecord.WORKFLOW_DEPENDENT, CreateSources.FALSE,
            PlasticToValidate.SOURCE, PipelineTransformation.NONE, ForwardMessage.NONE, VolumeConcUpdate.MERCURY_ONLY),
    POST_HYBRIDIZATION_THERMO_CYCLER_LOADED("PostHybridizationThermoCyclerLoaded",
            ExpectSourcesEmpty.FALSE, ExpectTargetsEmpty.TRUE, SystemOfRecord.WORKFLOW_DEPENDENT, CreateSources.FALSE,
            PlasticToValidate.SOURCE, PipelineTransformation.NONE, ForwardMessage.NONE, VolumeConcUpdate.MERCURY_ONLY),
    BAIT_SETUP("BaitSetup",
            ExpectSourcesEmpty.TRUE, ExpectTargetsEmpty.TRUE, SystemOfRecord.BOTH, CreateSources.FALSE,
            PlasticToValidate.SOURCE, PipelineTransformation.NONE, ForwardMessage.NONE, VolumeConcUpdate.MERCURY_ONLY),
    BAIT_ADDITION("BaitAddition",
            ExpectSourcesEmpty.TRUE, ExpectTargetsEmpty.FALSE, SystemOfRecord.WORKFLOW_DEPENDENT, CreateSources.FALSE,
            PlasticToValidate.TARGET, PipelineTransformation.NONE, ForwardMessage.NONE, VolumeConcUpdate.MERCURY_ONLY),
    BEAD_ADDITION("BeadAddition",
            ExpectSourcesEmpty.TRUE, ExpectTargetsEmpty.TRUE, SystemOfRecord.WORKFLOW_DEPENDENT, CreateSources.FALSE,
            PlasticToValidate.SOURCE, PipelineTransformation.NONE, ForwardMessage.NONE, VolumeConcUpdate.MERCURY_ONLY),
    AP_WASH("APWash",
            ExpectSourcesEmpty.TRUE, ExpectTargetsEmpty.TRUE, SystemOfRecord.WORKFLOW_DEPENDENT, CreateSources.FALSE,
            PlasticToValidate.SOURCE, PipelineTransformation.NONE, ForwardMessage.NONE, VolumeConcUpdate.MERCURY_ONLY),
    GS_WASH_1("GSWash1",
            ExpectSourcesEmpty.TRUE, ExpectTargetsEmpty.TRUE, SystemOfRecord.WORKFLOW_DEPENDENT, CreateSources.FALSE,
            PlasticToValidate.SOURCE, PipelineTransformation.NONE, ForwardMessage.NONE, VolumeConcUpdate.MERCURY_ONLY),
    GS_WASH_2("GSWash2",
            ExpectSourcesEmpty.TRUE, ExpectTargetsEmpty.TRUE, SystemOfRecord.WORKFLOW_DEPENDENT, CreateSources.FALSE,
            PlasticToValidate.SOURCE, PipelineTransformation.NONE, ForwardMessage.NONE, VolumeConcUpdate.MERCURY_ONLY),
    GS_WASH_3("GSWash3",
            ExpectSourcesEmpty.TRUE, ExpectTargetsEmpty.TRUE, SystemOfRecord.WORKFLOW_DEPENDENT, CreateSources.FALSE,
            PlasticToValidate.SOURCE, PipelineTransformation.NONE, ForwardMessage.NONE, VolumeConcUpdate.MERCURY_ONLY),
    GS_WASH_4("GSWash4",
            ExpectSourcesEmpty.TRUE, ExpectTargetsEmpty.TRUE, SystemOfRecord.WORKFLOW_DEPENDENT, CreateSources.FALSE,
            PlasticToValidate.SOURCE, PipelineTransformation.NONE, ForwardMessage.NONE, VolumeConcUpdate.MERCURY_ONLY),
    GS_WASH_5("GSWash5",
            ExpectSourcesEmpty.TRUE, ExpectTargetsEmpty.TRUE, SystemOfRecord.WORKFLOW_DEPENDENT, CreateSources.FALSE,
            PlasticToValidate.SOURCE, PipelineTransformation.NONE, ForwardMessage.NONE, VolumeConcUpdate.MERCURY_ONLY),
    GS_WASH_6("GSWash6",
            ExpectSourcesEmpty.TRUE, ExpectTargetsEmpty.TRUE, SystemOfRecord.WORKFLOW_DEPENDENT, CreateSources.FALSE,
            PlasticToValidate.SOURCE, PipelineTransformation.NONE, ForwardMessage.NONE, VolumeConcUpdate.MERCURY_ONLY),
    CATCH_ENRICHMENT_SETUP("CatchEnrichmentSetup",
            ExpectSourcesEmpty.TRUE, ExpectTargetsEmpty.TRUE, SystemOfRecord.WORKFLOW_DEPENDENT, CreateSources.FALSE,
            PlasticToValidate.SOURCE, PipelineTransformation.NONE, ForwardMessage.NONE, VolumeConcUpdate.MERCURY_ONLY),
    POST_CATCH_ENRICHMENT_SETUP_THERMO_CYCLER_LOADED("PostCatchEnrichmentSetupThermoCyclerLoaded",
            ExpectSourcesEmpty.TRUE, ExpectTargetsEmpty.TRUE, SystemOfRecord.WORKFLOW_DEPENDENT, CreateSources.FALSE,
            PlasticToValidate.SOURCE, PipelineTransformation.NONE, ForwardMessage.NONE, VolumeConcUpdate.MERCURY_ONLY),
    CATCH_ENRICHMENT_CLEANUP("CatchEnrichmentCleanup",
            ExpectSourcesEmpty.FALSE, ExpectTargetsEmpty.TRUE, SystemOfRecord.WORKFLOW_DEPENDENT, CreateSources.FALSE,
            PlasticToValidate.SOURCE, PipelineTransformation.NONE, ForwardMessage.NONE, VolumeConcUpdate.MERCURY_ONLY),
    NORMALIZED_CATCH_REGISTRATION("NormalizedCatchRegistration",
            ExpectSourcesEmpty.FALSE, ExpectTargetsEmpty.TRUE, SystemOfRecord.WORKFLOW_DEPENDENT, CreateSources.FALSE,
            PlasticToValidate.SOURCE, PipelineTransformation.PCR, ForwardMessage.NONE, VolumeConcUpdate.MERCURY_ONLY),
    CATCH_PICO("CatchPico",
            ExpectSourcesEmpty.FALSE, ExpectTargetsEmpty.TRUE, SystemOfRecord.WORKFLOW_DEPENDENT, CreateSources.FALSE,
            PlasticToValidate.SOURCE, PipelineTransformation.NONE, ForwardMessage.NONE, VolumeConcUpdate.MERCURY_ONLY),

    // QTP
    POOLING_BUCKET("PoolingBucket",
            ExpectSourcesEmpty.TRUE, ExpectTargetsEmpty.TRUE, SystemOfRecord.MERCURY, CreateSources.FALSE,
            PlasticToValidate.SOURCE, PipelineTransformation.NONE, ForwardMessage.NONE, VolumeConcUpdate.MERCURY_ONLY),
    POOLING_TRANSFER("PoolingTransfer",
            ExpectSourcesEmpty.FALSE, ExpectTargetsEmpty.TRUE, SystemOfRecord.WORKFLOW_DEPENDENT, CreateSources.FALSE,
            PlasticToValidate.SOURCE, PipelineTransformation.NONE, ForwardMessage.NONE, VolumeConcUpdate.MERCURY_ONLY),
    ICE_POST_HYB_POOLING_TRANSFER("IcePostHybPoolingTransfer",
            ExpectSourcesEmpty.FALSE, ExpectTargetsEmpty.TRUE, SystemOfRecord.WORKFLOW_DEPENDENT, CreateSources.FALSE,
            PlasticToValidate.SOURCE, PipelineTransformation.NONE, ForwardMessage.NONE, VolumeConcUpdate.MERCURY_ONLY),
    ECO_TRANSFER("EcoTransfer",
            ExpectSourcesEmpty.FALSE, ExpectTargetsEmpty.TRUE, SystemOfRecord.WORKFLOW_DEPENDENT, CreateSources.FALSE,
            PlasticToValidate.SOURCE, PipelineTransformation.NONE, ForwardMessage.NONE, VolumeConcUpdate.MERCURY_ONLY),
    VIIA7_TRANSFER("Viia7Transfer",
            ExpectSourcesEmpty.FALSE, ExpectTargetsEmpty.TRUE, SystemOfRecord.WORKFLOW_DEPENDENT, CreateSources.FALSE,
            PlasticToValidate.SOURCE, PipelineTransformation.NONE, ForwardMessage.NONE, VolumeConcUpdate.MERCURY_ONLY),
    NORMALIZATION_TRANSFER("NormalizationTransfer",
            ExpectSourcesEmpty.FALSE, ExpectTargetsEmpty.TRUE, SystemOfRecord.WORKFLOW_DEPENDENT, CreateSources.FALSE,
            PlasticToValidate.SOURCE, PipelineTransformation.NONE, ForwardMessage.NONE, VolumeConcUpdate.MERCURY_ONLY),
    DENATURE_TRANSFER("DenatureTransfer",
            ExpectSourcesEmpty.FALSE, ExpectTargetsEmpty.TRUE, SystemOfRecord.WORKFLOW_DEPENDENT, CreateSources.FALSE,
            PlasticToValidate.SOURCE, PipelineTransformation.NONE, ForwardMessage.NONE, VolumeConcUpdate.MERCURY_ONLY),
    STRIP_TUBE_B_TRANSFER("StripTubeBTransfer",
            ExpectSourcesEmpty.TRUE, ExpectTargetsEmpty.FALSE, SystemOfRecord.WORKFLOW_DEPENDENT, CreateSources.FALSE,
            PlasticToValidate.SOURCE, PipelineTransformation.NONE, ForwardMessage.NONE, VolumeConcUpdate.MERCURY_ONLY),

    // HiSeq 2000
    FLOWCELL_TRANSFER("FlowcellTransfer",
            ExpectSourcesEmpty.TRUE, ExpectTargetsEmpty.FALSE, SystemOfRecord.WORKFLOW_DEPENDENT, CreateSources.FALSE,
            PlasticToValidate.SOURCE, PipelineTransformation.NONE, ForwardMessage.NONE, VolumeConcUpdate.MERCURY_ONLY),
    FLOWCELL_LOADED("FlowcellLoaded",
            ExpectSourcesEmpty.TRUE, ExpectTargetsEmpty.FALSE, SystemOfRecord.WORKFLOW_DEPENDENT, CreateSources.FALSE,
            PlasticToValidate.SOURCE, PipelineTransformation.NONE, ForwardMessage.NONE, VolumeConcUpdate.MERCURY_ONLY),

    // For HiSeq 2500
    DENATURE_TO_FLOWCELL_TRANSFER("DenatureToFlowcellTransfer",
            ExpectSourcesEmpty.FALSE, ExpectTargetsEmpty.TRUE, SystemOfRecord.WORKFLOW_DEPENDENT, CreateSources.FALSE,
            PlasticToValidate.SOURCE, PipelineTransformation.NONE, ForwardMessage.NONE, VolumeConcUpdate.MERCURY_ONLY),
    DENATURE_TO_DILUTION_TRANSFER("DenatureToDilutionTransfer",
            ExpectSourcesEmpty.FALSE, ExpectTargetsEmpty.TRUE, SystemOfRecord.WORKFLOW_DEPENDENT, CreateSources.FALSE,
            PlasticToValidate.SOURCE, PipelineTransformation.NONE, ForwardMessage.NONE, VolumeConcUpdate.MERCURY_ONLY),
    DILUTION_TO_FLOWCELL_TRANSFER("DilutionToFlowcellTransfer",
            ExpectSourcesEmpty.FALSE, ExpectTargetsEmpty.TRUE, SystemOfRecord.WORKFLOW_DEPENDENT, CreateSources.FALSE,
            PlasticToValidate.SOURCE, PipelineTransformation.NONE, ForwardMessage.NONE, VolumeConcUpdate.MERCURY_ONLY),

    // for MiSeq
    DENATURE_TO_REAGENT_KIT_TRANSFER("MiseqReagentKitLoading",
            ExpectSourcesEmpty.FALSE, ExpectTargetsEmpty.TRUE, SystemOfRecord.WORKFLOW_DEPENDENT, CreateSources.FALSE,
            PlasticToValidate.SOURCE, PipelineTransformation.NONE, ForwardMessage.NONE, VolumeConcUpdate.MERCURY_ONLY),
    REAGENT_KIT_TO_FLOWCELL_TRANSFER("ReagentKitToFlowcellTransfer",
            ExpectSourcesEmpty.FALSE, ExpectTargetsEmpty.TRUE, SystemOfRecord.WORKFLOW_DEPENDENT, CreateSources.FALSE,
            PlasticToValidate.SOURCE, PipelineTransformation.NONE, ForwardMessage.NONE, VolumeConcUpdate.MERCURY_ONLY),

    // Sage
    SAGE_LOADING("SageLoading",
            ExpectSourcesEmpty.TRUE, ExpectTargetsEmpty.FALSE, SystemOfRecord.SQUID, CreateSources.FALSE,
            PlasticToValidate.SOURCE, PipelineTransformation.NONE, ForwardMessage.NONE, VolumeConcUpdate.MERCURY_ONLY),
    SAGE_LOADED("SageLoaded",
            ExpectSourcesEmpty.TRUE, ExpectTargetsEmpty.FALSE, SystemOfRecord.SQUID, CreateSources.FALSE,
            PlasticToValidate.SOURCE, PipelineTransformation.NONE, ForwardMessage.NONE, VolumeConcUpdate.MERCURY_ONLY),
    SAGE_UNLOADING("SageUnloading",
            ExpectSourcesEmpty.TRUE, ExpectTargetsEmpty.FALSE, SystemOfRecord.SQUID, CreateSources.FALSE,
            PlasticToValidate.SOURCE, PipelineTransformation.NONE, ForwardMessage.NONE, VolumeConcUpdate.MERCURY_ONLY),
    SAGE_CLEANUP("SageCleanup",
            ExpectSourcesEmpty.TRUE, ExpectTargetsEmpty.FALSE, SystemOfRecord.SQUID, CreateSources.FALSE,
            PlasticToValidate.SOURCE, PipelineTransformation.SIZE_SELECTION, ForwardMessage.NONE, VolumeConcUpdate.MERCURY_ONLY),

    // Fluidigm
    FLUIDIGM_SAMPLE_INPUT("FluidigmSampleInput",
            ExpectSourcesEmpty.TRUE, ExpectTargetsEmpty.FALSE, SystemOfRecord.SQUID, CreateSources.FALSE,
            PlasticToValidate.SOURCE, PipelineTransformation.NONE, ForwardMessage.NONE, VolumeConcUpdate.MERCURY_ONLY),
    FLUIDIGM_INDEXED_ADAPTER_INPUT("FluidigmIndexedAdapterInput",
            ExpectSourcesEmpty.TRUE, ExpectTargetsEmpty.FALSE, SystemOfRecord.SQUID, CreateSources.FALSE,
            PlasticToValidate.SOURCE, PipelineTransformation.LIGATION, ForwardMessage.NONE, VolumeConcUpdate.MERCURY_ONLY),
    FLUIDIGM_INDEXED_ADAPTER_PCR("FluidigmIndexedAdapterPCR",
            ExpectSourcesEmpty.TRUE, ExpectTargetsEmpty.FALSE, SystemOfRecord.SQUID, CreateSources.FALSE,
            PlasticToValidate.SOURCE, PipelineTransformation.PCR, ForwardMessage.NONE, VolumeConcUpdate.MERCURY_ONLY),
    FLUIDIGM_PRODUCT_DILUTION("FluidigmProductDilution",
            ExpectSourcesEmpty.TRUE, ExpectTargetsEmpty.FALSE, SystemOfRecord.SQUID, CreateSources.FALSE,
            PlasticToValidate.SOURCE, PipelineTransformation.NONE, ForwardMessage.NONE, VolumeConcUpdate.MERCURY_ONLY),
    FLUIDIGM_HARVESTING_TO_RACK("FluidigmHarvestingToRack",
            ExpectSourcesEmpty.TRUE, ExpectTargetsEmpty.FALSE, SystemOfRecord.SQUID, CreateSources.FALSE,
            PlasticToValidate.SOURCE, PipelineTransformation.NONE, ForwardMessage.NONE, VolumeConcUpdate.MERCURY_ONLY),
    FLUIDIGM_HARVESTING("FluidigmHarvesting",
            ExpectSourcesEmpty.TRUE, ExpectTargetsEmpty.FALSE, SystemOfRecord.SQUID, CreateSources.FALSE,
            PlasticToValidate.SOURCE, PipelineTransformation.NONE, ForwardMessage.NONE, VolumeConcUpdate.MERCURY_ONLY),
    FLUIDIGM_FINAL_TRANSFER("FluidigmFinalTransfer",
            ExpectSourcesEmpty.TRUE, ExpectTargetsEmpty.FALSE, SystemOfRecord.SQUID, CreateSources.FALSE,
            PlasticToValidate.SOURCE, PipelineTransformation.NONE, ForwardMessage.NONE, VolumeConcUpdate.MERCURY_ONLY),

    // Jump
    JUMP_INITIAL_SHEARING_TRANSFER("JumpInitialShearingTransfer",
            ExpectSourcesEmpty.FALSE, ExpectTargetsEmpty.TRUE, SystemOfRecord.SQUID, CreateSources.FALSE,
            PlasticToValidate.SOURCE, PipelineTransformation.NONE, ForwardMessage.NONE, VolumeConcUpdate.MERCURY_ONLY),
    JUMP_INITIAL_SIZE_SELECTION("JumpInitialSizeSelection",
            ExpectSourcesEmpty.FALSE, ExpectTargetsEmpty.TRUE, SystemOfRecord.SQUID, CreateSources.FALSE,
            PlasticToValidate.SOURCE, PipelineTransformation.SIZE_SELECTION, ForwardMessage.NONE,
            VolumeConcUpdate.MERCURY_ONLY),
    JUMP_END_REPAIR_1("JumpEndRepair1",
            ExpectSourcesEmpty.TRUE, ExpectTargetsEmpty.FALSE, SystemOfRecord.SQUID, CreateSources.FALSE,
            PlasticToValidate.SOURCE, PipelineTransformation.NONE, ForwardMessage.NONE, VolumeConcUpdate.MERCURY_ONLY),
    JUMP_INITIAL_SIZE_SELECTION_QC("JumpInitialSizeSelectionQC",
            ExpectSourcesEmpty.FALSE, ExpectTargetsEmpty.TRUE, SystemOfRecord.SQUID, CreateSources.FALSE,
            PlasticToValidate.SOURCE, PipelineTransformation.NONE, ForwardMessage.NONE, VolumeConcUpdate.MERCURY_ONLY),
    JUMP_END_REPAIR_1_CLEANUP("JumpEndRepair1Cleanup",
            ExpectSourcesEmpty.FALSE, ExpectTargetsEmpty.TRUE, SystemOfRecord.SQUID, CreateSources.FALSE,
            PlasticToValidate.SOURCE, PipelineTransformation.NONE, ForwardMessage.NONE, VolumeConcUpdate.MERCURY_ONLY),
    JUMP_END_REPAIR_1_QC("JumpEndRepair1QC",
            ExpectSourcesEmpty.FALSE, ExpectTargetsEmpty.TRUE, SystemOfRecord.SQUID, CreateSources.FALSE,
            PlasticToValidate.SOURCE, PipelineTransformation.NONE, ForwardMessage.NONE, VolumeConcUpdate.MERCURY_ONLY),
    JUMP_CIRCULARIZATION_SETUP("JumpCircularizationSetup",
            ExpectSourcesEmpty.FALSE, ExpectTargetsEmpty.TRUE, SystemOfRecord.SQUID, CreateSources.FALSE,
            PlasticToValidate.SOURCE, PipelineTransformation.NONE, ForwardMessage.NONE, VolumeConcUpdate.MERCURY_ONLY),
    JUMP_EXONUCLEASE_TREATMENT("JumpExonucleaseTreatment",
            ExpectSourcesEmpty.TRUE, ExpectTargetsEmpty.FALSE, SystemOfRecord.SQUID, CreateSources.FALSE,
            PlasticToValidate.SOURCE, PipelineTransformation.NONE, ForwardMessage.NONE, VolumeConcUpdate.MERCURY_ONLY),
    JUMP_EXO_INACTIVATION("JumpExoInactivation",
            ExpectSourcesEmpty.FALSE, ExpectTargetsEmpty.TRUE, SystemOfRecord.SQUID, CreateSources.FALSE,
            PlasticToValidate.SOURCE, PipelineTransformation.NONE, ForwardMessage.NONE, VolumeConcUpdate.MERCURY_ONLY),
    JUMP_POST_CIRC_SIZE_SELECTION("JumpPostCircSizeSelection",
            ExpectSourcesEmpty.FALSE, ExpectTargetsEmpty.TRUE, SystemOfRecord.SQUID, CreateSources.FALSE,
            PlasticToValidate.SOURCE, PipelineTransformation.SIZE_SELECTION, ForwardMessage.NONE, VolumeConcUpdate.MERCURY_ONLY),
    JUMP_IMMOBILIZATION("JumpImmobilization",
            ExpectSourcesEmpty.TRUE, ExpectTargetsEmpty.FALSE, SystemOfRecord.SQUID, CreateSources.FALSE,
            PlasticToValidate.SOURCE, PipelineTransformation.NONE, ForwardMessage.NONE, VolumeConcUpdate.MERCURY_ONLY),
    JUMP_IMMOBILIZATION_WASH("JumpImmobilizationWash",
            ExpectSourcesEmpty.FALSE, ExpectTargetsEmpty.TRUE, SystemOfRecord.SQUID, CreateSources.FALSE,
            PlasticToValidate.SOURCE, PipelineTransformation.NONE, ForwardMessage.NONE, VolumeConcUpdate.MERCURY_ONLY),
    JUMP_END_REPAIR_2("JumpEndRepair2",
            ExpectSourcesEmpty.FALSE, ExpectTargetsEmpty.TRUE, SystemOfRecord.SQUID, CreateSources.FALSE,
            PlasticToValidate.SOURCE, PipelineTransformation.NONE, ForwardMessage.NONE, VolumeConcUpdate.MERCURY_ONLY),
    JUMP_END_REPAIR_2_WASH("JumpEndRepair2Wash",
            ExpectSourcesEmpty.FALSE, ExpectTargetsEmpty.TRUE, SystemOfRecord.SQUID, CreateSources.FALSE,
            PlasticToValidate.SOURCE, PipelineTransformation.NONE, ForwardMessage.NONE, VolumeConcUpdate.MERCURY_ONLY),
    JUMP_A_TAILING("JumpATailing",
            ExpectSourcesEmpty.FALSE, ExpectTargetsEmpty.TRUE, SystemOfRecord.SQUID, CreateSources.FALSE,
            PlasticToValidate.SOURCE, PipelineTransformation.NONE, ForwardMessage.NONE, VolumeConcUpdate.MERCURY_ONLY),
    JUMP_A_TAILING_WASH("JumpATailingWash",
            ExpectSourcesEmpty.FALSE, ExpectTargetsEmpty.TRUE, SystemOfRecord.SQUID, CreateSources.FALSE,
            PlasticToValidate.SOURCE, PipelineTransformation.NONE, ForwardMessage.NONE, VolumeConcUpdate.MERCURY_ONLY),
    JUMP_ADD_INDEX("JumpAddIndex",
            ExpectSourcesEmpty.FALSE, ExpectTargetsEmpty.TRUE, SystemOfRecord.SQUID, CreateSources.FALSE,
            PlasticToValidate.SOURCE, PipelineTransformation.NONE, ForwardMessage.NONE, VolumeConcUpdate.MERCURY_ONLY),
    JUMP_ADAPTER_LIGATION("JumpAdapterLigation",
            ExpectSourcesEmpty.FALSE, ExpectTargetsEmpty.TRUE, SystemOfRecord.SQUID, CreateSources.FALSE,
            PlasticToValidate.SOURCE, PipelineTransformation.LIGATION, ForwardMessage.NONE, VolumeConcUpdate.MERCURY_ONLY),
    JUMP_ADAPTER_LIGATION_WASH("JumpAdapterLigationWash",
            ExpectSourcesEmpty.FALSE, ExpectTargetsEmpty.TRUE, SystemOfRecord.SQUID, CreateSources.FALSE,
            PlasticToValidate.SOURCE, PipelineTransformation.NONE, ForwardMessage.NONE, VolumeConcUpdate.MERCURY_ONLY),
    JUMP_AMPLIFICATION("JumpAmplification",
            ExpectSourcesEmpty.FALSE, ExpectTargetsEmpty.TRUE, SystemOfRecord.SQUID, CreateSources.FALSE,
            PlasticToValidate.SOURCE, PipelineTransformation.NONE, ForwardMessage.NONE, VolumeConcUpdate.MERCURY_ONLY),
    JUMP_FINAL_LIBRARY_ELUTION("JumpFinalLibraryElution",
            ExpectSourcesEmpty.FALSE, ExpectTargetsEmpty.TRUE, SystemOfRecord.SQUID, CreateSources.FALSE,
            PlasticToValidate.SOURCE, PipelineTransformation.NONE, ForwardMessage.NONE, VolumeConcUpdate.MERCURY_ONLY),
    JUMP_FINAL_LIBRARY_SIZE_SELECTION("JumpFinalLibrarySizeSelection",
            ExpectSourcesEmpty.FALSE, ExpectTargetsEmpty.TRUE, SystemOfRecord.SQUID, CreateSources.FALSE,
            PlasticToValidate.SOURCE, PipelineTransformation.SIZE_SELECTION, ForwardMessage.NONE, VolumeConcUpdate.MERCURY_ONLY),
    JUMP_FINAL_LIBRARY_QC("JumpFinalLibraryQC",
            ExpectSourcesEmpty.FALSE, ExpectTargetsEmpty.TRUE, SystemOfRecord.SQUID, CreateSources.FALSE,
            PlasticToValidate.SOURCE, PipelineTransformation.NONE, ForwardMessage.NONE, VolumeConcUpdate.MERCURY_ONLY),
    JUMP_ENRICHED_LIBRARY_REGISTRATION("JumpEnrichedLibraryRegistration",
            ExpectSourcesEmpty.FALSE, ExpectTargetsEmpty.TRUE, SystemOfRecord.SQUID, CreateSources.FALSE,
            PlasticToValidate.SOURCE, PipelineTransformation.NONE, ForwardMessage.NONE, VolumeConcUpdate.MERCURY_ONLY),

    // BSP Pico
    PICO_PLATING_BUCKET("PicoPlatingBucket",
            ExpectSourcesEmpty.TRUE, ExpectTargetsEmpty.TRUE, SystemOfRecord.MERCURY, CreateSources.FALSE,
            PlasticToValidate.SOURCE, PipelineTransformation.NONE, ForwardMessage.NONE, VolumeConcUpdate.MERCURY_ONLY),
    PICO_PLATING_QC("PicoPlatingQC",
            ExpectSourcesEmpty.TRUE, ExpectTargetsEmpty.TRUE, SystemOfRecord.MERCURY, CreateSources.FALSE,
            PlasticToValidate.SOURCE, PipelineTransformation.NONE, ForwardMessage.NONE, VolumeConcUpdate.MERCURY_ONLY),
    PICO_DILUTION_TRANSFER("PicoDilutionTransfer",
            ExpectSourcesEmpty.TRUE, ExpectTargetsEmpty.TRUE, SystemOfRecord.MERCURY, CreateSources.FALSE,
            PlasticToValidate.SOURCE, PipelineTransformation.NONE, ForwardMessage.NONE, VolumeConcUpdate.MERCURY_ONLY),
    PICO_BUFFER_ADDITION("PicoBufferAddition",
            ExpectSourcesEmpty.TRUE, ExpectTargetsEmpty.TRUE, SystemOfRecord.MERCURY, CreateSources.FALSE,
            PlasticToValidate.SOURCE, PipelineTransformation.NONE, ForwardMessage.NONE, VolumeConcUpdate.MERCURY_ONLY),
    PICO_MICROFLUOR_TRANSFER("PicoMicrofluorTransfer",
            ExpectSourcesEmpty.TRUE, ExpectTargetsEmpty.TRUE, SystemOfRecord.MERCURY, CreateSources.FALSE,
            PlasticToValidate.SOURCE, PipelineTransformation.NONE, ForwardMessage.NONE, VolumeConcUpdate.MERCURY_ONLY),
    PICO_STANDARDS_TRANSFER("PicoStandardsTransfer",
            ExpectSourcesEmpty.TRUE, ExpectTargetsEmpty.TRUE, SystemOfRecord.MERCURY, CreateSources.TRUE,
            PlasticToValidate.SOURCE, PipelineTransformation.NONE, ForwardMessage.NONE, VolumeConcUpdate.MERCURY_ONLY),
    PICO_PLATING_POST_NORM_PICO("PicoPlatingPostNorm",
            ExpectSourcesEmpty.TRUE, ExpectTargetsEmpty.TRUE, SystemOfRecord.MERCURY, CreateSources.FALSE,
            PlasticToValidate.SOURCE, PipelineTransformation.NONE, ForwardMessage.NONE, VolumeConcUpdate.MERCURY_ONLY),
    PICO_REWORK("PicoRework",
            ExpectSourcesEmpty.TRUE, ExpectTargetsEmpty.TRUE, SystemOfRecord.MERCURY, CreateSources.FALSE,
            PlasticToValidate.SOURCE, PipelineTransformation.NONE, ForwardMessage.NONE, VolumeConcUpdate.BSP_AND_MERCURY),
    PICO_TRANSFER("PicoTransfer",
            ExpectSourcesEmpty.FALSE, ExpectTargetsEmpty.TRUE, SystemOfRecord.MERCURY, CreateSources.FALSE,
            PlasticToValidate.SOURCE, PipelineTransformation.NONE, ForwardMessage.NONE, VolumeConcUpdate.MERCURY_ONLY),

    // Dried Blood Spot
    DBS_SAMPLE_PUNCH("DBSSamplePunch",
            ExpectSourcesEmpty.FALSE, ExpectTargetsEmpty.TRUE, SystemOfRecord.MERCURY, CreateSources.FALSE,
            PlasticToValidate.SOURCE, PipelineTransformation.NONE, ForwardMessage.NONE, VolumeConcUpdate.MERCURY_ONLY),
    DBS_INCUBATION_MIX("DBSIncubationMix",
            ExpectSourcesEmpty.TRUE, ExpectTargetsEmpty.FALSE, SystemOfRecord.MERCURY, CreateSources.FALSE,
            PlasticToValidate.SOURCE, PipelineTransformation.NONE, ForwardMessage.NONE, VolumeConcUpdate.MERCURY_ONLY),
    DBS_LYSIS_BUFFER("DBSLysisBuffer",
            ExpectSourcesEmpty.TRUE, ExpectTargetsEmpty.FALSE, SystemOfRecord.MERCURY, CreateSources.FALSE,
            PlasticToValidate.SOURCE, PipelineTransformation.NONE, ForwardMessage.NONE, VolumeConcUpdate.MERCURY_ONLY),
    DBS_MAGNETIC_RESIN("DBSMagneticResin",
            ExpectSourcesEmpty.TRUE, ExpectTargetsEmpty.FALSE, SystemOfRecord.MERCURY, CreateSources.FALSE,
            PlasticToValidate.SOURCE, PipelineTransformation.NONE, ForwardMessage.NONE, VolumeConcUpdate.MERCURY_ONLY),
    DBS_1ST_PURIFICATION("DBS1stPurification",
            ExpectSourcesEmpty.FALSE, ExpectTargetsEmpty.TRUE, SystemOfRecord.MERCURY, CreateSources.FALSE,
            PlasticToValidate.SOURCE, PipelineTransformation.NONE, ForwardMessage.NONE, VolumeConcUpdate.MERCURY_ONLY),
    DBS_WASH_BUFFER("DBSWashBuffer",
            ExpectSourcesEmpty.TRUE, ExpectTargetsEmpty.FALSE, SystemOfRecord.MERCURY, CreateSources.FALSE,
            PlasticToValidate.SOURCE, PipelineTransformation.NONE, ForwardMessage.NONE, VolumeConcUpdate.MERCURY_ONLY),
    DBS_2ND_PURIFICATION("DBS2ndPurification",
            ExpectSourcesEmpty.FALSE, ExpectTargetsEmpty.TRUE, SystemOfRecord.MERCURY, CreateSources.FALSE,
            PlasticToValidate.SOURCE, PipelineTransformation.NONE, ForwardMessage.NONE, VolumeConcUpdate.MERCURY_ONLY),
    DBS_ELUTION_BUFFER("DBSElutionBuffer",
            ExpectSourcesEmpty.FALSE, ExpectTargetsEmpty.TRUE, SystemOfRecord.MERCURY, CreateSources.FALSE,
            PlasticToValidate.SOURCE, PipelineTransformation.NONE, ForwardMessage.NONE, VolumeConcUpdate.MERCURY_ONLY),
    DBS_FINAL_TRANSFER("DBSFinalTransfer",
            ExpectSourcesEmpty.FALSE, ExpectTargetsEmpty.TRUE, SystemOfRecord.MERCURY, CreateSources.FALSE,
            PlasticToValidate.SOURCE, PipelineTransformation.NONE, ForwardMessage.NONE, VolumeConcUpdate.MERCURY_ONLY),

    //Cryovial Blood and Saliva Extraction
    BLOOD_CRYOVIAL_EXTRACTION("BloodCryovialExtraction",
            ExpectSourcesEmpty.FALSE, ExpectTargetsEmpty.TRUE, SystemOfRecord.MERCURY, CreateSources.TRUE,
            PlasticToValidate.SOURCE, PipelineTransformation.NONE, ForwardMessage.BSP, VolumeConcUpdate.MERCURY_ONLY),
    BLOOD_DEEPWELL_CHEMAGEN_TRANSFER("ExtractionsBloodDeepwellToChemagen",
            ExpectSourcesEmpty.TRUE, ExpectTargetsEmpty.TRUE, SystemOfRecord.MERCURY, CreateSources.FALSE,
            PlasticToValidate.SOURCE, PipelineTransformation.NONE, ForwardMessage.BSP, VolumeConcUpdate.MERCURY_ONLY),
    BLOOD_CHEMAGEN_TO_FINAL_RACK("ExtractionsBloodChemagenToFinalRack",
            ExpectSourcesEmpty.TRUE, ExpectTargetsEmpty.TRUE, SystemOfRecord.MERCURY, CreateSources.FALSE,
            PlasticToValidate.SOURCE, PipelineTransformation.NONE, ForwardMessage.BSP, VolumeConcUpdate.MERCURY_ONLY),
    SALIVA_CRYOVIAL_EXTRACTION("SalivaCryovialExtraction",
            ExpectSourcesEmpty.FALSE, ExpectTargetsEmpty.TRUE, SystemOfRecord.MERCURY, CreateSources.TRUE,
            PlasticToValidate.SOURCE, PipelineTransformation.NONE, ForwardMessage.BSP, VolumeConcUpdate.MERCURY_ONLY),
    SALIVA_DEEPWELL_CHEMAGEN_TRANSFER("ExtractionsSalivaDeepwellToChemagen",
            ExpectSourcesEmpty.TRUE, ExpectTargetsEmpty.TRUE, SystemOfRecord.MERCURY, CreateSources.FALSE,
            PlasticToValidate.SOURCE, PipelineTransformation.NONE, ForwardMessage.BSP, VolumeConcUpdate.MERCURY_ONLY),
    SALIVA_CHEMAGEN_TO_FINAL_RACK("ExtractionsSalivaChemagenToFinalRack",
            ExpectSourcesEmpty.TRUE, ExpectTargetsEmpty.TRUE, SystemOfRecord.MERCURY, CreateSources.FALSE,
            PlasticToValidate.SOURCE, PipelineTransformation.NONE, ForwardMessage.BSP, VolumeConcUpdate.MERCURY_ONLY),


    // TruSeq Custom Amplicon
    TSCA_POST_NORM_TRANSFER("TSCAPostNormTransfer",
            ExpectSourcesEmpty.FALSE, ExpectTargetsEmpty.TRUE, SystemOfRecord.SQUID, CreateSources.FALSE,
            PlasticToValidate.SOURCE, PipelineTransformation.NONE, ForwardMessage.NONE, VolumeConcUpdate.MERCURY_ONLY),
    TSCA_HYB_SET_UP("TSCAHybSetUp",
            ExpectSourcesEmpty.FALSE, ExpectTargetsEmpty.TRUE, SystemOfRecord.SQUID, CreateSources.FALSE,
            PlasticToValidate.SOURCE, PipelineTransformation.NONE, ForwardMessage.NONE, VolumeConcUpdate.MERCURY_ONLY),
    TSCA_CAT_ADDITION("TSCACATAddition",
            ExpectSourcesEmpty.FALSE, ExpectTargetsEmpty.TRUE, SystemOfRecord.SQUID, CreateSources.FALSE,
            PlasticToValidate.SOURCE, PipelineTransformation.NONE, ForwardMessage.NONE, VolumeConcUpdate.MERCURY_ONLY),
    TSCA_HYBRIDIZATION_CLEANUP("TSCAHybridizationCleanUp",
            ExpectSourcesEmpty.FALSE, ExpectTargetsEmpty.TRUE, SystemOfRecord.SQUID, CreateSources.FALSE,
            PlasticToValidate.SOURCE, PipelineTransformation.NONE, ForwardMessage.NONE, VolumeConcUpdate.MERCURY_ONLY),
    TSCA_FLP_PREP("TSCAFLPPrep",
            ExpectSourcesEmpty.FALSE, ExpectTargetsEmpty.TRUE, SystemOfRecord.SQUID, CreateSources.FALSE,
            PlasticToValidate.SOURCE, PipelineTransformation.NONE, ForwardMessage.NONE, VolumeConcUpdate.MERCURY_ONLY),
    TSCA_SW1_ADDITION1("TSCASW1Addition1",
            ExpectSourcesEmpty.FALSE, ExpectTargetsEmpty.TRUE, SystemOfRecord.SQUID, CreateSources.FALSE,
            PlasticToValidate.SOURCE, PipelineTransformation.NONE, ForwardMessage.NONE, VolumeConcUpdate.MERCURY_ONLY),
    TSCA_SW1_ADDITION2("TSCASW1Addition2",
            ExpectSourcesEmpty.FALSE, ExpectTargetsEmpty.TRUE, SystemOfRecord.SQUID, CreateSources.FALSE,
            PlasticToValidate.SOURCE, PipelineTransformation.NONE, ForwardMessage.NONE, VolumeConcUpdate.MERCURY_ONLY),
    TSCA_UB1_ADDITION("TSCAUB1Addition",
            ExpectSourcesEmpty.FALSE, ExpectTargetsEmpty.TRUE, SystemOfRecord.SQUID, CreateSources.FALSE,
            PlasticToValidate.SOURCE, PipelineTransformation.NONE, ForwardMessage.NONE, VolumeConcUpdate.MERCURY_ONLY),
    TSCA_EXTENSION_LIGATION_SETUP("TSCAExtensionLigationSetup",
            ExpectSourcesEmpty.FALSE, ExpectTargetsEmpty.TRUE, SystemOfRecord.SQUID, CreateSources.FALSE,
            PlasticToValidate.SOURCE, PipelineTransformation.NONE, ForwardMessage.NONE, VolumeConcUpdate.MERCURY_ONLY),
    TSCA_INDEXED_AMPLICON_PCR("TSCAIndexedAmpliconPCR",
            ExpectSourcesEmpty.FALSE, ExpectTargetsEmpty.TRUE, SystemOfRecord.SQUID, CreateSources.FALSE,
            PlasticToValidate.SOURCE, PipelineTransformation.NONE, ForwardMessage.NONE, VolumeConcUpdate.MERCURY_ONLY),
    TSCA_INDEX_ADDITION("TSCAIndexAddition",
            ExpectSourcesEmpty.FALSE, ExpectTargetsEmpty.TRUE, SystemOfRecord.SQUID, CreateSources.FALSE,
            PlasticToValidate.SOURCE, PipelineTransformation.LIGATION, ForwardMessage.NONE, VolumeConcUpdate.MERCURY_ONLY),
    TSCA_AMPLICON_REGISTRATION("TSCAAmpliconRegistration",
            ExpectSourcesEmpty.FALSE, ExpectTargetsEmpty.TRUE, SystemOfRecord.SQUID, CreateSources.FALSE,
            PlasticToValidate.SOURCE, PipelineTransformation.NONE, ForwardMessage.NONE, VolumeConcUpdate.MERCURY_ONLY),
    TSCA_AMPLICON_PCR_CLEANUP("TSCAAmpliconPCRCleanup",
            ExpectSourcesEmpty.FALSE, ExpectTargetsEmpty.TRUE, SystemOfRecord.SQUID, CreateSources.FALSE,
            PlasticToValidate.SOURCE, PipelineTransformation.NONE, ForwardMessage.NONE, VolumeConcUpdate.MERCURY_ONLY),
    TSCA_POOL_CREATION("TSCAPoolCreation",
            ExpectSourcesEmpty.FALSE, ExpectTargetsEmpty.TRUE, SystemOfRecord.SQUID, CreateSources.FALSE,
            PlasticToValidate.SOURCE, PipelineTransformation.NONE, ForwardMessage.NONE, VolumeConcUpdate.MERCURY_ONLY),

    // 16S
    ILLUMINA_16S_TO_PRIMER_TRANSFER("Illumina16StoPrimerTransfer",
            ExpectSourcesEmpty.FALSE, ExpectTargetsEmpty.TRUE, SystemOfRecord.SQUID, CreateSources.FALSE,
            PlasticToValidate.SOURCE, PipelineTransformation.LIGATION, ForwardMessage.NONE, VolumeConcUpdate.MERCURY_ONLY),
    ILLUMINA_16S_PCR_PRODUCT_TRANSFER("Illumina16SPcrProductTransfer",
            ExpectSourcesEmpty.FALSE, ExpectTargetsEmpty.TRUE, SystemOfRecord.SQUID, CreateSources.FALSE,
            PlasticToValidate.SOURCE, PipelineTransformation.NONE, ForwardMessage.NONE, VolumeConcUpdate.MERCURY_ONLY),

    // cDNA dUTP
    POLY_A_TRANSFER("PolyATransfer",
            ExpectSourcesEmpty.FALSE, ExpectTargetsEmpty.TRUE, SystemOfRecord.SQUID, CreateSources.FALSE,
            PlasticToValidate.SOURCE, PipelineTransformation.NONE, ForwardMessage.NONE, VolumeConcUpdate.MERCURY_ONLY),
    POLY_A_SELECTION("PolyASelection",
            ExpectSourcesEmpty.FALSE, ExpectTargetsEmpty.TRUE, SystemOfRecord.SQUID, CreateSources.FALSE,
            PlasticToValidate.SOURCE, PipelineTransformation.NONE, ForwardMessage.NONE, VolumeConcUpdate.MERCURY_ONLY),
    DNASE("Dnase",
            ExpectSourcesEmpty.FALSE, ExpectTargetsEmpty.TRUE, SystemOfRecord.SQUID, CreateSources.FALSE,
            PlasticToValidate.SOURCE, PipelineTransformation.NONE, ForwardMessage.NONE, VolumeConcUpdate.MERCURY_ONLY),
    DNASE_CLEANUP("DnaseCleanup",
            ExpectSourcesEmpty.FALSE, ExpectTargetsEmpty.TRUE, SystemOfRecord.SQUID, CreateSources.FALSE,
            PlasticToValidate.SOURCE, PipelineTransformation.NONE, ForwardMessage.NONE, VolumeConcUpdate.MERCURY_ONLY),
    FRAGMENTATION("Fragmentation",
            ExpectSourcesEmpty.FALSE, ExpectTargetsEmpty.TRUE, SystemOfRecord.SQUID, CreateSources.FALSE,
            PlasticToValidate.SOURCE, PipelineTransformation.NONE, ForwardMessage.NONE, VolumeConcUpdate.MERCURY_ONLY),
    FRAGMENTATION_CLEANUP("FragmentationCleanup",
            ExpectSourcesEmpty.FALSE, ExpectTargetsEmpty.TRUE, SystemOfRecord.SQUID, CreateSources.FALSE,
            PlasticToValidate.SOURCE, PipelineTransformation.NONE, ForwardMessage.NONE, VolumeConcUpdate.MERCURY_ONLY),
    FIRST_STRAND("FirstStrand",
            ExpectSourcesEmpty.FALSE, ExpectTargetsEmpty.TRUE, SystemOfRecord.SQUID, CreateSources.FALSE,
            PlasticToValidate.SOURCE, PipelineTransformation.NONE, ForwardMessage.NONE, VolumeConcUpdate.MERCURY_ONLY),
    FIRST_STRAND_CLEANUP("FirstStrandCleanup",
            ExpectSourcesEmpty.FALSE, ExpectTargetsEmpty.TRUE, SystemOfRecord.SQUID, CreateSources.FALSE,
            PlasticToValidate.SOURCE, PipelineTransformation.NONE, ForwardMessage.NONE, VolumeConcUpdate.MERCURY_ONLY),
    SECOND_STRAND("SecondStrand",
            ExpectSourcesEmpty.FALSE, ExpectTargetsEmpty.TRUE, SystemOfRecord.SQUID, CreateSources.FALSE,
            PlasticToValidate.SOURCE, PipelineTransformation.NONE, ForwardMessage.NONE, VolumeConcUpdate.MERCURY_ONLY),
    SECOND_STRAND_CLEANUP("SecondStrandCleanup",
            ExpectSourcesEmpty.FALSE, ExpectTargetsEmpty.TRUE, SystemOfRecord.SQUID, CreateSources.FALSE,
            PlasticToValidate.SOURCE, PipelineTransformation.NONE, ForwardMessage.NONE, VolumeConcUpdate.MERCURY_ONLY),
    USER("USER",
            ExpectSourcesEmpty.FALSE, ExpectTargetsEmpty.TRUE, SystemOfRecord.SQUID, CreateSources.FALSE,
            PlasticToValidate.SOURCE, PipelineTransformation.NONE, ForwardMessage.NONE, VolumeConcUpdate.MERCURY_ONLY),
    CDNA_PONDENRICHMENT_CLEANUP("cDNAPondEnrichmentCleanup",
            ExpectSourcesEmpty.FALSE, ExpectTargetsEmpty.TRUE, SystemOfRecord.SQUID, CreateSources.FALSE,
            PlasticToValidate.SOURCE, PipelineTransformation.PCR, ForwardMessage.NONE, VolumeConcUpdate.MERCURY_ONLY),

    // cDNA TruSeq
    POLY_A_SELECTION_TS("PolyASelectionTS",
            ExpectSourcesEmpty.FALSE, ExpectTargetsEmpty.TRUE, SystemOfRecord.SQUID, CreateSources.FALSE,
            PlasticToValidate.SOURCE, PipelineTransformation.NONE, ForwardMessage.NONE, VolumeConcUpdate.MERCURY_ONLY),
    FIRST_STRAND_TS("FirstStrandTS",
            ExpectSourcesEmpty.FALSE, ExpectTargetsEmpty.TRUE, SystemOfRecord.SQUID, CreateSources.FALSE,
            PlasticToValidate.SOURCE, PipelineTransformation.NONE, ForwardMessage.NONE, VolumeConcUpdate.MERCURY_ONLY),
    SECOND_STRAND_TS("SecondStrandTS",
            ExpectSourcesEmpty.FALSE, ExpectTargetsEmpty.TRUE, SystemOfRecord.SQUID, CreateSources.FALSE,
            PlasticToValidate.SOURCE, PipelineTransformation.NONE, ForwardMessage.NONE, VolumeConcUpdate.MERCURY_ONLY),
    SECOND_STRAND_CLEANUP_TS("SecondStrandCleanupTS",
            ExpectSourcesEmpty.FALSE, ExpectTargetsEmpty.TRUE, SystemOfRecord.SQUID, CreateSources.FALSE,
            PlasticToValidate.SOURCE, PipelineTransformation.NONE, ForwardMessage.NONE, VolumeConcUpdate.MERCURY_ONLY),
    END_REPAIR_TS("EndRepairTS",
            ExpectSourcesEmpty.FALSE, ExpectTargetsEmpty.TRUE, SystemOfRecord.SQUID, CreateSources.FALSE,
            PlasticToValidate.SOURCE, PipelineTransformation.NONE, ForwardMessage.NONE, VolumeConcUpdate.MERCURY_ONLY),
    END_REPAIR_CLEANUP_TS("EndRepairCleanupTS",
            ExpectSourcesEmpty.FALSE, ExpectTargetsEmpty.TRUE, SystemOfRecord.SQUID, CreateSources.FALSE,
            PlasticToValidate.SOURCE, PipelineTransformation.NONE, ForwardMessage.NONE, VolumeConcUpdate.MERCURY_ONLY),
    A_BASE_TS("ABaseTS",
            ExpectSourcesEmpty.FALSE, ExpectTargetsEmpty.TRUE, SystemOfRecord.SQUID, CreateSources.FALSE,
            PlasticToValidate.SOURCE, PipelineTransformation.NONE, ForwardMessage.NONE, VolumeConcUpdate.MERCURY_ONLY),
    INDEXED_ADAPTER_LIGATION_TS("IndexedAdapterLigationTS",
            ExpectSourcesEmpty.FALSE, ExpectTargetsEmpty.TRUE, SystemOfRecord.SQUID, CreateSources.FALSE,
            PlasticToValidate.SOURCE, PipelineTransformation.LIGATION, ForwardMessage.NONE, VolumeConcUpdate.MERCURY_ONLY),
    ADAPTER_LIGATION_CLEANUP_TS("AdapterLigationCleanupTS",
            ExpectSourcesEmpty.FALSE, ExpectTargetsEmpty.TRUE, SystemOfRecord.SQUID, CreateSources.FALSE,
            PlasticToValidate.SOURCE, PipelineTransformation.NONE, ForwardMessage.NONE, VolumeConcUpdate.MERCURY_ONLY),
    ENRICHMENT_TS("EnrichmentTS",
            ExpectSourcesEmpty.FALSE, ExpectTargetsEmpty.TRUE, SystemOfRecord.SQUID, CreateSources.FALSE,
            PlasticToValidate.SOURCE, PipelineTransformation.PCR, ForwardMessage.NONE, VolumeConcUpdate.MERCURY_ONLY),
    ENRICHMENT_CLEANUP_TS("EnrichmentCleanupTS",
            ExpectSourcesEmpty.FALSE, ExpectTargetsEmpty.TRUE, SystemOfRecord.SQUID, CreateSources.FALSE,
            PlasticToValidate.SOURCE, PipelineTransformation.NONE, ForwardMessage.NONE, VolumeConcUpdate.MERCURY_ONLY),

    // Illumina Genome Network
    IGN_PRE_NORM_TRANSFER("IGNPreNormTransfer",
            ExpectSourcesEmpty.FALSE, ExpectTargetsEmpty.TRUE, SystemOfRecord.SQUID, CreateSources.FALSE,
            PlasticToValidate.SOURCE, PipelineTransformation.NONE, ForwardMessage.NONE, VolumeConcUpdate.MERCURY_ONLY),
    IGN_NORM_TRANSFER("IGNNormTransfer",
            ExpectSourcesEmpty.FALSE, ExpectTargetsEmpty.TRUE, SystemOfRecord.SQUID, CreateSources.FALSE,
            PlasticToValidate.SOURCE, PipelineTransformation.NONE, ForwardMessage.NONE, VolumeConcUpdate.MERCURY_ONLY),
    IGN_INCOMING_PICO("IGNIncomingPico",
            ExpectSourcesEmpty.FALSE, ExpectTargetsEmpty.TRUE, SystemOfRecord.SQUID, CreateSources.FALSE,
            PlasticToValidate.SOURCE, PipelineTransformation.NONE, ForwardMessage.NONE, VolumeConcUpdate.MERCURY_ONLY),
    IGN_GAP_ALIQUOT("IGNGapAliquot",
            ExpectSourcesEmpty.FALSE, ExpectTargetsEmpty.TRUE, SystemOfRecord.SQUID, CreateSources.FALSE,
            PlasticToValidate.SOURCE, PipelineTransformation.NONE, ForwardMessage.NONE, VolumeConcUpdate.MERCURY_ONLY),

    VIRAL_PCR_PRODUCT_POOL_TRANSFER("ViralPcrProductPoolTransfer",
            ExpectSourcesEmpty.FALSE, ExpectTargetsEmpty.TRUE, SystemOfRecord.SQUID, CreateSources.FALSE,
            PlasticToValidate.SOURCE, PipelineTransformation.NONE, ForwardMessage.NONE, VolumeConcUpdate.MERCURY_ONLY),
    ONE_STEP_RT_PCR_PRODUCT_POOL_TRANSFER("OneStepRtPcrProductPoolTransfer",
            ExpectSourcesEmpty.FALSE, ExpectTargetsEmpty.TRUE, SystemOfRecord.SQUID, CreateSources.FALSE,
            PlasticToValidate.SOURCE, PipelineTransformation.NONE, ForwardMessage.NONE, VolumeConcUpdate.MERCURY_ONLY),

    // From BSP UI
    /**
     * "Packaging" indicates the samples are being placed in a package for shipping.
     */
    SAMPLE_PACKAGE("SamplePackage",
            ExpectSourcesEmpty.FALSE, ExpectTargetsEmpty.TRUE, SystemOfRecord.MERCURY, CreateSources.FALSE,
            PlasticToValidate.SOURCE, PipelineTransformation.NONE, ForwardMessage.NONE, VolumeConcUpdate.MERCURY_ONLY),
    INITIAL_TARE("InitialTare",
            ExpectSourcesEmpty.FALSE, ExpectTargetsEmpty.TRUE, SystemOfRecord.MERCURY, CreateSources.FALSE,
            PlasticToValidate.SOURCE, PipelineTransformation.NONE, ForwardMessage.NONE, VolumeConcUpdate.BSP_AND_MERCURY),
    SAMPLE_RECEIPT("SampleReceipt",
            ExpectSourcesEmpty.FALSE, ExpectTargetsEmpty.TRUE, SystemOfRecord.MERCURY, CreateSources.FALSE,
            PlasticToValidate.SOURCE, PipelineTransformation.NONE, ForwardMessage.NONE, VolumeConcUpdate.BSP_AND_MERCURY),
    WEIGHT_MEASUREMENT("WeightMeasurement",
            ExpectSourcesEmpty.FALSE, ExpectTargetsEmpty.TRUE, SystemOfRecord.MERCURY, CreateSources.FALSE,
            PlasticToValidate.SOURCE, PipelineTransformation.NONE, ForwardMessage.NONE, VolumeConcUpdate.BSP_AND_MERCURY),
    VOLUME_ADDITION("VolumeAddition",
            ExpectSourcesEmpty.FALSE, ExpectTargetsEmpty.TRUE, SystemOfRecord.MERCURY, CreateSources.FALSE,
            PlasticToValidate.SOURCE, PipelineTransformation.NONE, ForwardMessage.NONE, VolumeConcUpdate.MERCURY_ONLY),
    INITIAL_NORMALIZATION("InitialNormalization",
            ExpectSourcesEmpty.FALSE, ExpectTargetsEmpty.TRUE, SystemOfRecord.MERCURY, CreateSources.FALSE,
            PlasticToValidate.SOURCE, PipelineTransformation.NONE, ForwardMessage.NONE, VolumeConcUpdate.MERCURY_ONLY),
    SAMPLES_EXTRACTION_START("SamplesExtractionStart",
            ExpectSourcesEmpty.FALSE, ExpectTargetsEmpty.TRUE, SystemOfRecord.MERCURY, CreateSources.FALSE,
            PlasticToValidate.SOURCE, PipelineTransformation.NONE, ForwardMessage.NONE, VolumeConcUpdate.MERCURY_ONLY),
    SAMPLES_EXTRACTION_END_TRANSFER("SamplesExtractionEndTransfer",
            ExpectSourcesEmpty.FALSE, ExpectTargetsEmpty.TRUE, SystemOfRecord.MERCURY, CreateSources.FALSE,
            PlasticToValidate.SOURCE, PipelineTransformation.NONE, ForwardMessage.NONE, VolumeConcUpdate.MERCURY_ONLY),
    SAMPLES_NORMALIZATION_TRANSFER("SamplesNormalizationTransfer",
            ExpectSourcesEmpty.FALSE, ExpectTargetsEmpty.TRUE, SystemOfRecord.MERCURY, CreateSources.FALSE,
            PlasticToValidate.SOURCE, PipelineTransformation.NONE, ForwardMessage.NONE, VolumeConcUpdate.MERCURY_ONLY),
    SAMPLES_PLATING_TO_COVARIS("SamplesPlatingToCovaris",
            ExpectSourcesEmpty.FALSE, ExpectTargetsEmpty.TRUE, SystemOfRecord.MERCURY, CreateSources.FALSE,
            PlasticToValidate.SOURCE, PipelineTransformation.NONE, ForwardMessage.NONE, VolumeConcUpdate.MERCURY_ONLY),
    SAMPLES_DAUGHTER_PLATE_CREATION("SamplesDaughterPlateCreation",
            ExpectSourcesEmpty.FALSE, ExpectTargetsEmpty.FALSE, SystemOfRecord.MERCURY, CreateSources.TRUE,
            PlasticToValidate.BOTH, PipelineTransformation.NONE, ForwardMessage.NONE, VolumeConcUpdate.MERCURY_ONLY),
    AUTO_DAUGHTER_PLATE_CREATION("AutomatedDaughterPlateCreation",
            ExpectSourcesEmpty.FALSE, ExpectTargetsEmpty.FALSE, SystemOfRecord.MERCURY, CreateSources.TRUE,
            PlasticToValidate.BOTH, PipelineTransformation.NONE, ForwardMessage.BSP, VolumeConcUpdate.MERCURY_ONLY),
    SAMPLE_IMPORT("SampleImport",
            ExpectSourcesEmpty.FALSE, ExpectTargetsEmpty.TRUE, SystemOfRecord.MERCURY, CreateSources.FALSE,
            PlasticToValidate.SOURCE, PipelineTransformation.NONE, ForwardMessage.NONE, VolumeConcUpdate.MERCURY_ONLY),
    SEQ_PLATING_NORMALIZATION("SeqPlatingNormalization",
            ExpectSourcesEmpty.FALSE, ExpectTargetsEmpty.TRUE, SystemOfRecord.MERCURY, CreateSources.TRUE,
            PlasticToValidate.SOURCE, PipelineTransformation.NONE, ForwardMessage.NONE, VolumeConcUpdate.BSP_AND_MERCURY),
    ARRAY_PLATING_DILUTION("ArrayPlatingDilution",
            ExpectSourcesEmpty.FALSE, ExpectTargetsEmpty.TRUE, SystemOfRecord.MERCURY, CreateSources.TRUE,
            PlasticToValidate.SOURCE, PipelineTransformation.NONE, ForwardMessage.BSP, VolumeConcUpdate.MERCURY_ONLY),
    SHEARING_ALIQUOT("ShearingAliquot",
            ExpectSourcesEmpty.FALSE, ExpectTargetsEmpty.TRUE, SystemOfRecord.MERCURY, CreateSources.FALSE,
            PlasticToValidate.SOURCE, PipelineTransformation.NONE, ForwardMessage.NONE, VolumeConcUpdate.MERCURY_ONLY),
    FINGERPRINTING_ALIQUOT("FingerprintingAliquot",
            ExpectSourcesEmpty.FALSE, ExpectTargetsEmpty.TRUE, SystemOfRecord.MERCURY, CreateSources.FALSE,
            PlasticToValidate.SOURCE, PipelineTransformation.NONE, ForwardMessage.NONE, VolumeConcUpdate.MERCURY_ONLY),
    FINGERPRINTING_PLATE_SETUP("FingerprintingPlateSetup",
            ExpectSourcesEmpty.FALSE, ExpectTargetsEmpty.TRUE, SystemOfRecord.MERCURY, CreateSources.FALSE,
            PlasticToValidate.SOURCE, PipelineTransformation.NONE, ForwardMessage.NONE, VolumeConcUpdate.MERCURY_ONLY),

    // mRRBS
    MRRBS_GENOMIC_TRANSFER("mRRBSGenomicTransfer",
            ExpectSourcesEmpty.FALSE, ExpectTargetsEmpty.TRUE, SystemOfRecord.SQUID, CreateSources.FALSE,
            PlasticToValidate.SOURCE, PipelineTransformation.NONE, ForwardMessage.NONE, VolumeConcUpdate.MERCURY_ONLY),
    MRRBS_INDEXING("mRRBSIndexing",
            ExpectSourcesEmpty.FALSE, ExpectTargetsEmpty.TRUE, SystemOfRecord.SQUID, CreateSources.FALSE,
            PlasticToValidate.SOURCE, PipelineTransformation.LIGATION, ForwardMessage.NONE, VolumeConcUpdate.MERCURY_ONLY),
    MRRBS_NORM_LIBS("mRRBSNormLibs",
            ExpectSourcesEmpty.FALSE, ExpectTargetsEmpty.TRUE, SystemOfRecord.SQUID, CreateSources.FALSE,
            PlasticToValidate.SOURCE, PipelineTransformation.NONE, ForwardMessage.NONE, VolumeConcUpdate.MERCURY_ONLY),
    MRRBS_FINAL_PRODUCT_POOL("mRRBSFinalProductPool",
            ExpectSourcesEmpty.FALSE, ExpectTargetsEmpty.TRUE, SystemOfRecord.SQUID, CreateSources.FALSE,
            PlasticToValidate.SOURCE, PipelineTransformation.NONE, ForwardMessage.NONE, VolumeConcUpdate.MERCURY_ONLY),

    // Malaria Illumina
    MALARIA_PCR1("Malaria_PCR1",
            ExpectSourcesEmpty.FALSE, ExpectTargetsEmpty.TRUE, SystemOfRecord.SQUID, CreateSources.FALSE,
            PlasticToValidate.SOURCE, PipelineTransformation.LIGATION, ForwardMessage.NONE, VolumeConcUpdate.MERCURY_ONLY),
    MALARIA_PCRTAIL2("Malaria_PCRTail2",
            ExpectSourcesEmpty.FALSE, ExpectTargetsEmpty.TRUE, SystemOfRecord.SQUID, CreateSources.FALSE,
            PlasticToValidate.SOURCE, PipelineTransformation.LIGATION, ForwardMessage.NONE, VolumeConcUpdate.MERCURY_ONLY),
    MALARIA_PCR_NORM("Malaria_PCR_Norm",
            ExpectSourcesEmpty.FALSE, ExpectTargetsEmpty.TRUE, SystemOfRecord.SQUID, CreateSources.FALSE,
            PlasticToValidate.SOURCE, PipelineTransformation.NONE, ForwardMessage.NONE, VolumeConcUpdate.MERCURY_ONLY),
    MALARIA_MISEQ_PCR_POOL_SPRI("MalariaMiseqPCRPoolSPRI",
            ExpectSourcesEmpty.FALSE, ExpectTargetsEmpty.TRUE, SystemOfRecord.SQUID, CreateSources.FALSE,
            PlasticToValidate.SOURCE, PipelineTransformation.NONE, ForwardMessage.NONE, VolumeConcUpdate.MERCURY_ONLY),

    // Malaria PacBio
    MALARIA_BEP_PCR("Malaria_BEP_PCR",
            ExpectSourcesEmpty.FALSE, ExpectTargetsEmpty.TRUE, SystemOfRecord.SQUID, CreateSources.FALSE,
            PlasticToValidate.SOURCE, PipelineTransformation.NONE, ForwardMessage.NONE, VolumeConcUpdate.MERCURY_ONLY),
    MALARIA_BEP_PRIMER_TRANSFER("MalariaBEPPrimerTransfer",
            ExpectSourcesEmpty.FALSE, ExpectTargetsEmpty.TRUE, SystemOfRecord.SQUID, CreateSources.FALSE,
            PlasticToValidate.TARGET, PipelineTransformation.LIGATION, ForwardMessage.NONE, VolumeConcUpdate.MERCURY_ONLY),
    MALARIA_BEP_PCR_NORM("Malaria_BEP_PCR_Norm",
            ExpectSourcesEmpty.FALSE, ExpectTargetsEmpty.TRUE, SystemOfRecord.SQUID, CreateSources.FALSE,
            PlasticToValidate.SOURCE, PipelineTransformation.NONE, ForwardMessage.NONE, VolumeConcUpdate.MERCURY_ONLY),
    MALARIA_BEP_POOL("Malaria_BEP_Pool",
            ExpectSourcesEmpty.FALSE, ExpectTargetsEmpty.TRUE, SystemOfRecord.SQUID, CreateSources.FALSE,
            PlasticToValidate.SOURCE, PipelineTransformation.NONE, ForwardMessage.NONE, VolumeConcUpdate.MERCURY_ONLY),

    // Illumina Content Exome (ICE)
    ICE_BUCKET("IceBucket",
            ExpectSourcesEmpty.TRUE, ExpectTargetsEmpty.TRUE, SystemOfRecord.WORKFLOW_DEPENDENT, CreateSources.FALSE,
            PlasticToValidate.SOURCE, PipelineTransformation.NONE, ForwardMessage.NONE, VolumeConcUpdate.MERCURY_ONLY),
    ICE_POOLING_TRANSFER("IcePoolingTransfer",
            ExpectSourcesEmpty.FALSE, ExpectTargetsEmpty.TRUE, SystemOfRecord.WORKFLOW_DEPENDENT, CreateSources.FALSE,
            PlasticToValidate.SOURCE, PipelineTransformation.NONE, ForwardMessage.NONE, VolumeConcUpdate.MERCURY_ONLY),
    ICE_POOL_TEST("IcePoolTest",
            ExpectSourcesEmpty.FALSE, ExpectTargetsEmpty.TRUE, SystemOfRecord.WORKFLOW_DEPENDENT, CreateSources.FALSE,
            PlasticToValidate.SOURCE, PipelineTransformation.NONE, ForwardMessage.NONE, VolumeConcUpdate.MERCURY_ONLY),
    ICE_SPRI_CONCENTRATION("IceSPRIConcentration",
            ExpectSourcesEmpty.FALSE, ExpectTargetsEmpty.TRUE, SystemOfRecord.WORKFLOW_DEPENDENT, CreateSources.FALSE,
            PlasticToValidate.SOURCE, PipelineTransformation.NONE, ForwardMessage.NONE, VolumeConcUpdate.MERCURY_ONLY),
    ICE_96_PLEX_SPRI_CONCENTRATION("Ice96PlexSpriConcentration",
            ExpectSourcesEmpty.FALSE, ExpectTargetsEmpty.TRUE, SystemOfRecord.WORKFLOW_DEPENDENT, CreateSources.FALSE,
            PlasticToValidate.SOURCE, PipelineTransformation.NONE, ForwardMessage.NONE, VolumeConcUpdate.MERCURY_ONLY),
    ICE_1ST_HYBRIDIZATION("Ice1stHybridization",
            ExpectSourcesEmpty.FALSE, ExpectTargetsEmpty.TRUE, SystemOfRecord.WORKFLOW_DEPENDENT, CreateSources.FALSE,
            PlasticToValidate.SOURCE, PipelineTransformation.NONE, ForwardMessage.NONE, VolumeConcUpdate.MERCURY_ONLY),
    POST_ICE_1ST_HYBRIDIZATION_THERMO_CYCLER_LOADED("PostIce1stHybridizationThermoCyclerLoaded",
            ExpectSourcesEmpty.FALSE, ExpectTargetsEmpty.TRUE, SystemOfRecord.WORKFLOW_DEPENDENT, CreateSources.FALSE,
            PlasticToValidate.SOURCE, PipelineTransformation.NONE, ForwardMessage.NONE, VolumeConcUpdate.MERCURY_ONLY),
    ICE_1S_TBAIT_ADDITION("Ice1stBaitAddition",
            ExpectSourcesEmpty.FALSE, ExpectTargetsEmpty.TRUE, SystemOfRecord.WORKFLOW_DEPENDENT, CreateSources.FALSE,
            PlasticToValidate.TARGET, PipelineTransformation.NONE, ForwardMessage.NONE, VolumeConcUpdate.MERCURY_ONLY),
    ICE_1S_TBAIT_PICK("Ice1stBaitPick",
            ExpectSourcesEmpty.FALSE, ExpectTargetsEmpty.TRUE, SystemOfRecord.WORKFLOW_DEPENDENT, CreateSources.FALSE,
            PlasticToValidate.TARGET, PipelineTransformation.NONE, ForwardMessage.NONE, VolumeConcUpdate.MERCURY_ONLY),
    ICE_1ST_CAPTURE("Ice1stCapture",
            ExpectSourcesEmpty.FALSE, ExpectTargetsEmpty.TRUE, SystemOfRecord.WORKFLOW_DEPENDENT, CreateSources.FALSE,
            PlasticToValidate.SOURCE, PipelineTransformation.NONE, ForwardMessage.NONE, VolumeConcUpdate.MERCURY_ONLY),
    POST_ICE_1ST_CAPTURE_THERMO_CYCLER_LOADED("PostIce1stCaptureThermoCyclerLoaded",
            ExpectSourcesEmpty.FALSE, ExpectTargetsEmpty.TRUE, SystemOfRecord.WORKFLOW_DEPENDENT, CreateSources.FALSE,
            PlasticToValidate.SOURCE, PipelineTransformation.NONE, ForwardMessage.NONE, VolumeConcUpdate.MERCURY_ONLY),
    ICE_2ND_HYBRIDIZATION("Ice2ndHybridization",
            ExpectSourcesEmpty.FALSE, ExpectTargetsEmpty.TRUE, SystemOfRecord.WORKFLOW_DEPENDENT, CreateSources.FALSE,
            PlasticToValidate.SOURCE, PipelineTransformation.NONE, ForwardMessage.NONE, VolumeConcUpdate.MERCURY_ONLY),
    POST_ICE_2ND_HYBRIDIZATION_THERMO_CYCLER_LOADED("PostIce2ndHybridizationThermoCyclerLoaded",
            ExpectSourcesEmpty.FALSE, ExpectTargetsEmpty.TRUE, SystemOfRecord.WORKFLOW_DEPENDENT, CreateSources.FALSE,
            PlasticToValidate.SOURCE, PipelineTransformation.NONE, ForwardMessage.NONE, VolumeConcUpdate.MERCURY_ONLY),
    ICE_2ND_BAIT_ADDITION("Ice2ndBaitAddition",
            ExpectSourcesEmpty.FALSE, ExpectTargetsEmpty.TRUE, SystemOfRecord.WORKFLOW_DEPENDENT, CreateSources.FALSE,
            PlasticToValidate.TARGET, PipelineTransformation.NONE, ForwardMessage.NONE, VolumeConcUpdate.MERCURY_ONLY),
    ICE_2ND_BAIT_PICK("Ice2ndBaitPick",
            ExpectSourcesEmpty.FALSE, ExpectTargetsEmpty.TRUE, SystemOfRecord.WORKFLOW_DEPENDENT, CreateSources.FALSE,
            PlasticToValidate.TARGET, PipelineTransformation.NONE, ForwardMessage.NONE, VolumeConcUpdate.MERCURY_ONLY),
    ICE_2ND_CAPTURE("Ice2ndCapture",
            ExpectSourcesEmpty.FALSE, ExpectTargetsEmpty.TRUE, SystemOfRecord.WORKFLOW_DEPENDENT, CreateSources.FALSE,
            PlasticToValidate.SOURCE, PipelineTransformation.NONE, ForwardMessage.NONE, VolumeConcUpdate.MERCURY_ONLY),
    POST_ICE_2ND_CAPTURE_THERMO_CYCLER_LOADED("PostIce2ndCaptureThermoCyclerLoaded",
            ExpectSourcesEmpty.FALSE, ExpectTargetsEmpty.TRUE, SystemOfRecord.WORKFLOW_DEPENDENT, CreateSources.FALSE,
            PlasticToValidate.SOURCE, PipelineTransformation.NONE, ForwardMessage.NONE, VolumeConcUpdate.MERCURY_ONLY),
    ICE_CATCH_CLEANUP("IceCatchCleanup",
            ExpectSourcesEmpty.FALSE, ExpectTargetsEmpty.TRUE, SystemOfRecord.WORKFLOW_DEPENDENT, CreateSources.FALSE,
            PlasticToValidate.SOURCE, PipelineTransformation.NONE, ForwardMessage.NONE, VolumeConcUpdate.MERCURY_ONLY),
    ICE_CATCH_ENRICHMENT_SETUP("IceCatchEnrichmentSetup",
            ExpectSourcesEmpty.FALSE, ExpectTargetsEmpty.TRUE, SystemOfRecord.WORKFLOW_DEPENDENT, CreateSources.FALSE,
            PlasticToValidate.SOURCE, PipelineTransformation.NONE, ForwardMessage.NONE, VolumeConcUpdate.MERCURY_ONLY),
    POST_ICE_CATCH_ENRICHMENT_SETUP_THERMO_CYCLER_LOADED("PostIceCatchEnrichmentSetupThermoCyclerLoaded",
            ExpectSourcesEmpty.FALSE, ExpectTargetsEmpty.TRUE, SystemOfRecord.WORKFLOW_DEPENDENT, CreateSources.FALSE,
            PlasticToValidate.SOURCE, PipelineTransformation.NONE, ForwardMessage.NONE, VolumeConcUpdate.MERCURY_ONLY),
    ICE_CATCH_ENRICHMENT_CLEANUP("IceCatchEnrichmentCleanup",
            ExpectSourcesEmpty.FALSE, ExpectTargetsEmpty.TRUE, SystemOfRecord.WORKFLOW_DEPENDENT, CreateSources.FALSE,
            PlasticToValidate.SOURCE, PipelineTransformation.PCR, ForwardMessage.NONE, VolumeConcUpdate.MERCURY_ONLY),
    ICE_CATCH_PICO("IceCatchPico",
            ExpectSourcesEmpty.FALSE, ExpectTargetsEmpty.TRUE, SystemOfRecord.WORKFLOW_DEPENDENT, CreateSources.FALSE,
            PlasticToValidate.SOURCE, PipelineTransformation.NONE, ForwardMessage.NONE, VolumeConcUpdate.MERCURY_ONLY),

    // Nexome
    PRE_TAGMENTATION_TRANSFER("PreTagmentationTransfer",
            ExpectSourcesEmpty.FALSE, ExpectTargetsEmpty.TRUE, SystemOfRecord.SQUID, CreateSources.FALSE,
            PlasticToValidate.SOURCE, PipelineTransformation.NONE, ForwardMessage.NONE, VolumeConcUpdate.MERCURY_ONLY),
    TAGMENTATION("Tagmentation",
            ExpectSourcesEmpty.FALSE, ExpectTargetsEmpty.TRUE, SystemOfRecord.SQUID, CreateSources.FALSE,
            PlasticToValidate.SOURCE, PipelineTransformation.NONE, ForwardMessage.NONE, VolumeConcUpdate.MERCURY_ONLY),
    STOP_TAGMENTATION("StopTagmentation",
            ExpectSourcesEmpty.FALSE, ExpectTargetsEmpty.TRUE, SystemOfRecord.SQUID, CreateSources.FALSE,
            PlasticToValidate.SOURCE, PipelineTransformation.NONE, ForwardMessage.NONE, VolumeConcUpdate.MERCURY_ONLY),
    TAGMENTATION_CLEANUP("TagmentationCleanup",
            ExpectSourcesEmpty.FALSE, ExpectTargetsEmpty.TRUE, SystemOfRecord.SQUID, CreateSources.FALSE,
            PlasticToValidate.SOURCE, PipelineTransformation.NONE, ForwardMessage.NONE, VolumeConcUpdate.MERCURY_ONLY),
    NEXTERA_DUAL_INDEX_PCR("NexteraDualIndexPCR",
            ExpectSourcesEmpty.FALSE, ExpectTargetsEmpty.TRUE, SystemOfRecord.SQUID, CreateSources.FALSE,
            PlasticToValidate.TARGET, PipelineTransformation.PCR, ForwardMessage.NONE, VolumeConcUpdate.MERCURY_ONLY),
    NEXTERA_PCR_CLEANUP("NexteraPCRCleanup",
            ExpectSourcesEmpty.FALSE, ExpectTargetsEmpty.TRUE, SystemOfRecord.SQUID, CreateSources.FALSE,
            PlasticToValidate.SOURCE, PipelineTransformation.NONE, ForwardMessage.NONE, VolumeConcUpdate.MERCURY_ONLY),
    NEXOME_POND_PICO("NexomePondPico",
            ExpectSourcesEmpty.FALSE, ExpectTargetsEmpty.TRUE, SystemOfRecord.SQUID, CreateSources.FALSE,
            PlasticToValidate.SOURCE, PipelineTransformation.NONE, ForwardMessage.NONE, VolumeConcUpdate.MERCURY_ONLY),
    NEXOME_POOLING_TRANSFER("NexomePoolingTransfer",
            ExpectSourcesEmpty.FALSE, ExpectTargetsEmpty.TRUE, SystemOfRecord.SQUID, CreateSources.FALSE,
            PlasticToValidate.SOURCE, PipelineTransformation.NONE, ForwardMessage.NONE, VolumeConcUpdate.MERCURY_ONLY),
    NEXOME_POOL_TEST("NexomePoolTest",
            ExpectSourcesEmpty.FALSE, ExpectTargetsEmpty.TRUE, SystemOfRecord.SQUID, CreateSources.FALSE,
            PlasticToValidate.SOURCE, PipelineTransformation.NONE, ForwardMessage.NONE, VolumeConcUpdate.MERCURY_ONLY),
    NEXOME_SPRI_CONCENTRATION("NexomeSPRIConcentration",
            ExpectSourcesEmpty.FALSE, ExpectTargetsEmpty.TRUE, SystemOfRecord.SQUID, CreateSources.FALSE,
            PlasticToValidate.SOURCE, PipelineTransformation.NONE, ForwardMessage.NONE, VolumeConcUpdate.MERCURY_ONLY),
    NEXOME_1ST_HYBRIDIZATION("Nexome1stHybridization",
            ExpectSourcesEmpty.FALSE, ExpectTargetsEmpty.TRUE, SystemOfRecord.SQUID, CreateSources.FALSE,
            PlasticToValidate.SOURCE, PipelineTransformation.NONE, ForwardMessage.NONE, VolumeConcUpdate.MERCURY_ONLY),
    NEXOME_1ST_BAIT_ADDITION("Nexome1stBaitAddition",
            ExpectSourcesEmpty.FALSE, ExpectTargetsEmpty.TRUE, SystemOfRecord.SQUID, CreateSources.FALSE,
            PlasticToValidate.TARGET, PipelineTransformation.NONE, ForwardMessage.NONE, VolumeConcUpdate.MERCURY_ONLY),
    NEXOME_1ST_CAPTURE("Nexome1stCapture",
            ExpectSourcesEmpty.FALSE, ExpectTargetsEmpty.TRUE, SystemOfRecord.SQUID, CreateSources.FALSE,
            PlasticToValidate.SOURCE, PipelineTransformation.NONE, ForwardMessage.NONE, VolumeConcUpdate.MERCURY_ONLY),
    NEXOME_2ND_BAIT_ADDITION("Nexome2ndBaitAddition",
            ExpectSourcesEmpty.FALSE, ExpectTargetsEmpty.TRUE, SystemOfRecord.SQUID, CreateSources.FALSE,
            PlasticToValidate.SOURCE, PipelineTransformation.NONE, ForwardMessage.NONE, VolumeConcUpdate.MERCURY_ONLY),
    NEXOME_2ND_HYBRIDIZATION("Nexome2ndHybridization",
            ExpectSourcesEmpty.FALSE, ExpectTargetsEmpty.TRUE, SystemOfRecord.SQUID, CreateSources.FALSE,
            PlasticToValidate.SOURCE, PipelineTransformation.NONE, ForwardMessage.NONE, VolumeConcUpdate.MERCURY_ONLY),
    NEXOME_2ND_CAPTURE("Nexome2ndCapture",
            ExpectSourcesEmpty.FALSE, ExpectTargetsEmpty.TRUE, SystemOfRecord.SQUID, CreateSources.FALSE,
            PlasticToValidate.SOURCE, PipelineTransformation.NONE, ForwardMessage.NONE, VolumeConcUpdate.MERCURY_ONLY),
    NEXOME_CATCH_CLEANUP("NexomeCatchCleanup",
            ExpectSourcesEmpty.FALSE, ExpectTargetsEmpty.TRUE, SystemOfRecord.SQUID, CreateSources.FALSE,
            PlasticToValidate.SOURCE, PipelineTransformation.CAPTURE, ForwardMessage.NONE, VolumeConcUpdate.MERCURY_ONLY),
    NEXOME_CATCH_ENRICHMENT("NexomeCatchEnrichment",
            ExpectSourcesEmpty.FALSE, ExpectTargetsEmpty.TRUE, SystemOfRecord.SQUID, CreateSources.FALSE,
            PlasticToValidate.SOURCE, PipelineTransformation.NONE, ForwardMessage.NONE, VolumeConcUpdate.MERCURY_ONLY),
    NEXOME_CATCH_ENRICHMENT_CLEANUP("NexomeCatchEnrichmentCleanup",
            ExpectSourcesEmpty.FALSE, ExpectTargetsEmpty.TRUE, SystemOfRecord.SQUID, CreateSources.FALSE,
            PlasticToValidate.SOURCE, PipelineTransformation.PCR, ForwardMessage.NONE, VolumeConcUpdate.MERCURY_ONLY),
    NEXOME_CATCH_PICO("NexomeCatchPico",
            ExpectSourcesEmpty.FALSE, ExpectTargetsEmpty.TRUE, SystemOfRecord.SQUID, CreateSources.FALSE,
            PlasticToValidate.SOURCE, PipelineTransformation.NONE, ForwardMessage.NONE, VolumeConcUpdate.MERCURY_ONLY),

    // Nexome V2
    TWO_PLATE_TAGMENTATION_CLEANUP("2PlateTagmentationCleanup",
            ExpectSourcesEmpty.FALSE, ExpectTargetsEmpty.TRUE, SystemOfRecord.SQUID, CreateSources.FALSE,
            PlasticToValidate.SOURCE, PipelineTransformation.NONE, ForwardMessage.NONE, VolumeConcUpdate.MERCURY_ONLY),
    NEXTERA_PCR_TRANSFER("NexteraPCRTransfer",
            ExpectSourcesEmpty.FALSE, ExpectTargetsEmpty.TRUE, SystemOfRecord.SQUID, CreateSources.FALSE,
            PlasticToValidate.SOURCE, PipelineTransformation.NONE, ForwardMessage.NONE, VolumeConcUpdate.MERCURY_ONLY),
    TWO_PLATE_NEXTERA_PCR_CLEANUP("2PlateNexteraPCRCleanup",
            ExpectSourcesEmpty.FALSE, ExpectTargetsEmpty.TRUE, SystemOfRecord.SQUID, CreateSources.FALSE,
            PlasticToValidate.SOURCE, PipelineTransformation.NONE, ForwardMessage.NONE, VolumeConcUpdate.MERCURY_ONLY),
    THREE_84_WELL_POND_PICO("384WellPondPico",
            ExpectSourcesEmpty.FALSE, ExpectTargetsEmpty.TRUE, SystemOfRecord.SQUID, CreateSources.FALSE,
            PlasticToValidate.SOURCE, PipelineTransformation.NONE, ForwardMessage.NONE, VolumeConcUpdate.MERCURY_ONLY),
    NEXOME_NORM_TRANSFER("NexomeNormTransfer",
            ExpectSourcesEmpty.FALSE, ExpectTargetsEmpty.TRUE, SystemOfRecord.SQUID, CreateSources.FALSE,
            PlasticToValidate.SOURCE, PipelineTransformation.NONE, ForwardMessage.NONE, VolumeConcUpdate.MERCURY_ONLY),
    NEXOME_BAIT_ADDITION("NexomeBaitAddition",
            ExpectSourcesEmpty.FALSE, ExpectTargetsEmpty.TRUE, SystemOfRecord.SQUID, CreateSources.FALSE,
            PlasticToValidate.SOURCE, PipelineTransformation.NONE, ForwardMessage.NONE, VolumeConcUpdate.MERCURY_ONLY),
    TWO_PLATE_NEXOME_2ND_CAPTURE("2PlateNexome2ndCapture",
            ExpectSourcesEmpty.FALSE, ExpectTargetsEmpty.TRUE, SystemOfRecord.SQUID, CreateSources.FALSE,
            PlasticToValidate.SOURCE, PipelineTransformation.NONE, ForwardMessage.NONE, VolumeConcUpdate.MERCURY_ONLY),
    TWO_PLATE_NEXOME_CATCH_CLEANUP("2PlateNexomeCatchCleanup",
            ExpectSourcesEmpty.FALSE, ExpectTargetsEmpty.TRUE, SystemOfRecord.SQUID, CreateSources.FALSE,
            PlasticToValidate.SOURCE, PipelineTransformation.NONE, ForwardMessage.NONE, VolumeConcUpdate.MERCURY_ONLY),
    NEXOME_PCR_TRANSFER("NexomePCRTransfer",
            ExpectSourcesEmpty.FALSE, ExpectTargetsEmpty.TRUE, SystemOfRecord.SQUID, CreateSources.FALSE,
            PlasticToValidate.SOURCE, PipelineTransformation.NONE, ForwardMessage.NONE, VolumeConcUpdate.MERCURY_ONLY),
    TWO_PLATE_NEXOME_CATCH_ENRICHMENT("2PlateNexomeCatchEnrichment",
            ExpectSourcesEmpty.FALSE, ExpectTargetsEmpty.TRUE, SystemOfRecord.SQUID, CreateSources.FALSE,
            PlasticToValidate.SOURCE, PipelineTransformation.PCR, ForwardMessage.NONE, VolumeConcUpdate.MERCURY_ONLY),

    // PCRFree
    PCR_FREE_POND_REGISTRATION("PCRFreePondRegistration",
            ExpectSourcesEmpty.FALSE, ExpectTargetsEmpty.TRUE, SystemOfRecord.SQUID, CreateSources.FALSE,
            PlasticToValidate.SOURCE, PipelineTransformation.NONE, ForwardMessage.NONE, VolumeConcUpdate.MERCURY_ONLY),
    PCR_FREE_08X_CLEANUP("PCRFree0.8xCleanup",
            ExpectSourcesEmpty.FALSE, ExpectTargetsEmpty.TRUE, SystemOfRecord.SQUID, CreateSources.FALSE,
            PlasticToValidate.SOURCE, PipelineTransformation.NONE, ForwardMessage.NONE, VolumeConcUpdate.MERCURY_ONLY),
    PCR_FREE_3X_CLEANUP("PCRFree3.0xCleanup",
            ExpectSourcesEmpty.FALSE, ExpectTargetsEmpty.TRUE, SystemOfRecord.SQUID, CreateSources.FALSE,
            PlasticToValidate.SOURCE, PipelineTransformation.NONE, ForwardMessage.NONE, VolumeConcUpdate.MERCURY_ONLY),

    PCR_PLUS_POND_REGISTRATION("PCRPlusPondRegistration",
            ExpectSourcesEmpty.FALSE, ExpectTargetsEmpty.TRUE, SystemOfRecord.SQUID, CreateSources.FALSE,
            PlasticToValidate.SOURCE, PipelineTransformation.NONE, ForwardMessage.NONE, VolumeConcUpdate.MERCURY_ONLY),

    /**
     * TODO SGM  the following names are place holders.  They will be re-evaluated as development of
     */
    SHEARING_BUCKET("ShearingBucket",
            ExpectSourcesEmpty.TRUE, ExpectTargetsEmpty.TRUE, SystemOfRecord.MERCURY, CreateSources.FALSE,
            PlasticToValidate.SOURCE, PipelineTransformation.NONE, ForwardMessage.NONE, VolumeConcUpdate.MERCURY_ONLY),
//    SHEARING_BUCKET_ENTRY ("ShearingBucketEntry", ExpectSourcesEmpty.TRUE, ExpectTargetsEmpty.TRUE, SystemOfRecord.MERCURY),
//    SHEARING_BUCKET_EXIT ("ShearingBucketExit", ExpectSourcesEmpty.TRUE, ExpectTargetsEmpty.TRUE, SystemOfRecord.MERCURY),
    COLLABORATOR_TRANSFER("CollaboratorTransfer", ExpectSourcesEmpty.FALSE, ExpectTargetsEmpty.TRUE,
            SystemOfRecord.MERCURY, CreateSources.FALSE, PlasticToValidate.TARGET, PipelineTransformation.NONE,
            ForwardMessage.NONE, VolumeConcUpdate.MERCURY_ONLY),

    // Activity - sent by decks for otherwise non-messaged protocols (technology development); used by Analytics to
    // track usage.
    ACTIVITY_BEGIN("ActivityBegin",
            ExpectSourcesEmpty.FALSE, ExpectTargetsEmpty.TRUE, SystemOfRecord.MERCURY, CreateSources.FALSE,
            PlasticToValidate.SOURCE, PipelineTransformation.NONE, ForwardMessage.NONE, VolumeConcUpdate.MERCURY_ONLY),
    ACTIVITY_END("ActivityEnd",
            ExpectSourcesEmpty.FALSE, ExpectTargetsEmpty.TRUE, SystemOfRecord.MERCURY, CreateSources.FALSE,
            PlasticToValidate.SOURCE, PipelineTransformation.NONE, ForwardMessage.NONE, VolumeConcUpdate.MERCURY_ONLY),

//    EXTRACTION_BUCKET("ExtractionBucket",ExpectSourcesEmpty.TRUE, ExpectTargetsEmpty.TRUE, SystemOfRecord.MERCURY, CreateSources.FALSE,
//                PlasticToValidate.SOURCE, PipelineTransformation.NONE, ForwardMessage.NONE, VolumeConcUpdate.MERCURY_ONLY),
//    WHOLE_BLOOD_EXTRACTION("WholeBloodExtraction",ExpectSourcesEmpty.TRUE, ExpectTargetsEmpty.TRUE, SystemOfRecord.MERCURY, CreateSources.FALSE,
//                PlasticToValidate.SOURCE, PipelineTransformation.NONE, ForwardMessage.NONE, VolumeConcUpdate.MERCURY_ONLY),
//    TISSUE_EXTRACTION("TissueExtraction",ExpectSourcesEmpty.TRUE, ExpectTargetsEmpty.TRUE, SystemOfRecord.MERCURY, CreateSources.FALSE,
//                PlasticToValidate.SOURCE, PipelineTransformation.NONE, ForwardMessage.NONE, VolumeConcUpdate.MERCURY_ONLY),
    ALLPREP_EXTRACTION_BUCKET("AllPrepExtractionBucket", ExpectSourcesEmpty.TRUE,
            ExpectTargetsEmpty.TRUE, SystemOfRecord.MERCURY, CreateSources.FALSE, PlasticToValidate.SOURCE,
            PipelineTransformation.NONE, ForwardMessage.NONE, VolumeConcUpdate.MERCURY_ONLY),

    // Transfer blood to micro centrifuge tube
    EXTRACT_BLOOD_TO_MICRO("ExtractBloodToMicro",
            ExpectSourcesEmpty.TRUE, ExpectTargetsEmpty.TRUE, SystemOfRecord.MERCURY, CreateSources.FALSE,
            PlasticToValidate.SOURCE, PipelineTransformation.NONE, ForwardMessage.NONE, VolumeConcUpdate.MERCURY_ONLY,
            MessageType.RECEPTACLE_TRANSFER_EVENT, BarcodedTube.BarcodedTubeType.VacutainerBloodTube3,
            BarcodedTube.BarcodedTubeType.EppendoffFliptop15, new String[]{"Proteinase K", "Buffer AL", "100% Ethanol"}),
    // Transfer blood to spin column
    EXTRACT_BLOOD_MICRO_TO_SPIN("ExtractBloodMicroToSpin",
            ExpectSourcesEmpty.TRUE, ExpectTargetsEmpty.TRUE, SystemOfRecord.MERCURY, CreateSources.FALSE,
            PlasticToValidate.SOURCE, PipelineTransformation.NONE, ForwardMessage.NONE, VolumeConcUpdate.MERCURY_ONLY,
            MessageType.RECEPTACLE_TRANSFER_EVENT, BarcodedTube.BarcodedTubeType.EppendoffFliptop15,
            BarcodedTube.BarcodedTubeType.SpinColumn, new String[]{"Buffer AW1", "Buffer AW2", "Buffer AE"}),
    // Transfer blood to matrix tube
    EXTRACT_BLOOD_SPIN_TO_MATRIX("ExtractBloodSpinToMatrix",
            ExpectSourcesEmpty.TRUE, ExpectTargetsEmpty.TRUE, SystemOfRecord.MERCURY, CreateSources.FALSE,
            PlasticToValidate.SOURCE, PipelineTransformation.NONE, ForwardMessage.NONE, VolumeConcUpdate.MERCURY_ONLY,
            MessageType.RECEPTACLE_TRANSFER_EVENT, BarcodedTube.BarcodedTubeType.SpinColumn,
            BarcodedTube.BarcodedTubeType.MatrixTube075, new String[]{}, LabVessel.MaterialType.DNA),

    // Transfer cell suspension to microcentrifuge tube
    EXTRACT_CELL_SUSP_TO_MICRO("ExtractCellSuspToMicro",
            ExpectSourcesEmpty.TRUE, ExpectTargetsEmpty.TRUE, SystemOfRecord.MERCURY, CreateSources.FALSE,
            PlasticToValidate.SOURCE, PipelineTransformation.NONE, ForwardMessage.NONE, VolumeConcUpdate.MERCURY_ONLY,
            MessageType.RECEPTACLE_TRANSFER_EVENT, BarcodedTube.BarcodedTubeType.Cryovial05,
            BarcodedTube.BarcodedTubeType.EppendoffFliptop15, new String[]{"Proteinase K", "Buffer AL", "100% Ethanol"}),
    // Transfer cell suspension to spin column
    EXTRACT_CELL_SUSP_MICRO_TO_SPIN("ExtractCellSuspMicroToSpin",
            ExpectSourcesEmpty.TRUE, ExpectTargetsEmpty.TRUE, SystemOfRecord.MERCURY, CreateSources.FALSE,
            PlasticToValidate.SOURCE, PipelineTransformation.NONE, ForwardMessage.NONE, VolumeConcUpdate.MERCURY_ONLY,
            MessageType.RECEPTACLE_TRANSFER_EVENT, BarcodedTube.BarcodedTubeType.EppendoffFliptop15,
            BarcodedTube.BarcodedTubeType.SpinColumn, new String[]{"Buffer AW1", "Buffer AW2", "Buffer AE"}),
    // Optional Transfer cell suspension to micro centrifuge tube
    EXTRACT_CELL_SUSP_SPIN_TO_MICRO("ExtractCellSuspSpinToMicro",
            ExpectSourcesEmpty.TRUE, ExpectTargetsEmpty.TRUE, SystemOfRecord.MERCURY, CreateSources.FALSE,
            PlasticToValidate.SOURCE, PipelineTransformation.NONE, ForwardMessage.NONE, VolumeConcUpdate.MERCURY_ONLY,
            MessageType.RECEPTACLE_TRANSFER_EVENT, BarcodedTube.BarcodedTubeType.SpinColumn,
            BarcodedTube.BarcodedTubeType.EppendoffFliptop15, new String[]{"SPRI", "70% Ethanol", "TE"}),
    // Transfer cell suspension to matrix tube
    EXTRACT_CELL_SUSP_TO_MATRIX("ExtractCellSuspToMatrix",
            ExpectSourcesEmpty.TRUE, ExpectTargetsEmpty.TRUE, SystemOfRecord.MERCURY, CreateSources.FALSE,
            PlasticToValidate.SOURCE, PipelineTransformation.NONE, ForwardMessage.NONE, VolumeConcUpdate.MERCURY_ONLY,
            MessageType.RECEPTACLE_TRANSFER_EVENT, BarcodedTube.BarcodedTubeType.SpinColumn,
            BarcodedTube.BarcodedTubeType.MatrixTube075, new String[]{}, LabVessel.MaterialType.DNA),

    // Transfer tissue in paraffin to micro centrifuge tube
    EXTRACT_FFPE_TO_MICRO1("ExtractFfpeToMicro1",
            ExpectSourcesEmpty.TRUE, ExpectTargetsEmpty.TRUE, SystemOfRecord.MERCURY, CreateSources.FALSE,
            PlasticToValidate.SOURCE, PipelineTransformation.NONE, ForwardMessage.NONE, VolumeConcUpdate.MERCURY_ONLY,
            MessageType.RECEPTACLE_TRANSFER_EVENT, BarcodedTube.BarcodedTubeType.Slide,
            BarcodedTube.BarcodedTubeType.EppendoffFliptop15, new String[]{"Deparaffinization Solution", "Buffer ATL", "Proteinase K"}),
    // Transfer tissue in paraffin to micro centrifuge tube to micro centrifuge tube
    EXTRACT_FFPE_MICRO1_TO_MICRO2("ExtractFfpeMicro1ToMicro2",
            ExpectSourcesEmpty.TRUE, ExpectTargetsEmpty.TRUE, SystemOfRecord.MERCURY, CreateSources.FALSE,
            PlasticToValidate.SOURCE, PipelineTransformation.NONE, ForwardMessage.NONE, VolumeConcUpdate.MERCURY_ONLY,
            MessageType.RECEPTACLE_TRANSFER_EVENT, BarcodedTube.BarcodedTubeType.EppendoffFliptop15,
            BarcodedTube.BarcodedTubeType.EppendoffFliptop15, new String[]{"RNase A", "Buffer AL", "100% Ethanol"}),
    // Transfer tissue in paraffin to spin column
    EXTRACT_FFPE_MICRO2_TO_SPIN("ExtractFfpeMicro2ToSpin",
            ExpectSourcesEmpty.TRUE, ExpectTargetsEmpty.TRUE, SystemOfRecord.MERCURY, CreateSources.FALSE,
            PlasticToValidate.SOURCE, PipelineTransformation.NONE, ForwardMessage.NONE, VolumeConcUpdate.MERCURY_ONLY,
            MessageType.RECEPTACLE_TRANSFER_EVENT, BarcodedTube.BarcodedTubeType.EppendoffFliptop15,
            BarcodedTube.BarcodedTubeType.SpinColumn, new String[]{"Buffer AW1", "Buffer AW2", "Buffer ATE"}),
    // Transfer tissue in paraffin to matrix tube
    EXTRACT_FFPE_SPIN_TO_MATRIX("ExtractFfpeSpinToMatrix",
            ExpectSourcesEmpty.TRUE, ExpectTargetsEmpty.TRUE, SystemOfRecord.MERCURY, CreateSources.FALSE,
            PlasticToValidate.SOURCE, PipelineTransformation.NONE, ForwardMessage.NONE, VolumeConcUpdate.MERCURY_ONLY,
            MessageType.RECEPTACLE_TRANSFER_EVENT, BarcodedTube.BarcodedTubeType.SpinColumn,
            BarcodedTube.BarcodedTubeType.MatrixTube075, new String[]{}, LabVessel.MaterialType.DNA),

    // Transfer fresh frozen tissue to micro centrifuge tube
    EXTRACT_FRESH_TISSUE_TO_MICRO("ExtractFreshTissueToMicro",
            ExpectSourcesEmpty.TRUE, ExpectTargetsEmpty.TRUE, SystemOfRecord.MERCURY, CreateSources.FALSE,
            PlasticToValidate.SOURCE, PipelineTransformation.NONE, ForwardMessage.NONE, VolumeConcUpdate.MERCURY_ONLY,
            MessageType.RECEPTACLE_TRANSFER_EVENT, BarcodedTube.BarcodedTubeType.TissueCassette,
            BarcodedTube.BarcodedTubeType.EppendoffFliptop15, new String[]{"Buffer ATL", "Proteinase K", "RNase", "Buffer AL", "Ethanol"}),
    // Transfer fresh frozen tissue to spin column
    EXTRACT_FRESH_TISSUE_MICRO_TO_SPIN("ExtractFreshTissueMicroToSpin",
            ExpectSourcesEmpty.TRUE, ExpectTargetsEmpty.TRUE, SystemOfRecord.MERCURY, CreateSources.FALSE,
            PlasticToValidate.SOURCE, PipelineTransformation.NONE, ForwardMessage.NONE, VolumeConcUpdate.MERCURY_ONLY,
            MessageType.RECEPTACLE_TRANSFER_EVENT, BarcodedTube.BarcodedTubeType.EppendoffFliptop15,
            BarcodedTube.BarcodedTubeType.SpinColumn, new String[]{"Buffer AW1", "Buffer AW2", "Buffer AE"}),
    // Transfer fresh frozen tissue to matrix tube
    EXTRACT_FRESH_TISSUE_SPIN_TO_MATRIX("ExtractFreshTissueSpinToMatrix",
            ExpectSourcesEmpty.TRUE, ExpectTargetsEmpty.TRUE, SystemOfRecord.MERCURY, CreateSources.FALSE,
            PlasticToValidate.SOURCE, PipelineTransformation.NONE, ForwardMessage.NONE, VolumeConcUpdate.MERCURY_ONLY,
            MessageType.RECEPTACLE_TRANSFER_EVENT, BarcodedTube.BarcodedTubeType.SpinColumn,
            BarcodedTube.BarcodedTubeType.MatrixTube075, new String[]{}, LabVessel.MaterialType.DNA),

    // Transfer saliva to conical tube
    EXTRACT_SALIVA_TO_CONICAL("ExtractSalivaToConical",
            ExpectSourcesEmpty.TRUE, ExpectTargetsEmpty.TRUE, SystemOfRecord.MERCURY, CreateSources.FALSE,
            PlasticToValidate.SOURCE, PipelineTransformation.NONE, ForwardMessage.NONE, VolumeConcUpdate.MERCURY_ONLY,
            MessageType.RECEPTACLE_TRANSFER_EVENT, BarcodedTube.BarcodedTubeType.OrageneTube,
            BarcodedTube.BarcodedTubeType.Conical50, new String[]{"PBS", "Proteinase K", "Buffer AL", "100% Ethanol"}),
    // Transfer saliva to spin column
    EXTRACT_SALIVA_CONICAL_TO_SPIN("ExtractSalivaConicalToSpin",
            ExpectSourcesEmpty.TRUE, ExpectTargetsEmpty.TRUE, SystemOfRecord.MERCURY, CreateSources.FALSE,
            PlasticToValidate.SOURCE, PipelineTransformation.NONE, ForwardMessage.NONE, VolumeConcUpdate.MERCURY_ONLY,
            MessageType.RECEPTACLE_TRANSFER_EVENT, BarcodedTube.BarcodedTubeType.Conical50,
            BarcodedTube.BarcodedTubeType.SpinColumn, new String[]{"Buffer AW1", "Buffer AW2", "Buffer AE"}),
    // Transfer saliva to matrix tube
    EXTRACT_SALIVA_SPIN_TO_MATRIX("ExtractSalivaSpinToMatrix",
            ExpectSourcesEmpty.TRUE, ExpectTargetsEmpty.TRUE, SystemOfRecord.MERCURY, CreateSources.FALSE,
            PlasticToValidate.SOURCE, PipelineTransformation.NONE, ForwardMessage.NONE, VolumeConcUpdate.MERCURY_ONLY,
            MessageType.RECEPTACLE_TRANSFER_EVENT, BarcodedTube.BarcodedTubeType.SpinColumn,
            BarcodedTube.BarcodedTubeType.MatrixTube075, new String[]{}, LabVessel.MaterialType.DNA),

    //Infinium
    INFINIUM_AMPLIFICATION("InfiniumAmplification",
            ExpectSourcesEmpty.FALSE, ExpectTargetsEmpty.TRUE, SystemOfRecord.MERCURY, CreateSources.FALSE,
            PlasticToValidate.SOURCE, PipelineTransformation.NONE, ForwardMessage.GAP, VolumeConcUpdate.MERCURY_ONLY),
    INFINIUM_AMPLIFICATION_REAGENT_ADDITION("InfiniumAmplificationReagentAddition",
            ExpectSourcesEmpty.TRUE, ExpectTargetsEmpty.TRUE, SystemOfRecord.MERCURY, CreateSources.FALSE,
            PlasticToValidate.SOURCE, PipelineTransformation.NONE, ForwardMessage.GAP, VolumeConcUpdate.MERCURY_ONLY),
    INFINIUM_FRAGMENTATION("InfiniumFragmentation",
            ExpectSourcesEmpty.TRUE, ExpectTargetsEmpty.TRUE, SystemOfRecord.MERCURY, CreateSources.FALSE,
            PlasticToValidate.SOURCE, PipelineTransformation.NONE, ForwardMessage.GAP, VolumeConcUpdate.MERCURY_ONLY),
    INFINIUM_POST_FRAGMENTATION_HYB_OVEN_LOADED("InfiniumPostFragmentationHybOvenLoaded",
            ExpectSourcesEmpty.FALSE, ExpectTargetsEmpty.TRUE, SystemOfRecord.MERCURY, CreateSources.FALSE,
            PlasticToValidate.SOURCE, PipelineTransformation.NONE, ForwardMessage.GAP, VolumeConcUpdate.MERCURY_ONLY),
    INFINIUM_PRECIPITATION("InfiniumPrecipitation",
            ExpectSourcesEmpty.TRUE, ExpectTargetsEmpty.TRUE, SystemOfRecord.MERCURY, CreateSources.FALSE,
            PlasticToValidate.SOURCE, PipelineTransformation.NONE, ForwardMessage.GAP, VolumeConcUpdate.MERCURY_ONLY),
    INFINIUM_POST_PRECIPITATION_HEAT_BLOCK_LOADED("InfiniumPostPrecipitationHeatBlockLoaded",
            ExpectSourcesEmpty.FALSE, ExpectTargetsEmpty.TRUE, SystemOfRecord.MERCURY, CreateSources.FALSE,
            PlasticToValidate.SOURCE, PipelineTransformation.NONE, ForwardMessage.GAP, VolumeConcUpdate.MERCURY_ONLY),
    INFINIUM_PRECIPITATION_ISOPROPANOL_ADDITION("InfiniumPrecipitationIsopropanolAddition",
            ExpectSourcesEmpty.FALSE, ExpectTargetsEmpty.TRUE, SystemOfRecord.MERCURY, CreateSources.FALSE,
            PlasticToValidate.SOURCE, PipelineTransformation.NONE, ForwardMessage.GAP, VolumeConcUpdate.MERCURY_ONLY),
    INFINIUM_RESUSPENSION("InfiniumResuspension",
            ExpectSourcesEmpty.FALSE, ExpectTargetsEmpty.TRUE, SystemOfRecord.MERCURY, CreateSources.FALSE,
            PlasticToValidate.SOURCE, PipelineTransformation.NONE, ForwardMessage.GAP, VolumeConcUpdate.MERCURY_ONLY),
    INFINIUM_POST_RESUSPENSION_HYB_OVEN("InfiniumPostResuspensionHybOven",
            ExpectSourcesEmpty.FALSE, ExpectTargetsEmpty.TRUE, SystemOfRecord.MERCURY, CreateSources.FALSE,
            PlasticToValidate.SOURCE, PipelineTransformation.NONE, ForwardMessage.GAP, VolumeConcUpdate.MERCURY_ONLY),
    INFINIUM_HYBRIDIZATION("InfiniumHybridization",
            ExpectSourcesEmpty.FALSE, ExpectTargetsEmpty.TRUE, SystemOfRecord.MERCURY, CreateSources.FALSE,
            PlasticToValidate.SOURCE, PipelineTransformation.NONE, ForwardMessage.GAP, VolumeConcUpdate.MERCURY_ONLY),
    INFINIUM_HYB_CHAMBER_LOADED("InfiniumHybChamberLoaded",
            ExpectSourcesEmpty.FALSE, ExpectTargetsEmpty.TRUE, SystemOfRecord.MERCURY, CreateSources.FALSE,
            PlasticToValidate.SOURCE, PipelineTransformation.NONE, ForwardMessage.GAP, VolumeConcUpdate.MERCURY_ONLY),
    INFINIUM_WASH("InfiniumWash",
            ExpectSourcesEmpty.FALSE, ExpectTargetsEmpty.TRUE, SystemOfRecord.MERCURY, CreateSources.FALSE,
            PlasticToValidate.SOURCE, PipelineTransformation.NONE, ForwardMessage.GAP, VolumeConcUpdate.MERCURY_ONLY),
    INFINIUM_XSTAIN("InfiniumXStain",
            ExpectSourcesEmpty.FALSE, ExpectTargetsEmpty.TRUE, SystemOfRecord.MERCURY, CreateSources.FALSE,
            PlasticToValidate.SOURCE, PipelineTransformation.NONE, ForwardMessage.GAP, VolumeConcUpdate.MERCURY_ONLY),
    // Generic events that are qualified by workflow
    // todo jmt need different versions for PLATE_EVENT and RECEPTACLE_EVENT?
    CENTRIFUGE("Centrifuge",
            ExpectSourcesEmpty.TRUE, ExpectTargetsEmpty.TRUE, SystemOfRecord.MERCURY, CreateSources.FALSE,
            PlasticToValidate.SOURCE, PipelineTransformation.NONE, ForwardMessage.NONE, VolumeConcUpdate.MERCURY_ONLY),
    INCUBATE("Incubate",
            ExpectSourcesEmpty.TRUE, ExpectTargetsEmpty.TRUE, SystemOfRecord.MERCURY, CreateSources.FALSE,
            PlasticToValidate.SOURCE, PipelineTransformation.NONE, ForwardMessage.NONE, VolumeConcUpdate.MERCURY_ONLY),
    MIX("Mix",
            ExpectSourcesEmpty.TRUE, ExpectTargetsEmpty.TRUE, SystemOfRecord.MERCURY, CreateSources.FALSE,
            PlasticToValidate.SOURCE, PipelineTransformation.NONE, ForwardMessage.NONE, VolumeConcUpdate.MERCURY_ONLY),
    WASH("Wash",
            ExpectSourcesEmpty.TRUE, ExpectTargetsEmpty.TRUE, SystemOfRecord.MERCURY, CreateSources.FALSE,
            PlasticToValidate.SOURCE, PipelineTransformation.NONE, ForwardMessage.NONE, VolumeConcUpdate.MERCURY_ONLY),
    ADD_REAGENT("AddReagent",
            ExpectSourcesEmpty.TRUE, ExpectTargetsEmpty.TRUE, SystemOfRecord.MERCURY, CreateSources.FALSE,
            PlasticToValidate.SOURCE, PipelineTransformation.NONE, ForwardMessage.NONE, VolumeConcUpdate.MERCURY_ONLY,
            MessageType.RECEPTACLE_EVENT, null, null, null),
    PREP("Prep",
            ExpectSourcesEmpty.TRUE, ExpectTargetsEmpty.TRUE, SystemOfRecord.MERCURY, CreateSources.FALSE,
            PlasticToValidate.SOURCE, PipelineTransformation.NONE, ForwardMessage.NONE, VolumeConcUpdate.MERCURY_ONLY),
    DISCARD("Discard",
            ExpectSourcesEmpty.TRUE, ExpectTargetsEmpty.TRUE, SystemOfRecord.MERCURY, CreateSources.FALSE,
            PlasticToValidate.SOURCE, PipelineTransformation.NONE, ForwardMessage.NONE, VolumeConcUpdate.MERCURY_ONLY),
    MOVE("Move",
            ExpectSourcesEmpty.TRUE, ExpectTargetsEmpty.TRUE, SystemOfRecord.MERCURY, CreateSources.FALSE,
            PlasticToValidate.SOURCE, PipelineTransformation.NONE, ForwardMessage.NONE, VolumeConcUpdate.MERCURY_ONLY),
    STORE("Store",
            ExpectSourcesEmpty.TRUE, ExpectTargetsEmpty.TRUE, SystemOfRecord.MERCURY, CreateSources.FALSE,
            PlasticToValidate.SOURCE, PipelineTransformation.NONE, ForwardMessage.NONE, VolumeConcUpdate.MERCURY_ONLY),
    ;


    private final String name;

    private final ExpectSourcesEmpty expectedEmptySources;

    private final ExpectTargetsEmpty expectedEmptyTargets;

    private final CreateSources createSources;

    private final LabVessel.MaterialType resultingMaterialType;

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
        /** This is currently the most important transformation.  In general, the Zims library name should include
         * the barcode of the tube following the first post-shearing PCR event. */
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

    /**
     * Whether to send this event message to BSP.
     */
    public enum ForwardMessage {
        BSP,
        GAP,
        NONE;
    }

    private final PipelineTransformation pipelineTransformation;
    private final ForwardMessage forwardMessage;

    public enum VolumeConcUpdate {
        MERCURY_ONLY,
        BSP_AND_MERCURY
    }
    private final VolumeConcUpdate volumeConcUpdate;

    public enum MessageType {
        PLATE_EVENT,
        PLATE_TRANSFER_EVENT,
        STATION_SETUP_EVENT,
        PLATE_CHERRY_PICK_EVENT,
        RECEPTACLE_PLATE_TRANSFER_EVENT,
        RECEPTACLE_EVENT,
        RECEPTACLE_TRANSFER_EVENT
    }

    private MessageType messageType;

    private VesselTypeGeometry sourceVesselTypeGeometry;
    private VesselTypeGeometry targetVesselTypeGeometry;

    private String[] reagentNames;

    /**
     * One attempt at trying to make a very generic
     * {@link LabEvent} to handle lots of different
     * {@link LabEventName event names}
     * @param name                   the event name in the message
     * @param expectSourcesEmpty     whether it's an error for the source vessels to have content
     * @param expectTargetsEmpty     whether it's an error for the target vessels to have content
     * @param systemOfRecord         which system is responsible for handling the message
     * @param createSources          whether sources should be created, if they don't exist
     * @param plasticToValidate      determines whether sources and / or targets are validated
     * @param pipelineTransformation type of transformation, from the analysis pipeline's perspective
     * @param forwardMessage         where to forward event message: BSP or GAP
     * @param volumeConcUpdate       in which systems to update volume and concentration
     */
    LabEventType(String name, ExpectSourcesEmpty expectSourcesEmpty, ExpectTargetsEmpty expectTargetsEmpty,
            SystemOfRecord systemOfRecord, CreateSources createSources, PlasticToValidate plasticToValidate,
            PipelineTransformation pipelineTransformation, ForwardMessage forwardMessage,
            VolumeConcUpdate volumeConcUpdate) {
        this(name, expectSourcesEmpty, expectTargetsEmpty, systemOfRecord, createSources, plasticToValidate,
                pipelineTransformation, forwardMessage, volumeConcUpdate, null,null,null,null);
    }

    LabEventType(String name, ExpectSourcesEmpty expectSourcesEmpty, ExpectTargetsEmpty expectTargetsEmpty,
                 SystemOfRecord systemOfRecord, CreateSources createSources, PlasticToValidate plasticToValidate,
                 PipelineTransformation pipelineTransformation, ForwardMessage forwardMessage,
                 VolumeConcUpdate volumeConcUpdate, MessageType messageType,
                 VesselTypeGeometry sourceVesselTypeGeometry, VesselTypeGeometry targetVesselTypeGeometry,
                 String[] reagentNames) {
        this(name, expectSourcesEmpty, expectTargetsEmpty, systemOfRecord, createSources, plasticToValidate,
                pipelineTransformation, forwardMessage, volumeConcUpdate, messageType, sourceVesselTypeGeometry,
                targetVesselTypeGeometry, reagentNames, null);
    }

    LabEventType(String name, ExpectSourcesEmpty expectSourcesEmpty, ExpectTargetsEmpty expectTargetsEmpty,
                 SystemOfRecord systemOfRecord, CreateSources createSources, PlasticToValidate plasticToValidate,
                 PipelineTransformation pipelineTransformation, ForwardMessage forwardMessage,
                 VolumeConcUpdate volumeConcUpdate, MessageType messageType,
                 VesselTypeGeometry sourceVesselTypeGeometry, VesselTypeGeometry targetVesselTypeGeometry,
                 String[] reagentNames, LabVessel.MaterialType resultingMaterialType) {
        this.name = name;
        this.expectedEmptySources = expectSourcesEmpty;
        this.expectedEmptyTargets = expectTargetsEmpty;
        this.systemOfRecord = systemOfRecord;
        this.createSources = createSources;
        this.plasticToValidate = plasticToValidate;
        this.pipelineTransformation = pipelineTransformation;
        this.forwardMessage = forwardMessage;
        this.volumeConcUpdate = volumeConcUpdate;
        this.messageType = messageType;
        this.sourceVesselTypeGeometry = sourceVesselTypeGeometry;
        this.targetVesselTypeGeometry = targetVesselTypeGeometry;
        this.reagentNames = reagentNames;
        this.resultingMaterialType = resultingMaterialType;
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

    public static Set<LabEventType> getLabEventTypesForMaterialType(LabVessel.MaterialType materialType) {
        Set<LabEventType> resultSet=new HashSet<>();
        for (LabEventType labEventType : LabEventType.values()) {
            if (labEventType.resultingMaterialType == materialType) {
                resultSet.add(labEventType);
            }
        }
        return resultSet;
    }

    public ForwardMessage getForwardMessage() {
        return forwardMessage;
    }

    public VolumeConcUpdate getVolumeConcUpdate() {
        return volumeConcUpdate;
    }

    public LabVessel.MaterialType getResultingMaterialType() {
        return resultingMaterialType;
    }

    public MessageType getMessageType() {
        return messageType;
    }

    public VesselTypeGeometry getSourceVesselTypeGeometry() {
        return sourceVesselTypeGeometry;
    }

    public VesselTypeGeometry getTargetVesselTypeGeometry() {
        return targetVesselTypeGeometry;
    }

    public String[] getReagentNames() {
        return reagentNames;
    }
}

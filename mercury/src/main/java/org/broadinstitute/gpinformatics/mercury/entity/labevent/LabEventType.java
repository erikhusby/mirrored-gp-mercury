package org.broadinstitute.gpinformatics.mercury.entity.labevent;

import org.apache.commons.lang3.ArrayUtils;
import org.broadinstitute.gpinformatics.mercury.entity.Metadata;
import org.broadinstitute.gpinformatics.mercury.entity.vessel.BarcodedTube;
import org.broadinstitute.gpinformatics.mercury.entity.vessel.MaterialType;
import org.broadinstitute.gpinformatics.mercury.entity.vessel.PlateWell;
import org.broadinstitute.gpinformatics.mercury.entity.vessel.RackOfTubes;
import org.broadinstitute.gpinformatics.mercury.entity.vessel.SBSSection;
import org.broadinstitute.gpinformatics.mercury.entity.vessel.StaticPlate;
import org.broadinstitute.gpinformatics.mercury.entity.vessel.VesselTypeGeometry;

import javax.annotation.Nullable;
import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlTransient;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Properties common to all events of a particular message type
 */
public enum LabEventType {

    // Preflight
    PREFLIGHT_CLEANUP("PreFlightCleanup",
            ExpectSourcesEmpty.TRUE, ExpectTargetsEmpty.TRUE, SystemOfRecord.WORKFLOW_DEPENDENT, CreateSources.FALSE,
            PlasticToValidate.SOURCE, PipelineTransformation.NONE, ForwardMessage.NONE, VolumeConcUpdate.MERCURY_ONLY,
            LibraryType.NONE_ASSIGNED),
    PREFLIGHT_PICO_SETUP("PreflightPicoSetup",
            ExpectSourcesEmpty.TRUE, ExpectTargetsEmpty.TRUE, SystemOfRecord.WORKFLOW_DEPENDENT, CreateSources.FALSE,
            PlasticToValidate.SOURCE, PipelineTransformation.NONE, ForwardMessage.NONE, VolumeConcUpdate.MERCURY_ONLY,
            LibraryType.NONE_ASSIGNED),
    PREFLIGHT_NORMALIZATION("PreflightNormalization",
            ExpectSourcesEmpty.TRUE, ExpectTargetsEmpty.TRUE, SystemOfRecord.WORKFLOW_DEPENDENT, CreateSources.FALSE,
            PlasticToValidate.SOURCE, PipelineTransformation.NONE, ForwardMessage.NONE, VolumeConcUpdate.MERCURY_ONLY,
            LibraryType.NONE_ASSIGNED),
    PREFLIGHT_POST_NORM_PICO_SETUP("PreflightPostNormPicoSetup",
            ExpectSourcesEmpty.TRUE, ExpectTargetsEmpty.TRUE, SystemOfRecord.WORKFLOW_DEPENDENT, CreateSources.FALSE,
            PlasticToValidate.SOURCE, PipelineTransformation.NONE, ForwardMessage.NONE, VolumeConcUpdate.MERCURY_ONLY,
            LibraryType.NONE_ASSIGNED),

    // Shearing
    SHEARING_TRANSFER("ShearingTransfer",
            ExpectSourcesEmpty.FALSE, ExpectTargetsEmpty.TRUE, SystemOfRecord.WORKFLOW_DEPENDENT, CreateSources.FALSE,
            PlasticToValidate.SOURCE, PipelineTransformation.NONE, ForwardMessage.NONE, VolumeConcUpdate.MERCURY_ONLY,
            new ManualTransferDetails.Builder(MessageType.PLATE_TRANSFER_EVENT, RackOfTubes.RackType.Matrix96,
                    StaticPlate.PlateType.Eppendorf96,
                    new ReagentRequirements[]{new ReagentRequirements("CrimpCapLot")}).
                    build(),
            LibraryType.NONE_ASSIGNED),
    COVARIS_LOADED("CovarisLoaded",
            ExpectSourcesEmpty.FALSE, ExpectTargetsEmpty.TRUE, SystemOfRecord.WORKFLOW_DEPENDENT, CreateSources.FALSE,
            PlasticToValidate.SOURCE, PipelineTransformation.NONE, ForwardMessage.NONE, VolumeConcUpdate.MERCURY_ONLY,
            LibraryType.NONE_ASSIGNED),
    POST_SHEARING_TRANSFER_CLEANUP("PostShearingTransferCleanup",
            ExpectSourcesEmpty.FALSE, ExpectTargetsEmpty.TRUE, SystemOfRecord.WORKFLOW_DEPENDENT, CreateSources.FALSE,
            PlasticToValidate.SOURCE, PipelineTransformation.NONE, ForwardMessage.NONE, VolumeConcUpdate.MERCURY_ONLY,
            new ManualTransferDetails.Builder(MessageType.PLATE_TRANSFER_EVENT, StaticPlate.PlateType.Eppendorf96,
                    StaticPlate.PlateType.Eppendorf96,
                    new ReagentRequirements[]{new ReagentRequirements("SPRI"),
                            new ReagentRequirements("70% Ethanol"),
                            new ReagentRequirements("EB")}).
                    build(),
            LibraryType.NONE_ASSIGNED),
    SHEARING_QC("ShearingQC",
            ExpectSourcesEmpty.FALSE, ExpectTargetsEmpty.TRUE, SystemOfRecord.WORKFLOW_DEPENDENT, CreateSources.FALSE,
            PlasticToValidate.SOURCE, PipelineTransformation.NONE, ForwardMessage.NONE, VolumeConcUpdate.MERCURY_ONLY,
            LibraryType.NONE_ASSIGNED),

    UMI_ADDITION("UMIAddition",
            ExpectSourcesEmpty.FALSE, ExpectTargetsEmpty.FALSE, SystemOfRecord.MERCURY, CreateSources.FALSE,
            PlasticToValidate.TARGET, PipelineTransformation.NONE, ForwardMessage.NONE, VolumeConcUpdate.MERCURY_ONLY,
            LibraryType.NONE_ASSIGNED),
    POST_UMI_ADDITION_THERMO_CYCLER_LOADED("PostUMIAdditionThermoCyclerLoaded",
            ExpectSourcesEmpty.FALSE, ExpectTargetsEmpty.TRUE, SystemOfRecord.MERCURY, CreateSources.FALSE,
            PlasticToValidate.SOURCE, PipelineTransformation.NONE, ForwardMessage.NONE, VolumeConcUpdate.MERCURY_ONLY,
            LibraryType.NONE_ASSIGNED),
    UMI_CLEANUP("UMICleanup",
            ExpectSourcesEmpty.FALSE, ExpectTargetsEmpty.TRUE, SystemOfRecord.MERCURY, CreateSources.FALSE,
            PlasticToValidate.SOURCE, PipelineTransformation.NONE, ForwardMessage.NONE, VolumeConcUpdate.MERCURY_ONLY,
            LibraryType.NONE_ASSIGNED),
    DUAL_INDEX_PCR("DualIndexPCR",
            ExpectSourcesEmpty.FALSE, ExpectTargetsEmpty.TRUE, SystemOfRecord.WORKFLOW_DEPENDENT, CreateSources.FALSE,
            PlasticToValidate.TARGET, PipelineTransformation.PCR, ForwardMessage.NONE, VolumeConcUpdate.MERCURY_ONLY,
            LibraryType.NONE_ASSIGNED),
    POST_DUAL_INDEX_THERMO_CYCLER_LOADED("PostDualIndexThermoCyclerLoaded",
            ExpectSourcesEmpty.FALSE, ExpectTargetsEmpty.TRUE, SystemOfRecord.MERCURY, CreateSources.FALSE,
            PlasticToValidate.SOURCE, PipelineTransformation.NONE, ForwardMessage.NONE, VolumeConcUpdate.MERCURY_ONLY,
            LibraryType.NONE_ASSIGNED),

    // Library construction
    END_REPAIR("EndRepair",
            ExpectSourcesEmpty.TRUE, ExpectTargetsEmpty.TRUE, SystemOfRecord.WORKFLOW_DEPENDENT, CreateSources.FALSE,
            PlasticToValidate.SOURCE, PipelineTransformation.NONE, ForwardMessage.NONE, VolumeConcUpdate.MERCURY_ONLY,
            LibraryType.NONE_ASSIGNED),
    POST_END_REPAIR_THERMO_CYCLER_LOADED("PostEndRepairThermoCyclerLoaded",
            ExpectSourcesEmpty.FALSE, ExpectTargetsEmpty.TRUE, SystemOfRecord.WORKFLOW_DEPENDENT, CreateSources.FALSE,
            PlasticToValidate.SOURCE, PipelineTransformation.NONE, ForwardMessage.NONE, VolumeConcUpdate.MERCURY_ONLY,
            LibraryType.NONE_ASSIGNED),
    BEAD_RESUSPENSION("BeadResuspension",
            ExpectSourcesEmpty.TRUE, ExpectTargetsEmpty.TRUE, SystemOfRecord.MERCURY, CreateSources.FALSE,
            PlasticToValidate.SOURCE, PipelineTransformation.NONE, ForwardMessage.NONE, VolumeConcUpdate.MERCURY_ONLY,
            LibraryType.NONE_ASSIGNED),
    END_REPAIR_CLEANUP("EndRepairCleanup",
            ExpectSourcesEmpty.TRUE, ExpectTargetsEmpty.TRUE, SystemOfRecord.WORKFLOW_DEPENDENT, CreateSources.FALSE,
            PlasticToValidate.SOURCE, PipelineTransformation.NONE, ForwardMessage.NONE, VolumeConcUpdate.MERCURY_ONLY,
            LibraryType.NONE_ASSIGNED),
    A_BASE("ABase",
            ExpectSourcesEmpty.TRUE, ExpectTargetsEmpty.TRUE, SystemOfRecord.WORKFLOW_DEPENDENT, CreateSources.FALSE,
            PlasticToValidate.SOURCE, PipelineTransformation.NONE, ForwardMessage.NONE, VolumeConcUpdate.MERCURY_ONLY,
            LibraryType.NONE_ASSIGNED),
    POST_ABASE_THERMO_CYCLER_LOADED("PostAbaseThermoCyclerLoaded",
            ExpectSourcesEmpty.FALSE, ExpectTargetsEmpty.TRUE, SystemOfRecord.WORKFLOW_DEPENDENT, CreateSources.FALSE,
            PlasticToValidate.SOURCE, PipelineTransformation.NONE, ForwardMessage.NONE, VolumeConcUpdate.MERCURY_ONLY,
            LibraryType.NONE_ASSIGNED),
    A_BASE_CLEANUP("ABaseCleanup",
            ExpectSourcesEmpty.TRUE, ExpectTargetsEmpty.TRUE, SystemOfRecord.WORKFLOW_DEPENDENT, CreateSources.FALSE,
            PlasticToValidate.SOURCE, PipelineTransformation.NONE, ForwardMessage.NONE, VolumeConcUpdate.MERCURY_ONLY,
            LibraryType.NONE_ASSIGNED),
    INDEXED_ADAPTER_LIGATION("IndexedAdapterLigation",
            ExpectSourcesEmpty.TRUE, ExpectTargetsEmpty.FALSE, SystemOfRecord.WORKFLOW_DEPENDENT, CreateSources.FALSE,
            PlasticToValidate.TARGET, PipelineTransformation.LIGATION, ForwardMessage.NONE, VolumeConcUpdate.MERCURY_ONLY,
            LibraryType.NONE_ASSIGNED),
    POST_INDEXED_ADAPTER_LIGATION_THERMO_CYCLER_LOADED("PostIndexedAdapterLigationThermoCyclerLoaded",
            ExpectSourcesEmpty.TRUE, ExpectTargetsEmpty.TRUE, SystemOfRecord.WORKFLOW_DEPENDENT, CreateSources.FALSE,
            PlasticToValidate.SOURCE, PipelineTransformation.NONE, ForwardMessage.NONE, VolumeConcUpdate.MERCURY_ONLY,
            LibraryType.NONE_ASSIGNED),
    ADAPTER_LIGATION_CLEANUP("AdapterLigationCleanup",
            ExpectSourcesEmpty.FALSE, ExpectTargetsEmpty.TRUE, SystemOfRecord.WORKFLOW_DEPENDENT, CreateSources.FALSE,
            PlasticToValidate.SOURCE, PipelineTransformation.NONE, ForwardMessage.NONE, VolumeConcUpdate.MERCURY_ONLY,
            LibraryType.NONE_ASSIGNED),
    // no longer used, needed to import past messages
    WGS_ADAPTER_LIGATION_CLEANUP("WGSAdapterLigationCleanup",
            ExpectSourcesEmpty.FALSE, ExpectTargetsEmpty.TRUE, SystemOfRecord.WORKFLOW_DEPENDENT, CreateSources.FALSE,
            PlasticToValidate.SOURCE, PipelineTransformation.NONE, ForwardMessage.NONE, VolumeConcUpdate.MERCURY_ONLY,
            LibraryType.NONE_ASSIGNED),
    // The PCR is associated with PondRegistration, because we want a tube barcode for the pipeline
    POND_ENRICHMENT("PondEnrichment",
            ExpectSourcesEmpty.TRUE, ExpectTargetsEmpty.TRUE, SystemOfRecord.WORKFLOW_DEPENDENT, CreateSources.FALSE,
            PlasticToValidate.SOURCE, PipelineTransformation.NONE, ForwardMessage.NONE, VolumeConcUpdate.MERCURY_ONLY,
            LibraryType.NONE_ASSIGNED),
    // The PCR is associated with PondRegistration, because we want a tube barcode for the pipeline
    INDEX_P5_POND_ENRICHMENT("IndexP5PondEnrichment",
            ExpectSourcesEmpty.TRUE, ExpectTargetsEmpty.TRUE, SystemOfRecord.WORKFLOW_DEPENDENT, CreateSources.FALSE,
            PlasticToValidate.TARGET, PipelineTransformation.NONE, ForwardMessage.NONE, VolumeConcUpdate.MERCURY_ONLY,
            LibraryType.NONE_ASSIGNED),
    POST_POND_ENRICHMENT_THERMO_CYCLER_LOADED("PostPondEnrichmentThermoCyclerLoaded",
            ExpectSourcesEmpty.TRUE, ExpectTargetsEmpty.TRUE, SystemOfRecord.WORKFLOW_DEPENDENT, CreateSources.FALSE,
            PlasticToValidate.SOURCE, PipelineTransformation.NONE, ForwardMessage.NONE, VolumeConcUpdate.MERCURY_ONLY,
            LibraryType.NONE_ASSIGNED),
    POND_ENRICHMENT_CLEANUP("HybSelPondEnrichmentCleanup",
            ExpectSourcesEmpty.FALSE, ExpectTargetsEmpty.TRUE, SystemOfRecord.WORKFLOW_DEPENDENT, CreateSources.FALSE,
            PlasticToValidate.SOURCE, PipelineTransformation.NONE, ForwardMessage.NONE, VolumeConcUpdate.MERCURY_ONLY,
            LibraryType.NONE_ASSIGNED),
    POND_REGISTRATION("PondRegistration",
            ExpectSourcesEmpty.FALSE, ExpectTargetsEmpty.TRUE, SystemOfRecord.WORKFLOW_DEPENDENT, CreateSources.FALSE,
            PlasticToValidate.SOURCE, PipelineTransformation.PCR, ForwardMessage.NONE, VolumeConcUpdate.MERCURY_ONLY,
            LibraryType.ENRICHED_POND),
    POND_PICO("PondPico",
            ExpectSourcesEmpty.FALSE, ExpectTargetsEmpty.TRUE, SystemOfRecord.WORKFLOW_DEPENDENT, CreateSources.FALSE,
            PlasticToValidate.SOURCE, PipelineTransformation.NONE, ForwardMessage.NONE, VolumeConcUpdate.MERCURY_ONLY,
            LibraryType.NONE_ASSIGNED),
    POND_NORMALIZATION("PondNormalization",
            ExpectSourcesEmpty.FALSE, ExpectTargetsEmpty.TRUE, SystemOfRecord.MERCURY, CreateSources.FALSE,
            PlasticToValidate.SOURCE, PipelineTransformation.NONE, ForwardMessage.NONE, VolumeConcUpdate.MERCURY_ONLY,
            LibraryType.NONE_ASSIGNED),


    // Hybrid Selection
    PRE_SELECTION_POOL("PreSelectionPool",
            ExpectSourcesEmpty.FALSE, ExpectTargetsEmpty.TRUE, SystemOfRecord.WORKFLOW_DEPENDENT, CreateSources.FALSE,
            PlasticToValidate.SOURCE, PipelineTransformation.NONE, ForwardMessage.NONE, VolumeConcUpdate.MERCURY_ONLY,
            LibraryType.NONE_ASSIGNED),
    HYBRIDIZATION_BUCKET("HybridizationBucket",
            ExpectSourcesEmpty.TRUE, ExpectTargetsEmpty.TRUE, SystemOfRecord.MERCURY, CreateSources.FALSE,
            PlasticToValidate.SOURCE, PipelineTransformation.NONE, ForwardMessage.NONE, VolumeConcUpdate.MERCURY_ONLY,
            LibraryType.NONE_ASSIGNED),
    HYBRIDIZATION("Hybridization",
            ExpectSourcesEmpty.FALSE, ExpectTargetsEmpty.TRUE, SystemOfRecord.WORKFLOW_DEPENDENT, CreateSources.FALSE,
            PlasticToValidate.SOURCE, PipelineTransformation.NONE, ForwardMessage.NONE, VolumeConcUpdate.MERCURY_ONLY,
            LibraryType.NONE_ASSIGNED),
    POST_HYBRIDIZATION_THERMO_CYCLER_LOADED("PostHybridizationThermoCyclerLoaded",
            ExpectSourcesEmpty.FALSE, ExpectTargetsEmpty.TRUE, SystemOfRecord.WORKFLOW_DEPENDENT, CreateSources.FALSE,
            PlasticToValidate.SOURCE, PipelineTransformation.NONE, ForwardMessage.NONE, VolumeConcUpdate.MERCURY_ONLY,
            LibraryType.NONE_ASSIGNED),
    BAIT_SETUP("BaitSetup",
            ExpectSourcesEmpty.TRUE, ExpectTargetsEmpty.TRUE, SystemOfRecord.BOTH, CreateSources.FALSE,
            PlasticToValidate.SOURCE, PipelineTransformation.NONE, ForwardMessage.NONE, VolumeConcUpdate.MERCURY_ONLY,
            LibraryType.NONE_ASSIGNED),
    BAIT_ADDITION("BaitAddition",
            ExpectSourcesEmpty.TRUE, ExpectTargetsEmpty.FALSE, SystemOfRecord.WORKFLOW_DEPENDENT, CreateSources.FALSE,
            PlasticToValidate.TARGET, PipelineTransformation.NONE, ForwardMessage.NONE, VolumeConcUpdate.MERCURY_ONLY,
            LibraryType.NONE_ASSIGNED),
    BEAD_ADDITION("BeadAddition",
            ExpectSourcesEmpty.TRUE, ExpectTargetsEmpty.TRUE, SystemOfRecord.WORKFLOW_DEPENDENT, CreateSources.FALSE,
            PlasticToValidate.SOURCE, PipelineTransformation.NONE, ForwardMessage.NONE, VolumeConcUpdate.MERCURY_ONLY,
            LibraryType.NONE_ASSIGNED),
    AP_WASH("APWash",
            ExpectSourcesEmpty.TRUE, ExpectTargetsEmpty.TRUE, SystemOfRecord.WORKFLOW_DEPENDENT, CreateSources.FALSE,
            PlasticToValidate.SOURCE, PipelineTransformation.NONE, ForwardMessage.NONE, VolumeConcUpdate.MERCURY_ONLY,
            LibraryType.NONE_ASSIGNED),
    GS_WASH_1("GSWash1",
            ExpectSourcesEmpty.TRUE, ExpectTargetsEmpty.TRUE, SystemOfRecord.WORKFLOW_DEPENDENT, CreateSources.FALSE,
            PlasticToValidate.SOURCE, PipelineTransformation.NONE, ForwardMessage.NONE, VolumeConcUpdate.MERCURY_ONLY,
            LibraryType.NONE_ASSIGNED),
    GS_WASH_2("GSWash2",
            ExpectSourcesEmpty.TRUE, ExpectTargetsEmpty.TRUE, SystemOfRecord.WORKFLOW_DEPENDENT, CreateSources.FALSE,
            PlasticToValidate.SOURCE, PipelineTransformation.NONE, ForwardMessage.NONE, VolumeConcUpdate.MERCURY_ONLY,
            LibraryType.NONE_ASSIGNED),
    GS_WASH_3("GSWash3",
            ExpectSourcesEmpty.TRUE, ExpectTargetsEmpty.TRUE, SystemOfRecord.WORKFLOW_DEPENDENT, CreateSources.FALSE,
            PlasticToValidate.SOURCE, PipelineTransformation.NONE, ForwardMessage.NONE, VolumeConcUpdate.MERCURY_ONLY,
            LibraryType.NONE_ASSIGNED),
    GS_WASH_4("GSWash4",
            ExpectSourcesEmpty.TRUE, ExpectTargetsEmpty.TRUE, SystemOfRecord.WORKFLOW_DEPENDENT, CreateSources.FALSE,
            PlasticToValidate.SOURCE, PipelineTransformation.NONE, ForwardMessage.NONE, VolumeConcUpdate.MERCURY_ONLY,
            LibraryType.NONE_ASSIGNED),
    GS_WASH_5("GSWash5",
            ExpectSourcesEmpty.TRUE, ExpectTargetsEmpty.TRUE, SystemOfRecord.WORKFLOW_DEPENDENT, CreateSources.FALSE,
            PlasticToValidate.SOURCE, PipelineTransformation.NONE, ForwardMessage.NONE, VolumeConcUpdate.MERCURY_ONLY,
            LibraryType.NONE_ASSIGNED),
    GS_WASH_6("GSWash6",
            ExpectSourcesEmpty.TRUE, ExpectTargetsEmpty.TRUE, SystemOfRecord.WORKFLOW_DEPENDENT, CreateSources.FALSE,
            PlasticToValidate.SOURCE, PipelineTransformation.NONE, ForwardMessage.NONE, VolumeConcUpdate.MERCURY_ONLY,
            LibraryType.NONE_ASSIGNED),
    CATCH_ENRICHMENT_SETUP("CatchEnrichmentSetup",
            ExpectSourcesEmpty.TRUE, ExpectTargetsEmpty.TRUE, SystemOfRecord.WORKFLOW_DEPENDENT, CreateSources.FALSE,
            PlasticToValidate.SOURCE, PipelineTransformation.NONE, ForwardMessage.NONE, VolumeConcUpdate.MERCURY_ONLY,
            LibraryType.NONE_ASSIGNED),
    POST_CATCH_ENRICHMENT_SETUP_THERMO_CYCLER_LOADED("PostCatchEnrichmentSetupThermoCyclerLoaded",
            ExpectSourcesEmpty.TRUE, ExpectTargetsEmpty.TRUE, SystemOfRecord.WORKFLOW_DEPENDENT, CreateSources.FALSE,
            PlasticToValidate.SOURCE, PipelineTransformation.NONE, ForwardMessage.NONE, VolumeConcUpdate.MERCURY_ONLY,
            LibraryType.NONE_ASSIGNED),
    CATCH_ENRICHMENT_CLEANUP("CatchEnrichmentCleanup",
            ExpectSourcesEmpty.FALSE, ExpectTargetsEmpty.TRUE, SystemOfRecord.WORKFLOW_DEPENDENT, CreateSources.FALSE,
            PlasticToValidate.SOURCE, PipelineTransformation.NONE, ForwardMessage.NONE, VolumeConcUpdate.MERCURY_ONLY,
            LibraryType.NONE_ASSIGNED),
    NORMALIZED_CATCH_REGISTRATION("NormalizedCatchRegistration",
            ExpectSourcesEmpty.FALSE, ExpectTargetsEmpty.TRUE, SystemOfRecord.WORKFLOW_DEPENDENT, CreateSources.FALSE,
            PlasticToValidate.SOURCE, PipelineTransformation.PCR, ForwardMessage.NONE, VolumeConcUpdate.MERCURY_ONLY,
            LibraryType.HYBRID_SELECTION_AGILENT_CATCH),
    CATCH_PICO("CatchPico",
            ExpectSourcesEmpty.FALSE, ExpectTargetsEmpty.TRUE, SystemOfRecord.WORKFLOW_DEPENDENT, CreateSources.FALSE,
            PlasticToValidate.SOURCE, PipelineTransformation.NONE, ForwardMessage.NONE, VolumeConcUpdate.MERCURY_ONLY,
            LibraryType.NONE_ASSIGNED),

    // QTP
    POOLING_BUCKET("PoolingBucket",
            ExpectSourcesEmpty.TRUE, ExpectTargetsEmpty.TRUE, SystemOfRecord.MERCURY, CreateSources.FALSE,
            PlasticToValidate.SOURCE, PipelineTransformation.NONE, ForwardMessage.NONE, VolumeConcUpdate.MERCURY_ONLY,
            LibraryType.NONE_ASSIGNED),
    POOLING_TRANSFER("PoolingTransfer",
            ExpectSourcesEmpty.FALSE, ExpectTargetsEmpty.TRUE, SystemOfRecord.WORKFLOW_DEPENDENT, CreateSources.FALSE,
            PlasticToValidate.SOURCE, PipelineTransformation.NONE, ForwardMessage.NONE, VolumeConcUpdate.MERCURY_ONLY,
            new ManualTransferDetails.Builder(MessageType.PLATE_CHERRY_PICK_EVENT, RackOfTubes.RackType.Matrix96,
                    RackOfTubes.RackType.Matrix96, new ReagentRequirements[]{new ReagentRequirements("EB")})
            .build(),
            LibraryType.POOLED),
    CALIBRATED_POOLING_TRANSFER("CalibratedPoolingTransfer",
            ExpectSourcesEmpty.FALSE, ExpectTargetsEmpty.TRUE, SystemOfRecord.MERCURY, CreateSources.FALSE,
            PlasticToValidate.SOURCE, PipelineTransformation.NONE, ForwardMessage.NONE, VolumeConcUpdate.MERCURY_ONLY,
            LibraryType.CALIBRATED_POOLED),
    ICE_POST_HYB_POOLING_TRANSFER("IcePostHybPoolingTransfer",
            ExpectSourcesEmpty.FALSE, ExpectTargetsEmpty.TRUE, SystemOfRecord.WORKFLOW_DEPENDENT, CreateSources.FALSE,
            PlasticToValidate.SOURCE, PipelineTransformation.NONE, ForwardMessage.NONE, VolumeConcUpdate.MERCURY_ONLY,
            LibraryType.POOLED),
    ECO_TRANSFER("EcoTransfer",
            ExpectSourcesEmpty.FALSE, ExpectTargetsEmpty.TRUE, SystemOfRecord.WORKFLOW_DEPENDENT, CreateSources.FALSE,
            PlasticToValidate.SOURCE, PipelineTransformation.NONE, ForwardMessage.NONE, VolumeConcUpdate.MERCURY_ONLY,
            LibraryType.NONE_ASSIGNED),
    VIIA7_TRANSFER("Viia7Transfer",
            ExpectSourcesEmpty.FALSE, ExpectTargetsEmpty.TRUE, SystemOfRecord.WORKFLOW_DEPENDENT, CreateSources.FALSE,
            PlasticToValidate.SOURCE, PipelineTransformation.NONE, ForwardMessage.NONE, VolumeConcUpdate.MERCURY_ONLY,
            LibraryType.NONE_ASSIGNED),
    NORMALIZATION_TRANSFER("NormalizationTransfer",
            ExpectSourcesEmpty.FALSE, ExpectTargetsEmpty.TRUE, SystemOfRecord.WORKFLOW_DEPENDENT, CreateSources.FALSE,
            PlasticToValidate.SOURCE, PipelineTransformation.NONE, ForwardMessage.NONE, VolumeConcUpdate.MERCURY_ONLY,
            new ManualTransferDetails.Builder(MessageType.PLATE_CHERRY_PICK_EVENT, RackOfTubes.RackType.Matrix96,
                    RackOfTubes.RackType.Matrix96, new ReagentRequirements[]{new ReagentRequirements("EB")})
                    .build(),
            LibraryType.NORMALIZED),
    DENATURE_TRANSFER("DenatureTransfer",
            ExpectSourcesEmpty.FALSE, ExpectTargetsEmpty.TRUE, SystemOfRecord.WORKFLOW_DEPENDENT, CreateSources.FALSE,
            PlasticToValidate.SOURCE, PipelineTransformation.NONE, ForwardMessage.NONE, VolumeConcUpdate.MERCURY_ONLY,
            new ManualTransferDetails.Builder(MessageType.PLATE_CHERRY_PICK_EVENT, RackOfTubes.RackType.Matrix96,
                    RackOfTubes.RackType.Matrix96,
                    new ReagentRequirements[]{new ReagentRequirements("Hyb Buffer"),
                            new ReagentRequirements("NaOH"),
                            new ReagentRequirements("NucleaseFreeWater")})
                    .build(),
            LibraryType.DENATURED),
    STRIP_TUBE_B_TRANSFER("StripTubeBTransfer",
            ExpectSourcesEmpty.TRUE, ExpectTargetsEmpty.FALSE, SystemOfRecord.WORKFLOW_DEPENDENT, CreateSources.FALSE,
            PlasticToValidate.SOURCE, PipelineTransformation.NONE, ForwardMessage.NONE, VolumeConcUpdate.MERCURY_ONLY,
            new ManualTransferDetails.Builder(MessageType.STRIP_TUBE_CHERRY_PICK_EVENT, RackOfTubes.RackType.Matrix96,
                    RackOfTubes.RackType.StripTubeRackOf12,
                    new ReagentRequirements[]{new ReagentRequirements("Hyb Buffer")}).
                    sourceBarcodedTubeType(BarcodedTube.BarcodedTubeType.MatrixTube).
                    targetBarcodedTubeType(BarcodedTube.BarcodedTubeType.StripTube).
                    build(),
            LibraryType.NONE_ASSIGNED),

    // HiSeq 2000
    FLOWCELL_TRANSFER("FlowcellTransfer",
            ExpectSourcesEmpty.TRUE, ExpectTargetsEmpty.FALSE, SystemOfRecord.WORKFLOW_DEPENDENT, CreateSources.FALSE,
            PlasticToValidate.SOURCE, PipelineTransformation.NONE, ForwardMessage.NONE, VolumeConcUpdate.MERCURY_ONLY,
            new ManualTransferDetails.Builder(MessageType.PLATE_TRANSFER_EVENT, StaticPlate.ManualTransferFlowCellType.StripTube1x1,
                    StaticPlate.ManualTransferFlowCellType.FlowCell8,
                    new ReagentRequirements[]{new ReagentRequirements("CbotReagentKit")}).
                    machineNames(new String[]{"ST-001", "ST-002", "ST-003", "ST-004", "ST-005", "ST-006", "ST-007", "ST-008",
                            "ST-009", "ST-010", "ST-011", "ST-012", "ST-013", "ST-014", "ST-015", "ST-016", "ST-017",
                            "ST-018", "ST-019", "ST-020", "ST-021", "ST-022", "ST-023", "ST-024", "ST-025", "ST-026",
                            "ST-027", "ST-028", "ST-029", "ST-030", "ST-031", "ST-032", "ST-033", "ST-034", "ST-035",
                            "ST-036", "ST-037"}).build(),
            LibraryType.NONE_ASSIGNED),
    FLOWCELL_LOADED("FlowcellLoaded",
            ExpectSourcesEmpty.TRUE, ExpectTargetsEmpty.FALSE, SystemOfRecord.WORKFLOW_DEPENDENT, CreateSources.FALSE,
            PlasticToValidate.SOURCE, PipelineTransformation.NONE, ForwardMessage.NONE, VolumeConcUpdate.MERCURY_ONLY,
            LibraryType.NONE_ASSIGNED),

    // For HiSeq 2500
    DENATURE_TO_FLOWCELL_TRANSFER("DenatureToFlowcellTransfer",
            ExpectSourcesEmpty.FALSE, ExpectTargetsEmpty.TRUE, SystemOfRecord.WORKFLOW_DEPENDENT, CreateSources.FALSE,
            PlasticToValidate.SOURCE, PipelineTransformation.NONE, ForwardMessage.NONE, VolumeConcUpdate.MERCURY_ONLY,
            LibraryType.HISEQ_FLOWCELL),
    DENATURE_TO_DILUTION_TRANSFER("DenatureToDilutionTransfer",
            ExpectSourcesEmpty.FALSE, ExpectTargetsEmpty.TRUE, SystemOfRecord.WORKFLOW_DEPENDENT, CreateSources.FALSE,
            PlasticToValidate.SOURCE, PipelineTransformation.NONE, ForwardMessage.NONE, VolumeConcUpdate.MERCURY_ONLY,
            LibraryType.DILUTED_DENATURE),
    DILUTION_TO_FLOWCELL_TRANSFER("DilutionToFlowcellTransfer",
            ExpectSourcesEmpty.FALSE, ExpectTargetsEmpty.TRUE, SystemOfRecord.WORKFLOW_DEPENDENT, CreateSources.FALSE,
            PlasticToValidate.SOURCE, PipelineTransformation.NONE, ForwardMessage.NONE, VolumeConcUpdate.MERCURY_ONLY,
            LibraryType.HISEQ_FLOWCELL),

    // for MiSeq
    DENATURE_TO_REAGENT_KIT_TRANSFER("MiseqReagentKitLoading",
            ExpectSourcesEmpty.FALSE, ExpectTargetsEmpty.TRUE, SystemOfRecord.WORKFLOW_DEPENDENT, CreateSources.FALSE,
            PlasticToValidate.SOURCE, PipelineTransformation.NONE, ForwardMessage.NONE, VolumeConcUpdate.MERCURY_ONLY,
            LibraryType.NONE_ASSIGNED),
    REAGENT_KIT_TO_FLOWCELL_TRANSFER("ReagentKitToFlowcellTransfer",
            ExpectSourcesEmpty.FALSE, ExpectTargetsEmpty.TRUE, SystemOfRecord.WORKFLOW_DEPENDENT, CreateSources.FALSE,
            PlasticToValidate.SOURCE, PipelineTransformation.NONE, ForwardMessage.NONE, VolumeConcUpdate.MERCURY_ONLY,
            LibraryType.MISEQ_FLOWCELL),

    // Dev Samples
    DEV("DevCherryPick",
            ExpectSourcesEmpty.FALSE, ExpectTargetsEmpty.TRUE, SystemOfRecord.MERCURY, CreateSources.FALSE,
            PlasticToValidate.SOURCE, PipelineTransformation.NONE, ForwardMessage.NONE, VolumeConcUpdate.MERCURY_ONLY,
            new ManualTransferDetails.Builder(MessageType.PLATE_CHERRY_PICK_EVENT, RackOfTubes.RackType.Matrix96Anonymous,
                    RackOfTubes.RackType.Matrix96Anonymous).sourceContainerPrefix("DevSrc").targetContainerPrefix("DevDest").build(),
            LibraryType.NONE_ASSIGNED),

    // Sage
    SAGE_LOADING("SageLoading",
            ExpectSourcesEmpty.TRUE, ExpectTargetsEmpty.FALSE, SystemOfRecord.WORKFLOW_DEPENDENT, CreateSources.FALSE,
            PlasticToValidate.SOURCE, PipelineTransformation.NONE, ForwardMessage.NONE, VolumeConcUpdate.MERCURY_ONLY,
            LibraryType.NONE_ASSIGNED),
    SAGE_LOADED("SageLoaded",
            ExpectSourcesEmpty.TRUE, ExpectTargetsEmpty.FALSE, SystemOfRecord.WORKFLOW_DEPENDENT, CreateSources.FALSE,
            PlasticToValidate.SOURCE, PipelineTransformation.NONE, ForwardMessage.NONE, VolumeConcUpdate.MERCURY_ONLY,
            LibraryType.NONE_ASSIGNED),
    SAGE_UNLOADING("SageUnloading",
            ExpectSourcesEmpty.TRUE, ExpectTargetsEmpty.FALSE, SystemOfRecord.WORKFLOW_DEPENDENT, CreateSources.FALSE,
            PlasticToValidate.SOURCE, PipelineTransformation.NONE, ForwardMessage.NONE, VolumeConcUpdate.MERCURY_ONLY,
            LibraryType.NONE_ASSIGNED),
    SAGE_CLEANUP("SageCleanup",
            ExpectSourcesEmpty.TRUE, ExpectTargetsEmpty.FALSE, SystemOfRecord.WORKFLOW_DEPENDENT, CreateSources.FALSE,
            PlasticToValidate.SOURCE, PipelineTransformation.SIZE_SELECTION, ForwardMessage.NONE, VolumeConcUpdate.MERCURY_ONLY,
            LibraryType.NONE_ASSIGNED),

    // 10X
    TENX_MAKE_PLATE("10XMakePlate",
            ExpectSourcesEmpty.FALSE, ExpectTargetsEmpty.TRUE, SystemOfRecord.MERCURY, CreateSources.FALSE,
            PlasticToValidate.SOURCE, PipelineTransformation.NONE, ForwardMessage.NONE, VolumeConcUpdate.MERCURY_ONLY,
            LibraryType.NONE_ASSIGNED),
    TENX_CHIP_LOADING("10XChipLoading",
            ExpectSourcesEmpty.FALSE, ExpectTargetsEmpty.TRUE, SystemOfRecord.MERCURY, CreateSources.FALSE,
            PlasticToValidate.SOURCE, PipelineTransformation.NONE, ForwardMessage.NONE, VolumeConcUpdate.MERCURY_ONLY,
            LibraryType.NONE_ASSIGNED),
    TENX_CHIP_UNLOADING("10XChipUnloading",
            ExpectSourcesEmpty.FALSE, ExpectTargetsEmpty.TRUE, SystemOfRecord.MERCURY, CreateSources.FALSE,
            PlasticToValidate.SOURCE, PipelineTransformation.NONE, ForwardMessage.NONE, VolumeConcUpdate.MERCURY_ONLY,
            LibraryType.NONE_ASSIGNED),
    TENX_EMULSION_BREAKING("10XEmulsionBreaking",
            ExpectSourcesEmpty.FALSE, ExpectTargetsEmpty.TRUE, SystemOfRecord.MERCURY, CreateSources.FALSE,
            PlasticToValidate.SOURCE, PipelineTransformation.NONE, ForwardMessage.NONE, VolumeConcUpdate.MERCURY_ONLY,
            LibraryType.NONE_ASSIGNED),
    TENX_DYNABEAD_CLEANUP("10XDynabeadCleanup",
            ExpectSourcesEmpty.FALSE, ExpectTargetsEmpty.TRUE, SystemOfRecord.MERCURY, CreateSources.FALSE,
            PlasticToValidate.SOURCE, PipelineTransformation.NONE, ForwardMessage.NONE, VolumeConcUpdate.MERCURY_ONLY,
            LibraryType.NONE_ASSIGNED),
    TENX_PRE_LC_SPRI("10XPreLCSpri",
            ExpectSourcesEmpty.FALSE, ExpectTargetsEmpty.TRUE, SystemOfRecord.MERCURY, CreateSources.FALSE,
            PlasticToValidate.SOURCE, PipelineTransformation.NONE, ForwardMessage.NONE, VolumeConcUpdate.MERCURY_ONLY,
            LibraryType.NONE_ASSIGNED),

    // Fluidigm
    FLUIDIGM_SAMPLE_INPUT("FluidigmSampleInput",
            ExpectSourcesEmpty.TRUE, ExpectTargetsEmpty.FALSE, SystemOfRecord.WORKFLOW_DEPENDENT, CreateSources.FALSE,
            PlasticToValidate.SOURCE, PipelineTransformation.NONE, ForwardMessage.NONE, VolumeConcUpdate.MERCURY_ONLY,
            LibraryType.NONE_ASSIGNED),
    FLUIDIGM_INDEXED_ADAPTER_INPUT("FluidigmIndexedAdapterInput",
            ExpectSourcesEmpty.TRUE, ExpectTargetsEmpty.FALSE, SystemOfRecord.WORKFLOW_DEPENDENT, CreateSources.FALSE,
            PlasticToValidate.SOURCE, PipelineTransformation.LIGATION, ForwardMessage.NONE, VolumeConcUpdate.MERCURY_ONLY,
            LibraryType.NONE_ASSIGNED),
    FLUIDIGM_INDEXED_ADAPTER_PCR("FluidigmIndexedAdapterPCR",
            ExpectSourcesEmpty.TRUE, ExpectTargetsEmpty.FALSE, SystemOfRecord.WORKFLOW_DEPENDENT, CreateSources.FALSE,
            PlasticToValidate.SOURCE, PipelineTransformation.PCR, ForwardMessage.NONE, VolumeConcUpdate.MERCURY_ONLY,
            LibraryType.NONE_ASSIGNED),
    FLUIDIGM_PRODUCT_DILUTION("FluidigmProductDilution",
            ExpectSourcesEmpty.TRUE, ExpectTargetsEmpty.FALSE, SystemOfRecord.WORKFLOW_DEPENDENT, CreateSources.FALSE,
            PlasticToValidate.SOURCE, PipelineTransformation.NONE, ForwardMessage.NONE, VolumeConcUpdate.MERCURY_ONLY,
            LibraryType.NONE_ASSIGNED),
    FLUIDIGM_HARVESTING_TO_RACK("FluidigmHarvestingToRack",
            ExpectSourcesEmpty.TRUE, ExpectTargetsEmpty.FALSE, SystemOfRecord.WORKFLOW_DEPENDENT, CreateSources.FALSE,
            PlasticToValidate.SOURCE, PipelineTransformation.NONE, ForwardMessage.NONE, VolumeConcUpdate.MERCURY_ONLY,
            LibraryType.NONE_ASSIGNED),
    FLUIDIGM_HARVESTING("FluidigmHarvesting",
            ExpectSourcesEmpty.TRUE, ExpectTargetsEmpty.FALSE, SystemOfRecord.WORKFLOW_DEPENDENT, CreateSources.FALSE,
            PlasticToValidate.SOURCE, PipelineTransformation.NONE, ForwardMessage.NONE, VolumeConcUpdate.MERCURY_ONLY,
            LibraryType.NONE_ASSIGNED),
    FLUIDIGM_FINAL_TRANSFER("FluidigmFinalTransfer",
            ExpectSourcesEmpty.TRUE, ExpectTargetsEmpty.FALSE, SystemOfRecord.WORKFLOW_DEPENDENT, CreateSources.FALSE,
            PlasticToValidate.SOURCE, PipelineTransformation.NONE, ForwardMessage.NONE, VolumeConcUpdate.MERCURY_ONLY,
            LibraryType.NONE_ASSIGNED),

    // Jump
    JUMP_INITIAL_SHEARING_TRANSFER("JumpInitialShearingTransfer",
            ExpectSourcesEmpty.FALSE, ExpectTargetsEmpty.TRUE, SystemOfRecord.WORKFLOW_DEPENDENT, CreateSources.FALSE,
            PlasticToValidate.SOURCE, PipelineTransformation.NONE, ForwardMessage.NONE, VolumeConcUpdate.MERCURY_ONLY,
            LibraryType.NONE_ASSIGNED),
    JUMP_INITIAL_SIZE_SELECTION("JumpInitialSizeSelection",
            ExpectSourcesEmpty.FALSE, ExpectTargetsEmpty.TRUE, SystemOfRecord.WORKFLOW_DEPENDENT, CreateSources.FALSE,
            PlasticToValidate.SOURCE, PipelineTransformation.SIZE_SELECTION, ForwardMessage.NONE,
            VolumeConcUpdate.MERCURY_ONLY, LibraryType.NONE_ASSIGNED),
    JUMP_END_REPAIR_1("JumpEndRepair1",
            ExpectSourcesEmpty.TRUE, ExpectTargetsEmpty.FALSE, SystemOfRecord.WORKFLOW_DEPENDENT, CreateSources.FALSE,
            PlasticToValidate.SOURCE, PipelineTransformation.NONE, ForwardMessage.NONE, VolumeConcUpdate.MERCURY_ONLY,
            LibraryType.NONE_ASSIGNED),
    JUMP_INITIAL_SIZE_SELECTION_QC("JumpInitialSizeSelectionQC",
            ExpectSourcesEmpty.FALSE, ExpectTargetsEmpty.TRUE, SystemOfRecord.WORKFLOW_DEPENDENT, CreateSources.FALSE,
            PlasticToValidate.SOURCE, PipelineTransformation.NONE, ForwardMessage.NONE, VolumeConcUpdate.MERCURY_ONLY,
            LibraryType.NONE_ASSIGNED),
    JUMP_END_REPAIR_1_CLEANUP("JumpEndRepair1Cleanup",
            ExpectSourcesEmpty.FALSE, ExpectTargetsEmpty.TRUE, SystemOfRecord.WORKFLOW_DEPENDENT, CreateSources.FALSE,
            PlasticToValidate.SOURCE, PipelineTransformation.NONE, ForwardMessage.NONE, VolumeConcUpdate.MERCURY_ONLY,
            LibraryType.NONE_ASSIGNED),
    JUMP_END_REPAIR_1_QC("JumpEndRepair1QC",
            ExpectSourcesEmpty.FALSE, ExpectTargetsEmpty.TRUE, SystemOfRecord.WORKFLOW_DEPENDENT, CreateSources.FALSE,
            PlasticToValidate.SOURCE, PipelineTransformation.NONE, ForwardMessage.NONE, VolumeConcUpdate.MERCURY_ONLY,
            LibraryType.NONE_ASSIGNED),
    JUMP_CIRCULARIZATION_SETUP("JumpCircularizationSetup",
            ExpectSourcesEmpty.FALSE, ExpectTargetsEmpty.TRUE, SystemOfRecord.WORKFLOW_DEPENDENT, CreateSources.FALSE,
            PlasticToValidate.SOURCE, PipelineTransformation.NONE, ForwardMessage.NONE, VolumeConcUpdate.MERCURY_ONLY,
            LibraryType.NONE_ASSIGNED),
    JUMP_EXONUCLEASE_TREATMENT("JumpExonucleaseTreatment",
            ExpectSourcesEmpty.TRUE, ExpectTargetsEmpty.FALSE, SystemOfRecord.WORKFLOW_DEPENDENT, CreateSources.FALSE,
            PlasticToValidate.SOURCE, PipelineTransformation.NONE, ForwardMessage.NONE, VolumeConcUpdate.MERCURY_ONLY,
            LibraryType.NONE_ASSIGNED),
    JUMP_EXO_INACTIVATION("JumpExoInactivation",
            ExpectSourcesEmpty.FALSE, ExpectTargetsEmpty.TRUE, SystemOfRecord.WORKFLOW_DEPENDENT, CreateSources.FALSE,
            PlasticToValidate.SOURCE, PipelineTransformation.NONE, ForwardMessage.NONE, VolumeConcUpdate.MERCURY_ONLY,
            LibraryType.NONE_ASSIGNED),
    JUMP_POST_CIRC_SIZE_SELECTION("JumpPostCircSizeSelection",
            ExpectSourcesEmpty.FALSE, ExpectTargetsEmpty.TRUE, SystemOfRecord.WORKFLOW_DEPENDENT, CreateSources.FALSE,
            PlasticToValidate.SOURCE, PipelineTransformation.SIZE_SELECTION, ForwardMessage.NONE, VolumeConcUpdate.MERCURY_ONLY,
            LibraryType.NONE_ASSIGNED),
    JUMP_IMMOBILIZATION("JumpImmobilization",
            ExpectSourcesEmpty.TRUE, ExpectTargetsEmpty.FALSE, SystemOfRecord.WORKFLOW_DEPENDENT, CreateSources.FALSE,
            PlasticToValidate.SOURCE, PipelineTransformation.NONE, ForwardMessage.NONE, VolumeConcUpdate.MERCURY_ONLY,
            LibraryType.NONE_ASSIGNED),
    JUMP_IMMOBILIZATION_WASH("JumpImmobilizationWash",
            ExpectSourcesEmpty.FALSE, ExpectTargetsEmpty.TRUE, SystemOfRecord.WORKFLOW_DEPENDENT, CreateSources.FALSE,
            PlasticToValidate.SOURCE, PipelineTransformation.NONE, ForwardMessage.NONE, VolumeConcUpdate.MERCURY_ONLY,
            LibraryType.NONE_ASSIGNED),
    JUMP_END_REPAIR_2("JumpEndRepair2",
            ExpectSourcesEmpty.FALSE, ExpectTargetsEmpty.TRUE, SystemOfRecord.WORKFLOW_DEPENDENT, CreateSources.FALSE,
            PlasticToValidate.SOURCE, PipelineTransformation.NONE, ForwardMessage.NONE, VolumeConcUpdate.MERCURY_ONLY,
            LibraryType.NONE_ASSIGNED),
    JUMP_END_REPAIR_2_WASH("JumpEndRepair2Wash",
            ExpectSourcesEmpty.FALSE, ExpectTargetsEmpty.TRUE, SystemOfRecord.WORKFLOW_DEPENDENT, CreateSources.FALSE,
            PlasticToValidate.SOURCE, PipelineTransformation.NONE, ForwardMessage.NONE, VolumeConcUpdate.MERCURY_ONLY,
            LibraryType.NONE_ASSIGNED),
    JUMP_A_TAILING("JumpATailing",
            ExpectSourcesEmpty.FALSE, ExpectTargetsEmpty.TRUE, SystemOfRecord.WORKFLOW_DEPENDENT, CreateSources.FALSE,
            PlasticToValidate.SOURCE, PipelineTransformation.NONE, ForwardMessage.NONE, VolumeConcUpdate.MERCURY_ONLY,
            LibraryType.NONE_ASSIGNED),
    JUMP_A_TAILING_WASH("JumpATailingWash",
            ExpectSourcesEmpty.FALSE, ExpectTargetsEmpty.TRUE, SystemOfRecord.WORKFLOW_DEPENDENT, CreateSources.FALSE,
            PlasticToValidate.SOURCE, PipelineTransformation.NONE, ForwardMessage.NONE, VolumeConcUpdate.MERCURY_ONLY,
            LibraryType.NONE_ASSIGNED),
    JUMP_ADD_INDEX("JumpAddIndex",
            ExpectSourcesEmpty.FALSE, ExpectTargetsEmpty.TRUE, SystemOfRecord.WORKFLOW_DEPENDENT, CreateSources.FALSE,
            PlasticToValidate.SOURCE, PipelineTransformation.NONE, ForwardMessage.NONE, VolumeConcUpdate.MERCURY_ONLY,
            LibraryType.NONE_ASSIGNED),
    JUMP_ADAPTER_LIGATION("JumpAdapterLigation",
            ExpectSourcesEmpty.FALSE, ExpectTargetsEmpty.TRUE, SystemOfRecord.WORKFLOW_DEPENDENT, CreateSources.FALSE,
            PlasticToValidate.SOURCE, PipelineTransformation.LIGATION, ForwardMessage.NONE, VolumeConcUpdate.MERCURY_ONLY,
            LibraryType.NONE_ASSIGNED),
    JUMP_ADAPTER_LIGATION_WASH("JumpAdapterLigationWash",
            ExpectSourcesEmpty.FALSE, ExpectTargetsEmpty.TRUE, SystemOfRecord.WORKFLOW_DEPENDENT, CreateSources.FALSE,
            PlasticToValidate.SOURCE, PipelineTransformation.NONE, ForwardMessage.NONE, VolumeConcUpdate.MERCURY_ONLY,
            LibraryType.NONE_ASSIGNED),
    JUMP_AMPLIFICATION("JumpAmplification",
            ExpectSourcesEmpty.FALSE, ExpectTargetsEmpty.TRUE, SystemOfRecord.WORKFLOW_DEPENDENT, CreateSources.FALSE,
            PlasticToValidate.SOURCE, PipelineTransformation.NONE, ForwardMessage.NONE, VolumeConcUpdate.MERCURY_ONLY,
            LibraryType.NONE_ASSIGNED),
    JUMP_FINAL_LIBRARY_ELUTION("JumpFinalLibraryElution",
            ExpectSourcesEmpty.FALSE, ExpectTargetsEmpty.TRUE, SystemOfRecord.WORKFLOW_DEPENDENT, CreateSources.FALSE,
            PlasticToValidate.SOURCE, PipelineTransformation.NONE, ForwardMessage.NONE, VolumeConcUpdate.MERCURY_ONLY,
            LibraryType.NONE_ASSIGNED),
    JUMP_FINAL_LIBRARY_SIZE_SELECTION("JumpFinalLibrarySizeSelection",
            ExpectSourcesEmpty.FALSE, ExpectTargetsEmpty.TRUE, SystemOfRecord.WORKFLOW_DEPENDENT, CreateSources.FALSE,
            PlasticToValidate.SOURCE, PipelineTransformation.SIZE_SELECTION, ForwardMessage.NONE, VolumeConcUpdate.MERCURY_ONLY,
            LibraryType.NONE_ASSIGNED),
    JUMP_FINAL_LIBRARY_QC("JumpFinalLibraryQC",
            ExpectSourcesEmpty.FALSE, ExpectTargetsEmpty.TRUE, SystemOfRecord.WORKFLOW_DEPENDENT, CreateSources.FALSE,
            PlasticToValidate.SOURCE, PipelineTransformation.NONE, ForwardMessage.NONE, VolumeConcUpdate.MERCURY_ONLY,
            LibraryType.NONE_ASSIGNED),
    JUMP_ENRICHED_LIBRARY_REGISTRATION("JumpEnrichedLibraryRegistration",
            ExpectSourcesEmpty.FALSE, ExpectTargetsEmpty.TRUE, SystemOfRecord.WORKFLOW_DEPENDENT, CreateSources.FALSE,
            PlasticToValidate.SOURCE, PipelineTransformation.NONE, ForwardMessage.NONE, VolumeConcUpdate.MERCURY_ONLY,
            LibraryType.NONE_ASSIGNED),

    // BSP Pico
    PICO_PLATING_BUCKET("PicoPlatingBucket",
            ExpectSourcesEmpty.TRUE, ExpectTargetsEmpty.TRUE, SystemOfRecord.MERCURY, CreateSources.FALSE,
            PlasticToValidate.SOURCE, PipelineTransformation.NONE, ForwardMessage.NONE, VolumeConcUpdate.MERCURY_ONLY,
            LibraryType.NONE_ASSIGNED),
    PICO_PLATING_QC("PicoPlatingQC",
            ExpectSourcesEmpty.TRUE, ExpectTargetsEmpty.TRUE, SystemOfRecord.MERCURY, CreateSources.FALSE,
            PlasticToValidate.SOURCE, PipelineTransformation.NONE, ForwardMessage.NONE, VolumeConcUpdate.MERCURY_ONLY,
            LibraryType.NONE_ASSIGNED),
    PICO_DILUTION_TRANSFER("PicoDilutionTransfer",
            ExpectSourcesEmpty.TRUE, ExpectTargetsEmpty.TRUE, SystemOfRecord.MERCURY, CreateSources.FALSE,
            PlasticToValidate.SOURCE, PipelineTransformation.NONE, ForwardMessage.NONE, VolumeConcUpdate.MERCURY_ONLY,
            LibraryType.NONE_ASSIGNED),
    PICO_DILUTION_TRANSFER_FORWARD_BSP("PicoDilutionTransferForwardBsp",
            ExpectSourcesEmpty.TRUE, ExpectTargetsEmpty.TRUE, SystemOfRecord.MERCURY, CreateSources.FALSE,
            PlasticToValidate.SOURCE, PipelineTransformation.NONE, ForwardMessage.BSP, VolumeConcUpdate.MERCURY_ONLY,
            LibraryType.NONE_ASSIGNED, TranslateBspMessage.SECTION_TO_CHERRY),
    PICO_BUFFER_ADDITION("PicoBufferAddition",
            ExpectSourcesEmpty.TRUE, ExpectTargetsEmpty.TRUE, SystemOfRecord.MERCURY, CreateSources.FALSE,
            PlasticToValidate.SOURCE, PipelineTransformation.NONE, ForwardMessage.NONE, VolumeConcUpdate.MERCURY_ONLY,
            LibraryType.NONE_ASSIGNED),
    PICO_MICROFLUOR_TRANSFER("PicoMicrofluorTransfer",
            ExpectSourcesEmpty.TRUE, ExpectTargetsEmpty.TRUE, SystemOfRecord.MERCURY, CreateSources.FALSE,
            PlasticToValidate.SOURCE, PipelineTransformation.NONE, ForwardMessage.NONE, VolumeConcUpdate.MERCURY_ONLY,
            LibraryType.NONE_ASSIGNED),
    PICO_STANDARDS_TRANSFER("PicoStandardsTransfer",
            ExpectSourcesEmpty.TRUE, ExpectTargetsEmpty.TRUE, SystemOfRecord.MERCURY, CreateSources.TRUE,
            PlasticToValidate.SOURCE, PipelineTransformation.NONE, ForwardMessage.NONE, VolumeConcUpdate.MERCURY_ONLY,
            LibraryType.NONE_ASSIGNED),
    PICO_PLATING_POST_NORM_PICO("PicoPlatingPostNorm",
            ExpectSourcesEmpty.TRUE, ExpectTargetsEmpty.TRUE, SystemOfRecord.MERCURY, CreateSources.FALSE,
            PlasticToValidate.SOURCE, PipelineTransformation.NONE, ForwardMessage.NONE, VolumeConcUpdate.MERCURY_ONLY,
            LibraryType.NONE_ASSIGNED),
    PICO_REWORK("PicoRework",
            ExpectSourcesEmpty.TRUE, ExpectTargetsEmpty.TRUE, SystemOfRecord.MERCURY, CreateSources.FALSE,
            PlasticToValidate.SOURCE, PipelineTransformation.NONE, ForwardMessage.NONE, VolumeConcUpdate.BSP_AND_MERCURY, LibraryType.NONE_ASSIGNED),
    PICO_TRANSFER("PicoTransfer",
            ExpectSourcesEmpty.FALSE, ExpectTargetsEmpty.TRUE, SystemOfRecord.MERCURY, CreateSources.FALSE,
            PlasticToValidate.SOURCE, PipelineTransformation.NONE, ForwardMessage.NONE, VolumeConcUpdate.MERCURY_ONLY,
            LibraryType.NONE_ASSIGNED),

    //RiboGreen
    RIBO_TRANSFER("RiboTransfer",
            ExpectSourcesEmpty.FALSE, ExpectTargetsEmpty.TRUE, SystemOfRecord.MERCURY, CreateSources.FALSE,
            PlasticToValidate.SOURCE, PipelineTransformation.NONE, ForwardMessage.NONE, VolumeConcUpdate.MERCURY_ONLY,
            LibraryType.NONE_ASSIGNED),
    RIBO_DILUTION_TRANSFER("RiboDilutionTransfer",
            ExpectSourcesEmpty.TRUE, ExpectTargetsEmpty.TRUE, SystemOfRecord.MERCURY, CreateSources.FALSE,
            PlasticToValidate.SOURCE, PipelineTransformation.NONE, ForwardMessage.NONE, VolumeConcUpdate.MERCURY_ONLY,
            LibraryType.NONE_ASSIGNED),
    RIBO_MICROFLUOR_TRANSFER("RiboMicrofluorTransfer",
            ExpectSourcesEmpty.TRUE, ExpectTargetsEmpty.TRUE, SystemOfRecord.MERCURY, CreateSources.FALSE,
            PlasticToValidate.SOURCE, PipelineTransformation.NONE, ForwardMessage.NONE, VolumeConcUpdate.MERCURY_ONLY,
            LibraryType.NONE_ASSIGNED),
    RIBO_BUFFER_ADDITION("RiboBufferAddition",
            ExpectSourcesEmpty.TRUE, ExpectTargetsEmpty.TRUE, SystemOfRecord.MERCURY, CreateSources.FALSE,
            PlasticToValidate.SOURCE, PipelineTransformation.NONE, ForwardMessage.NONE, VolumeConcUpdate.MERCURY_ONLY,
            LibraryType.NONE_ASSIGNED),
    POLY_A_TS_ALIQUOT("PolyATSAliquot",
            ExpectSourcesEmpty.FALSE, ExpectTargetsEmpty.TRUE, SystemOfRecord.MERCURY, CreateSources.FALSE,
            PlasticToValidate.SOURCE, PipelineTransformation.NONE, ForwardMessage.NONE, VolumeConcUpdate.MERCURY_ONLY,
            LibraryType.NONE_ASSIGNED),
    POLY_A_TS_ALIQUOT_SPIKE("PolyATSAliquotSpike",
            ExpectSourcesEmpty.FALSE, ExpectTargetsEmpty.TRUE, SystemOfRecord.MERCURY, CreateSources.FALSE,
            PlasticToValidate.SOURCE, PipelineTransformation.NONE, ForwardMessage.NONE, VolumeConcUpdate.MERCURY_ONLY,
            LibraryType.NONE_ASSIGNED),

    // Dried Blood Spot
    DBS_SAMPLE_PUNCH("DBSSamplePunch",
            ExpectSourcesEmpty.FALSE, ExpectTargetsEmpty.TRUE, SystemOfRecord.MERCURY, CreateSources.TRUE,
            PlasticToValidate.SOURCE, PipelineTransformation.NONE, ForwardMessage.BSP, VolumeConcUpdate.MERCURY_ONLY,
            new ManualTransferDetails.Builder(MessageType.PLATE_TRANSFER_EVENT,
                    RackOfTubes.RackType.FTAPaperHolder,
                    StaticPlate.PlateType.Plate96RoundWellBlock2000).
                    targetSection(SBSSection.ALL96).
                    sourceBarcodedTubeType(BarcodedTube.BarcodedTubeType.FTAPaper).
                    sourceSection(SBSSection.ALL96).
                    sourceContainerPrefix("DBS").
                limsFile(true).build(),
            LibraryType.NONE_ASSIGNED, TranslateBspMessage.SECTION_TO_CHERRY),
    DBS_INCUBATION_MIX("DBSIncubationMix",
            ExpectSourcesEmpty.TRUE, ExpectTargetsEmpty.FALSE, SystemOfRecord.MERCURY, CreateSources.FALSE,
            PlasticToValidate.SOURCE, PipelineTransformation.NONE, ForwardMessage.NONE, VolumeConcUpdate.MERCURY_ONLY,
            LibraryType.NONE_ASSIGNED),
    DBS_LYSIS_BUFFER("DBSLysisBuffer",
            ExpectSourcesEmpty.TRUE, ExpectTargetsEmpty.FALSE, SystemOfRecord.MERCURY, CreateSources.FALSE,
            PlasticToValidate.SOURCE, PipelineTransformation.NONE, ForwardMessage.NONE, VolumeConcUpdate.MERCURY_ONLY,
            LibraryType.NONE_ASSIGNED),
    DBS_MAGNETIC_RESIN("DBSMagneticResin",
            ExpectSourcesEmpty.TRUE, ExpectTargetsEmpty.FALSE, SystemOfRecord.MERCURY, CreateSources.FALSE,
            PlasticToValidate.SOURCE, PipelineTransformation.NONE, ForwardMessage.NONE, VolumeConcUpdate.MERCURY_ONLY,
            LibraryType.NONE_ASSIGNED),
    DBS_1ST_PURIFICATION("DBS1stPurification",
            ExpectSourcesEmpty.FALSE, ExpectTargetsEmpty.TRUE, SystemOfRecord.MERCURY, CreateSources.FALSE,
            PlasticToValidate.SOURCE, PipelineTransformation.NONE, ForwardMessage.BSP, VolumeConcUpdate.MERCURY_ONLY,
            LibraryType.NONE_ASSIGNED, TranslateBspMessage.SECTION_TO_CHERRY),
    DBS_WASH_BUFFER("DBSWashBuffer",
            ExpectSourcesEmpty.TRUE, ExpectTargetsEmpty.FALSE, SystemOfRecord.MERCURY, CreateSources.FALSE,
            PlasticToValidate.SOURCE, PipelineTransformation.NONE, ForwardMessage.NONE, VolumeConcUpdate.MERCURY_ONLY,
            LibraryType.NONE_ASSIGNED),
    DBS_2ND_PURIFICATION("DBS2ndPurification",
            ExpectSourcesEmpty.FALSE, ExpectTargetsEmpty.TRUE, SystemOfRecord.MERCURY, CreateSources.FALSE,
            PlasticToValidate.SOURCE, PipelineTransformation.NONE, ForwardMessage.BSP, VolumeConcUpdate.MERCURY_ONLY,
            new ManualTransferDetails.Builder(MessageType.PLATE_TRANSFER_EVENT,
                    StaticPlate.PlateType.Plate96RoundWellBlock2000,
                    StaticPlate.PlateType.Plate96RoundWellBlock2000).
                    targetSection(SBSSection.ALL96).
                    sourceSection(SBSSection.ALL96).
                    build(),
            LibraryType.NONE_ASSIGNED, TranslateBspMessage.SECTION_TO_CHERRY),
    DBS_ELUTION_BUFFER("DBSElutionBuffer",
            ExpectSourcesEmpty.FALSE, ExpectTargetsEmpty.TRUE, SystemOfRecord.MERCURY, CreateSources.FALSE,
            PlasticToValidate.SOURCE, PipelineTransformation.NONE, ForwardMessage.NONE, VolumeConcUpdate.MERCURY_ONLY,
            LibraryType.NONE_ASSIGNED),
    DBS_FINAL_TRANSFER("DBSFinalTransfer",
            ExpectSourcesEmpty.FALSE, ExpectTargetsEmpty.TRUE, SystemOfRecord.MERCURY, CreateSources.FALSE,
            PlasticToValidate.SOURCE, PipelineTransformation.NONE, ForwardMessage.BSP, VolumeConcUpdate.MERCURY_ONLY,
            null, MaterialType.DNA_DNA_GENOMIC, LibraryType.NONE_ASSIGNED),

    //Cryovial Blood and Saliva Extraction
    BLOOD_CENTRIFUGE("BloodCentrifuge",
            ExpectSourcesEmpty.FALSE, ExpectTargetsEmpty.TRUE, SystemOfRecord.MERCURY, CreateSources.FALSE,
            PlasticToValidate.SOURCE, PipelineTransformation.NONE, ForwardMessage.NONE, VolumeConcUpdate.BSP_AND_MERCURY,
            new ManualTransferDetails.Builder(MessageType.PLATE_EVENT,
                    RackOfTubes.RackType.FlipperRackRow24,
                    RackOfTubes.RackType.FlipperRackRow24).
                    targetVolume(true).
                    build(),
            LibraryType.NONE_ASSIGNED),
    URINE_CENTRIFUGE("UrineCentrifuge",
            ExpectSourcesEmpty.FALSE, ExpectTargetsEmpty.TRUE, SystemOfRecord.MERCURY, CreateSources.FALSE,
            PlasticToValidate.SOURCE, PipelineTransformation.NONE, ForwardMessage.NONE, VolumeConcUpdate.BSP_AND_MERCURY,
            new ManualTransferDetails.Builder(MessageType.PLATE_EVENT,
                    RackOfTubes.RackType.FlipperRackRow24,
                    RackOfTubes.RackType.FlipperRackRow24).
                    targetVolume(true).
                    build(),
            LibraryType.NONE_ASSIGNED),
    CSF_CENTRIFUGE("CsfCentrifuge",
            ExpectSourcesEmpty.FALSE, ExpectTargetsEmpty.TRUE, SystemOfRecord.MERCURY, CreateSources.FALSE,
            PlasticToValidate.SOURCE, PipelineTransformation.NONE, ForwardMessage.NONE, VolumeConcUpdate.BSP_AND_MERCURY,
            new ManualTransferDetails.Builder(MessageType.PLATE_EVENT,
                    RackOfTubes.RackType.FlipperRackRow24,
                    RackOfTubes.RackType.FlipperRackRow24).
                    targetVolume(true).
                    build(),
            LibraryType.NONE_ASSIGNED),
    BLOOD_BUFFY_COAT_TRANSFER("BloodBuffyCoatTransfer",
            ExpectSourcesEmpty.FALSE, ExpectTargetsEmpty.TRUE, SystemOfRecord.MERCURY, CreateSources.FALSE,
            PlasticToValidate.SOURCE, PipelineTransformation.NONE, ForwardMessage.NONE, VolumeConcUpdate.MERCURY_ONLY,
            new ManualTransferDetails.Builder(MessageType.PLATE_CHERRY_PICK_EVENT,
                    RackOfTubes.RackType.FlipperRackRow8, RackOfTubes.RackType.FlipperRackRow8).
                    sourceBarcodedTubeType(BarcodedTube.BarcodedTubeType.VacutainerBloodTube6).
                    sourceContainerPrefix("Blood").
                    targetVesselTypeGeometries(new VesselTypeGeometry[] {
                            BarcodedTube.BarcodedTubeType.Sarstedt_Tube_2mL
                    }).
                    targetBarcodedTubeType(BarcodedTube.BarcodedTubeType.Sarstedt_Tube_2mL).
                    targetContainerPrefix("BuffyCoat").
                    targetVolume(true).
                    requireSingleParticipant(true).
                    build(),
            LibraryType.NONE_ASSIGNED, "_BC", Metadata.Key.TUMOR_NORMAL, "Normal", MaterialType.WHOLE_BLOOD_BUFFY_COAT,
            SourceHandling.DEPLETE),
    BLOOD_PLASMA_TRANSFER("BloodPlasmaBuffyTransfer",
            ExpectSourcesEmpty.FALSE, ExpectTargetsEmpty.TRUE, SystemOfRecord.MERCURY, CreateSources.FALSE,
            PlasticToValidate.SOURCE, PipelineTransformation.NONE, ForwardMessage.BSP, VolumeConcUpdate.MERCURY_ONLY,
            new ManualTransferDetails.Builder(MessageType.PLATE_CHERRY_PICK_EVENT,
                    RackOfTubes.RackType.FlipperRackRow8,
                    RackOfTubes.RackType.FlipperRackRow8).
                    sourceBarcodedTubeType(BarcodedTube.BarcodedTubeType.VacutainerBloodTube6).
                    sourceContainerPrefix("Blood").
                    targetBarcodedTubeType(BarcodedTube.BarcodedTubeType.CentriCutieSC_5).
                    targetContainerPrefix("Plasma").
                    secondaryEvent(BLOOD_BUFFY_COAT_TRANSFER).
                    requireSingleParticipant(true).
                    build(),
            MaterialType.PLASMA_PLASMA, LibraryType.NONE_ASSIGNED),
    PLASMA_CENTRIFUGE("PlasmaCentrifuge",
            ExpectSourcesEmpty.FALSE, ExpectTargetsEmpty.TRUE, SystemOfRecord.MERCURY, CreateSources.FALSE,
            PlasticToValidate.SOURCE, PipelineTransformation.NONE, ForwardMessage.NONE, VolumeConcUpdate.MERCURY_ONLY,
            new ManualTransferDetails.Builder(MessageType.PLATE_EVENT,
                    RackOfTubes.RackType.FlipperRackRow24,
                    RackOfTubes.RackType.FlipperRackRow24).build(),
            LibraryType.NONE_ASSIGNED),
    BLOOD_PLASMA_SECOND_TRANSFER("BloodPlasmaSecondTransfer",
            ExpectSourcesEmpty.FALSE, ExpectTargetsEmpty.TRUE, SystemOfRecord.MERCURY, CreateSources.FALSE,
            PlasticToValidate.SOURCE, PipelineTransformation.NONE, ForwardMessage.NONE, VolumeConcUpdate.MERCURY_ONLY,
            new ManualTransferDetails.Builder(MessageType.PLATE_CHERRY_PICK_EVENT,
                    RackOfTubes.RackType.FlipperRackRow8,
                    RackOfTubes.RackType.FlipperRackRow8).
                    sourceBarcodedTubeType(BarcodedTube.BarcodedTubeType.CentriCutieSC_5).
                    targetBarcodedTubeType(BarcodedTube.BarcodedTubeType.FluidX_6mL).
                    targetVolume(true).
                    requireSingleParticipant(true).
                    build(),
            LibraryType.NONE_ASSIGNED, "_P", Metadata.Key.TUMOR_NORMAL, "Tumor", MaterialType.PLASMA_PLASMA,
            SourceHandling.DEPLETE),
    CSF_TRANSFER("CsfTransfer",
            ExpectSourcesEmpty.FALSE, ExpectTargetsEmpty.TRUE, SystemOfRecord.MERCURY, CreateSources.TRUE,
            PlasticToValidate.SOURCE, PipelineTransformation.NONE, ForwardMessage.BSP_APPLY_SM_IDS, VolumeConcUpdate.MERCURY_ONLY,
            new ManualTransferDetails.Builder(MessageType.PLATE_CHERRY_PICK_EVENT,
                    RackOfTubes.RackType.FlipperRackRow8,
                    RackOfTubes.RackType.FlipperRackRow8).
                    sourceBarcodedTubeType(BarcodedTube.BarcodedTubeType.VacutainerBloodTube6).
                    sourceContainerPrefix("CSFSource").
                    targetBarcodedTubeType(BarcodedTube.BarcodedTubeType.FluidX_6mL).
                    targetContainerPrefix("CSFTarget").
                    targetVolume(true).
                    requireSingleParticipant(true).
                    build(),
            MaterialType.BODILY_FLUID_CEREBROSPINAL_FLUID, LibraryType.NONE_ASSIGNED,
            SourceHandling.DEPLETE),
    URINE_TRANSFER("UrineTransfer",
            ExpectSourcesEmpty.FALSE, ExpectTargetsEmpty.TRUE, SystemOfRecord.MERCURY, CreateSources.TRUE,
            PlasticToValidate.SOURCE, PipelineTransformation.NONE, ForwardMessage.BSP, VolumeConcUpdate.MERCURY_ONLY,
            new ManualTransferDetails.Builder(MessageType.PLATE_CHERRY_PICK_EVENT,
                    RackOfTubes.RackType.FlipperRackRow8,
                    RackOfTubes.RackType.FlipperRackRow8).
                    sourceBarcodedTubeType(BarcodedTube.BarcodedTubeType.VacutainerBloodTube6).
                    sourceContainerPrefix("UrineSource").
                    targetBarcodedTubeType(BarcodedTube.BarcodedTubeType.CentriCutieSC_5).
                    targetContainerPrefix("UrineTarget").
                    requireSingleParticipant(true).
                    build(),
            MaterialType.BODILY_FLUID_URINE, LibraryType.NONE_ASSIGNED, SourceHandling.DEPLETE),
    URINE_SECOND_TRANSFER("UrineSecondTransfer",
            ExpectSourcesEmpty.FALSE, ExpectTargetsEmpty.TRUE, SystemOfRecord.MERCURY, CreateSources.FALSE,
            PlasticToValidate.SOURCE, PipelineTransformation.NONE, ForwardMessage.BSP_APPLY_SM_IDS, VolumeConcUpdate.MERCURY_ONLY,
            new ManualTransferDetails.Builder(MessageType.PLATE_CHERRY_PICK_EVENT,
                    RackOfTubes.RackType.FlipperRackRow8,
                    RackOfTubes.RackType.FlipperRackRow8).
                    sourceBarcodedTubeType(BarcodedTube.BarcodedTubeType.CentriCutieSC_5).
                    sourceContainerPrefix("UrineSource").
                    targetBarcodedTubeType(BarcodedTube.BarcodedTubeType.FluidX_6mL).
                    targetContainerPrefix("UrineTarget").
                    targetVolume(true).
                    requireSingleParticipant(true).
                    build(),
            MaterialType.BODILY_FLUID_URINE, LibraryType.POOLED, SourceHandling.DEPLETE),
    BLOOD_PLASMA_POOLING_TRANSFER("BloodPlasmaPoolingTransfer",
            ExpectSourcesEmpty.FALSE, ExpectTargetsEmpty.TRUE, SystemOfRecord.MERCURY, CreateSources.FALSE,
            PlasticToValidate.SOURCE, PipelineTransformation.NONE, ForwardMessage.BSP, VolumeConcUpdate.MERCURY_ONLY,
            LibraryType.NONE_ASSIGNED),
    QIASYMPHONY_CELL_FREE("QiaSymphonyCellFree",
            ExpectSourcesEmpty.FALSE, ExpectTargetsEmpty.TRUE, SystemOfRecord.MERCURY, CreateSources.FALSE,
            PlasticToValidate.SOURCE, PipelineTransformation.NONE, ForwardMessage.BSP, VolumeConcUpdate.MERCURY_ONLY,
            new ManualTransferDetails.Builder(MessageType.PLATE_TRANSFER_EVENT,
                    RackOfTubes.RackType.QiasymphonyCarrier24,
                    RackOfTubes.RackType.Matrix96SlotRack14,
                    new ReagentRequirements[]{new ReagentRequirements("QIASymphony Kit")}).
                    sourceBarcodedTubeType(BarcodedTube.BarcodedTubeType.FluidX_6mL).
                    targetBarcodedTubeType(BarcodedTube.BarcodedTubeType.MatrixTube075).
                    sourceSection(SBSSection.P96_24ROWSOF4_COLWISE_8TIP).
                    targetSection(SBSSection.ALL96).limsFile(true).build(),
            MaterialType.DNA_DNA_CELL_FREE, LibraryType.NONE_ASSIGNED, SourceHandling.DEPLETE),
    QIASYMPHONY_GENOMIC("QiaSymphonyGenomic",
            ExpectSourcesEmpty.FALSE, ExpectTargetsEmpty.TRUE, SystemOfRecord.MERCURY, CreateSources.FALSE,
            PlasticToValidate.SOURCE, PipelineTransformation.NONE, ForwardMessage.BSP, VolumeConcUpdate.MERCURY_ONLY,
            new ManualTransferDetails.Builder(MessageType.PLATE_TRANSFER_EVENT,
                    RackOfTubes.RackType.QiasymphonyCarrier24,
                    RackOfTubes.RackType.Matrix96SlotRack14,
                    new ReagentRequirements[]{new ReagentRequirements("QIASymphony Kit")}).
                    sourceBarcodedTubeType(BarcodedTube.BarcodedTubeType.FluidX_6mL).
                    targetBarcodedTubeType(BarcodedTube.BarcodedTubeType.MatrixTube075).
                    sourceSection(SBSSection.P96_24ROWSOF4_COLWISE_8TIP).
                    targetSection(SBSSection.ALL96).limsFile(true).build(),
            MaterialType.DNA_DNA_GENOMIC, LibraryType.NONE_ASSIGNED, SourceHandling.DEPLETE),
    BLOOD_CRYOVIAL_EXTRACTION("BloodCryovialExtraction",
            ExpectSourcesEmpty.FALSE, ExpectTargetsEmpty.TRUE, SystemOfRecord.MERCURY, CreateSources.TRUE,
            PlasticToValidate.SOURCE, PipelineTransformation.NONE, ForwardMessage.BSP, VolumeConcUpdate.MERCURY_ONLY,
            LibraryType.NONE_ASSIGNED),
    BLOOD_DEEPWELL_CHEMAGEN_TRANSFER("ExtractionsBloodDeepwellToChemagen",
            ExpectSourcesEmpty.TRUE, ExpectTargetsEmpty.TRUE, SystemOfRecord.MERCURY, CreateSources.FALSE,
            PlasticToValidate.SOURCE, PipelineTransformation.NONE, ForwardMessage.BSP, VolumeConcUpdate.MERCURY_ONLY,
            LibraryType.NONE_ASSIGNED),
    BLOOD_CHEMAGEN_TO_FINAL_RACK("ExtractionsBloodChemagenToFinalRack",
            ExpectSourcesEmpty.TRUE, ExpectTargetsEmpty.TRUE, SystemOfRecord.MERCURY, CreateSources.FALSE,
            PlasticToValidate.SOURCE, PipelineTransformation.NONE, ForwardMessage.BSP, VolumeConcUpdate.MERCURY_ONLY,
            LibraryType.NONE_ASSIGNED),
    SALIVA_CRYOVIAL_EXTRACTION("SalivaCryovialExtraction",
            ExpectSourcesEmpty.FALSE, ExpectTargetsEmpty.TRUE, SystemOfRecord.MERCURY, CreateSources.TRUE,
            PlasticToValidate.SOURCE, PipelineTransformation.NONE, ForwardMessage.BSP, VolumeConcUpdate.MERCURY_ONLY,
            LibraryType.NONE_ASSIGNED),
    SALIVA_DEEPWELL_CHEMAGEN_TRANSFER("ExtractionsSalivaDeepwellToChemagen",
            ExpectSourcesEmpty.TRUE, ExpectTargetsEmpty.TRUE, SystemOfRecord.MERCURY, CreateSources.FALSE,
            PlasticToValidate.SOURCE, PipelineTransformation.NONE, ForwardMessage.BSP, VolumeConcUpdate.MERCURY_ONLY,
            LibraryType.NONE_ASSIGNED),
    SALIVA_CHEMAGEN_TO_FINAL_RACK("ExtractionsSalivaChemagenToFinalRack",
            ExpectSourcesEmpty.TRUE, ExpectTargetsEmpty.TRUE, SystemOfRecord.MERCURY, CreateSources.FALSE,
            PlasticToValidate.SOURCE, PipelineTransformation.NONE, ForwardMessage.BSP, VolumeConcUpdate.MERCURY_ONLY,
            LibraryType.NONE_ASSIGNED),

    //TNA from Stool Extratcion
    STOOL_EXTRACTION_SETUP("StoolExtractionSetup",
            ExpectSourcesEmpty.TRUE, ExpectTargetsEmpty.TRUE, SystemOfRecord.MERCURY, CreateSources.FALSE,
            PlasticToValidate.SOURCE, PipelineTransformation.NONE, ForwardMessage.NONE, VolumeConcUpdate.MERCURY_ONLY,
            LibraryType.NONE_ASSIGNED),
    STOOL_FINAL_TRANSFER("StoolFinalTransfer",
            ExpectSourcesEmpty.TRUE, ExpectTargetsEmpty.TRUE, SystemOfRecord.MERCURY, CreateSources.FALSE,
            PlasticToValidate.SOURCE, PipelineTransformation.NONE, ForwardMessage.NONE, VolumeConcUpdate.MERCURY_ONLY,
            LibraryType.NONE_ASSIGNED),
    STOOL_RNA_ALIQUOT("StoolRnaAliquot",
            ExpectSourcesEmpty.TRUE, ExpectTargetsEmpty.TRUE, SystemOfRecord.MERCURY, CreateSources.FALSE,
            PlasticToValidate.SOURCE, PipelineTransformation.NONE, ForwardMessage.NONE, VolumeConcUpdate.MERCURY_ONLY,
            LibraryType.NONE_ASSIGNED),
    STOOL_RNASE_ADDITION("StoolRNaseAddition",
            ExpectSourcesEmpty.FALSE, ExpectTargetsEmpty.TRUE, SystemOfRecord.MERCURY, CreateSources.FALSE,
            PlasticToValidate.SOURCE, PipelineTransformation.NONE, ForwardMessage.NONE, VolumeConcUpdate.MERCURY_ONLY,
            LibraryType.NONE_ASSIGNED),
    STOOL_SPRI_CLEANUP("StoolSPRICleanup",
            ExpectSourcesEmpty.TRUE, ExpectTargetsEmpty.TRUE, SystemOfRecord.MERCURY, CreateSources.FALSE,
            PlasticToValidate.SOURCE, PipelineTransformation.NONE, ForwardMessage.NONE, VolumeConcUpdate.MERCURY_ONLY,
            LibraryType.NONE_ASSIGNED),

    // Qiagen All Prep Stool Extraction
    STOOL_BEAD_PLATE_TRANSFER("StoolBeadPlateTransfer",
            ExpectSourcesEmpty.FALSE, ExpectTargetsEmpty.TRUE, SystemOfRecord.MERCURY, CreateSources.TRUE,
            PlasticToValidate.SOURCE, PipelineTransformation.NONE, ForwardMessage.BSP, VolumeConcUpdate.MERCURY_ONLY,
            new ManualTransferDetails.Builder(MessageType.PLATE_TRANSFER_EVENT,
                    RackOfTubes.RackType.Eppendorf12x8BoxWell,
                    StaticPlate.PlateType.Plate96WellPowerBead).
                    targetSection(SBSSection.ALL96).
                    sourceSection(SBSSection.ALL96).targetWellTypeGeometry(PlateWell.WellType.Well2000).
                    sourceBarcodedTubeType(BarcodedTube.BarcodedTubeType.Cryovial2018).sourceMassRemoved(true).
                    reagentRequirements(new ReagentRequirements[]{new ReagentRequirements("AllPrep 96 PowerFecal DNA/RNA")}).
                    build(),
            MaterialType.OTHER_LYSATE, LibraryType.NONE_ASSIGNED),
    STOOL_BEAD_BEATING("BeadBeating",
            ExpectSourcesEmpty.FALSE, ExpectTargetsEmpty.TRUE, SystemOfRecord.MERCURY, CreateSources.TRUE,
            PlasticToValidate.SOURCE, PipelineTransformation.NONE, ForwardMessage.NONE, VolumeConcUpdate.MERCURY_ONLY,
            new ManualTransferDetails.Builder(MessageType.PLATE_EVENT,
                    null,
                    StaticPlate.PlateType.Plate96WellPowerBead).
                    targetSection(SBSSection.ALL96).build(),
            LibraryType.NONE_ASSIGNED),
    STOOL_SUPERNATENT_TRANSFER("StoolSupernatentTransfer",
            ExpectSourcesEmpty.FALSE, ExpectTargetsEmpty.TRUE, SystemOfRecord.MERCURY, CreateSources.TRUE,
            PlasticToValidate.SOURCE, PipelineTransformation.NONE, ForwardMessage.BSP, VolumeConcUpdate.MERCURY_ONLY,
            new ManualTransferDetails.Builder(MessageType.PLATE_TRANSFER_EVENT,
                    StaticPlate.PlateType.Plate96WellPowerBead,
                    StaticPlate.PlateType.Plate96RoundWellBlock2000).
                    targetSection(SBSSection.ALL96).
                    sourceSection(SBSSection.ALL96).build(),
            LibraryType.NONE_ASSIGNED, SourceHandling.DEPLETE),
    STOOL_IRS_ADDITION("StoolIRSAddition",
            ExpectSourcesEmpty.FALSE, ExpectTargetsEmpty.TRUE, SystemOfRecord.MERCURY, CreateSources.TRUE,
            PlasticToValidate.SOURCE, PipelineTransformation.NONE, ForwardMessage.NONE, VolumeConcUpdate.MERCURY_ONLY,
            new ManualTransferDetails.Builder(MessageType.PLATE_EVENT,
                    null,
                    StaticPlate.PlateType.Plate96RoundWellBlock2000).
                    targetSection(SBSSection.ALL96).build(),
            LibraryType.NONE_ASSIGNED),
    STOOL_IRS_SUPERNATENT_TRANSFER("StoolIRSSupernatentTransfer",
            ExpectSourcesEmpty.FALSE, ExpectTargetsEmpty.TRUE, SystemOfRecord.MERCURY, CreateSources.TRUE,
            PlasticToValidate.SOURCE, PipelineTransformation.NONE, ForwardMessage.BSP, VolumeConcUpdate.MERCURY_ONLY,
            new ManualTransferDetails.Builder(MessageType.PLATE_TRANSFER_EVENT,
                    StaticPlate.PlateType.Plate96RoundWellBlock2000,
                    StaticPlate.PlateType.Plate96RoundWellBlock2000).
                    targetSection(SBSSection.ALL96).
                    sourceSection(SBSSection.ALL96).build(),
            LibraryType.NONE_ASSIGNED, SourceHandling.DEPLETE),
    STOOL_FILTER_PLATE_TRANSFER("StoolFilterPlateTransfer",
            ExpectSourcesEmpty.FALSE, ExpectTargetsEmpty.TRUE, SystemOfRecord.MERCURY, CreateSources.TRUE,
            PlasticToValidate.SOURCE, PipelineTransformation.NONE, ForwardMessage.BSP, VolumeConcUpdate.MERCURY_ONLY,
            new ManualTransferDetails.Builder(MessageType.PLATE_TRANSFER_EVENT,
                    StaticPlate.PlateType.Plate96RoundWellBlock2000,
                    StaticPlate.PlateType.Plate96RNEasyWell1000).
                    targetSection(SBSSection.ALL96).
                    sourceSection(SBSSection.ALL96).build(),
            LibraryType.NONE_ASSIGNED, SourceHandling.DEPLETE),
    STOOL_RNA_CAPTURE_PLATE_TRANSFER("StoolRNACapturePlateTransfer",
            ExpectSourcesEmpty.FALSE, ExpectTargetsEmpty.TRUE, SystemOfRecord.MERCURY, CreateSources.TRUE,
            PlasticToValidate.SOURCE, PipelineTransformation.NONE, ForwardMessage.BSP, VolumeConcUpdate.MERCURY_ONLY,
            new ManualTransferDetails.Builder(MessageType.PLATE_TRANSFER_EVENT,
                    StaticPlate.PlateType.Plate96RoundWellBlock2000,
                    StaticPlate.PlateType.Plate96RoundWellBlock2000).
                    targetSection(SBSSection.ALL96).
                    sourceSection(SBSSection.ALL96).build(),
            LibraryType.NONE_ASSIGNED, SourceHandling.DEPLETE),
    STOOL_RNA_ISOLATION("StoolRNAIsolation",
            ExpectSourcesEmpty.FALSE, ExpectTargetsEmpty.TRUE, SystemOfRecord.MERCURY, CreateSources.TRUE,
            PlasticToValidate.SOURCE, PipelineTransformation.NONE, ForwardMessage.BSP, VolumeConcUpdate.MERCURY_ONLY,
            new ManualTransferDetails.Builder(MessageType.PLATE_TRANSFER_EVENT,
                    StaticPlate.PlateType.Plate96RoundWellBlock2000,
                    StaticPlate.PlateType.Plate96RNEasyWell1000).
                    targetSection(SBSSection.ALL96).
                    sourceSection(SBSSection.ALL96).build(),
            LibraryType.NONE_ASSIGNED, SourceHandling.DEPLETE),
    STOOL_DNA_ISOLATION("StoolDNAIsolation",
            ExpectSourcesEmpty.FALSE, ExpectTargetsEmpty.TRUE, SystemOfRecord.MERCURY, CreateSources.TRUE,
            PlasticToValidate.SOURCE, PipelineTransformation.NONE, ForwardMessage.NONE, VolumeConcUpdate.MERCURY_ONLY,
            new ManualTransferDetails.Builder(MessageType.PLATE_EVENT,
                    null,
                    StaticPlate.PlateType.Plate96RNEasyWell1000).
                    targetSection(SBSSection.ALL96).build(),
            LibraryType.NONE_ASSIGNED),
    STOOL_RNA_FILTER_TO_ELUTION_PLATE("StoolRNAFilterToElutionPlate",
            ExpectSourcesEmpty.FALSE, ExpectTargetsEmpty.TRUE, SystemOfRecord.MERCURY, CreateSources.TRUE,
            PlasticToValidate.SOURCE, PipelineTransformation.NONE, ForwardMessage.BSP, VolumeConcUpdate.MERCURY_ONLY,
            new ManualTransferDetails.Builder(MessageType.PLATE_TRANSFER_EVENT,
                    StaticPlate.PlateType.Plate96RNEasyWell1000,
                    StaticPlate.PlateType.Plate96Well200PCR).
                    targetSection(SBSSection.ALL96).
                    sourceSection(SBSSection.ALL96).build(),
            LibraryType.NONE_ASSIGNED, SourceHandling.DEPLETE),
    STOOL_DNA_FILTER_TO_ELUTION_PLATE("StoolDNAFilterToElutionPlate",
            ExpectSourcesEmpty.FALSE, ExpectTargetsEmpty.TRUE, SystemOfRecord.MERCURY, CreateSources.TRUE,
            PlasticToValidate.SOURCE, PipelineTransformation.NONE, ForwardMessage.BSP, VolumeConcUpdate.MERCURY_ONLY,
            new ManualTransferDetails.Builder(MessageType.PLATE_TRANSFER_EVENT,
                    StaticPlate.PlateType.Plate96RNEasyWell1000,
                    StaticPlate.PlateType.Plate96Well200PCR).
                    targetSection(SBSSection.ALL96).
                    sourceSection(SBSSection.ALL96).build(),
            LibraryType.NONE_ASSIGNED, SourceHandling.DEPLETE),
    STOOL_RNA_FINAL_ELUTION("StoolRNAFinalElution",
            ExpectSourcesEmpty.FALSE, ExpectTargetsEmpty.TRUE, SystemOfRecord.MERCURY, CreateSources.TRUE,
            PlasticToValidate.SOURCE, PipelineTransformation.NONE, ForwardMessage.BSP, VolumeConcUpdate.MERCURY_ONLY,
            new ManualTransferDetails.Builder(MessageType.PLATE_TRANSFER_EVENT,
                    StaticPlate.PlateType.Plate96Well200PCR,
                    RackOfTubes.RackType.Matrix96SlotRack075).
                    targetSection(SBSSection.ALL96).
                    sourceSection(SBSSection.ALL96).
                    targetBarcodedTubeType(BarcodedTube.BarcodedTubeType.MatrixTube075).build(),
            MaterialType.RNA_TOTAL_RNA, LibraryType.NONE_ASSIGNED, SourceHandling.DEPLETE),
    STOOL_DNA_ELUTION("StoolDNAElution",
            ExpectSourcesEmpty.FALSE, ExpectTargetsEmpty.TRUE, SystemOfRecord.MERCURY, CreateSources.TRUE,
            PlasticToValidate.SOURCE, PipelineTransformation.NONE, ForwardMessage.BSP, VolumeConcUpdate.MERCURY_ONLY,
            new ManualTransferDetails.Builder(MessageType.PLATE_TRANSFER_EVENT,
                    StaticPlate.PlateType.Plate96RNEasyWell1000,
                    RackOfTubes.RackType.Matrix96SlotRack075).
                    targetSection(SBSSection.ALL96).
                    sourceSection(SBSSection.ALL96).
                    targetBarcodedTubeType(BarcodedTube.BarcodedTubeType.MatrixTube075).build(),
            MaterialType.DNA_DNA_GENOMIC, LibraryType.NONE_ASSIGNED, SourceHandling.DEPLETE),

    //ALL Prep Forward BSP
    ALL_PREP_TRANSFER_CRYOVIAL("AllPrepTransferCryovial",
            ExpectSourcesEmpty.FALSE, ExpectTargetsEmpty.TRUE, SystemOfRecord.MERCURY, CreateSources.TRUE,
            PlasticToValidate.SOURCE, PipelineTransformation.NONE, ForwardMessage.BSP, VolumeConcUpdate.MERCURY_ONLY,
            new ManualTransferDetails.Builder(MessageType.PLATE_TRANSFER_EVENT,
                    RackOfTubes.RackType.FlipperRackRow24,
                    RackOfTubes.RackType.FlipperRackRow24).
                    targetSection(SBSSection.ROWOF24).
                    sourceSection(SBSSection.ROWOF24)
                    .sourceBarcodedTubeType(BarcodedTube.BarcodedTubeType.Cryovial2018)
                    .targetBarcodedTubeType(BarcodedTube.BarcodedTubeType.Cryovial2018)
                    .build(),
            LibraryType.NONE_ASSIGNED),
    ALLPREP_CRYOVIAL_TO_QIASHREDDER("AllPrepCryovialToQiashredder",
            ExpectSourcesEmpty.FALSE, ExpectTargetsEmpty.TRUE, SystemOfRecord.MERCURY, CreateSources.TRUE,
            PlasticToValidate.SOURCE, PipelineTransformation.NONE, ForwardMessage.BSP, VolumeConcUpdate.MERCURY_ONLY,
            new ManualTransferDetails.Builder(MessageType.PLATE_TRANSFER_EVENT,
                    RackOfTubes.RackType.FlipperRackRow24,
                    RackOfTubes.RackType.FlipperRackRow24).
                    targetSection(SBSSection.ROWOF24).
                    sourceSection(SBSSection.ROWOF24)
                    .sourceBarcodedTubeType(BarcodedTube.BarcodedTubeType.Cryovial2018)
                    .targetBarcodedTubeType(BarcodedTube.BarcodedTubeType.EppendorfFliptop20)
                    .build(),
            LibraryType.NONE_ASSIGNED),
    ALLPREP_LYSATE_TO_DNA_SPIN("AllPrepLysateToDnaSpin",
            ExpectSourcesEmpty.FALSE, ExpectTargetsEmpty.TRUE, SystemOfRecord.MERCURY, CreateSources.TRUE,
            PlasticToValidate.SOURCE, PipelineTransformation.NONE, ForwardMessage.BSP, VolumeConcUpdate.MERCURY_ONLY,
            new ManualTransferDetails.Builder(MessageType.PLATE_TRANSFER_EVENT,
                    RackOfTubes.RackType.FlipperRackRow24,
                    RackOfTubes.RackType.FlipperRackRow24).
                    targetSection(SBSSection.ROWOF24).
                    sourceSection(SBSSection.ROWOF24)
                    .sourceBarcodedTubeType(BarcodedTube.BarcodedTubeType.EppendorfFliptop20)
                    .targetBarcodedTubeType(BarcodedTube.BarcodedTubeType.SpinColumn)
                    .build(),
            LibraryType.NONE_ASSIGNED),
    ALLPREP_DNA_SPIN_TO_FLOWTHROUGH("AllPrepDnaSpinToFlowthrough",
            ExpectSourcesEmpty.FALSE, ExpectTargetsEmpty.TRUE, SystemOfRecord.MERCURY, CreateSources.TRUE,
            PlasticToValidate.SOURCE, PipelineTransformation.NONE, ForwardMessage.BSP, VolumeConcUpdate.MERCURY_ONLY,
            new ManualTransferDetails.Builder(MessageType.PLATE_TRANSFER_EVENT,
                    RackOfTubes.RackType.FlipperRackRow24,
                    RackOfTubes.RackType.FlipperRackRow24).
                    targetSection(SBSSection.ROWOF24).
                    sourceSection(SBSSection.ROWOF24)
                    .sourceBarcodedTubeType(BarcodedTube.BarcodedTubeType.SpinColumn)
                    .targetBarcodedTubeType(BarcodedTube.BarcodedTubeType.EppendorfFliptop20)
                    .build(),
            LibraryType.NONE_ASSIGNED),
    ALLPREP_FLOWTHROUGH_TO_RNA_SPIN1("AllPrepFlowthroughToRnaSpin1",
            ExpectSourcesEmpty.FALSE, ExpectTargetsEmpty.TRUE, SystemOfRecord.MERCURY, CreateSources.TRUE,
            PlasticToValidate.SOURCE, PipelineTransformation.NONE, ForwardMessage.BSP, VolumeConcUpdate.MERCURY_ONLY,
            new ManualTransferDetails.Builder(MessageType.PLATE_TRANSFER_EVENT,
                    RackOfTubes.RackType.FlipperRackRow24,
                    RackOfTubes.RackType.FlipperRackRow24).
                    targetSection(SBSSection.ROWOF24).
                    sourceSection(SBSSection.ROWOF24)
                    .sourceBarcodedTubeType(BarcodedTube.BarcodedTubeType.SpinColumn)
                    .targetBarcodedTubeType(BarcodedTube.BarcodedTubeType.EppendorfFliptop20)
                    .build(),
            LibraryType.NONE_ASSIGNED),
    ALLPREP_FLOWTHROUGH_TO_RNA_SPIN2("AllPrepFlowthroughToRnaSpin2",
            ExpectSourcesEmpty.FALSE, ExpectTargetsEmpty.TRUE, SystemOfRecord.MERCURY, CreateSources.TRUE,
            PlasticToValidate.SOURCE, PipelineTransformation.NONE, ForwardMessage.NONE, VolumeConcUpdate.MERCURY_ONLY,
            new ManualTransferDetails.Builder(MessageType.PLATE_TRANSFER_EVENT,
                    RackOfTubes.RackType.FlipperRackRow24,
                    RackOfTubes.RackType.FlipperRackRow24).
                    targetSection(SBSSection.ROWOF24).
                    sourceSection(SBSSection.ROWOF24)
                    .sourceBarcodedTubeType(BarcodedTube.BarcodedTubeType.SpinColumn)
                    .targetBarcodedTubeType(BarcodedTube.BarcodedTubeType.EppendorfFliptop20)
                    .build(),
            LibraryType.NONE_ASSIGNED),
    ALLPREP_FLOWTHROUGH_TO_RNA_SPIN3("AllPrepFlowthroughToRnaSpin3",
            ExpectSourcesEmpty.FALSE, ExpectTargetsEmpty.TRUE, SystemOfRecord.MERCURY, CreateSources.TRUE,
            PlasticToValidate.SOURCE, PipelineTransformation.NONE, ForwardMessage.NONE, VolumeConcUpdate.MERCURY_ONLY,
            new ManualTransferDetails.Builder(MessageType.PLATE_TRANSFER_EVENT,
                    RackOfTubes.RackType.FlipperRackRow24,
                    RackOfTubes.RackType.FlipperRackRow24).
                    targetSection(SBSSection.ROWOF24).
                    sourceSection(SBSSection.ROWOF24)
                    .sourceBarcodedTubeType(BarcodedTube.BarcodedTubeType.SpinColumn)
                    .targetBarcodedTubeType(BarcodedTube.BarcodedTubeType.EppendorfFliptop15)
                    .build(),
            LibraryType.NONE_ASSIGNED),
    ALLPREP_RNA_TO_MICRO("AllPrepRnaToMicro",
            ExpectSourcesEmpty.FALSE, ExpectTargetsEmpty.TRUE, SystemOfRecord.MERCURY, CreateSources.TRUE,
            PlasticToValidate.SOURCE, PipelineTransformation.NONE, ForwardMessage.BSP, VolumeConcUpdate.MERCURY_ONLY,
            new ManualTransferDetails.Builder(MessageType.PLATE_TRANSFER_EVENT,
                    RackOfTubes.RackType.FlipperRackRow24,
                    RackOfTubes.RackType.FlipperRackRow24).
                    targetSection(SBSSection.ROWOF24).
                    sourceSection(SBSSection.ROWOF24)
                    .sourceBarcodedTubeType(BarcodedTube.BarcodedTubeType.SpinColumn)
                    .targetBarcodedTubeType(BarcodedTube.BarcodedTubeType.CentriCutieSC_5)
                    .build(),
            LibraryType.NONE_ASSIGNED),
    ALLPREP_EXTRACT_TO_RNA("AllPrepExtractToRna",
            ExpectSourcesEmpty.FALSE, ExpectTargetsEmpty.TRUE, SystemOfRecord.MERCURY, CreateSources.TRUE,
            PlasticToValidate.SOURCE, PipelineTransformation.NONE, ForwardMessage.BSP, VolumeConcUpdate.MERCURY_ONLY,
            new ManualTransferDetails.Builder(MessageType.PLATE_TRANSFER_EVENT,
                    RackOfTubes.RackType.FlipperRackRow24,
                    RackOfTubes.RackType.Matrix96SlotRack075).
                    targetSection(SBSSection.ROWOF24).
                    sourceSection(SBSSection.P96_ROWSAB)
                    .sourceBarcodedTubeType(BarcodedTube.BarcodedTubeType.CentriCutieSC_5)
                    .targetBarcodedTubeType(BarcodedTube.BarcodedTubeType.MatrixTube075)
                    .build(),
            MaterialType.RNA_TOTAL_RNA, LibraryType.NONE_ASSIGNED),
    ALLPREP_DNA_TO_MICRO("AllPrepDnaToMicro",
            ExpectSourcesEmpty.FALSE, ExpectTargetsEmpty.TRUE, SystemOfRecord.MERCURY, CreateSources.TRUE,
            PlasticToValidate.SOURCE, PipelineTransformation.NONE, ForwardMessage.BSP, VolumeConcUpdate.MERCURY_ONLY,
            new ManualTransferDetails.Builder(MessageType.PLATE_TRANSFER_EVENT,
                    RackOfTubes.RackType.FlipperRackRow24,
                    RackOfTubes.RackType.FlipperRackRow24).
                    targetSection(SBSSection.ROWOF24).
                    sourceSection(SBSSection.ROWOF24)
                    .sourceBarcodedTubeType(BarcodedTube.BarcodedTubeType.SpinColumn)
                    .targetBarcodedTubeType(BarcodedTube.BarcodedTubeType.CentriCutieSC_5)
                    .build(),
            LibraryType.NONE_ASSIGNED),
    ALLPREP_EXTRACT_TO_DNA("AllPrepExtractToDna",
            ExpectSourcesEmpty.FALSE, ExpectTargetsEmpty.TRUE, SystemOfRecord.MERCURY, CreateSources.TRUE,
            PlasticToValidate.SOURCE, PipelineTransformation.NONE, ForwardMessage.BSP, VolumeConcUpdate.MERCURY_ONLY,
            new ManualTransferDetails.Builder(MessageType.PLATE_TRANSFER_EVENT,
                    RackOfTubes.RackType.FlipperRackRow24,
                    RackOfTubes.RackType.Matrix96SlotRack075).
                    targetSection(SBSSection.ROWOF24).
                    sourceSection(SBSSection.P96_ROWSAB)
                    .sourceBarcodedTubeType(BarcodedTube.BarcodedTubeType.CentriCutieSC_5)
                    .targetBarcodedTubeType(BarcodedTube.BarcodedTubeType.MatrixTube075)
                    .build(),
            MaterialType.DNA_DNA_GENOMIC, LibraryType.NONE_ASSIGNED),


    // TruSeq Custom Amplicon
    TSCA_POST_NORM_TRANSFER("TSCAPostNormTransfer",
            ExpectSourcesEmpty.FALSE, ExpectTargetsEmpty.TRUE, SystemOfRecord.WORKFLOW_DEPENDENT, CreateSources.FALSE,
            PlasticToValidate.SOURCE, PipelineTransformation.NONE, ForwardMessage.NONE, VolumeConcUpdate.MERCURY_ONLY,
            LibraryType.NONE_ASSIGNED),
    TSCA_HYB_SET_UP("TSCAHybSetUp",
            ExpectSourcesEmpty.FALSE, ExpectTargetsEmpty.TRUE, SystemOfRecord.WORKFLOW_DEPENDENT, CreateSources.FALSE,
            PlasticToValidate.SOURCE, PipelineTransformation.NONE, ForwardMessage.NONE, VolumeConcUpdate.MERCURY_ONLY,
            LibraryType.NONE_ASSIGNED),
    TSCA_CAT_ADDITION("TSCACATAddition",
            ExpectSourcesEmpty.FALSE, ExpectTargetsEmpty.TRUE, SystemOfRecord.WORKFLOW_DEPENDENT, CreateSources.FALSE,
            PlasticToValidate.SOURCE, PipelineTransformation.NONE, ForwardMessage.NONE, VolumeConcUpdate.MERCURY_ONLY,
            LibraryType.NONE_ASSIGNED),
    TSCA_HYBRIDIZATION_CLEANUP("TSCAHybridizationCleanUp",
            ExpectSourcesEmpty.FALSE, ExpectTargetsEmpty.TRUE, SystemOfRecord.WORKFLOW_DEPENDENT, CreateSources.FALSE,
            PlasticToValidate.SOURCE, PipelineTransformation.NONE, ForwardMessage.NONE, VolumeConcUpdate.MERCURY_ONLY,
            LibraryType.NONE_ASSIGNED),
    TSCA_FLP_PREP("TSCAFLPPrep",
            ExpectSourcesEmpty.FALSE, ExpectTargetsEmpty.TRUE, SystemOfRecord.WORKFLOW_DEPENDENT, CreateSources.FALSE,
            PlasticToValidate.SOURCE, PipelineTransformation.NONE, ForwardMessage.NONE, VolumeConcUpdate.MERCURY_ONLY,
            LibraryType.NONE_ASSIGNED),
    TSCA_SW1_ADDITION1("TSCASW1Addition1",
            ExpectSourcesEmpty.FALSE, ExpectTargetsEmpty.TRUE, SystemOfRecord.WORKFLOW_DEPENDENT, CreateSources.FALSE,
            PlasticToValidate.SOURCE, PipelineTransformation.NONE, ForwardMessage.NONE, VolumeConcUpdate.MERCURY_ONLY,
            LibraryType.NONE_ASSIGNED),
    TSCA_SW1_ADDITION2("TSCASW1Addition2",
            ExpectSourcesEmpty.FALSE, ExpectTargetsEmpty.TRUE, SystemOfRecord.WORKFLOW_DEPENDENT, CreateSources.FALSE,
            PlasticToValidate.SOURCE, PipelineTransformation.NONE, ForwardMessage.NONE, VolumeConcUpdate.MERCURY_ONLY,
            LibraryType.NONE_ASSIGNED),
    TSCA_UB1_ADDITION("TSCAUB1Addition",
            ExpectSourcesEmpty.FALSE, ExpectTargetsEmpty.TRUE, SystemOfRecord.WORKFLOW_DEPENDENT, CreateSources.FALSE,
            PlasticToValidate.SOURCE, PipelineTransformation.NONE, ForwardMessage.NONE, VolumeConcUpdate.MERCURY_ONLY,
            LibraryType.NONE_ASSIGNED),
    TSCA_EXTENSION_LIGATION_SETUP("TSCAExtensionLigationSetup",
            ExpectSourcesEmpty.FALSE, ExpectTargetsEmpty.TRUE, SystemOfRecord.WORKFLOW_DEPENDENT, CreateSources.FALSE,
            PlasticToValidate.SOURCE, PipelineTransformation.NONE, ForwardMessage.NONE, VolumeConcUpdate.MERCURY_ONLY,
            LibraryType.NONE_ASSIGNED),
    TSCA_INDEXED_AMPLICON_PCR("TSCAIndexedAmpliconPCR",
            ExpectSourcesEmpty.FALSE, ExpectTargetsEmpty.TRUE, SystemOfRecord.WORKFLOW_DEPENDENT, CreateSources.FALSE,
            PlasticToValidate.SOURCE, PipelineTransformation.NONE, ForwardMessage.NONE, VolumeConcUpdate.MERCURY_ONLY,
            LibraryType.NONE_ASSIGNED),
    TSCA_INDEX_REGISTRATION("TSCAIndexRegistration",
            ExpectSourcesEmpty.FALSE, ExpectTargetsEmpty.TRUE, SystemOfRecord.MERCURY, CreateSources.FALSE,
            PlasticToValidate.SOURCE, PipelineTransformation.NONE, ForwardMessage.NONE, VolumeConcUpdate.MERCURY_ONLY,
            LibraryType.NONE_ASSIGNED),
    TSCA_INDEX_ADDITION("TSCAIndexAddition",
            ExpectSourcesEmpty.FALSE, ExpectTargetsEmpty.TRUE, SystemOfRecord.WORKFLOW_DEPENDENT, CreateSources.FALSE,
            PlasticToValidate.SOURCE, PipelineTransformation.LIGATION, ForwardMessage.NONE, VolumeConcUpdate.MERCURY_ONLY,
            LibraryType.NONE_ASSIGNED),
    TSCA_AMPLICON_REGISTRATION("TSCAAmpliconRegistration",
            ExpectSourcesEmpty.FALSE, ExpectTargetsEmpty.TRUE, SystemOfRecord.WORKFLOW_DEPENDENT, CreateSources.FALSE,
            PlasticToValidate.SOURCE, PipelineTransformation.NONE, ForwardMessage.NONE, VolumeConcUpdate.MERCURY_ONLY,
            LibraryType.NONE_ASSIGNED),
    TSCA_AMPLICON_PCR_CLEANUP("TSCAAmpliconPCRCleanup",
            ExpectSourcesEmpty.FALSE, ExpectTargetsEmpty.TRUE, SystemOfRecord.WORKFLOW_DEPENDENT, CreateSources.FALSE,
            PlasticToValidate.SOURCE, PipelineTransformation.NONE, ForwardMessage.NONE, VolumeConcUpdate.MERCURY_ONLY,
            LibraryType.NONE_ASSIGNED),
    TSCA_POOL_CREATION("TSCAPoolCreation",
            ExpectSourcesEmpty.FALSE, ExpectTargetsEmpty.TRUE, SystemOfRecord.WORKFLOW_DEPENDENT, CreateSources.FALSE,
            PlasticToValidate.SOURCE, PipelineTransformation.NONE, ForwardMessage.NONE, VolumeConcUpdate.MERCURY_ONLY,
            LibraryType.NONE_ASSIGNED),

    // 16S
    ILLUMINA_16S_TO_PRIMER_TRANSFER("Illumina16StoPrimerTransfer",
            ExpectSourcesEmpty.FALSE, ExpectTargetsEmpty.TRUE, SystemOfRecord.WORKFLOW_DEPENDENT, CreateSources.FALSE,
            PlasticToValidate.SOURCE, PipelineTransformation.LIGATION, ForwardMessage.NONE, VolumeConcUpdate.MERCURY_ONLY,
            LibraryType.NONE_ASSIGNED),
    ILLUMINA_16S_PCR_PRODUCT_TRANSFER("Illumina16SPcrProductTransfer",
            ExpectSourcesEmpty.FALSE, ExpectTargetsEmpty.TRUE, SystemOfRecord.WORKFLOW_DEPENDENT, CreateSources.FALSE,
            PlasticToValidate.SOURCE, PipelineTransformation.NONE, ForwardMessage.NONE, VolumeConcUpdate.MERCURY_ONLY,
            LibraryType.NONE_ASSIGNED),

    // 16S V2
    ILLUMINA_16S_PCR_SETUP("16SPCRSetup",
            ExpectSourcesEmpty.FALSE, ExpectTargetsEmpty.TRUE, SystemOfRecord.WORKFLOW_DEPENDENT, CreateSources.FALSE,
            PlasticToValidate.SOURCE, PipelineTransformation.PCR, ForwardMessage.NONE, VolumeConcUpdate.MERCURY_ONLY,
            LibraryType.NONE_ASSIGNED),
    ILLUMINA_16S_PRIMER_ADDITION("16SPrimerAddition",
            ExpectSourcesEmpty.FALSE, ExpectTargetsEmpty.TRUE, SystemOfRecord.WORKFLOW_DEPENDENT, CreateSources.FALSE,
            PlasticToValidate.TARGET, PipelineTransformation.LIGATION, ForwardMessage.NONE, VolumeConcUpdate.MERCURY_ONLY,
            LibraryType.NONE_ASSIGNED),
    ILLUMINA_16S_CALIPER_SETUP("16SCaliperSetup",
            ExpectSourcesEmpty.FALSE, ExpectTargetsEmpty.TRUE, SystemOfRecord.WORKFLOW_DEPENDENT, CreateSources.FALSE,
            PlasticToValidate.SOURCE, PipelineTransformation.NONE, ForwardMessage.NONE, VolumeConcUpdate.MERCURY_ONLY,
            LibraryType.NONE_ASSIGNED),
    // Illumina16SPcrProductTransfer
    ILLUMINA_16S_POOLING_TRANSFER("16SPoolingTransfer",
            ExpectSourcesEmpty.FALSE, ExpectTargetsEmpty.TRUE, SystemOfRecord.WORKFLOW_DEPENDENT, CreateSources.FALSE,
            PlasticToValidate.SOURCE, PipelineTransformation.NONE, ForwardMessage.NONE, VolumeConcUpdate.MERCURY_ONLY,
            LibraryType.NONE_ASSIGNED),
    ILLUMINA_16S_BULK_SPRI("16SBulkSPRI",
            ExpectSourcesEmpty.FALSE, ExpectTargetsEmpty.TRUE, SystemOfRecord.WORKFLOW_DEPENDENT, CreateSources.FALSE,
            PlasticToValidate.SOURCE, PipelineTransformation.NONE, ForwardMessage.NONE, VolumeConcUpdate.MERCURY_ONLY,
            LibraryType.NONE_ASSIGNED),
    ILLUMINA_16S_COLUMN_CLEANUP("16SColumnCleanup",
            ExpectSourcesEmpty.FALSE, ExpectTargetsEmpty.TRUE, SystemOfRecord.MERCURY, CreateSources.FALSE,
            PlasticToValidate.SOURCE, PipelineTransformation.NONE, ForwardMessage.NONE, VolumeConcUpdate.MERCURY_ONLY,
            new ManualTransferDetails.Builder(MessageType.RECEPTACLE_TRANSFER_EVENT,
                    BarcodedTube.BarcodedTubeType.MatrixTubeSC14,
                    BarcodedTube.BarcodedTubeType.MatrixTubeSC05,
                    new ReagentRequirements[]{new ReagentRequirements("Proteinase K"),
                            new ReagentRequirements("Buffer AL"),
                            new ReagentRequirements("100% Ethanol")})
                    .build(),
            LibraryType.NONE_ASSIGNED),

    //Globin Depletion
    GLOBIN_BINDING("GlobinBinding",
            ExpectSourcesEmpty.FALSE, ExpectTargetsEmpty.TRUE, SystemOfRecord.WORKFLOW_DEPENDENT, CreateSources.FALSE,
            PlasticToValidate.SOURCE, PipelineTransformation.NONE, ForwardMessage.BSP, VolumeConcUpdate.MERCURY_ONLY,
            LibraryType.NONE_ASSIGNED),
    GLOBIN_DEPLETION("GlobinDepletion",
            ExpectSourcesEmpty.FALSE, ExpectTargetsEmpty.TRUE, SystemOfRecord.WORKFLOW_DEPENDENT, CreateSources.FALSE,
            PlasticToValidate.SOURCE, PipelineTransformation.NONE, ForwardMessage.BSP, VolumeConcUpdate.MERCURY_ONLY,
            LibraryType.NONE_ASSIGNED),

    //Ribo Zero
    RIBO_ZERO_TRANSFER("RiboZeroTransfer",
            ExpectSourcesEmpty.FALSE, ExpectTargetsEmpty.TRUE, SystemOfRecord.WORKFLOW_DEPENDENT, CreateSources.FALSE,
            PlasticToValidate.SOURCE, PipelineTransformation.NONE, ForwardMessage.NONE, VolumeConcUpdate.MERCURY_ONLY,
            LibraryType.NONE_ASSIGNED),
    RIBO_ZERO_DEPLETION("RiboZeroDepletion",
            ExpectSourcesEmpty.FALSE, ExpectTargetsEmpty.TRUE, SystemOfRecord.WORKFLOW_DEPENDENT, CreateSources.FALSE,
            PlasticToValidate.SOURCE, PipelineTransformation.NONE, ForwardMessage.NONE, VolumeConcUpdate.MERCURY_ONLY,
            LibraryType.NONE_ASSIGNED),
    RIBO_ZERO_ADDITIONAL_TRANSFER("RiboZeroAdditionalTransfer",
            ExpectSourcesEmpty.FALSE, ExpectTargetsEmpty.TRUE, SystemOfRecord.WORKFLOW_DEPENDENT, CreateSources.FALSE,
            PlasticToValidate.SOURCE, PipelineTransformation.NONE, ForwardMessage.NONE, VolumeConcUpdate.MERCURY_ONLY,
            LibraryType.NONE_ASSIGNED),
    RIBO_ZERO_CLEANUP("RiboZeroCleanup",
            ExpectSourcesEmpty.FALSE, ExpectTargetsEmpty.TRUE, SystemOfRecord.WORKFLOW_DEPENDENT, CreateSources.FALSE,
            PlasticToValidate.SOURCE, PipelineTransformation.NONE, ForwardMessage.NONE, VolumeConcUpdate.MERCURY_ONLY,
            LibraryType.NONE_ASSIGNED),
    EPF_ADDITION("EPFAddition",
            ExpectSourcesEmpty.FALSE, ExpectTargetsEmpty.TRUE, SystemOfRecord.WORKFLOW_DEPENDENT, CreateSources.FALSE,
            PlasticToValidate.SOURCE, PipelineTransformation.NONE, ForwardMessage.NONE, VolumeConcUpdate.MERCURY_ONLY,
            LibraryType.NONE_ASSIGNED),

    // cDNA dUTP
    POLY_A_TRANSFER("PolyATransfer",
            ExpectSourcesEmpty.FALSE, ExpectTargetsEmpty.TRUE, SystemOfRecord.WORKFLOW_DEPENDENT, CreateSources.FALSE,
            PlasticToValidate.SOURCE, PipelineTransformation.NONE, ForwardMessage.NONE, VolumeConcUpdate.MERCURY_ONLY,
            LibraryType.NONE_ASSIGNED),
    POLY_A_SELECTION("PolyASelection",
            ExpectSourcesEmpty.FALSE, ExpectTargetsEmpty.TRUE, SystemOfRecord.WORKFLOW_DEPENDENT, CreateSources.FALSE,
            PlasticToValidate.SOURCE, PipelineTransformation.NONE, ForwardMessage.NONE, VolumeConcUpdate.MERCURY_ONLY,
            LibraryType.NONE_ASSIGNED),
    ERCC_SPIKE_IN("ERCCSpikeIn",
            ExpectSourcesEmpty.FALSE, ExpectTargetsEmpty.TRUE, SystemOfRecord.WORKFLOW_DEPENDENT, CreateSources.FALSE,
            PlasticToValidate.SOURCE, PipelineTransformation.NONE, ForwardMessage.NONE, VolumeConcUpdate.MERCURY_ONLY,
            LibraryType.NONE_ASSIGNED),
    DNASE("Dnase",
            ExpectSourcesEmpty.FALSE, ExpectTargetsEmpty.TRUE, SystemOfRecord.WORKFLOW_DEPENDENT, CreateSources.FALSE,
            PlasticToValidate.SOURCE, PipelineTransformation.NONE, ForwardMessage.NONE, VolumeConcUpdate.MERCURY_ONLY,
            LibraryType.NONE_ASSIGNED),
    DNASE_CLEANUP("DnaseCleanup",
            ExpectSourcesEmpty.FALSE, ExpectTargetsEmpty.TRUE, SystemOfRecord.WORKFLOW_DEPENDENT, CreateSources.FALSE,
            PlasticToValidate.SOURCE, PipelineTransformation.NONE, ForwardMessage.NONE, VolumeConcUpdate.MERCURY_ONLY,
            LibraryType.NONE_ASSIGNED),
    FRAGMENTATION("Fragmentation",
            ExpectSourcesEmpty.FALSE, ExpectTargetsEmpty.TRUE, SystemOfRecord.WORKFLOW_DEPENDENT, CreateSources.FALSE,
            PlasticToValidate.SOURCE, PipelineTransformation.NONE, ForwardMessage.NONE, VolumeConcUpdate.MERCURY_ONLY,
            LibraryType.NONE_ASSIGNED),
    FRAGMENTATION_CLEANUP("FragmentationCleanup",
            ExpectSourcesEmpty.FALSE, ExpectTargetsEmpty.TRUE, SystemOfRecord.WORKFLOW_DEPENDENT, CreateSources.FALSE,
            PlasticToValidate.SOURCE, PipelineTransformation.NONE, ForwardMessage.NONE, VolumeConcUpdate.MERCURY_ONLY,
            LibraryType.NONE_ASSIGNED),
    FIRST_STRAND("FirstStrand",
            ExpectSourcesEmpty.FALSE, ExpectTargetsEmpty.TRUE, SystemOfRecord.WORKFLOW_DEPENDENT, CreateSources.FALSE,
            PlasticToValidate.SOURCE, PipelineTransformation.NONE, ForwardMessage.NONE, VolumeConcUpdate.MERCURY_ONLY,
            LibraryType.NONE_ASSIGNED),
    FIRST_STRAND_CLEANUP("FirstStrandCleanup",
            ExpectSourcesEmpty.FALSE, ExpectTargetsEmpty.TRUE, SystemOfRecord.WORKFLOW_DEPENDENT, CreateSources.FALSE,
            PlasticToValidate.SOURCE, PipelineTransformation.NONE, ForwardMessage.NONE, VolumeConcUpdate.MERCURY_ONLY,
            LibraryType.NONE_ASSIGNED),
    SECOND_STRAND("SecondStrand",
            ExpectSourcesEmpty.FALSE, ExpectTargetsEmpty.TRUE, SystemOfRecord.WORKFLOW_DEPENDENT, CreateSources.FALSE,
            PlasticToValidate.SOURCE, PipelineTransformation.NONE, ForwardMessage.NONE, VolumeConcUpdate.MERCURY_ONLY,
            LibraryType.NONE_ASSIGNED),
    SECOND_STRAND_CLEANUP("SecondStrandCleanup",
            ExpectSourcesEmpty.FALSE, ExpectTargetsEmpty.TRUE, SystemOfRecord.WORKFLOW_DEPENDENT, CreateSources.FALSE,
            PlasticToValidate.SOURCE, PipelineTransformation.NONE, ForwardMessage.NONE, VolumeConcUpdate.MERCURY_ONLY,
            LibraryType.NONE_ASSIGNED),
    USER("USER",
            ExpectSourcesEmpty.FALSE, ExpectTargetsEmpty.TRUE, SystemOfRecord.WORKFLOW_DEPENDENT, CreateSources.FALSE,
            PlasticToValidate.SOURCE, PipelineTransformation.NONE, ForwardMessage.NONE, VolumeConcUpdate.MERCURY_ONLY,
            LibraryType.NONE_ASSIGNED),
    CDNA_PONDENRICHMENT_CLEANUP("cDNAPondEnrichmentCleanup",
            ExpectSourcesEmpty.FALSE, ExpectTargetsEmpty.TRUE, SystemOfRecord.WORKFLOW_DEPENDENT, CreateSources.FALSE,
            PlasticToValidate.SOURCE, PipelineTransformation.PCR, ForwardMessage.NONE, VolumeConcUpdate.MERCURY_ONLY,
            LibraryType.NONE_ASSIGNED),

    // cDNA TruSeq
    TRU_SEQ_STRAND_SPECIFIC_BUCKET("TruSeqStrandSpecificBucket",
            ExpectSourcesEmpty.TRUE, ExpectTargetsEmpty.TRUE, SystemOfRecord.WORKFLOW_DEPENDENT, CreateSources.FALSE,
            PlasticToValidate.SOURCE, PipelineTransformation.NONE, ForwardMessage.NONE, VolumeConcUpdate.MERCURY_ONLY,
            LibraryType.NONE_ASSIGNED),
    POLY_A_SELECTION_TS("PolyASelectionTS",
            ExpectSourcesEmpty.FALSE, ExpectTargetsEmpty.TRUE, SystemOfRecord.WORKFLOW_DEPENDENT, CreateSources.FALSE,
            PlasticToValidate.SOURCE, PipelineTransformation.NONE, ForwardMessage.NONE, VolumeConcUpdate.MERCURY_ONLY,
            LibraryType.NONE_ASSIGNED),
    POLY_A_BINDING_THERMO_CYCLER_LOADED("PolyABindingTSThermoCyclerLoaded",
            ExpectSourcesEmpty.FALSE, ExpectTargetsEmpty.TRUE, SystemOfRecord.WORKFLOW_DEPENDENT, CreateSources.FALSE,
            PlasticToValidate.SOURCE, PipelineTransformation.NONE, ForwardMessage.NONE, VolumeConcUpdate.MERCURY_ONLY,
            LibraryType.NONE_ASSIGNED),
    MRNA_ELUTION_TS_THERMO_CYCLER_LOADED("MRNAElutionTSThermoCyclerLoaded",
            ExpectSourcesEmpty.FALSE, ExpectTargetsEmpty.TRUE, SystemOfRecord.WORKFLOW_DEPENDENT, CreateSources.FALSE,
            PlasticToValidate.SOURCE, PipelineTransformation.NONE, ForwardMessage.NONE, VolumeConcUpdate.MERCURY_ONLY,
            LibraryType.NONE_ASSIGNED),
    FRAGMENTATION_TS_THERMO_CYCLER_LOADED("FragmentationTSThermoCyclerLoaded",
            ExpectSourcesEmpty.FALSE, ExpectTargetsEmpty.TRUE, SystemOfRecord.WORKFLOW_DEPENDENT, CreateSources.FALSE,
            PlasticToValidate.SOURCE, PipelineTransformation.NONE, ForwardMessage.NONE, VolumeConcUpdate.MERCURY_ONLY,
            LibraryType.NONE_ASSIGNED),
    FIRST_STRAND_TS("FirstStrandTS",
            ExpectSourcesEmpty.FALSE, ExpectTargetsEmpty.TRUE, SystemOfRecord.WORKFLOW_DEPENDENT, CreateSources.FALSE,
            PlasticToValidate.SOURCE, PipelineTransformation.NONE, ForwardMessage.NONE, VolumeConcUpdate.MERCURY_ONLY,
            LibraryType.NONE_ASSIGNED),
    FIRST_STRAND_TS_THERMO_CYCLER_LOADED("FirstStrandTSThermoCyclerLoaded",
            ExpectSourcesEmpty.FALSE, ExpectTargetsEmpty.TRUE, SystemOfRecord.WORKFLOW_DEPENDENT, CreateSources.FALSE,
            PlasticToValidate.SOURCE, PipelineTransformation.NONE, ForwardMessage.NONE, VolumeConcUpdate.MERCURY_ONLY,
            LibraryType.NONE_ASSIGNED),
    SECOND_STRAND_TS("SecondStrandTS",
            ExpectSourcesEmpty.FALSE, ExpectTargetsEmpty.TRUE, SystemOfRecord.WORKFLOW_DEPENDENT, CreateSources.FALSE,
            PlasticToValidate.SOURCE, PipelineTransformation.NONE, ForwardMessage.NONE, VolumeConcUpdate.MERCURY_ONLY,
            LibraryType.NONE_ASSIGNED),
    SECOND_STRAND_TS_THERMO_CYCLER_LOADED("SecondStrandTSThermoCyclerLoaded",
            ExpectSourcesEmpty.FALSE, ExpectTargetsEmpty.TRUE, SystemOfRecord.WORKFLOW_DEPENDENT, CreateSources.FALSE,
            PlasticToValidate.SOURCE, PipelineTransformation.NONE, ForwardMessage.NONE, VolumeConcUpdate.MERCURY_ONLY,
            LibraryType.NONE_ASSIGNED),
    SECOND_STRAND_CLEANUP_TS("SecondStrandCleanupTS",
            ExpectSourcesEmpty.FALSE, ExpectTargetsEmpty.TRUE, SystemOfRecord.WORKFLOW_DEPENDENT, CreateSources.FALSE,
            PlasticToValidate.SOURCE, PipelineTransformation.NONE, ForwardMessage.NONE, VolumeConcUpdate.MERCURY_ONLY,
            LibraryType.NONE_ASSIGNED),
    END_REPAIR_TS("EndRepairTS",
            ExpectSourcesEmpty.FALSE, ExpectTargetsEmpty.TRUE, SystemOfRecord.WORKFLOW_DEPENDENT, CreateSources.FALSE,
            PlasticToValidate.SOURCE, PipelineTransformation.NONE, ForwardMessage.NONE, VolumeConcUpdate.MERCURY_ONLY,
            LibraryType.NONE_ASSIGNED),
    END_REPAIR_CLEANUP_TS("EndRepairCleanupTS",
            ExpectSourcesEmpty.FALSE, ExpectTargetsEmpty.TRUE, SystemOfRecord.WORKFLOW_DEPENDENT, CreateSources.FALSE,
            PlasticToValidate.SOURCE, PipelineTransformation.NONE, ForwardMessage.NONE, VolumeConcUpdate.MERCURY_ONLY,
            LibraryType.NONE_ASSIGNED),
    A_BASE_TS("ABaseTS",
            ExpectSourcesEmpty.FALSE, ExpectTargetsEmpty.TRUE, SystemOfRecord.WORKFLOW_DEPENDENT, CreateSources.FALSE,
            PlasticToValidate.SOURCE, PipelineTransformation.NONE, ForwardMessage.NONE, VolumeConcUpdate.MERCURY_ONLY,
            LibraryType.NONE_ASSIGNED),
    A_BASE_TS_THERMO_CYCLER_LOADED("ABaseTSThermoCyclerLoaded",
            ExpectSourcesEmpty.FALSE, ExpectTargetsEmpty.TRUE, SystemOfRecord.WORKFLOW_DEPENDENT, CreateSources.FALSE,
            PlasticToValidate.SOURCE, PipelineTransformation.NONE, ForwardMessage.NONE, VolumeConcUpdate.MERCURY_ONLY,
            LibraryType.NONE_ASSIGNED),
    INDEXED_ADAPTER_LIGATION_TS("IndexedAdapterLigationTS",
            ExpectSourcesEmpty.FALSE, ExpectTargetsEmpty.TRUE, SystemOfRecord.WORKFLOW_DEPENDENT, CreateSources.FALSE,
            PlasticToValidate.TARGET, PipelineTransformation.LIGATION, ForwardMessage.NONE, VolumeConcUpdate.MERCURY_ONLY,
            LibraryType.NONE_ASSIGNED),
    ADAPTER_LIGATION_TS_THERMO_CYCLER_LOADED("AdapterLigationTSThermoCyclerLoaded",
            ExpectSourcesEmpty.FALSE, ExpectTargetsEmpty.TRUE, SystemOfRecord.WORKFLOW_DEPENDENT, CreateSources.FALSE,
            PlasticToValidate.SOURCE, PipelineTransformation.NONE, ForwardMessage.NONE, VolumeConcUpdate.MERCURY_ONLY,
            LibraryType.NONE_ASSIGNED),
    ADAPTER_LIGATION_CLEANUP_TS("AdapterLigationCleanupTS",
            ExpectSourcesEmpty.FALSE, ExpectTargetsEmpty.TRUE, SystemOfRecord.WORKFLOW_DEPENDENT, CreateSources.FALSE,
            PlasticToValidate.SOURCE, PipelineTransformation.NONE, ForwardMessage.NONE, VolumeConcUpdate.MERCURY_ONLY,
            LibraryType.NONE_ASSIGNED),
    INDEX_P5_POND_ENRICHMENT_TS("IndexP5PondEnrichmentTS",
            ExpectSourcesEmpty.TRUE, ExpectTargetsEmpty.TRUE, SystemOfRecord.WORKFLOW_DEPENDENT, CreateSources.FALSE,
            PlasticToValidate.TARGET, PipelineTransformation.NONE, ForwardMessage.NONE, VolumeConcUpdate.MERCURY_ONLY,
            LibraryType.NONE_ASSIGNED),
    // The PCR is associated with EnrichmentCleanupTS, because we want a tube barcode for the pipeline
    ENRICHMENT_TS("EnrichmentTS",
            ExpectSourcesEmpty.FALSE, ExpectTargetsEmpty.TRUE, SystemOfRecord.WORKFLOW_DEPENDENT, CreateSources.FALSE,
            PlasticToValidate.SOURCE, PipelineTransformation.NONE, ForwardMessage.NONE, VolumeConcUpdate.MERCURY_ONLY,
            LibraryType.NONE_ASSIGNED),
    ENRICHMENT_TS_THERMO_CYCLER_LOADED("EnrichmentTSThermoCyclerLoaded",
            ExpectSourcesEmpty.FALSE, ExpectTargetsEmpty.TRUE, SystemOfRecord.WORKFLOW_DEPENDENT, CreateSources.FALSE,
            PlasticToValidate.SOURCE, PipelineTransformation.NONE, ForwardMessage.NONE, VolumeConcUpdate.MERCURY_ONLY,
            LibraryType.NONE_ASSIGNED),
    ENRICHMENT_CLEANUP_TS("EnrichmentCleanupTS",
            ExpectSourcesEmpty.FALSE, ExpectTargetsEmpty.TRUE, SystemOfRecord.WORKFLOW_DEPENDENT, CreateSources.FALSE,
            PlasticToValidate.SOURCE, PipelineTransformation.PCR, ForwardMessage.NONE, VolumeConcUpdate.MERCURY_ONLY,
            LibraryType.ENRICHED_POND),

    // Illumina Genome Network
    IGN_PRE_NORM_TRANSFER("IGNPreNormTransfer",
            ExpectSourcesEmpty.FALSE, ExpectTargetsEmpty.TRUE, SystemOfRecord.WORKFLOW_DEPENDENT, CreateSources.FALSE,
            PlasticToValidate.SOURCE, PipelineTransformation.NONE, ForwardMessage.NONE, VolumeConcUpdate.MERCURY_ONLY,
            LibraryType.NONE_ASSIGNED),
    IGN_NORM_TRANSFER("IGNNormTransfer",
            ExpectSourcesEmpty.FALSE, ExpectTargetsEmpty.TRUE, SystemOfRecord.WORKFLOW_DEPENDENT, CreateSources.FALSE,
            PlasticToValidate.SOURCE, PipelineTransformation.NONE, ForwardMessage.NONE, VolumeConcUpdate.MERCURY_ONLY,
            LibraryType.NONE_ASSIGNED),
    IGN_INCOMING_PICO("IGNIncomingPico",
            ExpectSourcesEmpty.FALSE, ExpectTargetsEmpty.TRUE, SystemOfRecord.WORKFLOW_DEPENDENT, CreateSources.FALSE,
            PlasticToValidate.SOURCE, PipelineTransformation.NONE, ForwardMessage.NONE, VolumeConcUpdate.MERCURY_ONLY,
            LibraryType.NONE_ASSIGNED),
    IGN_GAP_ALIQUOT("IGNGapAliquot",
            ExpectSourcesEmpty.FALSE, ExpectTargetsEmpty.TRUE, SystemOfRecord.WORKFLOW_DEPENDENT, CreateSources.FALSE,
            PlasticToValidate.SOURCE, PipelineTransformation.NONE, ForwardMessage.NONE, VolumeConcUpdate.MERCURY_ONLY,
            LibraryType.NONE_ASSIGNED),

    VIRAL_PCR_PRODUCT_POOL_TRANSFER("ViralPcrProductPoolTransfer",
            ExpectSourcesEmpty.FALSE, ExpectTargetsEmpty.TRUE, SystemOfRecord.WORKFLOW_DEPENDENT, CreateSources.FALSE,
            PlasticToValidate.SOURCE, PipelineTransformation.NONE, ForwardMessage.NONE, VolumeConcUpdate.MERCURY_ONLY,
            LibraryType.NONE_ASSIGNED),
    ONE_STEP_RT_PCR_PRODUCT_POOL_TRANSFER("OneStepRtPcrProductPoolTransfer",
            ExpectSourcesEmpty.FALSE, ExpectTargetsEmpty.TRUE, SystemOfRecord.WORKFLOW_DEPENDENT, CreateSources.FALSE,
            PlasticToValidate.SOURCE, PipelineTransformation.NONE, ForwardMessage.NONE, VolumeConcUpdate.MERCURY_ONLY,
            LibraryType.NONE_ASSIGNED),

    // From BSP UI
    /**
     * "Packaging" indicates the samples are being placed in a package for shipping.
     */
    SAMPLE_PACKAGE("SamplePackage",
            ExpectSourcesEmpty.FALSE, ExpectTargetsEmpty.TRUE, SystemOfRecord.MERCURY, CreateSources.FALSE,
            PlasticToValidate.SOURCE, PipelineTransformation.NONE, ForwardMessage.NONE, VolumeConcUpdate.MERCURY_ONLY,
            LibraryType.NONE_ASSIGNED),
    INITIAL_TARE("InitialTare",
            ExpectSourcesEmpty.FALSE, ExpectTargetsEmpty.TRUE, SystemOfRecord.MERCURY, CreateSources.FALSE,
            PlasticToValidate.SOURCE, PipelineTransformation.NONE, ForwardMessage.NONE, VolumeConcUpdate.BSP_AND_MERCURY, LibraryType.NONE_ASSIGNED),
    SAMPLE_RECEIPT("SampleReceipt",
            ExpectSourcesEmpty.FALSE, ExpectTargetsEmpty.TRUE, SystemOfRecord.MERCURY, CreateSources.FALSE,
            PlasticToValidate.SOURCE, PipelineTransformation.NONE, ForwardMessage.NONE, VolumeConcUpdate.BSP_AND_MERCURY, LibraryType.NONE_ASSIGNED),
    WEIGHT_MEASUREMENT("WeightMeasurement",
            ExpectSourcesEmpty.FALSE, ExpectTargetsEmpty.TRUE, SystemOfRecord.MERCURY, CreateSources.FALSE,
            PlasticToValidate.SOURCE, PipelineTransformation.NONE, ForwardMessage.NONE, VolumeConcUpdate.BSP_AND_MERCURY, LibraryType.NONE_ASSIGNED),
    VOLUME_MEASUREMENT("VolumeMeasurement",
            ExpectSourcesEmpty.FALSE, ExpectTargetsEmpty.TRUE, SystemOfRecord.MERCURY, CreateSources.FALSE,
            PlasticToValidate.SOURCE, PipelineTransformation.NONE, ForwardMessage.NONE, VolumeConcUpdate.BSP_AND_MERCURY, LibraryType.NONE_ASSIGNED),
    VOLUME_ADDITION("VolumeAddition",
            ExpectSourcesEmpty.FALSE, ExpectTargetsEmpty.TRUE, SystemOfRecord.MERCURY, CreateSources.FALSE,
            PlasticToValidate.SOURCE, PipelineTransformation.NONE, ForwardMessage.NONE, VolumeConcUpdate.BSP_AND_MERCURY,
            LibraryType.NONE_ASSIGNED),
    INITIAL_NORMALIZATION("InitialNormalization",
            ExpectSourcesEmpty.FALSE, ExpectTargetsEmpty.TRUE, SystemOfRecord.MERCURY, CreateSources.FALSE,
            PlasticToValidate.SOURCE, PipelineTransformation.NONE, ForwardMessage.NONE, VolumeConcUpdate.MERCURY_ONLY,
            LibraryType.NONE_ASSIGNED),
    SPIKE_IN("SpikeIn",
            ExpectSourcesEmpty.FALSE, ExpectTargetsEmpty.TRUE, SystemOfRecord.MERCURY, CreateSources.FALSE,
            PlasticToValidate.SOURCE, PipelineTransformation.NONE, ForwardMessage.NONE, VolumeConcUpdate.MERCURY_ONLY,
            LibraryType.NONE_ASSIGNED),
    SAMPLES_EXTRACTION_START("SamplesExtractionStart",
            ExpectSourcesEmpty.FALSE, ExpectTargetsEmpty.TRUE, SystemOfRecord.MERCURY, CreateSources.FALSE,
            PlasticToValidate.SOURCE, PipelineTransformation.NONE, ForwardMessage.NONE, VolumeConcUpdate.MERCURY_ONLY,
            LibraryType.NONE_ASSIGNED),
    SAMPLES_EXTRACTION_END_TRANSFER("SamplesExtractionEndTransfer",
            ExpectSourcesEmpty.FALSE, ExpectTargetsEmpty.TRUE, SystemOfRecord.MERCURY, CreateSources.FALSE,
            PlasticToValidate.SOURCE, PipelineTransformation.NONE, ForwardMessage.NONE, VolumeConcUpdate.MERCURY_ONLY,
            LibraryType.NONE_ASSIGNED),
    SAMPLES_NORMALIZATION_TRANSFER("SamplesNormalizationTransfer",
            ExpectSourcesEmpty.FALSE, ExpectTargetsEmpty.TRUE, SystemOfRecord.MERCURY, CreateSources.FALSE,
            PlasticToValidate.SOURCE, PipelineTransformation.NONE, ForwardMessage.NONE, VolumeConcUpdate.MERCURY_ONLY,
            LibraryType.NONE_ASSIGNED),
    SAMPLES_PLATING_TO_COVARIS("SamplesPlatingToCovaris",
            ExpectSourcesEmpty.FALSE, ExpectTargetsEmpty.TRUE, SystemOfRecord.MERCURY, CreateSources.FALSE,
            PlasticToValidate.SOURCE, PipelineTransformation.NONE, ForwardMessage.NONE, VolumeConcUpdate.MERCURY_ONLY,
            LibraryType.NONE_ASSIGNED),
    SAMPLES_DAUGHTER_PLATE_CREATION("SamplesDaughterPlateCreation",
            ExpectSourcesEmpty.FALSE, ExpectTargetsEmpty.FALSE, SystemOfRecord.MERCURY, CreateSources.TRUE,
            PlasticToValidate.BOTH, PipelineTransformation.NONE, ForwardMessage.NONE, VolumeConcUpdate.MERCURY_ONLY,
            LibraryType.NONE_ASSIGNED),
    AUTO_DAUGHTER_PLATE_CREATION("AutomatedDaughterPlateCreation",
            ExpectSourcesEmpty.FALSE, ExpectTargetsEmpty.FALSE, SystemOfRecord.MERCURY, CreateSources.TRUE,
            PlasticToValidate.BOTH, PipelineTransformation.NONE, ForwardMessage.BSP, VolumeConcUpdate.MERCURY_ONLY,
            LibraryType.NONE_ASSIGNED, TranslateBspMessage.SECTION_TO_CHERRY),
    SONIC_DAUGHTER_PLATE_CREATION("SonicDaughterPlateCreation",
            ExpectSourcesEmpty.FALSE, ExpectTargetsEmpty.TRUE, SystemOfRecord.MERCURY, CreateSources.FALSE,
            PlasticToValidate.SOURCE, PipelineTransformation.NONE, ForwardMessage.NONE, VolumeConcUpdate.MERCURY_ONLY,
            LibraryType.NONE_ASSIGNED),
    SAMPLE_IMPORT("SampleImport",
            ExpectSourcesEmpty.FALSE, ExpectTargetsEmpty.TRUE, SystemOfRecord.MERCURY, CreateSources.FALSE,
            PlasticToValidate.SOURCE, PipelineTransformation.NONE, ForwardMessage.NONE, VolumeConcUpdate.MERCURY_ONLY,
            LibraryType.NONE_ASSIGNED),
    SEQ_PLATING_NORMALIZATION("SeqPlatingNormalization",
            ExpectSourcesEmpty.FALSE, ExpectTargetsEmpty.TRUE, SystemOfRecord.MERCURY, CreateSources.TRUE,
            PlasticToValidate.SOURCE, PipelineTransformation.NONE, ForwardMessage.NONE, VolumeConcUpdate.BSP_AND_MERCURY,
            LibraryType.NONE_ASSIGNED),
    ARRAY_PLATING_DILUTION("ArrayPlatingDilution",
            ExpectSourcesEmpty.FALSE, ExpectTargetsEmpty.TRUE, SystemOfRecord.MERCURY, CreateSources.TRUE,
            PlasticToValidate.SOURCE, PipelineTransformation.NONE, ForwardMessage.BSP, VolumeConcUpdate.MERCURY_ONLY,
            LibraryType.NONE_ASSIGNED, TranslateBspMessage.SECTION_TO_CHERRY),
    SHEARING_ALIQUOT("ShearingAliquot",
            ExpectSourcesEmpty.FALSE, ExpectTargetsEmpty.TRUE, SystemOfRecord.MERCURY, CreateSources.FALSE,
            PlasticToValidate.SOURCE, PipelineTransformation.NONE, ForwardMessage.NONE, VolumeConcUpdate.MERCURY_ONLY,
            LibraryType.NONE_ASSIGNED),
    FINGERPRINTING_ALIQUOT("FingerprintingAliquot",
            ExpectSourcesEmpty.FALSE, ExpectTargetsEmpty.TRUE, SystemOfRecord.MERCURY, CreateSources.FALSE,
            PlasticToValidate.SOURCE, PipelineTransformation.NONE, ForwardMessage.NONE, VolumeConcUpdate.MERCURY_ONLY,
            LibraryType.NONE_ASSIGNED),
    FINGERPRINTING_ALIQUOT_FORWARD_BSP("FingerprintingAliquotForwardBsp",
            ExpectSourcesEmpty.FALSE, ExpectTargetsEmpty.TRUE, SystemOfRecord.MERCURY, CreateSources.FALSE,
            PlasticToValidate.SOURCE, PipelineTransformation.NONE, ForwardMessage.BSP, VolumeConcUpdate.MERCURY_ONLY,
            LibraryType.NONE_ASSIGNED, TranslateBspMessage.SECTION_TO_CHERRY),
    FINGERPRINTING_PLATE_SETUP("FingerprintingPlateSetup",
            ExpectSourcesEmpty.FALSE, ExpectTargetsEmpty.TRUE, SystemOfRecord.MERCURY, CreateSources.FALSE,
            PlasticToValidate.SOURCE, PipelineTransformation.NONE, ForwardMessage.NONE, VolumeConcUpdate.MERCURY_ONLY,
            LibraryType.NONE_ASSIGNED),
    FINGERPRINTING_PLATE_SETUP_FORWARD_BSP("FingerprintingPlateSetupForwardBsp",
            ExpectSourcesEmpty.FALSE, ExpectTargetsEmpty.TRUE, SystemOfRecord.MERCURY, CreateSources.FALSE,
            PlasticToValidate.SOURCE, PipelineTransformation.NONE, ForwardMessage.BSP, VolumeConcUpdate.MERCURY_ONLY,
            LibraryType.NONE_ASSIGNED, AddMetadataToBsp.PDO, TranslateBspMessage.SECTION_TO_CHERRY),
    EMERGE_VOLUME_TRANSFER("EmergeVolumeTransfer",
            ExpectSourcesEmpty.FALSE, ExpectTargetsEmpty.TRUE, SystemOfRecord.MERCURY, CreateSources.FALSE,
            PlasticToValidate.SOURCE, PipelineTransformation.NONE, ForwardMessage.NONE, VolumeConcUpdate.MERCURY_ONLY,
            LibraryType.NONE_ASSIGNED),

    // mRRBS
    MRRBS_GENOMIC_TRANSFER("mRRBSGenomicTransfer",
            ExpectSourcesEmpty.FALSE, ExpectTargetsEmpty.TRUE, SystemOfRecord.WORKFLOW_DEPENDENT, CreateSources.FALSE,
            PlasticToValidate.SOURCE, PipelineTransformation.NONE, ForwardMessage.NONE, VolumeConcUpdate.MERCURY_ONLY,
            LibraryType.NONE_ASSIGNED),
    MRRBS_INDEXING("mRRBSIndexing",
            ExpectSourcesEmpty.FALSE, ExpectTargetsEmpty.TRUE, SystemOfRecord.WORKFLOW_DEPENDENT, CreateSources.FALSE,
            PlasticToValidate.SOURCE, PipelineTransformation.LIGATION, ForwardMessage.NONE, VolumeConcUpdate.MERCURY_ONLY,
            LibraryType.NONE_ASSIGNED),
    MRRBS_NORM_LIBS("mRRBSNormLibs",
            ExpectSourcesEmpty.FALSE, ExpectTargetsEmpty.TRUE, SystemOfRecord.WORKFLOW_DEPENDENT, CreateSources.FALSE,
            PlasticToValidate.SOURCE, PipelineTransformation.NONE, ForwardMessage.NONE, VolumeConcUpdate.MERCURY_ONLY,
            LibraryType.NONE_ASSIGNED),
    MRRBS_FINAL_PRODUCT_POOL("mRRBSFinalProductPool",
            ExpectSourcesEmpty.FALSE, ExpectTargetsEmpty.TRUE, SystemOfRecord.WORKFLOW_DEPENDENT, CreateSources.FALSE,
            PlasticToValidate.SOURCE, PipelineTransformation.NONE, ForwardMessage.NONE, VolumeConcUpdate.MERCURY_ONLY,
            LibraryType.NONE_ASSIGNED),

    // Malaria Illumina
    MALARIA_PCR1("Malaria_PCR1",
            ExpectSourcesEmpty.FALSE, ExpectTargetsEmpty.TRUE, SystemOfRecord.WORKFLOW_DEPENDENT, CreateSources.FALSE,
            PlasticToValidate.SOURCE, PipelineTransformation.LIGATION, ForwardMessage.NONE, VolumeConcUpdate.MERCURY_ONLY,
            LibraryType.NONE_ASSIGNED),
    MALARIA_PCRTAIL2("Malaria_PCRTail2",
            ExpectSourcesEmpty.FALSE, ExpectTargetsEmpty.TRUE, SystemOfRecord.WORKFLOW_DEPENDENT, CreateSources.FALSE,
            PlasticToValidate.SOURCE, PipelineTransformation.LIGATION, ForwardMessage.NONE, VolumeConcUpdate.MERCURY_ONLY,
            LibraryType.NONE_ASSIGNED),
    MALARIA_PCR_NORM("Malaria_PCR_Norm",
            ExpectSourcesEmpty.FALSE, ExpectTargetsEmpty.TRUE, SystemOfRecord.WORKFLOW_DEPENDENT, CreateSources.FALSE,
            PlasticToValidate.SOURCE, PipelineTransformation.NONE, ForwardMessage.NONE, VolumeConcUpdate.MERCURY_ONLY,
            LibraryType.NONE_ASSIGNED),
    MALARIA_CALIPER("Malaria_Caliper",
            ExpectSourcesEmpty.FALSE, ExpectTargetsEmpty.TRUE, SystemOfRecord.WORKFLOW_DEPENDENT, CreateSources.FALSE,
            PlasticToValidate.SOURCE, PipelineTransformation.NONE, ForwardMessage.NONE, VolumeConcUpdate.MERCURY_ONLY,
            LibraryType.NONE_ASSIGNED),
    MALARIA_MISEQ_PCR_POOL_SPRI("MalariaMiseqPCRPoolSPRI",
            ExpectSourcesEmpty.FALSE, ExpectTargetsEmpty.TRUE, SystemOfRecord.WORKFLOW_DEPENDENT, CreateSources.FALSE,
            PlasticToValidate.SOURCE, PipelineTransformation.NONE, ForwardMessage.NONE, VolumeConcUpdate.MERCURY_ONLY,
            LibraryType.NONE_ASSIGNED),

    // Malaria PacBio
    MALARIA_BEP_PCR("Malaria_BEP_PCR",
            ExpectSourcesEmpty.FALSE, ExpectTargetsEmpty.TRUE, SystemOfRecord.WORKFLOW_DEPENDENT, CreateSources.FALSE,
            PlasticToValidate.SOURCE, PipelineTransformation.NONE, ForwardMessage.NONE, VolumeConcUpdate.MERCURY_ONLY,
            LibraryType.NONE_ASSIGNED),
    MALARIA_BEP_PRIMER_TRANSFER("MalariaBEPPrimerTransfer",
            ExpectSourcesEmpty.FALSE, ExpectTargetsEmpty.TRUE, SystemOfRecord.WORKFLOW_DEPENDENT, CreateSources.FALSE,
            PlasticToValidate.TARGET, PipelineTransformation.LIGATION, ForwardMessage.NONE, VolumeConcUpdate.MERCURY_ONLY,
            LibraryType.NONE_ASSIGNED),
    MALARIA_BEP_PCR_NORM("Malaria_BEP_PCR_Norm",
            ExpectSourcesEmpty.FALSE, ExpectTargetsEmpty.TRUE, SystemOfRecord.WORKFLOW_DEPENDENT, CreateSources.FALSE,
            PlasticToValidate.SOURCE, PipelineTransformation.NONE, ForwardMessage.NONE, VolumeConcUpdate.MERCURY_ONLY,
            LibraryType.NONE_ASSIGNED),
    MALARIA_BEP_POOL("Malaria_BEP_Pool",
            ExpectSourcesEmpty.FALSE, ExpectTargetsEmpty.TRUE, SystemOfRecord.WORKFLOW_DEPENDENT, CreateSources.FALSE,
            PlasticToValidate.SOURCE, PipelineTransformation.NONE, ForwardMessage.NONE, VolumeConcUpdate.MERCURY_ONLY,
            LibraryType.NONE_ASSIGNED),

    //Microbial Nextera XT (MOC)
    MICROBIAL_SOURCE_RACK_POOL("MicrobialSourceRackPool",
            ExpectSourcesEmpty.FALSE, ExpectTargetsEmpty.TRUE, SystemOfRecord.MERCURY, CreateSources.FALSE,
            PlasticToValidate.SOURCE, PipelineTransformation.NONE, ForwardMessage.NONE, VolumeConcUpdate.MERCURY_ONLY,
            LibraryType.NONE_ASSIGNED),
    MICROBIAL_TAGMENTATION("MicrobialTagmentation",
            ExpectSourcesEmpty.FALSE, ExpectTargetsEmpty.TRUE, SystemOfRecord.MERCURY, CreateSources.FALSE,
            PlasticToValidate.SOURCE, PipelineTransformation.NONE, ForwardMessage.NONE, VolumeConcUpdate.MERCURY_ONLY,
            LibraryType.NONE_ASSIGNED),
    MICROBIAL_NEUTRALIZATION("MicrobialNeutralization",
            ExpectSourcesEmpty.FALSE, ExpectTargetsEmpty.TRUE, SystemOfRecord.MERCURY, CreateSources.FALSE,
            PlasticToValidate.SOURCE, PipelineTransformation.NONE, ForwardMessage.NONE, VolumeConcUpdate.MERCURY_ONLY,
            LibraryType.NONE_ASSIGNED),
    MICROBIAL_INDEX_PCR("MicrobialIndexPCR",
            ExpectSourcesEmpty.FALSE, ExpectTargetsEmpty.TRUE, SystemOfRecord.MERCURY, CreateSources.FALSE,
            PlasticToValidate.SOURCE, PipelineTransformation.NONE, ForwardMessage.NONE, VolumeConcUpdate.MERCURY_ONLY,
            LibraryType.NONE_ASSIGNED),
    MICROBIAL_SPRI("MicrobialSPRI",
            ExpectSourcesEmpty.FALSE, ExpectTargetsEmpty.TRUE, SystemOfRecord.MERCURY, CreateSources.FALSE,
            PlasticToValidate.SOURCE, PipelineTransformation.NONE, ForwardMessage.NONE, VolumeConcUpdate.MERCURY_ONLY,
            LibraryType.NONE_ASSIGNED),
    MICROBIAL_POOL_TRANSFER("MicrobialPoolTransfer",
            ExpectSourcesEmpty.FALSE, ExpectTargetsEmpty.TRUE, SystemOfRecord.MERCURY, CreateSources.FALSE,
            PlasticToValidate.SOURCE, PipelineTransformation.NONE, ForwardMessage.NONE, VolumeConcUpdate.MERCURY_ONLY,
            LibraryType.NONE_ASSIGNED),

    // Illumina Content Exome (ICE)
    ICE_BUCKET("IceBucket",
            ExpectSourcesEmpty.TRUE, ExpectTargetsEmpty.TRUE, SystemOfRecord.WORKFLOW_DEPENDENT, CreateSources.FALSE,
            PlasticToValidate.SOURCE, PipelineTransformation.NONE, ForwardMessage.NONE, VolumeConcUpdate.MERCURY_ONLY,
            LibraryType.NONE_ASSIGNED),
    ICE_POOLING_TRANSFER("IcePoolingTransfer",
            ExpectSourcesEmpty.FALSE, ExpectTargetsEmpty.TRUE, SystemOfRecord.WORKFLOW_DEPENDENT, CreateSources.FALSE,
            PlasticToValidate.SOURCE, PipelineTransformation.NONE, ForwardMessage.NONE, VolumeConcUpdate.MERCURY_ONLY,
            LibraryType.POOLED),
    ICE_POOL_TEST("IcePoolTest",
            ExpectSourcesEmpty.FALSE, ExpectTargetsEmpty.TRUE, SystemOfRecord.WORKFLOW_DEPENDENT, CreateSources.FALSE,
            PlasticToValidate.SOURCE, PipelineTransformation.NONE, ForwardMessage.NONE, VolumeConcUpdate.MERCURY_ONLY,
            LibraryType.NONE_ASSIGNED),
    ICE_SPRI_CONCENTRATION("IceSPRIConcentration",
            ExpectSourcesEmpty.FALSE, ExpectTargetsEmpty.TRUE, SystemOfRecord.WORKFLOW_DEPENDENT, CreateSources.FALSE,
            PlasticToValidate.SOURCE, PipelineTransformation.NONE, ForwardMessage.NONE, VolumeConcUpdate.MERCURY_ONLY,
            LibraryType.NONE_ASSIGNED),
    ICE_96_PLEX_SPRI_CONCENTRATION("Ice96PlexSpriConcentration",
            ExpectSourcesEmpty.FALSE, ExpectTargetsEmpty.TRUE, SystemOfRecord.WORKFLOW_DEPENDENT, CreateSources.FALSE,
            PlasticToValidate.SOURCE, PipelineTransformation.NONE, ForwardMessage.NONE, VolumeConcUpdate.MERCURY_ONLY,
            LibraryType.NONE_ASSIGNED),
    ICE_1ST_HYBRIDIZATION("Ice1stHybridization",
            ExpectSourcesEmpty.FALSE, ExpectTargetsEmpty.TRUE, SystemOfRecord.WORKFLOW_DEPENDENT, CreateSources.FALSE,
            PlasticToValidate.SOURCE, PipelineTransformation.NONE, ForwardMessage.NONE, VolumeConcUpdate.MERCURY_ONLY,
            LibraryType.NONE_ASSIGNED),
    POST_ICE_1ST_HYBRIDIZATION_THERMO_CYCLER_LOADED("PostIce1stHybridizationThermoCyclerLoaded",
            ExpectSourcesEmpty.FALSE, ExpectTargetsEmpty.TRUE, SystemOfRecord.WORKFLOW_DEPENDENT, CreateSources.FALSE,
            PlasticToValidate.SOURCE, PipelineTransformation.NONE, ForwardMessage.NONE, VolumeConcUpdate.MERCURY_ONLY,
            LibraryType.NONE_ASSIGNED),
    ICE_1S_TBAIT_ADDITION("Ice1stBaitAddition",
            ExpectSourcesEmpty.FALSE, ExpectTargetsEmpty.TRUE, SystemOfRecord.WORKFLOW_DEPENDENT, CreateSources.FALSE,
            PlasticToValidate.TARGET, PipelineTransformation.NONE, ForwardMessage.NONE, VolumeConcUpdate.MERCURY_ONLY,
            LibraryType.NONE_ASSIGNED),
    ICE_1S_TBAIT_PICK("Ice1stBaitPick",
            ExpectSourcesEmpty.FALSE, ExpectTargetsEmpty.TRUE, SystemOfRecord.WORKFLOW_DEPENDENT, CreateSources.FALSE,
            PlasticToValidate.TARGET, PipelineTransformation.NONE, ForwardMessage.NONE, VolumeConcUpdate.MERCURY_ONLY,
            LibraryType.NONE_ASSIGNED),
    ICE_1ST_CAPTURE("Ice1stCapture",
            ExpectSourcesEmpty.FALSE, ExpectTargetsEmpty.TRUE, SystemOfRecord.WORKFLOW_DEPENDENT, CreateSources.FALSE,
            PlasticToValidate.SOURCE, PipelineTransformation.NONE, ForwardMessage.NONE, VolumeConcUpdate.MERCURY_ONLY,
            LibraryType.NONE_ASSIGNED),
    POST_ICE_1ST_CAPTURE_THERMO_CYCLER_LOADED("PostIce1stCaptureThermoCyclerLoaded",
            ExpectSourcesEmpty.FALSE, ExpectTargetsEmpty.TRUE, SystemOfRecord.WORKFLOW_DEPENDENT, CreateSources.FALSE,
            PlasticToValidate.SOURCE, PipelineTransformation.NONE, ForwardMessage.NONE, VolumeConcUpdate.MERCURY_ONLY,
            LibraryType.NONE_ASSIGNED),
    ICE_2ND_HYBRIDIZATION("Ice2ndHybridization",
            ExpectSourcesEmpty.FALSE, ExpectTargetsEmpty.TRUE, SystemOfRecord.WORKFLOW_DEPENDENT, CreateSources.FALSE,
            PlasticToValidate.SOURCE, PipelineTransformation.NONE, ForwardMessage.NONE, VolumeConcUpdate.MERCURY_ONLY,
            LibraryType.NONE_ASSIGNED),
    POST_ICE_2ND_HYBRIDIZATION_THERMO_CYCLER_LOADED("PostIce2ndHybridizationThermoCyclerLoaded",
            ExpectSourcesEmpty.FALSE, ExpectTargetsEmpty.TRUE, SystemOfRecord.WORKFLOW_DEPENDENT, CreateSources.FALSE,
            PlasticToValidate.SOURCE, PipelineTransformation.NONE, ForwardMessage.NONE, VolumeConcUpdate.MERCURY_ONLY,
            LibraryType.NONE_ASSIGNED),
    ICE_2ND_BAIT_ADDITION("Ice2ndBaitAddition",
            ExpectSourcesEmpty.FALSE, ExpectTargetsEmpty.TRUE, SystemOfRecord.WORKFLOW_DEPENDENT, CreateSources.FALSE,
            PlasticToValidate.TARGET, PipelineTransformation.NONE, ForwardMessage.NONE, VolumeConcUpdate.MERCURY_ONLY,
            LibraryType.NONE_ASSIGNED),
    ICE_2ND_BAIT_PICK("Ice2ndBaitPick",
            ExpectSourcesEmpty.FALSE, ExpectTargetsEmpty.TRUE, SystemOfRecord.WORKFLOW_DEPENDENT, CreateSources.FALSE,
            PlasticToValidate.TARGET, PipelineTransformation.NONE, ForwardMessage.NONE, VolumeConcUpdate.MERCURY_ONLY,
            LibraryType.NONE_ASSIGNED),
    ICE_2ND_CAPTURE("Ice2ndCapture",
            ExpectSourcesEmpty.FALSE, ExpectTargetsEmpty.TRUE, SystemOfRecord.WORKFLOW_DEPENDENT, CreateSources.FALSE,
            PlasticToValidate.SOURCE, PipelineTransformation.NONE, ForwardMessage.NONE, VolumeConcUpdate.MERCURY_ONLY,
            LibraryType.NONE_ASSIGNED),
    POST_ICE_2ND_CAPTURE_THERMO_CYCLER_LOADED("PostIce2ndCaptureThermoCyclerLoaded",
            ExpectSourcesEmpty.FALSE, ExpectTargetsEmpty.TRUE, SystemOfRecord.WORKFLOW_DEPENDENT, CreateSources.FALSE,
            PlasticToValidate.SOURCE, PipelineTransformation.NONE, ForwardMessage.NONE, VolumeConcUpdate.MERCURY_ONLY,
            LibraryType.NONE_ASSIGNED),
    ICE_CATCH_CLEANUP("IceCatchCleanup",
            ExpectSourcesEmpty.FALSE, ExpectTargetsEmpty.TRUE, SystemOfRecord.WORKFLOW_DEPENDENT, CreateSources.FALSE,
            PlasticToValidate.SOURCE, PipelineTransformation.NONE, ForwardMessage.NONE, VolumeConcUpdate.MERCURY_ONLY,
            LibraryType.NONE_ASSIGNED),
    ICE_CATCH_ENRICHMENT_SETUP("IceCatchEnrichmentSetup",
            ExpectSourcesEmpty.FALSE, ExpectTargetsEmpty.TRUE, SystemOfRecord.WORKFLOW_DEPENDENT, CreateSources.FALSE,
            PlasticToValidate.SOURCE, PipelineTransformation.NONE, ForwardMessage.NONE, VolumeConcUpdate.MERCURY_ONLY,
            LibraryType.NONE_ASSIGNED),
    POST_ICE_CATCH_ENRICHMENT_SETUP_THERMO_CYCLER_LOADED("PostIceCatchEnrichmentSetupThermoCyclerLoaded",
            ExpectSourcesEmpty.FALSE, ExpectTargetsEmpty.TRUE, SystemOfRecord.WORKFLOW_DEPENDENT, CreateSources.FALSE,
            PlasticToValidate.SOURCE, PipelineTransformation.NONE, ForwardMessage.NONE, VolumeConcUpdate.MERCURY_ONLY,
            LibraryType.NONE_ASSIGNED),
    ICE_CATCH_ENRICHMENT_CLEANUP("IceCatchEnrichmentCleanup",
            ExpectSourcesEmpty.FALSE, ExpectTargetsEmpty.TRUE, SystemOfRecord.WORKFLOW_DEPENDENT, CreateSources.FALSE,
            PlasticToValidate.SOURCE, PipelineTransformation.PCR, ForwardMessage.NONE, VolumeConcUpdate.MERCURY_ONLY,
            LibraryType.HYBRID_SELECTION_ICE_ENRICHED_CATCH),
    ICE_CATCH_PICO("IceCatchPico",
            ExpectSourcesEmpty.FALSE, ExpectTargetsEmpty.TRUE, SystemOfRecord.WORKFLOW_DEPENDENT, CreateSources.FALSE,
            PlasticToValidate.SOURCE, PipelineTransformation.NONE, ForwardMessage.NONE, VolumeConcUpdate.MERCURY_ONLY,
            LibraryType.NONE_ASSIGNED),

    // Nexome
    PRE_TAGMENTATION_TRANSFER("PreTagmentationTransfer",
            ExpectSourcesEmpty.FALSE, ExpectTargetsEmpty.TRUE, SystemOfRecord.WORKFLOW_DEPENDENT, CreateSources.FALSE,
            PlasticToValidate.SOURCE, PipelineTransformation.NONE, ForwardMessage.NONE, VolumeConcUpdate.MERCURY_ONLY,
            LibraryType.NONE_ASSIGNED),
    TAGMENTATION("Tagmentation",
            ExpectSourcesEmpty.FALSE, ExpectTargetsEmpty.TRUE, SystemOfRecord.WORKFLOW_DEPENDENT, CreateSources.FALSE,
            PlasticToValidate.SOURCE, PipelineTransformation.NONE, ForwardMessage.NONE, VolumeConcUpdate.MERCURY_ONLY,
            LibraryType.NONE_ASSIGNED),
    STOP_TAGMENTATION("StopTagmentation",
            ExpectSourcesEmpty.FALSE, ExpectTargetsEmpty.TRUE, SystemOfRecord.WORKFLOW_DEPENDENT, CreateSources.FALSE,
            PlasticToValidate.SOURCE, PipelineTransformation.NONE, ForwardMessage.NONE, VolumeConcUpdate.MERCURY_ONLY,
            LibraryType.NONE_ASSIGNED),
    TAGMENTATION_CLEANUP("TagmentationCleanup",
            ExpectSourcesEmpty.FALSE, ExpectTargetsEmpty.TRUE, SystemOfRecord.WORKFLOW_DEPENDENT, CreateSources.FALSE,
            PlasticToValidate.SOURCE, PipelineTransformation.NONE, ForwardMessage.NONE, VolumeConcUpdate.MERCURY_ONLY,
            LibraryType.NONE_ASSIGNED),
    NEXTERA_DUAL_INDEX_PCR("NexteraDualIndexPCR",
            ExpectSourcesEmpty.FALSE, ExpectTargetsEmpty.TRUE, SystemOfRecord.WORKFLOW_DEPENDENT, CreateSources.FALSE,
            PlasticToValidate.TARGET, PipelineTransformation.PCR, ForwardMessage.NONE, VolumeConcUpdate.MERCURY_ONLY,
            LibraryType.NONE_ASSIGNED),
    NEXTERA_PCR_CLEANUP("NexteraPCRCleanup",
            ExpectSourcesEmpty.FALSE, ExpectTargetsEmpty.TRUE, SystemOfRecord.WORKFLOW_DEPENDENT, CreateSources.FALSE,
            PlasticToValidate.SOURCE, PipelineTransformation.NONE, ForwardMessage.NONE, VolumeConcUpdate.MERCURY_ONLY,
            LibraryType.NEXTERA_ENRICHED_LIBRARY),
    NEXOME_POND_PICO("NexomePondPico",
            ExpectSourcesEmpty.FALSE, ExpectTargetsEmpty.TRUE, SystemOfRecord.WORKFLOW_DEPENDENT, CreateSources.FALSE,
            PlasticToValidate.SOURCE, PipelineTransformation.NONE, ForwardMessage.NONE, VolumeConcUpdate.MERCURY_ONLY,
            LibraryType.NONE_ASSIGNED),
    NEXOME_POOLING_TRANSFER("NexomePoolingTransfer",
            ExpectSourcesEmpty.FALSE, ExpectTargetsEmpty.TRUE, SystemOfRecord.WORKFLOW_DEPENDENT, CreateSources.FALSE,
            PlasticToValidate.SOURCE, PipelineTransformation.NONE, ForwardMessage.NONE, VolumeConcUpdate.MERCURY_ONLY,
            LibraryType.NEXTERA_POOLED_NORMALIZED_LIBRARY),
    NEXOME_POOL_TEST("NexomePoolTest",
            ExpectSourcesEmpty.FALSE, ExpectTargetsEmpty.TRUE, SystemOfRecord.WORKFLOW_DEPENDENT, CreateSources.FALSE,
            PlasticToValidate.SOURCE, PipelineTransformation.NONE, ForwardMessage.NONE, VolumeConcUpdate.MERCURY_ONLY,
            LibraryType.NONE_ASSIGNED),
    NEXOME_SPRI_CONCENTRATION("NexomeSPRIConcentration",
            ExpectSourcesEmpty.FALSE, ExpectTargetsEmpty.TRUE, SystemOfRecord.WORKFLOW_DEPENDENT, CreateSources.FALSE,
            PlasticToValidate.SOURCE, PipelineTransformation.NONE, ForwardMessage.NONE, VolumeConcUpdate.MERCURY_ONLY,
            LibraryType.NEXTERA_SPRI_CONCENTRATED_POOL),
    NEXOME_1ST_HYBRIDIZATION("Nexome1stHybridization",
            ExpectSourcesEmpty.FALSE, ExpectTargetsEmpty.TRUE, SystemOfRecord.WORKFLOW_DEPENDENT, CreateSources.FALSE,
            PlasticToValidate.SOURCE, PipelineTransformation.NONE, ForwardMessage.NONE, VolumeConcUpdate.MERCURY_ONLY,
            LibraryType.NONE_ASSIGNED),
    NEXOME_1ST_BAIT_ADDITION("Nexome1stBaitAddition",
            ExpectSourcesEmpty.FALSE, ExpectTargetsEmpty.TRUE, SystemOfRecord.WORKFLOW_DEPENDENT, CreateSources.FALSE,
            PlasticToValidate.TARGET, PipelineTransformation.NONE, ForwardMessage.NONE, VolumeConcUpdate.MERCURY_ONLY,
            LibraryType.NONE_ASSIGNED),
    NEXOME_1ST_CAPTURE("Nexome1stCapture",
            ExpectSourcesEmpty.FALSE, ExpectTargetsEmpty.TRUE, SystemOfRecord.WORKFLOW_DEPENDENT, CreateSources.FALSE,
            PlasticToValidate.SOURCE, PipelineTransformation.NONE, ForwardMessage.NONE, VolumeConcUpdate.MERCURY_ONLY,
            LibraryType.NONE_ASSIGNED),
    NEXOME_2ND_BAIT_ADDITION("Nexome2ndBaitAddition",
            ExpectSourcesEmpty.FALSE, ExpectTargetsEmpty.TRUE, SystemOfRecord.WORKFLOW_DEPENDENT, CreateSources.FALSE,
            PlasticToValidate.SOURCE, PipelineTransformation.NONE, ForwardMessage.NONE, VolumeConcUpdate.MERCURY_ONLY,
            LibraryType.NONE_ASSIGNED),
    NEXOME_2ND_HYBRIDIZATION("Nexome2ndHybridization",
            ExpectSourcesEmpty.FALSE, ExpectTargetsEmpty.TRUE, SystemOfRecord.WORKFLOW_DEPENDENT, CreateSources.FALSE,
            PlasticToValidate.SOURCE, PipelineTransformation.NONE, ForwardMessage.NONE, VolumeConcUpdate.MERCURY_ONLY,
            LibraryType.NONE_ASSIGNED),
    NEXOME_2ND_HYBRIDIZATION_TRANSFER("Nexome2ndHybridizationTransfer",
            ExpectSourcesEmpty.FALSE, ExpectTargetsEmpty.TRUE, SystemOfRecord.WORKFLOW_DEPENDENT, CreateSources.FALSE,
            PlasticToValidate.SOURCE, PipelineTransformation.NONE, ForwardMessage.NONE, VolumeConcUpdate.MERCURY_ONLY,
            LibraryType.NONE_ASSIGNED),
    NEXOME_2ND_CAPTURE("Nexome2ndCapture",
            ExpectSourcesEmpty.FALSE, ExpectTargetsEmpty.TRUE, SystemOfRecord.WORKFLOW_DEPENDENT, CreateSources.FALSE,
            PlasticToValidate.SOURCE, PipelineTransformation.NONE, ForwardMessage.NONE, VolumeConcUpdate.MERCURY_ONLY,
            LibraryType.NONE_ASSIGNED),
    NEXOME_CATCH_CLEANUP("NexomeCatchCleanup",
            ExpectSourcesEmpty.FALSE, ExpectTargetsEmpty.TRUE, SystemOfRecord.WORKFLOW_DEPENDENT, CreateSources.FALSE,
            PlasticToValidate.SOURCE, PipelineTransformation.CAPTURE, ForwardMessage.NONE, VolumeConcUpdate.MERCURY_ONLY,
            LibraryType.NONE_ASSIGNED),
    NEXOME_CATCH_ENRICHMENT("NexomeCatchEnrichment",
            ExpectSourcesEmpty.FALSE, ExpectTargetsEmpty.TRUE, SystemOfRecord.WORKFLOW_DEPENDENT, CreateSources.FALSE,
            PlasticToValidate.SOURCE, PipelineTransformation.NONE, ForwardMessage.NONE, VolumeConcUpdate.MERCURY_ONLY,
            LibraryType.NONE_ASSIGNED),
    NEXOME_CATCH_ENRICHMENT_CLEANUP("NexomeCatchEnrichmentCleanup",
            ExpectSourcesEmpty.FALSE, ExpectTargetsEmpty.TRUE, SystemOfRecord.WORKFLOW_DEPENDENT, CreateSources.FALSE,
            PlasticToValidate.SOURCE, PipelineTransformation.PCR, ForwardMessage.NONE, VolumeConcUpdate.MERCURY_ONLY,
            LibraryType.NEXOME_CATCH),
    NEXOME_CATCH_PICO("NexomeCatchPico",
            ExpectSourcesEmpty.FALSE, ExpectTargetsEmpty.TRUE, SystemOfRecord.WORKFLOW_DEPENDENT, CreateSources.FALSE,
            PlasticToValidate.SOURCE, PipelineTransformation.NONE, ForwardMessage.NONE, VolumeConcUpdate.MERCURY_ONLY,
            LibraryType.NONE_ASSIGNED),

    // Nexome V2
    TWO_PLATE_TAGMENTATION_CLEANUP("2PlateTagmentationCleanup",
            ExpectSourcesEmpty.FALSE, ExpectTargetsEmpty.TRUE, SystemOfRecord.WORKFLOW_DEPENDENT, CreateSources.FALSE,
            PlasticToValidate.SOURCE, PipelineTransformation.NONE, ForwardMessage.NONE, VolumeConcUpdate.MERCURY_ONLY,
            LibraryType.NONE_ASSIGNED),
    NEXTERA_PCR_TRANSFER("NexteraPCRTransfer",
            ExpectSourcesEmpty.FALSE, ExpectTargetsEmpty.TRUE, SystemOfRecord.WORKFLOW_DEPENDENT, CreateSources.FALSE,
            PlasticToValidate.SOURCE, PipelineTransformation.NONE, ForwardMessage.NONE, VolumeConcUpdate.MERCURY_ONLY,
            LibraryType.NONE_ASSIGNED),
    TWO_PLATE_NEXTERA_PCR_CLEANUP("2PlateNexteraPCRCleanup",
            ExpectSourcesEmpty.FALSE, ExpectTargetsEmpty.TRUE, SystemOfRecord.WORKFLOW_DEPENDENT, CreateSources.FALSE,
            PlasticToValidate.SOURCE, PipelineTransformation.NONE, ForwardMessage.NONE, VolumeConcUpdate.MERCURY_ONLY,
            LibraryType.NONE_ASSIGNED),
    THREE_84_WELL_POND_PICO("384WellPondPico",
            ExpectSourcesEmpty.FALSE, ExpectTargetsEmpty.TRUE, SystemOfRecord.WORKFLOW_DEPENDENT, CreateSources.FALSE,
            PlasticToValidate.SOURCE, PipelineTransformation.NONE, ForwardMessage.NONE, VolumeConcUpdate.MERCURY_ONLY,
            LibraryType.NONE_ASSIGNED),
    NEXOME_NORM_TRANSFER("NexomeNormTransfer",
            ExpectSourcesEmpty.FALSE, ExpectTargetsEmpty.TRUE, SystemOfRecord.WORKFLOW_DEPENDENT, CreateSources.FALSE,
            PlasticToValidate.SOURCE, PipelineTransformation.NONE, ForwardMessage.NONE, VolumeConcUpdate.MERCURY_ONLY,
            LibraryType.NEXTERA_ENRICHED_LIBRARY),
    NEXOME_BAIT_ADDITION("NexomeBaitAddition",
            ExpectSourcesEmpty.FALSE, ExpectTargetsEmpty.TRUE, SystemOfRecord.WORKFLOW_DEPENDENT, CreateSources.FALSE,
            PlasticToValidate.SOURCE, PipelineTransformation.NONE, ForwardMessage.NONE, VolumeConcUpdate.MERCURY_ONLY,
            LibraryType.NONE_ASSIGNED),
    TWO_PLATE_NEXOME_2ND_CAPTURE("2PlateNexome2ndCapture",
            ExpectSourcesEmpty.FALSE, ExpectTargetsEmpty.TRUE, SystemOfRecord.WORKFLOW_DEPENDENT, CreateSources.FALSE,
            PlasticToValidate.SOURCE, PipelineTransformation.NONE, ForwardMessage.NONE, VolumeConcUpdate.MERCURY_ONLY,
            LibraryType.NONE_ASSIGNED),
    TWO_PLATE_NEXOME_CATCH_CLEANUP("2PlateNexomeCatchCleanup",
            ExpectSourcesEmpty.FALSE, ExpectTargetsEmpty.TRUE, SystemOfRecord.WORKFLOW_DEPENDENT, CreateSources.FALSE,
            PlasticToValidate.SOURCE, PipelineTransformation.NONE, ForwardMessage.NONE, VolumeConcUpdate.MERCURY_ONLY,
            LibraryType.NONE_ASSIGNED),
    NEXOME_PCR_TRANSFER("NexomePCRTransfer",
            ExpectSourcesEmpty.FALSE, ExpectTargetsEmpty.TRUE, SystemOfRecord.WORKFLOW_DEPENDENT, CreateSources.FALSE,
            PlasticToValidate.SOURCE, PipelineTransformation.NONE, ForwardMessage.NONE, VolumeConcUpdate.MERCURY_ONLY,
            LibraryType.NONE_ASSIGNED),
    TWO_PLATE_NEXOME_CATCH_ENRICHMENT("2PlateNexomeCatchEnrichment",
            ExpectSourcesEmpty.FALSE, ExpectTargetsEmpty.TRUE, SystemOfRecord.WORKFLOW_DEPENDENT, CreateSources.FALSE,
            PlasticToValidate.SOURCE, PipelineTransformation.PCR, ForwardMessage.NONE, VolumeConcUpdate.MERCURY_ONLY,
            LibraryType.NEXOME_CATCH),

    // PCRFree
    // Set PipelineTransformation to PCR because this is the first tube after the first PCR
    PCR_FREE_POND_REGISTRATION("PCRFreePondRegistration",
            ExpectSourcesEmpty.FALSE, ExpectTargetsEmpty.TRUE, SystemOfRecord.WORKFLOW_DEPENDENT, CreateSources.FALSE,
            PlasticToValidate.SOURCE, PipelineTransformation.PCR, ForwardMessage.NONE, VolumeConcUpdate.MERCURY_ONLY,
            LibraryType.PCR_FREE_POND),
    PCR_FREE_08X_CLEANUP("PCRFree0.8xCleanup",
            ExpectSourcesEmpty.FALSE, ExpectTargetsEmpty.TRUE, SystemOfRecord.WORKFLOW_DEPENDENT, CreateSources.FALSE,
            PlasticToValidate.SOURCE, PipelineTransformation.NONE, ForwardMessage.NONE, VolumeConcUpdate.MERCURY_ONLY,
            LibraryType.NONE_ASSIGNED),
    PCR_FREE_3X_CLEANUP("PCRFree3.0xCleanup",
            ExpectSourcesEmpty.FALSE, ExpectTargetsEmpty.TRUE, SystemOfRecord.WORKFLOW_DEPENDENT, CreateSources.FALSE,
            PlasticToValidate.SOURCE, PipelineTransformation.NONE, ForwardMessage.NONE, VolumeConcUpdate.MERCURY_ONLY,
            LibraryType.NONE_ASSIGNED),

    PCR_PLUS_POND_REGISTRATION("PCRPlusPondRegistration",
            ExpectSourcesEmpty.FALSE, ExpectTargetsEmpty.TRUE, SystemOfRecord.WORKFLOW_DEPENDENT, CreateSources.FALSE,
            PlasticToValidate.SOURCE, PipelineTransformation.PCR, ForwardMessage.NONE, VolumeConcUpdate.MERCURY_ONLY,
            LibraryType.PCR_PLUS_POND),
    PCR_PLUS_POND_NORMALIZATION("PCRPlusPondNormalization",
            ExpectSourcesEmpty.FALSE, ExpectTargetsEmpty.TRUE, SystemOfRecord.WORKFLOW_DEPENDENT, CreateSources.FALSE,
            PlasticToValidate.SOURCE, PipelineTransformation.NONE, ForwardMessage.NONE, VolumeConcUpdate.MERCURY_ONLY,
            LibraryType.PCR_PLUS_NORMALIZED_POND),
    POOL_CORRECTION("PoolCorrection",
            ExpectSourcesEmpty.FALSE, ExpectTargetsEmpty.TRUE, SystemOfRecord.MERCURY, CreateSources.FALSE,
            PlasticToValidate.SOURCE, PipelineTransformation.NONE, ForwardMessage.NONE, VolumeConcUpdate.MERCURY_ONLY,
            LibraryType.NONE_ASSIGNED),

    // HyperPrep PCRFree and ICE
    PRE_END_REPAIR_TRANSFER("PreEndRepairTransfer",
            ExpectSourcesEmpty.FALSE, ExpectTargetsEmpty.TRUE, SystemOfRecord.MERCURY, CreateSources.FALSE,
            PlasticToValidate.SOURCE, PipelineTransformation.NONE, ForwardMessage.NONE, VolumeConcUpdate.MERCURY_ONLY,
            LibraryType.NONE_ASSIGNED),
    END_REPAIR_ABASE("EndRepair_ABase",
            ExpectSourcesEmpty.TRUE, ExpectTargetsEmpty.TRUE, SystemOfRecord.WORKFLOW_DEPENDENT, CreateSources.FALSE,
            PlasticToValidate.SOURCE, PipelineTransformation.NONE, ForwardMessage.NONE, VolumeConcUpdate.MERCURY_ONLY,
            LibraryType.NONE_ASSIGNED),
    WGS_PCR_CLEANUP("WGSPCRCleanup",
            ExpectSourcesEmpty.FALSE, ExpectTargetsEmpty.TRUE, SystemOfRecord.WORKFLOW_DEPENDENT, CreateSources.FALSE,
            PlasticToValidate.SOURCE, PipelineTransformation.NONE, ForwardMessage.NONE, VolumeConcUpdate.MERCURY_ONLY,
            LibraryType.PCR_PLUS_POND),
    ICE_HYPERPREP_1ST_BAIT_PICK("IceHyperPrep1stBaitPick",
            ExpectSourcesEmpty.FALSE, ExpectTargetsEmpty.TRUE, SystemOfRecord.MERCURY, CreateSources.FALSE,
            PlasticToValidate.SOURCE, PipelineTransformation.NONE, ForwardMessage.NONE, VolumeConcUpdate.MERCURY_ONLY,
            LibraryType.NONE_ASSIGNED),
    ICE_HYPERPREP_1ST_BAIT_ADDITION("IceHyperPrep1stBaitAddition",
            ExpectSourcesEmpty.FALSE, ExpectTargetsEmpty.TRUE, SystemOfRecord.MERCURY, CreateSources.FALSE,
            PlasticToValidate.SOURCE, PipelineTransformation.NONE, ForwardMessage.NONE, VolumeConcUpdate.MERCURY_ONLY,
            LibraryType.NONE_ASSIGNED),
    ICE_HYPERPREP_1ST_HYBRIDIZATION("IceHyperPrep1stHybridization",
            ExpectSourcesEmpty.FALSE, ExpectTargetsEmpty.FALSE, SystemOfRecord.MERCURY, CreateSources.FALSE,
            PlasticToValidate.BOTH, PipelineTransformation.NONE, ForwardMessage.NONE, VolumeConcUpdate.MERCURY_ONLY,
            LibraryType.NONE_ASSIGNED),
    ICE_HYPERPREP_2ND_BAIT_PICK("IceHyperPrep2ndBaitPick",
            ExpectSourcesEmpty.FALSE, ExpectTargetsEmpty.TRUE, SystemOfRecord.MERCURY, CreateSources.FALSE,
            PlasticToValidate.SOURCE, PipelineTransformation.NONE, ForwardMessage.NONE, VolumeConcUpdate.MERCURY_ONLY,
            LibraryType.NONE_ASSIGNED),
    ICE_HYPERPREP_2ND_BAIT_ADDITION("IceHyperPrep2ndBaitAddition",
            ExpectSourcesEmpty.FALSE, ExpectTargetsEmpty.FALSE, SystemOfRecord.MERCURY, CreateSources.FALSE,
            PlasticToValidate.BOTH, PipelineTransformation.NONE, ForwardMessage.NONE, VolumeConcUpdate.MERCURY_ONLY,
            LibraryType.NONE_ASSIGNED),

    // Germline Exome
    PCR_TRANSFER("PCRTransfer",
            ExpectSourcesEmpty.FALSE, ExpectTargetsEmpty.TRUE, SystemOfRecord.MERCURY, CreateSources.FALSE,
            PlasticToValidate.SOURCE, PipelineTransformation.NONE, ForwardMessage.NONE, VolumeConcUpdate.MERCURY_ONLY,
            LibraryType.NONE_ASSIGNED),
    PCR_THERMO_CYCLER_LOADED("PcrThermoCyclerLoaded",
            ExpectSourcesEmpty.TRUE, ExpectTargetsEmpty.TRUE, SystemOfRecord.MERCURY, CreateSources.FALSE,
            PlasticToValidate.SOURCE, PipelineTransformation.NONE, ForwardMessage.NONE, VolumeConcUpdate.MERCURY_ONLY,
            LibraryType.NONE_ASSIGNED),
    GERMLINE_EXOME_PCR_CLEANUP("GermlineExomePCRCleanup",
            ExpectSourcesEmpty.FALSE, ExpectTargetsEmpty.TRUE, SystemOfRecord.MERCURY, CreateSources.FALSE,
            PlasticToValidate.SOURCE, PipelineTransformation.NONE, ForwardMessage.NONE, VolumeConcUpdate.MERCURY_ONLY,
            LibraryType.NONE_ASSIGNED),
    GERMLINE_EXOME_NORM_TRANSFER("GermlineExomeNormTransfer",
            ExpectSourcesEmpty.FALSE, ExpectTargetsEmpty.TRUE, SystemOfRecord.MERCURY, CreateSources.FALSE,
            PlasticToValidate.SOURCE, PipelineTransformation.PCR, ForwardMessage.NONE, VolumeConcUpdate.MERCURY_ONLY,
            LibraryType.GERMLINE_POND),

    // Custom Selection (e.g. twist)
    CUSTOM_SELECTION_BUCKET("CustomSelectionBucket",
            ExpectSourcesEmpty.TRUE, ExpectTargetsEmpty.TRUE, SystemOfRecord.MERCURY, CreateSources.FALSE,
            PlasticToValidate.SOURCE, PipelineTransformation.NONE, ForwardMessage.NONE, VolumeConcUpdate.MERCURY_ONLY,
            LibraryType.NONE_ASSIGNED),
    SELECTION_POOLING("SelectionPoolingTransfer",
            ExpectSourcesEmpty.FALSE, ExpectTargetsEmpty.TRUE, SystemOfRecord.MERCURY, CreateSources.FALSE,
            PlasticToValidate.SOURCE, PipelineTransformation.NONE, ForwardMessage.NONE, VolumeConcUpdate.MERCURY_ONLY,
            LibraryType.NONE_ASSIGNED),
    SELECTION_CONCENTRAION_POOLING("SelectionConcentrationTransfer",
            ExpectSourcesEmpty.FALSE, ExpectTargetsEmpty.TRUE, SystemOfRecord.MERCURY, CreateSources.FALSE,
            PlasticToValidate.SOURCE, PipelineTransformation.NONE, ForwardMessage.NONE, VolumeConcUpdate.MERCURY_ONLY,
            LibraryType.NONE_ASSIGNED),
    SELECTION_HYB_SETUP("SelectionHybSetup",
            ExpectSourcesEmpty.FALSE, ExpectTargetsEmpty.TRUE, SystemOfRecord.MERCURY, CreateSources.FALSE,
            PlasticToValidate.SOURCE, PipelineTransformation.NONE, ForwardMessage.NONE, VolumeConcUpdate.MERCURY_ONLY,
            LibraryType.NONE_ASSIGNED),
    SELECTION_BAIT_PICK("SelectionBaitPick",
            ExpectSourcesEmpty.FALSE, ExpectTargetsEmpty.TRUE, SystemOfRecord.MERCURY, CreateSources.FALSE,
            PlasticToValidate.SOURCE, PipelineTransformation.NONE, ForwardMessage.NONE, VolumeConcUpdate.MERCURY_ONLY,
            LibraryType.NONE_ASSIGNED),
    SELECTION_BAIT_ADDITION("SelectionBaitAddition",
            ExpectSourcesEmpty.FALSE, ExpectTargetsEmpty.TRUE, SystemOfRecord.MERCURY, CreateSources.FALSE,
            PlasticToValidate.SOURCE, PipelineTransformation.NONE, ForwardMessage.NONE, VolumeConcUpdate.MERCURY_ONLY,
            LibraryType.NONE_ASSIGNED),
    SELECTION_HYBRIDIZATION_THERMO_CYCLER_LOADED("SelectionHybridizationThermoCyclerLoaded",
            ExpectSourcesEmpty.FALSE, ExpectTargetsEmpty.TRUE, SystemOfRecord.MERCURY, CreateSources.FALSE,
            PlasticToValidate.SOURCE, PipelineTransformation.NONE, ForwardMessage.NONE, VolumeConcUpdate.MERCURY_ONLY,
            LibraryType.NONE_ASSIGNED),
    SELECTION_BEAD_BINDING("SelectionBeadBinding",
            ExpectSourcesEmpty.FALSE, ExpectTargetsEmpty.TRUE, SystemOfRecord.MERCURY, CreateSources.FALSE,
            PlasticToValidate.SOURCE, PipelineTransformation.NONE, ForwardMessage.NONE, VolumeConcUpdate.MERCURY_ONLY,
            LibraryType.NONE_ASSIGNED),
    SELECTION_CAPTURE("SelectionCapture",
            ExpectSourcesEmpty.FALSE, ExpectTargetsEmpty.TRUE, SystemOfRecord.MERCURY, CreateSources.FALSE,
            PlasticToValidate.SOURCE, PipelineTransformation.NONE, ForwardMessage.NONE, VolumeConcUpdate.MERCURY_ONLY,
            LibraryType.NONE_ASSIGNED),
    POST_SELECTION_CAPTURE_THERMO_CYCLER_LOADED("PostSelectionCaptureThermoCyclerLoaded",
            ExpectSourcesEmpty.FALSE, ExpectTargetsEmpty.TRUE, SystemOfRecord.MERCURY, CreateSources.FALSE,
            PlasticToValidate.SOURCE, PipelineTransformation.NONE, ForwardMessage.NONE, VolumeConcUpdate.MERCURY_ONLY,
            LibraryType.NONE_ASSIGNED),
    SELECTION_CATCH_PCR("SelectionCatchPCR",
            ExpectSourcesEmpty.FALSE, ExpectTargetsEmpty.TRUE, SystemOfRecord.MERCURY, CreateSources.FALSE,
            PlasticToValidate.SOURCE, PipelineTransformation.NONE, ForwardMessage.NONE, VolumeConcUpdate.MERCURY_ONLY,
            LibraryType.NONE_ASSIGNED),
    SELECTION_CATCH_REGISTRATION("SelectionCatchRegistration",
            ExpectSourcesEmpty.FALSE, ExpectTargetsEmpty.TRUE, SystemOfRecord.MERCURY, CreateSources.FALSE,
            PlasticToValidate.SOURCE, PipelineTransformation.NONE, ForwardMessage.NONE, VolumeConcUpdate.MERCURY_ONLY,
            LibraryType.SELECTION_CATCH),

    // Cell Free
    CF_DNA_PCR_SETUP("CFDnaPCRSetup",
            ExpectSourcesEmpty.FALSE, ExpectTargetsEmpty.TRUE, SystemOfRecord.WORKFLOW_DEPENDENT, CreateSources.FALSE,
            PlasticToValidate.SOURCE, PipelineTransformation.NONE, ForwardMessage.NONE, VolumeConcUpdate.MERCURY_ONLY,
            LibraryType.NONE_ASSIGNED),
    CF_DNA_POND_REGISTRATION("CFDnaPondRegistration",
            ExpectSourcesEmpty.FALSE, ExpectTargetsEmpty.TRUE, SystemOfRecord.WORKFLOW_DEPENDENT, CreateSources.FALSE,
            PlasticToValidate.SOURCE, PipelineTransformation.PCR, ForwardMessage.NONE, VolumeConcUpdate.MERCURY_ONLY,
            LibraryType.CF_POND),

    /**
     * TODO the following names are place holders.  They will be re-evaluated as development of
     */
    SHEARING_BUCKET("ShearingBucket",
            ExpectSourcesEmpty.TRUE, ExpectTargetsEmpty.TRUE, SystemOfRecord.MERCURY, CreateSources.FALSE,
            PlasticToValidate.SOURCE, PipelineTransformation.NONE, ForwardMessage.NONE, VolumeConcUpdate.MERCURY_ONLY,
            LibraryType.NONE_ASSIGNED),
//    SHEARING_BUCKET_ENTRY ("ShearingBucketEntry", ExpectSourcesEmpty.TRUE, ExpectTargetsEmpty.TRUE, SystemOfRecord.MERCURY),
//    SHEARING_BUCKET_EXIT ("ShearingBucketExit", ExpectSourcesEmpty.TRUE, ExpectTargetsEmpty.TRUE, SystemOfRecord.MERCURY),
    COLLABORATOR_TRANSFER("CollaboratorTransfer", ExpectSourcesEmpty.FALSE, ExpectTargetsEmpty.TRUE,
            SystemOfRecord.MERCURY, CreateSources.FALSE, PlasticToValidate.TARGET, PipelineTransformation.NONE,
            ForwardMessage.NONE, VolumeConcUpdate.MERCURY_ONLY,
            LibraryType.NONE_ASSIGNED),

    // Activity - sent by decks for otherwise non-messaged protocols (technology development); used by Analytics to
    // track usage.
    ACTIVITY_BEGIN("ActivityBegin",
            ExpectSourcesEmpty.FALSE, ExpectTargetsEmpty.TRUE, SystemOfRecord.MERCURY, CreateSources.FALSE,
            PlasticToValidate.SOURCE, PipelineTransformation.NONE, ForwardMessage.NONE, VolumeConcUpdate.MERCURY_ONLY,
            LibraryType.NONE_ASSIGNED),
    ACTIVITY_END("ActivityEnd",
            ExpectSourcesEmpty.FALSE, ExpectTargetsEmpty.TRUE, SystemOfRecord.MERCURY, CreateSources.FALSE,
            PlasticToValidate.SOURCE, PipelineTransformation.NONE, ForwardMessage.NONE, VolumeConcUpdate.MERCURY_ONLY,
            LibraryType.NONE_ASSIGNED),

    // Instrument QC - sent by decks to track that the lab has run an Instrument QC (Artel, Leak Test) in
    // otherwise non-messaged Liquid Handler protocols; Analytics to track last QC performed.
    INSTRUMENT_QC("InstrumentQC",
            ExpectSourcesEmpty.FALSE, ExpectTargetsEmpty.TRUE, SystemOfRecord.MERCURY, CreateSources.FALSE,
            PlasticToValidate.SOURCE, PipelineTransformation.NONE, ForwardMessage.NONE, VolumeConcUpdate.MERCURY_ONLY,
            LibraryType.NONE_ASSIGNED),

    DNA_AND_RNA_EXTRACTION_BUCKET("ExtractToDnaAndRnaBucket", ExpectSourcesEmpty.TRUE,
            ExpectTargetsEmpty.TRUE, SystemOfRecord.MERCURY, CreateSources.FALSE, PlasticToValidate.SOURCE,
            PipelineTransformation.NONE, ForwardMessage.NONE, VolumeConcUpdate.MERCURY_ONLY,
            LibraryType.NONE_ASSIGNED),
    DNA_AND_RNA_EXTRACTION_BUCKET_BSP("ExtractToDnaAndRnaBucketBSP", ExpectSourcesEmpty.TRUE,
            ExpectTargetsEmpty.TRUE, SystemOfRecord.MERCURY, CreateSources.FALSE, PlasticToValidate.SOURCE,
            PipelineTransformation.NONE, ForwardMessage.NONE, VolumeConcUpdate.MERCURY_ONLY,
            LibraryType.NONE_ASSIGNED),
    RNA_EXTRACTION_BUCKET("ExtractToRnaBucket", ExpectSourcesEmpty.TRUE,
            ExpectTargetsEmpty.TRUE, SystemOfRecord.MERCURY, CreateSources.FALSE, PlasticToValidate.SOURCE,
            PipelineTransformation.NONE, ForwardMessage.NONE, VolumeConcUpdate.MERCURY_ONLY,
            LibraryType.NONE_ASSIGNED),
    DNA_EXTRACTION_BUCKET("ExtractToDnaBucket", ExpectSourcesEmpty.TRUE,
            ExpectTargetsEmpty.TRUE, SystemOfRecord.MERCURY, CreateSources.FALSE, PlasticToValidate.SOURCE,
            PipelineTransformation.NONE, ForwardMessage.NONE, VolumeConcUpdate.MERCURY_ONLY,
            LibraryType.NONE_ASSIGNED),

    // Transfer blood to micro centrifuge tube
    EXTRACT_BLOOD_TO_MICRO("ExtractBloodToMicro",
            ExpectSourcesEmpty.TRUE, ExpectTargetsEmpty.TRUE, SystemOfRecord.MERCURY, CreateSources.FALSE,
            PlasticToValidate.SOURCE, PipelineTransformation.NONE, ForwardMessage.NONE, VolumeConcUpdate.MERCURY_ONLY,
            new ManualTransferDetails.Builder(MessageType.RECEPTACLE_TRANSFER_EVENT,
                    BarcodedTube.BarcodedTubeType.VacutainerBloodTube3,
                    BarcodedTube.BarcodedTubeType.EppendorfFliptop15,
                    new ReagentRequirements[]{new ReagentRequirements("Proteinase K"),
                            new ReagentRequirements("Buffer AL"),
                            new ReagentRequirements("100% Ethanol")}).
                    build(), LibraryType.NONE_ASSIGNED),
    // Transfer blood to spin column
    EXTRACT_BLOOD_MICRO_TO_SPIN("ExtractBloodMicroToSpin",
            ExpectSourcesEmpty.FALSE, ExpectTargetsEmpty.TRUE, SystemOfRecord.MERCURY, CreateSources.FALSE,
            PlasticToValidate.SOURCE, PipelineTransformation.NONE, ForwardMessage.BSP, VolumeConcUpdate.MERCURY_ONLY,
            new ManualTransferDetails.Builder(MessageType.PLATE_TRANSFER_EVENT,
                    RackOfTubes.RackType.FlipperRackRow8,
                    RackOfTubes.RackType.FlipperRackRow8,
                    new ReagentRequirements[]{new ReagentRequirements("Buffer AW1"),
                            new ReagentRequirements("Buffer AW2"),
                            new ReagentRequirements("Buffer AE")}).
                    sourceBarcodedTubeType(BarcodedTube.BarcodedTubeType.EppendorfFliptop15).
                    targetBarcodedTubeType(BarcodedTube.BarcodedTubeType.SpinColumn).
                    sourceSection(SBSSection.ROWOF8).
                    targetSection(SBSSection.ROWOF8).
                    targetVolume(true).
                    sourceContainerPrefix("WB").
                    useWebCam(true).build(), LibraryType.NONE_ASSIGNED),
    // Transfer blood to matrix tube
    EXTRACT_BLOOD_SPIN_TO_MATRIX("ExtractBloodSpinToMatrix",
            ExpectSourcesEmpty.TRUE, ExpectTargetsEmpty.TRUE, SystemOfRecord.MERCURY, CreateSources.FALSE,
            PlasticToValidate.SOURCE, PipelineTransformation.NONE, ForwardMessage.NONE, VolumeConcUpdate.MERCURY_ONLY,
            new ManualTransferDetails.Builder(MessageType.RECEPTACLE_TRANSFER_EVENT,
                    BarcodedTube.BarcodedTubeType.SpinColumn, BarcodedTube.BarcodedTubeType.MatrixTube075).build(),
            MaterialType.DNA, LibraryType.NONE_ASSIGNED),

    // Transfer cell suspension to microcentrifuge tube
    EXTRACT_CELL_SUSP_TO_MICRO("ExtractCellSuspToMicro",
            ExpectSourcesEmpty.TRUE, ExpectTargetsEmpty.TRUE, SystemOfRecord.MERCURY, CreateSources.FALSE,
            PlasticToValidate.SOURCE, PipelineTransformation.NONE, ForwardMessage.NONE, VolumeConcUpdate.MERCURY_ONLY,
            new ManualTransferDetails.Builder(MessageType.RECEPTACLE_TRANSFER_EVENT,
                    BarcodedTube.BarcodedTubeType.Cryovial05, BarcodedTube.BarcodedTubeType.EppendorfFliptop15,
                    new ReagentRequirements[]{new ReagentRequirements("Proteinase K"),
                            new ReagentRequirements("Buffer AL"),
                            new ReagentRequirements("100% Ethanol")}).
                    build(), LibraryType.NONE_ASSIGNED),
    // Transfer cell suspension to spin column
    EXTRACT_CELL_SUSP_MICRO_TO_SPIN("ExtractCellSuspMicroToSpin",
            ExpectSourcesEmpty.TRUE, ExpectTargetsEmpty.TRUE, SystemOfRecord.MERCURY, CreateSources.FALSE,
            PlasticToValidate.SOURCE, PipelineTransformation.NONE, ForwardMessage.NONE, VolumeConcUpdate.MERCURY_ONLY,
            new ManualTransferDetails.Builder(MessageType.RECEPTACLE_TRANSFER_EVENT,
                    BarcodedTube.BarcodedTubeType.EppendorfFliptop15, BarcodedTube.BarcodedTubeType.SpinColumn,
                    new ReagentRequirements[]{new ReagentRequirements("Buffer AW1"),
                            new ReagentRequirements("Buffer AW2"),
                            new ReagentRequirements("Buffer AE")}).
                    build(), LibraryType.NONE_ASSIGNED),
    // Optional Transfer cell suspension to micro centrifuge tube
    EXTRACT_CELL_SUSP_SPIN_TO_MICRO("ExtractCellSuspSpinToMicro",
            ExpectSourcesEmpty.TRUE, ExpectTargetsEmpty.TRUE, SystemOfRecord.MERCURY, CreateSources.FALSE,
            PlasticToValidate.SOURCE, PipelineTransformation.NONE, ForwardMessage.NONE, VolumeConcUpdate.MERCURY_ONLY,
            new ManualTransferDetails.Builder(MessageType.RECEPTACLE_TRANSFER_EVENT,
                    BarcodedTube.BarcodedTubeType.SpinColumn, BarcodedTube.BarcodedTubeType.EppendorfFliptop15,
                    new ReagentRequirements[]{new ReagentRequirements("SPRI"),
                            new ReagentRequirements("70% Ethanol"),
                            new ReagentRequirements("TE")}).
                    build(), LibraryType.NONE_ASSIGNED),
    // Transfer cell suspension to matrix tube
    EXTRACT_CELL_SUSP_TO_MATRIX("ExtractCellSuspToMatrix",
            ExpectSourcesEmpty.TRUE, ExpectTargetsEmpty.TRUE, SystemOfRecord.MERCURY, CreateSources.FALSE,
            PlasticToValidate.SOURCE, PipelineTransformation.NONE, ForwardMessage.NONE, VolumeConcUpdate.MERCURY_ONLY,
            new ManualTransferDetails.Builder(MessageType.RECEPTACLE_TRANSFER_EVENT,
                    BarcodedTube.BarcodedTubeType.SpinColumn, BarcodedTube.BarcodedTubeType.MatrixTube075).build(),
            MaterialType.DNA, LibraryType.NONE_ASSIGNED),

    // Transfer tissue in paraffin to micro centrifuge tube
    EXTRACT_FFPE_TO_MICRO1("ExtractFfpeToMicro1",
            ExpectSourcesEmpty.TRUE, ExpectTargetsEmpty.TRUE, SystemOfRecord.MERCURY, CreateSources.FALSE,
            PlasticToValidate.SOURCE, PipelineTransformation.NONE, ForwardMessage.NONE, VolumeConcUpdate.MERCURY_ONLY,
            new ManualTransferDetails.Builder(MessageType.RECEPTACLE_TRANSFER_EVENT,
                    BarcodedTube.BarcodedTubeType.Slide, BarcodedTube.BarcodedTubeType.EppendorfFliptop15,
                    new ReagentRequirements[]{new ReagentRequirements("Deparaffinization Solution"),
                            new ReagentRequirements("Buffer ATL"),
                            new ReagentRequirements("Proteinase K")}).
                    build(), LibraryType.NONE_ASSIGNED),
    // Transfer tissue in paraffin to micro centrifuge tube to micro centrifuge tube
    EXTRACT_FFPE_MICRO1_TO_MICRO2("ExtractFfpeMicro1ToMicro2",
            ExpectSourcesEmpty.TRUE, ExpectTargetsEmpty.TRUE, SystemOfRecord.MERCURY, CreateSources.FALSE,
            PlasticToValidate.SOURCE, PipelineTransformation.NONE, ForwardMessage.NONE, VolumeConcUpdate.MERCURY_ONLY,
            new ManualTransferDetails.Builder(MessageType.RECEPTACLE_TRANSFER_EVENT,
                    BarcodedTube.BarcodedTubeType.EppendorfFliptop15, BarcodedTube.BarcodedTubeType.EppendorfFliptop15,
                    new ReagentRequirements[]{new ReagentRequirements("RNase A"),
                            new ReagentRequirements("Buffer AL"),
                            new ReagentRequirements("100% Ethanol")}).
                    build(), LibraryType.NONE_ASSIGNED),
    // Transfer tissue in paraffin to spin column
    EXTRACT_FFPE_MICRO2_TO_SPIN("ExtractFfpeMicro2ToSpin",
            ExpectSourcesEmpty.TRUE, ExpectTargetsEmpty.TRUE, SystemOfRecord.MERCURY, CreateSources.FALSE,
            PlasticToValidate.SOURCE, PipelineTransformation.NONE, ForwardMessage.NONE, VolumeConcUpdate.MERCURY_ONLY,
            new ManualTransferDetails.Builder(MessageType.RECEPTACLE_TRANSFER_EVENT,
                    BarcodedTube.BarcodedTubeType.EppendorfFliptop15, BarcodedTube.BarcodedTubeType.SpinColumn,
                    new ReagentRequirements[]{new ReagentRequirements("Buffer AW1"),
                            new ReagentRequirements("Buffer AW2"),
                            new ReagentRequirements("Buffer ATE")}).
                    build(), LibraryType.NONE_ASSIGNED),
    // Transfer tissue in paraffin to matrix tube
    EXTRACT_FFPE_SPIN_TO_MATRIX("ExtractFfpeSpinToMatrix",
            ExpectSourcesEmpty.TRUE, ExpectTargetsEmpty.TRUE, SystemOfRecord.MERCURY, CreateSources.FALSE,
            PlasticToValidate.SOURCE, PipelineTransformation.NONE, ForwardMessage.NONE, VolumeConcUpdate.MERCURY_ONLY,
            new ManualTransferDetails.Builder(MessageType.RECEPTACLE_TRANSFER_EVENT,
                    BarcodedTube.BarcodedTubeType.SpinColumn, BarcodedTube.BarcodedTubeType.MatrixTube075).build(),
            MaterialType.DNA, LibraryType.NONE_ASSIGNED),

    // Transfer fresh frozen tissue to micro centrifuge tube
    EXTRACT_FRESH_TISSUE_TO_MICRO("ExtractFreshTissueToMicro",
            ExpectSourcesEmpty.TRUE, ExpectTargetsEmpty.TRUE, SystemOfRecord.MERCURY, CreateSources.FALSE,
            PlasticToValidate.SOURCE, PipelineTransformation.NONE, ForwardMessage.NONE, VolumeConcUpdate.MERCURY_ONLY,
            new ManualTransferDetails.Builder(MessageType.RECEPTACLE_TRANSFER_EVENT,
                    BarcodedTube.BarcodedTubeType.TissueCassette, BarcodedTube.BarcodedTubeType.EppendorfFliptop15,
                    new ReagentRequirements[]{new ReagentRequirements("Buffer ATL"),
                            new ReagentRequirements("Proteinase K"),
                            new ReagentRequirements("RNase"),
                            new ReagentRequirements("Buffer AL"),
                            new ReagentRequirements("Ethanol")}).
                    build(), LibraryType.NONE_ASSIGNED),
    // Transfer fresh frozen tissue to spin column
    EXTRACT_FRESH_TISSUE_MICRO_TO_SPIN("ExtractFreshTissueMicroToSpin",
            ExpectSourcesEmpty.TRUE, ExpectTargetsEmpty.TRUE, SystemOfRecord.MERCURY, CreateSources.FALSE,
            PlasticToValidate.SOURCE, PipelineTransformation.NONE, ForwardMessage.NONE, VolumeConcUpdate.MERCURY_ONLY,
            new ManualTransferDetails.Builder(MessageType.RECEPTACLE_TRANSFER_EVENT,
                    BarcodedTube.BarcodedTubeType.EppendorfFliptop15, BarcodedTube.BarcodedTubeType.SpinColumn,
                    new ReagentRequirements[]{new ReagentRequirements("Buffer AW1"),
                            new ReagentRequirements("Buffer AW2"),
                            new ReagentRequirements("Buffer AE")}).
                    build(), LibraryType.NONE_ASSIGNED),
    // Transfer fresh frozen tissue to matrix tube
    EXTRACT_FRESH_TISSUE_SPIN_TO_MATRIX("ExtractFreshTissueSpinToMatrix",
            ExpectSourcesEmpty.TRUE, ExpectTargetsEmpty.TRUE, SystemOfRecord.MERCURY, CreateSources.FALSE,
            PlasticToValidate.SOURCE, PipelineTransformation.NONE, ForwardMessage.NONE, VolumeConcUpdate.MERCURY_ONLY,
            new ManualTransferDetails.Builder(MessageType.RECEPTACLE_TRANSFER_EVENT,
                    BarcodedTube.BarcodedTubeType.SpinColumn, BarcodedTube.BarcodedTubeType.MatrixTube075).build(),
            MaterialType.DNA, LibraryType.NONE_ASSIGNED),

    // Transfer saliva to conical tube
    EXTRACT_SALIVA_TO_CONICAL("ExtractSalivaToConical",
            ExpectSourcesEmpty.TRUE, ExpectTargetsEmpty.TRUE, SystemOfRecord.MERCURY, CreateSources.FALSE,
            PlasticToValidate.SOURCE, PipelineTransformation.NONE, ForwardMessage.NONE, VolumeConcUpdate.MERCURY_ONLY,
            new ManualTransferDetails.Builder(MessageType.RECEPTACLE_TRANSFER_EVENT,
                    BarcodedTube.BarcodedTubeType.OrageneTube, BarcodedTube.BarcodedTubeType.Conical50,
                    new ReagentRequirements[]{new ReagentRequirements("Proteinase K"),
                            new ReagentRequirements("Buffer AL"),
                            new ReagentRequirements("100% Ethanol")}).
                    build(), LibraryType.NONE_ASSIGNED),
    // Transfer saliva to spin column
    EXTRACT_SALIVA_CONICAL_TO_SPIN("ExtractSalivaConicalToSpin",
            ExpectSourcesEmpty.TRUE, ExpectTargetsEmpty.TRUE, SystemOfRecord.MERCURY, CreateSources.FALSE,
            PlasticToValidate.SOURCE, PipelineTransformation.NONE, ForwardMessage.NONE, VolumeConcUpdate.MERCURY_ONLY,
            new ManualTransferDetails.Builder(MessageType.RECEPTACLE_TRANSFER_EVENT,
                    BarcodedTube.BarcodedTubeType.Conical50, BarcodedTube.BarcodedTubeType.SpinColumn,
                    new ReagentRequirements[]{new ReagentRequirements("Buffer AW1"),
                            new ReagentRequirements("Buffer AW2"),
                            new ReagentRequirements("Buffer AE")}).
                    build(), LibraryType.NONE_ASSIGNED),
    // Transfer saliva to matrix tube
    EXTRACT_SALIVA_SPIN_TO_MATRIX("ExtractSalivaSpinToMatrix",
            ExpectSourcesEmpty.TRUE, ExpectTargetsEmpty.TRUE, SystemOfRecord.MERCURY, CreateSources.FALSE,
            PlasticToValidate.SOURCE, PipelineTransformation.NONE, ForwardMessage.NONE, VolumeConcUpdate.MERCURY_ONLY,
            new ManualTransferDetails.Builder(MessageType.RECEPTACLE_TRANSFER_EVENT,
                    BarcodedTube.BarcodedTubeType.SpinColumn, BarcodedTube.BarcodedTubeType.MatrixTube075).build(),
            MaterialType.DNA, LibraryType.NONE_ASSIGNED),

    RNA_CALIPER_SETUP("RNACaliperSetup",
            ExpectSourcesEmpty.TRUE, ExpectTargetsEmpty.TRUE, SystemOfRecord.MERCURY, CreateSources.FALSE,
            PlasticToValidate.SOURCE, PipelineTransformation.NONE, ForwardMessage.NONE, VolumeConcUpdate.MERCURY_ONLY,
            LibraryType.NONE_ASSIGNED),

    //Infinium
    INFINIUM_BUCKET("InfiniumBucket",
            ExpectSourcesEmpty.TRUE, ExpectTargetsEmpty.TRUE, SystemOfRecord.MERCURY, CreateSources.FALSE,
            PlasticToValidate.SOURCE, PipelineTransformation.NONE, ForwardMessage.NONE, VolumeConcUpdate.MERCURY_ONLY,
            LibraryType.NONE_ASSIGNED),
    INFINIUM_METHYLATION_ZYMO_TRANSFER("InfiniumMethylationZymoTransferElution",
            ExpectSourcesEmpty.FALSE, ExpectTargetsEmpty.TRUE, SystemOfRecord.MERCURY, CreateSources.FALSE,
            PlasticToValidate.SOURCE, PipelineTransformation.NONE, ForwardMessage.NONE, VolumeConcUpdate.MERCURY_ONLY,
            LibraryType.NONE_ASSIGNED),
    INFINIUM_METHYLATION_BUFFER_ADDITION_1("InfiniumMethylationBufferAddition1",
            ExpectSourcesEmpty.TRUE, ExpectTargetsEmpty.TRUE, SystemOfRecord.MERCURY, CreateSources.FALSE,
            PlasticToValidate.SOURCE, PipelineTransformation.NONE, ForwardMessage.NONE, VolumeConcUpdate.MERCURY_ONLY,
            LibraryType.NONE_ASSIGNED),
    INFINIUM_METHYLATION_BUFFER_ADDITION_2("InfiniumMethylationBufferAddition2",
            ExpectSourcesEmpty.TRUE, ExpectTargetsEmpty.TRUE, SystemOfRecord.MERCURY, CreateSources.FALSE,
            PlasticToValidate.SOURCE, PipelineTransformation.NONE, ForwardMessage.NONE, VolumeConcUpdate.MERCURY_ONLY,
            LibraryType.NONE_ASSIGNED),
    INFINIUM_METHYLATION_FILTER_PLATE_TRANSFER("InfiniumMethylationFilterPlateTransfer",
            ExpectSourcesEmpty.FALSE, ExpectTargetsEmpty.TRUE, SystemOfRecord.MERCURY, CreateSources.FALSE,
            PlasticToValidate.SOURCE, PipelineTransformation.NONE, ForwardMessage.NONE, VolumeConcUpdate.MERCURY_ONLY,
            LibraryType.NONE_ASSIGNED),
    INFINIUM_METHYLATION_WASH_1("InfiniumMethylationWash1",
            ExpectSourcesEmpty.TRUE, ExpectTargetsEmpty.TRUE, SystemOfRecord.MERCURY, CreateSources.FALSE,
            PlasticToValidate.SOURCE, PipelineTransformation.NONE, ForwardMessage.NONE, VolumeConcUpdate.MERCURY_ONLY,
            LibraryType.NONE_ASSIGNED),
    INFINIUM_METHYLATION_DESULPHONATION("InfiniumMethylationDesulphonation",
            ExpectSourcesEmpty.TRUE, ExpectTargetsEmpty.TRUE, SystemOfRecord.MERCURY, CreateSources.FALSE,
            PlasticToValidate.SOURCE, PipelineTransformation.NONE, ForwardMessage.NONE, VolumeConcUpdate.MERCURY_ONLY,
            LibraryType.NONE_ASSIGNED),
    INFINIUM_METHYLATION_WASH_2("InfiniumMethylationWash2",
            ExpectSourcesEmpty.TRUE, ExpectTargetsEmpty.TRUE, SystemOfRecord.MERCURY, CreateSources.FALSE,
            PlasticToValidate.SOURCE, PipelineTransformation.NONE, ForwardMessage.NONE, VolumeConcUpdate.MERCURY_ONLY,
            LibraryType.NONE_ASSIGNED),
    INFINIUM_METHYLATION_WASH_3("InfiniumMethylationWash3",
            ExpectSourcesEmpty.TRUE, ExpectTargetsEmpty.TRUE, SystemOfRecord.MERCURY, CreateSources.FALSE,
            PlasticToValidate.SOURCE, PipelineTransformation.NONE, ForwardMessage.NONE, VolumeConcUpdate.MERCURY_ONLY,
            LibraryType.NONE_ASSIGNED),
    INFINIUM_METHYLATION_ELUTION("InfiniumMethylationElution",
            ExpectSourcesEmpty.FALSE, ExpectTargetsEmpty.TRUE, SystemOfRecord.MERCURY, CreateSources.FALSE,
            PlasticToValidate.SOURCE, PipelineTransformation.NONE, ForwardMessage.NONE, VolumeConcUpdate.MERCURY_ONLY,
            LibraryType.NONE_ASSIGNED),
    INFINIUM_AMPLIFICATION("InfiniumAmplification",
            ExpectSourcesEmpty.FALSE, ExpectTargetsEmpty.TRUE, SystemOfRecord.MERCURY, CreateSources.FALSE,
            PlasticToValidate.SOURCE, PipelineTransformation.NONE, ForwardMessage.GAP, VolumeConcUpdate.MERCURY_ONLY,
            LibraryType.NONE_ASSIGNED),
    INFINIUM_AMPLIFICATION_REAGENT_ADDITION("InfiniumAmplificationReagentAddition",
            ExpectSourcesEmpty.TRUE, ExpectTargetsEmpty.TRUE, SystemOfRecord.MERCURY, CreateSources.FALSE,
            PlasticToValidate.SOURCE, PipelineTransformation.NONE, ForwardMessage.GAP, VolumeConcUpdate.MERCURY_ONLY,
            LibraryType.NONE_ASSIGNED),
    INFINIUM_FRAGMENTATION("InfiniumFragmentation",
            ExpectSourcesEmpty.TRUE, ExpectTargetsEmpty.TRUE, SystemOfRecord.MERCURY, CreateSources.FALSE,
            PlasticToValidate.SOURCE, PipelineTransformation.NONE, ForwardMessage.GAP, VolumeConcUpdate.MERCURY_ONLY,
            LibraryType.NONE_ASSIGNED),
    INFINIUM_POST_FRAGMENTATION_HYB_OVEN_LOADED("InfiniumPostFragmentationHybOvenLoaded",
            ExpectSourcesEmpty.FALSE, ExpectTargetsEmpty.TRUE, SystemOfRecord.MERCURY, CreateSources.FALSE,
            PlasticToValidate.SOURCE, PipelineTransformation.NONE, ForwardMessage.GAP, VolumeConcUpdate.MERCURY_ONLY,
            LibraryType.NONE_ASSIGNED),
    INFINIUM_PRECIPITATION("InfiniumPrecipitation",
            ExpectSourcesEmpty.TRUE, ExpectTargetsEmpty.TRUE, SystemOfRecord.MERCURY, CreateSources.FALSE,
            PlasticToValidate.SOURCE, PipelineTransformation.NONE, ForwardMessage.GAP, VolumeConcUpdate.MERCURY_ONLY,
            LibraryType.NONE_ASSIGNED),
    INFINIUM_POST_PRECIPITATION_HEAT_BLOCK_LOADED("InfiniumPostPrecipitationHeatBlockLoaded",
            ExpectSourcesEmpty.FALSE, ExpectTargetsEmpty.TRUE, SystemOfRecord.MERCURY, CreateSources.FALSE,
            PlasticToValidate.SOURCE, PipelineTransformation.NONE, ForwardMessage.GAP, VolumeConcUpdate.MERCURY_ONLY,
            LibraryType.NONE_ASSIGNED),
    INFINIUM_PRECIPITATION_ISOPROPANOL_ADDITION("InfiniumPrecipitationIsopropanolAddition",
            ExpectSourcesEmpty.FALSE, ExpectTargetsEmpty.TRUE, SystemOfRecord.MERCURY, CreateSources.FALSE,
            PlasticToValidate.SOURCE, PipelineTransformation.NONE, ForwardMessage.GAP, VolumeConcUpdate.MERCURY_ONLY,
            LibraryType.NONE_ASSIGNED),
    INFINIUM_RESUSPENSION("InfiniumResuspension",
            ExpectSourcesEmpty.FALSE, ExpectTargetsEmpty.TRUE, SystemOfRecord.MERCURY, CreateSources.FALSE,
            PlasticToValidate.SOURCE, PipelineTransformation.NONE, ForwardMessage.GAP, VolumeConcUpdate.MERCURY_ONLY,
            LibraryType.NONE_ASSIGNED),
    INFINIUM_POST_RESUSPENSION_HYB_OVEN("InfiniumPostResuspensionHybOven",
            ExpectSourcesEmpty.FALSE, ExpectTargetsEmpty.TRUE, SystemOfRecord.MERCURY, CreateSources.FALSE,
            PlasticToValidate.SOURCE, PipelineTransformation.NONE, ForwardMessage.GAP, VolumeConcUpdate.MERCURY_ONLY,
            LibraryType.NONE_ASSIGNED),
    INFINIUM_HYBRIDIZATION("InfiniumHybridization",
            ExpectSourcesEmpty.FALSE, ExpectTargetsEmpty.TRUE, SystemOfRecord.MERCURY, CreateSources.FALSE,
            PlasticToValidate.SOURCE, PipelineTransformation.NONE, ForwardMessage.GAP, VolumeConcUpdate.MERCURY_ONLY,
            LibraryType.NONE_ASSIGNED),
    INFINIUM_POST_HYBRIDIZATION_HYB_OVEN_LOADED("InfiniumPostHybridizationHybOvenLoaded",
            ExpectSourcesEmpty.FALSE, ExpectTargetsEmpty.TRUE, SystemOfRecord.MERCURY, CreateSources.FALSE,
            PlasticToValidate.SOURCE, PipelineTransformation.NONE, ForwardMessage.GAP, VolumeConcUpdate.MERCURY_ONLY,
            LibraryType.NONE_ASSIGNED),
    INFINIUM_HYB_CHAMBER_LOADED("InfiniumHybChamberLoaded",
            ExpectSourcesEmpty.FALSE, ExpectTargetsEmpty.TRUE, SystemOfRecord.MERCURY, CreateSources.FALSE,
            PlasticToValidate.SOURCE, PipelineTransformation.NONE, ForwardMessage.GAP, VolumeConcUpdate.MERCURY_ONLY,
            LibraryType.NONE_ASSIGNED),
    INFINIUM_XSTAIN("InfiniumXStain",
            ExpectSourcesEmpty.FALSE, ExpectTargetsEmpty.TRUE, SystemOfRecord.MERCURY, CreateSources.FALSE,
            PlasticToValidate.SOURCE, PipelineTransformation.NONE, ForwardMessage.GAP, VolumeConcUpdate.MERCURY_ONLY,
            new ManualTransferDetails.Builder(MessageType.PLATE_EVENT, null, StaticPlate.PlateType.InfiniumChip24,
                    new ReagentRequirements[]{new ReagentRequirements("LX1", Pattern.compile("wg.*\\d+.*(-lx1|-LX1)\\b"), 6, false),
                            new ReagentRequirements("LX2", Pattern.compile("wg.*\\d+.*(-lx2|-LX2)\\b"), 6, false),
                            new ReagentRequirements("EML", Pattern.compile("wg.*\\d+.*(-eml|-EML)\\b"), 6, false),
                            new ReagentRequirements("SML", Pattern.compile("wg.*\\d+.*(-sml|-SML)\\b"), 6, false),
                            new ReagentRequirements("ATM", Pattern.compile("wg.*\\d+.*(-atm|-ATM)\\b"), 6, false),
                            new ReagentRequirements("RA1", Pattern.compile("wg.*\\d+.*(-ra1|-RA1)\\b"), 1, false),
                            new ReagentRequirements("XC3", Pattern.compile("wg.*\\d+.*(-xc3|-XC3)\\b"), 1, false),
                            new ReagentRequirements("XC4", Pattern.compile("wg.*\\d+.*(-xc4|-XC4)\\b"), 1, false),
                            new ReagentRequirements("PB1", Pattern.compile("wg.*\\d+.*(-pb1|-PB1)\\b"), 2, false),
                            new ReagentRequirements("PB2", Pattern.compile("wg.*\\d+.*(-pb2|-PB2)\\b"), 1, false),
                            new ReagentRequirements("FORM20EDTA25", Pattern.compile("\\d{2}[a-zA-Z]\\d{2}[a-zA-Z]\\d{4}"), 1, true),
                            new ReagentRequirements("ETOH", Pattern.compile("\\d{2}[a-zA-Z]\\d{2}[a-zA-Z]\\d{4}"), 1, true)}).
            numEvents(24).machineNames(new String[]{"None", "Rose", "Lily", "Scrappy"}).build(), LibraryType.NONE_ASSIGNED),
    INFINIUM_XSTAIN_HD("InfiniumXStainHD",
            ExpectSourcesEmpty.FALSE, ExpectTargetsEmpty.TRUE, SystemOfRecord.MERCURY, CreateSources.FALSE,
            PlasticToValidate.SOURCE, PipelineTransformation.NONE, ForwardMessage.GAP, VolumeConcUpdate.MERCURY_ONLY,
            new ManualTransferDetails.Builder(MessageType.PLATE_EVENT, null, StaticPlate.PlateType.InfiniumChip24,
                    new ReagentRequirements[]{new ReagentRequirements("XC1", Pattern.compile("wg.*\\d+.*(-xc1|-XC1)\\b"), 6, false),
                            new ReagentRequirements("XC2", Pattern.compile("wg.*\\d+.*(-xc2|-XC2)\\b"), 6, false),
                            new ReagentRequirements("TEM", Pattern.compile("wg.*\\d+.*(-tem|-TEM)\\b"), 6, false),
                            new ReagentRequirements("STM", Pattern.compile("wg.*\\d+.*(-stm|-STM)\\b"), 6, false),
                            new ReagentRequirements("ATM", Pattern.compile("wg.*\\d+.*(-atm|-ATM)\\b"), 6, false),
                            new ReagentRequirements("RA1", Pattern.compile("wg.*\\d+.*(-ra1|-RA1)\\b"), 1, false),
                            new ReagentRequirements("XC3", Pattern.compile("wg.*\\d+.*(-xc3|-XC3)\\b"), 1, false),
                            new ReagentRequirements("XC4", Pattern.compile("wg.*\\d+.*(-xc4|-XC4)\\b"), 1, false),
                            new ReagentRequirements("PB1", Pattern.compile("wg.*\\d+.*(-pb1|-PB1)\\b"), 2, false),
                            new ReagentRequirements("PB2", Pattern.compile("wg.*\\d+.*(-pb2|-PB2)\\b"), 1, false),
                            new ReagentRequirements("FORM20EDTA25", Pattern.compile("\\d{2}[a-zA-Z]\\d{2}[a-zA-Z]\\d{4}"), 1, true),
                            new ReagentRequirements("ETOH", Pattern.compile("\\d{2}[a-zA-Z]\\d{2}[a-zA-Z]\\d{4}"), 1, true)}).
            numEvents(24).machineNames(new String[]{"None", "Rose", "Lily", "Scrappy"}).build(), LibraryType.NONE_ASSIGNED),
    INFINIUM_AUTOCALL_SOME_STARTED("InfiniumAutocallSomeStarted",
            ExpectSourcesEmpty.FALSE, ExpectTargetsEmpty.TRUE, SystemOfRecord.MERCURY, CreateSources.FALSE,
            PlasticToValidate.SOURCE, PipelineTransformation.NONE, ForwardMessage.NONE, VolumeConcUpdate.MERCURY_ONLY,
            LibraryType.NONE_ASSIGNED),
    INFINIUM_AUTOCALL_ALL_STARTED("InfiniumAutoCallAllStarted",
            ExpectSourcesEmpty.FALSE, ExpectTargetsEmpty.TRUE, SystemOfRecord.MERCURY, CreateSources.FALSE,
            PlasticToValidate.SOURCE, PipelineTransformation.NONE, ForwardMessage.NONE, VolumeConcUpdate.MERCURY_ONLY,
            LibraryType.NONE_ASSIGNED),
    INFINIUM_ARCHIVED("InfiniumArchived",
            ExpectSourcesEmpty.FALSE, ExpectTargetsEmpty.TRUE, SystemOfRecord.MERCURY, CreateSources.FALSE,
            PlasticToValidate.SOURCE, PipelineTransformation.NONE, ForwardMessage.NONE, VolumeConcUpdate.MERCURY_ONLY,
            LibraryType.NONE_ASSIGNED),

    // FP
    FP_PCR_1("FP_PCR1",
            ExpectSourcesEmpty.FALSE, ExpectTargetsEmpty.TRUE, SystemOfRecord.MERCURY, CreateSources.FALSE,
            PlasticToValidate.SOURCE, PipelineTransformation.NONE, ForwardMessage.NONE, VolumeConcUpdate.MERCURY_ONLY,
            LibraryType.NONE_ASSIGNED),
    FP_INDEX_PRIMER_TRANSFER("FP_IndexPrimerTransfer",
            ExpectSourcesEmpty.FALSE, ExpectTargetsEmpty.TRUE, SystemOfRecord.MERCURY, CreateSources.FALSE,
            PlasticToValidate.SOURCE, PipelineTransformation.NONE, ForwardMessage.NONE, VolumeConcUpdate.MERCURY_ONLY,
            LibraryType.NONE_ASSIGNED),
    FP_PCR_TAIL_2("FP_PCRTail2",
            ExpectSourcesEmpty.FALSE, ExpectTargetsEmpty.FALSE, SystemOfRecord.MERCURY, CreateSources.FALSE,
            PlasticToValidate.SOURCE, PipelineTransformation.NONE, ForwardMessage.NONE, VolumeConcUpdate.MERCURY_ONLY,
            LibraryType.NONE_ASSIGNED),
    FP_PRE_SPRI_POOLING("FP_PreSPRIPooling",
            ExpectSourcesEmpty.FALSE, ExpectTargetsEmpty.TRUE, SystemOfRecord.MERCURY, CreateSources.FALSE,
            PlasticToValidate.SOURCE, PipelineTransformation.NONE, ForwardMessage.NONE, VolumeConcUpdate.MERCURY_ONLY,
            LibraryType.NONE_ASSIGNED),
    FP_POOLING_TRANSFER("FP_PoolingTransfer",
            ExpectSourcesEmpty.FALSE, ExpectTargetsEmpty.TRUE, SystemOfRecord.MERCURY, CreateSources.FALSE,
            PlasticToValidate.SOURCE, PipelineTransformation.NONE, ForwardMessage.NONE, VolumeConcUpdate.MERCURY_ONLY,
            LibraryType.NONE_ASSIGNED),

    // Single Cell
    SINGLE_CELL_SMART_SEQ_BUCKET("SingleCellSmartSeqBucket",
            ExpectSourcesEmpty.TRUE, ExpectTargetsEmpty.TRUE, SystemOfRecord.MERCURY, CreateSources.FALSE,
            PlasticToValidate.SOURCE, PipelineTransformation.NONE, ForwardMessage.NONE, VolumeConcUpdate.MERCURY_ONLY,
            LibraryType.NONE_ASSIGNED),
    SINGLE_CELL_SPRI_ADDITION("SingleCellSPRIAddition",
            ExpectSourcesEmpty.TRUE, ExpectTargetsEmpty.TRUE, SystemOfRecord.MERCURY, CreateSources.FALSE,
            PlasticToValidate.SOURCE, PipelineTransformation.NONE, ForwardMessage.NONE, VolumeConcUpdate.MERCURY_ONLY,
            LibraryType.NONE_ASSIGNED),
    SINGLE_CELL_POLY_A("SingleCellPolyA",
            ExpectSourcesEmpty.TRUE, ExpectTargetsEmpty.TRUE, SystemOfRecord.MERCURY, CreateSources.FALSE,
            PlasticToValidate.SOURCE, PipelineTransformation.NONE, ForwardMessage.NONE, VolumeConcUpdate.MERCURY_ONLY,
            LibraryType.NONE_ASSIGNED),
    SINGLE_CELL_RT("SingleCellRT",
            ExpectSourcesEmpty.TRUE, ExpectTargetsEmpty.TRUE, SystemOfRecord.MERCURY, CreateSources.FALSE,
            PlasticToValidate.SOURCE, PipelineTransformation.NONE, ForwardMessage.NONE, VolumeConcUpdate.MERCURY_ONLY,
            LibraryType.NONE_ASSIGNED),
    SINGLE_CELL_WTA("SingleCellWTA",
            ExpectSourcesEmpty.TRUE, ExpectTargetsEmpty.TRUE, SystemOfRecord.MERCURY, CreateSources.FALSE,
            PlasticToValidate.SOURCE, PipelineTransformation.NONE, ForwardMessage.NONE, VolumeConcUpdate.MERCURY_ONLY,
            LibraryType.NONE_ASSIGNED),
    SINGLE_CELL_SPRI("SingleCellWTASpri",
            ExpectSourcesEmpty.TRUE, ExpectTargetsEmpty.TRUE, SystemOfRecord.MERCURY, CreateSources.FALSE,
            PlasticToValidate.SOURCE, PipelineTransformation.NONE, ForwardMessage.NONE, VolumeConcUpdate.MERCURY_ONLY,
            LibraryType.NONE_ASSIGNED),
    SINGLE_CELL_TRANSFER("SingleCellElutionTransfer",
            ExpectSourcesEmpty.FALSE, ExpectTargetsEmpty.TRUE, SystemOfRecord.MERCURY, CreateSources.TRUE,
            PlasticToValidate.SOURCE, PipelineTransformation.NONE, ForwardMessage.NONE, VolumeConcUpdate.MERCURY_ONLY,
            LibraryType.NONE_ASSIGNED),
    SINGLE_CELL_NORMALIZATION("SingleCellNormalization",
            ExpectSourcesEmpty.TRUE, ExpectTargetsEmpty.TRUE, SystemOfRecord.MERCURY, CreateSources.FALSE,
            PlasticToValidate.SOURCE, PipelineTransformation.NONE, ForwardMessage.NONE, VolumeConcUpdate.MERCURY_ONLY,
            LibraryType.NONE_ASSIGNED),
    SINGLE_CELL_TAGMENTATION("SingleCellTagmentation",
            ExpectSourcesEmpty.TRUE, ExpectTargetsEmpty.TRUE, SystemOfRecord.MERCURY, CreateSources.FALSE,
            PlasticToValidate.SOURCE, PipelineTransformation.NONE, ForwardMessage.NONE, VolumeConcUpdate.MERCURY_ONLY,
            LibraryType.NONE_ASSIGNED),
    SINGLE_CELL_STOP_TAGMENTATION("SingleCellStopTagmentation",
            ExpectSourcesEmpty.TRUE, ExpectTargetsEmpty.TRUE, SystemOfRecord.MERCURY, CreateSources.FALSE,
            PlasticToValidate.SOURCE, PipelineTransformation.NONE, ForwardMessage.NONE, VolumeConcUpdate.MERCURY_ONLY,
            LibraryType.NONE_ASSIGNED),
    SINGLE_CELL_INDEX_ADAPTER_LIGATION("SingleCellIndexAdapterLigation",
            ExpectSourcesEmpty.FALSE, ExpectTargetsEmpty.TRUE, SystemOfRecord.MERCURY, CreateSources.FALSE,
            PlasticToValidate.SOURCE, PipelineTransformation.NONE, ForwardMessage.NONE, VolumeConcUpdate.MERCURY_ONLY,
            LibraryType.NONE_ASSIGNED),
    SINGLE_CELL_POOLING("SingleCellPooling",
            ExpectSourcesEmpty.FALSE, ExpectTargetsEmpty.TRUE, SystemOfRecord.MERCURY, CreateSources.FALSE,
            PlasticToValidate.SOURCE, PipelineTransformation.NONE, ForwardMessage.NONE, VolumeConcUpdate.MERCURY_ONLY,
            LibraryType.NONE_ASSIGNED),
    SINGLE_CELL_BULK_SPRI_CLEANUP("SingleCellBulkSPRICleanup",
            ExpectSourcesEmpty.FALSE, ExpectTargetsEmpty.TRUE, SystemOfRecord.MERCURY, CreateSources.FALSE,
            PlasticToValidate.SOURCE, PipelineTransformation.NONE, ForwardMessage.NONE, VolumeConcUpdate.MERCURY_ONLY,
            LibraryType.NONE_ASSIGNED),

    // Single Cell 10X
    SINGLE_CELL_10X_BUCKET("SingleCell10XBucket",
            ExpectSourcesEmpty.TRUE, ExpectTargetsEmpty.TRUE, SystemOfRecord.MERCURY, CreateSources.FALSE,
            PlasticToValidate.SOURCE, PipelineTransformation.NONE, ForwardMessage.NONE, VolumeConcUpdate.MERCURY_ONLY,
            LibraryType.NONE_ASSIGNED),
    SINGLE_CELL_END_REPAIR_ABASE("SingleCellEndRepairABase",
            ExpectSourcesEmpty.TRUE, ExpectTargetsEmpty.TRUE, SystemOfRecord.MERCURY, CreateSources.FALSE,
            PlasticToValidate.SOURCE, PipelineTransformation.NONE, ForwardMessage.NONE, VolumeConcUpdate.MERCURY_ONLY,
            LibraryType.NONE_ASSIGNED),
    SINGLE_CELL_ATAIL_CLEANUP("SingleCellATailCleanup",
            ExpectSourcesEmpty.TRUE, ExpectTargetsEmpty.TRUE, SystemOfRecord.MERCURY, CreateSources.FALSE,
            PlasticToValidate.SOURCE, PipelineTransformation.NONE, ForwardMessage.NONE, VolumeConcUpdate.MERCURY_ONLY,
            LibraryType.NONE_ASSIGNED),
    SINGLE_CELL_ADAPTER_LIGATION("SingleCellAdapterLigation",
            ExpectSourcesEmpty.TRUE, ExpectTargetsEmpty.TRUE, SystemOfRecord.MERCURY, CreateSources.FALSE,
            PlasticToValidate.SOURCE, PipelineTransformation.NONE, ForwardMessage.NONE, VolumeConcUpdate.MERCURY_ONLY,
            LibraryType.NONE_ASSIGNED),
    SINGLE_CELL_LIGATION_CLEANUP("SingleCellLigationCleanup",
            ExpectSourcesEmpty.TRUE, ExpectTargetsEmpty.TRUE, SystemOfRecord.MERCURY, CreateSources.FALSE,
            PlasticToValidate.SOURCE, PipelineTransformation.NONE, ForwardMessage.NONE, VolumeConcUpdate.MERCURY_ONLY,
            LibraryType.NONE_ASSIGNED),
    SINGLE_CELL_INDEX_ADAPTER_PCR("SingleCellIndexAdapterPCR",
            ExpectSourcesEmpty.TRUE, ExpectTargetsEmpty.TRUE, SystemOfRecord.MERCURY, CreateSources.FALSE,
            PlasticToValidate.SOURCE, PipelineTransformation.NONE, ForwardMessage.NONE, VolumeConcUpdate.MERCURY_ONLY,
            LibraryType.NONE_ASSIGNED),
    SINGLE_CELL_DOUBLE_SIDED_CLEANUP("SingleCellDoubleSidedCleanup",
            ExpectSourcesEmpty.TRUE, ExpectTargetsEmpty.TRUE, SystemOfRecord.MERCURY, CreateSources.FALSE,
            PlasticToValidate.SOURCE, PipelineTransformation.NONE, ForwardMessage.NONE, VolumeConcUpdate.MERCURY_ONLY,
            LibraryType.NONE_ASSIGNED),


    // Generic events that are qualified by workflow
    CENTRIFUGE("Centrifuge",
            ExpectSourcesEmpty.TRUE, ExpectTargetsEmpty.TRUE, SystemOfRecord.MERCURY, CreateSources.FALSE,
            PlasticToValidate.SOURCE, PipelineTransformation.NONE, ForwardMessage.NONE, VolumeConcUpdate.MERCURY_ONLY,
            LibraryType.NONE_ASSIGNED),
    INCUBATE("Incubate",
            ExpectSourcesEmpty.TRUE, ExpectTargetsEmpty.TRUE, SystemOfRecord.MERCURY, CreateSources.FALSE,
            PlasticToValidate.SOURCE, PipelineTransformation.NONE, ForwardMessage.NONE, VolumeConcUpdate.MERCURY_ONLY,
            LibraryType.NONE_ASSIGNED),
    MIX("Mix",
            ExpectSourcesEmpty.TRUE, ExpectTargetsEmpty.TRUE, SystemOfRecord.MERCURY, CreateSources.FALSE,
            PlasticToValidate.SOURCE, PipelineTransformation.NONE, ForwardMessage.NONE, VolumeConcUpdate.MERCURY_ONLY,
            LibraryType.NONE_ASSIGNED),
    WASH("Wash",
            ExpectSourcesEmpty.TRUE, ExpectTargetsEmpty.TRUE, SystemOfRecord.MERCURY, CreateSources.FALSE,
            PlasticToValidate.SOURCE, PipelineTransformation.NONE, ForwardMessage.NONE, VolumeConcUpdate.MERCURY_ONLY,
            LibraryType.NONE_ASSIGNED),
    ADD_REAGENT("AddReagent",
            ExpectSourcesEmpty.TRUE, ExpectTargetsEmpty.TRUE, SystemOfRecord.MERCURY, CreateSources.FALSE,
            PlasticToValidate.SOURCE, PipelineTransformation.NONE, ForwardMessage.NONE, VolumeConcUpdate.MERCURY_ONLY,
            LibraryType.NONE_ASSIGNED),
    PREP("Prep",
            ExpectSourcesEmpty.TRUE, ExpectTargetsEmpty.TRUE, SystemOfRecord.MERCURY, CreateSources.FALSE,
            PlasticToValidate.SOURCE, PipelineTransformation.NONE, ForwardMessage.NONE, VolumeConcUpdate.MERCURY_ONLY,
            LibraryType.NONE_ASSIGNED),
    DISCARD("Discard",
            ExpectSourcesEmpty.TRUE, ExpectTargetsEmpty.TRUE, SystemOfRecord.MERCURY, CreateSources.FALSE,
            PlasticToValidate.SOURCE, PipelineTransformation.NONE, ForwardMessage.NONE, VolumeConcUpdate.MERCURY_ONLY,
            LibraryType.NONE_ASSIGNED),
    MOVE("Move",
            ExpectSourcesEmpty.TRUE, ExpectTargetsEmpty.TRUE, SystemOfRecord.MERCURY, CreateSources.FALSE,
            PlasticToValidate.SOURCE, PipelineTransformation.NONE, ForwardMessage.NONE, VolumeConcUpdate.MERCURY_ONLY,
            LibraryType.NONE_ASSIGNED),
    STORE("Store",
            ExpectSourcesEmpty.TRUE, ExpectTargetsEmpty.TRUE, SystemOfRecord.MERCURY, CreateSources.FALSE,
            PlasticToValidate.SOURCE, PipelineTransformation.NONE, ForwardMessage.NONE, VolumeConcUpdate.MERCURY_ONLY,
            LibraryType.NONE_ASSIGNED),
    IN_PLACE("In-Place",
            ExpectSourcesEmpty.TRUE, ExpectTargetsEmpty.TRUE, SystemOfRecord.MERCURY, CreateSources.FALSE,
            PlasticToValidate.SOURCE, PipelineTransformation.NONE, ForwardMessage.NONE, VolumeConcUpdate.MERCURY_ONLY,
            LibraryType.NONE_ASSIGNED),
    TRANSFER("Transfer",
            ExpectSourcesEmpty.FALSE, ExpectTargetsEmpty.TRUE, SystemOfRecord.MERCURY, CreateSources.FALSE,
            PlasticToValidate.SOURCE, PipelineTransformation.NONE, ForwardMessage.NONE, VolumeConcUpdate.MERCURY_ONLY,
            LibraryType.NONE_ASSIGNED),
    EXTRACT_TO_DNA("ExtractToDna",
            ExpectSourcesEmpty.FALSE, ExpectTargetsEmpty.TRUE, SystemOfRecord.MERCURY, CreateSources.FALSE,
            PlasticToValidate.SOURCE, PipelineTransformation.NONE, ForwardMessage.NONE, VolumeConcUpdate.MERCURY_ONLY,
            null, MaterialType.DNA, LibraryType.NONE_ASSIGNED),
    EXTRACT_TO_RNA("ExtractToRna",
            ExpectSourcesEmpty.FALSE, ExpectTargetsEmpty.TRUE, SystemOfRecord.MERCURY, CreateSources.FALSE,
            PlasticToValidate.SOURCE, PipelineTransformation.NONE, ForwardMessage.NONE, VolumeConcUpdate.MERCURY_ONLY,
            null, MaterialType.RNA, LibraryType.NONE_ASSIGNED),

    STORAGE_CHECK_IN("StorageCheckIn",
            ExpectSourcesEmpty.TRUE, ExpectTargetsEmpty.TRUE, SystemOfRecord.MERCURY, CreateSources.FALSE,
            PlasticToValidate.SOURCE, PipelineTransformation.NONE, ForwardMessage.NONE, VolumeConcUpdate.MERCURY_ONLY,
            LibraryType.NONE_ASSIGNED),
    STORAGE_CHECK_OUT("StorageCheckOut",
            ExpectSourcesEmpty.TRUE, ExpectTargetsEmpty.TRUE, SystemOfRecord.MERCURY, CreateSources.FALSE,
            PlasticToValidate.SOURCE, PipelineTransformation.NONE, ForwardMessage.NONE, VolumeConcUpdate.MERCURY_ONLY,
            LibraryType.NONE_ASSIGNED),
    STORAGE_MOVE("StorageMove",
            ExpectSourcesEmpty.TRUE, ExpectTargetsEmpty.TRUE, SystemOfRecord.MERCURY, CreateSources.FALSE,
            PlasticToValidate.SOURCE, PipelineTransformation.NONE, ForwardMessage.NONE, VolumeConcUpdate.MERCURY_ONLY,
            LibraryType.NONE_ASSIGNED),

    SEQ_ONLY_BUCKET("SeqOnlyBucket",
            ExpectSourcesEmpty.FALSE, ExpectTargetsEmpty.TRUE, SystemOfRecord.MERCURY, CreateSources.FALSE,
            PlasticToValidate.SOURCE, PipelineTransformation.NONE, ForwardMessage.NONE, VolumeConcUpdate.MERCURY_ONLY,
            LibraryType.NONE_ASSIGNED),
    ;

    private final String name;

    private final ExpectSourcesEmpty expectedEmptySources;

    private final ExpectTargetsEmpty expectedEmptyTargets;

    private final CreateSources createSources;

    private final MaterialType resultingMaterialType;

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
        BSP_APPLY_SM_IDS,
        GAP,
        NONE
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
        STRIP_TUBE_CHERRY_PICK_EVENT,
        RECEPTACLE_PLATE_TRANSFER_EVENT,
        RECEPTACLE_EVENT,
        RECEPTACLE_TRANSFER_EVENT
    }

    public enum LibraryType {
        ENRICHED_POND("Enriched Pond", "Pond"),
        PCR_FREE_POND("PCR-Free Pond", "Pond"),
        PCR_PLUS_POND("PCR-Plus Pond", "Pond"),
        GERMLINE_POND("Germline Pond", "Pond"),
        CF_POND("CF Pond", "Pond"),
        PCR_PLUS_NORMALIZED_POND("PCR-Plus Norm Pond", "Norm Pond"),
        HYBRID_SELECTION_AGILENT_CATCH("Enriched Catch", "Catch"),
        HYBRID_SELECTION_ICE_ENRICHED_CATCH("Enriched Catch", "Catch"),
        NEXTERA_ENRICHED_LIBRARY("Nextera Enriched", "Pond"),
        NEXTERA_POOLED_NORMALIZED_LIBRARY("Nextera Pooled Normalized", "Pooled"),
        NEXTERA_SPRI_CONCENTRATED_POOL("Nextera SPRI Concentrated Pool", "Pooled"),
        NEXOME_CATCH("Nexome Catch", "Catch"),
        SELECTION_CATCH("Selection Catch", "Catch"),
        POOLED("Pooled"),
        CALIBRATED_POOLED("Calibrated Pooled"),
        MISEQ_FLOWCELL("MiSeq Flowcell"),
        NONE_ASSIGNED(""),
        NORMALIZED("Normalized"),
        DENATURED("Denatured"),
        DILUTED_DENATURE("Diluted Denature"),
        HISEQ_FLOWCELL("HiSeq Flowcell");

        LibraryType( String mercuryDisplayName, String etlDisplayName ){
            this.mercuryDisplayName = mercuryDisplayName;
            this.etlDisplayName = etlDisplayName;
        }

        LibraryType( String displayName ){
            this.mercuryDisplayName = displayName;
            this.etlDisplayName = displayName;
        }

        private final String etlDisplayName;
        private final String mercuryDisplayName;

        public String getEtlDisplayName() {
            return etlDisplayName;
        }

        public String getMercuryDisplayName() {
            return mercuryDisplayName;
        }

    }

    /**
     * How the event expects to handle it's sources (if it is a transfer event).
     */
    public enum SourceHandling {
        DEPLETE("Deplete Well"),                    // Expect to deplete the source(s) of the transfer.
        TERMINATE_DEPLETED("Terminate Depleted"),   // Expected to terminate source(s) if the vol/mass reaches zero due to the transfer.
        NONE("None");                               // No special changes to the source(s) of the transfer.

        String displayName;

        SourceHandling(String displayName) {
            this.displayName = displayName;
        }

        public String getDisplayName() {
            return this.displayName;
        }
    }

    // Holds details regarding each reagent possibly used.
    public static class ReagentRequirements {

        private String reagentName;

        // todo jmt this should be a String, so it can be set from XML
        // XmlAccessType.FIELD and @XmlTransient doesn't work here, still get "does not have a no-arg default constructor"
        private Pattern barcodePattern;

        private int fieldCount = 1;

        private boolean expirationDateIncluded;

        // This constructor is required for xml loading.
        public ReagentRequirements(String reagentName) {
            this(reagentName, 1);
        }

        public ReagentRequirements(String reagentName, int fieldCount) {

            this(reagentName, null, fieldCount, true);
        }

        public ReagentRequirements(String reagentName, Pattern barcodePattern, int fieldCount) {

            this(reagentName, barcodePattern, fieldCount, true);
        }

        public ReagentRequirements(String reagentName, int fieldCount, boolean expirationDateIncluded) {
            this(reagentName, null, fieldCount, expirationDateIncluded);
        }

        public ReagentRequirements(String reagentName, Pattern barcodePattern, int fieldCount, boolean expirationDateIncluded) {
            this.reagentName = reagentName;
            this.barcodePattern = barcodePattern;
            this.fieldCount = fieldCount;
            this.expirationDateIncluded = expirationDateIncluded;
        }

        public ReagentRequirements() {}

        public String getReagentName() {
            return reagentName;
        }

        public void setReagentName(String reagentName) {
            this.reagentName = reagentName;
        }

        public Pattern getBarcodePattern() {
            return barcodePattern;
        }

        public int getFieldCount() {
            return fieldCount;
        }

        public void setFieldCount(int fieldCount) {
            this.fieldCount = fieldCount;
        }

        public boolean isExpirationDateIncluded() {
            return expirationDateIncluded;
        }

        public void setExpirationDateIncluded(boolean expirationDateIncluded) {
            this.expirationDateIncluded = expirationDateIncluded;
        }

        /**
         * Compare the passed barcode to the pattern noted for the reagent (if it exists).
         *
         * @param barcode Barcode scanned
         *
         * @return True if no pattern is set to be checked or if the barcode scanned does match the pattern.
         */
        public boolean verifyBarcode(String barcode) {
            // If the regex field is blank then we aren't verifying the barcode.
            if (getBarcodePattern() == null) {
                return true;
            }

            Matcher matcher = getBarcodePattern().matcher(barcode);
            return matcher.matches();
        }
    }

    private final LibraryType libraryType;

    private String collabSampleSuffix;

    private Metadata.Key metadataKey;

    private String metadataValue;

    private SourceHandling sourceHandling;

    /**
     * Determines what metadata is added to messages forwarded to BSP.
     */
    public enum AddMetadataToBsp {
        PDO
    }

    private AddMetadataToBsp addMetadataToBsp;

    /**
     * Determines whether messages that are forwarded to BSP are translated to a different format.
     */
    public enum TranslateBspMessage {
        /** Translate section-based plate transfer to cherry pick transfer, to give finer grained control over
         * sources and destinations.  This is necessary in some transfers that involve mixtures of Mercury and
         * BSP samples.
         */
        SECTION_TO_CHERRY,
        /** Leave event as is. */
        NONE
    }

    private TranslateBspMessage translateBspMessage = TranslateBspMessage.NONE;

    @XmlAccessorType(XmlAccessType.FIELD)
    public static class ManualTransferDetails {
        /** Determines layout of page and type of message built. */
        private MessageType messageType;

        /** Determines layout of page for source vessel. */
        @XmlTransient
        private VesselTypeGeometry sourceVesselTypeGeometry;

        /** Used by JAXB, because it doesn't support interfaces. */
        private String sourceVesselTypeGeometryString;

        /** The set of positions in the source geometry from which to transfer */
        private SBSSection sourceSection;

        /** Whether to allow entry of source volumes. */
        private boolean sourceVolume;

        /** For containers that don't have barcodes (e.g. flipper racks), the prefix to the synthetic barcode. */
        private String sourceContainerPrefix;

        /** Determines layout of page for destination vessel. */
        @XmlTransient
        private VesselTypeGeometry targetVesselTypeGeometry;

        /** Used by JAXB, because it doesn't support interfaces. */
        private String targetVesselTypeGeometryString;

        /** The set of positions in the target geometry from which to transfer. */
        private SBSSection targetSection;

        /** Whether to allow entry of target volumes. */
        private boolean targetVolume;

        /** Whether to allow entry of target mass. */
        private boolean sourceMassRemoved;

        /** For containers that don't have barcodes (e.g. flipper racks), the prefix to the synthetic barcode. */
        private String targetContainerPrefix;

        /** If true, display error message when target does not exist.  */
        private boolean targetExpectedToExist = false;

        /** If false, display error message when target has transfers.  */
        private boolean targetExpectedEmpty = true;

        /** Allows multiple events to share one set of reagents. */
        private int numEvents = 1;

        /** Prompts user with a list of machines. */
        private String[] machineNames = {};

        /** Prompts user with a list of source vessel geometry types. */
        @XmlTransient
        private VesselTypeGeometry[] sourceVesselTypeGeometries = {};

        /** For source rack geometries, specifies the type of tube in the rack */
        private BarcodedTube.BarcodedTubeType sourceBarcodedTubeType = BarcodedTube.BarcodedTubeType.MatrixTube;

        /** Prompts user with a list of target vessel geometry types. */
        @XmlTransient
        private VesselTypeGeometry[] targetVesselTypeGeometries = {};

        /** For target rack geometries, specifies the type of tube in the rack */
        private BarcodedTube.BarcodedTubeType targetBarcodedTubeType = BarcodedTube.BarcodedTubeType.MatrixTube;

        @XmlTransient
        private VesselTypeGeometry targetWellTypeGeometry;

        /** For Jaxb **/
        private String targetWellTypeString;

        /** Allows a transfer from one source to two destinations */
        private LabEventType secondaryEvent;

        /** Supports verification that two transfers have same source and destination. */
        private LabEventType repeatedEvent;

        /** Supports verification that two transfers have same source and destination. */
        private String repeatedWorkflowQualifier;

        /** What to display in the button that sends the message. */
        private String buttonValue = "Transfer";

        /** If transfer can be filled from a generated LIMs file from automation. */
        private boolean limsFile = false;

        /** True if all sources in a cherry pick must be from one participant. */
        private boolean requireSingleParticipant = false;

        /** If transfer can be filled from scanning a picture via a webcam */
        private boolean useWebCam = false;

        /** Details regarding reagents used. */
        private ReagentRequirements[] reagentRequirements = {};

        @XmlTransient
        private Map<String, ReagentRequirements> mapReagentNameToReagentRequirements;

        /** For JAXB */
        public ManualTransferDetails() {
        }

        @SuppressWarnings("AccessingNonPublicFieldOfAnotherObject")
        public ManualTransferDetails(Builder builder) {
            messageType = builder.messageType;
            sourceVesselTypeGeometry = builder.sourceVesselTypeGeometry;
            sourceBarcodedTubeType = builder.sourceBarcodedTubeType;
            sourceSection = builder.sourceSection;
            sourceVolume = builder.sourceVolume;
            sourceContainerPrefix = builder.sourceContainerPrefix;
            targetVesselTypeGeometry = builder.targetVesselTypeGeometry;
            targetBarcodedTubeType = builder.targetBarcodedTubeType;
            targetSection = builder.targetSection;
            targetVolume = builder.targetVolume;
            sourceMassRemoved = builder.sourceMassRemoved;
            targetContainerPrefix = builder.targetContainerPrefix;
            numEvents = builder.numEvents;
            machineNames = builder.machineNames;
            secondaryEvent = builder.secondaryEvent;
            repeatedEvent = builder.repeatedEvent;
            repeatedWorkflowQualifier = builder.repeatedWorkflowQualifier;
            buttonValue = builder.buttonValue;
            targetExpectedToExist = builder.targetExpectedToExist;
            targetExpectedEmpty = builder.targetExpectedEmpty;
            sourceVesselTypeGeometries = builder.sourceVesselTypeGeometries;
            targetVesselTypeGeometries = builder.targetVesselTypeGeometries;
            limsFile = builder.limsFile;
            requireSingleParticipant = builder.requireSingleParticipant;
            useWebCam = builder.useWebCam;
            targetWellTypeGeometry = builder.targetWellTypeGeometry;
            reagentRequirements = builder.reagentRequirements;
        }

        public static class Builder {
            private final MessageType messageType;
            private final VesselTypeGeometry sourceVesselTypeGeometry;
            private final VesselTypeGeometry targetVesselTypeGeometry;
            private VesselTypeGeometry targetWellTypeGeometry;

            private SBSSection sourceSection;
            private SBSSection targetSection;
            private int numEvents = 1;
            private String[] machineNames = {};
            private VesselTypeGeometry[] sourceVesselTypeGeometries = {};
            private BarcodedTube.BarcodedTubeType sourceBarcodedTubeType;
            private boolean sourceVolume;
            private String sourceContainerPrefix;
            private VesselTypeGeometry[] targetVesselTypeGeometries = {};
            private BarcodedTube.BarcodedTubeType targetBarcodedTubeType;
            private boolean targetVolume;
            private boolean sourceMassRemoved;
            private String targetContainerPrefix;
            private LabEventType secondaryEvent;
            private LabEventType repeatedEvent;
            private String repeatedWorkflowQualifier;
            private String buttonValue = "Transfer";
            private boolean targetExpectedToExist = false;
            private boolean targetExpectedEmpty = true;
            private boolean limsFile = false;
            private boolean useWebCam = false;
            private boolean requireSingleParticipant = false;
            private ReagentRequirements[] reagentRequirements = {};

            public Builder(MessageType messageType, VesselTypeGeometry sourceVesselTypeGeometry,
                           VesselTypeGeometry targetVesselTypeGeometry, ReagentRequirements[] reagentRequirements) {
                this(messageType, sourceVesselTypeGeometry, targetVesselTypeGeometry);

                this.reagentRequirements = reagentRequirements;
            }

            public Builder(MessageType messageType, VesselTypeGeometry sourceVesselTypeGeometry,
                    VesselTypeGeometry targetVesselTypeGeometry) {
                this.messageType = messageType;
                this.sourceVesselTypeGeometry = sourceVesselTypeGeometry;
                this.targetVesselTypeGeometry = targetVesselTypeGeometry;
            }

            public Builder sourceSection(SBSSection sourceSection) {
                this.sourceSection = sourceSection;
                return this;
            }

            public Builder targetSection(SBSSection targetSection) {
                this.targetSection = targetSection;
                return this;
            }

            public Builder numEvents(int numEvents) {
                this.numEvents = numEvents;
                return this;
            }

            public Builder machineNames(String[] machineNames) {
                this.machineNames = machineNames;
                return this;
            }

            public Builder sourceVesselTypeGeometries(VesselTypeGeometry[] sourceVesselTypeGeometries) {
                this.sourceVesselTypeGeometries = sourceVesselTypeGeometries;
                return this;
            }

            public Builder sourceBarcodedTubeType(BarcodedTube.BarcodedTubeType sourceBarcodedTubeType) {
                this.sourceBarcodedTubeType = sourceBarcodedTubeType;
                return this;
            }

            public Builder sourceVolume(boolean sourceVolume) {
                this.sourceVolume = sourceVolume;
                return this;
            }

            public Builder sourceContainerPrefix(String sourceContainerPrefix) {
                this.sourceContainerPrefix = sourceContainerPrefix;
                return this;
            }

            public Builder targetVesselTypeGeometries(VesselTypeGeometry[] targetVesselTypeGeometries) {
                this.targetVesselTypeGeometries = targetVesselTypeGeometries;
                return this;
            }

            public Builder targetBarcodedTubeType(BarcodedTube.BarcodedTubeType targetBarcodedTubeType) {
                this.targetBarcodedTubeType = targetBarcodedTubeType;
                return this;
            }

            public Builder targetVolume(boolean targetVolume) {
                this.targetVolume = targetVolume;
                return this;
            }

            public Builder sourceMassRemoved(boolean sourceMassRemoved) {
                this.sourceMassRemoved = sourceMassRemoved;
                return this;
            }

            public Builder targetContainerPrefix(String targetContainerPrefix) {
                this.targetContainerPrefix = targetContainerPrefix;
                return this;
            }

            public Builder secondaryEvent(LabEventType secondaryEvent) {
                this.secondaryEvent = secondaryEvent;
                return this;
            }

            public Builder repeatedEvent(LabEventType repeatedEvent) {
                this.repeatedEvent = repeatedEvent;
                return this;
            }

            public Builder repeatedWorkflowQualifier(String repeatedWorkflowQualifier) {
                this.repeatedWorkflowQualifier = repeatedWorkflowQualifier;
                return this;
            }

            public Builder buttonValue(String buttonValue) {
                this.buttonValue = buttonValue;
                return this;
            }

            public Builder targetExpectedToExist(boolean targetExpectedToExist) {
                this.targetExpectedToExist = targetExpectedToExist;
                return this;
            }

            public Builder targetExpectedEmpty(boolean targetExpectedEmpty) {
                this.targetExpectedEmpty = targetExpectedEmpty;
                return this;
            }

            public Builder limsFile(boolean limsFile) {
                this.limsFile = limsFile;
                return this;
            }

            public Builder requireSingleParticipant(boolean requireSingleParticipant) {
                this.requireSingleParticipant = requireSingleParticipant;
                return this;
            }

            public Builder targetWellTypeGeometry(PlateWell.WellType wellType) {
                this.targetWellTypeGeometry = wellType;
                return this;
            }

            public Builder useWebCam(boolean useWebCam) {
                this.useWebCam = useWebCam;
                return this;
            }

            public Builder reagentRequirements(ReagentRequirements[] reagentRequirements) {
                this.reagentRequirements = reagentRequirements;
                return this;
            }

            public ManualTransferDetails build() {
                return new ManualTransferDetails(this);
            }
        }

        public MessageType getMessageType() {
            return messageType;
        }

        public VesselTypeGeometry getSourceVesselTypeGeometry() {
            if (sourceVesselTypeGeometry == null && sourceVesselTypeGeometryString != null) {
                sourceVesselTypeGeometry = convertGeometryString(sourceVesselTypeGeometryString);
            }
            return sourceVesselTypeGeometry;
        }

        private VesselTypeGeometry convertGeometryString(String sourceVesselTypeGeometryString) {
            String[] strings = sourceVesselTypeGeometryString.split("\\.");
            VesselTypeGeometry vesselTypeGeometry;
            switch (strings[0]) {
                case "RackType":
                    vesselTypeGeometry = RackOfTubes.RackType.getByName(strings[1]);
                    break;
                case "PlateType":
                    vesselTypeGeometry = StaticPlate.PlateType.getByAutomationName(strings[1]);
                    break;
                case "BarcodedTubeType":
                    vesselTypeGeometry = BarcodedTube.BarcodedTubeType.getByAutomationName(strings[1]);
                    break;
                case "WellType":
                    vesselTypeGeometry = PlateWell.WellType.getByAutomationName(strings[1]);
                    break;
                default:
                    throw new RuntimeException("Unknown type " + strings[0]);
            }
            return vesselTypeGeometry;
        }

        public SBSSection getSourceSection() {
            return sourceSection;
        }

        public boolean sourceVolume() {
            return sourceVolume;
        }

        public String getSourceContainerPrefix() {
            return sourceContainerPrefix;
        }

        public VesselTypeGeometry getTargetVesselTypeGeometry() {
            if (targetVesselTypeGeometry == null && targetVesselTypeGeometryString != null) {
                targetVesselTypeGeometry = convertGeometryString(targetVesselTypeGeometryString);
            }
            return targetVesselTypeGeometry;
        }

        public SBSSection getTargetSection() {
            return targetSection;
        }

        public boolean targetVolume() {
            return targetVolume;
        }

        public boolean sourceMassRemoved() {
            return sourceMassRemoved;
        }

        public String getTargetContainerPrefix() {
            return targetContainerPrefix;
        }

        public boolean isTargetExpectedToExist() {
            return targetExpectedToExist;
        }

        public boolean isTargetExpectedEmpty() {
            return targetExpectedEmpty;
        }

        public int getNumEvents() {
            return numEvents;
        }

        public String[] getMachineNames() {
            return machineNames;
        }

        public VesselTypeGeometry[] getSourceVesselTypeGeometries() {
            return sourceVesselTypeGeometries;
        }

        public Set<String> getSourceVesselTypeGeometriesString() {
            Set<String> vesselTypeNames = new HashSet<>();
            for (VesselTypeGeometry vesselTypeGeometry: getSourceVesselTypeGeometries()) {
                vesselTypeNames.add(vesselTypeGeometry.getDisplayName());
            }
            return vesselTypeNames;
        }

        public BarcodedTube.BarcodedTubeType getSourceBarcodedTubeType() {
            return sourceBarcodedTubeType;
        }

        public VesselTypeGeometry[] getTargetVesselTypeGeometries() {
            return targetVesselTypeGeometries;
        }

        public Set<String> getTargetVesselTypeGeometriesString() {
            Set<String> vesselTypeNames = new HashSet<>();
            for (VesselTypeGeometry vesselTypeGeometry: getTargetVesselTypeGeometries()) {
                vesselTypeNames.add(vesselTypeGeometry.getDisplayName());
            }
            return vesselTypeNames;
        }

        public BarcodedTube.BarcodedTubeType getTargetBarcodedTubeType() {
            return targetBarcodedTubeType;
        }

        public VesselTypeGeometry getTargetWellType() {
            if (targetWellTypeGeometry == null && targetWellTypeString != null) {
                targetWellTypeGeometry = convertGeometryString(targetWellTypeString);
            }
            return targetWellTypeGeometry;
        }

        public List<String> getReagentNames() {
            List<String> reagentNames = new ArrayList<>(reagentRequirements.length);
            if (ArrayUtils.isNotEmpty(reagentRequirements)) {
                for (ReagentRequirements reagentRequirement : reagentRequirements) {
                    reagentNames.add(reagentRequirement.getReagentName());
                }
            }
            return reagentNames;
        }

        public Map<String, ReagentRequirements> getMapReagentNameToRequirements() {
            if (mapReagentNameToReagentRequirements == null) {
                mapReagentNameToReagentRequirements = new HashMap<>();

                for (ReagentRequirements reagentRequirement : reagentRequirements) {
                    mapReagentNameToReagentRequirements.put(reagentRequirement.getReagentName(), reagentRequirement);
                }

            }
            return mapReagentNameToReagentRequirements;
        }

        public LabEventType getSecondaryEvent() {
            return secondaryEvent;
        }

        public LabEventType getRepeatedEvent() {
            return repeatedEvent;
        }

        public String getRepeatedWorkflowQualifier() {
            return repeatedWorkflowQualifier;
        }

        public String getButtonValue() {
            return buttonValue;
        }

        public boolean isLimsFile() {
            return limsFile;
        }

        public boolean isRequireSingleParticipant() {
            return requireSingleParticipant;
        }

        public boolean isUseWebCam() {
            return useWebCam;
        }

        public ReagentRequirements[] getReagentRequirements() {
            return reagentRequirements;
        }
    }

    private ManualTransferDetails manualTransferDetails;

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
            VolumeConcUpdate volumeConcUpdate, LibraryType libraryType) {
        this(name, expectSourcesEmpty, expectTargetsEmpty, systemOfRecord, createSources, plasticToValidate,
                pipelineTransformation, forwardMessage, volumeConcUpdate, null, null, libraryType);
    }

    LabEventType(String name, ExpectSourcesEmpty expectSourcesEmpty, ExpectTargetsEmpty expectTargetsEmpty,
            SystemOfRecord systemOfRecord, CreateSources createSources, PlasticToValidate plasticToValidate,
            PipelineTransformation pipelineTransformation, ForwardMessage forwardMessage,
            VolumeConcUpdate volumeConcUpdate, LibraryType libraryType, TranslateBspMessage translateBspMessage) {
        this(name, expectSourcesEmpty, expectTargetsEmpty, systemOfRecord, createSources, plasticToValidate,
                pipelineTransformation, forwardMessage, volumeConcUpdate, null, null, libraryType);
        this.translateBspMessage = translateBspMessage;
    }

    LabEventType(String name, ExpectSourcesEmpty expectSourcesEmpty, ExpectTargetsEmpty expectTargetsEmpty,
            SystemOfRecord systemOfRecord, CreateSources createSources, PlasticToValidate plasticToValidate,
            PipelineTransformation pipelineTransformation, ForwardMessage forwardMessage,
            VolumeConcUpdate volumeConcUpdate, LibraryType libraryType, AddMetadataToBsp addMetadataToBsp,
            TranslateBspMessage translateBspMessage) {
        this(name, expectSourcesEmpty, expectTargetsEmpty, systemOfRecord, createSources, plasticToValidate,
                pipelineTransformation, forwardMessage, volumeConcUpdate, null, null, libraryType);
        this.addMetadataToBsp = addMetadataToBsp;
        this.translateBspMessage = translateBspMessage;
    }

    LabEventType(String name, ExpectSourcesEmpty expectSourcesEmpty, ExpectTargetsEmpty expectTargetsEmpty,
                 SystemOfRecord systemOfRecord, CreateSources createSources, PlasticToValidate plasticToValidate,
                 PipelineTransformation pipelineTransformation, ForwardMessage forwardMessage,
                 VolumeConcUpdate volumeConcUpdate, ManualTransferDetails manualTransferDetails, LibraryType libraryType) {
        this(name, expectSourcesEmpty, expectTargetsEmpty, systemOfRecord, createSources, plasticToValidate,
                pipelineTransformation, forwardMessage, volumeConcUpdate, manualTransferDetails, null, libraryType);
    }

    LabEventType(String name, ExpectSourcesEmpty expectSourcesEmpty, ExpectTargetsEmpty expectTargetsEmpty,
                 SystemOfRecord systemOfRecord, CreateSources createSources, PlasticToValidate plasticToValidate,
                 PipelineTransformation pipelineTransformation, ForwardMessage forwardMessage,
                 VolumeConcUpdate volumeConcUpdate, ManualTransferDetails manualTransferDetails, LibraryType libraryType,
                 SourceHandling sourceHandling) {
        this(name, expectSourcesEmpty, expectTargetsEmpty, systemOfRecord, createSources, plasticToValidate,
                pipelineTransformation, forwardMessage, volumeConcUpdate, manualTransferDetails, null, libraryType,
                sourceHandling);
    }

    LabEventType(String name, ExpectSourcesEmpty expectSourcesEmpty, ExpectTargetsEmpty expectTargetsEmpty,
                 SystemOfRecord systemOfRecord, CreateSources createSources, PlasticToValidate plasticToValidate,
                 PipelineTransformation pipelineTransformation, ForwardMessage forwardMessage,
                 VolumeConcUpdate volumeConcUpdate, ManualTransferDetails manualTransferDetails, LibraryType libraryType,
                 TranslateBspMessage translateBspMessage) {
        this(name, expectSourcesEmpty, expectTargetsEmpty, systemOfRecord, createSources, plasticToValidate,
                pipelineTransformation, forwardMessage, volumeConcUpdate, manualTransferDetails, null, libraryType,
                translateBspMessage);
    }

    LabEventType(String name, ExpectSourcesEmpty expectSourcesEmpty, ExpectTargetsEmpty expectTargetsEmpty,
                 SystemOfRecord systemOfRecord, CreateSources createSources, PlasticToValidate plasticToValidate,
                 PipelineTransformation pipelineTransformation, ForwardMessage forwardMessage,
                 VolumeConcUpdate volumeConcUpdate, ManualTransferDetails manualTransferDetails, LibraryType libraryType,
                 String collabSampleSuffix, Metadata.Key metadataKey, String metadataValue, MaterialType resultingMaterialType,
                 SourceHandling sourceHandling) {
        this(name, expectSourcesEmpty, expectTargetsEmpty, systemOfRecord, createSources, plasticToValidate,
                pipelineTransformation, forwardMessage, volumeConcUpdate, manualTransferDetails, resultingMaterialType,
                libraryType);
        this.collabSampleSuffix = collabSampleSuffix;
        this.metadataKey = metadataKey;
        this.metadataValue = metadataValue;
        this.sourceHandling = sourceHandling;
    }

    LabEventType(String name, ExpectSourcesEmpty expectSourcesEmpty, ExpectTargetsEmpty expectTargetsEmpty,
            SystemOfRecord systemOfRecord, CreateSources createSources, PlasticToValidate plasticToValidate,
            PipelineTransformation pipelineTransformation, ForwardMessage forwardMessage,
            VolumeConcUpdate volumeConcUpdate, ManualTransferDetails manualTransferDetails,
            MaterialType resultingMaterialType, LibraryType libraryType) {
        this.name = name;
        this.expectedEmptySources = expectSourcesEmpty;
        this.expectedEmptyTargets = expectTargetsEmpty;
        this.systemOfRecord = systemOfRecord;
        this.createSources = createSources;
        this.plasticToValidate = plasticToValidate;
        this.pipelineTransformation = pipelineTransformation;
        this.forwardMessage = forwardMessage;
        this.volumeConcUpdate = volumeConcUpdate;
        this.manualTransferDetails = manualTransferDetails;
        this.resultingMaterialType = resultingMaterialType;
        this.libraryType = libraryType;
    }

    LabEventType(String name, ExpectSourcesEmpty expectSourcesEmpty, ExpectTargetsEmpty expectTargetsEmpty,
                 SystemOfRecord systemOfRecord, CreateSources createSources, PlasticToValidate plasticToValidate,
                 PipelineTransformation pipelineTransformation, ForwardMessage forwardMessage,
                 VolumeConcUpdate volumeConcUpdate, ManualTransferDetails manualTransferDetails,
                 MaterialType resultingMaterialType, LibraryType libraryType,
                 SourceHandling sourceHandling) {
        this(name, expectSourcesEmpty, expectTargetsEmpty, systemOfRecord, createSources, plasticToValidate,
                pipelineTransformation, forwardMessage, volumeConcUpdate, manualTransferDetails,
                resultingMaterialType, libraryType);
        this.sourceHandling = sourceHandling;
    }

    LabEventType(String name, ExpectSourcesEmpty expectSourcesEmpty, ExpectTargetsEmpty expectTargetsEmpty,
                 SystemOfRecord systemOfRecord, CreateSources createSources, PlasticToValidate plasticToValidate,
                 PipelineTransformation pipelineTransformation, ForwardMessage forwardMessage,
                 VolumeConcUpdate volumeConcUpdate, ManualTransferDetails manualTransferDetails,
                 MaterialType resultingMaterialType, LibraryType libraryType, TranslateBspMessage translateBspMessage) {
        this.name = name;
        this.expectedEmptySources = expectSourcesEmpty;
        this.expectedEmptyTargets = expectTargetsEmpty;
        this.systemOfRecord = systemOfRecord;
        this.createSources = createSources;
        this.plasticToValidate = plasticToValidate;
        this.pipelineTransformation = pipelineTransformation;
        this.forwardMessage = forwardMessage;
        this.volumeConcUpdate = volumeConcUpdate;
        this.manualTransferDetails = manualTransferDetails;
        this.resultingMaterialType = resultingMaterialType;
        this.libraryType = libraryType;
        this.translateBspMessage = translateBspMessage;
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

    public static Set<LabEventType> getLabEventTypesForMaterialType(MaterialType materialType) {
        Set<LabEventType> resultSet=new HashSet<>();
        for (LabEventType labEventType : LabEventType.values()) {
            if (labEventType.resultingMaterialType == materialType) {
                resultSet.add(labEventType);
            }
        }
        return resultSet;
    }

    public static List<LabEventType> getLabEventsWithLibraryEtlDisplayName(String etlDisplayName) {
        Set<LabEventType> resultSet =new HashSet<>();
        for (LabEventType labEventType : LabEventType.values()) {
            if (labEventType.getLibraryType().getEtlDisplayName().equals(etlDisplayName)) {
                resultSet.add(labEventType);
            }
        }
        return new ArrayList<>(resultSet);
    }

    public ForwardMessage getForwardMessage() {
        return forwardMessage;
    }

    public VolumeConcUpdate getVolumeConcUpdate() {
        return volumeConcUpdate;
    }

    public MaterialType getResultingMaterialType() {
        return resultingMaterialType;
    }

    @Nullable
    public ManualTransferDetails getManualTransferDetails() {
        return manualTransferDetails;
    }

    public LibraryType getLibraryType(){
        return libraryType;
    }

    public String getCollabSampleSuffix() {
        return collabSampleSuffix;
    }

    public Metadata.Key getMetadataKey() {
        return metadataKey;
    }

    public String getMetadataValue() {
        return metadataValue;
    }

    public AddMetadataToBsp getAddMetadataToBsp() {
        return addMetadataToBsp;
    }

    public TranslateBspMessage getTranslateBspMessage() {
        return translateBspMessage;
    }

    public SourceHandling getSourceHandling() {
        return sourceHandling;
    }

    /**
     * Check whether this event type expects to deplete all sources (if there are any transfers).
     *
     * @return True if the DEPLETE flag is set on the eventType, otherwise false.
     */
    public boolean depleteSources() {
        return getSourceHandling() != null && getSourceHandling() == SourceHandling.DEPLETE;
    }

    /**
     * Check whether this event type expects to allow terminating depleted sources (if there are any transfers).
     *
     * @return true if TERMINATE_DEPLETED flag is set on the eventType, otherwise false.
     */
    public boolean terminateDepletedSources() {
        return getSourceHandling() != null && getSourceHandling() == SourceHandling.TERMINATE_DEPLETED;
    }
}

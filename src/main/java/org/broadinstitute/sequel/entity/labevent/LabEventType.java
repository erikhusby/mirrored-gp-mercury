package org.broadinstitute.sequel.entity.labevent;

import org.broadinstitute.sequel.entity.vessel.MolecularState;

import java.util.HashMap;
import java.util.Map;

/**
 * Properties common to all events of a particular message type
 */
public enum LabEventType {
    PREFLIGHT_PICO_SETUP ("PreflightPicoSetup", true, true,
            MolecularState.STRANDEDNESS.DOUBLE_STRANDED, MolecularState.DNA_OR_RNA.DNA),

    PREFLIGHT_NORMALIZATION("PreflightNormalization", true, true,
            MolecularState.STRANDEDNESS.DOUBLE_STRANDED, MolecularState.DNA_OR_RNA.DNA),

    PREFLIGHT_POST_NORM_PICO_SETUP ("PreflightPostNormPicoSetup", true, true,
            MolecularState.STRANDEDNESS.DOUBLE_STRANDED, MolecularState.DNA_OR_RNA.DNA),

    SHEARING_TRANSFER("ShearingTransfer", false, true,
            MolecularState.STRANDEDNESS.DOUBLE_STRANDED, MolecularState.DNA_OR_RNA.DNA),

    POST_SHEARING_TRANSFER_CLEANUP("PostShearingTransferCleanup", false, true,
            MolecularState.STRANDEDNESS.DOUBLE_STRANDED, MolecularState.DNA_OR_RNA.DNA),

    SHEARING_QC ("ShearingQC", false, true,
            MolecularState.STRANDEDNESS.DOUBLE_STRANDED, MolecularState.DNA_OR_RNA.DNA),

    END_REPAIR ("EndRepair", true, true,
            MolecularState.STRANDEDNESS.DOUBLE_STRANDED, MolecularState.DNA_OR_RNA.DNA),

    END_REPAIR_CLEANUP ("EndRepairCleanup", true, true,
            MolecularState.STRANDEDNESS.DOUBLE_STRANDED, MolecularState.DNA_OR_RNA.DNA),

    A_BASE ("ABase", true, true,
            MolecularState.STRANDEDNESS.DOUBLE_STRANDED, MolecularState.DNA_OR_RNA.DNA),

    A_BASE_CLEANUP ("ABaseCleanup", true, true,
            MolecularState.STRANDEDNESS.DOUBLE_STRANDED, MolecularState.DNA_OR_RNA.DNA),

    INDEXED_ADAPTER_LIGATION ("IndexedAdapterLigation", true, false,
            MolecularState.STRANDEDNESS.DOUBLE_STRANDED, MolecularState.DNA_OR_RNA.DNA),

    ADAPTER_LIGATION_CLEANUP ("AdapterLigationCleanup", false, true,
            MolecularState.STRANDEDNESS.DOUBLE_STRANDED, MolecularState.DNA_OR_RNA.DNA),

    // no longer used, needed to import past messages
    WGS_ADAPTER_LIGATION_CLEANUP ("WGSAdapterLigationCleanup", false, true,
            MolecularState.STRANDEDNESS.DOUBLE_STRANDED, MolecularState.DNA_OR_RNA.DNA),

    POND_ENRICHMENT ("PondEnrichment", true, true,
            MolecularState.STRANDEDNESS.DOUBLE_STRANDED, MolecularState.DNA_OR_RNA.DNA),

    POND_ENRICHMENT_CLEANUP ("HybSelPondEnrichmentCleanup", false, true,
            MolecularState.STRANDEDNESS.DOUBLE_STRANDED, MolecularState.DNA_OR_RNA.DNA),

    POND_REGISTRATION ("PondRegistration", false, true,
            MolecularState.STRANDEDNESS.DOUBLE_STRANDED, MolecularState.DNA_OR_RNA.DNA),

    PRE_SELECTION_POOL ("PreSelectionPool", false, true,
            MolecularState.STRANDEDNESS.DOUBLE_STRANDED, MolecularState.DNA_OR_RNA.DNA),

    HYBRIDIZATION ("Hybridization", false, true,
            MolecularState.STRANDEDNESS.DOUBLE_STRANDED, MolecularState.DNA_OR_RNA.DNA),

    BAIT_SETUP ("BaitSetup", true, true,
            MolecularState.STRANDEDNESS.DOUBLE_STRANDED, MolecularState.DNA_OR_RNA.DNA),

    BAIT_ADDITION ("BaitAddition", true, false,
            MolecularState.STRANDEDNESS.DOUBLE_STRANDED, MolecularState.DNA_OR_RNA.DNA),

    BEAD_ADDITION ("BeadAddition", true, true,
            MolecularState.STRANDEDNESS.DOUBLE_STRANDED, MolecularState.DNA_OR_RNA.DNA),

    AP_WASH ("APWash", true, true,
            MolecularState.STRANDEDNESS.DOUBLE_STRANDED, MolecularState.DNA_OR_RNA.DNA),

    GS_WASH_1 ("GSWash1", true, true,
            MolecularState.STRANDEDNESS.DOUBLE_STRANDED, MolecularState.DNA_OR_RNA.DNA),

    GS_WASH_2 ("GSWash2", true, true,
            MolecularState.STRANDEDNESS.DOUBLE_STRANDED, MolecularState.DNA_OR_RNA.DNA),

    // no longer used, needed to import past messages
    GS_WASH_3 ("GSWash3", true, true,
            MolecularState.STRANDEDNESS.DOUBLE_STRANDED, MolecularState.DNA_OR_RNA.DNA),

    // no longer used, needed to import past messages
    GS_WASH_4 ("GSWash4", true, true,
            MolecularState.STRANDEDNESS.DOUBLE_STRANDED, MolecularState.DNA_OR_RNA.DNA),

    // no longer used, needed to import past messages
    GS_WASH_5 ("GSWash5", true, true,
            MolecularState.STRANDEDNESS.DOUBLE_STRANDED, MolecularState.DNA_OR_RNA.DNA),

    // no longer used, needed to import past messages
    GS_WASH_6 ("GSWash6", true, true,
            MolecularState.STRANDEDNESS.DOUBLE_STRANDED, MolecularState.DNA_OR_RNA.DNA),

    CATCH_ENRICHMENT_SETUP ("CatchEnrichmentSetup", true, true,
            MolecularState.STRANDEDNESS.DOUBLE_STRANDED, MolecularState.DNA_OR_RNA.DNA),

    CATCH_ENRICHMENT_CLEANUP ("CatchEnrichmentCleanup", false, true,
            MolecularState.STRANDEDNESS.DOUBLE_STRANDED, MolecularState.DNA_OR_RNA.DNA),

    NORMALIZED_CATCH_REGISTRATION ("NormalizedCatchRegistration", false, true,
            MolecularState.STRANDEDNESS.DOUBLE_STRANDED, MolecularState.DNA_OR_RNA.DNA),

    POOLING_TRANSFER ("PoolingTransfer", false, true,
            MolecularState.STRANDEDNESS.DOUBLE_STRANDED, MolecularState.DNA_OR_RNA.DNA),

    NORMALIZATION_TRANSFER ("NormalizationTransfer", false, true,
            MolecularState.STRANDEDNESS.DOUBLE_STRANDED, MolecularState.DNA_OR_RNA.DNA),

    DENATURE_TRANSFER ("DenatureTransfer", false, true,
            MolecularState.STRANDEDNESS.SINGLE_STRANDED, MolecularState.DNA_OR_RNA.DNA),

    STRIP_TUBE_B_TRANSFER ("StripTubeBTransfer", true, false,
            MolecularState.STRANDEDNESS.SINGLE_STRANDED, MolecularState.DNA_OR_RNA.DNA),

    FLOWCELL_TRANSFER ("FlowcellTransfer", true, false,
            MolecularState.STRANDEDNESS.SINGLE_STRANDED, MolecularState.DNA_OR_RNA.DNA),

    SAGE_LOADING ("SageLoading", true, false,
            MolecularState.STRANDEDNESS.DOUBLE_STRANDED, MolecularState.DNA_OR_RNA.DNA),

    SAGE_UNLOADING ("SageUnloading", true, false,
            MolecularState.STRANDEDNESS.DOUBLE_STRANDED, MolecularState.DNA_OR_RNA.DNA),

    SAGE_CLEANUP ("SageCleanup", true, false,
            MolecularState.STRANDEDNESS.DOUBLE_STRANDED, MolecularState.DNA_OR_RNA.DNA),

    FLUIDIGM_SAMPLE_INPUT ("FluidigmSampleInput", true, false,
            MolecularState.STRANDEDNESS.DOUBLE_STRANDED, MolecularState.DNA_OR_RNA.DNA),

    FLUIDIGM_INDEXED_ADAPTER_INPUT ("FluidigmIndexedAdapterInput", true, false,
            MolecularState.STRANDEDNESS.DOUBLE_STRANDED, MolecularState.DNA_OR_RNA.DNA),

    FLUIDIGM_HARVESTING_TO_RACK ("FluidigmHarvestingToRack", true, false,
            MolecularState.STRANDEDNESS.DOUBLE_STRANDED, MolecularState.DNA_OR_RNA.DNA),

    JUMP_INITIAL_SHEARING_TRANSFER("JumpInitialShearingTransfer", false, true,
            MolecularState.STRANDEDNESS.DOUBLE_STRANDED, MolecularState.DNA_OR_RNA.DNA),

    JUMP_INITIAL_SIZE_SELECTION("JumpInitialSizeSelection", false, true,
            MolecularState.STRANDEDNESS.DOUBLE_STRANDED, MolecularState.DNA_OR_RNA.DNA),

    JUMP_END_REPAIR_1("JumpEndRepair1", true, false,
            MolecularState.STRANDEDNESS.DOUBLE_STRANDED, MolecularState.DNA_OR_RNA.DNA),

    JUMP_INITIAL_SIZE_SELECTION_QC("JumpInitialSizeSelectionQC", false, true,
            MolecularState.STRANDEDNESS.DOUBLE_STRANDED, MolecularState.DNA_OR_RNA.DNA),

    JUMP_END_REPAIR_1_CLEANUP("JumpEndRepair1Cleanup", false, true,
            MolecularState.STRANDEDNESS.DOUBLE_STRANDED, MolecularState.DNA_OR_RNA.DNA),

    JUMP_END_REPAIR_1_QC("JumpEndRepair1QC", false, true,
            MolecularState.STRANDEDNESS.DOUBLE_STRANDED, MolecularState.DNA_OR_RNA.DNA),

    JUMP_CIRCULARIZATION_SETUP("JumpCircularizationSetup", false, true,
            MolecularState.STRANDEDNESS.DOUBLE_STRANDED, MolecularState.DNA_OR_RNA.DNA),

    JUMP_EXONUCLEASE_TREATMENT("JumpExonucleaseTreatment", true, false,
            MolecularState.STRANDEDNESS.DOUBLE_STRANDED, MolecularState.DNA_OR_RNA.DNA),

    JUMP_EXO_INACTIVATION("JumpExoInactivation", false, true,
            MolecularState.STRANDEDNESS.DOUBLE_STRANDED, MolecularState.DNA_OR_RNA.DNA),

    JUMP_POST_CIRC_SIZE_SELECTION("JumpPostCircSizeSelection", false, true,
            MolecularState.STRANDEDNESS.DOUBLE_STRANDED, MolecularState.DNA_OR_RNA.DNA),

    JUMP_IMMOBILIZATION("JumpImmobilization", true, false,
            MolecularState.STRANDEDNESS.DOUBLE_STRANDED, MolecularState.DNA_OR_RNA.DNA),

    JUMP_IMMOBILIZATION_WASH("JumpImmobilizationWash", false, true,
            MolecularState.STRANDEDNESS.DOUBLE_STRANDED, MolecularState.DNA_OR_RNA.DNA),

    JUMP_END_REPAIR_2("JumpEndRepair2", false, true,
            MolecularState.STRANDEDNESS.DOUBLE_STRANDED, MolecularState.DNA_OR_RNA.DNA),

    JUMP_END_REPAIR_2_WASH("JumpEndRepair2Wash", false, true,
            MolecularState.STRANDEDNESS.DOUBLE_STRANDED, MolecularState.DNA_OR_RNA.DNA),

    JUMP_A_TAILING("JumpATailing", false, true,
            MolecularState.STRANDEDNESS.DOUBLE_STRANDED, MolecularState.DNA_OR_RNA.DNA),

    JUMP_A_TAILING_WASH("JumpATailingWash", false, true,
            MolecularState.STRANDEDNESS.DOUBLE_STRANDED, MolecularState.DNA_OR_RNA.DNA),

    JUMP_ADD_INDEX("JumpAddIndex", false, true,
            MolecularState.STRANDEDNESS.DOUBLE_STRANDED, MolecularState.DNA_OR_RNA.DNA),

    JUMP_ADAPTER_LIGATION("JumpAdapterLigation", false, true,
            MolecularState.STRANDEDNESS.DOUBLE_STRANDED, MolecularState.DNA_OR_RNA.DNA),

    JUMP_ADAPTER_LIGATION_WASH("JumpAdapterLigationWash", false, true,
            MolecularState.STRANDEDNESS.DOUBLE_STRANDED, MolecularState.DNA_OR_RNA.DNA),

    JUMP_AMPLIFICATION("JumpAmplification", false, true,
            MolecularState.STRANDEDNESS.DOUBLE_STRANDED, MolecularState.DNA_OR_RNA.DNA),

    JUMP_FINAL_LIBRARY_ELUTION("JumpFinalLibraryElution", false, true,
            MolecularState.STRANDEDNESS.DOUBLE_STRANDED, MolecularState.DNA_OR_RNA.DNA),

    JUMP_FINAL_LIBRARY_SIZE_SELECTION("JumpFinalLibrarySizeSelection", false, true,
            MolecularState.STRANDEDNESS.DOUBLE_STRANDED, MolecularState.DNA_OR_RNA.DNA),

    JUMP_FINAL_LIBRARY_QC("JumpFinalLibraryQC", false, true,
            MolecularState.STRANDEDNESS.DOUBLE_STRANDED, MolecularState.DNA_OR_RNA.DNA),

    JUMP_ENRICHED_LIBRARY_REGISTRATION("JumpEnrichedLibraryRegistration", false, true,
            MolecularState.STRANDEDNESS.DOUBLE_STRANDED, MolecularState.DNA_OR_RNA.DNA),

    PICO_DILUTION_TRANSFER ("PicoDilutionTransfer", true, true,
            MolecularState.STRANDEDNESS.DOUBLE_STRANDED, MolecularState.DNA_OR_RNA.DNA),

    PICO_MICROFLOUR_TRANSFER ("PicoMicroflourTransfer", true, true,
            MolecularState.STRANDEDNESS.DOUBLE_STRANDED, MolecularState.DNA_OR_RNA.DNA),

    PICO_STANDARDS_TRANSFER ("PicoStandardsTransfer", true, true,
            MolecularState.STRANDEDNESS.DOUBLE_STRANDED, MolecularState.DNA_OR_RNA.DNA),
    ;

    private final String name;

    private final boolean expectedEmptySources;

    private final boolean expectedEmptyTargets;

    private final MolecularState.DNA_OR_RNA nucleicAcidType;

    private final MolecularState.STRANDEDNESS targetStrand;

    private static final Map<String, LabEventType> mapNameToType = new HashMap<String, LabEventType>();

    /**
     * One attempt at trying to make a very generic
     * {@link LabEvent} to handle lots of different
     * {@link LabEventName event names}
     * @param name
     * @param expectSourcesEmpty
     * @param expectTargetsEmpty
     * @param targetStrand if null, inherit the same {@link org.broadinstitute.sequel.entity.vessel.MolecularState.STRANDEDNESS strand}
*                     from the {@link org.broadinstitute.sequel.entity.labevent.LabEvent#getSourceLabVessels()}
     * @param nucleicAcid if null, inherit the same {@link org.broadinstitute.sequel.entity.vessel.MolecularState.DNA_OR_RNA nucleic acid}
*                     from the {@link org.broadinstitute.sequel.entity.labevent.LabEvent#getSourceLabVessels() sources}
     */
    LabEventType(String name,
            boolean expectSourcesEmpty,
            boolean expectTargetsEmpty,
            MolecularState.STRANDEDNESS targetStrand,
            MolecularState.DNA_OR_RNA nucleicAcid) {
        this.name = name;
        this.expectedEmptySources = expectSourcesEmpty;
        this.expectedEmptyTargets = expectTargetsEmpty;
        this.nucleicAcidType = nucleicAcid;
        this.targetStrand = targetStrand;
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

    public MolecularState.DNA_OR_RNA getNucleicAcidType() {
        return nucleicAcidType;
    }

    public MolecularState.STRANDEDNESS getTargetStrand() {
        return targetStrand;
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

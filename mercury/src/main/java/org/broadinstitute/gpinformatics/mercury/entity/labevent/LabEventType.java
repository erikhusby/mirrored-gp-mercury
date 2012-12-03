package org.broadinstitute.gpinformatics.mercury.entity.labevent;

import org.broadinstitute.gpinformatics.mercury.entity.vessel.NucleicAcid;
import org.broadinstitute.gpinformatics.mercury.entity.vessel.Strandedness;

import java.util.HashMap;
import java.util.Map;

/**
 * Properties common to all events of a particular message type
 */
public enum LabEventType {
    PREFLIGHT_CLEANUP ("PreFlightCleanup", true, true,
            Strandedness.DOUBLE_STRANDED, NucleicAcid.DNA),
    PREFLIGHT_PICO_SETUP ("PreflightPicoSetup", true, true,
            Strandedness.DOUBLE_STRANDED, NucleicAcid.DNA),
    PREFLIGHT_NORMALIZATION("PreflightNormalization", true, true,
            Strandedness.DOUBLE_STRANDED, NucleicAcid.DNA),
    PREFLIGHT_POST_NORM_PICO_SETUP ("PreflightPostNormPicoSetup", true, true,
            Strandedness.DOUBLE_STRANDED, NucleicAcid.DNA),

    SHEARING_TRANSFER("ShearingTransfer", false, true,
            Strandedness.DOUBLE_STRANDED, NucleicAcid.DNA),

    PLATING_TO_SHEARING_TUBES("PlatingToShearingTubes", false, true,
            Strandedness.DOUBLE_STRANDED, NucleicAcid.DNA),

    COVARIS_LOADED("CovarisLoaded", false, true,
            Strandedness.DOUBLE_STRANDED, NucleicAcid.DNA),
    POST_SHEARING_TRANSFER_CLEANUP("PostShearingTransferCleanup", false, true,
            Strandedness.DOUBLE_STRANDED, NucleicAcid.DNA),
    SHEARING_QC ("ShearingQC", false, true,
            Strandedness.DOUBLE_STRANDED, NucleicAcid.DNA),

    END_REPAIR ("EndRepair", true, true,
            Strandedness.DOUBLE_STRANDED, NucleicAcid.DNA),
    POST_END_REPAIR_THERMO_CYCLER_LOADED("PostEndRepairThermoCyclerLoaded", false, true,
            Strandedness.DOUBLE_STRANDED, NucleicAcid.DNA),
    END_REPAIR_CLEANUP ("EndRepairCleanup", true, true,
            Strandedness.DOUBLE_STRANDED, NucleicAcid.DNA),
    A_BASE ("ABase", true, true,
            Strandedness.DOUBLE_STRANDED, NucleicAcid.DNA),
    POST_ABASE_THERMO_CYCLER_LOADED("PostAbaseThermoCyclerLoaded", false, true,
            Strandedness.DOUBLE_STRANDED, NucleicAcid.DNA),
    A_BASE_CLEANUP ("ABaseCleanup", true, true,
            Strandedness.DOUBLE_STRANDED, NucleicAcid.DNA),
    INDEXED_ADAPTER_LIGATION ("IndexedAdapterLigation", true, false,
            Strandedness.DOUBLE_STRANDED, NucleicAcid.DNA),
    POST_INDEXED_ADAPTER_LIGATION_THERMO_CYCLER_LOADED("PostIndexedAdapterLigationThermoCyclerLoaded", true, true,
            Strandedness.DOUBLE_STRANDED, NucleicAcid.DNA),
    ADAPTER_LIGATION_CLEANUP ("AdapterLigationCleanup", false, true,
            Strandedness.DOUBLE_STRANDED, NucleicAcid.DNA),
    // no longer used, needed to import past messages
    WGS_ADAPTER_LIGATION_CLEANUP ("WGSAdapterLigationCleanup", false, true,
            Strandedness.DOUBLE_STRANDED, NucleicAcid.DNA),

    POND_ENRICHMENT ("PondEnrichment", true, true,
            Strandedness.DOUBLE_STRANDED, NucleicAcid.DNA),
    POST_POND_ENRICHMENT_THERMO_CYCLER_LOADED("PostPondEnrichmentThermoCyclerLoaded", true, true,
            Strandedness.DOUBLE_STRANDED, NucleicAcid.DNA),
    POND_ENRICHMENT_CLEANUP ("HybSelPondEnrichmentCleanup", false, true,
            Strandedness.DOUBLE_STRANDED, NucleicAcid.DNA),
    POND_REGISTRATION ("PondRegistration", false, true,
            Strandedness.DOUBLE_STRANDED, NucleicAcid.DNA),
    POND_PICO("PondPico", false, true,
            Strandedness.DOUBLE_STRANDED, NucleicAcid.DNA),

    PRE_SELECTION_POOL ("PreSelectionPool", false, true,
            Strandedness.DOUBLE_STRANDED, NucleicAcid.DNA),
    HYBRIDIZATION ("Hybridization", false, true,
            Strandedness.DOUBLE_STRANDED, NucleicAcid.DNA),
    BAIT_SETUP ("BaitSetup", true, true,
            Strandedness.DOUBLE_STRANDED, NucleicAcid.DNA),
    BAIT_ADDITION ("BaitAddition", true, false,
            Strandedness.DOUBLE_STRANDED, NucleicAcid.DNA),
    BEAD_ADDITION ("BeadAddition", true, true,
            Strandedness.DOUBLE_STRANDED, NucleicAcid.DNA),
    AP_WASH ("APWash", true, true,
            Strandedness.DOUBLE_STRANDED, NucleicAcid.DNA),
    GS_WASH_1 ("GSWash1", true, true,
            Strandedness.DOUBLE_STRANDED, NucleicAcid.DNA),
    GS_WASH_2 ("GSWash2", true, true,
            Strandedness.DOUBLE_STRANDED, NucleicAcid.DNA),
    // no longer used, needed to import past messages
    GS_WASH_3 ("GSWash3", true, true,
            Strandedness.DOUBLE_STRANDED, NucleicAcid.DNA),
    // no longer used, needed to import past messages
    GS_WASH_4 ("GSWash4", true, true,
            Strandedness.DOUBLE_STRANDED, NucleicAcid.DNA),
    // no longer used, needed to import past messages
    GS_WASH_5 ("GSWash5", true, true,
            Strandedness.DOUBLE_STRANDED, NucleicAcid.DNA),
    // no longer used, needed to import past messages
    GS_WASH_6 ("GSWash6", true, true,
            Strandedness.DOUBLE_STRANDED, NucleicAcid.DNA),
    CATCH_ENRICHMENT_SETUP ("CatchEnrichmentSetup", true, true,
            Strandedness.DOUBLE_STRANDED, NucleicAcid.DNA),
    CATCH_ENRICHMENT_CLEANUP ("CatchEnrichmentCleanup", false, true,
            Strandedness.DOUBLE_STRANDED, NucleicAcid.DNA),
    NORMALIZED_CATCH_REGISTRATION ("NormalizedCatchRegistration", false, true,
            Strandedness.DOUBLE_STRANDED, NucleicAcid.DNA),
    CATCH_PICO ("CatchPico", false, true,
            Strandedness.DOUBLE_STRANDED, NucleicAcid.DNA),

    POOLING_TRANSFER ("PoolingTransfer", false, true,
            Strandedness.DOUBLE_STRANDED, NucleicAcid.DNA),
    ECO_TRANSFER ("EcoTransfer", false, true,
            Strandedness.DOUBLE_STRANDED, NucleicAcid.DNA),
    NORMALIZATION_TRANSFER ("NormalizationTransfer", false, true,
            Strandedness.DOUBLE_STRANDED, NucleicAcid.DNA),
    DENATURE_TRANSFER ("DenatureTransfer", false, true,
            Strandedness.SINGLE_STRANDED, NucleicAcid.DNA),
    STRIP_TUBE_B_TRANSFER ("StripTubeBTransfer", true, false,
            Strandedness.SINGLE_STRANDED, NucleicAcid.DNA),
    FLOWCELL_TRANSFER ("FlowcellTransfer", true, false,
            Strandedness.SINGLE_STRANDED, NucleicAcid.DNA),
    FLOWCELL_LOADED ("FlowcellLoaded", true, false,
            Strandedness.SINGLE_STRANDED, NucleicAcid.DNA),

    SAGE_LOADING ("SageLoading", true, false,
            Strandedness.DOUBLE_STRANDED, NucleicAcid.DNA),
    SAGE_LOADED ("SageLoaded", true, false,
            Strandedness.DOUBLE_STRANDED, NucleicAcid.DNA),
    SAGE_UNLOADING ("SageUnloading", true, false,
            Strandedness.DOUBLE_STRANDED, NucleicAcid.DNA),
    SAGE_CLEANUP ("SageCleanup", true, false,
            Strandedness.DOUBLE_STRANDED, NucleicAcid.DNA),

    FLUIDIGM_SAMPLE_INPUT ("FluidigmSampleInput", true, false,
            Strandedness.DOUBLE_STRANDED, NucleicAcid.DNA),
    FLUIDIGM_INDEXED_ADAPTER_INPUT ("FluidigmIndexedAdapterInput", true, false,
            Strandedness.DOUBLE_STRANDED, NucleicAcid.DNA),
    FLUIDIGM_INDEXED_ADAPTER_PCR ("FluidigmIndexedAdapterPCR", true, false,
            Strandedness.DOUBLE_STRANDED, NucleicAcid.DNA),
    FLUIDIGM_PRODUCT_DILUTION ("FluidigmProductDilution", true, false,
            Strandedness.DOUBLE_STRANDED, NucleicAcid.DNA),
    FLUIDIGM_HARVESTING_TO_RACK ("FluidigmHarvestingToRack", true, false,
            Strandedness.DOUBLE_STRANDED, NucleicAcid.DNA),
    FLUIDIGM_HARVESTING ("FluidigmHarvesting", true, false,
            Strandedness.DOUBLE_STRANDED, NucleicAcid.DNA),
    FLUIDIGM_FINAL_TRANSFER("FluidigmFinalTransfer", true, false,
            Strandedness.DOUBLE_STRANDED, NucleicAcid.DNA),

    JUMP_INITIAL_SHEARING_TRANSFER("JumpInitialShearingTransfer", false, true,
            Strandedness.DOUBLE_STRANDED, NucleicAcid.DNA),
    JUMP_INITIAL_SIZE_SELECTION("JumpInitialSizeSelection", false, true,
            Strandedness.DOUBLE_STRANDED, NucleicAcid.DNA),
    JUMP_END_REPAIR_1("JumpEndRepair1", true, false,
            Strandedness.DOUBLE_STRANDED, NucleicAcid.DNA),
    JUMP_INITIAL_SIZE_SELECTION_QC("JumpInitialSizeSelectionQC", false, true,
            Strandedness.DOUBLE_STRANDED, NucleicAcid.DNA),
    JUMP_END_REPAIR_1_CLEANUP("JumpEndRepair1Cleanup", false, true,
            Strandedness.DOUBLE_STRANDED, NucleicAcid.DNA),
    JUMP_END_REPAIR_1_QC("JumpEndRepair1QC", false, true,
            Strandedness.DOUBLE_STRANDED, NucleicAcid.DNA),
    JUMP_CIRCULARIZATION_SETUP("JumpCircularizationSetup", false, true,
            Strandedness.DOUBLE_STRANDED, NucleicAcid.DNA),
    JUMP_EXONUCLEASE_TREATMENT("JumpExonucleaseTreatment", true, false,
            Strandedness.DOUBLE_STRANDED, NucleicAcid.DNA),
    JUMP_EXO_INACTIVATION("JumpExoInactivation", false, true,
            Strandedness.DOUBLE_STRANDED, NucleicAcid.DNA),
    JUMP_POST_CIRC_SIZE_SELECTION("JumpPostCircSizeSelection", false, true,
            Strandedness.DOUBLE_STRANDED, NucleicAcid.DNA),
    JUMP_IMMOBILIZATION("JumpImmobilization", true, false,
            Strandedness.DOUBLE_STRANDED, NucleicAcid.DNA),
    JUMP_IMMOBILIZATION_WASH("JumpImmobilizationWash", false, true,
            Strandedness.DOUBLE_STRANDED, NucleicAcid.DNA),
    JUMP_END_REPAIR_2("JumpEndRepair2", false, true,
            Strandedness.DOUBLE_STRANDED, NucleicAcid.DNA),
    JUMP_END_REPAIR_2_WASH("JumpEndRepair2Wash", false, true,
            Strandedness.DOUBLE_STRANDED, NucleicAcid.DNA),
    JUMP_A_TAILING("JumpATailing", false, true,
            Strandedness.DOUBLE_STRANDED, NucleicAcid.DNA),
    JUMP_A_TAILING_WASH("JumpATailingWash", false, true,
            Strandedness.DOUBLE_STRANDED, NucleicAcid.DNA),
    JUMP_ADD_INDEX("JumpAddIndex", false, true,
            Strandedness.DOUBLE_STRANDED, NucleicAcid.DNA),
    JUMP_ADAPTER_LIGATION("JumpAdapterLigation", false, true,
            Strandedness.DOUBLE_STRANDED, NucleicAcid.DNA),
    JUMP_ADAPTER_LIGATION_WASH("JumpAdapterLigationWash", false, true,
            Strandedness.DOUBLE_STRANDED, NucleicAcid.DNA),
    JUMP_AMPLIFICATION("JumpAmplification", false, true,
            Strandedness.DOUBLE_STRANDED, NucleicAcid.DNA),
    JUMP_FINAL_LIBRARY_ELUTION("JumpFinalLibraryElution", false, true,
            Strandedness.DOUBLE_STRANDED, NucleicAcid.DNA),
    JUMP_FINAL_LIBRARY_SIZE_SELECTION("JumpFinalLibrarySizeSelection", false, true,
            Strandedness.DOUBLE_STRANDED, NucleicAcid.DNA),
    JUMP_FINAL_LIBRARY_QC("JumpFinalLibraryQC", false, true,
            Strandedness.DOUBLE_STRANDED, NucleicAcid.DNA),
    JUMP_ENRICHED_LIBRARY_REGISTRATION("JumpEnrichedLibraryRegistration", false, true,
            Strandedness.DOUBLE_STRANDED, NucleicAcid.DNA),

    PICO_DILUTION_TRANSFER ("PicoDilutionTransfer", true, true,
            Strandedness.DOUBLE_STRANDED, NucleicAcid.DNA),
    PICO_BUFFER_ADDITION("PicoBufferAddition", true, true,
            Strandedness.DOUBLE_STRANDED, NucleicAcid.DNA),
    PICO_MICROFLUOR_TRANSFER("PicoMicrofluorTransfer", true, true,
            Strandedness.DOUBLE_STRANDED, NucleicAcid.DNA),
    PICO_STANDARDS_TRANSFER ("PicoStandardsTransfer", true, true,
            Strandedness.DOUBLE_STRANDED, NucleicAcid.DNA),

    DBS_SAMPLE_PUNCH ("DBSSamplePunch", false, true,
            Strandedness.DOUBLE_STRANDED, NucleicAcid.DNA),
    DBS_INCUBATION_MIX ("DBSIncubationMix", true, false,
            Strandedness.DOUBLE_STRANDED, NucleicAcid.DNA),
    DBS_LYSIS_BUFFER ("DBSLysisBuffer", true, false,
            Strandedness.DOUBLE_STRANDED, NucleicAcid.DNA),
    DBS_MAGNETIC_RESIN ("DBSMagneticResin", true, false,
            Strandedness.DOUBLE_STRANDED, NucleicAcid.DNA),
    DBS_1ST_PURIFICATION ("DBS1stPurification", false, true,
            Strandedness.DOUBLE_STRANDED, NucleicAcid.DNA),
    DBS_WASH_BUFFER ("DBSWashBuffer", true, false,
            Strandedness.DOUBLE_STRANDED, NucleicAcid.DNA),
    DBS_2ND_PURIFICATION ("DBS2ndPurification", false, true,
            Strandedness.DOUBLE_STRANDED, NucleicAcid.DNA),
    DBS_ELUTION_BUFFER ("DBSElutionBuffer", false, true,
            Strandedness.DOUBLE_STRANDED, NucleicAcid.DNA),
    DBS_FINAL_TRANSFER ("DBSFinalTransfer", false, true,
            Strandedness.DOUBLE_STRANDED, NucleicAcid.DNA),

    TSCA_POST_NORM_TRANSFER("TSCAPostNormTransfer", false, true,
            Strandedness.DOUBLE_STRANDED, NucleicAcid.DNA),
    TSCA_HYB_SET_UP("TSCAHybSetUp", false, true,
            Strandedness.DOUBLE_STRANDED, NucleicAcid.DNA),
    TSCA_CAT_ADDITION("TSCACATAddition", false, true,
            Strandedness.DOUBLE_STRANDED, NucleicAcid.DNA),
    TSCA_HYBRIDIZATION_CLEANUP("TSCAHybridizationCleanUp", false, true,
            Strandedness.DOUBLE_STRANDED, NucleicAcid.DNA),
    TSCA_FLP_PREP("TSCAFLPPrep", false, true,
            Strandedness.DOUBLE_STRANDED, NucleicAcid.DNA),
    TSCA_EXTENSION_LIGATION_SETUP("TSCAExtensionLigationSetup", false, true,
            Strandedness.DOUBLE_STRANDED, NucleicAcid.DNA),
    TSCA_INDEXED_AMPLICON_PCR("TSCAIndexedAmpliconPCR", false, true,
            Strandedness.DOUBLE_STRANDED, NucleicAcid.DNA),
    TSCA_INDEX_ADDITION("TSCAIndexAddition", false, true,
            Strandedness.DOUBLE_STRANDED, NucleicAcid.DNA),
    TSCA_AMPLICON_REGISTRATION("TSCAAmpliconRegistration", false, true,
            Strandedness.DOUBLE_STRANDED, NucleicAcid.DNA),
    TSCA_AMPLICON_PCR_CLEANUP("TSCAAmpliconPCRCleanup", false, true,
            Strandedness.DOUBLE_STRANDED, NucleicAcid.DNA),
    TSCA_POOL_CREATION("TSCAPoolCreation", false, true,
            Strandedness.DOUBLE_STRANDED, NucleicAcid.DNA),

    ILLUMINA_16S_TO_PRIMER_TRANSFER("Illumina16StoPrimerTransfer", false, true,
            Strandedness.DOUBLE_STRANDED, NucleicAcid.DNA),
    ILLUMINA_16S_PCR_PRODUCT_TRANSFER("Illumina16SPcrProductTransfer", false, true,
            Strandedness.DOUBLE_STRANDED, NucleicAcid.DNA),

    POLY_A_TRANSFER("PolyATransfer", false, true,
            Strandedness.DOUBLE_STRANDED, NucleicAcid.DNA),
    POLY_A_SELECTION("PolyASelection", false, true,
            Strandedness.DOUBLE_STRANDED, NucleicAcid.DNA),
    DNASE("Dnase", false, true,
            Strandedness.DOUBLE_STRANDED, NucleicAcid.DNA),
    DNASE_CLEANUP("DnaseCleanup", false, true,
            Strandedness.DOUBLE_STRANDED, NucleicAcid.DNA),
    FRAGMENTATION("Fragmentation", false, true,
            Strandedness.DOUBLE_STRANDED, NucleicAcid.DNA),
    FRAGMENTATION_CLEANUP("FragmentationCleanup", false, true,
            Strandedness.DOUBLE_STRANDED, NucleicAcid.DNA),
    FIRST_STRAND("FirstStrand", false, true,
            Strandedness.DOUBLE_STRANDED, NucleicAcid.DNA),
    FIRST_STRAND_CLEANUP("FirstStrandCleanup", false, true,
            Strandedness.DOUBLE_STRANDED, NucleicAcid.DNA),
    SECOND_STRAND("SecondStrand", false, true,
            Strandedness.DOUBLE_STRANDED, NucleicAcid.DNA),
    SECOND_STRAND_CLEANUP("SecondStrandCleanup", false, true,
            Strandedness.DOUBLE_STRANDED, NucleicAcid.DNA),
    USER("USER", false, true,
            Strandedness.DOUBLE_STRANDED, NucleicAcid.DNA),
    CDNA_PONDENRICHMENT_CLEANUP("cDNAPondEnrichmentCleanup", false, true,
            Strandedness.DOUBLE_STRANDED, NucleicAcid.DNA),

    POLY_A_SELECTION_TS("PolyASelectionTS", false, true,
            Strandedness.DOUBLE_STRANDED, NucleicAcid.DNA),
    FIRST_STRAND_TS("FirstStrandTS", false, true,
            Strandedness.DOUBLE_STRANDED, NucleicAcid.DNA),
    SECOND_STRAND_TS("SecondStrandTS", false, true,
            Strandedness.DOUBLE_STRANDED, NucleicAcid.DNA),
    SECOND_STRAND_CLEANUP_TS("SecondStrandCleanupTS", false, true,
            Strandedness.DOUBLE_STRANDED, NucleicAcid.DNA),
    END_REPAIR_TS("EndRepairTS", false, true,
            Strandedness.DOUBLE_STRANDED, NucleicAcid.DNA),
    END_REPAIR_CLEANUP_TS("EndRepairCleanupTS", false, true,
            Strandedness.DOUBLE_STRANDED, NucleicAcid.DNA),
    A_BASE_TS("ABaseTS", false, true,
            Strandedness.DOUBLE_STRANDED, NucleicAcid.DNA),
    INDEXED_ADAPTER_LIGATION_TS("IndexedAdapterLigationTS", false, true,
            Strandedness.DOUBLE_STRANDED, NucleicAcid.DNA),
    ADAPTERLIGATION_CLEANUP_TS("AdapterLigationCleanupTS", false, true,
            Strandedness.DOUBLE_STRANDED, NucleicAcid.DNA),
    ENRICHMENT_TS("EnrichmentTS", false, true,
            Strandedness.DOUBLE_STRANDED, NucleicAcid.DNA),
    ENRICHMENT_CLEANUP_TS("EnrichmentCleanupTS", false, true,
            Strandedness.DOUBLE_STRANDED, NucleicAcid.DNA),

    IGN_PRE_NORM_TRANSFER("IGNPreNormTransfer", false, true,
            Strandedness.DOUBLE_STRANDED, NucleicAcid.DNA),
    IGN_NORM_TRANSFER("IGNNormTransfer", false, true,
            Strandedness.DOUBLE_STRANDED, NucleicAcid.DNA),
    IGN_INCOMING_PICO("IGNIncomingPico", false, true,
            Strandedness.DOUBLE_STRANDED, NucleicAcid.DNA),
    IGN_GAP_ALIQUOT("IGNGapAliquot", false, true,
            Strandedness.DOUBLE_STRANDED, NucleicAcid.DNA),

    VIRAL_PCR_PRODUCT_POOL_TRANSFER("ViralPcrProductPoolTransfer", false, true,
            Strandedness.DOUBLE_STRANDED, NucleicAcid.DNA),
    ONE_STEP_RT_PCR_PRODUCT_POOL_TRANSFER("OneStepRtPcrProductPoolTransfer", false, true,
            Strandedness.DOUBLE_STRANDED, NucleicAcid.DNA),

    /**
     * TODO SGM  the following names are place holders.  They will be re-evaluated as development of
     */
    SHEARING_BUCKET("ShearingBucket", true, true, Strandedness.DOUBLE_STRANDED, NucleicAcid.DNA),
    SHEARING_BUCKET_ENTRY ("ShearingBucketEntry", true, true, Strandedness.DOUBLE_STRANDED, NucleicAcid.DNA),
    SHEARING_BUCKET_EXIT ("ShearingBucketExit", true, true, Strandedness.DOUBLE_STRANDED, NucleicAcid.DNA),
    ;

    private final String name;

    /**
     * TODO The following 4 fields will be re-evaluated to see if they are necessary
     */
    private final boolean expectedEmptySources;

    private final boolean expectedEmptyTargets;

    private final NucleicAcid nucleicAcidType;

    private final Strandedness targetStrand;

    private static final Map<String, LabEventType> mapNameToType = new HashMap<String, LabEventType>();

    /**
     * One attempt at trying to make a very generic
     * {@link LabEvent} to handle lots of different
     * {@link LabEventName event names}
     * @param name
     * @param expectSourcesEmpty
     * @param expectTargetsEmpty
     * @param targetStrand if null, inherit the same {@link org.broadinstitute.gpinformatics.mercury.entity.vessel.Strandedness strand}
*                     from the {@link org.broadinstitute.gpinformatics.mercury.entity.labevent.LabEvent#getSourceLabVessels()}
     * @param nucleicAcid if null, inherit the same {@link org.broadinstitute.gpinformatics.mercury.entity.vessel.NucleicAcid nucleic acid}
*                     from the {@link org.broadinstitute.gpinformatics.mercury.entity.labevent.LabEvent#getSourceLabVessels() sources}
     */
    LabEventType(String name,
            boolean expectSourcesEmpty,
            boolean expectTargetsEmpty,
            Strandedness targetStrand,
            NucleicAcid nucleicAcid) {
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

    public NucleicAcid getNucleicAcidType() {
        return nucleicAcidType;
    }

    public Strandedness getTargetStrand() {
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

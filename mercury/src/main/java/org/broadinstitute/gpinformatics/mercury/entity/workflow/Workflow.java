package org.broadinstitute.gpinformatics.mercury.entity.workflow;

import org.apache.commons.lang3.ObjectUtils;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.Collection;
import java.util.Comparator;
import java.util.EnumSet;
import java.util.HashSet;
import java.util.Set;

/**
 * This represents the workflow associated with a product.
 *
 * The enum name field must exactly match WorkflowConfig.xml productWorkflowDef name value.
 */
public enum Workflow {
    AGILENT_EXOME_EXPRESS("Agilent Exome Express"),
    ICE_EXOME_EXPRESS("ICE Exome Express"),
    HYBRID_SELECTION("Hybrid Selection"),
    WHOLE_GENOME("Whole Genome"),
    PCR_FREE("Whole Genome PCR Free"),
    PCR_PLUS("Whole Genome PCR Plus"),
    PCR_FREE_HYPER_PREP("Whole Genome PCR Free HyperPrep"),
    PCR_PLUS_HYPER_PREP("Whole Genome PCR Plus HyperPrep"),
    CELL_FREE_HYPER_PREP("Cell Free HyperPrep"),
    CELL_FREE_HYPER_PREP_UMIS("Cell Free HyperPrep With UMIs"),
    G_DNA_ULTRA_LOW_PASS_WGS("gDNA Ultra Low Pass WGS"),
    ICE_EXOME_EXPRESS_HYPER_PREP("Hyper Prep ICE Exome Express"),
    ICE_EXOME_EXPRESS_HYPER_PREP_UMIS("Hyper Prep ICE Exome Express With UMIs"),
    CUSTOM_SELECTION("Custom Selection"),
    ICE("ICE"),
    ICE_CRSP("ICE CRSP"),
    CLINICAL_WHOLE_BLOOD_EXTRACTION("Clinical Whole Blood Extraction"),
    DNA_RNA_EXTRACTION_CELL_PELLETS("DNA and RNA from Cell Pellets"),
    DNA_RNA_EXTRACTION_CELL_PELLETS_BSP("DNA and RNA from Cell Pellets BSP"),
    TRU_SEQ_STRAND_SPECIFIC_CRSP("TruSeq Strand Specific CRSP"),
    TEN_X("10X"),
    INFINIUM("Infinium"),
    INFINIUM_METHYLATION("Infinium Methylation"),
    MALARIA("Malaria"),
    LASSO("Lasso"),
    DNA_RNA_EXTRACTION_STOOL("DNA and RNA from Stool"),
    SINGLE_CELL_SMART_SEQ("Single Cell SmartSeq"),
    SINGLE_CELL_10X("Single Cell 10X"),
    SEQ_ONLY("Seq Only"),
    NEXOME("Nexome"),
    /** Use this to indicate that no workflow is associated. */
    NONE(null, false);

    /**
     * The name of the workflow. More than that, this is the value that is persisted (at least in some cases, i.e.,
     * Product.workflowName) and is therefore not necessarily appropriate as a UI label (i.e., null for the NONE value).
     */
    @Nullable
    private final String name;

    // None was created as null for the name and the database is storing the name instead of the enum because the
    // enum is not used as a string. Because of this, had to add this displayable value so that the UI can override
    // the internal value here. This could be removed and NONE could be used by the UI if a fixup is done to store
    // the value as an enum.
    private final boolean displayable;

    Workflow(@Nullable String name) {
        this(name, true);
    }

    Workflow(@Nullable String name, boolean displayable) {
        this.name = name;
        this.displayable = displayable;
    }

    /**
     * Workflow processes that Mercury supports.
     */
    // todo jmt make this a flag on the enum
    public static final EnumSet<Workflow> SUPPORTED_WORKFLOWS =
            EnumSet.of(AGILENT_EXOME_EXPRESS, ICE_EXOME_EXPRESS, ICE_CRSP, CLINICAL_WHOLE_BLOOD_EXTRACTION,
                    DNA_RNA_EXTRACTION_CELL_PELLETS, TRU_SEQ_STRAND_SPECIFIC_CRSP, PCR_FREE, PCR_PLUS,
                    PCR_FREE_HYPER_PREP, PCR_PLUS_HYPER_PREP, CELL_FREE_HYPER_PREP, ICE_EXOME_EXPRESS_HYPER_PREP,
                    TEN_X, INFINIUM, INFINIUM_METHYLATION, MALARIA, CELL_FREE_HYPER_PREP_UMIS,
                    ICE_EXOME_EXPRESS_HYPER_PREP_UMIS, DNA_RNA_EXTRACTION_STOOL, CUSTOM_SELECTION,
                    DNA_RNA_EXTRACTION_CELL_PELLETS_BSP, LASSO, SINGLE_CELL_SMART_SEQ, SINGLE_CELL_10X, SEQ_ONLY,
                    G_DNA_ULTRA_LOW_PASS_WGS, NEXOME
            );

    public boolean isWorkflowSupportedByMercury() {
        return SUPPORTED_WORKFLOWS.contains(this);
    }

    @Nullable
    public String getWorkflowName() {
        return name;
    }

    @Nonnull
    public static Workflow findByName(@Nullable String searchName) {
        if (searchName != null) {
            for (Workflow workflow : values()) {
                if (searchName.equals(workflow.name)) {
                    return workflow;
                }
            }
        }
        return NONE;
    }

    public static Comparator<Workflow> BY_NAME = new Comparator<Workflow>() {
        @Override
        public int compare(Workflow workflow, Workflow otherWorkflow) {
            if (workflow == otherWorkflow) {
                return 0;
            }
            if (workflow == null) {
                return -1;
            }
            if (otherWorkflow == null) {
                return 1;
            }
            return ObjectUtils.compare(workflow.getWorkflowName(), otherWorkflow.getWorkflowName());
        }
    };

    public static Collection<String> workflowNamesOf(Collection<Workflow> workflows) {
        Set<String> workflowNames = new HashSet<>();
        for (Workflow workflow : workflows) {
            workflowNames.add(workflow.getWorkflowName());
        }
        return workflowNames;
    }
}

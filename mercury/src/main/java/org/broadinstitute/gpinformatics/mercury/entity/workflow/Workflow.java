package org.broadinstitute.gpinformatics.mercury.entity.workflow;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;

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
    ICE("ICE"),
    ICE_CRSP("ICE CRSP"),
    CLINICAL_EXTRACTION("Clinical Whole Blood Extraction"),
    DNA_RNA_EXTRACTION_FROZEN_TISSUE("DNA and RNA for Frozen Tissue - AllPrep"),
    DNA_RNA_EXTRACTION_CELL_PELLETS("DNA and RNA from Cell Pellets"),
    DNA_RNA_EXTRACTION_FFPE_SECTIONS("DNA and RNA for FFPE - AllPrep (sections)"),
    DNA_RNA_EXTRACTION_FFPE_SCROLLS("DNA and RNA for FFPE - AllPrep (scrolls)"),
    DNA_RNA_EXTRACTION_FFPE_BLOCKS("DNA and RNA for FFPE - AllPrep (blocks)"),
    DNA_RNA_EXTRACTION_STOOL("DNA and RNA for Stool - Chemagen"),
    DNA_EXTRACTION_WHOLE_BLOOD_MANUAL("DNA Extraction from Whole Blood - Manual"),
    DNA_EXTRACTION_WHOLE_BLOOD_CHEMAGEN("DNA Extraction from Whole Blood - Chemagen"),
    DNA_EXTRACTION_SALIVA("DNA Extraction from Saliva"),
    DNA_EXTRACTION_CELL_PELLETS("DNA Extraction from Cell Pellets"),
    DNA_EXTRACTION_FROZEN_TISSUE("DNA Extraction from Frozen Tissue"),
    DNA_EXTRACTION_FFPE_SECTIONS("DNA Extraction from FFPE Tissue (sections)"),
    DNA_EXTRACTION_FFPE_SCROLLS("DNA Extraction from FFPE Tissue (scrolls)"),
    DNA_EXTRACTION_FFPE_CORES("DNA Extraction from FFPE Tissue (cores)"),
    DNA_EXTRACTION_BLOOD_SPOTS("DNA Extraction from Blood Spots"),
    DNA_EXTRACTION_BUFFY_COATS("DNA Extraction from Buffy Coats"),
    RNA_EXTRACTION_FROZEN_TISSUE("RNA Extraction from Frozen Tissue"),
    RNA_EXTRACTION_FFPE_SECTIONS("RNA Extraction from FFPE Tissue (sections)"),
    RNA_EXTRACTION_CELL_PELLETS("RNA Extraction from Cell Pellets"),
    RNA_EXTRACTION_FFPE_SCROLLS("RNA Extraction from FFPE Tissue (scrolls)"),
    RNA_EXTRACTION_FFPE_CORES("RNA Extraction from FFPE Tissue (cores)"),
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
    public static final EnumSet<Workflow> SUPPORTED_WORKFLOWS =
            EnumSet.of(AGILENT_EXOME_EXPRESS, ICE_EXOME_EXPRESS, ICE_CRSP,
                    DNA_RNA_EXTRACTION_FROZEN_TISSUE, CLINICAL_EXTRACTION,
                    DNA_RNA_EXTRACTION_CELL_PELLETS,
                    DNA_RNA_EXTRACTION_FFPE_SECTIONS,
                    DNA_RNA_EXTRACTION_FFPE_SCROLLS,
                    DNA_RNA_EXTRACTION_FFPE_BLOCKS,
                    DNA_RNA_EXTRACTION_STOOL,
                    DNA_EXTRACTION_WHOLE_BLOOD_MANUAL,
                    DNA_EXTRACTION_WHOLE_BLOOD_CHEMAGEN,
                    DNA_EXTRACTION_SALIVA,
                    DNA_EXTRACTION_CELL_PELLETS,
                    DNA_EXTRACTION_FROZEN_TISSUE,
                    DNA_EXTRACTION_FFPE_SECTIONS,
                    DNA_EXTRACTION_FFPE_SCROLLS,
                    DNA_EXTRACTION_FFPE_CORES,
                    DNA_EXTRACTION_BLOOD_SPOTS,
                    DNA_EXTRACTION_BUFFY_COATS,
                    RNA_EXTRACTION_FROZEN_TISSUE,
                    RNA_EXTRACTION_FFPE_SECTIONS,
                    RNA_EXTRACTION_CELL_PELLETS,
                    RNA_EXTRACTION_FFPE_SCROLLS,
                    RNA_EXTRACTION_FFPE_CORES
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

    /**
     * Returns a list of the workflows for which the persisted workflow name is also usable as a label in the UI. This
     * excludes, for example, the NONE value which persists as null since null does not make a good UI label. UIs should
     * therefore use this list and, if applicable, manually add an option for the NONE value.
     *
     * The need for this method could be removed by adding a displayLabel property to Workflow. Additionally, the NONE
     * value should probably be moved to the top so that it shows up as the first option instead of the last option, but
     * that's more of a judgement call for the UI. The current implementation actually gives the UI more flexibility
     * about where to render the NONE option (or whether or not to render it at all).
     *
     * @return
     */
    public static List<Workflow> getVisibleWorkflowList() {
        // Remove the none from the list because the names are stored in the DB and NONE is null for that.
        List<Workflow> workflows = new ArrayList<>();
        for (Workflow currentWorkflow : Workflow.values()) {
            if (currentWorkflow.displayable) {
                workflows.add(currentWorkflow);
            }
        }

        return workflows;
    }
}

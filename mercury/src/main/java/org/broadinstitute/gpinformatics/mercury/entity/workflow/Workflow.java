package org.broadinstitute.gpinformatics.mercury.entity.workflow;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;

/**
 * This represents the workflow associated with a product.
 *
 * The enun name field must exactly match WorkflowConfig.xml productWorkflowDef name value.
 */
public enum Workflow {
    AGILENT_EXOME_EXPRESS("Agilent Exome Express"),
    ICE_EXOME_EXPRESS("ICE Exome Express"),
    HYBRID_SELECTION("Hybrid Selection"),
    WHOLE_GENOME("Whole Genome"),
    ICE("ICE"),
    ICE_CRSP("ICE CRSP"),
    CLINICAL_EXTRACTION("Clinical Whole Blood Extraction"),
    ALLPREP_EXTRACTION("AllPrep Extraction"),
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
            EnumSet.of(AGILENT_EXOME_EXPRESS, ICE_EXOME_EXPRESS, ICE_CRSP, CLINICAL_EXTRACTION);

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

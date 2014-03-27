package org.broadinstitute.gpinformatics.mercury.entity.workflow;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.Collection;

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
    /** Use this to indicate that no workflow is associated. */
    NONE(null, false);

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

    /** Workflow processes that Mercury supports. */
    public static final Collection<Workflow> SUPPORTED_WORKFLOWS = new ArrayList<Workflow>(){{
        add(AGILENT_EXOME_EXPRESS);
        add(ICE_EXOME_EXPRESS);
    }};

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

    public static Workflow[] getVisibleWorkflowList() {
        // Remove the none from the list because the names are stored in the DB and NONE is null for that.
        Workflow[] workflows = new Workflow[Workflow.values().length - 1];
        int i = 0;
        for (Workflow currentWorkflow : Workflow.values()) {
            if (currentWorkflow.displayable) {
                workflows[i++] = currentWorkflow;
            }
        }

        return workflows;
    }
}

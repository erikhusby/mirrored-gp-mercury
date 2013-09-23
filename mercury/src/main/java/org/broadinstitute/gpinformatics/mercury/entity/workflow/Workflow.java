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
    HYBRID_SELECTION("Hybrid Selection"),
    WHOLE_GENOME("Whole Genome"),
    ICE("ICE"),
    /** Use this to indicate that no workflow is associated. */
    NONE(null);

    @Nullable
    private final String name;

    Workflow(@Nullable String name) {
        this.name = name;
    }

    /** Workflow processes that Mercury supports. */
    public static final Collection<Workflow> SUPPORTED_WORKFLOWS = new ArrayList<Workflow>(){{
        add(AGILENT_EXOME_EXPRESS);
        add(ICE);
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
}

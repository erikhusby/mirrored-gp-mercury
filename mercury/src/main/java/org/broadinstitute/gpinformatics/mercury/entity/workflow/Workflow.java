package org.broadinstitute.gpinformatics.mercury.entity.workflow;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;

/**
 * This represents the workflow associated with a product.
 *
 * The enun names must exactly match WorkflowConfig.xml productDef elements.
 */
public enum Workflow {
    EXOME_EXPRESS("Exome Express"),
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

    /**
     * @param workflowName the workflow name to check
     * @return true if this is the exome express workflow
     */
    public static boolean isExomeExpress(@Nullable String workflowName) {
        return workflowName != null && workflowName.equals(EXOME_EXPRESS.name);
    }

    /** Workflow processes that Mercury supports. */
    public static final Collection<Workflow> SUPPORTED_WORKFLOWS = new ArrayList<Workflow>(){{
        add(EXOME_EXPRESS);
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

package org.broadinstitute.gpinformatics.mercury.entity.workflow;

import javax.annotation.Nullable;

public enum Workflow {
    EXOME_EXPRESS("Exome Express"),
    HYBRID_SELECTION("Hybrid Selection"),
    WHOLE_GENOME("Whole Genome"),
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

    @Nullable
    public String getWorkflowName() {
        return name;
    }

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

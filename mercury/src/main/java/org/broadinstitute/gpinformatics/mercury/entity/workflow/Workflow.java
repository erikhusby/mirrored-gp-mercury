package org.broadinstitute.gpinformatics.mercury.entity.workflow;

public enum Workflow {
    EXOME_EXPRESS("Exome Express"),
    HYBRID_SELECTION("Hybrid Selection"),
    WHOLE_GENOME("Whole Genome");

    private final String name;

    Workflow(String name) {
        this.name = name;
    }

    public boolean isExomeExpress() {
        return this == EXOME_EXPRESS;
    }

    public boolean isWholeGenome() {
        return this == WHOLE_GENOME;
    }

    /**
     * @param workflowName the workflow name to check
     * @return true if this is the exome express workflow
     */
    public static boolean isExomeExpress(String workflowName) {
        return EXOME_EXPRESS.name.equals(workflowName);
    }

    public String getWorkflowName() {
        return name;
    }

    public Workflow findByName(String searchName) {
        for (Workflow workflow : values()) {
            if (workflow.name.equals(searchName)) {
                return workflow;
            }
        }
        return null;
    }
}

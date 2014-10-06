package org.broadinstitute.gpinformatics.athena.boundary.projects;

/**
 * This enum handles the choices for shipping out sample kits through the collaboration portal.
 */
public enum SampleKitRecipient {
    PM("Project Manager"),
    COLLABORATOR("Collaborator");

    private String displayName;

    private SampleKitRecipient(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }
}

package org.broadinstitute.gpinformatics.athena.boundary.projects;

/**
 * This enum handles the choices for shipping out sample kits through the collaboration portal.
 */
public enum SampleKitShippingMethod {
    PM("Project Manager"),
    COLLABORATOR("Collaborator");

    private String displayName;

    private SampleKitShippingMethod(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }
}

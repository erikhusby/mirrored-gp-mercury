package org.broadinstitute.gpinformatics.mercury.entity.vessel;

/**
 * Abstraction over PlateType and RackType.
 */
public interface VesselTypeGeometry {

    String getDisplayName();

    VesselGeometry getVesselGeometry();

    boolean isBarcoded();
}

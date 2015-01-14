package org.broadinstitute.gpinformatics.mercury.entity.vessel;

/**
 * Abstraction over PlateType and RackType.
 */
public interface ContainerGeometryType {

    String getDisplayName();

    VesselGeometry getVesselGeometry();
}

package org.broadinstitute.gpinformatics.mercury.entity.vessel;

/**
 * Implemented by classes that embed VesselContainer
 */
public interface VesselContainerEmbedder<T extends LabVessel> {
    VesselContainer<T> getContainerRole();
}

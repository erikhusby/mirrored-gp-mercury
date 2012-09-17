package org.broadinstitute.sequel.entity.vessel;

/**
 * Implemented by classes that embed VesselContainer
 */
public interface VesselContainerEmbedder<T extends LabVessel> {
    VesselContainer<T> getVesselContainer();
}

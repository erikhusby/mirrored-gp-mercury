package org.broadinstitute.sequel.entity.vessel;

import java.util.Collection;

/**
 * A vessel that contains other vessels, e.g. a rack of tubes, a plate of wells, or a flowcell of lanes
 */
public interface VesselContainer<T extends LabVessel> {
    // todo jmt enum for positions
    LabVessel getVesselAtPosition(String position);
    /**
     * If this is a plate, this method could return
     * the {@link PlateWell wells}.  If this thing
     * is a {@link RackOfTubes}, this method could
     * return the {@link TwoDBarcodedTube} tubes in
     * the rack.
     * @return contained vessels
     */
    Collection<T> getContainedVessels();

    void addContainedVessel(T child, String position);
}

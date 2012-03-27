package org.broadinstitute.sequel.entity.vessel;

import org.hibernate.annotations.Parent;

import javax.persistence.Embeddable;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

/**
 * A vessel that contains other vessels, e.g. a rack of tubes, a plate of wells, or a flowcell of lanes
 */
@Embeddable
public class VesselContainer<T extends LabVessel> {
    /* rack holds tubes, tubes have barcodes and can be removed.
    * plate holds wells, wells can't be removed.
    * flowcell holds lanes.
    * PTP holds regions.
    * smartpac holds smrtcells, smrtcells are removed, but not replaced.
    * striptube holds tubes, tubes can't be removed, don't have barcodes. */
    private Map<String, T> mapPositionToVessel = new HashMap<String, T>();

    @Parent
    private LabVessel embedder;

    public VesselContainer() {
    }

    public VesselContainer(LabVessel embedder) {
        this.embedder = embedder;
    }

    // todo jmt enum for positions
    public T getVesselAtPosition(String position) {
        return this.mapPositionToVessel.get(position);
    }
    /**
     * If this is a plate, this method could return
     * the {@link PlateWell wells}.  If this thing
     * is a {@link RackOfTubes}, this method could
     * return the {@link TwoDBarcodedTube} tubes in
     * the rack.
     * @return contained vessels
     */
    public Collection<T> getContainedVessels() {
        return this.mapPositionToVessel.values();
    }

    public void addContainedVessel(T child, String position) {
        // todo jmt set reference to parent
        this.mapPositionToVessel.put(position, child);
    }

    public Set<String> getPositions() {
        return this.mapPositionToVessel.keySet();
    }

    public LabVessel getEmbedder() {
        return embedder;
    }

    public void setEmbedder(LabVessel embedder) {
        this.embedder = embedder;
    }

    // section transfers
    // position transfers
    // authorities
}

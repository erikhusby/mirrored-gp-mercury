package org.broadinstitute.sequel.entity.labevent;

import org.broadinstitute.sequel.entity.vessel.LabVessel;
import org.broadinstitute.sequel.entity.vessel.VesselContainer;

/**
 * Represents a transfer from a tube to all positions (wells) in a (plate) section
 */
public class VesselToSectionTransfer {
    private LabVessel sourceVessel;
    private String targetSection;
    private VesselContainer targetVesselContainer;

    public VesselToSectionTransfer(LabVessel sourceVessel, String targetSection, VesselContainer targetVesselContainer) {
        this.sourceVessel = sourceVessel;
        this.targetSection = targetSection;
        this.targetVesselContainer = targetVesselContainer;
    }

    public LabVessel getSourceVessel() {
        return sourceVessel;
    }

    public String getTargetSection() {
        return targetSection;
    }

    public VesselContainer getTargetVesselContainer() {
        return targetVesselContainer;
    }
}

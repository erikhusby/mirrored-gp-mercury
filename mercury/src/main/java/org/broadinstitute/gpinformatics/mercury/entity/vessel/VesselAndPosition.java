package org.broadinstitute.gpinformatics.mercury.entity.vessel;

/**
 * @author breilly
 */
public class VesselAndPosition {

    private LabVessel vessel;
    private VesselPosition position;

    public VesselAndPosition(LabVessel vessel, VesselPosition position) {
        this.vessel = vessel;
        this.position = position;
    }

    public LabVessel getVessel() {
        return vessel;
    }

    public VesselPosition getPosition() {
        return position;
    }
}

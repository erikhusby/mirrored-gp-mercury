package org.broadinstitute.gpinformatics.mercury.entity.vessel;

import javax.annotation.Nonnull;

/**
 * @author breilly
 */
public class VesselAndPosition {

    private LabVessel vessel;
    private VesselPosition position;

    public VesselAndPosition(@Nonnull LabVessel vessel, @Nonnull VesselPosition position) {
        if (vessel == null) {
            throw new RuntimeException("Vessel must not be null");
        }
        if (position == null) {
            throw new RuntimeException("Position must not be null");
        }
        this.vessel = vessel;
        this.position = position;
    }

    public LabVessel getVessel() {
        return vessel;
    }

    public VesselPosition getPosition() {
        return position;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        VesselAndPosition that = (VesselAndPosition) o;

        if (position != that.position) return false;
        if (!vessel.equals(that.vessel)) return false;

        return true;
    }

    @Override
    public int hashCode() {
        int result = vessel.hashCode();
        result = 31 * result + position.hashCode();
        return result;
    }

    @Override
    public String toString() {
        return "VesselAndPosition{" +
                "vessel=" + vessel +
                ", position=" + position +
                '}';
    }
}

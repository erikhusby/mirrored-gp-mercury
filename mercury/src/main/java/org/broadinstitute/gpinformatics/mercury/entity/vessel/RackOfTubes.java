package org.broadinstitute.gpinformatics.mercury.entity.vessel;

import org.hibernate.envers.Audited;

import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;

/**
 * A piece of plastic that holds tubes.  Can be reused to hold different sets of tubes.
 * Compare to TubeFormation.
 */
@Entity
@Audited
public class RackOfTubes extends LabVessel{

    /** For JPA */
    RackOfTubes() {
    }

    // todo jmt create interface implemented by this and PlateType, to get display name and geometry.
    public enum RackType {
        Matrix96("Matrix96", VesselGeometry.G12x8);

        private final String displayName;
        private final VesselGeometry vesselGeometry;

        RackType(String displayName, VesselGeometry vesselGeometry) {
            this.displayName = displayName;
            this.vesselGeometry = vesselGeometry;
        }

        public String getDisplayName() {
            return displayName;
        }

        public VesselGeometry getVesselGeometry() {
            return vesselGeometry;
        }
    }

    @Enumerated(EnumType.STRING)
    private RackType rackType;

    public RackOfTubes(String label, RackType rackType) {
        super(label);
        this.rackType = rackType;
    }

    public RackType getRackType() {
        return rackType;
    }

    @Override
    public VesselGeometry getVesselGeometry() {
        return rackType.getVesselGeometry();
    }

    @Override
    public CONTAINER_TYPE getType() {
        return CONTAINER_TYPE.RACK_OF_TUBES;
    }

}

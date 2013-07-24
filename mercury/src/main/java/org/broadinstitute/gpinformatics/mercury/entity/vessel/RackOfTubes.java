package org.broadinstitute.gpinformatics.mercury.entity.vessel;

import org.hibernate.envers.Audited;

import javax.persistence.CascadeType;
import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.ManyToMany;
import java.util.HashSet;
import java.util.Set;

/**
 * A piece of plastic that holds tubes.  Can be reused to hold different sets of tubes.
 * Compare to TubeFormation.
 */
@Entity
@Audited
public class RackOfTubes extends LabVessel {

    @ManyToMany(cascade = CascadeType.PERSIST, mappedBy = "racksOfTubes")
    private Set<TubeFormation> tubeFormations = new HashSet<>();

    /**
     * For JPA
     */
    protected RackOfTubes() {
    }

    // todo jmt create interface implemented by this and PlateType, to get display name and geometry.
    public enum RackType {
        Matrix96("Matrix96", VesselGeometry.G12x8);

        private final String         displayName;
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
    public ContainerType getType() {
        return ContainerType.RACK_OF_TUBES;
    }

    public Set<TubeFormation> getTubeFormations() {
        return tubeFormations;
    }
}

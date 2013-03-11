package org.broadinstitute.gpinformatics.mercury.entity.vessel;

import org.hibernate.envers.Audited;

import javax.persistence.Entity;

/**
 * One of many (usually 8) tubes in a StripTube
 */
@Entity
@Audited
public class StripTubeWell extends LabVessel {

    protected StripTubeWell(String label) {
        super(label);
    }

    protected StripTubeWell() {
    }

    @Override
    public VesselGeometry getVesselGeometry() {
        return VesselGeometry.STRIP_TUBE_WELL;
    }

    @Override
    public ContainerType getType() {
        return ContainerType.STRIP_TUBE_WELL;
    }

}

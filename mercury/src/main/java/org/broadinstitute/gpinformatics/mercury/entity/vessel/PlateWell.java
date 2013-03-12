package org.broadinstitute.gpinformatics.mercury.entity.vessel;

import org.hibernate.envers.Audited;

import javax.persistence.*;
@Entity
@Audited
public class PlateWell extends LabVessel {

    @ManyToOne(fetch = FetchType.LAZY)
    private StaticPlate plate;

    @Enumerated(EnumType.STRING)
    private VesselPosition vesselPosition;
    
    public PlateWell(StaticPlate p,VesselPosition vesselPosition) {
        super(p.getLabel() + vesselPosition);
        this.plate = p;
        this.vesselPosition = vesselPosition;
    }

    public PlateWell() {
    }

    @Override
    public VesselGeometry getVesselGeometry() {
        return VesselGeometry.WELL;
    }

    @Override
    public ContainerType getType() {
        return ContainerType.PLATE_WELL;
    }

}

package org.broadinstitute.gpinformatics.mercury.entity.vessel;

import org.hibernate.envers.Audited;

import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.FetchType;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
@Entity
@Audited
public class PlateWell extends LabVessel {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "PLATE")
    private StaticPlate plate;

    @Enumerated(EnumType.STRING)
    private VesselPosition vesselPosition;
    
    public PlateWell(StaticPlate staticPlate, VesselPosition vesselPosition) {
        super(staticPlate.getLabel() + vesselPosition);
        this.plate = staticPlate;
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

    public StaticPlate getPlate() {
        return plate;
    }

    public VesselPosition getVesselPosition() {
        return vesselPosition;
    }
}

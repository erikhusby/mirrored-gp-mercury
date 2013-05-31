package org.broadinstitute.gpinformatics.mercury.entity.vessel;

import org.hibernate.envers.Audited;

import javax.persistence.Entity;

/**
 * Simplistic representation of a MiSeq Reagent kit, AKA: Reagent Block
 */
@Entity
@Audited
public class MiSeqReagentKit extends StaticPlate  {
    public static final String LOADING_WELL = "D4";

    public MiSeqReagentKit(String label) {
        super(label, PlateType.Matrix96);
    }

    public MiSeqReagentKit() {
    }

    @Override
    public VesselGeometry getVesselGeometry() {
        return VesselGeometry.TUBE;
    }

    @Override
    public ContainerType getType() {
        return ContainerType.MISEQ_REAGENT_KIT;
    }
}

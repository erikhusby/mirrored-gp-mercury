package org.broadinstitute.gpinformatics.mercury.entity.vessel;

import org.hibernate.envers.Audited;

import javax.persistence.Entity;

/**
 * Simplistic representation of a MiSeq Reagent kit, AKA: Reagent Block
 */
@Entity
@Audited
public class MiSeqReagentKit extends StaticPlate  {
    public static final VesselPosition LOADING_WELL = VesselPosition.D04;
    public MiSeqReagentKit(String label) {
        super(label, PlateType.MiSeqReagentKit);
    }

    protected MiSeqReagentKit() {
    }

    @Override
    public VesselGeometry getVesselGeometry() {
        return VesselGeometry.MISEQ_REAGENT_KIT;
    }

    @Override
    public ContainerType getType() {
        return ContainerType.MISEQ_REAGENT_KIT;
    }
}

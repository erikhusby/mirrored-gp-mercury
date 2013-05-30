package org.broadinstitute.gpinformatics.mercury.entity.vessel;

import org.hibernate.envers.Audited;

import javax.annotation.Nonnull;
import javax.persistence.Entity;

/**
 * Simplistic representation of a MiSeq Reagent kit, AKA: Reagent Block
 */
@Entity
@Audited
public class MiSeqReagentKit extends LabVessel  {
    public MiSeqReagentKit(@Nonnull String label) {
        super(label);
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

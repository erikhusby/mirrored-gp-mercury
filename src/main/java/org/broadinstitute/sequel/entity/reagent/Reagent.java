package org.broadinstitute.sequel.entity.reagent;

import org.broadinstitute.sequel.entity.vessel.Containable;
import org.broadinstitute.sequel.entity.vessel.MolecularEnvelope;

import javax.persistence.Entity;
import javax.persistence.Id;

/**
 * Some chemistry bits applied to {@link Goop} to help
 * transform it into a sequenceable state.
 * 
 * Basic rule of thumb: Things that you want to
 * sequence are {@link Goop}.  Things that the lab
 * consumes from other vendors (IDT, Fluidigm,
 * Illumina, etc.) are {@link Reagent}s.  Oligos
 * like primers and baits are not {@link Goop}.
 * Although they contain DNA, they are considered
 * {@link Reagent}s.
 */
@Entity
public abstract class Reagent implements Containable {

    @Id
    private Long reagentId;

    /**
     * Returns the MolecularEnvelope that this
     * reagent applies to the target sample.
     * @return
     */
    public abstract MolecularEnvelope getMolecularEnvelopeDelta();

    public abstract String getReagentName();

    public abstract String getLot();

}

package org.broadinstitute.sequel;

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
public interface Reagent extends Containable {

    /**
     * Returns the MolecularEnvelope that this
     * reagent applies to the target sample.
     * @return
     */
    public MolecularEnvelope getMolecularEnvelopeDelta();

    public String getReagentName();

    public String getLot();

}

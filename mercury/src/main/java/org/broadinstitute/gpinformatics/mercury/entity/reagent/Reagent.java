package org.broadinstitute.gpinformatics.mercury.entity.reagent;

import org.broadinstitute.gpinformatics.mercury.entity.vessel.Containable;
import org.broadinstitute.gpinformatics.mercury.entity.vessel.MolecularEnvelope;
import org.hibernate.envers.Audited;

import javax.persistence.CascadeType;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.ManyToOne;
import javax.persistence.SequenceGenerator;
import javax.persistence.Table;

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
@Audited
@Table(schema = "mercury")
public abstract class Reagent implements Containable {

    @Id
    @SequenceGenerator(name = "SEQ_REAGENT", schema = "mercury", sequenceName = "SEQ_REAGENT")
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "SEQ_REAGENT")
    private Long reagentId;

    private String reagentName;

    private String lot;

    @ManyToOne(cascade = CascadeType.PERSIST)
    private MolecularEnvelope molecularEnvelope;

    protected Reagent(String reagentName, String lot, MolecularEnvelope molecularEnvelope) {
        this.reagentName = reagentName;
        this.lot = lot;
        this.molecularEnvelope = molecularEnvelope;
    }

    protected Reagent() {
    }

    /**
     * Returns the MolecularEnvelope that this
     * reagent applies to the target sample.
     * @return
     */
    public MolecularEnvelope getMolecularEnvelopeDelta() {
        return molecularEnvelope;
    }

    public String getReagentName() {
        return reagentName;
    }

    public void setReagentName(String reagentName) {
        this.reagentName = reagentName;
    }

    public String getLot() {
        return lot;
    }

    public void setLot(String lot) {
        this.lot = lot;
    }
}

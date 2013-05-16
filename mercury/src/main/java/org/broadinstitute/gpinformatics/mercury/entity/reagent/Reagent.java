package org.broadinstitute.gpinformatics.mercury.entity.reagent;

import org.hibernate.envers.Audited;

import javax.persistence.*;

/**
 * Some chemistry bits applied to Goop to help transform it into a sequenceable state.
 * 
 * Basic rule of thumb: Things that you want to sequence are Goop.  Things that the lab consumes from other
 * vendors (IDT, Fluidigm, Illumina, etc.) are {@link Reagent}s.  Oligos like primers and baits are not Goop.
 * Although they contain DNA, they are considered {@link Reagent}s.
 */
@Entity
@Audited
@Table(schema = "mercury")
public abstract class Reagent {

    @Id
    @SequenceGenerator(name = "SEQ_REAGENT", schema = "mercury", sequenceName = "SEQ_REAGENT")
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "SEQ_REAGENT")
    private Long reagentId;

    @Column(name = "REAGENT_NAME")
    private String name;

    @Column(name = "LOT")
    private String lot;

    protected Reagent(String reagentName, String lot) {
        this.name = reagentName;
        this.lot = lot;
    }

    protected Reagent() {
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getLot() {
        return lot;
    }

    public void setLot(String lot) {
        this.lot = lot;
    }
}

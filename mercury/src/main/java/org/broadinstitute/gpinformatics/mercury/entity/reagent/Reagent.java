package org.broadinstitute.gpinformatics.mercury.entity.reagent;

import org.hibernate.envers.Audited;

import javax.annotation.Nullable;
import javax.persistence.*;
import java.util.Date;

/**
 * Some chemistry bits applied to a Sample to help transform it into a sequenceable state.
 * 
 * Basic rule of thumb: Things that you want to sequence are Samples.  Things that the lab consumes from other
 * vendors (IDT, Fluidigm, Illumina, etc.) are {@link Reagent}s.  Oligos like primers and baits are not Samples.
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

    @Column(name = "REAGENT_NAME", nullable = true)
    private String name;

    @Column(name = "LOT", nullable = true)
    private String lot;

    @Column(name = "EXPIRATION", nullable = true)
    private Date expiration;

    protected Reagent(@Nullable String reagentName, @Nullable String lot, @Nullable Date expiration) {
        this.name = reagentName;
        this.lot = lot;
        this.expiration = expiration;
    }

    protected Reagent() {
    }

    public Long getReagentId(){
        return reagentId;
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

    public Date getExpiration() {
        return expiration;
    }

    public void setExpiration(Date expiration) {
        this.expiration = expiration;
    }
}

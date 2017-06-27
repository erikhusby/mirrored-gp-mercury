package org.broadinstitute.gpinformatics.mercury.entity.reagent;

import org.broadinstitute.gpinformatics.mercury.entity.labevent.LabEventReagent;
import org.hibernate.annotations.BatchSize;
import org.hibernate.envers.Audited;

import javax.annotation.Nullable;
import javax.persistence.*;
import java.util.Comparator;
import java.util.Date;
import java.util.HashSet;
import java.util.Set;

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

    /**
     * Used for Reagent list sorting
     */
    public static final Comparator<Reagent> BY_NAME_LOT_EXP = new Comparator<Reagent>() {
        @Override
        public int compare(Reagent o1, Reagent o2) {
            int result = compareField( o1.getName(), o2.getName() );
            if( result == 0 ) {
                result = compareField( o1.getLot(), o2.getLot() );
            }
            if( result == 0 ) {
                result = compareField(o1.getExpiration(), o2.getExpiration());
            }
            return result;
        }

        /**
         * Everything nullable and either String or Date
         */
        private int compareField(Comparable me, Comparable you) {
            if( me == null && you == null ) {
                return 0;
            } else if( me == null && you != null ) {
                return -1;
            } else if( me != null && you == null ) {
                return 1;
            } else {
                return me.compareTo(you);
            }
        }
    };


    @Id
    @SequenceGenerator(name = "SEQ_REAGENT", schema = "mercury", sequenceName = "SEQ_REAGENT")
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "SEQ_REAGENT")
    @Column(name = "REAGENT_ID", nullable = true)
    private Long reagentId;

    @Column(name = "REAGENT_NAME", nullable = true)
    private String name;

    @Column(name = "LOT", nullable = true)
    private String lot;

    @Column(name = "EXPIRATION", nullable = true)
    private Date expiration;

    @Column(name = "FIRST_USE", nullable = true)
    private Date firstUse;

    @OneToMany(cascade = {CascadeType.PERSIST, CascadeType.REMOVE}, mappedBy = "reagent")
    @BatchSize(size = 100)
    private Set<LabEventReagent> labEventReagents = new HashSet<>();

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

    public Set<LabEventReagent> getLabEventReagents() {
        return labEventReagents;
    }

    public Date getFirstUse() {
        return firstUse;
    }

    public void setFirstUse(Date firstUse) {
        this.firstUse = firstUse;
    }
}

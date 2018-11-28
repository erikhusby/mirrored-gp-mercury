package org.broadinstitute.gpinformatics.mercury.entity.labevent;

import org.broadinstitute.gpinformatics.mercury.entity.Metadata;
import org.broadinstitute.gpinformatics.mercury.entity.reagent.Reagent;
import org.hibernate.envers.Audited;

import javax.persistence.CascadeType;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.JoinTable;
import javax.persistence.ManyToMany;
import javax.persistence.ManyToOne;
import javax.persistence.PrePersist;
import javax.persistence.SequenceGenerator;
import javax.persistence.Table;
import javax.persistence.UniqueConstraint;
import java.math.BigDecimal;
import java.util.HashSet;
import java.util.Set;

/**
 * A many-to-many table to record use of Reagents in LabEvents.
 */
@Entity
@Audited
@Table(schema = "mercury", name = "lab_event_reagents",
        uniqueConstraints = @UniqueConstraint(columnNames = {"LAB_EVENT", "REAGENTS"})
)
public class LabEventReagent {

    @SuppressWarnings("unused")
    @Id
    @SequenceGenerator(name = "SEQ_LAB_EVENT_REAGENT", schema = "mercury", sequenceName = "SEQ_LAB_EVENT_REAGENT")
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "SEQ_LAB_EVENT_REAGENT")
    @Column(name = "LAB_EVENT_REAGENT_ID")
    private Long labEventReagentId;

    @ManyToOne
    @JoinColumn(name = "LAB_EVENT")
    private LabEvent labEvent;

    @ManyToOne(cascade = CascadeType.PERSIST)
    @JoinColumn(name = "REAGENTS")
    private Reagent reagent;

    @ManyToMany(cascade = CascadeType.PERSIST)
    @JoinTable(name = "le_reagent_metadata", schema = "mercury",
            joinColumns = @JoinColumn(name = "LAB_EVENT_REAGENT_ID"),
            inverseJoinColumns = @JoinColumn(name = "METADATA_ID"))
    private Set<Metadata> metadata = new HashSet<>();

    private BigDecimal volume;

    /** For JPA. */
    protected LabEventReagent() {
    }

    public LabEventReagent(LabEvent labEvent, Reagent reagent) {
        this.labEvent = labEvent;
        this.reagent = reagent;
    }

    public LabEventReagent(LabEvent labEvent, Reagent reagent, BigDecimal volume) {
        this(labEvent, reagent);
        this.volume = volume;
    }

    public LabEventReagent(LabEvent labEvent, Reagent reagent, Set<Metadata> metadataSet) {
        this(labEvent, reagent);
        this.metadata = metadataSet;
    }

    public LabEvent getLabEvent() {
        return labEvent;
    }

    public Reagent getReagent() {
        return reagent;
    }

    public BigDecimal getVolume() {
        return volume;
    }

    public void setVolume(BigDecimal volume) {
        this.volume = volume;
    }

    public Set<Metadata> getMetadata() {
        return metadata;
    }

    public void setMetadata(Set<Metadata> metadata) {
        this.metadata = metadata;
    }

    @PrePersist
    public void prePersist(){
        if( reagent.getFirstUse() == null ) {
            reagent.setFirstUse( labEvent.getEventDate() );
        }
    }
}

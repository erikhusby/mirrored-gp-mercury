package org.broadinstitute.gpinformatics.mercury.entity.labevent;

import org.broadinstitute.gpinformatics.mercury.entity.reagent.Reagent;
import org.hibernate.envers.Audited;

import javax.persistence.EmbeddedId;
import javax.persistence.Entity;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.MapsId;
import javax.persistence.Table;
import java.math.BigDecimal;

/**
 * A many-to-many table to record use of Reagents in LabEvents.
 */
@Entity
@Audited
@Table(schema = "mercury", name = "lab_event_reagents")
public class LabEventReagent {

    @EmbeddedId
    private LabEventReagentKey labEventReagentKey = new LabEventReagentKey();

    @MapsId("labEvent")
    @ManyToOne
    @JoinColumn(name = "LAB_EVENT")
    private LabEvent labEvent;

    @MapsId("reagents")
    @ManyToOne
    @JoinColumn(name = "REAGENTS")
    private Reagent reagent;

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
}

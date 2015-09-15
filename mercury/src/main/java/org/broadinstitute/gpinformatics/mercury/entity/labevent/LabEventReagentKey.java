package org.broadinstitute.gpinformatics.mercury.entity.labevent;

import javax.persistence.Embeddable;
import java.io.Serializable;

/**
 * Composite primary key for LabEventReagent.
 */
@Embeddable
public class LabEventReagentKey implements Serializable {
    private Long labEvent;
    private Long reagents;

    public LabEventReagentKey(Long labEvent, Long reagents) {
        this.labEvent = labEvent;
        this.reagents = reagents;
    }

    protected LabEventReagentKey() {
    }

    public Long getLabEvent() {
        return labEvent;
    }

    public void setLabEvent(Long labEvent) {
        this.labEvent = labEvent;
    }

    public Long getReagents() {
        return reagents;
    }

    public void setReagents(Long reagents) {
        this.reagents = reagents;
    }
}

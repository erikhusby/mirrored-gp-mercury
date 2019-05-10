package org.broadinstitute.gpinformatics.mercury.control.hsa.state;

import org.broadinstitute.gpinformatics.mercury.entity.run.IlluminaSequencingRunChamber;
import org.hibernate.envers.Audited;

import javax.persistence.CascadeType;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.Table;

@Entity
@Audited
public class DemultiplexState extends State {

    @ManyToOne(fetch = FetchType.LAZY, cascade = {CascadeType.PERSIST})
    @JoinColumn(name = "RUN_CHAMBER")
    private IlluminaSequencingRunChamber runChamber;

    protected DemultiplexState() {
    }

    public DemultiplexState(String name, FiniteStateMachine finiteStateMachine, IlluminaSequencingRunChamber runChamber) {
        super(name, finiteStateMachine);
        this.runChamber = runChamber;
    }

    public IlluminaSequencingRunChamber getRunChamber() {
        return runChamber;
    }

    public void setRunChamber(IlluminaSequencingRunChamber runChamber) {
        this.runChamber = runChamber;
    }
}

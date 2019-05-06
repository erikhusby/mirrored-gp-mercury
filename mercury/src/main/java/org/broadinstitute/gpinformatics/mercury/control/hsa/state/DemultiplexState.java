package org.broadinstitute.gpinformatics.mercury.control.hsa.state;

import org.broadinstitute.gpinformatics.mercury.entity.run.IlluminaSequencingRunChamber;
import org.hibernate.envers.Audited;

import javax.persistence.Entity;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.Table;

@Entity
@Audited
@Table(schema = "mercury")
public class DemultiplexState extends State {

    @ManyToOne
    @JoinColumn(name = "RUN_CHAMBER")
    private IlluminaSequencingRunChamber runChamber;

    protected DemultiplexState() {
    }

    public DemultiplexState(String name, IlluminaSequencingRunChamber runChamber) {
        super(name);
        this.runChamber = runChamber;
    }

    public IlluminaSequencingRunChamber getRunChamber() {
        return runChamber;
    }

    public void setRunChamber(IlluminaSequencingRunChamber runChamber) {
        this.runChamber = runChamber;
    }
}

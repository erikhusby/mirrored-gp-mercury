package org.broadinstitute.gpinformatics.mercury.entity.run;

import org.hibernate.envers.Audited;

import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.ManyToOne;
import javax.persistence.SequenceGenerator;
import javax.persistence.Table;

/**
 * Holds run chamber (lane) level information about a run.
 */
@Entity
@Audited
@Table(schema = "mercury")
public class SequencingRunChamber {

    @SequenceGenerator(name = "SEQ_SEQUENCING_RUN_CHAMBER", schema = "mercury", sequenceName = "SEQ_SEQUENCING_RUN_CHAMBER")
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "SEQ_SEQUENCING_RUN_CHAMBER")
    @Id
    private Long sequencingRunChamberId;

    @ManyToOne
    private SequencingRun sequencingRun;

    @ManyToOne
    private RunChamber runChamber;

    private String actualReadStructure;

    /** For JPA. */
    protected SequencingRunChamber() {
    }

    public SequencingRunChamber(SequencingRun sequencingRun,
            RunChamber runChamber) {
        this.sequencingRun = sequencingRun;
        this.runChamber = runChamber;
    }
}

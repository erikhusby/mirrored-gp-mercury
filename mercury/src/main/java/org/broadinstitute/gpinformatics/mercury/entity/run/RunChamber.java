package org.broadinstitute.gpinformatics.mercury.entity.run;

import org.broadinstitute.gpinformatics.mercury.entity.vessel.LabVessel;
import org.hibernate.envers.Audited;

import javax.persistence.Entity;
import javax.persistence.OneToMany;
import java.util.HashSet;
import java.util.Set;

/**
 * A "subsection" of a run cartridge.
 * PTP region, flowcell lane, sequencing plate
 * well, etc.
 *
 * In the illumina model, things like
 * read length must be uniform across
 * the flowcell, but as technology improves,
 * this will likely move down to the
 * chamber to accommodate a heterogeneous
 * set of "run configurations" in a single run.
 *
 * Question: at what point does one stop
 * making run chambers and just keep location
 * information as string metadata?  it's a
 * question of scale.  Is each microwell on
 * pacbio or each "well" on the PTP slide
 * a run chamber?  Maybe technically, but I
 * don't want 5 billion run chamber rows
 * in the database.  So let's say that a
 * run chamber is the finest granularity
 * that we can reliably load with sample.
 *
 */
@Entity
@Audited
public abstract class RunChamber extends LabVessel {

    /** For JPA. */
    protected RunChamber() {
    }

    public RunChamber(String label) {
        super(label);
    }

    public abstract String getChamberName();

}

package org.broadinstitute.gpinformatics.mercury.entity.run;

import org.broadinstitute.gpinformatics.mercury.entity.vessel.LabVessel;

/**
 * A "subsection" of a run cartridge.
 * PTP region, flowcell lane, sequencing plate
 * well, etc.
 *
 * In the illumina model, things like
 * read length must be uniform across
 * the flowcell, but as technology improves,
 * this will likely move down to the
 * chamber to accomodate a hetergenous
 * set of "run configrations" in a single run.
 *
 * Question: at what point does one stop
 * making run chambers and just keep location
 * information as string metadata?  it's a
 * question of scale.  Is each microwell on
 * pacbio or each "well" on the PTP slide
 * a run chamber?  Maybe technically, but I
 * don't want 5 billion run chamber rows
 * in the database.  So let's say that a
 * run chamber is the finest granulatiry
 * that we can reliably load with sample.
 *
 */
public abstract class RunChamber extends LabVessel {

    public RunChamber(String label) {
        super(label);
    }

    /**
     * Does this belong on the chamber or the run
     * configuration?  Not sure it makes much differenct.
     * Basically a pointer to the raw sequencer output.
     * @return
     */
    public abstract Iterable<OutputDataLocation> getDataDirectories();

    public abstract String getChamberName();

}

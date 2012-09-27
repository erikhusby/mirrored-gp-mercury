package org.broadinstitute.gpinformatics.mercury.entity.queue;

/**
 * Parameters for a particular lab work queue.
 *
 * For the designation queue, this is stuff
 * like cycle count, miseq/hiseq.
 *
 * For aliquoting from BSP, this is stuff
 * like volume, concentration
 *
 * LabWorkQueues have a "bucket" for each
 * parameters instance, so you have to write
 * equals() and hashCode() here.
 */
public interface LabWorkQueueParameters {

    /**
     * Summarize the bucket in human readable
     * text.  For a 76bp paired HiSeq run,
     * this will say "76bp HiSeq Paired".
     *
     * For a shear target range of 200bp,
     * this will say "200bp"
     *
     * The returned string is textual instructions
     * for the person in the lab doing the work.
     * What do they need to know in order to
     * choose the right protocol?
     * @return
     */
    public String toText();
}

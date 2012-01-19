package org.broadinstitute.sequel;

/**
 * Basically a way to summarize certain critical
 * aspects of {@link MolecularState} to detect
 * when a {@link LabEvent} is going to result in
 * inconsistent {@link MolecularState}.
 * 
 * An odd feature falls out of the use of this
 * class: detection of out of order messages,
 * for the cases where out of order messages cause
 * us problems.
 * 
 * When we're not changing the {@link MolecularState}
 * during a {@link LabEvent}, it doesn't matter
 * that the event is happening out of order.  For
 * example, doing a straight transfer.  The
 * {@link LabEvent#getSourceLabVessels() sources}
 * and {@link LabEvent#getTargetLabVessels() targets}
 * share the same {@link SampleSheet}.
 * 
 * But when we start to {@link LabVessel#branchSampleSheets() branch the SampleSheet},
 * we have to worry about complex event chains in which an upstream
 * {@link MolecularState}-changing event is not received.
 * 
 * We have to be very careful here.  Is it okay
 * to have DNA and RNA in a pool?  What about
 * having samples which have adaptors
 * and samples which don't?  What
 * about samples which are indexed and
 * samples which aren't?
 * 
 * Recall that if we think that a {@link LabEvent} is
 * going to mangle the {@link MolecularState} of something,
 * we'll send out advisory notes (email, jira, etc.),
 * fail to set the {@link SampleInstance sample metadata} for
 * {@link LabEvent#getTargetLabVessels() targets of an event},
 * but still persist the fact that the event has happened.
 *
 * So we're not halting the lab process.  But we're
 * not filling in the metadata, which means that at some
 * point, things will break.
 */
public class MolecularStateTemplate {

    /**
     * Human readable representation, such as
     * "Envelope: Primer/Index/Index/Adaptor, Double Stranded DNA".
     * @return
     */
    public String toText() {
        throw new RuntimeException("Method not yet implemented.");
    }

    /**
     * Very important to implement this.  We detect {@link InconsistentMolecularState}
     * after building a Collection of these things
     * and checking the size of the collection.
     * @param other
     * @return
     */
    public boolean equals(Object other) {
        throw new RuntimeException("Method not yet implemented.");
    }

    /**
     * Very important.
     * @return
     */
    public int hashCode() {
        throw new RuntimeException("Method not yet implemented.");
    }
}

package org.broadinstitute.gpinformatics.mercury.entity.vessel;

import org.hibernate.envers.Audited;

import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.SequenceGenerator;
import javax.persistence.Table;

/**
 * Basically a way to summarize certain critical
 * aspects of {@link MolecularState} to detect
 * when a {@link org.broadinstitute.gpinformatics.mercury.entity.labevent.LabEvent} is going to result in
 * inconsistent {@link MolecularState}.
 * 
 * An odd feature falls out of the use of this
 * class: detection of out of order messages,
 * for the cases where out of order messages cause
 * us problems.
 * 
 * When we're not changing the {@link MolecularState}
 * during a {@link org.broadinstitute.gpinformatics.mercury.entity.labevent.LabEvent}, it doesn't matter
 * that the event is happening out of order.  For
 * example, doing a straight transfer.  The
 * {@link org.broadinstitute.gpinformatics.mercury.entity.labevent.LabEvent#getSourceLabVessels() sources}
 * and {@link org.broadinstitute.gpinformatics.mercury.entity.labevent.LabEvent#getTargetLabVessels() targets}
 * share the same {@link org.broadinstitute.gpinformatics.mercury.entity.sample.SampleSheet}.
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
 * Recall that if we think that a {@link org.broadinstitute.gpinformatics.mercury.entity.labevent.LabEvent} is
 * going to mangle the {@link MolecularState} of something,
 * we'll send out advisory notes (email, jira, etc.),
 * fail to set the {@link org.broadinstitute.gpinformatics.mercury.entity.sample.SampleInstance sample metadata} for
 * {@link org.broadinstitute.gpinformatics.mercury.entity.labevent.LabEvent#getTargetLabVessels() targets of an event},
 * but still persist the fact that the event has happened.
 *
 * So we're not halting the lab process.  But we're
 * not filling in the metadata, which means that at some
 * point, things will break.
 */
@Entity
@Audited
@Table(schema = "mercury")
public class MolecularStateTemplate {

    @Id
    @SequenceGenerator(name = "SEQ_MOLECULAR_STATE_TEMPLATE", schema = "mercury", sequenceName = "SEQ_MOLECULAR_STATE_TEMPLATE")
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "SEQ_MOLECULAR_STATE_TEMPLATE")
    private Long molecularStateTemplateId;

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

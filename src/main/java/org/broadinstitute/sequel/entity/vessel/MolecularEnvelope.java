package org.broadinstitute.sequel.entity.vessel;

import org.hibernate.envers.Audited;

import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.SequenceGenerator;

/**
 * Target DNA is surrounded with various
 * appendages (or adaptors--but in the
 * non-DNA sense of the word) to facilitate
 * its travel through various biological
 * operations.
 *
 * It's analagous to network envelopes/datagrams and
 * payloads.  Take
 * a piece of target DNA and put adaptors
 * on the 3' and 5' ends.  Then a piece
 * of machine can do step X to it.  When
 * that's done, attach another set
 * of adaptors to the output of that
 * step, and step Y can be applied.
 *
 * At the end of LC, you have a complicated
 * molecule, all of which we end up sequencing,
 * but some of which we throw away before
 * publishing the sequence of the target of interest.
 *
 * Need to link to a nice diagram in confluence
 * of how all this stuff comes together.  We
 * end up sequence a concatenation of various
 * envelopes, and we need to know where
 * the various adaptor sequences are and
 * where the target DNA of interest is.
 *
 * Equals() is very important here.  Equals() will
 * be used to determine whether the expected molecular
 * envelope for an event matches the molecular
 * envelope of (typically) the source in a transfer.
 */

@Entity
@Audited
public abstract class MolecularEnvelope {

    @Id
    @SequenceGenerator(name = "SEQ_MOLECULAR_ENVELOPE", sequenceName = "SEQ_MOLECULAR_ENVELOPE")
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "SEQ_MOLECULAR_ENVELOPE")
    private Long molecularEnvelopeId;

    public enum FUNCTIONAL_ROLE {
        INDEX,ADAPTOR,SEQUENCING_PRIMER,PCR_PRIMER
    }

    /**
     * Primer?  adaptor?  index? pcr primer?
     * @return
     */
    public abstract FUNCTIONAL_ROLE getFunctionalRole();

    /**
     * What DNA thing is on the 3' end of the molecular?
     * Might be null.
     * @return
     */
    public abstract MolecularAppendage get3PrimeAttachment();

    /**
     * What DNA thing is on the 3' end of the molecular?
     * Might be null.
     * @return
     */
    public abstract MolecularAppendage get5PrimeAttachment();

    // todo abstract class to implement getContainedEnvelope and surroundWith()

    /**
     * Is there another envelope inside this envelope?
     * Often there are multiple envelopes.  For
     * example, the insert DNA (target paylod of
     * ultimate interest) may be surrounded by sequencing
     * primers, which are surrounded by fluidigm adaptors,
     * which are in turn surrounded by molecular
     * indexes, which are in turn surrounded by ion, illumina,
     * or 454 library adaptors.
     *
     * When null is returned, you've reached the
     * target DNA.
     * @return
     */
    public abstract MolecularEnvelope getContainedEnvelope();

    /**
     * Add a surrounding envelope around this envelope.
     * If you've already added sequencing primers and
     * are about to attach adaptors, call this method
     * to attach your adaptors.
     * @param containingEnvelope
     */
    public abstract void surroundWith(MolecularEnvelope containingEnvelope);

    /**
     * Does the envelope contain this appendage, regardless
     * of position?
     * @param appendage
     * @return
     */
    public abstract boolean contains(MolecularAppendage appendage);

    public abstract boolean contains3Prime(MolecularAppendage appendage);

    public abstract boolean contains5Prime(MolecularAppendage appendage);
}

package org.broadinstitute.gpinformatics.mercury.entity.reagent;

import org.hibernate.envers.Audited;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.NamedQueries;
import javax.persistence.NamedQuery;
import javax.persistence.SequenceGenerator;
import javax.persistence.Table;
import javax.persistence.Transient;
import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;

/**
 * A sequence of nucleotides that serves to add a chemical identifier to a target sequence.
 * Legitimate sequences are non-empty and contain the letters A, C, T, G or U, but do not
 * contain both T *and* U.
 */
@Entity
@Audited
@Table(schema = "mercury")
public class MolecularIndex implements Serializable {
    public static final long serialVersionUID = 2011122101l;

    private static final Map<Character, Integer> BASE_INDEXES = new HashMap<Character, Integer>(4);
    static {
        BASE_INDEXES.put('A', 0);
        BASE_INDEXES.put('C', 1);
        BASE_INDEXES.put('G', 2);
        BASE_INDEXES.put('T', 3);
        BASE_INDEXES.put('U', 3); // Not a typo: T and U use the same index
    }
    private static final Integer EMPTY_INDEX = new Integer(4);

    private Long id;

    private String sequence;

    protected MolecularIndex() {
        // No-arg c'tor for Hibernate's sake
    }

    /**
     * All of the component molecular indexes should have been created in the database
     * right after initial release of the code, so it's likely that this constructor
     * should not be called.
     */
    public MolecularIndex(final String sequence) {
        this.setSequence(sequence);
    }

    @Id
    @SequenceGenerator(name="seq_molecular_index", schema = "mercury", sequenceName="seq_molecular_index")
    @GeneratedValue(strategy= GenerationType.SEQUENCE, generator="seq_molecular_index")
    @Column(name = "id")
    @SuppressWarnings("unused")
    private Long getId() {
        return id;
    }

    @SuppressWarnings("unused")
    private void setId(final Long id) {
        this.id = id;
    }

    /**
     * Generates and returns a name unique to a nucleotide sequence. Throws
     * IllegalArgumentException if the String contains characters other than
     * 'A', 'T', 'U', 'C' or 'G'.
     */
    @Transient
    String generateNameFromSequence() {

        // **THIS IMPLEMENTATION MUST NOT CHANGE** because finding MolecularIndexingSchemes
        // in the database relies on the strings produced here. Changing the values of these
        // arrays or the way in which they're accessed runs the risk of creating names that
        // collide with those already used.

        // For each triplet of nucleotides, this algorithm uses the first two bases to choose a
        // letter in the MAJOR_GROUP, then uses the third base to choose another from the
        // MINOR_GROUP. After evaluating all the triplets, an 's' character is appended if the
        // sequence contains Us instead of Ts. It's designed this way so that the output string
        // is slightly smaller than the input and stands some chance of being "pronounceable".
        // Also, this is a prefix coding, so the original sequence could, in theory, be
        // recovered from the name.
        //
        // Sadly I had to use the character 'u' in the MAJOR_GROUP. Sequences of size
        // 3n + 1 (where n is the number of triplets) and whose nucleotide at 3n is a T, will
        // have a name ending in 'u'. I figured this was the least obnoxious way to set this up.
        //
        //
        //
        // -jrose, 12/21/11

        final String[] MAJOR_GROUP = new String[] {
                "b", "c", "d", "f", "g",
                "h", "j", "k", "l", "m",
                "n", "p", "r", "t", "v",
                "w", "x", "y", "z", "u"
        };

        // Only four here because we'll never encode an empty base
        final String[] MINOR_GROUP = new String[] {
                "a", "e", "i", "o"
        };

        final StringBuilder builder = new StringBuilder();
        for (int i = 0; i < this.getSequence().length() ; ) {
            // NB that incrementing i happens three times within this loop
            final Integer majorGroupMultiplier = BASE_INDEXES.get(this.getSequence().charAt(i++));
            final Integer majorGroupAdder = (i < this.getSequence().length()) ? BASE_INDEXES.get(this.sequence.charAt(i++)) : EMPTY_INDEX;
            if (majorGroupMultiplier == null || majorGroupAdder == null) {
                throw new IllegalArgumentException("The sequence " + this.getSequence() + " contains an illegal base (major group).");
            }
            builder.append(MAJOR_GROUP[majorGroupMultiplier * 5 + majorGroupAdder]);

            if (this.getSequence().length() > i) {
                final Integer minorGroupIndex = BASE_INDEXES.get(this.sequence.charAt(i++));
                if (minorGroupIndex == null) {
                    throw new IllegalArgumentException("The sequence " + this.getSequence() + " contains an illegal base (minor group).");
                }
                builder.append(MINOR_GROUP[minorGroupIndex]);
            }
        }

        if (this.sequence.contains("U")) builder.append("s");

        // Turn this into a proper noun by capitalizing the first letter
        final String name = builder.toString();
        return name.substring(0, 1).toUpperCase() + name.substring(1);
    }

    /**
     * Returns the sequence of bases that form this molecular index. Guaranteed to never be null
     * or have a length of zero.
     */
//    @Column(name = "sequence", nullable = false)
    public String getSequence() {
        return this.sequence;
    }

    /*
      * @see #getSequence()
      *
      * The provided value must never be null or empty, and must not contain both
      * Ts and Us.
      */
    @SuppressWarnings("unused")
    private void setSequence(final String sequence) {
        if (sequence ==  null) throw new NullPointerException("A non-null sequence is required.");
        if (sequence.isEmpty()) throw new IllegalArgumentException("The sequence must have a length greater than zero.");

        final String upperSequence = sequence.trim().toUpperCase();

        if (upperSequence.contains("U") && upperSequence.contains("T")) {
            throw new IllegalArgumentException("Cannot have both T and U in the same sequence.");
        }

        for (final char base : upperSequence.toCharArray()) {
            if ( ! BASE_INDEXES.containsKey(base)) {
                throw new IllegalArgumentException("The sequence must only have the letters A, C, T, G, or U, not " + base);
            }
        }

        this.sequence = upperSequence;
    }

    /**
     * The sequence of a MolecularIndex determines whether one instance is identical
     * to another.
     */
    @Override
    public boolean equals(final Object other) {
        if (this == other) return true;
        if ( ! (other instanceof MolecularIndex)) return false;

        return ((MolecularIndex) other).getSequence().equals(this.getSequence());
    }

    @Override
    public int hashCode() {
        return this.getSequence().hashCode();
    }
}

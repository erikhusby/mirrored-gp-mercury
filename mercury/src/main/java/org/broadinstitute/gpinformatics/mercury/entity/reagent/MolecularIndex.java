package org.broadinstitute.gpinformatics.mercury.entity.reagent;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.tuple.Pair;
import org.hibernate.annotations.BatchSize;
import org.hibernate.envers.Audited;

import javax.persistence.CascadeType;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.ManyToMany;
import javax.persistence.SequenceGenerator;
import javax.persistence.Table;
import javax.persistence.Transient;
import javax.persistence.UniqueConstraint;
import java.io.Serializable;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * A sequence of nucleotides that serves to add a chemical identifier to a target sequence.
 * Legitimate sequences are non-empty and contain the letters A, C, T, G or U, but do not
 * contain both T *and* U.
 */
@Entity
@Audited
@Table(schema = "mercury", uniqueConstraints = @UniqueConstraint(name = "uk_mi_sequence", columnNames = {"sequence"}))
public class MolecularIndex implements Serializable {
    private static final long serialVersionUID = 2011122101L;

    private static final Map<Character, Integer> BASE_INDEXES = new HashMap<>(4);
    static {
        BASE_INDEXES.put('A', 0);
        BASE_INDEXES.put('C', 1);
        BASE_INDEXES.put('G', 2);
        BASE_INDEXES.put('T', 3);
        BASE_INDEXES.put('U', 3); // Not a typo: T and U use the same index
    }
    private static final int EMPTY_INDEX = 4;

    @Id
    @SequenceGenerator(name="seq_molecular_index", schema = "mercury", sequenceName="seq_molecular_index")
    @GeneratedValue(strategy= GenerationType.SEQUENCE, generator="seq_molecular_index")
    @Column(name = "molecular_index_id")
    private Long molecularIndexId;

    @ManyToMany(cascade = {CascadeType.PERSIST}, fetch= FetchType.LAZY, mappedBy = "indexes")
    @BatchSize(size = 500)
    private Set<MolecularIndexingScheme> molecularIndexingSchemes = new HashSet<>();

    private String sequence;

    protected MolecularIndex() {
        // No-arg c'tor for Hibernate's sake
    }

    /** Constructor for creating a new index. */
    public MolecularIndex(String sequence) {
        setSequence(sequence);
    }

    @SuppressWarnings("unused")
    private Long getMolecularIndexId() {
        return molecularIndexId;
    }

    @SuppressWarnings("unused")
    private void setMolecularIndexId(Long molecularIndexId) {
        this.molecularIndexId = molecularIndexId;
    }

    private static final String[] MAJOR_GROUP = {
            "b", "c", "d", "f", "g",
            "h", "j", "k", "l", "m",
            "n", "p", "r", "t", "v",
            "w", "x", "y", "z", "u"
    };

    // Only four here because we'll never encode an empty base
    private static final String[] MINOR_GROUP = {
            "a", "e", "i", "o"
    };

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


        StringBuilder builder = new StringBuilder();
        for (int i = 0; i < getSequence().length() ; ) {
            // NB that incrementing i happens three times within this loop
            Integer majorGroupMultiplier = BASE_INDEXES.get(getSequence().charAt(i++));
            Integer majorGroupAdder = (i < getSequence().length()) ? BASE_INDEXES.get(sequence.charAt(i++)) : EMPTY_INDEX;
            if (majorGroupMultiplier == null || majorGroupAdder == null) {
                throw new IllegalArgumentException("The sequence " + getSequence() + " contains an illegal base (major group).");
            }
            builder.append(MAJOR_GROUP[majorGroupMultiplier * 5 + majorGroupAdder]);

            if (getSequence().length() > i) {
                Integer minorGroupIndex = BASE_INDEXES.get(sequence.charAt(i++));
                if (minorGroupIndex == null) {
                    throw new IllegalArgumentException("The sequence " + getSequence() + " contains an illegal base (minor group).");
                }
                builder.append(MINOR_GROUP[minorGroupIndex]);
            }
        }

        if (sequence.contains("U")) {
            builder.append("s");
        }

        // Turn this into a proper noun by capitalizing the first letter
        String name = builder.toString();
        return name.substring(0, 1).toUpperCase() + name.substring(1);
    }

    /**
     * Returns the sequence of bases that form this molecular index. Guaranteed to never be null
     * or have a length of zero.
     */
//    @Column(name = "sequence", nullable = false)
    public String getSequence() {
        return sequence;
    }

    /*
      * @see #getSequence()
      *
      * The provided value must never be null or empty, and must not contain both
      * Ts and Us.
      */
    @SuppressWarnings("unused")
    private void setSequence(String sequence) {
        Pair<Boolean, String> pair = validatedUpperCaseSequence(sequence);
        if (pair.getLeft()) {
            this.sequence = pair.getRight();
        } else {
            throw new IllegalArgumentException(pair.getRight());
        }
    }

    /** Returns validity status and either upper cased sequence or the error message. */
    public static Pair<Boolean, String> validatedUpperCaseSequence(String sequence) {
        if (StringUtils.isBlank(sequence)) {
            return Pair.of(false, "The sequence must have a length greater than zero.");
        }
        String upperSequence = sequence.trim().toUpperCase();
        if (upperSequence.contains("U") && upperSequence.contains("T")) {
            return Pair.of(false, "Cannot have both T and U in the same sequence.");
        }

        for (char base : upperSequence.toCharArray()) {
            if ( ! BASE_INDEXES.containsKey(base)) {
                return Pair.of(false, "The sequence must only have the letters A, C, T, G, or U, not " + base);
            }
        }
        return Pair.of(true, upperSequence);
    }

    /**
     * The sequence of a MolecularIndex determines whether one instance is identical
     * to another.
     */
    @Override
    public boolean equals(Object other) {
        if (this == other) {
            return true;
        }
        if ( ! (other instanceof MolecularIndex)) {
            return false;
        }

        return ((MolecularIndex) other).getSequence().equals(getSequence());
    }

    @Override
    public int hashCode() {
        return getSequence().hashCode();
    }

    public Set<MolecularIndexingScheme> getMolecularIndexingSchemes() {
        return molecularIndexingSchemes;
    }
}

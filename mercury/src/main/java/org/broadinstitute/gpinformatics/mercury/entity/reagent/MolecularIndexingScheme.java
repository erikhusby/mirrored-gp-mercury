package org.broadinstitute.gpinformatics.mercury.entity.reagent;

import org.broadinstitute.gpinformatics.mercury.control.reagent.MolecularIndexingSchemeFactory;
import org.hibernate.annotations.Sort;
import org.hibernate.annotations.SortType;
import org.hibernate.envers.Audited;

import javax.persistence.*;
import java.util.Collections;
import java.util.Map;
import java.util.SortedMap;
import java.util.TreeMap;

/**
 * A collection of {@link MolecularIndex}es, with their positions.
 *
 * Positions are technology-specific: the names used by Illumina, for instance,
 * are not the same (conceptually, physically, or any other way you might see
 * it) as the ones used by 454. This class will make sure that all the component
 * {@link MolecularIndex}es you might add belong to the same technology. When
 * specifying schemes to the {@link MolecularIndexingSchemeFactory}, make sure
 * that all the {@link PositionHint}s are from the same enum and you'll be fine.
 *
 * Do not call the constructor, {@link #addIndexPosition(PositionHint, MolecularIndex)}
 * or {@link #setIndexPositions(java.util.SortedMap)}. Instead, create new schemes and add
 * indexes to existing ones by calling the factory methods on
 * {@link MolecularIndexingSchemeFactory}.
 */
@Entity
@Audited
@Table(schema = "mercury", uniqueConstraints = @UniqueConstraint(name = "uk_mis_name", columnNames = {"name"}))
@NamedNativeQueries({
        @NamedNativeQuery(
                name="MolecularIndexingScheme.findSingleIndexScheme",
                query="SELECT mis.molecular_indexing_scheme_id, mis.name " +
                        "FROM molecular_indexing_scheme mis, molecular_index mi, molecular_index_position mip1, " +
                        "(SELECT scheme_id FROM molecular_index_position GROUP BY scheme_id HAVING count(*) = 1) scheme_count " +
                        "WHERE mi.sequence = :indexSequence " +
                        "  AND mip1.mapkey = :indexPosition " +
                        "  AND mi.molecular_index_id = mip1.index_id " +
                        "  AND mip1.scheme_id = scheme_count.scheme_id " +
                        "  AND mip1.scheme_id = mis.molecular_indexing_scheme_id",
                resultClass=MolecularIndexingScheme.class
        ),
        @NamedNativeQuery(
                name="MolecularIndexingScheme.findDualIndexScheme",
                query="SELECT mis.molecular_indexing_scheme_id, mis.name " +
                        "FROM molecular_indexing_scheme mis, " +
                        "molecular_index mi1, molecular_index mi2, " +
                        "molecular_index_position mip1, molecular_index_position mip2, " +
                        "(SELECT scheme_id FROM molecular_index_position GROUP BY scheme_id HAVING count(*) = 2) scheme_count " +
                        "WHERE mi1.sequence = :indexSequence1 " +
                        "  AND mip1.mapkey = :indexPosition1 " +
                        "  AND mi2.sequence = :indexSequence2 " +
                        "  AND mip2.mapkey = :indexPosition2 " +
                        "  AND mi1.molecular_index_id = mip1.index_id " +
                        "  AND mi2.molecular_index_id = mip2.index_id " +
                        "  AND mip1.scheme_id = scheme_count.scheme_id " +
                        "  AND mip1.scheme_id = mip2.scheme_id " +
                        "  AND mip1.scheme_id = mis.molecular_indexing_scheme_id",
                resultClass=MolecularIndexingScheme.class
        ),
        @NamedNativeQuery(
                name="MolecularIndexingScheme.findTripleIndexScheme",
                query="SELECT mis.molecular_indexing_scheme_id, mis.name " +
                        "FROM molecular_indexing_scheme mis, " +
                        "molecular_index mi1, molecular_index mi2, molecular_index mi3, " +
                        "molecular_index_position mip1, molecular_index_position mip2, molecular_index_position mip3, " +
                        "(SELECT scheme_id FROM molecular_index_position GROUP BY scheme_id HAVING count(*) = 3) scheme_count " +
                        "WHERE mi1.sequence = :indexSequence1 " +
                        "  AND mip1.mapkey = :indexPosition1 " +
                        "  AND mi2.sequence = :indexSequence2 " +
                        "  AND mip2.mapkey = :indexPosition2 " +
                        "  AND mi3.sequence = :indexSequence3 " +
                        "  AND mip3.mapkey = :indexPosition3 " +
                        "  AND mi1.molecular_index_id = mip1.index_id " +
                        "  AND mi2.molecular_index_id = mip2.index_id " +
                        "  AND mi3.molecular_index_id = mip3.index_id " +
                        "  AND mip1.scheme_id = scheme_count.scheme_id " +
                        "  AND mip1.scheme_id = mip2.scheme_id " +
                        "  AND mip2.scheme_id = mip3.scheme_id " +
                        "  AND mip1.scheme_id = mis.molecular_indexing_scheme_id",
                resultClass=MolecularIndexingScheme.class
        ),
        @NamedNativeQuery(
                name="MolecularIndexingScheme.findAllIlluminaSchemes",
                query = "SELECT mis.molecular_indexing_scheme_id, mis.name " +
                        "FROM molecular_indexing_scheme mis, molecular_index mi, molecular_index_position mip1, " +
                        "(SELECT scheme_id FROM molecular_index_position GROUP BY scheme_id HAVING count(*) = 1) scheme_count " +
                        "WHERE mip1.mapkey like 'ILLUMINA_%'" +
                        "  AND mi.molecular_index_id = mip1.index_id " +
                        "  AND mip1.scheme_id = scheme_count.scheme_id " +
                        "  AND mip1.scheme_id = mis.molecular_indexing_scheme_id",
                resultClass=MolecularIndexingScheme.class
        )
})
@NamedQueries({
        @NamedQuery(
                name="MolecularIndexingScheme.findByName",
                query="SELECT mis FROM MolecularIndexingScheme mis WHERE mis.name = :name"
        )
})
public class MolecularIndexingScheme {

    /*
      * Implementation note: The position scheme implemented here has two sides.
      * One faces the database (IndexPosition) and is not technology specific; the
      * other faces the outside world and IS technology specific. Dividing the
      * world up like this allows us to create a more sane and less error-prone
      * API.
      *
      * Also, the order in which the PositionHint enums add their values is
      * important, since it partially determines the name of the schemes generated
      * with those positions. It's legal to add new enum values before, after or
      * between the existing ones; it is illegal, however, to change their relative
      * ordering.
      */

    public interface PositionHint {

        /**
         * Returns the name of the technology appropriate for this position.
         */
        public String getTechnology();

        /**
         * Returns the internal name for the position. This value is generally
         * not usable beyond the MolecularIndexingScheme code.
         */
        public IndexPosition getIndexPosition();

        /**
         * If the implementing class is an enum, returns a String identical to a
         * call to PositionHint.name(). Undefined for other implementing types.
         */
        // The ugly part: enums, when they implement an interface, don't expose
        // Enum.name() when all you have is a reference to the interface because
        // what's implementing it might not be an enum. This hack ensures that
        // we can get at that value anyway.
        public String getName();

        /**
         * Returns true if this PositionHint value should be associated with a
         * component index when there is only one index in the MolecularIndexingScheme.
         */
        public boolean isDefault();
    }

    /**
     * {@link PositionHint}s associated with Illumina sequencing constructs.
     */
/*
    public enum IlluminaPositionHint implements PositionHint {
        P3,
        P5,
        P7,
        IS1,
        IS2,
        IS3,
        IS4,
        IS5,
        IS6;

        @Override
        public boolean isDefault() {
            return this == P7;
        }

        @Override
        public IndexPosition getIndexPosition() {
            // NB, UPDATE THIS SWITCH AND IndexPosition whenever enum values are added or deleted.
            switch (this) {
                case P3 : return IndexPosition.ILLUMINA_P3;
                case P5 : return IndexPosition.ILLUMINA_P5;
                case P7 : return IndexPosition.ILLUMINA_P7;
			case IS1 : return IndexPosition.ILLUMINA_IS1;
			case IS2 : return IndexPosition.ILLUMINA_IS2;
			case IS3 : return IndexPosition.ILLUMINA_IS3;
			case IS4 : return IndexPosition.ILLUMINA_IS4;
			case IS5 : return IndexPosition.ILLUMINA_IS5;
			case IS6 : return IndexPosition.ILLUMINA_IS6;
                default : throw new IllegalArgumentException("The IndexPosition for IlluminaPositionHint " + this + " is undefined.");
            }
        }

        @Override
        public String getTechnology() {
            return "Illumina";
        }

        @Override
        public String getName() {
            return this.name();
        }
    }

    */
/**
     * {@link PositionHint}s associated with 454 sequencing constructs.
     *//*

    public enum Four54PositionHint implements PositionHint {
        A,
        B;

        @Override
        public boolean isDefault() {
            return this == A;
        }

        @Override
        public IndexPosition getIndexPosition() {
            // NB, UPDATE THIS SWITCH AND IndexPosition whenever enum values are added or deleted.
            switch (this) {
                case A : return IndexPosition.FOUR54_A;
                case B : return IndexPosition.FOUR54_B;
                default : throw new IllegalArgumentException("The IndexPosition for Four54PositionHint " + this + " is undefined.");
            }
        }

        @Override
        public String getTechnology() {
            return "454";
        }

        @Override
        public String getName() {
            return this.name();
        }
    }

    */
/**
     * {@link PositionHint}s associated with 454 sequencing constructs.
     *//*

    public enum IonPositionHint implements PositionHint {
        A,
        B;

        @Override
        public boolean isDefault() {
            return this == A;
        }

        @Override
        public IndexPosition getIndexPosition() {
            // NB, UPDATE THIS SWITCH AND IndexPosition whenever enum values are added or deleted.
            switch (this) {
                case A : return IndexPosition.ION_A;
                case B : return IndexPosition.ION_B;
                default : throw new IllegalArgumentException("The IndexPosition for Ion " + this + " is undefined.");
            }
        }

        @Override
        public String getTechnology() {
            return "Ion";
        }

        @Override
        public String getName() {
            return this.name();
        }
    }

    public enum GssrPositionHint implements PositionHint {
        INTRA;

        @Override
        public boolean isDefault() {
            return this == INTRA;
        }

        @Override
        public IndexPosition getIndexPosition() {
            // NB, UPDATE THIS SWITCH AND IndexPosition whenever enum values are added or deleted.
            switch (this) {
                case INTRA : return IndexPosition.GSSR_INTRA;
                default : throw new IllegalArgumentException("The IndexPosition for GssrPositionHint " + this + " is undefined.");
            }
        }

        @Override
        public String getTechnology() {
            return "GSSR";
        }

        @Override
        public String getName() {
            return this.name();
        }
    }

    public enum IdentifierPositionHint implements PositionHint {
        ONLY;

        @Override
        public boolean isDefault() {
            return this == ONLY;
        }

        @Override
        public IndexPosition getIndexPosition() {
            // NB, UPDATE THIS SWITCH AND IndexPosition whenever enum values are added or deleted.
            switch (this) {
                case ONLY : return IndexPosition.IDENTIFIER_ONLY;
                default : throw new IllegalArgumentException("The IndexPosition for IdentifierPositionHint " + this + " is undefined.");
            }
        }

        @Override
        public String getTechnology() {
            return "Identifier";
        }

        @Override
        public String getName() {
            return this.name();
        }
    }
*/

    /**
     * All the positions across all the technologies. These are logically
     * duplicates of the values in the technology-specific enums, but are
     * database-facing and so have different literal values. Because they
     * are database-facing, they're not usable outside of
     * MolecularIndexingScheme. Instead, use a PositionHint implementation.
     */
    public enum IndexPosition implements PositionHint {
        FOUR54_A(/*Four54PositionHint.A, */"454", true, "A"),
        FOUR54_B(/*Four54PositionHint.B, */"454", false, "B"),
        ION_A(/*IonPositionHint.A, */"Ion", true, "A"),
        ION_B(/*IonPositionHint.B, */"Ion", false, "B"),
        GSSR_INTRA(/*GssrPositionHint.INTRA, */"GSSR", true, "INTRA"),
        IDENTIFIER_ONLY(/*IdentifierPositionHint.ONLY, */"Identifier", true, "ONLY"),
        ILLUMINA_P3(/*IlluminaPositionHint.P3, */"Illumina", false, "P3"),
        ILLUMINA_P5(/*IlluminaPositionHint.P5, */"Illumina", false, "P5"),
        ILLUMINA_P7(/*IlluminaPositionHint.P7, */"Illumina", true, "P7"),
        ILLUMINA_IS1(/*IlluminaPositionHint.IS1, */"Illumina", false, "IS1"),
        ILLUMINA_IS2(/*IlluminaPositionHint.IS2, */"Illumina", false, "IS2"),
        ILLUMINA_IS3(/*IlluminaPositionHint.IS3, */"Illumina", false, "IS3"),
        ILLUMINA_IS4(/*IlluminaPositionHint.IS4, */"Illumina", false, "IS4"),
        ILLUMINA_IS5(/*IlluminaPositionHint.IS5, */"Illumina", false, "IS5"),
        ILLUMINA_IS6(/*IlluminaPositionHint.IS6, */"Illumina", false, "IS6");

        /*
           * IMPORTANT NOTE:
           * Update the values above whenever a value in any of the PositionHint
           * enums is added. You must also update a switch statement in that
           * PositionHint enum. Failing to do so means the application is broken.
           * (This was built this way because you can't refer to one enum from
           * from the c'tor of another.)
           *
           * DO NOT REMOVE, CHANGE OR REORDER ENUM VALUES. Or, rather, if you
           * decide you absolutely must change or remove them, you have to update
           * all the values in the database as well, including the names of the
           * index schemes. See the notes at generateName() for more information.
           */

//        private final PositionHint positionHint;
        private String technology;
        private boolean isDefault;
        private String name;

        private IndexPosition(/*final PositionHint position, */String technology, boolean aDefault, String name) {
//            this.positionHint = position;
            this.technology = technology;
            isDefault = aDefault;
            this.name = name;
        }

/*
        private PositionHint getPositionHint() {
            return this.positionHint;
        }
*/

        @Override
        public String getTechnology() {
            return technology;
        }

        @Override
        public IndexPosition getIndexPosition() {
            return this;
        }

        @Override
        public String getName() {
            return name;
        }

        @Override
        public boolean isDefault() {
            return isDefault;
        }
    }

    private Long molecularIndexingSchemeId;

    private String name;

    private final SortedMap<PositionHint, MolecularIndex> indexes = new TreeMap<PositionHint, MolecularIndex>();

    /**
     * Returns the PositionHint that should be associated with an index when there
     * is only one index in the MolecularIndexingScheme. Guaranteed to never be
     * null.
     */
    public static PositionHint getDefaultPositionHint(final String technology) {
        if (technology == null) {
            throw new NullPointerException("The technology specified cannot be null.");
        }

        if (technology.isEmpty()) {
            throw new IllegalArgumentException("Cannot specify an empty technology string.");
        }

        for (final IndexPosition internalPosition : IndexPosition.values()) {
//            final PositionHint externalPosition = internalPosition.getPositionHint();
            if (internalPosition.getTechnology().equals(technology) && internalPosition.isDefault()) {
                return internalPosition;
            }
        }

        throw new IllegalArgumentException(
                "Technology " + technology + " doesn't have any PositionHints.");
    }

    /**
     * No-args constructor for Hibernate's sake. Developers should use
     * {@link MolecularIndexingSchemeFactory} to create scheme instances, not the
     * constructors in this class.
     */
    protected MolecularIndexingScheme() { }

    /**
     * Creates a new MolecularIndexingScheme with the given position/indexes.
     * Throws IllegalArgumentException if the positions do not all belong to the
     * same technology, or if the list of pairs is empty. Developers should use
     * {@link MolecularIndexingSchemeFactory} to create scheme instances, not the
     * constructors in this class.
     */
    // todo jmt switch this back to package visible
    public MolecularIndexingScheme(final Map<PositionHint, MolecularIndex> positionIndexMap) {
        if (positionIndexMap == null) {
            throw new NullPointerException("The list of index pairs can't be null.");
        }

        if (positionIndexMap.isEmpty()) {
            throw new IllegalArgumentException("The list of index pairs didn't contain any elements");
        }

        for (final PositionHint position : positionIndexMap.keySet()) {
            this.addIndexPosition(position, positionIndexMap.get(position));
        }

        this.setName(this.generateName());
    }

    @Id
    @SequenceGenerator(name="seq_molecular_indexing_scheme", schema = "mercury", sequenceName="seq_molecular_indexing_scheme")
    @GeneratedValue(strategy= GenerationType.SEQUENCE, generator="seq_molecular_indexing_scheme")
    @Column(name = "molecular_indexing_scheme_id")
    @SuppressWarnings("unused")
    private Long getMolecularIndexingSchemeId() {
        return molecularIndexingSchemeId;
    }

    @SuppressWarnings("unused")
    private void setMolecularIndexingSchemeId(final Long molecularIndexingSchemeId) {
        this.molecularIndexingSchemeId = molecularIndexingSchemeId;
    }

    /**
     * Returns the indexes that compose this scheme, with their positions. The
     * positions returned are those appropriate for storing the database, which means
     * they're not generally usable outside of this class. Same goes for this method.
     */
    @ManyToMany(cascade = {CascadeType.PERSIST}, fetch= FetchType.LAZY)
    @JoinTable(
            schema = "mercury",
            name="MOLECULAR_INDEX_POSITION",
            joinColumns=@JoinColumn(name="scheme_id", referencedColumnName = "molecular_indexing_scheme_id"),
            inverseJoinColumns=@JoinColumn(name="index_id", referencedColumnName = "molecular_index_id")
    )
    @MapKeyJoinColumn(table="MOLECULAR_INDEX_POSITION", name="scheme_id")
    @MapKeyEnumerated(EnumType.STRING)
    // todo jmt hbm2ddl seems to ignore this, it always uses mapkey
//    @MapKeyColumn(name="POSITION_HINT")
    @MapKeyColumn(name="mapkey")
    @Sort(type= SortType.NATURAL)
    // todo jmt this set manipulation seems to confuse Hibernate, it deletes and re-inserts the collection repeatedly
    SortedMap<IndexPosition, MolecularIndex> getIndexPositions() {
        final SortedMap<IndexPosition, MolecularIndex> indexMap = new TreeMap<IndexPosition, MolecularIndex>();
        for (final Map.Entry<PositionHint, MolecularIndex> entry : this.indexes.entrySet()) {
            // Dereferencing the PositionHint into its IndexPosition
            // since these are the values stored via Hibernate. The
            // corresponding setter must do the inverse operation.
            indexMap.put(entry.getKey().getIndexPosition(), entry.getValue());
        }
        return indexMap;
    }

/*
    void setIndexPositions(final SortedMap<IndexPosition, MolecularIndex> indexesAndPositions) {
        // Dereferencing the IndexPosition into its PositionHint since
        // those values are the ones useful in this code. The corresponding
        // getter must do the inverse operation.
        for (final Map.Entry<IndexPosition, MolecularIndex> entry : indexesAndPositions.entrySet()) {
            this.addIndexPosition(
                    entry.getKey().getPositionHint(),
                    entry.getValue());
        }
    }
*/

    /**
     * Returns a "pretty" name for this scheme. The name is based on the sequence
     * of the component {@link MolecularIndex}es. Two schemes, both containing the
     * same {@link MolecularIndex}es but with different positions, will have
     * different names; the names will be identical if the component indexes are
     * identical and in identical positions. Guaranteed to never be null or to be
     * non-empty.
     */
    @Column(name="name")
    public String getName() {
        return this.name;
    }

    @SuppressWarnings("unused")
    protected void setName(final String name) {
        this.name = name;
    }

    /*
      * Appends the given MolecularIndex to the collection associated with this
      * scheme. This code enforces the rules surrounding the indexes' positions
      * specified in the class documentation. This method is not useful outside
      * of the MolecularIndexingScheme. If you need a scheme with additional
      * component indexes, use the factory to get or create one.
      */
    private void addIndexPosition(final PositionHint position, final MolecularIndex index) {
        if (index == null) throw new NullPointerException("The given MolecularIndex cannot be null.");

        for (final PositionHint existingHint : this.getIndexes().keySet()) {
            if ( ! existingHint.getClass().equals(position.getClass())) {
                throw new IllegalArgumentException(
                        "Cannot add positions from two different " +
                                "technologies to the same scheme.");
            }
        }

        // Check the type THEN the contains, otherwise .containsKey()
        // throws a ClassCastException. Also, apparently Hibernate
        // calls setters while persisting, which means that just
        // testing that the position is occupied will always throw
        if (this.indexes.containsKey(position) && ! this.indexes.get(position).equals(index)) {
            throw new IllegalArgumentException(
                    "The scheme already contains a position " + position.toString() +
                            " with a different index " + this.indexes.get(position).getSequence());
        }

        this.indexes.put(position, index);
    }

    /**
     * Returns an unmodifiable {@link SortedMap} of the {@link MolecularIndex}es
     * that compose this molecular indexing scheme, keyed by their position.
     * Neither the returned map nor any values within it will be null.
     */
    @Transient
    public SortedMap<PositionHint, MolecularIndex> getIndexes() {
        return Collections.unmodifiableSortedMap(this.indexes);
    }

    /**
     * Returns the MolecularIndex at the given position. Returns null if no index
     * with the given position exists.
     */
    @Transient
    public MolecularIndex getIndex(final PositionHint position) {
        return this.getIndexes().get(position);
    }

    /**
     * Returns the number of component indexes in this scheme. Guaranteed to be
     * zero or greater.
     */
    @Transient
    public int getIndexCount() {
        return this.getIndexes().size();
	}

    /**
     * Identity is based on the name of the scheme: each name is unique for schemes
     * with different component indexes or component indexes in different positions,
     * and so should suffice for .equals().
     */
    @Override
    public boolean equals(final Object other) {
        if (other == this) return true;
        if ( ! (other instanceof MolecularIndexingScheme)) return false;

        final MolecularIndexingScheme otherScheme = (MolecularIndexingScheme) other;

        return this.getName().equals(otherScheme.getName());
    }

    @Override
    public int hashCode() {
        return this.getName().hashCode();
    }

    /*
      * Creates/returns a String identifier (a "name") for this scheme based on the
      * sequences of the component indexes and the technology used. Outside code that
      * wants to use this string should use {@link #getName()}.
      *
      * **THIS IMPLEMENTATION MUST NOT CHANGE** because finding MolecularIndexingSchemes
      * in the database relies on the strings produced here.
      *
      * Note also that, early on, it was decided that all single-index schemes should
      * adopt the "old" name of the single component molecular index, back when indexes
      * were something called "SeqSampleIdentifiers". This avoided the need to change a
      * large amount of data in analysis pipelines. These old names are simply the name
      * of the index, and so don't indicate the technology used or the index's position.
      * For Illumina, all single index positions are at ILLUMINA_P7; for 454, they're
      * all 454_INTRA.
      */
    private String generateName() {
        final StringBuilder builder = new StringBuilder();
        builder.append(this.getIndexes().firstKey().getTechnology());
        for (final Map.Entry<PositionHint, MolecularIndex> entry : this.getIndexes().entrySet()) {
            builder.append('_')
                    .append(entry.getKey())
                    .append("-")
                    .append(entry.getValue().generateNameFromSequence());
        }

        return builder.toString();
    }
}

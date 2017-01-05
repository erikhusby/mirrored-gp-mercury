package org.broadinstitute.gpinformatics.mercury.entity.reagent;

import org.broadinstitute.gpinformatics.mercury.control.reagent.MolecularIndexingSchemeFactory;
import org.hibernate.annotations.BatchSize;
import org.hibernate.annotations.Sort;
import org.hibernate.annotations.SortType;
import org.hibernate.envers.Audited;

import javax.persistence.CascadeType;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.FetchType;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.JoinTable;
import javax.persistence.ManyToMany;
import javax.persistence.MapKeyColumn;
import javax.persistence.MapKeyEnumerated;
import javax.persistence.MapKeyJoinColumn;
import javax.persistence.NamedNativeQueries;
import javax.persistence.NamedNativeQuery;
import javax.persistence.NamedQueries;
import javax.persistence.NamedQuery;
import javax.persistence.SequenceGenerator;
import javax.persistence.Table;
import javax.persistence.Transient;
import javax.persistence.UniqueConstraint;
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
 * {@link MolecularIndex}es you might add belong to the same technology.
 *
 * Do not call the constructor or {@link #addIndexPosition(IndexPosition, MolecularIndex)}.
 * Instead, create new schemes and add indexes to existing ones by calling the factory methods on
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

    public static final String TECHNOLOGY_454 = "454";
    public static final String TECHNOLOGY_ION = "Ion";
    public static final String TECHNOLOGY_GSSR = "GSSR";
    public static final String TECHNOLOGY_IDENTIFIER = "Identifier";
    public static final String TECHNOLOGY_ILLUMINA = "Illumina";

    /**
     * All the positions across all the technologies.
     * The order in which the PositionHint enums add their values is
     * important, since it partially determines the name of the schemes generated
     * with those positions. It's legal to add new enum values before, after or
     * between the existing ones; it is illegal, however, to change their relative
     * ordering.
     */
    public enum IndexPosition {
        FOUR54_A(TECHNOLOGY_454, true, "A"),
        FOUR54_B(TECHNOLOGY_454, false, "B"),
        ION_A(TECHNOLOGY_ION, true, "A"),
        ION_B(TECHNOLOGY_ION, false, "B"),
        GSSR_INTRA(TECHNOLOGY_GSSR, true, "INTRA"),
        IDENTIFIER_ONLY(TECHNOLOGY_IDENTIFIER, true, "ONLY"),
        ILLUMINA_P3(TECHNOLOGY_ILLUMINA, false, "P3"),
        ILLUMINA_P5(TECHNOLOGY_ILLUMINA, false, "P5"),
        ILLUMINA_P7(TECHNOLOGY_ILLUMINA, true, "P7"),
        ILLUMINA_IS1(TECHNOLOGY_ILLUMINA, false, "IS1"),
        ILLUMINA_IS2(TECHNOLOGY_ILLUMINA, false, "IS2"),
        ILLUMINA_IS3(TECHNOLOGY_ILLUMINA, false, "IS3"),
        ILLUMINA_IS4(TECHNOLOGY_ILLUMINA, false, "IS4"),
        ILLUMINA_IS5(TECHNOLOGY_ILLUMINA, false, "IS5"),
        ILLUMINA_IS6(TECHNOLOGY_ILLUMINA, false, "IS6");

        /*
           * DO NOT REMOVE, CHANGE OR REORDER ENUM VALUES. Or, rather, if you
           * decide you absolutely must change or remove them, you have to update
           * all the values in the database as well, including the names of the
           * index schemes. See the notes at generateName() for more information.
           */

        private String technology;
        private boolean isDefault;
        private String position;

        IndexPosition(String technology, boolean aDefault, String position) {
            this.technology = technology;
            isDefault = aDefault;
            this.position = position;
        }

        /**
         * Returns the name of the technology appropriate for this position.
         */
        public String getTechnology() {
            return technology;
        }

        /**
         * Returns the internal name for the position. This value is generally
         * not usable beyond the MolecularIndexingScheme code.
         */
        public IndexPosition getIndexPosition() {
            return this;
        }

        public String getPosition() {
            return position;
        }

        /**
         * Returns true if this PositionHint value should be associated with a
         * component index when there is only one index in the MolecularIndexingScheme.
         */
        public boolean isDefault() {
            return isDefault;
        }
    }

    @SuppressWarnings("UnusedDeclaration")
    @Id
    @SequenceGenerator(name="seq_molecular_indexing_scheme", schema = "mercury", sequenceName="seq_molecular_indexing_scheme")
    @GeneratedValue(strategy= GenerationType.SEQUENCE, generator="seq_molecular_indexing_scheme")
    @Column(name = "molecular_indexing_scheme_id")
    private Long molecularIndexingSchemeId;

    @Column(name="name")
    private String name;

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
    @BatchSize(size = 500)
    private SortedMap<IndexPosition, MolecularIndex> indexes = new TreeMap<>();

    /**
     * Returns the PositionHint that should be associated with an index when there
     * is only one index in the MolecularIndexingScheme. Guaranteed to never be
     * null.
     */
    public static IndexPosition getDefaultPositionHint(String technology) {
        if (technology == null) {
            throw new NullPointerException("The technology specified cannot be null.");
        }

        if (technology.isEmpty()) {
            throw new IllegalArgumentException("Cannot specify an empty technology string.");
        }

        for (IndexPosition internalPosition : IndexPosition.values()) {
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
    public MolecularIndexingScheme() { }

    /**
     * Creates a new MolecularIndexingScheme with the given position/indexes.
     * Throws IllegalArgumentException if the positions do not all belong to the
     * same technology, or if the list of pairs is empty. Developers should use
     * {@link MolecularIndexingSchemeFactory} to create scheme instances, not the
     * constructors in this class.
     */
    // todo jmt switch this back to package visible
    public MolecularIndexingScheme(Map<IndexPosition, MolecularIndex> positionIndexMap) {
        if (positionIndexMap == null) {
            throw new NullPointerException("The list of index pairs can't be null.");
        }

        if (positionIndexMap.isEmpty()) {
            throw new IllegalArgumentException("The list of index pairs didn't contain any elements");
        }

        for (Map.Entry<IndexPosition, MolecularIndex> indexPositionMolecularIndexEntry : positionIndexMap.entrySet()) {
            addIndexPosition(indexPositionMolecularIndexEntry.getKey(), indexPositionMolecularIndexEntry.getValue());
        }

        setName(generateName());
        for (MolecularIndex molecularIndex : positionIndexMap.values()) {
            molecularIndex.getMolecularIndexingSchemes().add(this);
        }
    }

    /**
     * Returns a "pretty" name for this scheme. The name is based on the sequence
     * of the component {@link MolecularIndex}es. Two schemes, both containing the
     * same {@link MolecularIndex}es but with different positions, will have
     * different names; the names will be identical if the component indexes are
     * identical and in identical positions. Guaranteed to never be null or to be
     * non-empty.
     */
    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    /*
      * Appends the given MolecularIndex to the collection associated with this
      * scheme. This code enforces the rules surrounding the indexes' positions
      * specified in the class documentation. This method is not useful outside
      * of the MolecularIndexingScheme. If you need a scheme with additional
      * component indexes, use the factory to get or create one.
      */
    public void addIndexPosition(IndexPosition position, MolecularIndex index) {
        if (index == null) {
            throw new NullPointerException("The given MolecularIndex cannot be null.");
        }

        for (IndexPosition existingHint : getIndexes().keySet()) {
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
        if (indexes.containsKey(position) && !indexes.get(position).equals(index)) {
            throw new IllegalArgumentException(
                    "The scheme already contains a position " + position.toString() +
                            " with a different index " + indexes.get(position).getSequence());
        }

        indexes.put(position, index);
    }

    /**
     * Returns an unmodifiable {@link SortedMap} of the {@link MolecularIndex}es
     * that compose this molecular indexing scheme, keyed by their position.
     * Neither the returned map nor any values within it will be null.
     */
    public SortedMap<IndexPosition, MolecularIndex> getIndexes() {
        return Collections.unmodifiableSortedMap(indexes);
    }

    /**
     * Returns the MolecularIndex at the given position. Returns null if no index
     * with the given position exists.
     */
    @Transient
    public MolecularIndex getIndex(IndexPosition position) {
        return getIndexes().get(position);
    }

    /**
     * Returns the number of component indexes in this scheme. Guaranteed to be
     * zero or greater.
     */
    @Transient
    public int getIndexCount() {
        return getIndexes().size();
	}

    /**
     * Identity is based on the name of the scheme: each name is unique for schemes
     * with different component indexes or component indexes in different positions,
     * and so should suffice for .equals().
     */
    @Override
    public boolean equals(Object other) {
        if (other == this) {
            return true;
        }
        if ( ! (other instanceof MolecularIndexingScheme)) {
            return false;
        }

        MolecularIndexingScheme otherScheme = (MolecularIndexingScheme) other;

        return getName().equals(otherScheme.getName());
    }

    @Override
    public int hashCode() {
        return getName().hashCode();
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
        StringBuilder builder = new StringBuilder();
        builder.append(getIndexes().firstKey().getTechnology());
        for (Map.Entry<IndexPosition, MolecularIndex> entry : getIndexes().entrySet()) {
            builder.append('_')
                    .append(entry.getKey().getPosition())
                    .append("-")
                    .append(entry.getValue().generateNameFromSequence());
        }

        return builder.toString();
    }

}

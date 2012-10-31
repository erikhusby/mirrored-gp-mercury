package org.broadinstitute.gpinformatics.athena.entity.products;


import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;
import org.hibernate.envers.Audited;

import javax.persistence.*;
import java.io.Serializable;


/**
 * Core entity for ProductFamilies.
 *
 * Making this a class and not an enum for the same reason we don't have concrete Products as subclasses
 * of an abstract AbstractProduct class: we want the ability to define these without changing code.
 * ProductFamily is by nature an enummy thing (in its current state it's nothing more than a controlled vocabulary name)
 * and does seem to beg for a nice way of being able to summon up persistent or detached instances of well known
 * ProductFamilies.  This is going to be a general problem in Mercury/Athena in need of a general solution.
 *
 * It's also possible that ProductFamilies turn out to be fairly static and an enum would suffice.
 *
 * @author mcovarr
 *
 */
@Entity
@Audited
@Table(schema = "athena", uniqueConstraints = @UniqueConstraint(columnNames = {"name"}))
public class ProductFamily implements Serializable, Comparable<ProductFamily> {

    /**
     * Known product families, a DAO method might accept one of these to return a persistent or detached instance
     * of one of these ProductFamilies if there was business logic that wanted to call out a specific ProductFamily.
     *
     * TODO get rid of this enum
     */

    public enum ProductFamilyName {
//        GENERAL_PRODUCTS("General Products"),
//        EXOME_SEQUENCING_ANALYSIS("Exome Sequencing Analysis"),
//        WHOLE_GENOME_SEQUENCING_ANALYSIS("Whole Genome Sequencing Analysis"),
//        WHOLE_GENOME_ARRAY_ANALYSIS("Whole Genome Array Analysis"),
//        RNA_ANALYSIS("RNA Analysis"),
//        ASSEMBLY_ANALYSIS("Assembly Analysis"),
//        METAGENOMIC_ANALYSIS("Metagenomic Analysis"),
//        EPIGENOMIC_ANALYSIS("Epigenomic Analysis"),
//        ILLUMINA_SEQUENCING_ONLY("Illumina Sequencing Only"),
//        ALTERNATIVE_TECHNOLOGIES("Alternative Technologies"),
//        CUSTOM_PRODUCTS_TARGETED_SEQUENCING("Targeted Sequencing"),

        // GPLIM-172 GPLIM-216 real product families
        DE_NOVO_ASSEMBLY("de novo Assembly"),
        ALTERNATIVE_LIBRARY_PREP_AND_DEVELOPMENT("Alternate Library Prep & Development"),
        DATA_ANALYSIS("Data Analysis"),
        EPIGENOMICS("Epigenomics"),
        EXOME("Exome"),
        METAGENOMICS("Metagenomics"),
        MICROBIAL_AND_VIRAL_ANALYSIS("Microbial & Viral Analysis"),
        RNA("RNA"),
        SAMPLE_INITIATION_QUALIFICATION_AND_CELL_CULTURE("Sample Initiation, Qualification & Cell Culture"),
        SEQUENCE_ONLY("Sequence Only"),
        SMALL_DESIGN_VALIDATION_AND_EXTENSION("Small Design, Validation & Extension"),
        WHOLE_GENOME("Whole Genome");



        private final String displayName;

        ProductFamilyName(String displayName) {
            this.displayName = displayName;
        }

        public String getDisplayName() {
            return displayName;
        }
    }


    @Id
    @SequenceGenerator(name = "SEQ_PRODUCT_FAMILY", schema = "athena", sequenceName = "SEQ_PRODUCT_FAMILY")
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "SEQ_PRODUCT_FAMILY")
    private Long productFamilyId;

    private String name;


    /**
     * JPA package visible constructor
     * @return
     */
    ProductFamily() {
    }


    public ProductFamily(String name) {

        if ( name == null )
            throw new NullPointerException( "Null name!" );

        this.name = name;
    }


    public Long getProductFamilyId() {
        return productFamilyId;
    }

    public String getName() {
        return name;
    }


    @Transient
    public String getDisplayName() {
        return ProductFamilyName.valueOf(getName()).getDisplayName();
    }

    @Override
    public int compareTo(ProductFamily productFamily) {
        return getName().compareTo(productFamily.getName());
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;

        if (!(o instanceof ProductFamily)) return false;

        ProductFamily that = (ProductFamily) o;

        return new EqualsBuilder().append(getName(), that.getName()).isEquals();

    }

    @Override
    public int hashCode() {
        return new HashCodeBuilder().append(getName()).toHashCode();
    }
}

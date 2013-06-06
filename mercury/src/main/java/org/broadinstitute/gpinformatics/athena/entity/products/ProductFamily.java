package org.broadinstitute.gpinformatics.athena.entity.products;


import org.apache.commons.lang3.builder.CompareToBuilder;
import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;
import org.hibernate.envers.Audited;

import javax.annotation.Nonnull;
import javax.persistence.*;
import java.io.Serializable;


/**
 * Core entity for ProductFamilies.
 */
@Entity
@Audited
@Table(schema = "athena", uniqueConstraints = @UniqueConstraint(columnNames = "name"))
public class ProductFamily implements Serializable, Comparable<ProductFamily> {

    @Id
    @SequenceGenerator(name = "SEQ_PRODUCT_FAMILY", schema = "athena", sequenceName = "SEQ_PRODUCT_FAMILY")
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "SEQ_PRODUCT_FAMILY")
    private Long productFamilyId;

    @Nonnull
    @Column(name ="NAME", nullable = false)
    private String name;

    /** Name of the Sequence Only Product Family.  Must be updated if the name is changed in the database! */
    private static final String SEQUENCE_ONLY_NAME = "Sequence Only";
    public static final String RNA_FAMILY_NAME = "RNA";

    public enum ProductFamilyName {
        RNA("RNA"),
        SMALL_DESIGN_VALIDATION_EXTENSION("Small Design, Validation & Extension"),
        SAMPLE_INITIATION_QUALIFICATION_CELL_CULTURE("Sample Initiation, Qualification & Cell Culture"),
        EXOME("Exome"),
        WHOLE_GENOME("Whole Genome"),
        DE_NOVO_ASSEMBLY("de novo Assembly"),
        MICROBIAL_VIRAL_ANALYSIS("Microbial & Viral Analysis"),
        SEQUENCE_ONLY("Sequence Only"),
        METAGENOMICS("Metagenomics"),
        ALTERNATE_LIBRARY_PREP_DEVELOPMENT("Alternate Library Prep & Development"),
        EPIGENOMICS("Epigenomics"),
        DATA_ANALYSIS("Data Analysis");

        private final String familyName;
        ProductFamilyName(String familyName) {
            this.familyName = familyName;
        }

        public String getFamilyName() {
            return familyName;
        }
    }

    /**
     * JPA package visible constructor. Stripes requires the empty constructor.
     */
    protected ProductFamily() {
    }

    public ProductFamily(@Nonnull String name) {
        if (name == null) {
            throw new NullPointerException("Null name!");
        }
        this.name = name;
    }

    public boolean isSupportsNumberOfLanes() {
        return SEQUENCE_ONLY_NAME.equals(name);
    }

    public Long getProductFamilyId() {
        return productFamilyId;
    }

    @Nonnull
    public String getName() {
        return name;
    }

    @Override
    public int compareTo(ProductFamily that) {
        CompareToBuilder builder = new CompareToBuilder();
        builder.append(getName(), that.getName());
        return builder.build();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;

        if (!(o instanceof ProductFamily)) return false;

        ProductFamily that = (ProductFamily) o;

        return new EqualsBuilder().append(name, that.getName()).isEquals();

    }

    @Override
    public int hashCode() {
        return new HashCodeBuilder().append(name).toHashCode();
    }

    public boolean isSupportsRin() {
        return name.equals(RNA_FAMILY_NAME);
    }
}

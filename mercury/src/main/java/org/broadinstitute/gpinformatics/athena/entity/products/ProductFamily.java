package org.broadinstitute.gpinformatics.athena.entity.products;


import org.apache.commons.lang3.builder.CompareToBuilder;
import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;
import org.broadinstitute.gpinformatics.infrastructure.submission.SubmissionLibraryDescriptor;
import org.hibernate.envers.Audited;

import javax.annotation.Nonnull;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.SequenceGenerator;
import javax.persistence.Table;
import javax.persistence.UniqueConstraint;
import java.io.Serializable;


/**
 * Core entity for ProductFamilies.
 */
@Entity
@Audited
@Table(schema = "athena", uniqueConstraints = @UniqueConstraint(columnNames = "NAME"))
public class ProductFamily implements Serializable, Comparable<ProductFamily> {

    private static final long serialVersionUID = 234809472774666093L;

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
    public static final String WHOLE_GENOME_GENOTYPING = "Whole Genome Genotyping";
    public static final String WHOLE_GENOME_SEQUENCING = "Whole Genome Sequencing";
    public static final String SAMPLE_INITIATION_QUALIFICATION_CELL_CULTURE_NAME = "Sample Initiation, Qualification & Cell Culture";

    public enum ProductFamilyInfo {
        RNA("RNA"),
        SMALL_DESIGN_VALIDATION_EXTENSION("Small Design, Validation & Extension"),
        SAMPLE_INITIATION_QUALIFICATION_CELL_CULTURE(SAMPLE_INITIATION_QUALIFICATION_CELL_CULTURE_NAME),
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
        private final SubmissionLibraryDescriptor submissionLibraryDescriptor;

        ProductFamilyInfo(String familyName) {
            this(familyName, ProductFamily.defaultLibraryDescriptor());
        }

        ProductFamilyInfo(String familyName, SubmissionLibraryDescriptor submissionLibraryDescriptor) {
            this.familyName = familyName;
            this.submissionLibraryDescriptor = submissionLibraryDescriptor;
        }

        public String getFamilyName() {
            return familyName;
        }

        public SubmissionLibraryDescriptor getSubmissionLibraryDescriptor() {
            return submissionLibraryDescriptor;
        }

        public static ProductFamilyInfo byFamilyName(String familyName) {
            for (ProductFamilyInfo productFamilyInfo : values()) {
                if (familyName.equals(productFamilyInfo.familyName)) {
                    return productFamilyInfo;
                }
            }
            return null;
        }
    }

    public static SubmissionLibraryDescriptor defaultLibraryDescriptor() {
        return SubmissionLibraryDescriptor.WHOLE_GENOME;
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

    public SubmissionLibraryDescriptor getSubmissionType() {
        ProductFamilyInfo productFamilyInfo = ProductFamilyInfo.byFamilyName(name);
        if (productFamilyInfo != null) {
            return productFamilyInfo.getSubmissionLibraryDescriptor();
        }
        return null;
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

    public boolean isSupportsPico() {
        return !name.equals(RNA_FAMILY_NAME);
    }

    public boolean isSupportsRin() {
        return name.equals(RNA_FAMILY_NAME);
    }

    public boolean isSupportsSkippingQuote() {
        return SAMPLE_INITIATION_QUALIFICATION_CELL_CULTURE_NAME.equalsIgnoreCase(name);
    }
}

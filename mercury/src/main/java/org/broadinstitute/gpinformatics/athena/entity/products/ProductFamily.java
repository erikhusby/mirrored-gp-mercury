package org.broadinstitute.gpinformatics.athena.entity.products;


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
@Table(uniqueConstraints = @UniqueConstraint(columnNames = {"name"}))
public class ProductFamily implements Serializable {

    /**
     * Known product families, a DAO method might accept one of these to return a persistent or detached instance
     * of one of these ProductFamilies if there was business logic that wanted to call out a specific ProductFamily.
     */
    public enum ProductFamilyName {
        GENERAL_PRODUCTS("General Products"),
        EXOME_SEQUENCING_ANALYSIS("Exome Sequencing Analysis"),
        WHOLE_GENOME_SEQUENCING_ANALYSIS("Whole Genome Sequencing Analysis"),
        WHOLE_GENOME_ARRAY_ANALYSIS("Whole Genome Array Analysis"),
        RNA_ANALYSIS("RNA Analysis"),
        ASSEMBLY_ANALYSIS("Assembly Analysis"),
        METAGENOMIC_ANALYSIS("Metagenomic Analysis"),
        EPIGENOMIC_ANALYSIS("Epigenomic Analysis"),
        ILLUMINA_SEQUENCING_ONLY("Illumina Sequencing Only"),
        ALTERNATIVE_TECHNOLOGIES("Alternative Technologies"),
        CUSTOM_PRODUCTS_TARGETED_SEQUENCING("Targeted Sequencing");

        private final String displayName;


        ProductFamilyName(String displayName) {
            this.displayName = displayName;
        }

        public String getDisplayName() {
            return displayName;
        }
    }

    @Id
    @SequenceGenerator(name = "SEQ_PRODUCT_FAMILY", sequenceName = "SEQ_PRODUCT_FAMILY")
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "SEQ_PRODUCT_FAMILY")
    private Long id;

    private String name;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof ProductFamily)) return false;

        ProductFamily that = (ProductFamily) o;

        if (!name.equals(that.name)) return false;

        return true;
    }

    @Override
    public int hashCode() {
        return name.hashCode();
    }
}

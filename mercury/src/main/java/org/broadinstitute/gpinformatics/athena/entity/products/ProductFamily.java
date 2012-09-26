package org.broadinstitute.gpinformatics.athena.entity.products;


import org.hibernate.envers.Audited;

import javax.persistence.*;


@Entity
@Audited
@Table(uniqueConstraints = @UniqueConstraint(columnNames = {"name"}))
/**
 *
 * MLC making this a class and not an enum for the same reason we don't have concrete Products as subclasses
 * of an abstract AbstractProduct class: we want the ability to define these without changing code.
 * ProductFamily is by nature an enumy thing (in its current state it's nothing more than a controlled vocabulary name)
 * and does seem to beg for a nice way of being able to summon up persistent or detached copies of well known
 * ProductFamilies.  This is going to be a general problem in Mercury/Athena in need of a general solution.

 * It's also possible that ProductFamilies turn out to be fairly static and an enum would suffice.
 *
 */
public class ProductFamily {

    /**
     * Known product families, a DAO method might accept one of these to return a persistent or detached instance
     * of one of these ProductFamilies if there was business logic that wanted to call out a specific ProductFamily.
     */
    public enum Name {
        EXOME_EXPRESS,
        EXOME_SEQUENCING,
        EXOME_CHIP,
        WHOLE_GENOME_SEQUENCING,
        RNA_SEQUENCING,
        DENOVO_ASSEMBLY,
        FLUIDIGM;
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
}

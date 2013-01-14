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
@Table(schema = "athena", uniqueConstraints = @UniqueConstraint(columnNames = "name"))
public class ProductFamily implements Serializable, Comparable<ProductFamily> {

    @Id
    @SequenceGenerator(name = "SEQ_PRODUCT_FAMILY", schema = "athena", sequenceName = "SEQ_PRODUCT_FAMILY")
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "SEQ_PRODUCT_FAMILY")
    private Long productFamilyId;

    private String name;

    /** Name of the Sequence Only Product Family.  Must be updated if the name is changed in the database! */
    private static final String SEQUENCE_ONLY_NAME = "Sequence Only";

    /**
     * JPA package visible constructor. Need the empty constructor so that stripes can create this when needed
     */
    public ProductFamily() {
    }

    public ProductFamily(@Nonnull String name) {
        if (name == null) {
            throw new NullPointerException("Null name!");
        }
        this.name = name;
    }

    public boolean isSupportsNumberOfLanes() {
        return name.equals(SEQUENCE_ONLY_NAME);
    }

    public Long getProductFamilyId() {
        return productFamilyId;
    }

    public String getName() {
        return name;
    }

    @Override
    public int compareTo(ProductFamily that) {
        CompareToBuilder builder = new CompareToBuilder();
        builder.append(this.getName(), that.getName());
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

    public void setProductFamilyId(Long productFamilyId) {
        this.productFamilyId = productFamilyId;
    }
}

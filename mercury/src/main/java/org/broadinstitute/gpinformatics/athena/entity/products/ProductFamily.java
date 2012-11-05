package org.broadinstitute.gpinformatics.athena.entity.products;


import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;
import org.hibernate.envers.Audited;

import javax.persistence.*;
import java.io.Serializable;
import java.util.Comparator;


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


    @Id
    @SequenceGenerator(name = "SEQ_PRODUCT_FAMILY", schema = "athena", sequenceName = "SEQ_PRODUCT_FAMILY")
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "SEQ_PRODUCT_FAMILY")
    private Long productFamilyId;

    private String name;


    public static final Comparator<ProductFamily> PRODUCT_FAMILY_COMPARATOR = new Comparator<ProductFamily>() {
        @Override
        public int compare(ProductFamily productFamily, ProductFamily productFamily1) {
            return productFamily.getName().compareToIgnoreCase(productFamily1.getName());
        }
    };


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

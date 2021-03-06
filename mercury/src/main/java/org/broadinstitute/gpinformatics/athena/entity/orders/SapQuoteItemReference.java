package org.broadinstitute.gpinformatics.athena.entity.orders;

import org.apache.commons.lang3.builder.CompareToBuilder;
import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;
import org.broadinstitute.gpinformatics.athena.entity.products.Product;
import org.broadinstitute.gpinformatics.infrastructure.jpa.UpdatedEntityInterceptor;
import org.hibernate.envers.Audited;
import org.jetbrains.annotations.NotNull;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.EntityListeners;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.SequenceGenerator;
import javax.persistence.Table;
import java.io.Serializable;

@Entity
@EntityListeners(UpdatedEntityInterceptor.class)
@Audited
@Table(name = "SAP_QUOTE_ITEM_REFERENCE", schema = "athena")
public class SapQuoteItemReference implements Serializable, Comparable<SapQuoteItemReference> {

    private static final long serialVersionUID = 5984798910740489773L;
    @Id
    @SequenceGenerator(name = "SEQ_SAP_QUOTE_ITEM_REF", schema = "athena", sequenceName = "SEQ_SAP_QUOTE_ITEM_REF")
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "SEQ_SAP_QUOTE_ITEM_REF")
    @Column(name = "SAP_QUOTE_ITEM_REFERENCE_ID")
    private Long sapQuoteItemReferenceId;

    @ManyToOne
    @JoinColumn(name = "MATERIAL_REFERENCE_PRODUCT_ID")
    private Product materialReference;

    @Column(name = "QUOTE_LINE_REFERENCE")
    private String quoteLineReference;

    @ManyToOne
    @JoinColumn(name = "PARENT_PRODUCT_ORDER")
    private ProductOrder parentOrderDetail;

    public SapQuoteItemReference() {
    }

    public SapQuoteItemReference(Product materialReference, String quoteLineReference) {
        this.materialReference = materialReference;
        this.quoteLineReference = quoteLineReference;
    }

    public Product getMaterialReference() {
        return materialReference;
    }

    public String getQuoteLineReference() {
        return quoteLineReference;
    }

    public ProductOrder getParentOrderDetail() {
        return parentOrderDetail;
    }

    public void setParentOrderDetail(ProductOrder parentOrderDetail) {
        this.parentOrderDetail = parentOrderDetail;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }

        if (o == null || getClass() != o.getClass()) {
            return false;
        }

        SapQuoteItemReference that = (SapQuoteItemReference) o;

        return new EqualsBuilder()
                .append(getMaterialReference(), that.getMaterialReference())
                .append(getQuoteLineReference(), that.getQuoteLineReference())
                .isEquals();
    }

    @Override
    public int hashCode() {
        return new HashCodeBuilder(17, 37)
                .append(getMaterialReference())
                .append(getQuoteLineReference())
                .toHashCode();
    }

    @Override
    public int compareTo(@NotNull SapQuoteItemReference o) {
        CompareToBuilder compareToBuilder = new CompareToBuilder();

        compareToBuilder.append(getMaterialReference(), o.getMaterialReference())
                .append(getQuoteLineReference(), o.getQuoteLineReference());

        return compareToBuilder.build();
    }
}

package org.broadinstitute.gpinformatics.athena.entity.orders;

import org.apache.commons.lang3.builder.CompareToBuilder;
import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;
import org.broadinstitute.gpinformatics.infrastructure.jpa.Updatable;
import org.broadinstitute.gpinformatics.infrastructure.jpa.UpdatedEntityInterceptor;
import org.broadinstitute.gpinformatics.mercury.entity.UpdateData;
import org.hibernate.envers.Audited;

import javax.persistence.CascadeType;
import javax.persistence.Column;
import javax.persistence.Embedded;
import javax.persistence.Entity;
import javax.persistence.EntityListeners;
import javax.persistence.FetchType;
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
@Table(name = "SAP_ORDER_DETAIL", schema = "athena")
public class SapOrderDetail implements Serializable, Updatable, Comparable<SapOrderDetail> {

    private static final long serialVersionUID = -4618988251536159923L;

    @Id
    @SequenceGenerator(name = "SEQ_SAP_ORDER_DETAIL", schema = "athena", sequenceName = "SEQ_SAP_ORDER_DETAIL")
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "SEQ_SAP_ORDER_DETAIL")
    @Column(name = "SAP_ORDER_DETAIL_ID")
    private Long sapOrderDetailId;

    @Column(name = "SAP_ORDER_NUMBER")
    private String sapOrderNumber;

    @Embedded
    private UpdateData updateData = new UpdateData();

    @Column(name = "PRIMARY_QUANTITY")
    private int primaryQuantity;

    private String quoteId;

    private String companyCode;

    @ManyToOne(fetch = FetchType.LAZY, cascade = {CascadeType.PERSIST, CascadeType.MERGE})
    @JoinColumn(name="REFERENCE_PRODUCT_ORDER")
    private ProductOrder referenceProductOrder;

    public SapOrderDetail() {
    }

    public SapOrderDetail(String sapOrderNumber, int primaryQuantity, String quoteId, String companyCode) {
        this.sapOrderNumber = sapOrderNumber;
        this.primaryQuantity = primaryQuantity;
        this.quoteId = quoteId;
    }

    public Long getSapOrderDetailId() {
        return sapOrderDetailId;
    }

    public String getSapOrderNumber() {
        return sapOrderNumber;
    }

    public int getPrimaryQuantity() {
        return primaryQuantity;
    }

    public void setPrimaryQuantity(int primaryQuantity) {
        this.primaryQuantity = primaryQuantity;
    }

    public ProductOrder getReferenceProductOrder() {
        return referenceProductOrder;
    }

    public void setReferenceProductOrder(
            ProductOrder referenceProductOrder) {
        this.referenceProductOrder = referenceProductOrder;
    }

    public String getQuoteId() {
        return quoteId;
    }

    public String getCompanyCode() {
        return companyCode;
    }

    public void setQuoteId(String quoteId) {
        this.quoteId = quoteId;
    }

    public void setCompanyCode(String companyCode) {
        this.companyCode = companyCode;
    }

    @Override
    public int compareTo(SapOrderDetail that) {

        CompareToBuilder compareToBuilder = new CompareToBuilder();

        compareToBuilder.append(updateData.getCreatedDate(), that.updateData.getCreatedDate());

        return compareToBuilder.build();
    }

    @Override
    public UpdateData getUpdateData() {
        return updateData;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }

        if (o == null || getClass() != o.getClass()) {
            return false;
        }

        SapOrderDetail that = (SapOrderDetail) o;

        return new EqualsBuilder()
                .append(primaryQuantity, that.primaryQuantity)
                .append(sapOrderNumber, that.sapOrderNumber)
                .append(referenceProductOrder, that.referenceProductOrder)
                .isEquals();
    }

    @Override
    public int hashCode() {
        return new HashCodeBuilder(17, 37)
                .append(sapOrderNumber)
                .append(primaryQuantity)
                .append(referenceProductOrder)
                .toHashCode();
    }
}

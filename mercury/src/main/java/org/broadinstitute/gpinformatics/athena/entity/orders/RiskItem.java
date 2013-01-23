package org.broadinstitute.gpinformatics.athena.entity.orders;

import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;
import org.broadinstitute.gpinformatics.athena.entity.products.RiskCriteria;
import org.hibernate.annotations.Index;
import org.hibernate.envers.Audited;

import javax.persistence.*;
import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;

/**
 * The purpose of this class is to capture any risks that
 * have been calculated on a productOrder sample.
 * Created by IntelliJ IDEA.
 * User: mccrory
 * Date: 1/18/13
 * Time: 4:17 PM
 */
@Entity
@Audited
@Table(schema = "ATHENA", name = "RISK_ITEM" )
public class RiskItem {
    @Id
    @SequenceGenerator(name = "SEQ_RISK", schema = "ATHENA", sequenceName = "SEQ_RISK", allocationSize = 10)
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "SEQ_RISK")
    private Long risk_id;

    @Index(name = "IX_RISK_RISK_CRITERIA")
    @ManyToOne
    @JoinColumn(name = "RISK_CRITERIA_ID")
    private RiskCriteria riskCriteria;

    @Index(name = "IX_RISK_ORDER_SAMPLE")
    @ManyToOne
    @JoinColumn(name = "PRODUCT_ORDER_SAMPLE")
    private ProductOrderSample productOrderSample;

    @Column(name = "occurred_date")
    private Date occurredDate;

    @Column(name = "REMARK")
    private String remark;

    public RiskItem() {
    }

    public RiskItem(RiskCriteria riskCriteria,
                    ProductOrderSample productOrderSample, Date occurredDate) {
        this.riskCriteria = riskCriteria;
        this.productOrderSample = productOrderSample;
        this.occurredDate = occurredDate;
    }

    public RiskCriteria getRiskCriteria() {
        return riskCriteria;
    }

    public void setRiskCriteria(RiskCriteria riskCriteria) {
        this.riskCriteria = riskCriteria;
    }

    public ProductOrderSample getProductOrderSample() {
        return productOrderSample;
    }

    public void setProductOrderSample(ProductOrderSample productOrderSample) {
        this.productOrderSample = productOrderSample;
    }

    public String getRemark() {
        return remark;
    }

    public void setRemark(String remark) {
        this.remark = remark;
    }

    public Date getOccurredDate() {
        return occurredDate;
    }

    public void setOccurredDate(Date occurredDate) {
        this.occurredDate = occurredDate;
    }


    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof RiskItem)) {
            return false;
        }

        final RiskItem riskItem = (RiskItem) o;
        return new EqualsBuilder().append(occurredDate, riskItem.getOccurredDate())
                .append(productOrderSample, riskItem.getProductOrderSample() )
                .append(riskCriteria, riskItem.getRiskCriteria() ).isEquals();

    }

    @Override
    public int hashCode() {
        return new HashCodeBuilder()
                .append(riskCriteria)
                .append(productOrderSample)
                .append(occurredDate).toHashCode();
    }

    public String getInformation() {
        String date = String.format("Current Time: %1$tm/%1$td/%1$tY %1$tH:%1$tM:%1$tS", occurredDate);
        return String.format("on %s, calculated risk of %s with comment: ", date, riskCriteria.getName() + remark);
    }
}

package org.broadinstitute.gpinformatics.athena.entity.orders;

import org.broadinstitute.gpinformatics.athena.entity.products.RiskCriteria;
import org.hibernate.annotations.Index;
import org.hibernate.envers.Audited;

import javax.persistence.*;
import java.text.MessageFormat;
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

        if (!occurredDate.equals(riskItem.occurredDate)) {
            return false;
        }
        if (!productOrderSample.equals(riskItem.productOrderSample)) {
            return false;
        }
        if (!riskCriteria.equals(riskItem.riskCriteria)) {
            return false;
        }

        return true;
    }

    @Override
    public int hashCode() {
        int result = riskCriteria.hashCode();
        result = 31 * result + productOrderSample.hashCode();
        result = 31 * result + occurredDate.hashCode();
        return result;
    }

    public String getInformation() {
        return MessageFormat.format(
                "At {0,time} on {0,date}, calculated risk of {1} with comment: {2}",
                occurredDate, riskCriteria.getName(), remark);
    }
}

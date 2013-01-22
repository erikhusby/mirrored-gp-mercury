package org.broadinstitute.gpinformatics.athena.entity.orders;

import org.broadinstitute.gpinformatics.athena.entity.products.RiskCriteria;
import org.hibernate.annotations.Index;
import org.hibernate.envers.Audited;

import javax.persistence.*;

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

    @Column(name = "REMARK")
    private String remark;

    public RiskItem() {
    }

    public RiskItem(RiskCriteria riskCriteria,
                    ProductOrderSample productOrderSample) {
        this.riskCriteria = riskCriteria;
        this.productOrderSample = productOrderSample;
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
}

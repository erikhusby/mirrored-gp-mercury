package org.broadinstitute.gpinformatics.athena.entity.orders;

import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;
import org.broadinstitute.gpinformatics.athena.entity.products.RiskCriteria;
import org.hibernate.annotations.Index;
import org.hibernate.envers.Audited;

import javax.persistence.*;
import java.text.MessageFormat;
import java.util.Date;

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
    @SequenceGenerator(name = "SEQ_RISK_ITEM", schema = "ATHENA", sequenceName = "SEQ_RISK_ITEM")
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "SEQ_RISK_ITEM")
    private Long risk_item_id;

    @Index(name = "IX_RISK_RISK_CRITERIA")
    @ManyToOne
    @JoinColumn(name = "RISK_CRITERIA_ID", referencedColumnName = "RISK_CRITERIA_ID")
    private RiskCriteria riskCriteria;

    @Column(name = "occurred_date")
    private Date occurredDate;

    @Column(name = "REMARK")
    private String remark;

    public RiskItem() {
    }

    public RiskItem(RiskCriteria riskCriteria, Date occurredDate) {
        this.riskCriteria = riskCriteria;
        this.occurredDate = occurredDate;
    }

    public RiskCriteria getRiskCriteria() {
        return riskCriteria;
    }

    public void setRiskCriteria(RiskCriteria riskCriteria) {
        this.riskCriteria = riskCriteria;
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

    public String getInformation() {
        return MessageFormat.format(
                "At {0,time} on {0,date}, calculated risk of {1} with comment: {2}",
                occurredDate, riskCriteria.getName(), remark);
    }
}

package org.broadinstitute.gpinformatics.athena.entity.orders;

import org.apache.commons.lang3.StringUtils;
import org.broadinstitute.gpinformatics.athena.entity.products.RiskCriterion;
import org.hibernate.annotations.Index;
import org.hibernate.envers.Audited;

import javax.annotation.Nullable;
import javax.persistence.*;
import java.io.Serializable;
import java.text.MessageFormat;
import java.util.Date;

import static org.broadinstitute.gpinformatics.athena.entity.products.Operator.OperatorType.BOOLEAN;

/**
 * The purpose of this class is to capture any risks that have been calculated on a productOrder sample.
 */
@SuppressWarnings("unused")
@Entity
@Audited
@Table(schema = "ATHENA", name = "RISK_ITEM" )
public class RiskItem implements Serializable {
    private static final long serialVersionUID = -7818942360426002526L;

    @Id
    @SequenceGenerator(name = "SEQ_RISK_ITEM", schema = "ATHENA", sequenceName = "SEQ_RISK_ITEM")
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "SEQ_RISK_ITEM")
    private Long riskItemId;

    @Index(name = "IX_RISK_RISK_CRITERIA")
    @ManyToOne
    @JoinColumn(name = "RISK_CRITERIA")
    private RiskCriterion riskCriterion;

    @Column(name = "OCCURRED_DATE")
    private Date occurredDate;

    @Column(name = "COMPARED_VALUE")
    private String comparedValue;

    @Column(name = "REMARK")
    private String remark;

    public RiskItem() {
    }

    public RiskItem(@Nullable RiskCriterion riskCriterion, @Nullable String comparedValue) {
        this.riskCriterion = riskCriterion;
        this.occurredDate = new Date();
        this.comparedValue = comparedValue;
    }

    public RiskItem(RiskCriterion riskCriterion, String comparedValue, String comment) {
        this(riskCriterion, comparedValue);
        this.remark = comment;
    }

    /**
     * This means that the risk item is passed, so don't make the user pass nulls
     *
     * @param comment The user comment on how it passed
     */
    public RiskItem(String comment) {
        this(null, null);
        this.remark = comment;
    }

    public boolean isOnRisk() {
        return riskCriterion != null;
    }

    public Long getRiskItemId() {
        return riskItemId;
    }

    public RiskCriterion getRiskCriterion() {
        return riskCriterion;
    }

    public void setRiskCriterion(RiskCriterion riskCriterion) {
        this.riskCriterion = riskCriterion;
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

    public String getComparedValue() {
        return comparedValue;
    }

    public void setComparedValue(String comparedValue) {
        this.comparedValue = comparedValue;
    }

    public String getInformation() {

        // If the criteria is null, then set empty
        if (riskCriterion == null) {
            return "";
        }

        String comment =
            StringUtils.isBlank(remark) ? "" : MessageFormat.format("with comment: {0}", remark);

        if (riskCriterion.getOperatorType() == BOOLEAN) {
            return MessageFormat.format(
                    "At {0,time} on {0,date}, calculated {1} {2}",
                    occurredDate, riskCriterion.getCalculationString(), comment);
        }

        return MessageFormat.format(
                "At {0,time} on {0,date}, calculated ({1}) risk on value {2} {3}",
                occurredDate, riskCriterion.getCalculationString(), comparedValue, comment);
    }

    @Override
    public String toString() {
        return getInformation();
    }
}

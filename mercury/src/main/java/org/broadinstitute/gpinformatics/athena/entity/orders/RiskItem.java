package org.broadinstitute.gpinformatics.athena.entity.orders;

import org.apache.commons.lang3.StringUtils;
import org.broadinstitute.gpinformatics.athena.entity.products.Operator;
import org.broadinstitute.gpinformatics.athena.entity.products.RiskCriteria;
import org.hibernate.annotations.Index;
import org.hibernate.envers.Audited;

import javax.persistence.*;
import java.io.Serializable;
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
public class RiskItem implements Serializable {
    private static final long serialVersionUID = -7818942360426002526L;

    @Id
    @SequenceGenerator(name = "SEQ_RISK_ITEM", schema = "ATHENA", sequenceName = "SEQ_RISK_ITEM")
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "SEQ_RISK_ITEM")
    private Long risk_item_id;

    @Index(name = "IX_RISK_RISK_CRITERIA")
    @ManyToOne
    private RiskCriteria riskCriteria;

    @Column(name = "OCCURRED_DATE")
    private Date occurredDate;

    @Column(name = "COMPARED_VALUE")
    private String comparedValue;

    @Column(name = "REMARK")
    private String remark;

    RiskItem() {
    }

    public RiskItem(RiskCriteria riskCriteria, String comparedValue) {
        this.riskCriteria = riskCriteria;
        this.occurredDate = new Date();
        this.comparedValue = comparedValue;
    }

    public RiskItem(RiskCriteria riskCriteria, String comparedValue, String comment) {
        this(riskCriteria, comparedValue);
        this.remark = comment;
    }

    /**
     * This means that the risk item is passed, so don't make the user pass nulls
     *
     * @param comment The user comment on how it passed
     */
    public
    RiskItem(String comment) {
        this(null, null);
        this.remark = comment;
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

    public String getComparedValue() {
        return comparedValue;
    }

    public void setComparedValue(String comparedValue) {
        this.comparedValue = comparedValue;
    }

    public String getInformation() {

        // If the criteria is null, then set empty
        if (riskCriteria == null) {
            return "";
        }

        String comment =
            StringUtils.isBlank(remark) ? "" : MessageFormat.format("with comment: {0}", remark);

        if (riskCriteria.getOperator().getType() == Operator.OperatorType.BOOLEAN) {
            return MessageFormat.format(
                    "At {0,time} on {0,date}, calculated {1} {2}",
                    occurredDate, riskCriteria.getCalculationString(), comment);
        }

        return MessageFormat.format(
                "At {0,time} on {0,date}, calculated ({1}) risk on value {2} {3}",
                occurredDate, riskCriteria.getCalculationString(), comparedValue, comment);
    }
}

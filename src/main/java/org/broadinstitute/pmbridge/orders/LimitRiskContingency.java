package org.broadinstitute.pmbridge.orders;

import org.broadinstitute.pmbridge.Namespaces;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlType;
import java.io.Serializable;
import java.math.BigDecimal;

/**
 * Created by IntelliJ IDEA.
 * User: mccrory
 * Date: 8/28/12
 * Time: 3:21 PM
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(namespace = Namespaces.ORDER_NS)
public class LimitRiskContingency extends RiskContingency implements Serializable {

    private BigDecimal limit;
    private LimitType limitType;

    @Override
    public RiskContingencyType getRiskContingencyType() {
        return RiskContingencyType.LIMIT;
    }

    public BigDecimal getLimit() {
        return limit;
    }

    public void setLimit(final BigDecimal limit) {
        this.limit = limit;
    }

    public LimitType getLimitType() {
        return limitType;
    }

    public void setLimitType(final LimitType limitType) {
        this.limitType = limitType;
    }

}

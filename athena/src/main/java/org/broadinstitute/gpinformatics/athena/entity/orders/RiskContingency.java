package org.broadinstitute.gpinformatics.athena.entity.orders;

import org.broadinstitute.gpinformatics.athena.Namespaces;

import javax.xml.bind.annotation.*;
import java.io.Serializable;

/**
 * Created by IntelliJ IDEA.
 * User: mccrory
 * Date: 8/28/12
 * Time: 10:43 AM
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(namespace = Namespaces.ORDER_NS)
@XmlSeeAlso({
        BooleanRiskContingency.class,
        LimitRiskContingency.class
})
public abstract class RiskContingency implements Serializable {

    @XmlAttribute
    private Risk risk;
    @XmlAttribute
    private RiskContingencyType riskContingencyType;
    @XmlAttribute
    private RiskScopeType riskScopeType;

    public Risk getRisk() {
        return risk;
    }

    public void setRisk(final Risk risk) {
        this.risk = risk;
    }

    public abstract RiskContingencyType getRiskContingencyType();

    protected void setRiskContingencyType(final RiskContingencyType riskContingencyType) {
        this.riskContingencyType = riskContingencyType;
    }

    public RiskScopeType getRiskScopeType() {
        return riskScopeType;
    }

    public void setRiskScopeType(final RiskScopeType riskScopeType) {
        this.riskScopeType = riskScopeType;
    }
}

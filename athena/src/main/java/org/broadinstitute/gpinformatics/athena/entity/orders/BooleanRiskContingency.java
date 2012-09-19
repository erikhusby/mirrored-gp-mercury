package org.broadinstitute.gpinformatics.athena.entity.orders;

import org.broadinstitute.gpinformatics.athena.Namespaces;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlType;
import java.io.Serializable;

/**
 * Created by IntelliJ IDEA.
 * User: mccrory
 * Date: 8/28/12
 * Time: 3:20 PM
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(namespace = Namespaces.ORDER_NS)
public final class BooleanRiskContingency extends RiskContingency implements Serializable {

    @XmlAttribute
    private Boolean answer;

    @Override
    public RiskContingencyType getRiskContingencyType() {
        return RiskContingencyType.YESNO;
    }

    public Boolean getAnswer() {
        return answer;
    }

    public void setAnswer(final Boolean answer) {
        this.answer = answer;
    }
}

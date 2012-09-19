package org.broadinstitute.gpinformatics.athena.entity.orders;

import org.broadinstitute.gpinformatics.athena.Namespaces;

import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlEnum;
import javax.xml.bind.annotation.XmlType;

/**
 * This class is intended to be a list of all supported risks.
 * This is not all risk  but just the ones that are supported informatically.
 * <p/>
 * Created by IntelliJ IDEA.
 * User: mccrory
 * Date: 8/28/12
 * Time: 11:57 AM
 */
@XmlType(namespace = Namespaces.ORDER_NS)
@XmlEnum
public enum Risk {

    SAMPLE_DEPLETION("Okay to deplete sample?", RiskContingencyType.YESNO),
    PROCEED_ON_RISK("Okay to proceed on Risk", RiskContingencyType.YESNO);

    @XmlAttribute
    private String riskQuestion;
    @XmlAttribute
    private RiskContingencyType riskContingencyType;

    private Risk(final String riskQuestion, final RiskContingencyType riskContingencyType) {
        this.riskQuestion = riskQuestion;
        this.riskContingencyType = riskContingencyType;
    }
}

package org.broadinstitute.pmbridge.orders;

import org.broadinstitute.pmbridge.Namespaces;

import javax.xml.bind.annotation.XmlEnum;
import javax.xml.bind.annotation.XmlEnumValue;
import javax.xml.bind.annotation.XmlType;

/**
 * An Enum to list the supported types of RiskContingencies
 * Created by IntelliJ IDEA.
 * User: mccrory
 * Date: 8/28/12
 * Time: 2:25 PM
 */
@XmlType(namespace = Namespaces.ORDER_NS)
@XmlEnum(String.class)
public enum RiskContingencyType {
    @XmlEnumValue("YesNo")
    YESNO,
    @XmlEnumValue("Limit")
    LIMIT,
    @XmlEnumValue("SingleChoice")
    SINGLECHOICE,
    @XmlEnumValue("MultiChoice")
    MULTICHOICE,
    @XmlEnumValue("Range")
    RANGE,
    @XmlEnumValue("Text")
    TEXT;
};
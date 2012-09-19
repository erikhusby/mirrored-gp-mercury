package org.broadinstitute.gpinformatics.athena.orders;

import org.broadinstitute.gpinformatics.athena.Namespaces;

import javax.xml.bind.annotation.XmlEnum;
import javax.xml.bind.annotation.XmlEnumValue;
import javax.xml.bind.annotation.XmlType;

/**
 * An enum to list the scope in increasing granularity.
 * Created by IntelliJ IDEA.
 * User: mccrory
 * Date: 8/28/12
 * Time: 2:25 PM
 */
@XmlType(namespace = Namespaces.ORDER_NS)
@XmlEnum(String.class)
public enum RiskScopeType {
    @XmlEnumValue("Order")
    ORDER,
    @XmlEnumValue("Product")
    PRODUCT,
    @XmlEnumValue("Sample")
    SAMPLE;
};



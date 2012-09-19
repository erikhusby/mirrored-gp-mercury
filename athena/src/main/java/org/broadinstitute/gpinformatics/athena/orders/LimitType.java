package org.broadinstitute.gpinformatics.athena.orders;

import org.broadinstitute.gpinformatics.athena.Namespaces;

import javax.xml.bind.annotation.XmlEnum;
import javax.xml.bind.annotation.XmlType;

/**
 * Created by IntelliJ IDEA.
 * User: mccrory
 * Date: 8/29/12
 * Time: 1:44 PM
 */
@XmlType(namespace = Namespaces.ORDER_NS)
@XmlEnum(String.class)
public enum LimitType {

    LESS_THAN,
    EQUAL_TO,
    GREATER_THAN;

}

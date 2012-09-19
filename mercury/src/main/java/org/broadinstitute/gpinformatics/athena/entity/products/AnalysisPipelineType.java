package org.broadinstitute.gpinformatics.athena.entity.products;

import org.broadinstitute.gpinformatics.athena.Namespaces;

import javax.xml.bind.annotation.XmlEnum;
import javax.xml.bind.annotation.XmlType;


/**
 * <p>Java class for AnalysisPipelineType.
 * <p/>
 * <p>The following schema fragment specifies the expected content contained within this class.
 * <p/>
 * <pre>
 * &lt;simpleType name="AnalysisPipelineType">
 *   &lt;restriction base="{http://www.w3.org/2001/XMLSchema}string">
 *     &lt;enumeration value="MPG"/>
 *     &lt;enumeration value="CANCER"/>
 *     &lt;enumeration value="OTHER"/>
 *   &lt;/restriction>
 * &lt;/simpleType>
 * </pre>
 */
@XmlType(namespace = Namespaces.PRODUCT_NS)
@XmlEnum(String.class)
public enum AnalysisPipelineType {

    MPG,
    CANCER,
    OTHER;

    public String value() {
        return name();
    }

    public static AnalysisPipelineType fromValue(String v) {
        return valueOf(v);
    }

}

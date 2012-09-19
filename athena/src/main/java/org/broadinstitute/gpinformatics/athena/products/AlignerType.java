package org.broadinstitute.gpinformatics.athena.products;

import org.broadinstitute.gpinformatics.athena.Namespaces;

import javax.xml.bind.annotation.XmlEnum;
import javax.xml.bind.annotation.XmlType;


/**
 * <p>Java class for AlignerType.
 * <p/>
 * <p>The following schema fragment specifies the expected content contained within this class.
 * <p/>
 * <pre>
 * &lt;simpleType name="AlignerType">
 *   &lt;restriction base="{http://www.w3.org/2001/XMLSchema}string">
 *     &lt;enumeration value="BWA"/>
 *     &lt;enumeration value="MAQ"/>
 *     &lt;enumeration value="TOPHAT"/>
 *   &lt;/restriction>
 * &lt;/simpleType>
 * </pre>
 */
@XmlType(namespace = Namespaces.PRODUCT_NS)
@XmlEnum(String.class)
public enum AlignerType {

    BWA,
    MAQ,
    TOPHAT;

    public String value() {
        return name();
    }

    public static AlignerType fromValue(String v) {
        return valueOf(v);
    }

}

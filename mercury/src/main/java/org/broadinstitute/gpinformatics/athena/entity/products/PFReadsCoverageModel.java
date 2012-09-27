package org.broadinstitute.gpinformatics.athena.entity.products;

import org.broadinstitute.gpinformatics.athena.Namespaces;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlType;
import java.io.Serializable;
import java.math.BigInteger;


/**
 * <p>Java class for PFReadsCoverageModel complex type.
 * <p/>
 * <p>The following schema fragment specifies the expected content contained within this class.
 * <p/>
 * <pre>
 * &lt;complexType name="PFReadsCoverageModel">
 *   &lt;complexContent>
 *     &lt;restriction base="{http://www.w3.org/2001/XMLSchema}anyType">
 *       &lt;attribute name="readsDesired" use="required" type="{http://www.w3.org/2001/XMLSchema}integer" />
 *     &lt;/restriction>
 *   &lt;/complexContent>
 * &lt;/complexType>
 * </pre>
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(namespace = Namespaces.PRODUCT_NS)
public class PFReadsCoverageModel
        implements Serializable {

    private final static long serialVersionUID = 12343L;
    @XmlAttribute(required = true)
    protected BigInteger readsDesired;

    /**
     * Gets the value of the readsDesired property.
     *
     * @return possible object is
     *         {@link java.math.BigInteger }
     */
    public BigInteger getReadsDesired() {
        return readsDesired;
    }

    /**
     * Sets the value of the readsDesired property.
     *
     * @param value allowed object is
     *              {@link java.math.BigInteger }
     */
    public void setReadsDesired(BigInteger value) {
        this.readsDesired = value;
    }

}

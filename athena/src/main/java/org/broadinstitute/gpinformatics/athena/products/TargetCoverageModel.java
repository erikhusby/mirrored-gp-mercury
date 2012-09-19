package org.broadinstitute.gpinformatics.athena.products;

import org.broadinstitute.gpinformatics.athena.Namespaces;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlType;
import java.io.Serializable;
import java.math.BigInteger;


/**
 * <p>Java class for TargetCoverageModel complex type.
 * <p/>
 * <p>The following schema fragment specifies the expected content contained within this class.
 * <p/>
 * <pre>
 * &lt;complexType name="TargetCoverageModel">
 *   &lt;complexContent>
 *     &lt;restriction base="{http://www.w3.org/2001/XMLSchema}anyType">
 *       &lt;attGroup ref="{urn:Topic}targetCoverageModelAttrs"/>
 *     &lt;/restriction>
 *   &lt;/complexContent>
 * &lt;/complexType>
 * </pre>
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(namespace = Namespaces.PRODUCT_NS)
public class TargetCoverageModel
        implements Serializable {

    private final static long serialVersionUID = 12343L;
    @XmlAttribute(required = true)
    protected BigInteger coveragePercentage;
    @XmlAttribute(required = true)
    protected BigInteger depth;

    /**
     * Gets the value of the coveragePercentage property.
     *
     * @return possible object is
     *         {@link java.math.BigInteger }
     */
    public BigInteger getCoveragePercentage() {
        return coveragePercentage;
    }

    /**
     * Sets the value of the coveragePercentage property.
     *
     * @param value allowed object is
     *              {@link java.math.BigInteger }
     */
    public void setCoveragePercentage(BigInteger value) {
        this.coveragePercentage = value;
    }

    /**
     * Gets the value of the depth property.
     *
     * @return possible object is
     *         {@link java.math.BigInteger }
     */
    public BigInteger getDepth() {
        return depth;
    }

    /**
     * Sets the value of the depth property.
     *
     * @param value allowed object is
     *              {@link java.math.BigInteger }
     */
    public void setDepth(BigInteger value) {
        this.depth = value;
    }

}

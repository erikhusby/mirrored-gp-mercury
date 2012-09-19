package org.broadinstitute.gpinformatics.athena.entity.products;

import org.broadinstitute.gpinformatics.athena.Namespaces;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlType;
import java.io.Serializable;
import java.math.BigInteger;


/**
 * Per Lauren, this corresponds to the "Depth" option on the PMBridge UI.  Platform PM's
 * will be responsible for translating this value into one of the "real" coverage models
 * as appropriate.
 * <p/>
 * <p/>
 * <p>Java class for ProgramPseudoDepthCoverageModel complex type.
 * <p/>
 * <p>The following schema fragment specifies the expected content contained within this class.
 * <p/>
 * <pre>
 * &lt;complexType name="ProgramPseudoDepthCoverageModel">
 *   &lt;complexContent>
 *     &lt;restriction base="{http://www.w3.org/2001/XMLSchema}anyType">
 *       &lt;sequence>
 *         &lt;element name="coverageDesired" type="{http://www.w3.org/2001/XMLSchema}integer"/>
 *       &lt;/sequence>
 *     &lt;/restriction>
 *   &lt;/complexContent>
 * &lt;/complexType>
 * </pre>
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(namespace = Namespaces.PRODUCT_NS, propOrder = {
        "coverageDesired"
})
public class ProgramPseudoDepthCoverageModel
        implements Serializable {

    private final static long serialVersionUID = 12343L;
    @XmlElement(required = true)
    protected BigInteger coverageDesired;

    /**
     * Gets the value of the coverageDesired property.
     *
     * @return possible object is
     *         {@link java.math.BigInteger }
     */
    public BigInteger getCoverageDesired() {
        return coverageDesired;
    }

    /**
     * Sets the value of the coverageDesired property.
     *
     * @param value allowed object is
     *              {@link java.math.BigInteger }
     */
    public void setCoverageDesired(BigInteger value) {
        this.coverageDesired = value;
    }

}

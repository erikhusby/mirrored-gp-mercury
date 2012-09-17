package org.broadinstitute.pmbridge.products;

import org.broadinstitute.pmbridge.Namespaces;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlType;
import java.io.Serializable;
import java.math.BigDecimal;


/**
 * <p>Java class for AttemptedLanesCoverageModel complex type.
 * <p/>
 * <p>The following schema fragment specifies the expected content contained within this class.
 * <p/>
 * <pre>
 * &lt;complexType name="AttemptedLanesCoverageModel">
 *   &lt;complexContent>
 *     &lt;restriction base="{http://www.w3.org/2001/XMLSchema}anyType">
 *       &lt;attribute name="attemptedLanes" use="required" type="{http://www.w3.org/2001/XMLSchema}decimal" />
 *     &lt;/restriction>
 *   &lt;/complexContent>
 * &lt;/complexType>
 * </pre>
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(namespace = Namespaces.PRODUCT_NS)
public class AttemptedLanesCoverageModel
        implements Serializable {

    private final static long serialVersionUID = 12343L;
    @XmlAttribute(required = true)
    protected BigDecimal attemptedLanes;

    /**
     * Gets the value of the attemptedLanes property.
     *
     * @return possible object is
     *         {@link java.math.BigDecimal }
     */
    public BigDecimal getAttemptedLanes() {
        return attemptedLanes;
    }

    /**
     * Sets the value of the attemptedLanes property.
     *
     * @param value allowed object is
     *              {@link java.math.BigDecimal }
     */
    public void setAttemptedLanes(BigDecimal value) {
        this.attemptedLanes = value;
    }

}

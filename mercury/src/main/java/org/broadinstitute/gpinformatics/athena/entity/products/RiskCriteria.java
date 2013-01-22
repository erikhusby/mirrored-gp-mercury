package org.broadinstitute.gpinformatics.athena.entity.products;

import org.broadinstitute.gpinformatics.athena.entity.orders.ProductOrderSample;
import org.hibernate.envers.Audited;

import javax.persistence.*;

/**
 * This base class represents the OnRisk criteria thresholds for a product
 *
 * @author mccrory
 */
@Entity
@Audited
@Table(schema = "ATHENA", name = "RISK_CRITERIA")
@Inheritance(strategy=InheritanceType.SINGLE_TABLE)
@DiscriminatorColumn(
    name="DISCRIMINATOR",
    discriminatorType=DiscriminatorType.STRING
)
public abstract class RiskCriteria {

    @Id
    @SequenceGenerator(name = "SEQ_RISK_CRITERIA", schema = "ATHENA", sequenceName = "SEQ_RISK_CRITERIA")
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "SEQ_RISK_CRITERIA")
    private Long risk_criteria_id;

    @Column(name = "NAME", length = 255)
    private String name;

    /**
     * Check to see if this risk criteria threshold has been crossed is satisfied with the data.
     * @param sample name
     * @return true if the sample is on risk
     */
    public abstract boolean onRisk(ProductOrderSample sample);

    public String getName() {
        return name;
    }

    public void setName(String attribute) {
        this.name = attribute;
    }

}

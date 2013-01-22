package org.broadinstitute.gpinformatics.athena.entity.products;

import clover.org.apache.commons.lang.StringUtils;
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


    protected RiskCriteria() {
    }

    protected RiskCriteria(String name) {
        this.name = name;
    }

    /**
     * Check to see if this risk criteria threshold has been crossed is satisfied with the data.
     * @param sample name
     * @return true if the sample is on risk
     */
    public abstract boolean onRisk(ProductOrderSample sample);

    public String getName() {
        if (StringUtils.isBlank(name)) {
            throw new NullPointerException( "Invalid Risk Criteria: Name must be non-null.");
        }
        return name;
    }

    public void setName(String attribute) {
        this.name = attribute;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof RiskCriteria)) {
            return false;
        }

        final RiskCriteria that = (RiskCriteria) o;

        if (name != null ? !name.equals(that.name) : that.name != null) {
            return false;
        }
        if (risk_criteria_id != null ? !risk_criteria_id.equals(that.risk_criteria_id) :
                that.risk_criteria_id != null) {
            return false;
        }

        return true;
    }

    @Override
    public int hashCode() {
        int result = risk_criteria_id != null ? risk_criteria_id.hashCode() : 0;
        result = 31 * result + (name != null ? name.hashCode() : 0);
        return result;
    }
}

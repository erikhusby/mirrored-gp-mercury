package org.broadinstitute.gpinformatics.athena.entity.products;

import org.broadinstitute.gpinformatics.athena.entity.orders.ProductOrderSample;

import javax.persistence.*;

/**
 * Created by IntelliJ IDEA.
 * User: mccrory
 * Date: 1/18/13
 * Time: 3:55 PM
 */
@Entity
@Table(name="risk_criteria")
@DiscriminatorValue("NUM")
public class ConcentrationRiskCriteria extends RiskCriteria {

    @Column(name = "numeric_operator", length = 30)
    @Enumerated(EnumType.STRING)
    private NumericOperator numericOperator;

    @Column(name = "numeric_value")
    private Double value;

    public ConcentrationRiskCriteria() {
    }

    public ConcentrationRiskCriteria(String name, NumericOperator numericOperator, Double value) {
        super(name);
        if ( ( numericOperator == null ) || (value == null)) {
            throw new NullPointerException( "Invalid Risk Criteria: Operator and value must be non-null.");
        }
        this.numericOperator = numericOperator;
        this.value = value;
    }

    @Override
    public boolean onRisk(ProductOrderSample sample) {
        boolean onRiskStatus = true;
        if ((sample != null) && (sample.getBspDTO() != null)) {
            onRiskStatus = numericOperator.apply(sample.getBspDTO().getConcentration(), value);
        }
        return onRiskStatus;
    }

    public NumericOperator getNumericOperator() {
        return numericOperator;
    }

    public void setNumericOperator(NumericOperator numericOperator) {
        this.numericOperator = numericOperator;
    }

    public Double getValue() {
        return value;
    }

    public void setValue(Double value) {
        this.value = value;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof ConcentrationRiskCriteria)) {
            return false;
        }
        if (!super.equals(o)) {
            return false;
        }

        final ConcentrationRiskCriteria that = (ConcentrationRiskCriteria) o;

        if (numericOperator != that.numericOperator) {
            return false;
        }
        if (value != null ? !value.equals(that.value) : that.value != null) {
            return false;
        }

        return true;
    }

    @Override
    public int hashCode() {
        int result = super.hashCode();
        result = 31 * result + (numericOperator != null ? numericOperator.hashCode() : 0);
        result = 31 * result + (value != null ? value.hashCode() : 0);
        return result;
    }
}

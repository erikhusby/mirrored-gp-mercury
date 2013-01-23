package org.broadinstitute.gpinformatics.athena.entity.products;

import org.broadinstitute.gpinformatics.athena.entity.orders.ProductOrderSample;

import javax.annotation.Nonnull;
import javax.persistence.*;

/**
 * Created by IntelliJ IDEA.
 * User: mccrory
 * Date: 1/18/13
 * Time: 3:55 PM
 */
@Entity
@Table(name="risk_criteria")
@DiscriminatorValue("ORIGMT")
public class OrigMaterialTypeRiskCriteria extends RiskCriteria {

    @Column(name = "string_operator", length = 30)
    @Enumerated(EnumType.STRING)
    private StringOperator stringOperator;

    @Column(name = "string_value")
    private String value;

    public OrigMaterialTypeRiskCriteria() {
    }

    public OrigMaterialTypeRiskCriteria(@Nonnull String name, @Nonnull StringOperator stringOperator, @Nonnull String value) {
        super(name);
        this.stringOperator = stringOperator;
        this.value = value;
    }

    @Override
    public boolean onRisk(ProductOrderSample sample) {
        boolean onRiskStatus = true;
        if ( sample != null ) {
//TODO GPLIM-645 - We need an API for Orig Material Type -- mt api call is just a place holder  !!!!!!!!!!!!!!!!!
            onRiskStatus = stringOperator.apply( sample.getBspDTO().getMaterialType(), value);
        }
        return onRiskStatus;
    }

    public StringOperator getStringOperator() {
        return stringOperator;
    }

    public void setStringOperator(StringOperator stringOperator) {
        this.stringOperator = stringOperator;
    }

    public String getValue() {
        return value;
    }

    public void setValue(String value) {
        this.value = value;
    }


    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof OrigMaterialTypeRiskCriteria)) {
            return false;
        }
        if (!super.equals(o)) {
            return false;
        }

        final OrigMaterialTypeRiskCriteria that = (OrigMaterialTypeRiskCriteria) o;

        if (stringOperator != that.stringOperator) {
            return false;
        }
        if (!value.equals(that.value)) {
            return false;
        }

        return true;
    }

    @Override
    public int hashCode() {
        int result = super.hashCode();
        result = 31 * result + stringOperator.hashCode();
        result = 31 * result + value.hashCode();
        return result;
    }
}

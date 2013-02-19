package org.broadinstitute.gpinformatics.athena.entity.products;

import org.broadinstitute.gpinformatics.athena.entity.orders.ProductOrderSample;
import org.hibernate.envers.Audited;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.persistence.*;
import java.io.Serializable;
import java.text.MessageFormat;
import java.util.List;

/**
 * This base class represents the OnRisk criteria thresholds for a product
 *
 * @author mccrory
 */
@Entity
@Audited
@Table(schema = "ATHENA", name = "RISK_CRITERIA")
public class RiskCriteria {

    private static final boolean DISPLAYED = true;
    private static final boolean NOT_DISPLAYED = false;

    @Id
    @SequenceGenerator(name = "SEQ_RISK_CRITERIA", schema = "ATHENA", sequenceName = "SEQ_RISK_CRITERIA")
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "SEQ_RISK_CRITERIA")
    private Long risk_criteria_id;

    @Column(name = "type", length = 30, nullable = false)
    @Enumerated(EnumType.STRING)
    private RiskCriteriaType type;

    @Column(name = "operator", length = 30, nullable = false)
    @Enumerated(EnumType.STRING)
    private Operator operator;

    @Column(name = "value")
    private String value;

    protected RiskCriteria() {
    }

    public RiskCriteria(@Nonnull RiskCriteriaType type, @Nonnull Operator operator, @Nullable String value) {
        if (!type.getOperators().contains(operator)) {
            throw new RuntimeException("operator: " + operator.getLabel() + " is not allowed on type: " + type.getLabel());
        }

        this.type = type;
        this.operator = operator;
        this.value = value;
    }

    /**
     * Check to see if this risk criteria threshold has been crossed is satisfied with the data.
     * @param sample name
     * @return true if the sample is on risk
     */
    public boolean onRisk(ProductOrderSample sample) {
        boolean onRiskStatus = false;
        if ((sample != null) && (sample.getBspDTO() != null)) {
            onRiskStatus = type.getRiskStatus(sample, operator, value);
        }

        return onRiskStatus;
    }

    public Operator getOperator() {
        return operator;
    }

    public void setOperator(Operator operator) {
        this.operator = operator;
    }

    public String getValue() {
        return value;
    }

    public void setValue(String value) {
        this.value = value;
    }

    public RiskCriteriaType getType() {
        return type;
    }

    public void setType(RiskCriteriaType type) {
        this.type = type;
    }

    public boolean isSame(String criteriaName, String operator, String value) {
        return criteriaName.equals(type.getLabel()) &&
            operator.equals(this.operator.getLabel()) &&
            value.equals(this.value);
    }

    public ValueProvider getValueProvider() {
        return type.valueProvider;
    }

    public static RiskCriteria createManual() {
        // Boolean does not use the value, so just set to true so that the answer is always that the operation is true
        return new RiskCriteria(RiskCriteria.RiskCriteriaType.MANUAL, Operator.IS, "true");
    }

    public String getCalculationString() {
        return MessageFormat.format("{0} {1} {2}", type.getLabel(), operator.getLabel(), operator.getType() == Operator.OperatorType.BOOLEAN ? "" : value);
    }

    public enum RiskCriteriaType {
        CONCENTRATION("Concentration", Operator.OperatorType.NUMERIC, DISPLAYED, new ValueProvider() {
            @Override
            public String getValue(ProductOrderSample sample) {
                return String.valueOf(sample.getBspDTO().getConcentration());
            }
        }),
        FFPE("FFPE", Operator.OperatorType.BOOLEAN, DISPLAYED, new ValueProvider() {
            @Override
            public String getValue(ProductOrderSample sample) {
                return String.valueOf(sample.getBspDTO().getFfpeStatus());
            }
        }),
        MANUAL("Manual", Operator.OperatorType.BOOLEAN, NOT_DISPLAYED, new ValueProvider() {
            @Override
            public String getValue(ProductOrderSample sample) {
                // Manual is used for manually failing a sample.
                return String.valueOf(true);
            }
        }),
        TOTAL_DNA("Total DNA", Operator.OperatorType.NUMERIC, DISPLAYED, new ValueProvider() {
            @Override
            public String getValue(ProductOrderSample sample) {
                return String.valueOf(sample.getBspDTO().getTotal());
            }
        });

        private final Operator.OperatorType operatorType;
        private final String label;
        private final ValueProvider valueProvider;
        private final boolean isDisplayed;

        RiskCriteriaType(String label, Operator.OperatorType operatorType, boolean isDisplayed, ValueProvider valueProvider) {
            this.label = label;
            this.operatorType = operatorType;
            this.valueProvider = valueProvider;
            this.isDisplayed = isDisplayed;
        }

        public String getLabel() {
            return label;
        }

        public boolean isDisplayed() {
            return isDisplayed;
        }

        public boolean getRiskStatus(ProductOrderSample sample, Operator operator, String value) {
            return operator.apply(valueProvider.getValue(sample), value);
        }

        public List<Operator> getOperators() {
            return Operator.findOperatorsByType(operatorType);
        }

        public static RiskCriteriaType findByLabel(String criteriaName) {
            for (RiskCriteriaType type : values()) {
                if (type.getLabel().equals(criteriaName)) {
                    return type;
                }
            }

            return null;
        }
    }

    public abstract static class ValueProvider implements Serializable {
        public abstract String getValue(ProductOrderSample sample);
    }
}

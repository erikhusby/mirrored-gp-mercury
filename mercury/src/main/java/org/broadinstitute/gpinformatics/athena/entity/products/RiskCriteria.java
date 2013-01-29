package org.broadinstitute.gpinformatics.athena.entity.products;

import org.broadinstitute.gpinformatics.athena.entity.orders.ProductOrderSample;
import org.hibernate.envers.Audited;

import javax.annotation.Nonnull;
import javax.persistence.*;
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

    @Column(name = "type", length = 30)
    @Enumerated(EnumType.STRING)
    private RiskCriteriaType type;

    @Column(name = "operator", length = 30)
    @Enumerated(EnumType.STRING)
    private Operator operator;

    @Column(name = "value")
    private String value;

    protected RiskCriteria() {
    }

    public RiskCriteria(@Nonnull RiskCriteriaType type, @Nonnull Operator operator, @Nonnull String value) {
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

    public Object getSampleValue(ProductOrderSample sample) {
        return type.getSampleValue(sample);
    }

    public static RiskCriteria createManual() {
        return new RiskCriteria(RiskCriteria.RiskCriteriaType.MANUAL, Operator.IS, "");
    }

    public String getCalculationString() {
        return MessageFormat.format("{0} {1} {2}", type.getLabel(), operator.getLabel(), value == null ? "" : value);
    }

    public enum RiskCriteriaType {
        CONCENTRATION("Concentration", Operator.OperatorType.NUMERIC, DISPLAYED, new SampleCalculation() {
            @Override
            public Object getSampleValue(ProductOrderSample sample) {
                return sample.getBspDTO().getConcentration();
            }
        }),
        FFPE("FFPE", Operator.OperatorType.BOOLEAN, DISPLAYED, new SampleCalculation() {
            @Override
            public Object getSampleValue(ProductOrderSample sample) {
                return sample.getBspDTO().getFfpeDerived();
            }
        }),
        MANUAL("Manual", Operator.OperatorType.BOOLEAN, NOT_DISPLAYED, new SampleCalculation() {
            @Override
            public Object getSampleValue(ProductOrderSample sample) {
                return true;
            }
        }),
        TOTAL_DNA("Total DNA", Operator.OperatorType.NUMERIC, DISPLAYED, new SampleCalculation() {
            @Override
            public Object getSampleValue(ProductOrderSample sample) {
                return sample.getBspDTO().getTotal();
            }
        });

        private final Operator.OperatorType operatorType;
        private final String label;
        private final SampleCalculation calculation;
        private final boolean isDisplayed;

        RiskCriteriaType(String label, Operator.OperatorType operatorType, boolean isDisplayed, SampleCalculation calculation) {
            this.label = label;
            this.operatorType = operatorType;
            this.calculation = calculation;
            this.isDisplayed = isDisplayed;
        }

        public Object getSampleValue(ProductOrderSample sample) {
            return calculation.getSampleValue(sample);
        }

        public String getLabel() {
            return label;
        }

        public boolean isDisplayed() {
            return isDisplayed;
        }

        public boolean getRiskStatus(ProductOrderSample sample, Operator operator, String value) {
            return calculation.calculateRiskStatus(sample, operator, value);
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

    public static abstract class SampleCalculation {
        boolean calculateRiskStatus(ProductOrderSample sample, Operator operator, String value) {
            return operator.apply(getSampleValue(sample).toString(), value);
        }

        public abstract Object getSampleValue(ProductOrderSample sample);
    }
}

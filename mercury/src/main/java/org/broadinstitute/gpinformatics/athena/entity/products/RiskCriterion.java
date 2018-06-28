package org.broadinstitute.gpinformatics.athena.entity.products;

import org.broadinstitute.gpinformatics.athena.entity.orders.ProductOrderSample;
import org.broadinstitute.gpinformatics.infrastructure.SampleData;
import org.broadinstitute.gpinformatics.mercury.entity.vessel.MaterialType;
import org.hibernate.envers.Audited;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.SequenceGenerator;
import javax.persistence.Table;
import java.io.Serializable;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collector;
import java.util.stream.Collectors;

/**
 * This class handles all the needs for defining an item of a criteria list. Each item has a type, which defines
 * what the user will compare, an operator, which defines the comparison performed and a value, which is what the
 * user value will be compared to.
 */
@SuppressWarnings("unused")
@Entity
@Audited
@Table(schema = "ATHENA", name = "RISK_CRITERIA")
public class RiskCriterion implements Serializable {

    @Id
    @SequenceGenerator(name = "SEQ_RISK_CRITERIA", schema = "ATHENA", sequenceName = "SEQ_RISK_CRITERIA")
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "SEQ_RISK_CRITERIA")
    private Long riskCriteriaId;

    @Column(name = "type", length = 30, nullable = false)
    @Enumerated(EnumType.STRING)
    private RiskCriteriaType type;

    @Column(name = "operator", length = 30, nullable = false)
    @Enumerated(EnumType.STRING)
    private Operator operator;

    @Column(name = "value")
    private String value;

    protected RiskCriterion() {
    }

    /**
     * This provides a way to create a fully specified criterion. On the product page, the user adds to a criteria
     * list and specifies all of this information.
     *
     * @param type The type of criterion from the RiskCriteriaType enum.
     * @param operator The operator that will be used to make the comparison.
     * @param value The value.
     */
    public RiskCriterion(@Nonnull RiskCriteriaType type, @Nonnull Operator operator, @Nullable String value) {
        if (!type.getOperators().contains(operator)) {
            throw new RuntimeException("operator: " + operator.getLabel() + " is not allowed on type: " + type.getLabel());
        }

        this.type = type;
        this.operator = operator;
        this.value = value;
    }

    /**
     * Check to see if this risk criteria threshold has been crossed is satisfied with the data.
     *
     * @param sample name
     *
     * @return true if the sample is on risk
     */
    public boolean onRisk(ProductOrderSample sample) {
        // The sample has to exist and be in bsp format for us to even check the risk status.
        return (sample != null) && sample.isInBspFormat() && type.getRiskStatus(sample, operator, value);
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

    /**
     * This creates a manual type criterion, which is a special boolean that lets the user say force a sample
     * to be on risk. For that reason, the value is always compared to 'true' so the result will be true or false based
     * on being true or false.
     *
     * @return The newly created criterion.
     */
    public static RiskCriterion createManual() {
        return new RiskCriterion(RiskCriterion.RiskCriteriaType.MANUAL, Operator.IS, "true");
    }

    /**
     * @return The string form of the criterion.
     */
    public String getCalculationString() {
        if (operator.getType() == Operator.OperatorType.BOOLEAN) {
            return MessageFormat.format("{0}", type.getLabel());
        }

        return MessageFormat.format("{0} {1} {2}", type.getLabel(), operator.getLabel(), value);
    }

    public Operator.OperatorType getOperatorType() {
        return operator.getType();
    }

    /**
     * An enumeration of all the types that a criterion can take on. If the value provider states that this is
     * displayable (default in base class) it will be shown in the Product create page UI.
     */
    public enum RiskCriteriaType {
        VOLUME("Volume", Operator.OperatorType.NUMERIC, new ValueProvider() {
            private static final long serialVersionUID = -5141795597813734321L;

            @Override
            public String getValue(ProductOrderSample sample) {
                return String.valueOf(sample.getSampleData().getVolume());
            }
        }),
        CONCENTRATION("Concentration", Operator.OperatorType.NUMERIC, new ValueProvider() {
            private static final long serialVersionUID = -6601133301434326498L;

            @Override
            public String getValue(ProductOrderSample sample) {
                return String.valueOf(sample.getSampleData().getConcentration());
            }
        }),
        WGA("Is WGA", Operator.OperatorType.BOOLEAN, new ValueProvider() {
            private static final long serialVersionUID = -4849732345451486536L;

            @Override
            public String getValue(ProductOrderSample sample) {
                return String.valueOf(sample.getSampleData().getMaterialType().contains("WGA"));
            }
        }),
        FFPE("Is FFPE", Operator.OperatorType.BOOLEAN, new ValueProvider() {
            private static final long serialVersionUID = -8406086522548244907L;

            @Override
            public String getValue(ProductOrderSample sample) {
                return String.valueOf(sample.getSampleData().getFfpeStatus());
            }
        }),
        MANUAL("Manual", Operator.OperatorType.BOOLEAN, new ValueProvider() {
            private static final long serialVersionUID = 1909671711388135931L;

            @Override
            public String getValue(ProductOrderSample sample) {
                // Manual is used for manually failing a sample.
                return String.valueOf(true);
            }

            @Override
            public boolean isDisplayed(Product product) {
                return false;
            }

        }),
        TOTAL_DNA("Total DNA", Operator.OperatorType.NUMERIC, new ValueProvider() {
            private static final long serialVersionUID = 5755357964417458956L;

            @Override
            public String getValue(ProductOrderSample sample) {
                return String.valueOf(sample.getSampleData().getTotal());
            }
        }),
        PICO_AGE("Last Pico over a year ago", Operator.OperatorType.BOOLEAN, new ValueProvider() {
            private static final long serialVersionUID = 1601375635726290926L;

            @Override
            public String getValue(ProductOrderSample sample) {
                SampleData sampleDTO = sample.getSampleData();

                // On risk if there is no pico date or if the run date is older than one year ago.
                return String.valueOf(((sampleDTO.getPicoRunDate() == null) ||
                        sampleDTO.getPicoRunDate().before(sample.getProductOrder().getOneYearAgo())));
            }
        }),
        RIN("RIN", Operator.OperatorType.NUMERIC, new ValueProvider() {

            private static final long serialVersionUID = -6022452431986609118L;

            @Override
            public String getValue(ProductOrderSample sample) {
                return getStringValueOfOrNull(sample.getSampleData().getRin());
            }
        }),
        RQS("RQS", Operator.OperatorType.NUMERIC, new ValueProvider() {
            @Override
            public String getValue(ProductOrderSample sample) {
                return getStringValueOfOrNull(sample.getSampleData().getRqs());
            }
        }),
        DV200("DV200", Operator.OperatorType.NUMERIC, new ValueProvider() {
            @Override
            public String getValue(ProductOrderSample sample) {
                return getStringValueOfOrNull(sample.getSampleData().getDv200());
            }
        }),
        MATERIAL_TYPE("Material Type", Operator.OperatorType.STRING, new ValueProvider() {
            @Override
            public String getValue(ProductOrderSample sample) {
                return sample.getSampleData().getMaterialType();
            }
        }, Arrays.stream(MaterialType.values()).map(MaterialType::getDisplayName)
                .collect(Collectors.toList())
        );

        private static String getStringValueOfOrNull(Double value) {
            if (value == null) {
                return null;
            } else {
                return String.valueOf(value);
            }
        }

        private final Operator.OperatorType operatorType;
        private final String label;
        private final ValueProvider valueProvider;
        private final List<String> suggestedValues;

        RiskCriteriaType(String label, Operator.OperatorType operatorType, ValueProvider valueProvider) {
            this(label, operatorType, valueProvider, null);
        }
        RiskCriteriaType(String label, Operator.OperatorType operatorType, ValueProvider valueProvider,
                         List<String> suggestedValues) {
            this.label = label;
            this.operatorType = operatorType;
            this.valueProvider = valueProvider;
            this.suggestedValues = suggestedValues;
        }

        public Operator.OperatorType getOperatorType() {
            return operatorType;
        }

        public String getLabel() {
            return label;
        }

        public boolean getDisplayed(Product product) {
            return valueProvider.isDisplayed(product);
        }

        public boolean getRiskStatus(ProductOrderSample sample, Operator operator, String value) {
            String valueFromSample = valueProvider.getValue(sample);
            if (valueFromSample == null) {
                // Sample is on risk if it has no data to apply the comparison to.
                return true;
            } else {
                return operator.apply(valueFromSample, value);
            }
        }

        public List<String> getSuggestedValues() {
            return suggestedValues;
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
        private static final long serialVersionUID = 746447531515367731L;

        public abstract String getValue(ProductOrderSample sample);

        /**
         * @return This determines whether the particular provider should be displayed based on the product
         */
        public boolean isDisplayed(Product product) {
            return true;
        }
    }
}

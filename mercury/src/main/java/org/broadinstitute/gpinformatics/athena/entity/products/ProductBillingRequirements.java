package org.broadinstitute.gpinformatics.athena.entity.products;

/**
 * This class represents the requirements for a product or addon to be billable.
 *
 * A product or addon is billable if the per-sample result 'attribute' is 'operator' 'value'.
 *
 * @author pshapiro
 */
public class ProductBillingRequirements {

    public enum Operator {
        GREATER_THAN(">"),
        LESS_THAN("<"),
        GREATER_THAN_OR_EQUAL_TO(">="),
        LESS_THAN_OR_EQUAL_TO("<=");

        public final String label;

        static Operator fromLabel(String label) {
            for (Operator op : values()) {
                if (op.label.equals(label)) {
                    return op;
                }
            }
            return null;
        }

        public String getLabel() {
            return label;
        }

        Operator(String label) {
            this.label = label;
        }
    }

    private String attribute;

    private Operator operator;

    private Double value;

    public String getAttribute() {
        return attribute;
    }

    public void setAttribute(String attribute) {
        this.attribute = attribute;
    }

    public Operator getOperator() {
        return operator;
    }

    public void setOperator(Operator operator) {
        this.operator = operator;
    }

    public Double getValue() {
        return value;
    }

    public void setValue(Double value) {
        this.value = value;
    }
}

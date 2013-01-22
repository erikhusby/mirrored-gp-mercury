package org.broadinstitute.gpinformatics.athena.entity.products;

/**
 * Created by IntelliJ IDEA.
 * User: mccrory
 * Date: 1/18/13
 * Time: 6:03 PM
 */
public enum NumericOperator {
    GREATER_THAN(">"),
    GREATER_THAN_OR_EQUAL_TO(">="),
    LESS_THAN("<"),
    LESS_THAN_OR_EQUAL_TO("<=");

    public final String label;

    NumericOperator(String label) {
        this.label = label;
    }

    public boolean apply(Double value, Double threshold) {
        switch (this) {
        case GREATER_THAN:
            return value > threshold;
        case GREATER_THAN_OR_EQUAL_TO:
            return value >= threshold;
        case LESS_THAN:
            return value < threshold;
        case LESS_THAN_OR_EQUAL_TO:
            return value <= threshold;
        }
        throw new RuntimeException();
    }

    static NumericOperator fromLabel(String label) {
        for (NumericOperator op : values()) {
            if (op.label.equals(label)) {
                return op;
            }
        }
        return null;
    }

    public String getLabel() {
        return label;
    }

}

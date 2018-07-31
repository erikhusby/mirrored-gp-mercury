package org.broadinstitute.gpinformatics.athena.entity.products;

import org.broadinstitute.gpinformatics.infrastructure.common.MathUtils;

import java.util.ArrayList;
import java.util.List;

/**
 * This is a generic comparator used for operations AND for billing
 */

public enum Operator {
    GREATER_THAN(">", OperatorType.NUMERIC),
    GREATER_THAN_OR_EQUAL_TO(">=", OperatorType.NUMERIC),
    LESS_THAN("<", OperatorType.NUMERIC),
    LESS_THAN_OR_EQUAL_TO("<=", OperatorType.NUMERIC),
    EQUALS("=", OperatorType.NUMERIC),
    EXACT_MATCH("equals", OperatorType.STRING),
    IS_IN("is in", OperatorType.STRING),
    IS("is On Risk", OperatorType.BOOLEAN);

    private final String label;
    private final OperatorType type;

    private Operator(String label, OperatorType type) {
        this.label = label;
        this.type = type;
    }

    public boolean apply(double d1, double d2) {
        switch (type) {
        case NUMERIC:
            return applyTyped(d1, d2);
        default:
            throw new RuntimeException();
        }
    }

    public boolean apply(String s1, String s2) {
        switch (type) {
        case STRING:
            return applyTyped(s1, s2);
        case NUMERIC:
            try {
                double d1 = Double.parseDouble(s1);
                double d2 = Double.parseDouble(s2);
                return applyTyped(d1, d2);
            } catch (NumberFormatException e) {
                // If we can't parse the arguments, consider the result to be 'failed'.
                return false;
            }
        case BOOLEAN:
            boolean b1 = Boolean.parseBoolean(s1);
            boolean b2 = Boolean.parseBoolean(s2);
            return applyTyped(b1, b2);
        }

        throw new RuntimeException();
    }

    public boolean applyTyped(String s1, String s2) {
        switch (this) {
        case EXACT_MATCH:
            return s1.equalsIgnoreCase(s2);
        case IS_IN:
            return s2.toLowerCase().contains(s1.toLowerCase());
        default:
            throw new RuntimeException();
        }
    }

    public boolean applyTyped(boolean b1, boolean b2) {
        switch (this) {
        case IS:
            return b1 == b2;
        default:
            throw new RuntimeException();
        }
    }

    public boolean applyTyped(double d1, double d2) {
        switch (this) {
        case GREATER_THAN:
            return d1 > d2;
        case GREATER_THAN_OR_EQUAL_TO:
            return d1 >= d2;
        case LESS_THAN:
            return d1 < d2;
        case LESS_THAN_OR_EQUAL_TO:
            return d1 <= d2;
        case EQUALS:
            return MathUtils.isSame(d1, d2);
        default:
            throw new RuntimeException();
        }
    }

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

    public OperatorType getType() {
        return type;
    }

    public static Operator findByLabel(String operatorLabel) {
        for (Operator type : values()) {
            if (type.getLabel().equals(operatorLabel)) {
                return type;
            }
        }

        return null;
    }

    public enum OperatorType {
        STRING, NUMERIC, BOOLEAN;

        public String getName() {
            return name();
        }
    }

    public static List<Operator> findOperatorsByType(OperatorType type) {
        List<Operator> operators = new ArrayList<>();
        for (Operator value : values()) {
            if (type == value.type) {
                operators.add(value);
            }
        }

        return operators;
    }
}
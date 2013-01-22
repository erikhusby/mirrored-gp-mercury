package org.broadinstitute.gpinformatics.athena.entity.products;

/**
 * Created by IntelliJ IDEA.
 * User: mccrory
 * Date: 1/18/13
 * Time: 3:31 PM
 */
public enum StringOperator {
    EQUALS("="),
    IN("IN");

    public final String label;

    StringOperator(String label) {
        this.label = label;
    }

    public boolean apply(String str1, String str2) {
        switch (this) {
        case EQUALS:
            return str1.equals( str2 );
        case IN:
            return str2.contains( str1 );
        }
        throw new RuntimeException();
    }

    static StringOperator fromLabel(String label) {
        for (StringOperator op : values()) {
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


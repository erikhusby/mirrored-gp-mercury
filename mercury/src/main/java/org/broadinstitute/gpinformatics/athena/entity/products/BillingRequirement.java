package org.broadinstitute.gpinformatics.athena.entity.products;

import org.hibernate.envers.Audited;

import javax.persistence.*;

/**
 * This class represents the requirements for a product or add-on to be billable.
 *
 * A product or add-on is billable if the per-sample result 'attribute' is 'operator' 'value'.
 *
 * @author pshapiro
 */
@Entity
@Audited
@Table(schema = "athena", name = "billing_requirement")
public class BillingRequirement {

    @Id
    @SequenceGenerator(name = "SEQ_BILLING_REQUIREMENT", schema = "athena", sequenceName = "SEQ_BILLING_REQUIREMENT")
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "SEQ_BILLING_REQUIREMENT")
    private Long billing_requirement_id;

    public enum Operator {
        GREATER_THAN(">"),
        GREATER_THAN_OR_EQUAL_TO(">="),
        LESS_THAN("<"),
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

    @Column(name = "attribute")
    private String attribute;

    @Column(name = "operator")
    @Enumerated(EnumType.STRING)
    private Operator operator;

    @Column(name = "value")
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

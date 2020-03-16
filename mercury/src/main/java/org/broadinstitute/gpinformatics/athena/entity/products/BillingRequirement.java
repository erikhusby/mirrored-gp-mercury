package org.broadinstitute.gpinformatics.athena.entity.products;

import org.broadinstitute.gpinformatics.athena.entity.work.MessageDataValue;
import org.hibernate.envers.Audited;

import javax.persistence.*;
import java.util.Map;

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

    public BillingRequirement() {
    }

    public BillingRequirement(String attribute, Operator operator, double value) {
        this.attribute = attribute;
        this.operator = operator;
        this.value = value;
    }

    /**
     * Check to see if this requirement is satisfied with the data.
     * @param data a name, value map of data
     * @return true if the requirements are satisfied.
     */
    public boolean canBill(Map<String, MessageDataValue> data) {
        MessageDataValue messageData = data.get(attribute);
        if (messageData != null) {
            try {
                double messageValue = Double.parseDouble(messageData.getValue());
                return operator.apply(messageValue, value);
            } catch (NumberFormatException e) {
                // Fall through to return false below.
            }
        }
        return false;
    }

    @Column(name = "attribute", length = 255)
    private String attribute;

    @Column(name = "operator", length = 30)
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

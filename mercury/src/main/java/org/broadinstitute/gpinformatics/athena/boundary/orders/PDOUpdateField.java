package org.broadinstitute.gpinformatics.athena.boundary.orders;

import org.broadinstitute.gpinformatics.athena.entity.orders.ProductOrder;
import org.broadinstitute.gpinformatics.infrastructure.jira.customfields.CustomField;

import javax.annotation.Nonnull;

/**
 * Utility class to help with mapping from display names of custom fields to their {@link org.broadinstitute.gpinformatics.infrastructure.jira.customfields.CustomFieldDefinition}s,
 * as well as tracking changes that have been made to the values of those fields relative to the existing state
 * of the PDO JIRA ticket.
 */
public class PDOUpdateField extends UpdateField<ProductOrder> {
    public PDOUpdateField(@Nonnull CustomField.SubmissionField field, Object newValue,
                          boolean isBulkField) {
        super(field, newValue, isBulkField);
    }

    public PDOUpdateField(@Nonnull CustomField.SubmissionField field, Object newValue) {
        super(field, newValue);
    }

    private PDOUpdateField(@Nonnull CustomField.SubmissionField field) {
        super(field);
        setNewValue(null);
    }

    /**
     * Returns a new field for the pdo's quote.
     */
    // todo arz move out of infratructure, make class similar to LCSETJiraFieldFactory?
    public static PDOUpdateField createPDOUpdateFieldForQuote(@Nonnull ProductOrder pdo) {
        return new PDOUpdateField(ProductOrder.JiraField.QUOTE_ID, pdo.getQuoteStringForJiraTicket());
    }

    /**
     * This method will clear the value of specified field.
     */
    public static PDOUpdateField clearedPDOUpdateField(@Nonnull CustomField.SubmissionField field) {
        return new PDOUpdateField(field);
    }
}

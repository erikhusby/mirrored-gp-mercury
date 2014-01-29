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
    public PDOUpdateField(@Nonnull CustomField.SubmissionField field, @Nonnull Object newValue,
                          boolean isBulkField) {
        super(field, newValue, isBulkField);
    }

    public PDOUpdateField(@Nonnull CustomField.SubmissionField field, @Nonnull Object newValue) {
        super(field, newValue);
    }

    /**
     * Returns a new field for the pdo's quote.
     */
    // todo arz move out of infratructure, make class similar to LCSETJiraFieldFactory?
    public static PDOUpdateField createPDOUpdateFieldForQuote(@Nonnull ProductOrder pdo) {
        return new PDOUpdateField(ProductOrder.JiraField.QUOTE_ID, pdo.getQuoteStringForJiraTicket());
    }
}
/*
 * The Broad Institute
 * SOFTWARE COPYRIGHT NOTICE AGREEMENT
 * This software and its documentation are copyright 2013 by the
 * Broad Institute/Massachusetts Institute of Technology. All rights are reserved.
 *
 * This software is supplied without any warranty or guaranteed support
 * whatsoever. Neither the Broad Institute nor MIT can be responsible for its
 * use, misuse, or functionality.
 */

package org.broadinstitute.gpinformatics.athena.boundary.orders;

import org.apache.commons.lang3.BooleanUtils;
import org.apache.commons.lang3.StringUtils;
import org.broadinstitute.gpinformatics.infrastructure.jira.JiraProject;
import org.broadinstitute.gpinformatics.infrastructure.jira.customfields.CustomField;
import org.broadinstitute.gpinformatics.infrastructure.jira.customfields.CustomFieldDefinition;
import org.broadinstitute.gpinformatics.infrastructure.jira.issue.CreateFields;
import org.broadinstitute.gpinformatics.infrastructure.jira.issue.IssueFieldsResponse;

import javax.annotation.Nonnull;
import java.util.Map;

public class UpdateField<PROJECT_TYPE extends JiraProject> {
    private final Object newValue;
    private final static String UPDATE_FIELD_NAME = "name";
    private final static String UPDATE_FIELD_VALUE = "value";

    /**
     * True if the field being updated is a 'bulk' item.  This means that it should be shown as plural in the
     * message, and its contents won't be shown in the message.
     */
    private final boolean isBulkField;
    private final CustomField.SubmissionField field;

    /**
     * Return the update message appropriate for this field.  If there are no changes this will return the empty
     * string, otherwise a string of the form "Product was updated from 'Old Product' to 'New Product'".
     *
     * @param jiraProject            the jira project
     * @param customFieldDefinitionMap contains the mapping from display names of fields to their JIRA IDs, needed
     *                                 to dig the old values contained in the fields out of the issueFieldsResponse
     * @param issueFieldsResponse      contains the old values
     *
     * @return the update message that goes in the JIRA ticket
     */
    public String getUpdateMessage(PROJECT_TYPE jiraProject,
                                   Map<String, CustomFieldDefinition> customFieldDefinitionMap,
                                   IssueFieldsResponse issueFieldsResponse) {

        if (!customFieldDefinitionMap.containsKey(getDisplayName())) {
            throw new RuntimeException(
                    "Custom field '" + getDisplayName() + "' not found in issue " + jiraProject.getJiraTicketKey());
        }
        CustomFieldDefinition customFieldDefinition = customFieldDefinitionMap.get(getDisplayName());

        Object previousValue = issueFieldsResponse.getFields().get(customFieldDefinition.getJiraCustomFieldId());
        Object oldValueToCompare;
        if (previousValue instanceof Map) {
            Map previousValueMap = ((Map<?, ?>) previousValue);
            if (previousValueMap.containsKey(UPDATE_FIELD_VALUE)) {
                oldValueToCompare = previousValueMap.get(UPDATE_FIELD_VALUE).toString();
            } else {
                oldValueToCompare = previousValueMap.get(UPDATE_FIELD_NAME).toString();
            }
        } else {
            oldValueToCompare = (previousValue != null) ? previousValue : "";
        }
        Object newValueToCompare = newValue;
        // Jira stores booleans as Yes and No. We need to convert the test value to a "Jira boolean"
        // or we will not be able to figure out if it has changed and we will always add a jira comment that the value
        // has been updated when in fact it may not have been.
        if (oldValueToCompare instanceof Double) {
            newValueToCompare = ((Integer) newValue).doubleValue();
        }

        if (newValue instanceof Boolean) {
            newValueToCompare = StringUtils.capitalize(BooleanUtils.toStringYesNo((Boolean) newValue));
        } else if (newValue instanceof CreateFields.Reporter) {
            // Need to special case Reporter type for display and comparison.
            oldValueToCompare = ((Map<?, ?>) previousValue).get(UPDATE_FIELD_NAME).toString();
            newValueToCompare = ((CreateFields.Reporter) newValue).getName();
        }
        if (!oldValueToCompare.equals(newValueToCompare)) {
            return getDisplayName() + (isBulkField ? " have " : " has ") + "been updated" +
                   (!isBulkField ? " from '" + oldValueToCompare + "' to '" + newValueToCompare + "'" : "") + ".\n";
        }
        return "";
    }

    public UpdateField(@Nonnull CustomField.SubmissionField field, @Nonnull Object newValue,
                       boolean isBulkField) {
        this.field = field;
        this.newValue = newValue;
        this.isBulkField = isBulkField;
    }

    public UpdateField(@Nonnull CustomField.SubmissionField field, @Nonnull Object newValue) {
        this(field, newValue, false);
    }

    public CustomField.SubmissionField getField() {
        return field;
    }

    public String getDisplayName() {
        return field.getName();
    }

    public Object getNewValue() {
        return newValue;
    }

    public boolean isBulkField() {
        return isBulkField;
    }
}

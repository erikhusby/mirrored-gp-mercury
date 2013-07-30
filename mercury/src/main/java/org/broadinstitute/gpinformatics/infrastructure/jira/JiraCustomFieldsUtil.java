package org.broadinstitute.gpinformatics.infrastructure.jira;

import org.broadinstitute.gpinformatics.athena.entity.orders.ProductOrder;
import org.broadinstitute.gpinformatics.athena.entity.project.ResearchProject;
import org.broadinstitute.gpinformatics.infrastructure.jira.customfields.CustomFieldDefinition;
import org.broadinstitute.gpinformatics.infrastructure.jira.issue.CreateFields;
import org.broadinstitute.gpinformatics.mercury.entity.workflow.LabBatch;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

public class JiraCustomFieldsUtil {

    public static final String PROTOCOL = "Protocol";

    public static final String WORK_REQUEST_IDS = "Work Request ID(s)";

    public static final String GSSR_IDS = "GSSR ID(s)";

    public static final String DESCRIPTION = "Description";

    public static final String[] REQUIRED_FIELD_NAMES =
            new String[]{PROTOCOL, WORK_REQUEST_IDS, GSSR_IDS, DESCRIPTION,
                    ProductOrder.JiraField.PRODUCT_FAMILY.getFieldName(),
                    ProductOrder.JiraField.QUOTE_ID.getFieldName(),
                    ResearchProject.RequiredSubmissionFields.FUNDING_SOURCE.getFieldName(),
                    ResearchProject.RequiredSubmissionFields.IRB_NOT_ENGAGED_FIELD.getFieldName(),
                    ResearchProject.RequiredSubmissionFields.IRB_IACUC_NUMBER.getFieldName(),
                    ResearchProject.RequiredSubmissionFields.COHORTS.getFieldName(),
                    ResearchProject.RequiredSubmissionFields.DESCRIPTION.getFieldName(),
                    LabBatch.TicketFields.PROTOCOL.getFieldName(),
                    LabBatch.TicketFields.WORK_REQUEST_IDS.getFieldName(),
                    LabBatch.TicketFields.POOLING_STATUS.getFieldName(),
                    LabBatch.TicketFields.PRIORITY.getFieldName(),
                    LabBatch.TicketFields.DUE_DATE.getFieldName(),
                    LabBatch.TicketFields.IMPORTANT.getFieldName(),
                    LabBatch.TicketFields.NUMBER_OF_CONTROLS.getFieldName(),
                    LabBatch.TicketFields.NUMBER_OF_SAMPLES.getFieldName(),
                    LabBatch.TicketFields.LIBRARY_QC_SEQUENCING_REQUIRED.getFieldName(),
                    LabBatch.TicketFields.PROGRESS_STATUS.getFieldName(),
                    LabBatch.TicketFields.GSSR_IDS.getFieldName(),
                    LabBatch.TicketFields.DESCRIPTION.getFieldName(),
                    LabBatch.TicketFields.SUMMARY.getFieldName(),
                    LabBatch.TicketFields.SEQUENCING_STATION.getFieldName(),
            };

    /**
     * Returns a map of Field name (from {@link #REQUIRED_FIELD_NAMES}) to actual field definition {@link CustomFieldDefinition}.
     */
    public static Map<String, CustomFieldDefinition> getRequiredLcSetFieldDefinitions(JiraService jiraService)
            throws IOException {
        final Map<String, CustomFieldDefinition> allCustomFields =
                jiraService.getRequiredFields(
                        new CreateFields.Project(CreateFields.ProjectType.LCSET_PROJECT.getKeyPrefix()),
                        CreateFields.IssueType.WHOLE_EXOME_HYBSEL);

        final Map<String, CustomFieldDefinition> requiredCustomFieldDefinitions =
                new HashMap<>();

        for (String requiredFieldName : REQUIRED_FIELD_NAMES) {
            boolean foundIt = false;
            for (CustomFieldDefinition customFieldDefinition : allCustomFields.values()) {
                if (requiredFieldName.equals(customFieldDefinition.getName())) {
                    foundIt = true;
                    requiredCustomFieldDefinitions.put(requiredFieldName, customFieldDefinition);
                    break;
                }
            }
            if (!foundIt) {
                throw new RuntimeException("Could not find required field '" +
                                           requiredFieldName + "' from jira service " +
                                           jiraService.getClass().getCanonicalName());
            }
        }

        return requiredCustomFieldDefinitions;
    }
}

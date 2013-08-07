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
                    ProductOrder.JiraField.PRODUCT_FAMILY.getName(),
                    ProductOrder.JiraField.QUOTE_ID.getName(),
                    ResearchProject.RequiredSubmissionFields.FUNDING_SOURCE.getName(),
                    ResearchProject.RequiredSubmissionFields.IRB_NOT_ENGAGED_FIELD.getName(),
                    ResearchProject.RequiredSubmissionFields.IRB_IACUC_NUMBER.getName(),
                    ResearchProject.RequiredSubmissionFields.COHORTS.getName(),
                    ResearchProject.RequiredSubmissionFields.DESCRIPTION.getName(),
                    LabBatch.TicketFields.PROTOCOL.getName(),
                    LabBatch.TicketFields.WORK_REQUEST_IDS.getName(),
                    LabBatch.TicketFields.POOLING_STATUS.getName(),
                    LabBatch.TicketFields.PRIORITY.getName(),
                    LabBatch.TicketFields.DUE_DATE.getName(),
                    LabBatch.TicketFields.IMPORTANT.getName(),
                    LabBatch.TicketFields.NUMBER_OF_CONTROLS.getName(),
                    LabBatch.TicketFields.NUMBER_OF_SAMPLES.getName(),
                    LabBatch.TicketFields.LIBRARY_QC_SEQUENCING_REQUIRED.getName(),
                    LabBatch.TicketFields.PROGRESS_STATUS.getName(),
                    LabBatch.TicketFields.GSSR_IDS.getName(),
                    LabBatch.TicketFields.DESCRIPTION.getName(),
                    LabBatch.TicketFields.SUMMARY.getName(),
                    LabBatch.TicketFields.SEQUENCING_STATION.getName(),
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

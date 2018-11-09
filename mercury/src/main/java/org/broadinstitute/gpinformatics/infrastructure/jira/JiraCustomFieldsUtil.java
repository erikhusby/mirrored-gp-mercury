package org.broadinstitute.gpinformatics.infrastructure.jira;

import org.broadinstitute.gpinformatics.athena.boundary.projects.ResearchProjectEjb;
import org.broadinstitute.gpinformatics.athena.entity.orders.ProductOrder;
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
                    ProductOrder.JiraField.PRODUCT.getName(),
                    ProductOrder.JiraField.QUOTE_ID.getName(),
                    ProductOrder.JiraField.ADD_ONS.getName(),
                    ProductOrder.JiraField.SAMPLE_IDS.getName(),
                    ProductOrder.JiraField.NUMBER_OF_SAMPLES.getName(),
                    ProductOrder.JiraField.LANES_PER_SAMPLE.getName(),
                    ProductOrder.JiraField.PUBLICATION_DEADLINE.getName(),
                    ProductOrder.JiraField.FUNDING_DEADLINE.getName(),
                    ResearchProjectEjb.RequiredSubmissionFields.FUNDING_SOURCE.getName(),
                    ResearchProjectEjb.RequiredSubmissionFields.COHORTS.getName(),
                    ResearchProjectEjb.RequiredSubmissionFields.DESCRIPTION.getName(),
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
                    LabBatch.TicketFields.CLUSTER_STATION.getName(),
                    LabBatch.TicketFields.BATCH_TYPE.getName(),
                    LabBatch.TicketFields.MATERIAL_TYPE.getName(),
                    LabBatch.TicketFields.LANE_INFO.getName(),
                    LabBatch.TicketFields.SAMPLES_ON_RISK.getName(),
                    LabBatch.TicketFields.RISK_CATEGORIZED_SAMPLES.getName(),
                    LabBatch.TicketFields.REWORK_SAMPLES.getName()
            };

    /**
     * Returns a map of Field name (from {@link #REQUIRED_FIELD_NAMES}) to actual field definition {@link CustomFieldDefinition}.
     */
    public static Map<String, CustomFieldDefinition> getRequiredLcSetFieldDefinitions(JiraService jiraService)
            throws IOException {
        Map<String, CustomFieldDefinition> allCustomFields =
                jiraService.getRequiredFields(
                        new CreateFields.Project(CreateFields.ProjectType.LCSET_PROJECT),
                        CreateFields.IssueType.WHOLE_EXOME_HYBSEL);

        Map<String, CustomFieldDefinition> requiredCustomFieldDefinitions = new HashMap<>();

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

    /**
     * Returns a map of Field name (from {@link #REQUIRED_FIELD_NAMES}) to actual field definition {@link CustomFieldDefinition}.
     */
    public static Map<String, CustomFieldDefinition> getAllowedFields(JiraService jiraService,
                                                                      final CreateFields.ProjectType projectType,
                                                                      final CreateFields.IssueType issueType)
            throws IOException {
        Map<String, CustomFieldDefinition> allCustomFields =
                jiraService.getRequiredFields(new CreateFields.Project(projectType), issueType);

        return allCustomFields;
    }

}

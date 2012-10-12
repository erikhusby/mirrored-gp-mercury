package org.broadinstitute.gpinformatics.infrastructure.jira;

import org.broadinstitute.gpinformatics.athena.entity.orders.ProductOrder;
import org.broadinstitute.gpinformatics.mercury.entity.workflow.LabBatch;
import org.broadinstitute.gpinformatics.infrastructure.jira.customfields.CustomFieldDefinition;
import org.broadinstitute.gpinformatics.infrastructure.jira.issue.CreateIssueRequest;

import java.io.IOException;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

public class JiraCustomFieldsUtil {

    public static final String PROTOCOL = "Protocol";

    public static final String WORK_REQUEST_IDS = "Work Request ID(s)";

    public static final String GSSR_IDS = "GSSR ID(s)";

    public static final String[] REQUIRED_FIELD_NAMES = new String[] {PROTOCOL,WORK_REQUEST_IDS,GSSR_IDS, ProductOrder.RequiredSubmissionFields.PRODUCT_FAMILY.getFieldName(),ProductOrder.RequiredSubmissionFields.QUOTE_ID.getFieldName(),};

    /**
     * Returns a map of Field name (from {@link #REQUIRED_FIELD_NAMES}) to actual field definition {@link CustomFieldDefinition}.
     */
    public static Map<String,CustomFieldDefinition> getRequiredLcSetFieldDefinitions(JiraService jiraService) throws IOException {
        final Map<String, CustomFieldDefinition> allCustomFields =
                jiraService.getRequiredFields ( new CreateIssueRequest.Fields.Project ( LabBatch.LCSET_PROJECT_PREFIX ),
                                                CreateIssueRequest.Fields.Issuetype.Whole_Exome_HybSel );

        final Map<String,CustomFieldDefinition> requiredCustomFieldDefinitions = new HashMap<String,CustomFieldDefinition>();

        for (String requiredFieldName : REQUIRED_FIELD_NAMES) {
            boolean foundIt = false;
            for (CustomFieldDefinition customFieldDefinition : allCustomFields.values()) {
                if (requiredFieldName.equals(customFieldDefinition.getName())) {
                    foundIt = true;
                    requiredCustomFieldDefinitions.put(requiredFieldName,customFieldDefinition);
                    break;
                }
            }
            if (!foundIt) {
                throw new RuntimeException("Could not find required field '" +
                                           requiredFieldName + "' from jira service " +
                                           jiraService.getClass().getCanonicalName());
            }
        }

        //this.fields.customFields.add(new CustomField(new CustomFieldDefinition("customfield_10020","Protocol",true),"test protocol"));
        //this.fields.customFields.add(new CustomField(new CustomFieldDefinition("customfield_10011","Work Request ID(s)",true),"WR 1 Billion!"));
        return requiredCustomFieldDefinitions;
    }
}

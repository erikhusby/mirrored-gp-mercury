package org.broadinstitute.gpinformatics.mercury.infrastructure.jira.customfields;

import org.codehaus.jackson.map.ObjectMapper;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Parses json response from jira createmeta call.  See https://developer.atlassian.com/static/rest/jira/5.0.html#id200251
 */
public class CustomFieldJsonParser {

    private static final String CUSTOMFIELD = "customfield";

    private static final String PROJECTS = "projects";

    private static final String ISSUETYPES = "issuetypes";

    private static final String FIELDS = "fields";

    private static final String NAME = "name";

    private static final String REQUIRED = "required";

    /**
     * Parses the custom fields from the given json response
     * @param jsonResponse
     * @return
     * @throws IOException
     */
    public static List<CustomFieldDefinition> parseCustomFields(String jsonResponse)
            throws IOException {
        final List<CustomFieldDefinition> customFields = new ArrayList<CustomFieldDefinition>();
        final Map root = new ObjectMapper().readValue(jsonResponse,Map.class);
        final List projects  = (List)root.get(PROJECTS);
        final List issueTypes = (List)((Map)projects.iterator().next()).get(ISSUETYPES);
        final Map parsedIssueType = (Map)issueTypes.iterator().next();
        final Map<String,Map> fields = (Map<String,Map>)parsedIssueType.get(FIELDS);

        for (Map.Entry<String,Map> field: fields.entrySet()) {
            String fieldId = field.getKey();
            Map fieldProperties = field.getValue();
            String fieldName = (String)fieldProperties.get(NAME);
            Boolean required = (Boolean)fieldProperties.get(REQUIRED);

            if (fieldId.startsWith(CUSTOMFIELD)) {
                customFields.add(new CustomFieldDefinition(fieldId,fieldName,required));
            }
        }
        return customFields;
    }
}

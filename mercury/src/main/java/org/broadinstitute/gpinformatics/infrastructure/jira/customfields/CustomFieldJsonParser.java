package org.broadinstitute.gpinformatics.infrastructure.jira.customfields;

import org.codehaus.jackson.map.ObjectMapper;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Parses json response from jira createmeta call.  See https://developer.atlassian.com/static/rest/jira/5.0.html#id200251
 */
public class CustomFieldJsonParser {

    private static final String CUSTOMFIELD = "customfield";

    private static final String CUSTOM_FIELD_INDICATOR = "custom";

    private static final String PROJECTS = "projects";

    private static final String ISSUETYPES = "issuetypes";

    private static final String FIELDS = "fields";

    private static final String NAME = "name";

    private static final String REQUIRED = "required";

    private static final String FIELD_ID = "id";

    /**
     * Parses the custom fields from the given json response
     *
     * @param jsonResponse
     * @return
     * @throws IOException
     */
    public static Map<String, CustomFieldDefinition> parseRequiredFields(String jsonResponse)
            throws IOException {
        final Map<String, CustomFieldDefinition> customFields = new HashMap<String, CustomFieldDefinition>();
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
                customFields.put(fieldName, new CustomFieldDefinition(fieldId, fieldName, required));
            }
        }
        return customFields;
    }
    /**
     * Parses the custom fields from the given json response
     *
     * @param jsonResponse
     * @return
     * @throws IOException
     */
    public static Map<String, CustomFieldDefinition> parseCustomFields(String jsonResponse)
            throws IOException {

        final Map<String, CustomFieldDefinition> customFields = new HashMap<String, CustomFieldDefinition>();

        final ArrayList<Map> root = new ObjectMapper().readValue(jsonResponse,ArrayList.class);

        Iterator<Map> values = root.iterator();

        while(values.hasNext()) {
            Map<String, Object> field = (Map<String, Object>)values.next();

            String fieldId = (String)field.get(FIELD_ID);
            String fieldName = (String)field.get(NAME);
            Boolean required = false;  //Leaving false for now until can better come up with a solution.

//            if ((Boolean)field.get(CUSTOM_FIELD_INDICATOR)) {
                customFields.put(fieldName, new CustomFieldDefinition(fieldId,fieldName,required));
//            }

            /*
            This needs a good way to account for different types (String, textfield, multi-select, etc.)
             */

        }
        return customFields;
    }
}

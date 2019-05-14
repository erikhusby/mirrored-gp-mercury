package org.broadinstitute.gpinformatics.infrastructure.jira.customfields;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.commons.lang3.StringUtils;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Parses json response from JIRA createmeta call.  See https://developer.atlassian.com/static/rest/jira/5.0.html#id200251
 */
public class CustomFieldJsonParser {

    private static final String PROJECTS = "projects";

    private static final String ISSUETYPES = "issuetypes";

    private static final String FIELDS = "fields";

    private static final String NAME = "name";

    private static final String REQUIRED = "required";

    private static final String ALLOWED_VALUES = "allowedValues";

    private static final String FIELD_ID = "id";

    /**
     * Parses the custom fields from the given json response.
     */
    public static Map<String, CustomFieldDefinition> parseRequiredFields(String jsonResponse) throws IOException {
        final Map<String, CustomFieldDefinition> customFields = new HashMap<>();
        final Map root = new ObjectMapper().readValue(jsonResponse, Map.class);
        final List projects = (List) root.get(PROJECTS);
        final List issueTypes = (List) ((Map) projects.iterator().next()).get(ISSUETYPES);
        final Map parsedIssueType = (Map) issueTypes.iterator().next();
        @SuppressWarnings("unchecked")
        final Map<String, Map> fields = (Map<String, Map>) parsedIssueType.get(FIELDS);

        for (Map.Entry<String, Map> field : fields.entrySet()) {
            String fieldId = field.getKey();
            Map fieldProperties = field.getValue();
            String fieldName = (String) fieldProperties.get(NAME);
            Boolean required = (Boolean) fieldProperties.get(REQUIRED);
            List<Map<String, String>> values = (ArrayList<Map<String, String>>) fieldProperties.get(ALLOWED_VALUES);

            Collection<CustomField.ValueContainer> allowedValues = new ArrayList<>();
            if (values!=null) {
                for (Map<String, String> allowedValue : values) {
                    if (allowedValue.containsKey("value")) {
                        String value = allowedValue.get("value");
                            allowedValues.add(new CustomField.ValueContainer(value));
                    }
                }
            }
            if (StringUtils.isNotBlank(fieldName)) {
                customFields.put(fieldName, new CustomFieldDefinition(fieldId, fieldName, required, allowedValues));
            }
        }
        return customFields;
    }

    /**
     * Parses the custom fields from the given json response.
     */
    @SuppressWarnings("unchecked")
    public static Map<String, CustomFieldDefinition> parseCustomFields(String jsonResponse)
            throws IOException {

        final Map<String, CustomFieldDefinition> customFields = new HashMap<>();

        final ArrayList<Map<String, Object>> root = new ObjectMapper().readValue(jsonResponse, ArrayList.class);

        for (Map<String, Object> field : root) {

            String fieldId = (String) field.get(FIELD_ID);
            String fieldName = (String) field.get(NAME);
            // Leaving false for now until can better come up with a solution.
            Boolean required = false;

            customFields.put(fieldName, new CustomFieldDefinition(fieldId, fieldName, required));

            // This needs a good way to account for different types (String, textfield, multi-select, etc.)
        }
        return customFields;
    }
}

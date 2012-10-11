package org.broadinstitute.gpinformatics.infrastructure.jira.issue.transition;

import java.util.List;

/**
* @author Scott Matthews
*         Date: 10/11/12
*         Time: 9:31 AM
*/
public class TransitionFields {

    private String required;
    private FieldSchema schema;
    private String name;
    private List<String>           operations;
    private List<String> allowedValues;

    public TransitionFields ( String requiredIn, FieldSchema schemaIn, String nameIn ) {
        required = requiredIn;
        schema = schemaIn;
        name = nameIn;
    }

    public TransitionFields ( String requiredIn, FieldSchema schemaIn, String nameIn, List<String> operationsIn,
                              List<String> allowedValuesIn ) {
        required = requiredIn;
        schema = schemaIn;
        name = nameIn;
        operations = operationsIn;
        allowedValues = allowedValuesIn;
    }

    public String getRequired ( ) {
        return required;
    }

    public FieldSchema getSchema ( ) {
        return schema;
    }

    public String getName ( ) {
        return name;
    }

    public List<String> getOperations ( ) {
        return operations;
    }

    public List<String> getAllowedValues ( ) {
        return allowedValues;
    }
}
